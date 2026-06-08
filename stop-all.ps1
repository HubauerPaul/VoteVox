<#
.SYNOPSIS
    Stops everything start-all.ps1 launched: the Vite dev servers, the Spring
    Boot backend, and the Docker containers.

.PARAMETER KeepDocker
    Leave the Postgres/pgAdmin containers running (only stop the app processes).
#>
[CmdletBinding()]
param(
    [switch]$KeepDocker
)

$root = $PSScriptRoot
Write-Host "=== VoteVox: stopping full stack ===" -ForegroundColor Cyan

# --- Kill whatever is listening on our dev ports -----------------------------
$ports = @(8080, 5173, 5174)
foreach ($port in $ports) {
    $conns = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
    if ($conns) {
        $conns.OwningProcess | Sort-Object -Unique | ForEach-Object {
            try {
                Stop-Process -Id $_ -Force -ErrorAction Stop
                Write-Host "[kill]   stopped process on port $port (PID $_)" -ForegroundColor Green
            } catch {
                Write-Host "[kill]   could not stop PID $_ on port $port" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "[kill]   nothing listening on port $port" -ForegroundColor DarkGray
    }
}

# --- Stop the Maven/Spring child JVMs (they may outlive the port) -------------
Get-CimInstance Win32_Process -Filter "Name='java.exe'" -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -and ($_.CommandLine -like '*votevox*' -or $_.CommandLine -like '*spring-boot*') } |
    ForEach-Object {
        try { Stop-Process -Id $_.ProcessId -Force -ErrorAction Stop; Write-Host "[kill]   stopped backend JVM (PID $($_.ProcessId))" -ForegroundColor Green } catch {}
    }

# --- Stop the Cloudflare tunnel (if running) ---------------------------------
$cfProcs = Get-Process cloudflared -ErrorAction SilentlyContinue
if ($cfProcs) {
    $cfProcs | ForEach-Object {
        try { Stop-Process -Id $_.Id -Force -ErrorAction Stop; Write-Host "[kill]   stopped cloudflared tunnel (PID $($_.Id))" -ForegroundColor Green } catch {}
    }
} else {
    Write-Host "[kill]   no cloudflared tunnel running" -ForegroundColor DarkGray
}

# --- Docker ------------------------------------------------------------------
if ($KeepDocker) {
    Write-Host "[docker] leaving containers running (-KeepDocker)." -ForegroundColor DarkGray
} else {
    Write-Host "[docker] stopping containers..." -ForegroundColor Green
    docker compose -f (Join-Path $root 'docker-compose.yml') down | Out-Null
}

Write-Host "=== VoteVox stopped ===" -ForegroundColor Cyan
