@echo off
REM 启动 Mock Jenkins Server

echo ========================================
echo Starting Mock Jenkins Server...
echo ========================================
echo.

set JAR_FILE=target\git-info-viewer-1.0.0-jar-with-dependencies.jar

if not exist "%JAR_FILE%" (
    echo Error: JAR file not found: %JAR_FILE%
    echo Please run: mvn clean package
    pause
    exit /b 1
)

echo Starting server on port 8888...
echo.

java -cp "%JAR_FILE%" com.gitviewer.MockJenkinsServer 8888

pause
