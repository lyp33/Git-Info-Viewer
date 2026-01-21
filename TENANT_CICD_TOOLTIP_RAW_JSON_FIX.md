# Tenant CI/CD Tooltip - Raw JSON Storage Fix

## Issue
User reported that many fields in the hover tooltip were showing "-" (empty) even though the API responses contained values for fields like `creator`, `package_title`, `queue_id`, and `modify_time`.

## Root Cause Analysis
While the parsing logic in `parseBuildResultFromJson()` was correct, there was a concern about data loss during the parsing process. The user suggested storing the raw JSON data directly in the BuildResult object to ensure no data is lost.

## Solution Implemented

### 1. Added Raw JSON Storage to BuildResult
**File**: `src/main/java/com/gitviewer/BuildResult.java`

Added a new field to store the complete raw JSON response:
```java
private String rawJsonData;  // Store raw JSON data for tooltip display

public String getRawJsonData() {
    return rawJsonData != null ? rawJsonData : "";
}

public void setRawJsonData(String rawJsonData) {
    this.rawJsonData = rawJsonData;
}
```

### 2. Updated Parsing to Store Raw JSON
**File**: `src/main/java/com/gitviewer/PortalApiClient.java`

Modified `parseBuildResultFromJson()` to store the raw JSON:
```java
private BuildResult parseBuildResultFromJson(JSONObject json) {
    BuildResult buildResult = new BuildResult();
    
    // Store raw JSON data for tooltip display
    buildResult.setRawJsonData(json.toString());
    
    // ... rest of parsing logic
}
```

### 3. Updated Tooltip to Extract from Raw JSON
**File**: `src/main/java/com/gitviewer/TenantCICDDialog.java`

Modified `showRowTooltip()` to extract data directly from raw JSON:
- Extracts fields directly from stored JSON string
- Falls back to BuildResult object fields if JSON parsing fails
- Added comprehensive error handling
- Added detailed console logging for debugging

Added new helper method `formatTime()`:
- Formats ISO 8601 timestamps to readable format
- Handles both formats: with and without milliseconds
- Handles special case: "0001-01-01T00:00:00Z" (empty date)

### 4. Added Required Imports
Added to `TenantCICDDialog.java`:
```java
import java.util.TimeZone;
import org.json.JSONObject;
```

## Benefits

1. **Data Integrity**: Raw JSON is preserved, ensuring no data loss during parsing
2. **Debugging**: Easy to inspect the exact API response for each record
3. **Flexibility**: Can extract additional fields in the future without modifying parsing logic
4. **Reliability**: Fallback mechanism ensures tooltip still works even if JSON parsing fails
5. **Consistency**: Works correctly for both query types (by plan and by application)

## API Response Structures

Both query types return the same field structure at the item level:

### By Plan Query
`/api/mo-fo/1.0/ops/multi_build?package_title=...`
- Returns: `{ app_build_histories: [...] }`
- Each item has: `id`, `queue_id`, `creator`, `package_title`, `modify_time`, etc.

### By Application Query
`/api/mo-fo/1.0/ops/build?user_name=...&app_name=...`
- Returns: `{ data: [...], total: N }`
- Each item has: `id`, `queue_id`, `creator`, `package_title`, `modify_time`, etc.

Both use the same `parseBuildResultFromJson()` method, which now stores the raw JSON.

## Testing

To verify the fix:
1. Run the application: `java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
2. Open Tenant CI/CD dialog
3. Connect to a tenant
4. Query by plan or by application
5. Hover over a result row for 1 second
6. Verify tooltip shows all fields with actual values (not "-")
7. Check console output for debug messages showing extracted values

## Console Output Example
```
Tooltip data from raw JSON: id=696f6790da7ff2f6fdfc2de8, queueId=1785762, creator=yunpeng.li@insuremo.com, packageTitle=v202601200722-20260120113127
```

## Files Modified
1. `src/main/java/com/gitviewer/BuildResult.java` - Added rawJsonData field
2. `src/main/java/com/gitviewer/PortalApiClient.java` - Store raw JSON during parsing
3. `src/main/java/com/gitviewer/TenantCICDDialog.java` - Extract tooltip data from raw JSON

## Build Status
✅ Compilation successful
✅ Package created: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
