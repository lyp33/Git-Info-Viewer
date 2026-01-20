# Favorites Recursive Loading Fix

## Problem
When double-clicking a favorited job, the system only loaded the first level of subdirectories and stopped. For deeply nested jobs like `job/gemini1/job/Manual-Build/job/tools_lock/job/update-bs-bff-version`, it would only load `gemini1` and fail to continue loading deeper levels.

## Root Cause
In `JenkinsBrowserDialog.findNodeByJobNames()`, the method would:
1. Load children of current node
2. Find matching child at index N
3. Immediately recurse to index N+1
4. **BUT** it never loaded the children of the matched node before recursing

This meant:
- Level 0 (root) → children loaded ✓
- Level 1 (gemini1) → found ✓, but children NOT loaded ✗
- Level 2 (Manual-Build) → search fails because children weren't loaded ✗

## Solution
Modified `findNodeByJobNames()` to ensure that when a matching child is found, its children are loaded BEFORE recursing to the next level:

```java
if (item.getName().equals(targetName)) {
    logToConsole("  Found match! Continuing to next level...");
    
    // KEY FIX: Load children of matched node before recursing
    if (index + 1 < jobNames.size()) {
        logToConsole("  Loading children of matched node before recursing...");
        ensureChildrenLoaded(child);
    }
    
    // Now recurse to next level
    return findNodeByJobNames(child, jobNames, index + 1);
}
```

## Result
Now the recursive loading works correctly:
1. Load root children → find gemini1
2. Load gemini1 children → find Manual-Build
3. Load Manual-Build children → find tools_lock
4. Load tools_lock children → find update-bs-bff-version
5. Successfully navigate to target job ✓

## Files Modified
- `src/main/java/com/gitviewer/JenkinsBrowserDialog.java`

## Testing
Double-click any favorited job with deep nesting. The system will:
1. Show "Loading..." dialog immediately
2. Recursively load ALL subdirectory levels
3. Navigate to and highlight the target job
4. Close the loading dialog

## Build
```bash
mvn clean package
```

Generated JAR: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
