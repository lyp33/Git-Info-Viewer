@echo off
echo ========================================
echo Testing Console Logging
echo ========================================
echo.
echo This will run the application with console output visible.
echo All System.out.println and logger output will be shown here.
echo.
echo Press any key to start...
pause > nul
echo.
echo ========================================
echo Starting Application...
echo ========================================
echo.

java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar

echo.
echo ========================================
echo Application closed.
echo ========================================
pause
