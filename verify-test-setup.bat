@echo off
REM 验证测试环境设置

echo ========================================
echo 验证 Mock Jenkins 测试环境
echo ========================================
echo.

set ERROR=0

echo [1/4] 检查 JAR 文件...
if exist "target\git-info-viewer-1.0.0-jar-with-dependencies.jar" (
    echo   ✓ JAR 文件存在
) else (
    echo   ✗ JAR 文件不存在
    echo   请运行: mvn clean package
    set ERROR=1
)

echo.
echo [2/4] 检查 MockJenkinsServer 源文件...
if exist "src\main\java\com\gitviewer\MockJenkinsServer.java" (
    echo   ✓ MockJenkinsServer.java 在正确位置
) else (
    echo   ✗ MockJenkinsServer.java 不存在
    set ERROR=1
)

echo.
echo [3/4] 检查启动脚本...
if exist "start-mock-jenkins.bat" (
    echo   ✓ start-mock-jenkins.bat 存在
) else (
    echo   ✗ start-mock-jenkins.bat 不存在
    set ERROR=1
)

echo.
echo [4/4] 检查文档...
if exist "QUICK_START_MOCK_JENKINS.md" (
    echo   ✓ QUICK_START_MOCK_JENKINS.md 存在
) else (
    echo   ✗ QUICK_START_MOCK_JENKINS.md 不存在
    set ERROR=1
)

if exist "MOCK_JENKINS_GUIDE.md" (
    echo   ✓ MOCK_JENKINS_GUIDE.md 存在
) else (
    echo   ✗ MOCK_JENKINS_GUIDE.md 不存在
    set ERROR=1
)

if exist "MOCK_JENKINS_TEST_READY.md" (
    echo   ✓ MOCK_JENKINS_TEST_READY.md 存在
) else (
    echo   ✗ MOCK_JENKINS_TEST_READY.md 不存在
    set ERROR=1
)

echo.
echo ========================================
if %ERROR%==0 (
    echo ✓ 所有检查通过！
    echo.
    echo 可以开始测试了：
    echo   1. 运行: start-mock-jenkins.bat
    echo   2. 运行: java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
    echo   3. 参考: MOCK_JENKINS_TEST_READY.md
) else (
    echo ✗ 发现问题，请检查上述错误
)
echo ========================================

pause
