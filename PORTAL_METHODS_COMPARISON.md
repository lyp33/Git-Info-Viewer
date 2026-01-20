# fetchPortalBuildOutput vs fetchPortalBuildOutputWithInfo 方法对比

## 概述

这两个方法都用于从 Portal API 获取 `build_output` 日志内容，但它们的**职责分工**和**使用场景**不同。

---

## 方法签名对比

### fetchPortalBuildOutput
```java
public String fetchPortalBuildOutput(String stageLog)
```

### fetchPortalBuildOutputWithInfo
```java
public String fetchPortalBuildOutputWithInfo(String stageLog, String portalUrl, Map<String, String> headers)
```

---

## 核心区别

| 特性 | fetchPortalBuildOutput | fetchPortalBuildOutputWithInfo |
|------|------------------------|-------------------------------|
| **参数** | 只需要 `stageLog` | 需要 `stageLog`, `portalUrl`, `headers` |
| **职责** | **一站式方法**：提取 URL/headers + 调用 API + 解析 JSON | **专注方法**：只负责调用 API + 解析 JSON |
| **URL 提取** | ✅ 自动从 stageLog 提取 | ❌ 需要外部提供 |
| **Headers 提取** | ✅ 自动从 stageLog 提取 | ❌ 需要外部提供 |
| **嵌套 JSON 支持** | ❌ 只检查根级别 `build_output` | ✅ 检查根级别 + `callback` 对象 |
| **转义序列解码** | ❌ 不处理 `\n`, `\u4e2d` 等 | ✅ 自动解码 `\n`, `\r`, `\t`, `\uXXXX` |
| **使用场景** | 简单场景，一次性调用 | UI 需要分步显示信息时 |

---

## 详细对比

### 1. fetchPortalBuildOutput（旧方法，功能有限）

**工作流程**：
```
stageLog 
  ↓
提取 Portal URL (extractPortalUrl)
  ↓
提取 Headers (extractCurlHeaders)
  ↓
调用 Portal API (sendGetRequestWithHeaders)
  ↓
解析 JSON (只检查根级别)
  ↓
返回 build_output
```

**JSON 解析逻辑**：
```java
JSONObject json = new JSONObject(jsonResponse);

if (json.has("build_output")) {  // ❌ 只检查根级别
    String buildOutput = json.getString("build_output");
    return buildOutput;
} else {
    return "No build_output field found...";
}
```

**问题**：
- ❌ 不支持嵌套 JSON（Portal API 实际返回的是嵌套结构）
- ❌ 不处理转义序列（`\n` 显示为字面字符串，不换行）
- ❌ 无法在 UI 中分步显示 API 请求信息

---

### 2. fetchPortalBuildOutputWithInfo（新方法，功能完整）

**工作流程**：
```
外部提供: portalUrl + headers
  ↓
调用 Portal API (sendGetRequestWithHeaders)
  ↓
解析 JSON (检查根级别 + callback 对象)
  ↓
检测转义序列
  ↓
解码转义序列 (decodeUnicodeEscapes)
  ↓
返回 build_output
```

**JSON 解析逻辑**（支持嵌套）：
```java
JSONObject json = new JSONObject(jsonResponse);
String buildOutput = null;

// 首先检查根级别
if (json.has("build_output")) {
    buildOutput = json.getString("build_output");
    System.out.println("Found build_output at root level");
}
// 如果根级别没有，检查 callback 对象
else if (json.has("callback")) {
    JSONObject callback = json.getJSONObject("callback");
    if (callback.has("build_output")) {
        buildOutput = callback.getString("build_output");
        System.out.println("Found build_output in callback object");  // ✅ 支持嵌套
    }
}
```

**转义序列解码**：
```java
if (hasEscapes) {
    buildOutput = decodeUnicodeEscapes(buildOutput);  // ✅ 解码 \n, \r, \t, \uXXXX
}
```

**优势**：
- ✅ 支持嵌套 JSON（Portal API 的实际格式）
- ✅ 自动解码转义序列（正确显示换行和中文）
- ✅ 允许 UI 层分步显示信息（先显示 API 请求信息，再显示响应）

---

## 实际使用场景

### 场景 1：简单调用（已废弃）

```java
// 旧方法 - 不推荐使用
String portalLog = apiClient.fetchPortalBuildOutput(stageLog);
portalLogTextArea.setText(portalLog);
```

**问题**：
- 无法显示 API 请求信息
- 不支持嵌套 JSON
- 不处理转义序列

