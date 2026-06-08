<#
.SYNOPSIS
    Starts the complete VoteVox stack for local development:
      1. PostgreSQL + pgAdmin (Docker Compose)
      2. Spring Boot backend            -> http://localhost:8080
      3. Voting UI   (student frontend) -> https://localhost:5173
      4. Admin UI    (admin frontend)   -> https://localhost:5174
      5. (optional) Cloudflare tunnel for the voting UI so voters can be on ANY
         network (mobile data / other WiFi), via -Tunnel.

    The two UIs serve HTTPS via mkcert. Each service runs in its own terminal
    window. Run .\stop-all.ps1 to tear everything down.

.PARAMETER SkipInstall
    Skip "npm install" for the frontends (use when deps are already installed).

.PARAMETER Tunnel
    Also open a Cloudflare quick tunnel to the voting UI and write its public
    URL into admin-ui\.env (VITE_QR_BASE_URL) so generated QR codes point at it.
    The tunnel URL is random per run; regenerate the token PDF after starting.

.EXAMPLE
    .\start-all.ps1
    .\start-all.ps1 -Tunnel
    .\start-all.ps1 -SkipInstall -Tunnel
#>
[CmdletBinding()]
param(
    [switch]$SkipInstall,
    [switch]$Tunnel
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$psHost = if (Get-Command pwsh -ErrorAction SilentlyContinue) { 'pwsh' } else { 'powershell' }

function Start-Service-Window {
    param([string]$Title, [string]$WorkingDir, [string]$Command)
    $inner = "`$Host.UI.RawUI.WindowTitle = '$Title'; Set-Location '$WorkingDir'; $Command"
    Start-Process $psHost -ArgumentList '-NoExit', '-Command', $inner | Out-Null
}

function Wait-ForHttps {
    param([int]$Port, [int]$Tries = 30)
    for ($i = 0; $i -lt $Tries; $i++) {
        try {
            Invoke-WebRequest "https://localhost:$Port/" -SkipCertificateCheck -UseBasicParsing -TimeoutSec 4 | Out-Null
            return $true
        } catch { Start-Sleep -Seconds 1 }
    }
    return $false
}

Write-Host "=== VoteVox: starting full stack ===" -ForegroundColor Cyan

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

# --- 2. Docker: Postgres + pgAdmin -------------------------------------------
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

# --- 3. Frontend deps (optional) ---------------------------------------------
if (-not $SkipInstall) {
    foreach ($ui in @('voting-ui', 'admin-ui')) {
        $uiPath = Join-Path $root $ui
        if (-not (Test-Path (Join-Path $uiPath 'node_modules'))) {
            Write-Host "[npm]    installing $ui dependencies..." -ForegroundColor Green
            Push-Location $uiPath; npm install | Out-Null; Pop-Location
        }
    }
}

# --- 4. Backend ---------------------------------------------------------------
Write-Host "[backend] launching Spring Boot (window)..." -ForegroundColor Green
$jwt = if ($env:JWT_SECRET) { $env:JWT_SECRET } else { 'change-me-in-production-must-be-at-least-32-chars-long' }
Start-Service-Window -Title 'VoteVox Backend (8080)' -WorkingDir (Join-Path $root 'backend') `
    -Command "`$env:JWT_SECRET='$jwt'; .\mvnw.cmd spring-boot:run"

# --- 5. Voting UI -------------------------------------------------------------
Write-Host "[voting]  launching Voting UI (window)..." -ForegroundColor Green
Start-Service-Window -Title 'VoteVox Voting UI (5173)' -WorkingDir (Join-Path $root 'voting-ui') `
    -Command 'npm run dev'

# --- 6. Optional Cloudflare tunnel (must run before admin UI so .env is set) --
if ($Tunnel) {
    Write-Host "[tunnel]  waiting for voting UI to come up..." -ForegroundColor Green
    if (-not (Wait-ForHttps -Port 5173 -Tries 40)) {
        Write-Host "[tunnel]  voting UI not reachable yet - skipping tunnel." -ForegroundColor Yellow
    } else {
        $cf = (Get-Command cloudflared -ErrorAction SilentlyContinue).Source
        if (-not $cf) {
            foreach ($p in @("C:\Program Files (x86)\cloudflared\cloudflared.exe",
                             "C:\Program Files\cloudflared\cloudflared.exe")) {
                if (Test-Path $p) { $cf = $p; break }
            }
        }
        if (-not $cf) {
            Write-Host "[tunnel]  cloudflared not found. Install: winget install Cloudflare.cloudflared" -ForegroundColor Yellow
        } else {
            $log = Join-Path $root 'cloudflared.log'
            if (Test-Path $log) { Remove-Item $log -Force }
            $tunnelCmd = "`$Host.UI.RawUI.WindowTitle='VoteVox Cloudflare Tunnel'; " +
                         "& `"$cf`" tunnel --url https://localhost:5173 --no-tls-verify 2>&1 | Tee-Object -FilePath `"$log`""
            Start-Process $psHost -ArgumentList '-NoExit', '-Command', $tunnelCmd | Out-Null

            Write-Host "[tunnel]  waiting for public URL..." -ForegroundColor Green
            $url = $null
            for ($i = 0; $i -lt 40; $i++) {
                Start-Sleep -Milliseconds 750
                $m = Select-String -Path $log -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' -ErrorAction SilentlyContinue | Select-Object -First 1
                if ($m) { $url = $m.Matches[0].Value; break }
            }
            if ($url) {
                @"
# Auto-written by start-all.ps1 -Tunnel - valid only while the tunnel runs.
VITE_QR_BASE_URL=$url/vote
"@ | Set-Content -Path (Join-Path $root 'admin-ui\.env') -Encoding utf8
                Write-Host "[tunnel]  live: $url  (written to admin-ui\.env)" -ForegroundColor Green
                Write-Host "[tunnel]  remember to regenerate the token PDF after the admin UI is up." -ForegroundColor Yellow
            } else {
                Write-Host "[tunnel]  could not detect tunnel URL - check cloudflared.log" -ForegroundColor Yellow
            }
        }
    }
}

# --- 7. Admin UI (last, so it picks up a tunnel-written .env) -----------------
Write-Host "[admin]   launching Admin UI (window)..." -ForegroundColor Green
Start-Service-Window -Title 'VoteVox Admin UI (5174)' -WorkingDir (Join-Path $root 'admin-ui') `
    -Command 'npm run dev'

Write-Host ""
Write-Host "=== VoteVox is starting up ===" -ForegroundColor Cyan
Write-Host "  Backend     http://localhost:8080" -ForegroundColor White
Write-Host "  Voting UI   https://localhost:5173   (LAN: https://<this-pc-ip>:5173)" -ForegroundColor White
Write-Host "  Admin UI    https://localhost:5174   (admin@votevox.at / Admin1234!)" -ForegroundColor White
Write-Host "  pgAdmin     http://localhost:5050" -ForegroundColor White
if ($Tunnel) { Write-Host "  Tunnel      see the 'Cloudflare Tunnel' window / cloudflared.log" -ForegroundColor White }
Write-Host ""
Write-Host "Backend takes ~10s to compile. Run .\stop-all.ps1 to stop everything." -ForegroundColor DarkGray
