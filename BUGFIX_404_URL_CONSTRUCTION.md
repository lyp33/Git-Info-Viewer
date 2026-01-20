# Bug Fix: Jenkins Job Browser 404 Error (v2)

## Issue Description
When expanding sub-directories in the Jenkins Job Browser, the application encountered HTTP 404 errors due to incorrect URL construction.

## Root Cause Analysis

### Primary Issue
The `extractJobPath()` method in `JenkinsBrowserDialog.java` failed to handle URL mismatches between:
- **Configured Base URL**: `http://172.25.32.166:8080` (with port)
- **Jenkins API Response URL**: `http://172.25.32.166/job/gemini/job/Manual-Build/` (without port)

When Jenkins returns URLs without the port number, the string comparison `url.startsWith(baseUrl)` fails, causing the method to return the full URL instead of extracting the relative path.

### Error Example
**Incorrect URL (before fix):**
```
http://172.25.32.166:8080//http://172.25.32.166/job/gemini/job/Manual-Build//api/json?tree=jobs[name,url,_class,jobs]
```

**Correct URL (after fix):**
```
http://172.25.32.166:8080/job/gemini/job/Manual-Build/api/json?tree=jobs[name,url,_class,jobs]
```

## Solution

### Version 1 (Incomplete)
Initial fix only handled the case where URLs matched exactly, but didn't account for port number differences.

### Version 2 (Complete Fix)
Enhanced the `extractJobPath()` method to handle multiple URL formats:

1. **Direct baseUrl match**: When URL starts with configured baseUrl
2. **Port mismatch handling**: Extract path from hostname when port differs
3. **Relative path handling**: Handle URLs that are already relative paths

### Code Changes
**File:** `src/main/java/com/gitviewer/JenkinsBrowserDialog.java`

**Method:** `extractJobPath(String url)`

**Key Improvements:**
```java
// Check if URL is a complete HTTP/HTTPS URL
if (url.startsWith("http://") || url.startsWith("https://")) {
    // Try direct baseUrl match first
    if (url.startsWith(baseUrl)) {
        // Extract relative path
    }
    
    // If no match (e.g., port mismatch), extract path after hostname
    int protocolEnd = url.indexOf("://");
    if (protocolEnd != -1) {
        int pathStart = url.indexOf("/", protocolEnd + 3);
        if (pathStart != -1) {
            String path = url.substring(pathStart + 1);
            // This extracts: job/gemini/job/Manual-Build
            return path;
        }
    }
}
```

### Enhanced Logging
Added detailed logging to help diagnose URL extraction:
- "Extracting path from URL: {url}"
- "Base URL: {baseUrl}"
- "Extracted job path (direct match): {path}"
- "Extracted job path (from host): {path}"
- "Using as relative path: {path}"

## Testing

### Build Verification
```bash
mvn clean package
# BUILD SUCCESS
# Total time: 12.987 s
# Finished at: 2026-01-17T20:22:17+08:00
```

### Test Scenarios
1. ✅ URL with matching port: `http://172.25.32.166:8080/job/gemini/`
2. ✅ URL without port: `http://172.25.32.166/job/gemini/job/Manual-Build/`
3. ✅ Relative path: `job/gemini/job/Manual-Build`
4. ✅ URL with trailing slashes
5. ✅ URL without trailing slashes

### Console Log Verification
The console log panel now shows:
- Original URL from Jenkins
- Configured base URL
- Extraction method used
- Final extracted path
- Complete API URL being called

## Files Modified
- `src/main/java/com/gitviewer/JenkinsBrowserDialog.java`

## Build Output
- `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
- Build time: January 17, 2026, 20:22:17

## Version History
- **v1**: January 17, 2026, 19:28 - Initial fix (incomplete)
- **v2**: January 17, 2026, 20:22 - Complete fix with port mismatch handling

## Verification Steps
1. Launch the application: `java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
2. Open CI/CD → Core/SDK Build
3. Expand the "Manual-Build" folder
4. Verify that sub-items load correctly
5. Check console log for:
   - "Extracting path from URL: http://172.25.32.166/job/gemini/job/Manual-Build/"
   - "Base URL: http://172.25.32.166:8080"
   - "Extracted job path (from host): job/gemini/job/Manual-Build"
   - "Successfully loaded X child items"

## Impact
- **Severity:** High (blocking feature functionality)
- **Scope:** Jenkins Job Browser navigation
- **Risk:** Low (isolated fix, no impact on other features)
- **Compatibility:** Handles both URLs with and without port numbers

## Related Issues
- Jenkins API sometimes returns URLs without port numbers
- URL comparison must be flexible to handle various Jenkins configurations

