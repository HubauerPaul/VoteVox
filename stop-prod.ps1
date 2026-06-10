<#
.SYNOPSIS
    Stops the VoteVox production stack (all Docker containers). The database
    contents are preserved (the postgres volume is kept). Add -Wipe to also
    delete all data and start fresh next time.
#>
[CmdletBinding()]
param(
    [switch]$Wipe
)
$root = $PSScriptRoot
$compose = Join-Path $root 'docker-compose.prod.yml'

Write-Host "==> Stopping VoteVox..." -ForegroundColor Cyan
if ($Wipe) {
    docker compose -f $compose down -v
    Write-Host "    Stopped and ALL DATA WIPED." -ForegroundColor Yellow
} else {
    docker compose -f $compose down
    Write-Host "    Stopped. Data is preserved." -ForegroundColor Green
}
Read-Host "Press Enter to close"
