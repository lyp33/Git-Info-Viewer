# Portal build_output 嵌套 JSON 解析修复

## 问题描述

应用显示 "No build_output field found in Portal API response"，但使用 Postman 测试同样的 API 可以看到 `build_output` 字段确实存在。

## 原因分析

### Portal API 的 JSON 结构

Portal API 返回的 JSON 结构是**嵌套的**，`build_output` 字段不在根级别，而是在 `callback` 对象内部：

```json
{
  "id": "696e1dd9b547926878e53eab",
  "queue_id": 1775928,
  "app_name": "common-bff",
  "user_name": "thailife",
  "job_name": "thailife/job/common-bff/",
  "image_name": "docker-all.repo.ebaotech.com/thailife/common-bff:24.08_thailife_devsdk_v0.056",
  "callback": {
    "callback_id": "ec305f89-d76d-4f21-b799-f592ffd18eff",
    "build_status": "Build Success",
    "build_output": "[2026-01-19T12:04:47.3642] Started by user ..."
  }
}
```

### 原有代码问题

原有代码只检查根级别的 `build_output`：

```java
JSONObject json = new JSONObject(jsonResponse);

if (json.has("build_output")) {  // ❌ 只检查根级别
    String buildOutput = json.getString("build_output");
    return buildOutput;
}
```

由于 `build_output` 在 `callback` 对象内部，`json.has("build_output")` 返回 `false`，导致找不到字段。

## 解决方案

修改 JSON 解析逻辑，支持两种情况：
1. **根级别**：`build_output` 直接在根对象中
2. **嵌套级别**：`build_output` 在 `callback` 对象中

### 修改内容

**文件**: `src/main/java/com/gitviewer/JenkinsApiClient.java`

**修改前**:
```java
JSONObject json = new JSONObject(jsonResponse);

if (json.has("build_output")) {
    String buildOutput = json.getString("build_output");
    return buildOutput;
} else {
    return "No build_output field found in Portal API response.";
}
```

**修改后**:
```java
JSONObject json = new JSONObject(jsonResponse);

String buildOutput = null;

// 首先检查根级别是否有 build_output
if (json.has("build_output")) {
    buildOutput = json.getString("build_output");
    System.out.println("[JenkinsApiClient] Found build_output at root level");
}
// 如果根级别没有，检查 callback 对象
else if (json.has("callback")) {
    System.out.println("[JenkinsApiClient] Checking callback object for build_output");
    JSONObject callback = json.getJSONObject("callback");
    if (callback.has("build_output")) {
        buildOutput = callback.getString("build_output");
        System.out.println("[JenkinsApiClient] Found build_output in callback object");
    }
}

if (buildOutput != null) {
    // 处理 build_output
    return buildOutput;
} else {
    return "No build_output field found in Portal API response (checked root and callback).";
}
```

## 解析流程

1. **解析 JSON 响应**：`JSONObject json = new JSONObject(jsonResponse)`
2. **检查根级别**：`json.has("build_output")`
   - 如果存在：直接获取 `json.getString("build_output")`
   - 如果不存在：继续下一步
3. **检查 callback 对象**：`json.has("callback")`
   - 如果存在：获取 callback 对象 `json.getJSONObject("callback")`
   - 检查 callback 中是否有 build_output：`callback.has("build_output")`
   - 如果存在：获取 `callback.getString("build_output")`
4. **返回结果**：
   - 如果找到 `build_output`：返回内容
   - 如果未找到：返回错误消息

## 调试日志

### 成功找到（在 callback 中）：
```
[JenkinsApiClient] Portal API response length: 1234
[JenkinsApiClient] Portal API response preview: {"id":"696e1dd9b547926878e53eab","queue_id":1775928,"app_name":"common-bff",...
[JenkinsApiClient] Checking callback object for build_output
[JenkinsApiClient] Found build_output in callback object
[JenkinsApiClient] Extracted build_output, length: 5678
```

### 成功找到（在根级别）：
```
[JenkinsApiClient] Portal API response length: 1234
[JenkinsApiClient] Portal API response preview: {"build_output":"[2026-01-19T12:04:47.3642] Started by user ...",...
[JenkinsApiClient] Found build_output at root level
[JenkinsApiClient] Extracted build_output, length: 5678
```

### 未找到：
```
[JenkinsApiClient] Portal API response length: 1234
[JenkinsApiClient] Portal API response preview: {"id":"696e1dd9b547926878e53eab","queue_id":1775928,...
[JenkinsApiClient] Checking callback object for build_output
[JenkinsApiClient] No build_output field found in response (checked root and callback)
```

## 支持的 JSON 格式

### 格式 1：根级别（向后兼容）
```json
{
  "build_output": "log content here..."
}
```

### 格式 2：嵌套在 callback 中（Portal API 实际格式）
```json
{
  "id": "...",
  "callback": {
    "build_output": "log content here..."
  }
}
```

## 测试验证

1. 打开应用程序
2. 进入 Jenkins Browser
3. 双击任意 Stage 打开 Stage Log 对话框
4. 切换到 Portal Log 标签页
5. 验证：
   - 应该能够成功提取 `build_output`
   - 不再显示 "No build_output field found" 错误
   - 日志内容正常显示
6. 查看控制台输出：
   - 应该看到 "Found build_output in callback object"
   - 应该看到 "Extracted build_output, length: XXXX"

## 编译和部署

```bash
mvn clean package
```

生成文件: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

**重要**：确保关闭正在运行的应用程序，然后使用新生成的 JAR 文件启动。

## 相关文件

- `src/main/java/com/gitviewer/JenkinsApiClient.java` - Portal API JSON 解析逻辑

## 编译状态

✅ **编译成功** (2026-01-20 18:11)

```
[INFO] BUILD SUCCESS
[INFO] Total time:  15.627 s
```

生成文件: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 完成时间

2026-01-20 18:11
