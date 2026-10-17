@echo off
setlocal
title Kirby Jad - offline preview
rem Renders every Kirby and Waddle Dee animation to PNG contact sheets in build\preview
rem (no client, no login) and opens the folder.
cd /d "%~dp0"
call gradlew.bat --no-daemon -q renderPreview
if errorlevel 1 (
    echo [X] Render failed - see above.
    pause
    exit /b 1
)
start "" "%~dp0build\preview"
