@echo off
echo ========================================
echo 验证新版本
echo ========================================
echo.

echo 1. 检查 JAR 文件时间戳
dir target\git-info-viewer-1.0.0-jar-with-dependencies.jar | findstr "2026"
echo.

echo 2. 检查编译的 class 文件时间戳
dir target\classes\com\gitviewer\JenkinsJobDetailsDialog.class | findstr "2026"
echo.

echo 3. 关闭所有 Java 进程
taskkill /F /IM java.exe >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo ✓ 已关闭 Java 进程
) else (
    echo ✓ 没有运行中的 Java 进程
)
echo.

echo 4. 等待 2 秒...
timeout /t 2 /nobreak >nul
echo.

echo 5. 启动新版本
echo 正在启动应用程序...
start "Git Info Viewer" java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
echo.

echo ========================================
echo 新版本已启动！
echo ========================================
echo.
echo 请测试：
echo 1. 打开 Jenkins Browser
echo 2. 双击任意 Job
echo 3. 查看是否还有错误弹窗
echo 4. 检查 Console Log 是否显示错误信息
echo.
pause
