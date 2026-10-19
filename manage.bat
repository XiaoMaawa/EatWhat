@echo off
chcp 65001 >nul
setlocal

rem ============================================================
rem  EatWhat launcher (double-click to run)
rem  Usage:
rem    manage.bat                show status
rem    manage.bat start          start all services
rem    manage.bat stop           stop all services
rem    manage.bat restart        restart
rem    manage.bat status         show status
rem    manage.bat start -Rebuild rebuild backend then start
rem ============================================================

cd /d "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0manage.ps1" %*

echo.
if "%~1"=="" pause
