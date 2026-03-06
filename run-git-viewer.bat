@echo off
REM Git Info Viewer - Windows Launcher
REM Uses javaw (no console window) with custom process name

REM Set application name for task manager
set APP_NAME=Git Info Viewer

REM Java options
set JAVA_OPTS=-Xms256m -Xmx1024m

REM Launch with javaw (no console window)
start "Git Info Viewer" javaw %JAVA_OPTS% -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
