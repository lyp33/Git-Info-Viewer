@echo off
title Git Info Viewer
echo Starting Git Info Viewer...
echo.

REM 检查Java是否安装
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 8 or higher and try again
    echo.
    pause
    exit /b 1
)

REM 检查jar文件是否存在
if not exist "git-info-viewer-1.0.0-jar-with-dependencies.jar" (
    echo ERROR: git-info-viewer-1.0.0-jar-with-dependencies.jar not found
    echo Please make sure the jar file is in the same directory as this bat file
    echo.
    pause
    exit /b 1
)

REM 启动应用程序（后台运行）
echo Starting application in background...
start "" javaw -jar git-info-viewer-1.0.0-jar-with-dependencies.jar

echo.
echo Application started successfully!
echo The Git Info Viewer window should appear shortly.
echo You can close this command window now.
echo.
timeout /t 3 >nul