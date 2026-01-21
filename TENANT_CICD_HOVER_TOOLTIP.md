# Tenant CI/CD - Row Hover Tooltip Feature

## Overview
Added hover tooltip functionality to the Tenant CI/CD results table that displays detailed build information after 1 second of mouse hover.

## Implementation Date
January 21, 2026

## Changes Made

### 1. BuildResult.java - Added New Fields
**File**: `src/main/java/com/gitviewer/BuildResult.java`

Added the following fields to store additional information from API:
- `queueId` - Queue ID from Portal API
- `modifyTime` - Modification timestamp
- `creator` - User who created the build
- `packageTitle` - Package title/name

Added getters/setters for all new fields:
- `getQueueId()` / `setQueueId()`
- `getModifyTime()` / `setModifyTime()`
- `getCreator()` / `setCreator()`
- `getPackageTitle()` / `setPackageTitle()`

Added new method:
- `getFormattedModifyTime()` - Formats modify_time from ISO 8601 to readable format

### 2. PortalApiClient.java - Enhanced JSON Parsing
**File**: `src/main/java/com/gitviewer/PortalApiClient.java`

Updated `parseBuildResultFromJson()` method to extract additional fields:
```java
buildResult.setQueueId(String.valueOf(json.optLong("queue_id", 0)));
buildResult.setModifyTime(json.optString("modify_time", ""));
buildResult.setCreator(json.optString("creator", ""));
buildResult.setPackageTitle(json.optString("package_title", ""));
```

### 3. TenantCICDDialog.java - Hover Tooltip Implementation
**File**: `src/main/java/com/gitviewer/TenantCICDDialog.java`

#### Added Fields:
- `hoverTimer` - javax.swing.Timer for 1-second delay
- `lastHoverRow` - Tracks the currently hovered row

#### New Methods:

**setupHoverTooltip()**
- Creates a Timer with 1000ms (1 second) delay
- Adds MouseMotionListener to track mouse movement
- Restarts timer when mouse moves to different row
- Clears tooltip when mouse exits table

**showRowTooltip(int viewRow)**
- Retrieves BuildResult for the hovered row
- Constructs HTML-formatted tooltip with detailed information
- Displays: ID, Queue ID, App Name, Creator, Package Title, Create Time, Modify Time, Image Name
- Uses table layout for clean presentation

**addTooltipRow(StringBuilder, String, String)**
- Helper method to add formatted rows to tooltip HTML
- Handles null/empty values by displaying "-"
- Applies consistent styling (bold labels, regular values)

**escapeHtml(String)**
- Escapes HTML special characters to prevent rendering issues
- Handles: &, <, >, ", '

#### Cleanup:
- Added hover timer cleanup in `dispose()` method
- Stops timer and sets to null to prevent memory leaks

## Tooltip Display Format

The tooltip displays information in a clean table format:

```
ID:             696f6ac9da7ff2f6fdfc2efa
Queue ID:       1785959
App Name:       thailife-bs
Creator:        yunpeng.li@insuremo.com
Package Title:  v202601200722-20260120113127
Create Time:    2026-01-20 11:31:28
Modify Time:    2026-01-20 11:36:33
Image Name:     docker-all.repo.ebaotech.com/thailife/thailife-bs:v202601200722
```

## User Experience

1. **Hover Trigger**: User moves mouse over a table row and keeps it still
2. **1-Second Delay**: Timer waits 1 second to avoid showing tooltip during quick mouse movements
3. **Tooltip Display**: Detailed information appears in a formatted HTML tooltip
4. **Auto-Hide**: Tooltip disappears when:
   - Mouse moves to a different row (timer restarts)
   - Mouse exits the table area
5. **No Interference**: Tooltip doesn't interfere with other table interactions (clicking, selecting, scrolling)

## Technical Details

### Timer Behavior
- **Delay**: 1000ms (1 second)
- **Repeats**: false (one-shot timer)
- **Restart**: Automatically restarts when mouse moves to different row
- **Cleanup**: Properly stopped and disposed in dialog cleanup

### HTML Tooltip Styling
- **Font**: Microsoft YaHei UI, 11px
- **Width**: 400px fixed width for consistent display
- **Colors**: 
  - Labels: #5f6368 (gray)
  - Values: #202124 (dark gray/black)
- **Layout**: HTML table with proper padding and spacing

### Performance Considerations
- Timer prevents tooltip from showing during rapid mouse movements
- Only one timer instance per dialog
- Tooltip generation is lightweight (string concatenation)
- No API calls triggered by hover (uses existing data)

## API Fields Mapping

From Portal API response (`/api/mo-fo/1.0/ops/build`):
```json
{
  "id": "696f6ac9da7ff2f6fdfc2efa",
  "queue_id": 1785959,
  "app_name": "thailife-bs",
  "creator": "yunpeng.li@insuremo.com",
  "package_title": "v202601200722-20260120113127",
  "create_time": "2026-01-20T11:31:28.804Z",
  "modify_time": "2026-01-20T11:36:33.386Z",
  "image_name": "docker-all.repo.ebaotech.com/thailife/thailife-bs:v202601200722"
}
```

## Testing Recommendations

1. **Hover Behavior**:
   - Hover over a row and wait 1 second - tooltip should appear
   - Move mouse quickly across rows - tooltip should not appear
   - Move mouse to different row while tooltip is showing - should update

2. **Tooltip Content**:
   - Verify all 8 fields are displayed correctly
   - Check that empty fields show "-"
   - Verify time formatting (should be readable, not ISO format)

3. **Edge Cases**:
   - Empty result set - no tooltip should appear
   - Very long values - should wrap properly within 400px width
   - Special characters in values - should be properly escaped

4. **Cleanup**:
   - Close dialog - verify no memory leaks
   - Open/close multiple times - verify timer is properly cleaned up

## Build Status
✅ **Compilation**: Successful
✅ **Packaging**: Successful
✅ **JAR Created**: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## Related Features
- Branch Filter (Task 7)
- Build Output Dialog (Task 4)
- Tenant CI/CD Query (Tasks 1-3)

## Notes
- Tooltip uses HTML rendering for rich formatting
- Font matches the rest of the application (Microsoft YaHei UI)
- Implementation follows Swing best practices for tooltip management
- No external dependencies required (uses built-in Swing components)
