@echo off
REM 测试 Loading 对话框 - 快速启动脚本

echo ========================================
echo 测试 Loading 对话框和进度条
echo ========================================
echo.

echo [提示] 此测试将展示：
echo   ✓ Loading 对话框
echo   ✓ 进度条动画
echo   ✓ 10 秒延迟效果
echo.

set JAR_FILE=target\git-info-viewer-1.0.0-jar-with-dependencies.jar

if not exist "%JAR_FILE%" (
    echo [错误] JAR 文件不存在
    echo 请先运行: mvn clean package
    pause
    exit /b 1
)

echo ========================================
echo 步骤 1: 启动 Mock Jenkins Server
echo ========================================
echo.
echo 请在新窗口中运行:
echo   start-mock-jenkins.bat
echo.
echo 然后按任意键继续...
pause > nul

echo.
echo ========================================
echo 步骤 2: 启动应用
echo ========================================
echo.
echo 正在启动应用...
java -jar "%JAR_FILE%"

echo.
echo ========================================
echo 测试完成
echo ========================================
echo.
echo 记得停止 Mock Server (Ctrl+C)
pause
