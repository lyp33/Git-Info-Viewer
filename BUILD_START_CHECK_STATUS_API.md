# Build Start - Check Status API Support

## Date: 2026-01-21

## Summary
Added special handling for "Build Start" status records in Tenant CI/CD results table. When double-clicking a record with "Build Start" status, the system now uses the `check_status` API instead of the `query_one` API.

## Problem
Previously, all build records used the same `query_one` API regardless of their build status. However, records with "Build Start" status need to use a different API endpoint (`check_status`) to get the current build status and output.

## Solution
Implemented conditional API selection based on the build status:
- **Build Start** status → Use `check_status` API
- **Other statuses** → Use `query_one` API (existing behavior)

## Implementation Details

### 1. TenantCICDDialog Changes
Modified `handleViewBuildOutput()` method to:
- Retrieve the build status from the selected record
- Pass a `useBuildStart` flag to BuildOutputDialog based on status
- Log the status for debugging

```java
String buildStatus = buildResult.getBuildStatus();
boolean useBuildStart = "Build Start".equalsIgnoreCase(buildStatus);

BuildOutputDialog dialog = new BuildOutputDialog(
    this, apiClient, currentTenant, currentToken, buildId, appName, useBuildStart);
```

### 2. BuildOutputDialog Changes
Added:
- New field: `private boolean useBuildStart`
- New constructor overload accepting `useBuildStart` parameter
- Backward-compatible constructor (defaults to `false`)
- Modified `loadBuildOutput()` to select API based on flag

API Selection Logic:
```java
if (useBuildStart) {
    // Build Start status
    url = "https://portal.insuremo.com/api/mo-fo/1.0/ops/build/history/check_status?id=" + buildId;
} else {
    // Other statuses
    url = "https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=" + buildId;
}
```

### 3. PortalApiClient Changes
Added method overload:
- `getBuildOutputById(tenantCode, token, buildId)` - existing method (backward compatible)
- `getBuildOutputById(tenantCode, token, buildId, useBuildStart)` - new method with API selection

Both methods use the same headers and parameters, only the URL changes based on the `useBuildStart` flag.

## API Endpoints

### check_status API (Build Start)
- **URL**: `https://portal.insuremo.com/api/mo-fo/1.0/ops/build/history/check_status?id={buildId}`
- **Method**: GET
- **Headers**:
  - `x-mo-target-tenant`: {tenantCode}
  - `authorization`: Bearer {token}
  - `Accept`: application/json

### query_one API (Other Statuses)
- **URL**: `https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id={buildId}`
- **Method**: GET
- **Headers**: Same as above

## Benefits
1. **Correct API usage**: Uses the appropriate API for each build status
2. **Real-time status**: Build Start records can get current status updates
3. **Backward compatible**: Existing functionality for other statuses unchanged
4. **Clear logging**: API type is logged for debugging

## Files Modified
- `src/main/java/com/gitviewer/TenantCICDDialog.java`
  - Modified `handleViewBuildOutput()` method
  - Added build status check and flag passing

- `src/main/java/com/gitviewer/BuildOutputDialog.java`
  - Added `useBuildStart` field
  - Added new constructor overload
  - Modified `loadBuildOutput()` for API selection
  - Enhanced logging with API type

- `src/main/java/com/gitviewer/PortalApiClient.java`
  - Added `getBuildOutputById()` method overload
  - Implemented API selection logic
  - Enhanced logging

## Testing
- [x] Compile successful
- [x] Package successful
- [ ] Test double-clicking "Build Start" record
- [ ] Verify check_status API is called
- [ ] Test double-clicking other status records
- [ ] Verify query_one API is still used
- [ ] Check build output display for both cases

## Related Features
- Tenant CI/CD results table
- Build output dialog
- Build status monitoring
