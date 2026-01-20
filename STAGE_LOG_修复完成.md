# Stage Log 获取方式修复完成

## 问题

原来使用的 API 不正确：
```
GET {jenkinsUrl}/{jobPath}/{buildNumber}/execution/node/{stageId}/wfapi/log
```

这个 API 返回的是元数据，而不是实际的日志内容：
```json
{
  "nodeId": "43",
  "nodeStatus": "SUCCESS",
  "length": 0,
  "hasMore": false,
  "consoleUrl": null
}
```

## 解决方案

### 新的实现方式

使用 `consoleText` API 获取完整的 Console Log，然后根据 Stage 名称提取对应的日志段落。

### API 端点

```
GET {jenkinsUrl}/{jobPath}/{buildNumber}/consoleText
```

**示例：**
```
GET http://172.25.32.166:8080/job/gemini/job/Manual-Build/job/thailifesdk/job/24.08_thailife_dev/job/all-in-one-auto-CV/243/consoleText
```

**返回：** 纯文本格式的完整 Console Log

## 代码修改

### 1. JenkinsApiClient.java

#### 添加缓存机制
```java
// 缓存 Console Log 以提高性能
private String cachedConsoleLog = null;
private int cachedBuildNumber = -1;
private String cachedJobPath = null;
```

#### 新增方法：fetchBuildConsoleLog
```java
/**
 * 获取构建的完整 Console Log
 */
public String fetchBuildConsoleLog(String jobPath, int buildNumber) throws IOException {
    String apiUrl = baseUrl + "/" + jobPath + "/" + buildNumber + "/consoleText";
    
    logger.info("=== Fetching Build Console Log ===");
    logger.info("API URL: {}", apiUrl);
    
    try {
        String response = sendGetRequest(apiUrl);
        logger.info("Console log fetched successfully, length: {}", response.length());
        return response;
    } catch (IOException e) {
        logger.error("Failed to fetch console log: {}", e.getMessage());
        throw new IOException("Failed to fetch console log: " + e.getMessage());
    }
}
```

#### 新增方法：extractStageLogFromConsole
```java
/**
 * 从完整的 Console Log 中提取特定 Stage 的日志
 */
public String extractStageLogFromConsole(String fullConsoleLog, String stageName) {
    if (fullConsoleLog == null || fullConsoleLog.isEmpty() || stageName == null) {
        return "";
    }
    
    // 查找 Stage 开始标记：[Pipeline] { (stageName)
    String stageStartMarker = "[Pipeline] { (" + stageName + ")";
    int startIndex = fullConsoleLog.indexOf(stageStartMarker);
    
    if (startIndex == -1) {
        logger.warn("Stage start marker not found for: {}", stageName);
        return "";
    }
    
    // 从 Stage 名称之前开始（包含 [Pipeline] stage）
    int stageLineStart = fullConsoleLog.lastIndexOf("[Pipeline] stage", startIndex);
    if (stageLineStart != -1) {
        startIndex = stageLineStart;
    }
    
    // 查找 Stage 结束标记：[Pipeline] // stage
    String stageEndMarker = "[Pipeline] // stage";
    int endIndex = fullConsoleLog.indexOf(stageEndMarker, startIndex);
    
    if (endIndex == -1) {
        logger.warn("Stage end marker not found for: {}", stageName);
        return fullConsoleLog.substring(startIndex);
    }
    
    // 包含结束标记
    endIndex = fullConsoleLog.indexOf("\n", endIndex);
    if (endIndex == -1) {
        endIndex = fullConsoleLog.length();
    }
    
    String stageLog = fullConsoleLog.substring(startIndex, endIndex);
    logger.info("Extracted stage log for '{}', length: {}", stageName, stageLog.length());
    return stageLog;
}
```

#### 更新方法：fetchStageLog
```java
/**
 * 获取 Stage 日志（使用缓存优化）
 */
public String fetchStageLog(String jobPath, int buildNumber, String stageId, String stageName) throws IOException {
    logger.info("=== Fetching Stage Log ===");
    logger.info("Job Path: {}", jobPath);
    logger.info("Build Number: {}", buildNumber);
    logger.info("Stage Name: {}", stageName);
    
    // 检查缓存
    if (cachedBuildNumber == buildNumber && 
        jobPath.equals(cachedJobPath) && 
        cachedConsoleLog != null) {
        logger.info("Using cached console log");
        return extractStageLogFromConsole(cachedConsoleLog, stageName);
    }
    
    // 获取完整的 Console Log
    logger.info("Fetching full console log...");
    String fullLog = fetchBuildConsoleLog(jobPath, buildNumber);
    
    // 缓存
    cachedConsoleLog = fullLog;
    cachedBuildNumber = buildNumber;
    cachedJobPath = jobPath;
    
    // 提取特定 Stage 的日志
    return extractStageLogFromConsole(fullLog, stageName);
}
```

### 2. JenkinsStageViewPanel.java

