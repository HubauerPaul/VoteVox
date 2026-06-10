<#
.SYNOPSIS
    Builds a clean .zip of the VoteVox project for sending to someone else.

    It copies the source WITHOUT the large/generated/machine-specific folders
    (node_modules, build output, certificates, .git, IDE folders, logs) and zips
    the result. The recipient only needs Docker Desktop and a double-click on
    "VoteVox starten.bat".
#>
[CmdletBinding()]
param(
    # Where to put the .zip (default: the folder ABOVE the project).
    [string]$OutDir = (Split-Path $PSScriptRoot -Parent)
)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$stamp = Get-Date -Format 'yyyy-MM-dd'
$stage = Join-Path $env:TEMP "VoteVox-package-$stamp"
$zip   = Join-Path $OutDir "VoteVox-$stamp.zip"

Write-Host "==> Packaging VoteVox for distribution..." -ForegroundColor Cyan

if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
if (Test-Path $zip)   { Remove-Item $zip -Force }

# Copy everything except generated / heavy / machine-specific content.
# /XD = exclude directories (matched by name at any depth), /XF = exclude files.
robocopy $root $stage /E `
    /XD node_modules target dist .git .idea logs certs `
    /XF *.log votevox-rootCA.pem `
    /NFL /NDL /NJH /NJS /NP | Out-Null
# robocopy exit codes 0-7 are success; 8+ is an error.
if ($LASTEXITCODE -ge 8) { throw "robocopy failed (code $LASTEXITCODE)" }

Compress-Archive -Path (Join-Path $stage '*') -DestinationPath $zip -Force
Remove-Item $stage -Recurse -Force

$sizeMB = [math]::Round((Get-Item $zip).Length / 1MB, 1)
Write-Host ""
Write-Host "Done. Created:" -ForegroundColor Green
Write-Host "   $zip  ($sizeMB MB)" -ForegroundColor White
Write-Host ""
Write-Host "Send that single .zip. The recipient:" -ForegroundColor Cyan
Write-Host "   1. installs Docker Desktop (and starts it once)," -ForegroundColor White
Write-Host "   2. unzips the file," -ForegroundColor White
Write-Host "   3. double-clicks 'VoteVox starten.bat'." -ForegroundColor White
Write-Host ""
Read-Host "Press Enter to close"
