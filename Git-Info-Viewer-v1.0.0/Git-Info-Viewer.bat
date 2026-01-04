@echo off
chcp 65001 >nul
title Git Info Viewer v1.0.0
color 0A

echo.
echo  ╔══════════════════════════════════════════════════════════════╗
echo  ║                    Git Info Viewer v1.0.0                   ║
echo  ║                     Git仓库信息查看工具                      ║
echo  ╚══════════════════════════════════════════════════════════════╝
echo.

REM 检查Java环境
echo [1/3] 检查Java环境...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ 错误: 未找到Java环境
    echo    请安装Java 8或更高版本后重试
    echo    下载地址: https://www.oracle.com/java/technologies/downloads/
    echo.
    pause
    exit /b 1
) else (
    echo ✅ Java环境检查通过
)

REM 检查应用程序文件
echo [2/3] 检查应用程序文件...
if not exist "git-info-viewer-1.0.0-jar-with-dependencies.jar" (
    echo ❌ 错误: 找不到应用程序文件
    echo    请确保 git-info-viewer-1.0.0-jar-with-dependencies.jar 文件存在
    echo.
    pause
    exit /b 1
) else (
    echo ✅ 应用程序文件检查通过
)

REM 启动应用程序
echo [3/3] 启动Git Info Viewer...
echo.
echo 🚀 正在启动应用程序，请稍候...
echo    (首次启动可能需要几秒钟时间)
echo.

java -Dfile.encoding=UTF-8 -jar git-info-viewer-1.0.0-jar-with-dependencies.jar

REM 检查退出状态
if %errorlevel% neq 0 (
    echo.
    echo ❌ 应用程序异常退出 (错误代码: %errorlevel%)
    echo    请检查Java版本或联系技术支持
    echo.
) else (
    echo.
    echo ✅ 应用程序正常退出
    echo.
)

pause