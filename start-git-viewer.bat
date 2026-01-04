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

REM 启动应用程序
echo Starting application...
java -jar git-info-viewer-1.0.0-jar-with-dependencies.jar

REM 如果应用程序异常退出，显示错误信息
if %errorlevel% neq 0 (
    echo.
    echo Application exited with error code: %errorlevel%
    echo.
    pause
)