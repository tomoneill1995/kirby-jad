@echo off
setlocal
title Kirby Jad - RuneLite test client
rem Launches the locally built RuneLite client (CursorRepos\runelite) with the Kirby Jad
rem plugin loaded.
cd /d "%~dp0"

rem Clear global Java options that can force headless mode.
set "JAVA_TOOL_OPTIONS="
set "_JAVA_OPTIONS="
set "JDK_JAVA_OPTIONS="

call gradlew.bat --no-daemon runPlugin
if errorlevel 1 (
    echo.
    echo [X] The client exited with an error - see above.
    echo     If the RuneLite jars are missing, build them first:
    echo       cd ..\runelite ^&^& gradlew.bat :client:publishToMavenLocal
    pause
)
