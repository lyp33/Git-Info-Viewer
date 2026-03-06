@echo off
echo ========================================
echo Testing Get Single Commit Feature
echo ========================================
echo.
echo This test will verify the new get_single_commit Tool
echo.
echo Test Steps:
echo 1. Run the application: mvn clean package
echo 2. Start the app: java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
echo 3. Open a Git project in the left tree
echo 4. Right-click and select "AI Chat"
echo 5. In AI Chat, ask: "XXX文件最后一次修改了什么？"
echo.
echo Expected Behavior:
echo - Round 1: AI calls get_file_commits to get commit list
echo - Round 2: AI calls get_single_commit with the latest commit SHA
echo - Round 3: AI returns FINISH and shows the answer with diff content
echo.
echo Watch the console log for:
echo [AI Chat] Tool name: get_file_commits
echo [AI Chat] Tool name: get_single_commit
echo [AI Chat] Agent decided to FINISH
echo.
echo ========================================
echo Building the application...
echo ========================================
echo.

mvn clean package

echo.
echo ========================================
echo Build Complete!
echo ========================================
echo.
echo Now run: java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
echo.
pause
