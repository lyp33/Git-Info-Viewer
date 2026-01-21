@echo off
echo ========================================
echo Verifying Logging Configuration
echo ========================================
echo.

echo Step 1: Checking if simplelogger.properties exists in source...
if exist "src\main\resources\simplelogger.properties" (
    echo [OK] Found: src\main\resources\simplelogger.properties
    echo.
    echo Content:
    type src\main\resources\simplelogger.properties
) else (
    echo [ERROR] File not found: src\main\resources\simplelogger.properties
)

echo.
echo ========================================
echo Step 2: Checking if JAR file exists...
if exist "target\git-info-viewer-1.0.0-jar-with-dependencies.jar" (
    echo [OK] Found: target\git-info-viewer-1.0.0-jar-with-dependencies.jar
) else (
    echo [ERROR] JAR file not found. Please run: mvn clean package
)

echo.
echo ========================================
echo Step 3: Instructions
echo ========================================
echo.
echo To see console logging output, you MUST use one of these methods:
echo.
echo   Method 1: run-with-console.bat
echo   Method 2: test-logging.bat
echo   Method 3: java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
echo.
echo DO NOT USE: restart-app.bat (it hides console output)
echo.
echo ========================================
echo.
pause
