# Stage Log API 研究

## 问题

`wfapi/log` API 返回的不是日志内容，而是日志元数据：

```json
{
  "nodeId": "43",
  "nodeStatus": "SUCCESS",
  "length": 0,
  "hasMore": false,
  "consoleUrl": null
}
```

## 正确的 API

### 方案 1：使用 consoleText API（推荐）

获取整个 Build 的 Console Log，然后过滤出特定 Stage 的内容：

```
GET {jenkinsUrl}/{jobPath}/{buildNumber}/consoleText
```

**示例：**
```
GET http://172.25.32.166:8080/job/gemini/job/Manual-Build/job/thailifesdk/job/24.08_thailife_dev/job/all-in-one-auto-CV/243/consoleText
```

**返回：** 纯文本格式的完整 Console Log

### 方案 2：使用 logText/progressiveText API

获取特定 Workflow Node 的日志：

```
GET {jenkinsUrl}/{jobPath}/{buildNumber}/execution/node/{nodeId}/wfapi/log/text
```

或者：

```
GET {jenkinsUrl}/{jobPath}/{buildNumber}/execution/node/{nodeId}/log
```

### 方案 3：使用 Blue Ocean API

```
GET {jenkinsUrl}/blue/rest/organizations/jenkins/pipelines/{jobPath}/runs/{buildNumber}/nodes/{nodeId}/log/
```

## 推荐方案：使用 consoleText

最简单可靠的方式是获取整个 Build 的 Console Log，然后根据 Stage 名称过滤内容。

### 实现步骤

1. 获取完整的 Console Log
2. 根据 Stage 名称查找对应的日志段落
3. 提取该段落中的 Build ID

### 代码实现

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

/**
 * 从完整的 Console Log 中提取特定 Stage 的日志
 */
public String extractStageLogFromConsole(String fullConsoleLog, String stageName) {
    if (fullConsoleLog == null || fullConsoleLog.isEmpty() || stageName == null) {
        return "";
    }
    
    // 查找 Stage 开始标记
    String stageStartPattern = "\\[Pipeline\\] stage.*?\\{ \\(" + Pattern.quote(stageName) + "\\)";
    String stageEndPattern = "\\[Pipeline\\] // stage";
    
    Pattern startPattern = Pattern.compile(stageStartPattern, Pattern.DOTALL);
    Matcher startMatcher = startPattern.matcher(fullConsoleLog);
    
    if (startMatcher.find()) {
        int startIndex = startMatcher.start();
        
        // 从 Stage 开始位置查找结束标记
        int endIndex = fullConsoleLog.indexOf(stageEndPattern, startIndex);
        if (endIndex > startIndex) {
            endIndex += stageEndPattern.length();
            return fullConsoleLog.substring(startIndex, endIndex);
        }
    }
    
    return "";
}

/**
 * 获取 Stage 日志（新实现）
 */
public String fetchStageLog(String jobPath, int buildNumber, String stageId, String stageName) throws IOException {
    // 获取完整的 Console Log
    String fullLog = fetchBuildConsoleLog(jobPath, buildNumber);
    
    // 提取特定 Stage 的日志
    String stageLog = extractStageLogFromConsole(fullLog, stageName);
    
    if (stageLog.isEmpty()) {
        logger.warn("No log found for stage: {}", stageName);
        return "No log found for stage: " + stageName;
    }
    
    return stageLog;
}
```

### Console Log 格式示例

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

### 提取逻辑

1. 查找 `[Pipeline] stage` 和 `[Pipeline] { (stageName)`
2. 查找下一个 `[Pipeline] // stage`
3. 提取这两个标记之间的内容

## 性能考虑

### 问题
- 每次点击 Stage 都要获取完整的 Console Log（可能很大）

### 优化方案

#### 方案 A：缓存 Console Log
```java
private String cachedConsoleLog = null;
private int cachedBuildNumber = -1;

public String fetchStageLog(String jobPath, int buildNumber, String stageId, String stageName) throws IOException {
    // 如果是同一个 Build，使用缓存
    if (cachedBuildNumber == buildNumber && cachedConsoleLog != null) {
        return extractStageLogFromConsole(cachedConsoleLog, stageName);
    }
    
    // 获取并缓存 Console Log
    cachedConsoleLog = fetchBuildConsoleLog(jobPath, buildNumber);
    cachedBuildNumber = buildNumber;
    
    return extractStageLogFromConsole(cachedConsoleLog, stageName);
}
```

#### 方案 B：预加载 Console Log
在显示 Stage 列表时就预加载 Console Log：

```java
public void displayStages(List<JenkinsStage> stages) {
    stageListModel.clear();
    
    if (stages != null && !stages.isEmpty()) {
        for (JenkinsStage stage : stages) {
            stageListModel.addElement(stage);
        }
        
        // 预加载 Console Log
        preloadConsoleLog();
    }
}

private void preloadConsoleLog() {
    SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
        @Override
        protected String doInBackground() throws Exception {
            return apiClient.fetchBuildConsoleLog(jobPath, buildNumber);
        }

        @Override
        protected void done() {
            try {
                String log = get();
                // 缓存到 apiClient 或本地
                logToConsole("Console log preloaded, length: " + log.length());
            } catch (Exception e) {
                logToConsole("Failed to preload console log: " + e.getMessage());
            }
        }
    };
    
    worker.execute();
}
```

## 总结

**正确的方案：**
1. 使用 `consoleText` API 获取完整的 Console Log
2. 根据 Stage 名称从 Console Log 中提取对应段落
3. 从提取的段落中解析 Build ID

**优点：**
- ✅ API 稳定可靠
- ✅ 返回纯文本，易于解析
- ✅ 包含所有需要的信息

**缺点：**
- ⚠️ 需要获取完整的 Console Log（可能较大）
- ⚠️ 需要解决方案：缓存或预加载

**下一步：**
更新 `JenkinsApiClient.java` 实现新的方案
