@echo off
echo Closing existing Java applications...
taskkill /F /IM java.exe /T 2>nul
timeout /t 2 /nobreak >nul

echo Starting Git Info Viewer...
start "Git Info Viewer" java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar

echo Application started!
