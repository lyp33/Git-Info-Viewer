@echo off
echo ========================================
echo Closing Java Applications
echo ========================================
echo.
echo Current Java processes:
echo.

REM List all Java processes
for /f "tokens=2" %%i in ('tasklist /FI "IMAGENAME eq java.exe" /NH') do (
    echo Process ID: %%i
)

echo.
echo Attempting to close all Java processes...
echo.

REM Kill all Java processes
taskkill /F /IM java.exe >nul 2>&1

if %ERRORLEVEL% EQU 0 (
    echo ✓ All Java processes closed successfully
) else (
    echo ✗ No Java processes found or failed to close
)

echo.
echo You can now rebuild the application with: mvn clean package
echo.
pause
