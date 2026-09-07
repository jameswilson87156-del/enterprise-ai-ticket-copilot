[CmdletBinding()]
param(
    [string]$EnvironmentFile = '',
    [ValidateRange(1024, 65535)]
    [int]$Port = 28083,
    [ValidateRange(30, 240)]
    [int]$StartupTimeoutSeconds = 150
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$backend = Join-Path $root 'backend'
$schema = Join-Path $backend 'src\test\resources\schema-h2.sql'
$baseUrl = "http://127.0.0.1:$Port"
$log = Join-Path ([System.IO.Path]::GetTempPath()) "ticket-copilot-deepseek-smoke-$Port.log"
$errorLog = "$log.err"
if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    $EnvironmentFile = Join-Path $root 'deploy\staging\.env'
}

function Read-EnvFile([string]$path) {
    if (-not (Test-Path -LiteralPath $path)) { throw "Provider environment file not found: $path" }
    $values = @{}
    foreach ($line in Get-Content -LiteralPath $path) {
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
            $value = $matches[2].Trim()
            if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            $values[$matches[1]] = $value
        }
    }
    return $values
}

function Require-Configured([hashtable]$values, [string]$name) {
    $value = [string]$values[$name]
    if ([string]::IsNullOrWhiteSpace($value) -or $value -match 'CHANGE_ME|placeholder|example') {
        throw "$name is missing or still contains a template value."
    }
    return $value
}

if (-not (Test-Path -LiteralPath $schema)) { throw "H2 schema not found: $schema" }
if (@(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue).Count -gt 0) {
    throw "Refusing to use occupied smoke port: $Port"
}

$values = Read-EnvFile $EnvironmentFile
$provider = Require-Configured $values 'TICKET_AI_PROVIDER'
$base = Require-Configured $values 'TICKET_AI_BASE_URL'
$model = Require-Configured $values 'TICKET_AI_MODEL'
$apiKey = Require-Configured $values 'TICKET_AI_API_KEY'
if ($provider.ToLowerInvariant() -ne 'deepseek') { throw "This smoke requires TICKET_AI_PROVIDER=deepseek." }

$names = @(
    'SERVER_PORT', 'SPRING_SQL_INIT_MODE', 'SPRING_SQL_INIT_SCHEMA_LOCATIONS',
    'TICKET_AI_PROVIDER', 'TICKET_AI_BASE_URL', 'TICKET_AI_MODEL',
    'TICKET_AI_API_KEY', 'TICKET_AI_PROTOCOL', 'TICKET_AI_FALLBACK_TO_LOCAL'
)
$saved = @{}
foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name) }
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

function Set-SmokeEnv([string]$name, [string]$value) { Set-Item -Path "Env:$name" -Value $value }

