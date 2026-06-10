@echo off
REM ============================================================
REM  VoteVox - Start (one click)
REM  Double-click this file to build and start the whole system.
REM ============================================================
title VoteVox starten
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0start-prod.ps1"
