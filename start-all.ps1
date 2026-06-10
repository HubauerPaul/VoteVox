<#
.SYNOPSIS
    Starts the VoteVox stack in DEVELOPMENT mode (hot-reloading dev servers):
      1. PostgreSQL + pgAdmin (Docker Compose)
      2. Spring Boot backend            -> http://localhost:8080
      3. Voting UI   (student frontend) -> https://localhost:5173
      4. Admin UI    (admin frontend)   -> https://localhost:5174

    The two UIs serve HTTPS via mkcert. Each service runs in its own terminal
    window. Run .\stop-all.ps1 to tear everything down.

    QR codes point at this PC's LAN IP (auto-detected) so phones on the SAME
    WiFi can vote: https://<this-pc-ip>:5173/vote

    For a packaged, non-developer "one click" run, use the production launcher
    instead: "VoteVox starten.bat" (everything in Docker).

.PARAMETER SkipInstall
    Skip "npm install" for the frontends (use when deps are already installed).
#>
[CmdletBinding()]
param(
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$psHost = if (Get-Command pwsh -ErrorAction SilentlyContinue) { 'pwsh' } else { 'powershell' }

function Start-Service-Window {
    param([string]$Title, [string]$WorkingDir, [string]$Command)
    $inner = "`$Host.UI.RawUI.WindowTitle = '$Title'; Set-Location '$WorkingDir'; $Command"
    Start-Process $psHost -ArgumentList '-NoExit', '-Command', $inner | Out-Null
}

function Get-LanIp {
    $cfg = Get-NetIPConfiguration -ErrorAction SilentlyContinue |
        Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' } |
        Select-Object -First 1
    if ($cfg -and $cfg.IPv4Address) { return $cfg.IPv4Address.IPAddress }
    $ip = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object { $_.IPAddress -like '192.168.*' -or $_.IPAddress -like '10.*' -or $_.IPAddress -like '172.*' } |
        Select-Object -First 1
    if ($ip) { return $ip.IPAddress }
    return 'localhost'
}

Write-Host "=== VoteVox: starting DEV stack ===" -ForegroundColor Cyan

# --- 1. Load .env (so the backend gets JWT_SECRET, DB creds, etc.) ------------
$envFile = Join-Path $root '.env'
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith('#') -and $line.Contains('=')) {
            $key, $value = $line.Split('=', 2)
            Set-Item -Path "Env:$($key.Trim())" -Value $value.Trim()
        }
    }
    Write-Host "[env]    loaded .env" -ForegroundColor DarkGray
}

# --- 2. Detect LAN IP -> QR codes + dev cert -------------------------------
$ip = Get-LanIp
$env:VOTEVOX_LAN_IP = $ip   # vite.config picks this up so the cert covers the IP
$qrBase = if ($ip -eq 'localhost') { 'https://localhost:5173/vote' } else { "https://${ip}:5173/vote" }
@"
# Auto-written by start-all.ps1 - QR codes point at this PC's LAN IP.
VITE_QR_BASE_URL=$qrBase
"@ | Set-Content -Path (Join-Path $root 'admin-ui\.env') -Encoding utf8
Write-Host "[net]    QR base URL: $qrBase" -ForegroundColor Green

# --- 3. Docker: Postgres + pgAdmin -------------------------------------------
Write-Host "[docker] starting postgres + pgadmin..." -ForegroundColor Green
docker compose -f (Join-Path $root 'docker-compose.yml') up -d | Out-Null

Write-Host "[docker] waiting for postgres to be healthy..." -ForegroundColor Green
$healthy = $false
for ($i = 0; $i -lt 30; $i++) {
    $status = docker inspect --format '{{.State.Health.Status}}' votevox-postgres-1 2>$null
    if ($status -eq 'healthy') { $healthy = $true; break }
    Start-Sleep -Seconds 2
}
if ($healthy) { Write-Host "[docker] postgres is healthy." -ForegroundColor Green }
else { Write-Host "[docker] WARNING: postgres not healthy yet - the backend may retry." -ForegroundColor Yellow }

# --- 4. Frontend deps (optional) ---------------------------------------------
if (-not $SkipInstall) {
    foreach ($ui in @('voting-ui', 'admin-ui')) {
        $uiPath = Join-Path $root $ui
        if (-not (Test-Path (Join-Path $uiPath 'node_modules'))) {
            Write-Host "[npm]    installing $ui dependencies..." -ForegroundColor Green
            Push-Location $uiPath; npm install | Out-Null; Pop-Location
        }
    }
}

# --- 5. Backend ---------------------------------------------------------------
Write-Host "[backend] launching Spring Boot (window)..." -ForegroundColor Green
$jwt = if ($env:JWT_SECRET) { $env:JWT_SECRET } else { 'change-me-in-production-must-be-at-least-32-chars-long' }
Start-Service-Window -Title 'VoteVox Backend (8080)' -WorkingDir (Join-Path $root 'backend') `
    -Command "`$env:JWT_SECRET='$jwt'; .\mvnw.cmd spring-boot:run"

# --- 6. Voting UI -------------------------------------------------------------
Write-Host "[voting]  launching Voting UI (window)..." -ForegroundColor Green
Start-Service-Window -Title 'VoteVox Voting UI (5173)' -WorkingDir (Join-Path $root 'voting-ui') `
    -Command "`$env:VOTEVOX_LAN_IP='$ip'; npm run dev"

# --- 7. Admin UI --------------------------------------------------------------
Write-Host "[admin]   launching Admin UI (window)..." -ForegroundColor Green
Start-Service-Window -Title 'VoteVox Admin UI (5174)' -WorkingDir (Join-Path $root 'admin-ui') `
    -Command "`$env:VOTEVOX_LAN_IP='$ip'; npm run dev"

Write-Host ""
Write-Host "=== VoteVox (DEV) is starting up ===" -ForegroundColor Cyan
Write-Host "  Backend     http://localhost:8080" -ForegroundColor White
Write-Host "  Voting UI   https://localhost:5173   (LAN: $qrBase)" -ForegroundColor White
Write-Host "  Admin UI    https://localhost:5174   (admin@votevox.at / Admin1234!)" -ForegroundColor White
Write-Host "  pgAdmin     http://localhost:5050" -ForegroundColor White
Write-Host ""
Write-Host "Phones must be on the SAME WiFi. Run .\stop-all.ps1 to stop everything." -ForegroundColor DarkGray
