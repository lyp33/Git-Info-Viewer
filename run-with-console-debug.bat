@echo off
echo ========================================
echo Starting Git Info Viewer with Console Debug
echo ========================================
echo.
echo This window will show all console output including debug logs.
echo Please keep this window open while using the application.
echo.
echo ========================================
echo.

java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar

echo.
echo ========================================
echo Application closed.
echo Press any key to exit...
pause >nul
