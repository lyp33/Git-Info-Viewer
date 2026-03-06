@echo off
echo ========================================
echo AI Chat File Content API - Test Guide
echo ========================================
echo.
echo This guide helps you test the new get_file_content API
echo.
echo ========================================
echo Test Scenarios:
echo ========================================
echo.
echo 1. Small File Test (^< 10KB)
echo    Question: "AIChatDialog.java 是做什么的？"
echo    Expected: AI reads file and explains its purpose
echo.
echo 2. Config File Test
echo    Question: "pom.xml 里有哪些依赖？"
echo    Expected: AI lists all dependencies
echo.
echo 3. Large File Test (^> 50KB)
echo    Question: "获取一个大文件的内容"
echo    Expected: Content truncated to 50KB with message
echo.
echo 4. File Not Found Test
echo    Question: "NotExist.java 是做什么的？"
echo    Expected: Friendly error message
echo.
echo 5. Branch Specific Test
echo    Question: "develop分支的README.md内容是什么？"
echo    Expected: Content from develop branch
echo.
echo 6. GitHub Repository Test
echo    - Select a GitHub repository
echo    - Ask about a file
echo    Expected: Base64 decoded content (not JSON)
echo.
echo ========================================
echo How to Test:
echo ========================================
echo.
echo 1. Run: java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
echo 2. Open: Chat -^> AI Chat
echo 3. Select a Git project in left panel
echo 4. Try the test questions above
echo 5. Check console logs for detailed output
echo.
echo ========================================
echo What to Check:
echo ========================================
echo.
echo [✓] File content is displayed correctly
echo [✓] Large files are truncated with message
echo [✓] Error messages are clear and helpful
echo [✓] GitHub files are decoded (not base64)
echo [✓] Branch selection works
echo [✓] AI can analyze file content
echo.
echo ========================================
echo Console Logs to Watch:
echo ========================================
echo.
echo [AI Chat] API Call: gitApiClient.getFileContent(...)
echo [AI Chat] File content too large (XXXXX chars), truncating to 50000
echo [Git API] Successfully decoded GitHub file content, length: XXXXX
echo.
echo ========================================
pause