try {
    Set-SmokeEnv 'SERVER_PORT' ([string]$Port)
    Set-SmokeEnv 'SPRING_SQL_INIT_MODE' 'always'
    Set-SmokeEnv 'SPRING_SQL_INIT_SCHEMA_LOCATIONS' "file:$($schema.Replace('\', '/'))"
    Set-SmokeEnv 'TICKET_AI_PROVIDER' $provider
    Set-SmokeEnv 'TICKET_AI_BASE_URL' $base
    Set-SmokeEnv 'TICKET_AI_MODEL' $model
    Set-SmokeEnv 'TICKET_AI_API_KEY' $apiKey
    Set-SmokeEnv 'TICKET_AI_PROTOCOL' ([string]$values['TICKET_AI_PROTOCOL'])
    Set-SmokeEnv 'TICKET_AI_FALLBACK_TO_LOCAL' 'false'

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
        if ($maven.HasExited) { throw "DeepSeek smoke process exited before health became ready. Logs: $log" }
        try { $health = Invoke-RestMethod -Uri "$baseUrl/api/health" -TimeoutSec 3 } catch {}
    } while ($null -eq $health -and (Get-Date) -lt $deadline)
    if ($null -eq $health -or $health.status -ne 'UP') { throw 'DeepSeek smoke health did not become UP.' }

    $agentLogin = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" `
        -ContentType 'application/json' -Body '{"username":"agent","password":"agent123"}'
    $agentHeaders = @{ Authorization = "Bearer $($agentLogin.token)" }
    $createBody = @{
        title = 'payment-service returns 500'
        description = 'Payment service fails after release and returns HTTP 500.'
        systemName = 'payment-service'
        urgency = 'P2'
        requester = 'DeepSeek Synthetic Runner'
        requesterDepartment = 'QA'
    } | ConvertTo-Json -Compress
    $created = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/tickets" `
        -Headers $agentHeaders -ContentType 'application/json' -Body $createBody
    $run = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/tickets/$($created.id)/run-copilot" -Headers $agentHeaders
    $trace = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/tickets/$($created.id)/trace-evidence" -Headers $agentHeaders
    $copilot = $trace.copilotRun

    if ($copilot.requestedProvider -ne 'deepseek' -or $copilot.actualProvider -ne 'deepseek') { throw 'Trace did not prove that DeepSeek was the actual Provider.' }
    if ($copilot.runStatus -ne 'SUCCESS' -or $copilot.fallbackUsed -ne $false -or $copilot.errorCategory -ne 'NONE') { throw 'DeepSeek run was not a successful no-fallback run.' }
    if ([int]$copilot.retrievalHitCount -lt 1 -or $copilot.outputProduced -ne $true -or $copilot.humanReviewRequired -ne $true) { throw 'DeepSeek run did not preserve retrieval and human-review gates.' }
    $structured = $trace.structuredOutput
    $validatedCitationCount = @($trace.validatedCitations).Count
    if ($copilot.citationValidationStatus -ne 'VALID' -or $copilot.outputValidationStatus -ne 'VALID' -or $validatedCitationCount -lt 1) {
        Write-Host ("TICKET_DEEPSEEK_SYNTHETIC_FAIL provider={0} actual={1} run={2} output={3} citations={4} validatedCitations={5} abstained={6} abstentionReason={7} validCitationCount={8} rejectedCitationCount={9}" -f `
            $copilot.requestedProvider,
            $copilot.actualProvider,
            $copilot.runStatus,
            $copilot.outputValidationStatus,
            $copilot.citationValidationStatus,
            $validatedCitationCount,
            $structured.abstained,
            $structured.abstentionReasonCode,
            $structured.validCitationCount,
            $structured.rejectedCitationCount)
        throw 'DeepSeek output or citation validation did not pass.'
    }

    $reviewerLogin = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/auth/login" `
        -ContentType 'application/json' -Body '{"username":"reviewer","password":"reviewer123"}'
    $reviewHeaders = @{ Authorization = "Bearer $($reviewerLogin.token)" }
    $approved = Invoke-RestMethod -Method Post -Uri "$baseUrl/api/tickets/$($created.id)/review/approve" `
        -Headers $reviewHeaders -ContentType 'application/json' `
        -Body (@{ comment = 'DeepSeek synthetic provider approval' } | ConvertTo-Json -Compress)
    if ($approved.status -ne 'RESOLVED') { throw "DeepSeek review approval status unexpected: $($approved.status)" }

    Write-Host "TICKET_DEEPSEEK_SYNTHETIC_OK provider=deepseek model=$model ticket=$($created.id) run=$($copilot.runStatus) citations=$(@($trace.validatedCitations).Count) final=$($approved.status)"
} finally {
    if ($maven -and -not $maven.HasExited) { Stop-Process -Id $maven.Id -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 2
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique)
    foreach ($listenerPid in $listeners) {
        $process = Get-CimInstance Win32_Process -Filter "ProcessId=$listenerPid" -ErrorAction SilentlyContinue
        if ($null -ne $process -and $process.CommandLine.Contains('EnterpriseAiTicketCopilotApplication')) {
            Stop-Process -Id $listenerPid -Force -ErrorAction SilentlyContinue
        }
    }
    Restore-Environment
}