#### 更新 loadStageLogToConsole 方法
```java
private void loadStageLogToConsole(JenkinsStage stage) {
    if (apiClient == null || stage.getName() == null || stage.getName().isEmpty()) {
        logToConsole("Cannot load module log: missing API client or module name");
        return;
    }
    
    logToConsole("Loading log for module: " + stage.getName());
    
    SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
        @Override
        protected String doInBackground() throws Exception {
            // 使用新的 API：传入 stageName
            return apiClient.fetchStageLog(jobPath, buildNumber, stage.getId(), stage.getName());
        }

        @Override
        protected void done() {
            try {
                String log = get();
                
                // 尝试从日志中提取 Stage Build ID
                Integer stageBuildId = apiClient.extractStageBuildId(log);
                if (stageBuildId != null) {
                    stage.setStageBuildNumber(stageBuildId);
                    logToConsole("✓ Detected Stage Build ID: #" + stageBuildId);
                    stageList.repaint();
                }
                
                logToConsole("=== Module Log: " + stage.getName() + " ===");
                logToConsole(log);
                logToConsole("=== End of Module Log ===");
            } catch (Exception e) {
                logToConsole("ERROR: Failed to load module log: " + e.getMessage());
                e.printStackTrace();
            }
        }
    };
    
    worker.execute();
}
```

## Console Log 格式

### 完整的 Console Log 示例
```
[Pipeline] Start of Pipeline
[Pipeline] node
Running on Jenkins in /var/jenkins_home/workspace/my-pipeline
[Pipeline] {
[Pipeline] stage
[Pipeline] { (gemini-pa-bs-parent)
[Pipeline] build
Scheduling project: gemini-pa-bs-parent
Starting building: gemini-pa-bs-parent of #809
Build gemini-pa-bs-parent #809 completed: SUCCESS
[Pipeline] }
[Pipeline] // stage
[Pipeline] stage
[Pipeline] { (bff-parent)
[Pipeline] build
Scheduling project: bff-parent
Starting building: bff-parent of #810
Build bff-parent #810 completed: SUCCESS
[Pipeline] }
[Pipeline] // stage
[Pipeline] }
[Pipeline] End of Pipeline
Finished: SUCCESS
```

### 提取的 Stage Log 示例
```
[Pipeline] stage
[Pipeline] { (gemini-pa-bs-parent)
[Pipeline] build
Scheduling project: gemini-pa-bs-parent
Starting building: gemini-pa-bs-parent of #809
Build gemini-pa-bs-parent #809 completed: SUCCESS
[Pipeline] }
[Pipeline] // stage
```

## 性能优化

### 缓存机制

为了避免每次点击 Stage 都重新获取完整的 Console Log，实现了缓存机制：

1. **首次请求**：获取完整的 Console Log 并缓存
2. **后续请求**：如果是同一个 Build，直接从缓存中提取 Stage Log
3. **缓存失效**：当切换到不同的 Build 时，缓存自动失效

### 缓存效果

- ✅ 第一次点击 Stage：需要获取完整 Console Log（可能较慢）
- ✅ 后续点击其他 Stage：直接从缓存提取（非常快）
- ✅ 切换到不同 Build：自动重新获取并缓存

## 工作流程

1. 用户双击 Build History 中的某个构建
2. 系统加载并显示 Stage 列表
3. 用户点击某个 Stage
4. 系统获取完整的 Console Log（首次）或使用缓存（后续）
5. 系统从 Console Log 中提取该 Stage 的日志段落
6. 系统从日志中提取 Build ID
7. UI 显示 Stage Log 和 Build ID

## 编译状态

✅ 编译成功 (mvn compile)

## 测试步骤

1. 关闭正在运行的应用程序
2. 运行 `mvn clean package` 重新打包
3. 启动应用程序
4. 打开 Jenkins Browser
5. 双击某个 Build
6. 点击 Stage 列表中的某个 Stage
7. 观察 Console Log 是否显示正确的日志内容
8. 观察是否提取到 Build ID

## 预期结果

- ✅ Console Log 显示该 Stage 的完整日志
- ✅ 日志中包含 "Starting building: ... of #809" 等信息
- ✅ 系统自动提取并显示 Build ID
- ✅ Stage 列表显示：`Stage名称 (持续时间) - Build #809`
- ✅ 后续点击其他 Stage 速度很快（使用缓存）

## 注意事项

1. **首次加载可能较慢**：因为需要获取完整的 Console Log
2. **后续加载很快**：使用缓存机制
3. **日志格式依赖**：依赖 Jenkins Pipeline 的标准日志格式
4. **Build ID 提取**：依赖日志中包含 "#数字" 格式

## 相关文档

- `STAGE_LOG_API_研究.md` - API 研究过程和方案对比
- `如何获取Stage_Log.md` - 原始文档（已过时）
- `STAGE_BUILD_ID_SOLUTION.md` - Build ID 提取方案
