# Stage Level Build ID 研究

## 问题描述

用户想要获取 **Stage 级别的 Build ID**（如图片中的 #809），而不是整体的 Build ID (#243)。

## 场景说明

```
Pipeline Build #243
├── Stage: gemini-pa-bs-parent
│   └── Triggered Job Build #809  ← 这个 ID
├── Stage: bff-parent
│   └── Triggered Job Build #810
└── Stage: common-bff
    └── Triggered Job Build #811
```

## Jenkins Pipeline 架构

### 两种 Stage 类型

1. **内联 Stage** (Inline Stage)
   - 在同一个 Pipeline 中执行
   - 没有独立的 Build ID
   - 只有 Stage ID（如 "6", "11"）

2. **触发式 Stage** (Triggered Job Stage)
   - 触发另一个独立的 Jenkins Job
   - 有独立的 Build ID（如 #809）
   - 通常用于调用其他项目

## 如何获取 Stage 的 Build ID

### 方法 1: 检查 wfapi/describe 响应

查看 API 返回的完整数据结构：

```bash
GET {jenkinsUrl}/{jobPath}/{buildNumber}/wfapi/describe
```

**可能包含的字段**:
```json
{
  "stages": [
    {
      "id": "6",
      "name": "gemini-pa-bs-parent",
      "status": "SUCCESS",
      "durationMillis": 39000,
      "startTimeMillis": 1737189145000,
      
      // 可能的字段（需要验证）
      "execNode": "...",
      "links": {
        "self": {
          "href": "/job/gemini/job/Manual-Build/243/execution/node/6/"
        }
      },
      
      // 如果是触发的 Job
      "downstreamBuilds": [
        {
          "jobName": "gemini-pa-bs-parent",
          "buildNumber": 809,
          "url": "/job/gemini-pa-bs-parent/809/"
        }
      ]
    }
  ]
}
```

### 方法 2: 使用 Stage 详细信息 API

```bash
GET {jenkinsUrl}/{jobPath}/{buildNumber}/execution/node/{stageId}/wfapi/describe
```

这个 API 可能返回更详细的 Stage 信息，包括触发的 Job 信息。

### 方法 3: 解析 Stage Log

从 Stage 的日志中提取 Build ID：

```bash
GET {jenkinsUrl}/{jobPath}/{buildNumber}/execution/node/{stageId}/wfapi/log
```

日志中可能包含：
```
Starting building: gemini » Manual-Build » thailifesdk » 24.08_thailife_dev » CI-Robot » Dependency-CI-ROBOT of #809
```

使用正则表达式提取：
```java
Pattern pattern = Pattern.compile("of #(\\d+)");
Matcher matcher = pattern.matcher(logText);
if (matcher.find()) {
    String stageBuildId = matcher.group(1);  // "809"
}
```

### 方法 4: 使用 Jenkins Blue Ocean API

Blue Ocean 提供了更现代的 API：

```bash
GET {jenkinsUrl}/blue/rest/organizations/jenkins/pipelines/{jobPath}/runs/{buildNumber}/nodes/{stageId}/
```

可能返回：
```json
{
  "id": "6",
  "displayName": "gemini-pa-bs-parent",
  "result": "SUCCESS",
  "state": "FINISHED",
  "durationInMillis": 39000,
  "startTime": "2024-01-18T13:52:26.000+0800",
  
  // 可能包含触发的 Job 信息
  "causeOfBlockage": null,
  "edges": [
    {
      "id": "809",
      "type": "DOWNSTREAM"
    }
  ]
}
```

## 实现建议

### 步骤 1: 增强 wfapi/describe 解析

修改 `JenkinsApiClient.fetchBuildStages()` 方法：

```java
public List<JenkinsStage> fetchBuildStages(String jobPath, int buildNumber) throws IOException {
    String apiUrl = baseUrl + "/" + jobPath + "/" + buildNumber + "/wfapi/describe";
    String response = sendGetRequest(apiUrl);
    JSONObject json = new JSONObject(response);
    
    List<JenkinsStage> stages = new ArrayList<>();
    
    if (json.has("stages")) {
        JSONArray stagesArray = json.getJSONArray("stages");
        
        for (int i = 0; i < stagesArray.length(); i++) {
            JSONObject stageJson = stagesArray.getJSONObject(i);
            
            String id = stageJson.optString("id", "");
            String name = stageJson.optString("name", "");
            String status = stageJson.optString("status", "");
            long durationMillis = stageJson.optLong("durationMillis", 0);
            long startTimeMillis = stageJson.optLong("startTimeMillis", 0);
            
            JenkinsStage stage = new JenkinsStage(name, status, durationMillis);
            stage.setId(id);
            stage.setStartTimeMillis(startTimeMillis);
            
            // 尝试获取 Stage 的 Build ID
            if (stageJson.has("downstreamBuilds")) {
                JSONArray downstreamBuilds = stageJson.getJSONArray("downstreamBuilds");
                if (downstreamBuilds.length() > 0) {
                    JSONObject downstream = downstreamBuilds.getJSONObject(0);
                    int stageBuildNumber = downstream.optInt("buildNumber", 0);
                    stage.setStageBuildNumber(stageBuildNumber);
                }
            }
            
            // 或者从 links 中提取
            if (stageJson.has("links")) {
                JSONObject links = stageJson.getJSONObject("links");
                // 解析 links 获取 Build ID
            }
            
            stages.add(stage);
        }
    }
    
    return stages;
}
```

