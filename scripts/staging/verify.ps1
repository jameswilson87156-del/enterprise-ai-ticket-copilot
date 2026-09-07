[CmdletBinding()]
param([string]$BaseUrl = 'http://127.0.0.1:8087')

$ErrorActionPreference = 'Stop'
$health = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/health" -TimeoutSec 10
if ($health.StatusCode -ne 200) { throw 'edge health check failed' }
$apiHealth = Invoke-RestMethod -Uri "$BaseUrl/api/health" -TimeoutSec 10
if ($apiHealth.status -ne 'UP') { throw 'ticket API health check failed' }
Write-Host "STAGING_TICKET_VERIFY_OK edge=$($health.StatusCode) api=$($apiHealth.status)"
