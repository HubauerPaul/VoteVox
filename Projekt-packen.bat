@echo off
REM ============================================================
REM  VoteVox - Build a clean .zip for sending to someone.
REM ============================================================
title VoteVox packen
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0pack-project.ps1"
