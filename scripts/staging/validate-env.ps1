[CmdletBinding()]
param([Alias('EnvFile')][string]$EnvironmentFile = 'deploy/staging/.env')

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$file = if ([IO.Path]::IsPathRooted($EnvironmentFile)) { $EnvironmentFile } else { Join-Path $root $EnvironmentFile }
if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw 'BLOCKED: project staging environment file is missing.' }
$values = @{}
foreach ($line in Get-Content -LiteralPath $file) {
    if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        $name = $matches[1]
        if ($values.ContainsKey($name)) { throw "Duplicate environment variable: $name" }
        $value = $matches[2].Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) { $value = $value.Substring(1, $value.Length - 2) }
        $values[$name] = $value
    }
}
function Require-Setting([string]$name) {
    $v = [string]$values[$name]
    if ([string]::IsNullOrWhiteSpace($v)) { throw "Missing staging configuration: $name" }
    if ($v -match '(?i)CHANGE_ME|placeholder|example\.com') { throw "Template placeholder is still present: $name" }
}
function Require-Https([string]$name) {
    $uri = $null
    if (-not [Uri]::TryCreate($values[$name], [UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -ne 'https' -or $uri.IsLoopback -or $uri.UserInfo -or $uri.Fragment) { throw "A non-loopback HTTPS URL without credentials is required: $name" }
}
function Require-Database([string]$name) {
    $v = [string]$values[$name]
    if ($v -notmatch '^jdbc:mysql://[A-Za-z0-9.-]+(?::[0-9]+)?/[A-Za-z0-9_]+\?' -or $v -notmatch '(?:\?|&)sslMode=VERIFY_IDENTITY(?:&|$)' -or $v -match '(?i)useSSL=false|allowPublicKeyRetrieval=true|://(?:localhost|127\.|mysql[:/])') { throw "Private MySQL URL with sslMode=VERIFY_IDENTITY is required: $name" }
}

$required = @(
    'TICKET_HOST',
    'ACME_EMAIL',
    'TICKET_DB_URL',
    'TICKET_DB_USERNAME',
    'TICKET_DB_PASSWORD',
    'TICKET_AUTH_ISSUER_URI',
    'TICKET_AUTH_AUDIENCE',
    'VITE_TICKET_AUTH_CLIENT_ID',
    'TICKET_AI_PROVIDER',
    'TICKET_AI_BASE_URL',
    'TICKET_AI_MODEL',
    'TICKET_AI_API_KEY',
    'TICKET_AI_FALLBACK_TO_LOCAL'
)
foreach ($name in $required) { Require-Setting $name }
Require-Https 'TICKET_AUTH_ISSUER_URI'
Require-Https 'TICKET_AI_BASE_URL'
Require-Database 'TICKET_DB_URL'
if ($values['ACME_EMAIL'] -notmatch '^[^@\s]+@[^@\s]+\.[^@\s]+$') { throw 'Invalid ACME_EMAIL.' }
if (@($values.Keys | Where-Object { $_ -match '^(COMMERCEFLOW_|VITE_COMMERCEFLOW_|PORTFOLIO_)' }).Count) { throw 'Cross-project environment variables are forbidden.' }
if ($values['TICKET_AI_FALLBACK_TO_LOCAL'] -ne 'false') { throw 'TICKET_AI_FALLBACK_TO_LOCAL must be false.' }
if ($values['TICKET_AI_PROVIDER'] -notin @('openai','deepseek','openai-compatible')) { throw 'Unsupported TICKET_AI_PROVIDER.' }
if ($values['TICKET_HOST'] -notmatch '^[A-Za-z0-9.-]+\.[A-Za-z]+$') { throw 'Invalid TICKET_HOST.' }
Write-Host "LOCAL_PASS: staging configuration syntax and isolation checks only; remote connectivity is STAGING_PENDING."