---

### 场景 2：UI 分步显示（当前使用）

```java
// 新方法 - 推荐使用
SwingWorker<PortalLogInfo, String> worker = new SwingWorker<>() {
    @Override
    protected PortalLogInfo doInBackground() throws Exception {
        // 第一步：提取 URL 和 headers
        String portalUrl = apiClient.extractPortalUrlPublic(stageLog);
        Map<String, String> headers = apiClient.extractCurlHeadersPublic(stageLog);
        
        // 第二步：显示 API 请求信息
        publish("=== Portal API Request Info ===\n");
        publish("URL: " + portalUrl + "\n");
        publish("Headers: ...\n\n");
        
        // 第三步：显示 "Loading..."
        publish("Loading...\n\n");
        
        // 第四步：调用 API（使用新方法）
        String portalLog = apiClient.fetchPortalBuildOutputWithInfo(stageLog, portalUrl, headers);
        
        return portalLog;
    }
    
    @Override
    protected void process(List<String> chunks) {
        // 逐步追加信息到 UI
        for (String chunk : chunks) {
            portalLogTextArea.append(chunk);
        }
    }
};
```

**优势**：
- ✅ 用户可以看到 API 请求信息（URL + Headers）
- ✅ 用户可以看到 "Loading..." 提示
- ✅ 支持嵌套 JSON 解析
- ✅ 正确显示换行和中文字符

---

## Portal API 的实际 JSON 结构

```json
{
  "id": "696e1dd9b547926878e53eab",
  "queue_id": 1775928,
  "app_name": "common-bff",
  "callback": {
    "callback_id": "ec305f89-d76d-4f21-b799-f592ffd18eff",
    "build_status": "Build Success",
    "build_output": "[2026-01-19T12:04:47.3642] Started by user ...\n..."
  }
}
```

- `fetchPortalBuildOutput`: ❌ 找不到 `build_output`（只检查根级别）
- `fetchPortalBuildOutputWithInfo`: ✅ 找到 `build_output`（检查 `callback` 对象）

---

## 当前代码使用情况

### JenkinsStageLogDialog.java

```java
// 使用新方法 fetchPortalBuildOutputWithInfo
private void loadPortalLogOnDemand() {
    SwingWorker<PortalLogInfo, String> worker = new SwingWorker<>() {
        @Override
        protected PortalLogInfo doInBackground() throws Exception {
            // 提取 URL 和 headers
            String portalUrl = apiClient.extractPortalUrlPublic(cachedStageLog);
            Map<String, String> headers = apiClient.extractCurlHeadersPublic(cachedStageLog);
            
            // 显示 API 信息
            publish("=== Portal API Request Info ===\n...");
            publish("Loading...\n\n");
            
            // 调用新方法
            String portalLog = apiClient.fetchPortalBuildOutputWithInfo(
                cachedStageLog, portalUrl, headers
            );
            
            return portalLog;
        }
    };
}
```

### 旧方法 loadPortalLogWithStageLog（已废弃）

```java
@Deprecated
private void loadPortalLogWithStageLog(String stageLog) {
    // 使用旧方法 fetchPortalBuildOutput
    String portalLog = apiClient.fetchPortalBuildOutput(stageLog);
    portalLogTextArea.setText(portalLog);
}
```

---

## 总结

| 方法 | 状态 | 推荐使用 |
|------|------|---------|
| `fetchPortalBuildOutput` | 功能有限，不支持嵌套 JSON | ❌ 不推荐 |
| `fetchPortalBuildOutputWithInfo` | 功能完整，支持嵌套 JSON + 转义解码 | ✅ 推荐 |

**建议**：
- 新代码应该使用 `fetchPortalBuildOutputWithInfo`
- 可以考虑将 `fetchPortalBuildOutput` 标记为 `@Deprecated`
- 或者重构 `fetchPortalBuildOutput` 使其内部调用 `fetchPortalBuildOutputWithInfo`

---

## 相关修复

1. **嵌套 JSON 支持** - `fetchPortalBuildOutputWithInfo` 检查 `callback` 对象
2. **转义序列解码** - 自动解码 `\n`, `\r`, `\t`, `\uXXXX`
3. **UI 分步显示** - 允许先显示 API 请求信息，再显示响应
4. **中文字体支持** - 使用 Microsoft YaHei 字体
5. **UI 冻结修复** - 限制显示 500KB 内容

---

**文档创建时间**: 2026-01-20 18:20
