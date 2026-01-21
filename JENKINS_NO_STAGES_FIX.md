# Jenkins Build Without Stages - Fix Complete

## Issue Description

Some Jenkins build history records ARE themselves a stage (no sub-stages). When clicking such a history record, the right-side Stage View section was empty because the API returned an empty stages list.

## Solution

Implemented detection for builds without stages and created a synthetic stage to represent the build itself.

## Changes Made

### 1. JenkinsJobDetailsDialog.java

Modified the `loadStageView()` method to detect when a build has no stages and create a synthetic stage:

```java
// 特殊情况：如果没有 stages，说明这个 build 本身就是一个 stage
// 创建一个合成的 stage 来代表这个 build
if (stages.isEmpty()) {
    logToConsole("No modules found - this build IS itself a stage");
    logToConsole("Creating synthetic stage for build #" + build.getNumber());
    
    JenkinsStage syntheticStage = new JenkinsStage();
    syntheticStage.setName("Build #" + build.getNumber());
    syntheticStage.setId("build-" + build.getNumber());  // 使用特殊的 ID 格式
    
    // 设置状态（从 build 的 result 映射到 stage 的 status）
    if (build.getResult() != null) {
        syntheticStage.setStatus(build.getResult());
    } else {
        syntheticStage.setStatus("IN_PROGRESS");
    }
    
    // 持续时间设置为 0（因为我们没有这个信息）
    syntheticStage.setDurationMillis(0);
    
    stages = new java.util.ArrayList<>();
    stages.add(syntheticStage);
    
    logToConsole("Created synthetic stage: " + syntheticStage.getName() + 
               ", Status: " + syntheticStage.getStatus());
}
```

**Key Points:**
- Synthetic stages use a special ID format: `"build-" + buildNumber`
- Status is mapped from the build's result
- Duration is set to 0 (not available)
- The synthetic stage is added to the stages list for display

### 2. JenkinsStageViewPanel.java

Updated `loadStageLogToConsole()` to handle synthetic stages:

```java
// 检查是否是合成的 stage（build 本身）
if (stage.getId() != null && stage.getId().startsWith("build-")) {
    logToConsole("Detected synthetic stage - loading build console log");
    return apiClient.fetchBuildConsoleLog(jobPath, buildNumber);
} else {
    // 使用新的 API：传入 stageName
    return apiClient.fetchStageLog(jobPath, buildNumber, stage.getId(), stage.getName());
}
```

Updated `openStageLogDialog()` to handle synthetic stages:

```java
// 检查是否是合成的 stage（build 本身）
if (stageId.startsWith("build-")) {
    logToConsole("Detected synthetic stage - this is a build without sub-stages");
    logToConsole("Opening build console log dialog instead of stage log dialog");
    
    // 对于合成 stage，直接显示 build 的 console log
    // 我们可以重用 JenkinsStageLogDialog，但传入特殊标记
    try {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        logToConsole("Creating JenkinsStageLogDialog for synthetic stage...");
        
        JenkinsStageLogDialog dialog = new JenkinsStageLogDialog(
            parentWindow, apiClient, jobPath, buildNumber, stage);
        
        logToConsole("Dialog created successfully");
        dialog.setVisible(true);
        logToConsole("Dialog closed");
    } catch (Throwable e) {
        logToConsole("ERROR: Exception while opening dialog: " + e.getClass().getName() + ": " + e.getMessage());
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, 
            "Failed to open module log dialog: " + e.getMessage(), 
            "Error", 
            JOptionPane.ERROR_MESSAGE);
    }
    return;
}
```

### 3. JenkinsStageLogDialog.java

Updated `loadJenkinsLog()` to handle synthetic stages:

```java
// 检查是否是合成的 stage（build 本身）
if (stage.getId() != null && stage.getId().startsWith("build-")) {
    System.out.println("[StageLogDialog] Detected synthetic stage - loading build console log directly");
    String buildLog = apiClient.fetchBuildConsoleLog(jobPath, buildNumber);
    System.out.println("[StageLogDialog] Build console log fetched, length: " + (buildLog != null ? buildLog.length() : 0));
    
    // 缓存 build log 供 Portal Log 使用
    cachedStageLog = buildLog;
    System.out.println("[StageLogDialog] Cached build log for Portal Log");
    
    return buildLog;
}
```

## How It Works

1. **Detection**: When `fetchBuildStages()` returns an empty list, the system detects this is a build without sub-stages
2. **Synthetic Stage Creation**: A synthetic stage is created with:
   - Name: "Build #[buildNumber]"
   - ID: "build-[buildNumber]" (special format for identification)
   - Status: Mapped from build result
   - Duration: 0 (not available)
3. **Display**: The synthetic stage appears in the Stage View panel like a normal stage
4. **Log Loading**: When clicked, the system detects the "build-" prefix in the ID and loads the build's console log directly instead of trying to fetch a stage log
5. **Dialog**: Double-clicking opens the stage log dialog, which also detects the synthetic stage and loads the build console log

## User Experience

- Builds without stages now display a single stage entry: "Build #[number]"
- Clicking the synthetic stage loads the build's console log
- Double-clicking opens the log dialog with the full build console log
- The status color matches the build result (green for success, red for failure, etc.)
- All existing functionality for normal stages remains unchanged

## Testing

Build and run the application:
```bash
mvn clean package
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

Test scenarios:
1. Open a Jenkins job with normal stages - should work as before
2. Open a Jenkins job where builds have no stages - should now show "Build #[number]" in Stage View
3. Click the synthetic stage - should load build console log
4. Double-click the synthetic stage - should open log dialog with build console log

## Files Modified

- `src/main/java/com/gitviewer/JenkinsJobDetailsDialog.java`
- `src/main/java/com/gitviewer/JenkinsStageViewPanel.java`
- `src/main/java/com/gitviewer/JenkinsStageLogDialog.java`

## Build Status

✅ Compilation successful
✅ Package created: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
