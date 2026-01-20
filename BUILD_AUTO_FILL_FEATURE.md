# Build Parameters Auto-Fill Feature

## Date: 2026-01-18

## Feature Description

When clicking "Build with Parameters" button, the system now automatically:
1. Fetches the latest build's parameters
2. Pre-fills `TENANT_NAME`, `mail_list`, and `versions` fields
3. Auto-increments the numeric part of the `versions` field

## Implementation Details

### Modified Files
- `src/main/java/com/gitviewer/JenkinsJobDetailsDialog.java`

### Key Changes

#### 1. Auto-Fetch Latest Build Parameters
When "Build with Parameters" is clicked, the system:
- Fetches the most recent build from history
- Retrieves all parameters from that build
- Pre-fills the build parameters dialog

#### 2. Version Auto-Increment
The `versions` field is automatically incremented:

**Examples**:
- `1.2.3` → `1.2.4`
- `v2.0.1-beta` → `v2.0.2-beta`
- `app-3.5.0` → `app-3.5.1`
- Multiple versions: `1.0.0,2.0.0` → `1.0.1,2.0.1`

**Algorithm**:
- Finds the last numeric sequence in each version string
- Increments that number by 1
- Preserves all prefixes, suffixes, and separators
- Supports multiple versions separated by commas, semicolons, or newlines

#### 3. Implementation Methods

**`openBuildParametersDialog()`**:
- Uses SwingWorker to fetch latest build parameters in background
- Calls `incrementVersionNumbers()` to auto-increment versions
- Logs all operations to console for debugging
- Falls back gracefully if no previous builds exist

**`incrementVersionNumbers(String versions)`**:
- Handles multiple versions separated by `,`, `;`, or newlines
- Calls `incrementSingleVersion()` for each version
- Returns comma-separated incremented versions

**`incrementSingleVersion(String version)`**:
- Uses regex pattern `\\d+` to find all numeric sequences
- Increments the last numeric sequence found
- Preserves version format (prefixes, suffixes, separators)

### Console Logging

The feature includes comprehensive logging:
```
Opening build parameters dialog...
Found latest build #243
Fetched 5 parameters from latest build
Incremented versions: 24.08_thailife_devsdk_v0.056 -> 24.08_thailife_devsdk_v0.057
```

## Usage

1. Open Jenkins Job Details dialog
2. Click "Build with Parameters" button
3. Dialog opens with pre-filled values:
   - `TENANT_NAME`: copied from latest build
   - `mail_list`: copied from latest build
   - `versions`: copied and auto-incremented from latest build
   - Other parameters: copied from latest build
4. Review and modify parameters if needed
5. Click "Build" to trigger the build

## Edge Cases Handled

1. **No Previous Builds**: Opens dialog without pre-filled values
2. **No Versions Parameter**: Other parameters still pre-filled
3. **Non-Numeric Versions**: Returns original value unchanged
4. **Empty Versions**: Returns empty string
5. **Complex Version Formats**: Handles prefixes, suffixes, and separators correctly

## Testing Scenarios

### Test 1: Simple Version Increment
- Input: `versions = "1.0.0"`
- Expected: `versions = "1.0.1"`

### Test 2: Complex Version Format
- Input: `versions = "24.08_thailife_devsdk_v0.056"`
- Expected: `versions = "24.08_thailife_devsdk_v0.057"`

### Test 3: Multiple Versions
- Input: `versions = "1.0.0,2.0.0,3.0.0"`
- Expected: `versions = "1.0.1,2.0.1,3.0.1"`

### Test 4: Version with Suffix
- Input: `versions = "v2.0.1-beta"`
- Expected: `versions = "v2.0.2-beta"`

### Test 5: Other Parameters
- Input: Latest build has `TENANT_NAME = "test-tenant"`, `mail_list = "user@example.com"`
- Expected: Both fields pre-filled with same values

## Build Status
✓ Compilation successful
✓ JAR created: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## Notes

- The feature works for both "Build with Parameters" and "Rebuild" actions
- Version increment logic is conservative - only increments if numeric pattern found
- All operations are logged to console for debugging
- Graceful fallback if latest build fetch fails
