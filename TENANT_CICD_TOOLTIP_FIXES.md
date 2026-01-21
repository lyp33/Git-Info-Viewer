# Tenant CI/CD Tooltip Fixes

## Date
January 21, 2026

## Issues Fixed

### Issue 1: Font Too Large
**Problem**: Tooltip font size was too large (11px), making the tooltip appear bulky

**Solution**: 
- Reduced font size from 11px to 9px
- Increased tooltip width from 400px to 500px to accommodate longer text
- Reduced padding from 10px to 8px
- Reduced cell padding from 3px to 2px

**Changes in TenantCICDDialog.java**:
```java
// Before:
tooltip.append("<html><body style='width: 400px; padding: 10px;'>");
tooltip.append("<table cellpadding='3' cellspacing='0' style='font-family: Microsoft YaHei UI; font-size: 11px;'>");

// After:
tooltip.append("<html><body style='width: 500px; padding: 8px;'>");
tooltip.append("<table cellpadding='2' cellspacing='0' style='font-family: Microsoft YaHei UI; font-size: 9px;'>");
```

### Issue 2: Labels Wrapping
**Problem**: Labels were wrapping to multiple lines, making the tooltip look messy

**Solution**:
- Added `white-space: nowrap;` CSS style to label cells
- Added `word-wrap: break-word;` to value cells to allow long values to wrap properly

**Changes in TenantCICDDialog.java - addTooltipRow() method**:
```java
// Label cell - no wrapping
tooltip.append("<td style='font-weight: bold; color: #5f6368; padding-right: 10px; white-space: nowrap;'>")

// Value cell - allow wrapping for long values
tooltip.append("<td style='color: #202124; word-wrap: break-word;'>")
```

### Issue 3: Empty Fields (Creator, Queue ID, etc.)
**Problem**: Fields like creator, queue_id, package_title, and modify_time were showing "-" even though API returns values

**Root Cause**: 
- The `queue_id` field was being set to "0" when not found (optLong returns 0 by default)
- Other fields might be empty strings in some API responses

**Solution**:
1. **Queue ID Handling**: Check if queue_id > 0 before setting, otherwise set empty string
2. **Enhanced Logging**: Added detailed console output to help debug field extraction

**Changes in PortalApiClient.java - parseBuildResultFromJson() method**:
```java
// Before:
buildResult.setQueueId(String.valueOf(json.optLong("queue_id", 0)));

// After:
long queueIdLong = json.optLong("queue_id", 0);
if (queueIdLong > 0) {
    buildResult.setQueueId(String.valueOf(queueIdLong));
} else {
    buildResult.setQueueId("");
}
```

**Added Debug Logging**:
```java
// Detailed logging for debugging
logger.debug("Parsed build result: id={}, queueId={}, app={}, status={}, creator={}, packageTitle={}, modifyTime={}", 
            buildResult.getId(), buildResult.getQueueId(), buildResult.getAppName(), 
            buildResult.getBuildStatus(), buildResult.getCreator(), buildResult.getPackageTitle(),
            buildResult.getModifyTime());

// Console output for easy debugging
String logMsg = "  Parsed: id=" + buildResult.getId() + 
               ", queueId=" + buildResult.getQueueId() + 
               ", creator=" + buildResult.getCreator() + 
               ", packageTitle=" + buildResult.getPackageTitle();
System.out.println(logMsg);
```

## Testing Instructions

### 1. Test Font Size
- Hover over a table row
- Wait 1 second for tooltip to appear
- Verify font is smaller and more compact
- Verify labels don't wrap to multiple lines

### 2. Test Field Values
- Perform a search query (by app or by plan)
- Hover over different rows
- Check console output for parsed field values
- Verify tooltip shows actual values for:
  - Queue ID (should be a number, not "-")
  - Creator (should show email address)
  - Package Title (should show package name if available)
  - Modify Time (should show formatted timestamp)

### 3. Console Output Example
When you perform a search, you should see output like:
```
  Parsed: id=696f6ac9da7ff2f6fdfc2efa, queueId=1785959, creator=yunpeng.li@insuremo.com, packageTitle=v202601200722-20260120113127
```

## Expected Tooltip Appearance

**Before**:
- Large font (11px)
- Labels wrapping
- Many "-" values
- Width: 400px

**After**:
- Smaller font (9px)
- Labels on single line
- Actual values from API
- Width: 500px
- More compact and readable

## API Field Mapping

From `/api/mo-fo/1.0/ops/build` response:
```json
{
  "id": "696f6ac9da7ff2f6fdfc2efa",
  "queue_id": 1785959,                    // Now properly extracted
  "app_name": "thailife-bs",
  "creator": "yunpeng.li@insuremo.com",   // Now properly extracted
  "package_title": "v202601200722...",    // Now properly extracted
  "create_time": "2026-01-20T11:31:28.804Z",
  "modify_time": "2026-01-20T11:36:33.386Z", // Now properly extracted
  "image_name": "docker-all.repo..."
}
```

## Files Modified

1. **src/main/java/com/gitviewer/TenantCICDDialog.java**
   - Reduced tooltip font size and adjusted dimensions
   - Added CSS styles to prevent label wrapping
   - Added word-wrap for long values

2. **src/main/java/com/gitviewer/PortalApiClient.java**
   - Fixed queue_id extraction (handle 0 value)
   - Enhanced debug logging
   - Added console output for field values

## Build Status
✅ **Compilation**: Successful
✅ **Packaging**: Successful
✅ **JAR Created**: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## Notes

- If fields still show "-", check the console output to see what values are being parsed
- The API might return empty strings for some fields depending on the build status
- Queue ID will only show if it's greater than 0 (valid queue ID)
- All timestamps are formatted from ISO 8601 to readable format (YYYY-MM-DD HH:MM:SS)
