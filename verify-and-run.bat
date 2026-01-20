@echo off
echo ========================================
echo 清理旧的编译文件...
echo ========================================
call mvn clean

echo.
echo ========================================
echo 编译最新代码...
echo ========================================
call mvn package

echo.
echo ========================================
echo 检查JAR文件...
echo ========================================
dir target\git-info-viewer-1.0.0-jar-with-dependencies.jar

echo.
echo ========================================
echo 计算MD5...
echo ========================================
powershell -Command "Get-FileHash target\git-info-viewer-1.0.0-jar-with-dependencies.jar -Algorithm MD5 | Select-Object Hash"

echo.
echo ========================================
echo 启动应用...
echo ========================================
echo 请检查窗口标题中的时间戳！
echo 窗口标题应该显示类似：Jenkins Job Browser - Build: Sat Jan 18 02:25:11 CST 2026
echo.
pause
java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
