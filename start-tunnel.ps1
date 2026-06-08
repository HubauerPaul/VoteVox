<#
.SYNOPSIS
    Opens a Cloudflare "quick tunnel" to the voting UI so voters can reach it
    from ANY network (mobile data, other WiFi) over a trusted-HTTPS URL with no
    certificate warning.

    It captures the random https://<...>.trycloudflare.com URL, writes it into
    admin-ui\.env as VITE_QR_BASE_URL (so generated QR codes point at the
    tunnel), and keeps the tunnel running in this window. Press Ctrl+C to stop.

.NOTES
    - Requires the voting UI (port 5173) and the backend to be running already
      (use .\start-all.ps1 first).
    - The URL is NEW every run. After this script prints it you must:
        1) restart the admin UI so it picks up the new .env, then
        2) regenerate the token QR PDF.
#>
[CmdletBinding()]
param()

$root = $PSScriptRoot
$ErrorActionPreference = 'Stop'

# Locate cloudflared.
$cf = (Get-Command cloudflared -ErrorAction SilentlyContinue).Source
if (-not $cf) {
    foreach ($p in @(
        "C:\Program Files (x86)\cloudflared\cloudflared.exe",
        "C:\Program Files\cloudflared\cloudflared.exe")) {
        if (Test-Path $p) { $cf = $p; break }
    }
}
if (-not $cf) {
    Write-Host "cloudflared not found. Install it with:  winget install Cloudflare.cloudflared" -ForegroundColor Red
    exit 1
}

$log = Join-Path $root 'cloudflared.log'
if (Test-Path $log) { Remove-Item $log -Force }

Write-Host "Starting Cloudflare tunnel to https://localhost:5173 ..." -ForegroundColor Cyan
$proc = Start-Process -FilePath $cf `
    -ArgumentList 'tunnel', '--url', 'https://localhost:5173', '--no-tls-verify' `
    -RedirectStandardError $log -RedirectStandardOutput "$log.out" -NoNewWindow -PassThru

# Wait for the public URL to appear in the log.
$url = $null
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Milliseconds 750
    $m = Select-String -Path $log -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' -ErrorAction SilentlyContinue |
         Select-Object -First 1
    if ($m) { $url = $m.Matches[0].Value; break }
}

if (-not $url) {
    Write-Host "Could not detect the tunnel URL. Check $log" -ForegroundColor Red
    if (-not $proc.HasExited) { $proc.Kill() }
    exit 1
}

# Write the QR base URL into admin-ui\.env.
$envPath = Join-Path $root 'admin-ui\.env'
@"
# Auto-written by start-tunnel.ps1 - valid only while this tunnel runs.
VITE_QR_BASE_URL=$url/vote
"@ | Set-Content -Path $envPath -Encoding utf8

Write-Host ""
Write-Host "=== Cloudflare tunnel is live ===" -ForegroundColor Green
Write-Host "  Voting URL : $url" -ForegroundColor White
Write-Host "  Wrote VITE_QR_BASE_URL=$url/vote to admin-ui\.env" -ForegroundColor DarkGray
Write-Host ""
Write-Host "NEXT: 1) restart the Admin UI (so it reloads .env)" -ForegroundColor Yellow
Write-Host "      2) regenerate the token QR PDF (codes will now point at the tunnel)" -ForegroundColor Yellow
Write-Host ""
Write-Host "Keep this window open during the vote. Press Ctrl+C to stop the tunnel." -ForegroundColor DarkGray

Wait-Process -Id $proc.Id
