# Console Log and Debug Enhancement

## Overview
Added a console log panel to the Job Details dialog and comprehensive debug logging to help diagnose issues with stage double-click functionality.

## Problem Description

### Issue: Stage Double-Click Not Working
When double-clicking on a stage module in the Stage View, nothing happens. The issue could be:
- Stage ID is null or empty
- API client not properly set
- Event handler not firing
- Dialog creation failing silently

## Solution

### 1. Added Console Log Panel

**Location:** Bottom of Job Details dialog

**Features:**
- Real-time logging of all operations
- Timestamped log entries
- Auto-scroll to latest log
- Clear Log button
- Monospaced font for readability

**Layout Changes:**
```
┌─────────────────────────────────────────┐
│  Build History                          │  200px
├─────────────────────────────────────────┤
│  Stage View                             │  200px
├─────────────────────────────────────────┤
│  Console Log                            │  Remaining space
│  [HH:mm:ss] Log message...             │
│  [Clear Log]                            │
└─────────────────────────────────────────┘
```

### 2. Comprehensive Logging

**Job Details Dialog Logs:**
- Dialog initialization
- Build history loading (start, success, count)
- Build selection
- Stage view loading (start, success, count)
- Stage details (name, ID, status)
- Error messages with stack traces

**Stage View Panel Logs:**
- Mouse click events (click count)
- Stage information (name, ID)
- API client status
- Job path and build number
- Double-click detection
- Error conditions (null checks)
- Dialog opening process

## Implementation Details

### Console Log Panel

**Component Structure:**
```java
JTextArea consoleLogArea = new JTextArea();
consoleLogArea.setEditable(false);
consoleLogArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
consoleLogArea.setBackground(new Color(240, 240, 240));
```

**Logging Method:**
```java
private void logToConsole(String message) {
    SwingUtilities.invokeLater(() -> {
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        consoleLogArea.append("[" + timestamp + "] " + message + "\n");
        consoleLogArea.setCaretPosition(consoleLogArea.getDocument().getLength());
    });
}
```

### Debug Logging in Stage View

**Mouse Click Handler:**
```java
@Override
public void mouseClicked(MouseEvent e) {
    System.out.println("[StageView] Mouse clicked on stage: " + stage.getName());
    System.out.println("[StageView] Click count: " + e.getClickCount());
    System.out.println("[StageView] Stage ID: " + (stage.getId() != null ? stage.getId() : "NULL"));
    System.out.println("[StageView] API Client: " + (apiClient != null ? "SET" : "NULL"));
    
    if (e.getClickCount() == 2) {
        System.out.println("[StageView] Double-click detected!");
        
        if (apiClient == null) {
            System.out.println("[StageView] ERROR: API Client is null!");
            return;
        }
        
        if (stage.getId() == null || stage.getId().isEmpty()) {
            System.out.println("[StageView] ERROR: Stage ID is null or empty!");
            return;
        }
        
        openStageLogDialog(stage);
    }
}
```

## Log Output Examples

### Successful Operation:
```
[00:15:23] Job Details Dialog initialized for: job/gemini/job/Manual-Build/job/all-in-one-auto-CI
[00:15:23] Loading build history for job: job/gemini/job/Manual-Build/job/all-in-one-auto-CI
[00:15:24] Successfully loaded 20 builds
[00:15:24] Auto-selected build #243
[00:15:24] Loading stage view for build #243
[00:15:25] Successfully loaded 7 stages
[00:15:25]   Stage: gemini-pa-bs-parent, ID: 123, Status: SUCCESS
[00:15:25]   Stage: bff-parent, ID: 124, Status: SUCCESS
[00:15:25]   Stage: common-bff, ID: 125, Status: SUCCESS
```

### Error Condition (Stage ID Missing):
```
[StageView] Mouse clicked on stage: gemini-pa-bs-parent
[StageView] Click count: 2
[StageView] Stage ID: NULL
[StageView] API Client: SET
[StageView] Double-click detected!
[StageView] ERROR: Stage ID is null or empty!
```

### Error Condition (API Client Missing):
```
[StageView] Mouse clicked on stage: bff-parent
[StageView] Click count: 2
[StageView] Stage ID: 124
[StageView] API Client: NULL
[StageView] Double-click detected!
[StageView] ERROR: API Client is null!
```

## Debugging Workflow

### Step 1: Check Console Log Panel
1. Open Job Details dialog
2. Look at Console Log panel at bottom
3. Check for initialization messages
4. Verify build history loaded
5. Verify stages loaded with IDs

### Step 2: Check System Console
1. Run application from command line
2. Double-click on a stage
3. Check console output for [StageView] messages
4. Identify which condition is failing

### Step 3: Diagnose Issue
Based on log output:
- **"Stage ID: NULL"** → Stage API not returning IDs
- **"API Client: NULL"** → setJobInfo() not called
- **"Click count: 1"** → Double-click not registering
- **No logs** → Event handler not attached

## Benefits

1. **Real-time Debugging** - See what's happening as it happens
2. **Persistent Log** - Review past operations
3. **Detailed Information** - All relevant data logged
4. **Easy Diagnosis** - Clear error messages
5. **User-Friendly** - Visible in UI, no need for console access

## UI Layout

### Three-Panel Split:
```
┌─────────────────────────────────────────────────────┐
│ Build History (200px)                               │
│ ● #243 - SUCCESS - Jan 17, 2026 10:31              │
│ ● #242 - SUCCESS - Jan 16, 2026 15:40              │
├─────────────────────────────────────────────────────┤
│ Stage View (200px)                                  │
│ [gemini-pa-bs-parent] [bff-parent] [common-bff]    │
├─────────────────────────────────────────────────────┤
│ Console Log (Remaining)                             │
│ [00:15:23] Job Details Dialog initialized...       │
│ [00:15:24] Successfully loaded 20 builds           │
│ [00:15:25] Successfully loaded 7 stages            │
│                                          [Clear Log]│
└─────────────────────────────────────────────────────┘
```

## Files Modified

1. `src/main/java/com/gitviewer/JenkinsJobDetailsDialog.java`
   - Added `consoleLogArea` field
   - Modified `initializeUI()` to add console log panel
   - Added `logToConsole()` method
   - Added logging to `loadBuildHistory()`
   - Added logging to `loadStageView()`
   - Changed layout to three-panel split

2. `src/main/java/com/gitviewer/JenkinsStageViewPanel.java`
   - Added detailed logging to mouse click handler
   - Added null checks with error logging
   - Added logging to `openStageLogDialog()`
   - Added System.out.println for console debugging

## Testing Checklist

✅ Console log panel appears at bottom
✅ Initialization message logged
✅ Build history loading logged
✅ Stage loading logged with details
✅ Stage IDs displayed in log
✅ Double-click events logged to console
✅ Error conditions logged clearly
✅ Clear Log button works
✅ Auto-scroll to latest log entry
✅ Application compiles successfully

## Build Information

**Build Command:** `mvn clean package`
**Build Status:** SUCCESS
**Output JAR:** `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
**Build Time:** 12.612s

## Completion Date
January 18, 2026
