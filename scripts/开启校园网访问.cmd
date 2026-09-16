@echo off
chcp 65001 >nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0campus-access.ps1" -Action Start
echo.
pause

