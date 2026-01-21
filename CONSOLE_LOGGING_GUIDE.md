# Console Logging Guide

## Problem
SLF4J logging output is not visible in the console when using certain batch files.

## Solution

### Current Logging Implementation
The application now uses **DUAL logging** to guarantee console output:
1. **SLF4J Simple Logger** - configured via `src/main/resources/simplelogger.properties`
2. **System.out.println()** - direct console output as backup

Both methods are used simultaneously in:
- `PortalApiClient.java`
- `TenantCICDDialog.java`
- `BuildOutputDialog.java`

### How to See Console Output

#### Option 1: Use `run-with-console.bat` (Recommended)
```batch
run-with-console.bat
```
This keeps the console window open and shows all output.

#### Option 2: Use `test-logging.bat` (New)
```batch
test-logging.bat
```
Specifically designed for testing logging output.

#### Option 3: Run Manually
```batch
java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
```

### ⚠️ DO NOT USE
**DO NOT use `restart-app.bat`** if you want to see console output!
- It uses `start` command which launches in a hidden window
- Console output goes to a background process

## Logging Configuration

### File: `src/main/resources/simplelogger.properties`
```properties
# Output to System.out (console)
org.slf4j.simpleLogger.logFile=System.out

# Default log level
org.slf4j.simpleLogger.defaultLogLevel=INFO

# Show date time
org.slf4j.simpleLogger.showDateTime=true
org.slf4j.simpleLogger.dateTimeFormat=yyyy-MM-dd HH:mm:ss

# Specific logger levels (DEBUG for detailed output)
org.slf4j.simpleLogger.log.com.gitviewer.PortalApiClient=DEBUG
org.slf4j.simpleLogger.log.com.gitviewer.TenantCICDDialog=DEBUG
org.slf4j.simpleLogger.log.com.gitviewer.BuildOutputDialog=DEBUG
```

## What You'll See

### When Connecting:
```
=== Getting Token ===
Username: xxx, TenantCode: xxx
=== POST Request ===
URL: https://portal.insuremo.com/cas/get-token
Header: Content-Type = application/json
Header: x-mo-user-source-id = platform
Header: x-mo-tenant-id = xxx
Header: x-mo-client-id = key
Body: [REDACTED - contains sensitive data]
=== Response (Success) ===
Status Code: 200
Body: {"access_token":"...","expire_in":7200,...}
```

### When Searching by Plan:
```
=== Getting Plan Names ===
TenantCode: xxx
=== GET Request ===
URL: https://portal.insuremo.com/api/mo-fo/1.0/ops/multi_build/title_list
Header: x-mo-target-tenant = xxx
Header: authorization = Bearer abcd...wxyz
Header: Accept = application/json
=== Response (Success) ===
Status Code: 200
Body: ["Plan 1","Plan 2","Plan 3",...]
Parsed 10 plan names
  Plan[0]: Plan 1
  Plan[1]: Plan 2
  ...
```

### When Viewing Build Output:
```
=== Getting Build Output by ID ===
TenantCode: xxx, BuildId: 12345
Full URL: https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=12345
Request headers: x-mo-target-tenant=xxx, authorization=Bearer abcd...wxyz
=== GET Request ===
URL: https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=12345
...
Response received, length: 5432
Response preview (first 500 chars): {"callback":{"build_output":"..."},...}
```

## Testing Steps

1. **Rebuild the application:**
   ```batch
   mvn clean package
   ```

2. **Run with console:**
   ```batch
   test-logging.bat
   ```
   OR
   ```batch
   run-with-console.bat
   ```

3. **Test Tenant CI/CD:**
   - Click "Tenant CI/CD" button
   - Click "Connect" - watch for connection logs
   - Enter a plan name and click "Search" - watch for API call logs
   - Double-click a result row - watch for build output logs

4. **Verify output:**
   - You should see detailed logs for every API call
   - Both logger output and System.out.println output
   - Request details (URL, headers, body)
   - Response details (status code, body preview)

## Troubleshooting

### If you still don't see output:

1. **Check the batch file:**
   - Make sure you're NOT using `restart-app.bat`
   - Use `run-with-console.bat` or `test-logging.bat`

2. **Check the JAR file:**
   - Verify `simplelogger.properties` is included:
     ```batch
     jar tf target\git-info-viewer-1.0.0-jar-with-dependencies.jar | findstr simplelogger
     ```
   - Should show: `simplelogger.properties`

3. **Rebuild if needed:**
   ```batch
   mvn clean package
   ```

4. **Check console encoding:**
   - If you see garbled Chinese characters, run:
     ```batch
     chcp 65001
     ```
   - Then run the application again

## Summary

✅ **Logging is configured correctly**
✅ **Dual logging (SLF4J + System.out) is implemented**
✅ **Use `run-with-console.bat` or `test-logging.bat` to see output**
❌ **Don't use `restart-app.bat` for debugging**
