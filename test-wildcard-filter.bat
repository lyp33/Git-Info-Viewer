@echo off
echo ========================================
echo Testing Wildcard Filter Feature
echo ========================================
echo.
echo This script will:
echo 1. Check if the latest JAR exists
echo 2. Show the JAR file timestamp
echo 3. Run the application
echo.

set JAR_FILE=target\git-info-viewer-1.0.0-jar-with-dependencies.jar

if not exist "%JAR_FILE%" (
    echo ERROR: JAR file not found!
    echo Please run: mvn package
    pause
    exit /b 1
)

echo JAR file found: %JAR_FILE%
echo.
echo File timestamp:
dir "%JAR_FILE%" | findstr "git-info-viewer"
echo.
echo ========================================
echo Starting application...
echo ========================================
echo.
echo Test Steps:
echo 1. Open Build Package dialog
echo 2. In the left filter box, type: thailife-*-bff
echo 3. You should only see applications matching that pattern
echo 4. Drag one application to the right side
echo 5. The filter should still be active (only matching apps shown)
echo.
echo Press any key to start the application...
pause > nul

java -jar "%JAR_FILE%"
