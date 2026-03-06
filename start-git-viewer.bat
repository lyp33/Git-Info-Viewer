@echo off
REM Git Info Viewer Launcher
REM This script sets a custom window title for better identification

title Git Info Viewer

REM Set Java options for better display name
set JAVA_OPTS=-Xms256m -Xmx1024m

REM Launch the application
java %JAVA_OPTS% -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar

REM Keep window open if there's an error
if errorlevel 1 (
    echo.
    echo Application exited with error code: %errorlevel%
    pause
)
