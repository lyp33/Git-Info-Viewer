# Portal Log 实现完成

## 修改时间
2026-01-18

## 功能描述
从 Stage Log 中提取 curl 命令的 URL 和 headers，调用 Portal API 获取 `build_output` 并显示在 Portal Log Tab 中。

## 实现逻辑

### 1. 从 Stage Log 中提取信息
从 Stage Log 中提取两类信息：

#### A. Portal API URL
- 匹配模式：`https://portal-gw\.insuremo\.com/[^\s'"]+`
- 示例：`https://portal-gw.insuremo.com/edBor/a/ops/build/query_one?id=696b58afb5479268788e51f07`

#### B. Curl Headers
- 匹配模式：`-H ['"]([^:]+):\s*([^'"]+)['"]`
- 示例：
  ```
  -H 'x-mo-target-tenant: thailife'
  -H 'Authorization: Bearer MOATcZjK0x5xEwjYK-PHw0xvdYvb7HkAR'
  ```

### 2. 调用 Portal API
使用提取的 URL 和 headers 发送 GET 请求：
```java
HttpURLConnection conn = (HttpURLConnection) url.openConnection();
conn.setRequestMethod("GET");
// 添加提取的 headers
for (Map.Entry<String, String> entry : headers.entrySet()) {
    conn.setRequestProperty(entry.getKey(), entry.getValue());
}
```

### 3. 解析 JSON 响应
从响应中提取 `build_output` 字段：
```java
JSONObject json = new JSONObject(jsonResponse);
String buildOutput = json.getString("build_output");
```

### 4. 显示在 Portal Log Tab
将 `build_output` 内容显示在 Portal Log Tab 的文本区域中。

## 工作流程

```
1. 用户双击 Stage
   ↓
2. 加载 Jenkins Log
   ↓
3. 获取 Stage Log
   ↓
4. 从 Stage Log 中提取:
   - Portal API URL
   - Curl Headers
   ↓
5. 调用 Portal API (GET 请求)
   ↓
6. 解析 JSON 响应
   ↓
7. 提取 build_output 字段
   ↓
8. 显示在 Portal Log Tab
```

## 修改的文件

### 1. `src/main/java/com/gitviewer/JenkinsApiClient.java`
**新增方法**：
- `fetchPortalBuildOutput(String stageLog)` - 主方法，从 Stage Log 中提取信息并调用 Portal API
- `extractPortalUrl(String stageLog)` - 提取 Portal API URL
- `extractCurlHeaders(String stageLog)` - 提取 curl headers
- `sendGetRequestWithHeaders(String urlString, Map<String, String> headers)` - 发送带自定义 headers 的 GET 请求

### 2. `src/main/java/com/gitviewer/JenkinsStageLogDialog.java`
**修改**：
- `loadJenkinsLog()` - 保存 Stage Log 并在完成后自动触发 Portal Log 加载
- `loadPortalLogWithStageLog(String stageLog)` - 使用 Stage Log 加载 Portal Log
- Refresh 按钮 - 只需调用 `loadJenkinsLog()`，会自动触发 Portal Log 刷新

## 关键特性

### 1. 自动提取 ✅
- 无需手动配置 Portal API URL
- 无需手动配置认证信息
- 直接从 Stage Log 中提取所有必要信息

### 2. 原装使用 ✅
- URL 不做任何修改或拼接
- Headers 原样使用
- 完全复制 curl 命令的行为

### 3. 错误处理 ✅
- 如果找不到 Portal URL，显示提示信息
- 如果 API 调用失败，显示错误信息
- 如果 JSON 中没有 `build_output`，显示完整响应供调试

### 4. 调试日志 ✅
- 详细的控制台输出
- 显示提取的 URL 和 headers
- 显示 API 响应长度和内容预览

## 正则表达式说明

### Portal URL 提取
```java
Pattern.compile("https://portal-gw\\.insuremo\\.com/[^\\s'\"]+")
```
- 匹配以 `https://portal-gw.insuremo.com/` 开头的 URL
- 直到遇到空格、单引号或双引号为止

### Curl Headers 提取
```java
Pattern.compile("-H\\s+['\"]([^:]+):\\s*([^'\"]+)['\"]")
```
- 匹配 `-H 'key: value'` 或 `-H "key: value"` 格式
- 捕获组 1：header 名称
- 捕获组 2：header 值

## 编译结果
✅ 编译成功
- JAR 文件位置: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 测试要点
1. ✅ 双击 Stage 后显示两个 Tab
2. ✅ Jenkins Log Tab 显示 Console Log
3. ✅ Portal Log Tab 自动加载（在 Jenkins Log 加载完成后）
4. ✅ Portal Log 显示 `build_output` 内容
5. ✅ 如果 Stage Log 中没有 Portal URL，显示提示信息
6. ✅ Refresh 按钮同时刷新两个 Tab
7. ✅ 两个 Tab 都有 "printing..." 提示

## 示例 Stage Log 格式

```bash
curl -i -L -X GET 'https://portal-gw.insuremo.com/edBor/a/ops/build/query_one?id=696b58afb5479268788e51f07' \
  -H 'x-mo-target-tenant: thailife' \
  -H 'Authorization: Bearer MOATcZjK0x5xEwjYK-PHw0xvdYvb7HkAR' \
  -H 'Content-Type: application/json' \
  -H 'Cookie: secure'
```

**提取结果**：
- URL: `https://portal-gw.insuremo.com/edBor/a/ops/build/query_one?id=696b58afb5479268788e51f07`
- Headers:
  - `x-mo-target-tenant: thailife`
  - `Authorization: Bearer MOATcZjK0x5xEwjYK-PHw0xvdYvb7HkAR`
  - `Content-Type: application/json`
  - `Cookie: secure`

## 技术亮点

### 1. 智能提取
使用正则表达式从非结构化的 Stage Log 中提取结构化信息

### 2. 零配置
无需用户配置任何 Portal API 相关信息，全部自动提取

### 3. 容错性强
- 找不到 URL 时给出明确提示
- API 调用失败时显示错误信息
- JSON 解析失败时显示原始响应

### 4. 异步加载
- Jenkins Log 和 Portal Log 独立加载
- 不会互相阻塞
- 用户体验流畅
