# Module Double-Click Debug Enhancement

## Issue
When double-clicking a module in the Stage View, the system shows an error "API Client is not initialized". From the web UI, we observed that:
- Module list shows `gemini-pa-bs-parent [ID: 6]` 
- But the actual build ID should be #809
- The ID: 6 appears to be the stage ID, not the build ID

## Enhanced Logging

Added extensive debug logging to help identify the root cause:

### 1. JenkinsApiClient.java

#### fetchBuildStages() Method
- Logs the job path, build number, and full API URL
- Logs the API response length
- Logs the number of stages found
- For each stage, logs: name, ID, status, and duration
- Logs warnings if no stages field is found
- Logs errors with full exception details

#### fetchStageLog() Method
- Logs the base URL, job path, build number, and stage ID
- Logs the constructed API URL
- Logs success with response length
- Logs detailed error information on failure

### 2. JenkinsStageViewPanel.java

Already has comprehensive logging:
- Logs when job info is set (API client, job path, build number)
- Logs when displaying modules (count and details for each)
- Logs when loading module logs
- Logs double-click events with stage name and ID
- All logs include timestamps and are output to both console area and system console

### 3. JenkinsJobDetailsDialog.java

Already has logging:
- Logs when loading build history
- Logs when loading module view
- Logs module details (name, ID, status)
- Logs when opening build parameters dialog

## Testing Instructions

1. Run the application with the new JAR
2. Navigate to a Jenkins job with pipeline stages
3. Double-click a module in the Module List
4. Check the Console Log area for detailed debug information
5. Also check the system console (if running from command line) for additional logs

## Expected Log Output

When double-clicking a module, you should see logs like:

```
[01:29:15] === Double-Click Event ===
[01:29:15] Stage: gemini-pa-bs-parent
[01:29:15] Stage ID: 6
[01:29:15] >>> Opening stage log dialog <<<
[01:29:15] openStageLogDialog called for module: gemini-pa-bs-parent
[01:29:15] Parent frame: Found
[01:29:15] Setting dialog visible...
```

And in the API client logs:

```
[JenkinsApiClient] === Fetching Stage Log ===
[JenkinsApiClient] Base URL: https://jenkins.example.com
[JenkinsApiClient] Job Path: job/folder/job/project
[JenkinsApiClient] Build Number: 809
[JenkinsApiClient] Stage ID: 6
[JenkinsApiClient] Constructed API URL: https://jenkins.example.com/job/folder/job/project/809/execution/node/6/wfapi/log
```

## Next Steps

Based on the logs, we can determine:
1. Is the stage ID correct? (should be 6 or something else?)
2. Is the URL construction correct?
3. Is the API endpoint `/execution/node/{stageId}/wfapi/log` the right one?
4. Do we need to use a different API endpoint or ID mapping?

## Files Modified

- `src/main/java/com/gitviewer/JenkinsApiClient.java` - Added detailed logging to fetchBuildStages() and fetchStageLog()

## Package Contents

- `git-info-viewer-1.0.0-jar-with-dependencies.jar` - Enhanced with debug logging

## Date

2026-01-18
