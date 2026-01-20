# 如何获取 Stage Log

## API 端点

### Jenkins Workflow API
```
GET {jenkinsUrl}/{jobPath}/{buildNumber}/execution/node/{stageId}/wfapi/log
```

## 参数说明

| 参数 | 说明 | 示例 |
|------|------|------|
| `jenkinsUrl` | Jenkins 服务器地址 | `http://localhost:8888` |
| `jobPath` | Job 路径 | `job/folder1/job/my-pipeline` |
| `buildNumber` | Build 编号 | `243` |
| `stageId` | Stage ID（从 wfapi/describe 获取） | `6` |

## 完整示例

### 1. 获取 Stage 列表（获取 Stage ID）

**API 请求：**
```
GET http://localhost:8888/job/folder1/job/my-pipeline/243/wfapi/describe
```

**响应示例：**
```json
{
  "id": "243",
  "name": "my-pipeline #243",
  "status": "SUCCESS",
  "stages": [
    {
      "id": "6",
      "name": "gemini-pa-bs-parent",
      "status": "SUCCESS",
      "durationMillis": 39000
    },
    {
      "id": "7",
      "name": "bff-parent",
      "status": "SUCCESS",
      "durationMillis": 55000
    }
  ]
}
```

### 2. 获取特定 Stage 的日志

**API 请求：**
```
GET http://localhost:8888/job/folder1/job/my-pipeline/243/execution/node/6/wfapi/log
```

**响应示例（纯文本）：**
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

## 代码实现

### JenkinsApiClient.java

```java
/**
 * 获取 Stage 日志
 */
public String fetchStageLog(String jobPath, int buildNumber, String stageId) throws IOException {
    // 构建 API URL
    String apiUrl = baseUrl + "/" + jobPath + "/" + buildNumber + "/execution/node/" + stageId + "/wfapi/log";
    
    logger.info("=== Fetching Stage Log ===");
    logger.info("API URL: {}", apiUrl);
    
    try {
        // 发送 GET 请求
        String response = sendGetRequest(apiUrl);
        logger.info("Stage log fetched successfully, length: {}", response.length());
        return response;
    } catch (IOException e) {
        logger.error("Failed to fetch stage log: {}", e.getMessage());
        throw new IOException("Failed to fetch stage log: " + e.getMessage());
    }
}
```

### JenkinsStageViewPanel.java

```java
/**
 * 加载 Stage 日志到控制台
 */
private void loadStageLogToConsole(JenkinsStage stage) {
    if (apiClient == null || stage.getId() == null || stage.getId().isEmpty()) {
        logToConsole("Cannot load module log: missing API client or module ID");
        return;
    }
    
    logToConsole("Loading log for module: " + stage.getName());
    
    SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
        @Override
        protected String doInBackground() throws Exception {
            // 调用 API 获取日志
            return apiClient.fetchStageLog(jobPath, buildNumber, stage.getId());
        }

        @Override
        protected void done() {
            try {
                String log = get();
                
                // 从日志中提取 Build ID
                Integer stageBuildId = apiClient.extractStageBuildId(log);
                if (stageBuildId != null) {
                    stage.setStageBuildNumber(stageBuildId);
                    logToConsole("✓ Detected Stage Build ID: #" + stageBuildId);
                    stageList.repaint();
                }
                
                // 显示日志
                logToConsole("=== Module Log: " + stage.getName() + " ===");
                logToConsole(log);
                logToConsole("=== End of Module Log ===");
            } catch (Exception e) {
                logToConsole("ERROR: Failed to load module log: " + e.getMessage());
            }
        }
    };
    
    worker.execute();
}
```

## URL 构建规则

### 基本规则
```
{baseUrl}/{jobPath}/{buildNumber}/execution/node/{stageId}/wfapi/log
```

### 嵌套 Job 的路径处理

对于嵌套的 Job，路径需要用 `/job/` 分隔：

**示例 1：顶层 Job**
- Job 名称：`my-pipeline`
- Job 路径：`job/my-pipeline`
- 完整 URL：`http://jenkins/job/my-pipeline/243/execution/node/6/wfapi/log`

**示例 2：一层嵌套**
- Folder：`folder1`
- Job 名称：`my-pipeline`
- Job 路径：`job/folder1/job/my-pipeline`
- 完整 URL：`http://jenkins/job/folder1/job/my-pipeline/243/execution/node/6/wfapi/log`

**示例 3：多层嵌套**
- 路径：`folder1/folder2/my-pipeline`
- Job 路径：`job/folder1/job/folder2/job/my-pipeline`
- 完整 URL：`http://jenkins/job/folder1/job/folder2/job/my-pipeline/243/execution/node/6/wfapi/log`

