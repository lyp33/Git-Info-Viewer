# Build Parameters - Versions Field Enhancement

## Overview
Modified the Build Parameters dialog to always display the `versions` parameter as a text field (instead of dropdown) and ensure it's properly pre-filled during rebuild operations.

## Problem Description

### Issue 1: Versions Field Type
The `versions` parameter was being displayed as a dropdown/combobox (ChoiceParameterDefinition) instead of an editable text field, making it difficult to enter custom version strings.

### Issue 2: Rebuild Not Pre-filling Versions
When clicking "Rebuild" on a previous build, the `versions` field was not showing the original build's version information, requiring manual re-entry.

## Solution

### 1. Force Versions Parameter to Text Field

**Implementation:**
- Added special handling in `createInputComponent()` method
- Check if parameter name is "versions"
- If yes, always create a `JTextField` regardless of the parameter's defined type
- This allows free-form text entry for version strings

**Code Changes:**
```java
// Special handling: versions parameter always uses text field (even if defined as Choice)
if ("versions".equals(param.getName())) {
    JTextField textField = new JTextField();
    textField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    
    String value = getParameterValue(param);
    if (value != null) {
        textField.setText(value);
    }
    
    textField.setPreferredSize(new Dimension(400, 28));
    return textField;
}
```

### 2. Ensure Rebuild Pre-fills Versions

**How It Works:**
The rebuild functionality already fetches all parameters from the original build using `fetchBuildParametersForRebuild()`. The parameters are passed to the Build Parameters dialog as `prefilledParameters`.

**Parameter Priority:**
The `getParameterValue()` method checks parameters in this order:
1. **Prefilled parameters** (from rebuild) - highest priority
2. **Default values** (from parameter definition) - fallback

**Code Flow:**
```java
// In JenkinsJobDetailsDialog.rebuildSelectedBuild()
Map<String, String> parameters = apiClient.fetchBuildParametersForRebuild(jobPath, buildNumber);

// Pass to dialog
JenkinsBuildParametersDialog dialog = new JenkinsBuildParametersDialog(
    parent, apiClient, jobPath, jobName, parameters);  // ← parameters includes versions

// In JenkinsBuildParametersDialog.getParameterValue()
if (prefilledParameters != null && prefilledParameters.containsKey(param.getName())) {
    return prefilledParameters.get(param.getName());  // ← Returns versions value
}
```

## Technical Details

### Modified Files

**JenkinsBuildParametersDialog.java**
- Modified `createInputComponent()` method
- Added special case for "versions" parameter
- Forces text field creation regardless of parameter type

### Parameter Extraction

The `fetchBuildParametersForRebuild()` method in `JenkinsApiClient` extracts all parameters from a build:

```java
// API call
GET /{jobPath}/{buildNumber}/api/json?tree=actions[parameters[name,value]]

// Extracts all parameters including:
// - BRANCH
// - versions
// - TENANT_NAME
// - mail_list
// - etc.
```

### UI Behavior

**Before:**
```
versions: [Dropdown with limited choices]
          ▼
```

**After:**
```
versions: [___________________________]  ← Editable text field
```

**Rebuild Behavior:**

**Before:**
```
versions: [___________________________]  ← Empty field
```

**After:**
```
versions: [24.08_thailife_devsdk_v0.056]  ← Pre-filled from original build
```

## Benefits

1. **Flexible Version Entry** - Users can type any version string
2. **Faster Rebuilds** - No need to re-enter version information
3. **Reduced Errors** - Pre-filled values reduce typos
4. **Better UX** - Consistent with other text parameters

## Testing Checklist

✅ Versions field displays as text field (not dropdown)
✅ Versions field is editable
✅ Build with Parameters shows empty versions field initially
✅ Rebuild pre-fills versions field with original value
✅ Rebuild pre-fills all other parameters correctly
✅ Can modify pre-filled versions value before building
✅ Application compiles successfully

## Example Usage

### Scenario 1: New Build
1. Click "Build with Parameters"
2. See versions field as empty text box
3. Type custom version: "24.08_thailife_devsdk_v0.057"
4. Click Build

### Scenario 2: Rebuild
1. Right-click on build #243 (versions: 24.08_thailife_devsdk_v0.056)
2. Click "Rebuild"
3. See versions field pre-filled with "24.08_thailife_devsdk_v0.056"
4. Optionally modify the version
5. Click Build

## Files Modified

1. `src/main/java/com/gitviewer/JenkinsBuildParametersDialog.java`
   - Modified `createInputComponent()` method
   - Added special handling for "versions" parameter

## Related Components

- `JenkinsApiClient.fetchBuildParametersForRebuild()` - Extracts parameters from build
- `JenkinsJobDetailsDialog.rebuildSelectedBuild()` - Initiates rebuild with parameters
- `JenkinsBuildParameter` - Parameter definition model

## Build Information

**Build Command:** `mvn clean package`
**Build Status:** SUCCESS
**Output JAR:** `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
**Build Time:** 15.873s

## Completion Date
January 18, 2026
