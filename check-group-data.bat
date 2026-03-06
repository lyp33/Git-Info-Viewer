@echo off
echo ========================================
echo Group Data Diagnostic Tool
echo ========================================
echo.

set SETTINGS_FILE=%USERPROFILE%\gitviewer.properties

if not exist "%SETTINGS_FILE%" (
    echo ERROR: Settings file not found!
    echo Expected location: %SETTINGS_FILE%
    echo.
    pause
    exit /b 1
)

echo Settings file found: %SETTINGS_FILE%
echo.
echo ========================================
echo Checking Group Data...
echo ========================================
echo.

findstr /C:"portal.favorite.groups" "%SETTINGS_FILE%"
if errorlevel 1 (
    echo No group data found in settings file.
) else (
    echo Group data found ^(see above^)
)

echo.
echo ========================================
echo Checking Ungrouped Favorites...
echo ========================================
echo.

findstr /C:"portal.ungrouped.favorites" "%SETTINGS_FILE%"
if errorlevel 1 (
    echo No ungrouped favorites found in settings file.
) else (
    echo Ungrouped favorites found ^(see above^)
)

echo.
echo ========================================
echo Full Settings File Content:
echo ========================================
echo.
type "%SETTINGS_FILE%"

echo.
echo ========================================
echo Diagnostic Complete
echo ========================================
pause