### 步骤 2: 从 Stage Log 中提取

如果 API 不提供，从日志中提取：

```java
public Integer extractStageBuildId(String stageLog) {
    // 匹配模式: "of #809" 或 "building: ... #809"
    Pattern pattern = Pattern.compile("(?:of|building:.*?)\\s*#(\\d+)");
    Matcher matcher = pattern.matcher(stageLog);
    
    if (matcher.find()) {
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    return null;
}
```

在加载 Stage Log 时调用：

```java
private void loadStageLogToConsole(JenkinsStage stage) {
    SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
        @Override
        protected String doInBackground() throws Exception {
            return apiClient.fetchStageLog(jobPath, buildNumber, stage.getId());
        }

        @Override
        protected void done() {
            try {
                String log = get();
                
                // 尝试提取 Stage Build ID
                Integer stageBuildId = extractStageBuildId(log);
                if (stageBuildId != null) {
                    stage.setStageBuildNumber(stageBuildId);
                    logToConsole("Detected Stage Build ID: #" + stageBuildId);
                }
                
                // 显示日志
                if (externalConsoleLogArea != null) {
                    externalConsoleLogArea.setText(log);
                }
            } catch (Exception e) {
                logToConsole("ERROR: " + e.getMessage());
            }
        }
    };
    
    worker.execute();
}
```

### 步骤 3: 更新 JenkinsStage 类

```java
public class JenkinsStage {
    private String id;
    private String name;
    private String status;
    private long durationMillis;
    private long startTimeMillis;
    private Integer stageBuildNumber;  // 新增：Stage 的 Build ID（如 809）
    
    public Integer getStageBuildNumber() {
        return stageBuildNumber;
    }
    
    public void setStageBuildNumber(Integer stageBuildNumber) {
        this.stageBuildNumber = stageBuildNumber;
    }
    
    public String getStageBuildDisplay() {
        if (stageBuildNumber != null && stageBuildNumber > 0) {
            return "#" + stageBuildNumber;
        }
        return "";
    }
}
```

### 步骤 4: 在 UI 中显示

```java
private class StageListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(...) {
        super.getListCellRendererComponent(...);
        
        if (value instanceof JenkinsStage) {
            JenkinsStage stage = (JenkinsStage) value;
            
            StringBuilder displayText = new StringBuilder();
            displayText.append(stage.getName());
            displayText.append(" (").append(stage.getFormattedDuration()).append(")");
            
            // 如果有 Stage Build ID，显示它
            if (stage.getStageBuildNumber() != null) {
                displayText.append(" - Build #").append(stage.getStageBuildNumber());
            }
            
            setText(displayText.toString());
        }
        
        return this;
    }
}
```

## 测试方案

### 1. 检查真实 Jenkins API

访问真实的 Jenkins 实例，查看 API 返回的完整数据：

```bash
curl -u username:token \
  "http://jenkins-server/job/gemini/job/Manual-Build/243/wfapi/describe" \
  | jq '.'
```

查找是否有 `downstreamBuilds`, `links`, `edges` 等字段。

### 2. 检查 Stage 详细信息 API

```bash
curl -u username:token \
  "http://jenkins-server/job/gemini/job/Manual-Build/243/execution/node/6/wfapi/describe" \
  | jq '.'
```

### 3. 分析 Stage Log

```bash
curl -u username:token \
  "http://jenkins-server/job/gemini/job/Manual-Build/243/execution/node/6/wfapi/log"
```

查找日志中的 Build ID 模式。

## 结论

**Stage 级别的 Build ID 可能通过以下方式获取**:

1. ✅ **从 wfapi/describe 的扩展字段**（如果 Jenkins 提供）
2. ✅ **从 Stage 详细信息 API**（`/execution/node/{stageId}/wfapi/describe`）
3. ✅ **从 Stage Log 中解析**（最可靠的方法）
4. ✅ **使用 Blue Ocean API**（如果可用）

**推荐实现顺序**:
1. 先尝试从 API 响应中获取（最干净）
2. 如果 API 不提供，从 Stage Log 中解析（最可靠）
3. 在 UI 中显示 Stage Build ID

## 下一步

需要你提供：
1. 真实 Jenkins 的 `wfapi/describe` 完整响应
2. 或者 Stage Log 的示例内容

这样我可以确定最佳的获取方式并实现代码。
