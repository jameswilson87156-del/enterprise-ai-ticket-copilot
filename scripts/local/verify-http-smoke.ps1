[CmdletBinding()]
param(
    [ValidateRange(1024, 65535)]
    [int]$Port = 28082,
    [ValidateRange(20, 180)]
    [int]$StartupTimeoutSeconds = 90
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$backend = Join-Path $root 'backend'
$schema = Join-Path $backend 'src\test\resources\schema-h2.sql'
$baseUrl = "http://127.0.0.1:$Port"
$log = Join-Path ([System.IO.Path]::GetTempPath()) "ticket-copilot-http-smoke-$Port.log"
$errorLog = "$log.err"

if (-not (Test-Path -LiteralPath $schema)) {
    throw "H2 schema not found: $schema"
}
if (@(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue).Count -gt 0) {
    throw "Refusing to use occupied smoke port: $Port"
}

$saved = @{
    SERVER_PORT = [Environment]::GetEnvironmentVariable('SERVER_PORT')
    SPRING_SQL_INIT_MODE = [Environment]::GetEnvironmentVariable('SPRING_SQL_INIT_MODE')
    SPRING_SQL_INIT_SCHEMA_LOCATIONS = [Environment]::GetEnvironmentVariable('SPRING_SQL_INIT_SCHEMA_LOCATIONS')
}
$maven = $null

function Restore-Environment {
    foreach ($name in $saved.Keys) {
        if ($null -eq $saved[$name]) {
            Remove-Item -Path "Env:$name" -ErrorAction SilentlyContinue
        } else {
            Set-Item -Path "Env:$name" -Value $saved[$name]
        }
    }
}

try {
    $env:SERVER_PORT = [string]$Port
    $env:SPRING_SQL_INIT_MODE = 'always'
    $env:SPRING_SQL_INIT_SCHEMA_LOCATIONS = "file:$($schema.Replace('\', '/'))"

    $maven = Start-Process -FilePath 'mvn.cmd' `
        -ArgumentList @('-Dspring-boot.run.profiles=test', '-Dspring-boot.run.useTestClasspath=true', 'spring-boot:run') `
        -WorkingDirectory $backend `
        -RedirectStandardOutput $log `
        -RedirectStandardError $errorLog `
        -WindowStyle Hidden `
        -PassThru

    $health = $null
    $deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
    do {
        Start-Sleep -Seconds 2
        if ($maven.HasExited) {
            $tail = if (Test-Path -LiteralPath $errorLog) { (Get-Content -LiteralPath $errorLog -Tail 30 -ErrorAction SilentlyContinue) -join "`n" } else { '' }
            throw "Ticket HTTP smoke process exited before health became ready. Log: $errorLog`n$tail"
        }
        try {
            $health = Invoke-RestMethod -Uri "$baseUrl/api/health" -TimeoutSec 3
        } catch {
            # The application is still starting; retry until the bounded deadline.
        }
    } while ($null -eq $health -and (Get-Date) -lt $deadline)

    if ($null -eq $health -or $health.status -ne 'UP') {
        throw "Ticket HTTP smoke health failed: $($health | ConvertTo-Json -Compress)"
    }

    $agentLogin = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" `
        -ContentType 'application/json' -Body '{"username":"agent","password":"agent123"}'
    $agentHeaders = @{ Authorization = "Bearer $($agentLogin.token)" }
    $createBody = @{
        title = 'Phase 1 HTTP smoke'
        description = 'Synthetic local acceptance ticket'
        systemName = 'office-faq'
        urgency = 'P3'
        requester = 'Phase 1 Runner'
        requesterDepartment = 'QA'
    } | ConvertTo-Json -Compress

    $created = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/tickets" `
        -Headers $agentHeaders -ContentType 'application/json' -Body $createBody
    $run = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/tickets/$($created.id)/run-copilot" `
        -Headers $agentHeaders
    $trace = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/tickets/$($created.id)/trace-evidence" `
        -Headers $agentHeaders

    if ([string]::IsNullOrWhiteSpace([string]$trace.runId)) {
        throw 'Ticket HTTP smoke did not return a Trace runId.'
    }

    $reviewerLogin = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" `
        -ContentType 'application/json' -Body '{"username":"reviewer","password":"reviewer123"}'
    $reviewHeaders = @{ Authorization = "Bearer $($reviewerLogin.token)" }
    $approved = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/tickets/$($created.id)/review/approve" `
        -Headers $reviewHeaders -ContentType 'application/json' `
        -Body (@{ comment = 'Phase 1 synthetic approval' } | ConvertTo-Json -Compress)

    if ($approved.status -ne 'RESOLVED') {
        throw "Ticket HTTP smoke approval status unexpected: $($approved.status)"
    }

    Write-Host "TICKET_HTTP_SMOKE_OK health=$($health.status) ticket=$($created.id) run=$($run.status) trace=$($trace.runId) final=$($approved.status)"
} finally {
    if ($maven -and -not $maven.HasExited) {
        Stop-Process -Id $maven.Id -Force -ErrorAction SilentlyContinue
    }

    Start-Sleep -Seconds 2
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique)
    foreach ($listenerPid in $listeners) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$listenerPid" -ErrorAction SilentlyContinue
        if ($null -ne $process -and $process.CommandLine.Contains("EnterpriseAiTicketCopilotApplication")) {
            Stop-Process -Id $listenerPid -Force -ErrorAction SilentlyContinue
        }
    }

    Restore-Environment
}
