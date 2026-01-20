# Stage View Enhancements

## Date: 2026-01-18

## Changes Made

### 1. Added Console Log Panel Below Stage View ✓
**File**: `JenkinsStageViewPanel.java`

The Stage View panel now has a two-part layout:
- **Top**: Horizontal scrollable stage boxes
- **Bottom**: Console log output area (black background, white text)

The console log displays:
- Initialization messages
- Stage loading information
- Click event details
- API call results
- Error messages with timestamps

### 2. Enhanced Debug Logging ✓

Added comprehensive logging throughout the panel:

**Initialization**:
```
[00:00:00.000] Stage View Panel initialized
```

**Job Info Set**:
```
[00:00:00.123] === Job Info Set ===
[00:00:00.123] API Client: SET
[00:00:00.123] Job Path: job/gemini/job/Manual-Build
[00:00:00.123] Build Number: 243
[00:00:00.123] ====================
```

**Stage Display**:
```
[00:00:00.456] === Displaying Stages ===
[00:00:00.456] Number of stages: 5
[00:00:00.456] Stage 1: gemini-pa-bs-parent (ID: 123, Status: SUCCESS)
[00:00:00.456] Stage 2: Dependency-CI-ROBOT (ID: 124, Status: SUCCESS)
...
[00:00:00.456] ========================
```

**Click Events**:
```
[00:00:01.789] === Mouse Click Event ===
[00:00:01.789] Stage: gemini-pa-bs-parent
[00:00:01.789] Click count: 2
[00:00:01.789] Button: 1
[00:00:01.789] Stage ID: 123
[00:00:01.789] API Client: SET
[00:00:01.789] Job Path: job/gemini/job/Manual-Build
[00:00:01.789] Build Number: 243
[00:00:01.789] >>> DOUBLE-CLICK DETECTED <<<
[00:00:01.789] Opening stage log dialog...
[00:00:01.789] ========================
```

All logs are:
- Displayed in the console log panel with timestamps
- Also output to System.out for external debugging
- Formatted with clear section markers (===)

### 3. Display Build ID on Each Stage ✓

Each stage box now shows:
- **Stage Name** (bold, 11pt)
- **Build ID** (small gray text, 9pt) - Format: "ID: 123"
- **Status Icon** (✓, ✗, ⟳, -)
- **Duration** (e.g., "2m 15s")

The Build ID is displayed in a smaller font below the stage name, making it easy to identify each stage's unique identifier.

### 4. Single Click Loads Stage Log to Console ✓

**Single Click Behavior**:
- Loads the stage log into the console log panel
- Uses the stage's build ID to fetch the log via API
- Displays log with clear section markers

**Example**:
```
[00:00:02.000] Single click - loading stage log in console...
[00:00:02.000] Loading log for stage: gemini-pa-bs-parent (ID: 123)
[00:00:02.100] === Stage Log: gemini-pa-bs-parent ===
[00:00:02.100] [actual log content from Jenkins]
[00:00:02.100] === End of Stage Log ===
```

### 5. Double Click Opens Stage Log Dialog ✓

**Double Click Behavior**:
- Opens a separate dialog window with the stage log
- Includes extensive error handling and logging
- Shows error dialogs if API client or stage ID is missing

**Error Handling**:
- If API Client is null: Shows error dialog and logs error
- If Stage ID is null/empty: Shows error dialog and logs error
- If dialog creation fails: Shows error dialog with exception message

### 6. Enhanced Visual Feedback ✓

**Mouse Hover**:
- Border changes from light gray (1px) to dark gray (2px)
- Cursor changes to hand cursor
- Indicates the stage is clickable

**Stage Box Size**:
- Increased from 120x80 to 140x100 pixels
- Provides more space for Build ID display

## API Integration

The stage log is fetched using the existing API method:
```java
apiClient.fetchStageLog(jobPath, buildNumber, stage.getId())
```

This constructs a URL like:
```
http://172.25.32.166:8080/job/gemini/job/Manual-Build/job/thailifesdk/job/24.08_thailife_sit/job/CI-Robot/job/Dependency-CI-ROBOT/500/console
```

Where:
- `jobPath`: Full job path
- `buildNumber`: Build number (e.g., 500)
- `stage.getId()`: Stage ID from the API response

## Layout Structure

```
┌─────────────────────────────────────────────────────────┐
│ Stage View Panel                                        │
├─────────────────────────────────────────────────────────┤
│ ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐          │
│ │Stage│  │Stage│  │Stage│  │Stage│  │Stage│  ← Stages │
│ │  1  │  │  2  │  │  3  │  │  4  │  │  5  │            │
│ │ID:12│  │ID:13│  │ID:14│  │ID:15│  │ID:16│            │
│ └─────┘  └─────┘  └─────┘  └─────┘  └─────┘            │
├─────────────────────────────────────────────────────────┤
│ Console Log                                             │
│ ┌─────────────────────────────────────────────────────┐ │
│ │ [00:00:00.000] Stage View Panel initialized        │ │
│ │ [00:00:00.123] === Job Info Set ===                │ │
│ │ [00:00:00.123] API Client: SET                     │ │
│ │ [00:00:00.123] Job Path: job/gemini/...            │ │
│ │ ...                                                 │ │
│ └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

## Testing

### Build Status
✓ Compilation successful
✓ JAR created: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

### Test Scenarios

1. **View Stages**: Open a job with pipeline stages
   - Expected: Stages displayed with names, IDs, status, and duration
   - Expected: Console log shows initialization and stage loading messages

2. **Single Click Stage**: Click once on a stage
   - Expected: Console log shows click event details
   - Expected: Stage log loads into console log panel

3. **Double Click Stage**: Double-click on a stage
   - Expected: Console log shows double-click detection
   - Expected: Stage log dialog opens in new window

4. **Error Handling**: Try clicking when API client is not set
   - Expected: Error dialog appears
   - Expected: Console log shows error message

5. **Mouse Hover**: Hover over a stage
   - Expected: Border becomes thicker and darker
   - Expected: Cursor changes to hand

## Files Modified
- `src/main/java/com/gitviewer/JenkinsStageViewPanel.java`

## Benefits

1. **Better Debugging**: Console log provides real-time feedback on all operations
2. **Stage Identification**: Build IDs visible on each stage for easy reference
3. **Quick Log Access**: Single click loads log without opening dialog
4. **Detailed Logging**: Comprehensive logs help diagnose issues
5. **Error Visibility**: Errors displayed both in dialogs and console log
6. **Improved UX**: Clear visual feedback and multiple interaction methods
