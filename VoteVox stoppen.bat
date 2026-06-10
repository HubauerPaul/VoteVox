@echo off
REM ============================================================
REM  VoteVox - Stop
REM  Double-click this file to stop the system (data is kept).
REM ============================================================
title VoteVox stoppen
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0stop-prod.ps1"