## 认证

### Basic Authentication

```java
private String getAuthHeader() {
    if (username == null || username.isEmpty() || apiToken == null || apiToken.isEmpty()) {
        return null;
    }
    String auth = username + ":" + apiToken;
    return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
}

private String sendGetRequest(String urlString) throws IOException {
    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Accept", "application/json");
    
    // 添加认证头
    String authHeader = getAuthHeader();
    if (authHeader != null) {
        conn.setRequestProperty("Authorization", authHeader);
    }
    
    // ... 处理响应
}
```

## 错误处理

### 常见错误

1. **404 Not Found**
   - 原因：Stage ID 不存在或 URL 构建错误
   - 解决：检查 Stage ID 是否正确，检查 URL 格式

2. **401 Unauthorized**
   - 原因：认证失败
   - 解决：检查用户名和 API Token

3. **403 Forbidden**
   - 原因：没有权限访问
   - 解决：检查用户权限

### 错误处理代码

```java
try {
    String log = apiClient.fetchStageLog(jobPath, buildNumber, stage.getId());
    // 处理日志
} catch (IOException e) {
    logToConsole("ERROR: Failed to load module log: " + e.getMessage());
    // 不影响主流程，继续执行
}
```

## 日志内容示例

### 成功的 Stage Log
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

### 失败的 Stage Log
```
[Pipeline] stage
[Pipeline] { (bff-parent)
[Pipeline] build
Scheduling project: bff-parent
Starting building: bff-parent of #810
Build bff-parent #810 completed: FAILURE
ERROR: Build failed with exit code 1
[Pipeline] }
[Pipeline] // stage
```

## 从日志中提取 Build ID

### 正则表达式
```java
Pattern pattern = Pattern.compile("(?:of|building:.*?)\\s*#(\\d+)");
```

### 匹配示例

| 日志文本 | 匹配结果 |
|---------|---------|
| `Starting building: gemini-pa-bs-parent of #809` | `809` |
| `Build gemini-pa-bs-parent #809 completed` | `809` |
| `CI-Robot of #809` | `809` |
| `building: project #810` | `810` |

### 提取代码
```java
public Integer extractStageBuildId(String stageLog) {
    if (stageLog == null || stageLog.isEmpty()) {
        return null;
    }
    
    Pattern pattern = Pattern.compile("(?:of|building:.*?)\\s*#(\\d+)");
    Matcher matcher = pattern.matcher(stageLog);
    
    if (matcher.find()) {
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse stage build ID: {}", matcher.group(1));
            return null;
        }
    }
    
    return null;
}
```

## 测试

### 使用 Mock Server 测试

Mock Server 需要实现以下端点：

```java
// MockJenkinsServer.java
private void handleStageLog(HttpExchange exchange, String path) throws IOException {
    // 解析路径：/job/xxx/243/execution/node/6/wfapi/log
    // 返回模拟的 Stage Log
    
    String mockLog = "[Pipeline] stage\n" +
                    "[Pipeline] { (gemini-pa-bs-parent)\n" +
                    "[Pipeline] build\n" +
                    "Scheduling project: gemini-pa-bs-parent\n" +
                    "Starting building: gemini-pa-bs-parent of #809\n" +
                    "Build gemini-pa-bs-parent #809 completed: SUCCESS\n" +
                    "[Pipeline] }\n" +
                    "[Pipeline] // stage\n";
    
    sendResponse(exchange, 200, mockLog);
}
```

### 测试步骤

1. 启动 Mock Server
2. 打开 Jenkins Browser
3. 双击某个 Build
4. 点击 Stage 列表中的某个 Stage
5. 观察 Console Log 是否显示日志内容
6. 检查是否提取到 Build ID

## 总结

获取 Stage Log 的完整流程：

1. **获取 Stage 列表** → 得到 Stage ID
   - API: `{jenkinsUrl}/{jobPath}/{buildNumber}/wfapi/describe`

2. **获取 Stage Log** → 得到日志内容
   - API: `{jenkinsUrl}/{jobPath}/{buildNumber}/execution/node/{stageId}/wfapi/log`

3. **提取 Build ID** → 从日志中解析
   - 使用正则表达式：`(?:of|building:.*?)\s*#(\\d+)`

4. **显示结果** → 更新 UI
   - 在 Stage 列表中显示：`Stage名称 (持续时间) - Build #809`
   - 在 Tooltip 中显示 Build ID
