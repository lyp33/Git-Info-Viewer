@echo off
REM 测试收藏工具 - 快捷脚本

if "%1"=="" (
    echo 用法: test-favorites.bat [create^|show^|delete^|test-dialog]
    echo.
    echo 命令:
    echo   create       - 创建测试收藏数据
    echo   show         - 显示当前收藏数据
    echo   delete       - 删除收藏数据文件
    echo   test-dialog  - 测试加载对话框
    echo.
    echo 示例:
    echo   test-favorites.bat create
    exit /b 1
)

set JAR_FILE=target\git-info-viewer-1.0.0-jar-with-dependencies.jar

if not exist "%JAR_FILE%" (
    echo 错误: JAR 文件不存在: %JAR_FILE%
    echo 请先运行: mvn clean package
    exit /b 1
)

echo 执行命令: %1
echo.

java -cp "%JAR_FILE%" com.gitviewer.TestFavoritesUtil %1

echo.
echo 完成!
pause
