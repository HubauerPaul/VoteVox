<#
.SYNOPSIS
    One-click production launcher for VoteVox. Everything runs in Docker.

    Steps it performs automatically:
      1. Make sure Docker Desktop is running (start it & wait, or show a clear
         message if it cannot be started).
      2. Detect this PC's current LAN IP address.
      3. Generate a locally-trusted HTTPS certificate (mkcert) for that IP so the
         phone camera works and there is no security warning once the root CA is
         installed on the phone.
      4. Build and start all containers (postgres, backend, voting UI, admin UI).
      5. Open the admin UI in the browser and print the QR / phone URLs.

    Designed to be launched by a non-technical user via "VoteVox starten.bat".
#>
[CmdletBinding()]
param(
    [switch]$NoBrowser
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$compose = Join-Path $root 'docker-compose.prod.yml'

function Write-Step($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "    $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "    $msg" -ForegroundColor Yellow }
function Write-Err($msg)  { Write-Host "    $msg" -ForegroundColor Red }

# --- 1. Docker ----------------------------------------------------------------
function Test-DockerReady {
    try { docker info *> $null; return ($LASTEXITCODE -eq 0) } catch { return $false }
}

Write-Step "Checking Docker..."
if (-not (Test-DockerReady)) {
    Write-Warn "Docker is not running. Trying to start Docker Desktop..."
    $dockerExe = @(
        "$env:ProgramFiles\Docker\Docker\Docker Desktop.exe",
        "${env:ProgramFiles(x86)}\Docker\Docker\Docker Desktop.exe"
    ) | Where-Object { Test-Path $_ } | Select-Object -First 1

    if (-not $dockerExe) {
        Write-Err "Docker Desktop is not installed (or not in the usual location)."
        Write-Err "Please install Docker Desktop from https://www.docker.com/products/docker-desktop"
        Write-Err "and start it once, then run this again."
        Read-Host "`nPress Enter to close"
        exit 1
    }

    Start-Process $dockerExe | Out-Null
    Write-Warn "Waiting for Docker to start (this can take a minute the first time)..."
    $ready = $false
    for ($i = 0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 2
        if (Test-DockerReady) { $ready = $true; break }
    }
    if (-not $ready) {
        Write-Err "Docker did not become ready in time."
        Write-Err "Please open Docker Desktop manually, wait until it says 'Running',"
        Write-Err "then start VoteVox again."
        Read-Host "`nPress Enter to close"
        exit 1
    }
}
Write-Ok "Docker is running."

# --- 2. Detect LAN IP ---------------------------------------------------------
function Get-LanIp {
    # Prefer the IPv4 of the adapter that owns the default route (the one the
    # phone will be able to reach over WiFi).
    $cfg = Get-NetIPConfiguration -ErrorAction SilentlyContinue |
        Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' } |
        Select-Object -First 1
    if ($cfg -and $cfg.IPv4Address) { return $cfg.IPv4Address.IPAddress }

    # Fallback: first private, non-loopback, non-APIPA IPv4.
    $ip = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
        Where-Object { $_.IPAddress -notlike '169.254.*' -and $_.IPAddress -ne '127.0.0.1' } |
        Where-Object { $_.IPAddress -like '192.168.*' -or $_.IPAddress -like '10.*' -or $_.IPAddress -like '172.*' } |
        Select-Object -First 1
    if ($ip) { return $ip.IPAddress }
    return $null
}

Write-Step "Detecting this PC's network address..."
$ip = Get-LanIp
if (-not $ip) {
    Write-Warn "Could not detect a LAN IP - falling back to localhost."
    Write-Warn "QR codes will only work on this PC, not on phones."
    $ip = 'localhost'
}
Write-Ok "Using address: $ip"

# --- 3. HTTPS certificate (mkcert) -------------------------------------------
Write-Step "Preparing the HTTPS certificate..."
$certDir = Join-Path $root 'certs'
if (-not (Test-Path $certDir)) { New-Item -ItemType Directory -Path $certDir | Out-Null }

$mkcert = @(
    "$env:USERPROFILE\.vite-plugin-mkcert\mkcert.exe",
    (Get-Command mkcert -ErrorAction SilentlyContinue).Source
) | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1

$certFile = Join-Path $certDir 'votevox.pem'
$keyFile  = Join-Path $certDir 'votevox-key.pem'

if ($mkcert) {
    # Preferred: mkcert signs with the (Windows-trusted) root CA -> no warning.
    $env:CAROOT = Split-Path $mkcert -Parent
    $hosts = @('localhost', '127.0.0.1')
    if ($ip -ne 'localhost') { $hosts += $ip }
    & $mkcert -cert-file $certFile -key-file $keyFile @hosts 2>&1 | Out-Null
    if (Test-Path $certFile) {
        Write-Ok "Trusted certificate ready (covers: $($hosts -join ', '))."
        # Copy the root CA next to the project for installing on phones.
        $rootCa = Join-Path $env:CAROOT 'rootCA.pem'
        if (Test-Path $rootCa) {
            Copy-Item $rootCa (Join-Path $root 'votevox-rootCA.pem') -Force
        }
    }
}

if (-not (Test-Path $certFile)) {
    # Fallback (e.g. a fresh PC without mkcert): make a self-signed certificate
    # with OpenSSL inside a tiny container. Docker is already running, so this
    # needs no extra tools installed on the machine.
    Write-Warn "mkcert not available - creating a self-signed certificate via Docker."
    Write-Warn "Phones will show a one-time 'not secure' warning that can be accepted."
    $san = 'subjectAltName=DNS:localhost,IP:127.0.0.1'
    if ($ip -ne 'localhost') { $san += ",IP:$ip" }
    docker run --rm -v "${certDir}:/out" alpine/openssl req -x509 -nodes -newkey rsa:2048 `
        -keyout /out/votevox-key.pem -out /out/votevox.pem -days 825 `
        -subj '/CN=VoteVox' -addext $san 2>&1 | Out-Null
    if (Test-Path $certFile) { Write-Ok "Self-signed certificate ready." }
}

if (-not (Test-Path $certFile)) {
    Write-Err "Could not create an HTTPS certificate - the web servers cannot start."
    Read-Host "`nPress Enter to close"
    exit 1
}

# --- 4. Build & start the stack ----------------------------------------------
$env:QR_BASE_URL    = if ($ip -eq 'localhost') { 'https://localhost:5173/vote' } else { "https://${ip}:5173/vote" }
$env:VOTEVOX_LAN_IP = $ip

Write-Step "Building and starting VoteVox (first run downloads a lot - be patient)..."
docker compose -f $compose up -d --build
if ($LASTEXITCODE -ne 0) {
    Write-Err "Docker failed to start the stack. See the messages above."
    Read-Host "`nPress Enter to close"
    exit 1
}

# --- 5. Wait until the admin UI answers, then open the browser ---------------
Write-Step "Waiting for VoteVox to come up..."
$up = $false
for ($i = 0; $i -lt 60; $i++) {
    try {
        Invoke-WebRequest 'https://localhost:5174/' -SkipCertificateCheck -UseBasicParsing -TimeoutSec 3 | Out-Null
        $up = $true; break
    } catch { Start-Sleep -Seconds 2 }
}
if ($up) { Write-Ok "VoteVox is up." }
else     { Write-Warn "Still starting - the backend may need another moment to finish booting." }

if (-not $NoBrowser) { Start-Process 'https://localhost:5174/' | Out-Null }

# --- Summary ------------------------------------------------------------------
Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  VoteVox is running" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Admin (this PC):  https://localhost:5174"            -ForegroundColor White
Write-Host "  Login:            admin@votevox.at / Admin1234!"     -ForegroundColor White
Write-Host ""
Write-Host "  Voters scan the printed QR codes, which open:"        -ForegroundColor White
Write-Host "  Voting (phones):  $($env:QR_BASE_URL)"                -ForegroundColor White
Write-Host ""
Write-Host "  Phones must be on the SAME WiFi as this PC." -ForegroundColor Yellow
Write-Host "  To remove the certificate warning on a phone, install" -ForegroundColor Yellow
Write-Host "  votevox-rootCA.pem (in this folder) on the phone once." -ForegroundColor Yellow
Write-Host ""
Write-Host "  Stop everything with:  VoteVox stoppen.bat" -ForegroundColor DarkGray
Write-Host ""
Read-Host "Press Enter to close this window (VoteVox keeps running)"
