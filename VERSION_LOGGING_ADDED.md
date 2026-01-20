# 版本日志和调试信息增强

## 更新内容

为了方便确认 JAR 包是否为最新版本，以及调试 Portal URL 提取问题，添加了详细的版本信息和调试日志。

## 1. 应用启动版本信息

**文件**: `src/main/java/com/gitviewer/GitViewerApp.java`

在应用启动时打印版本信息：

```
========================================
Git Info Viewer - Version 2026-01-20-16:30
Build: Portal URL Query Params Fix
========================================
```

**如何查看**：
- 启动应用程序
- 查看控制台输出
- 第一行就会显示版本信息

## 2. Portal URL 提取详细日志

**文件**: `src/main/java/com/gitviewer/JenkinsApiClient.java`

在 `extractPortalUrl` 方法中添加了详细的调试日志：

### 日志内容包括：

1. **版本标识**：
```
[JenkinsApiClient] ========================================
[JenkinsApiClient] VERSION: 2026-01-20-16:30 - Portal URL Query Params Fix
[JenkinsApiClient] Extracting Portal URL from stage log (line by line)...
[JenkinsApiClient] ========================================
```

2. **正则表达式模式**：
```
[JenkinsApiClient] Quoted pattern: ['\"]https://portal-gw\.insuremo\.com/[^'\"]*['\"]
[JenkinsApiClient] Unquoted pattern: https://portal-gw\.insuremo\.com/\S+
```

3. **匹配过程**：
```
[JenkinsApiClient] ----------------------------------------
[JenkinsApiClient] Found potential Portal URL line 123
[JenkinsApiClient] Line content: curl -s -L -X GET 'https://portal-gw.insuremo.com/...'
[JenkinsApiClient] ----------------------------------------
[JenkinsApiClient] Trying quoted pattern...
[JenkinsApiClient] Quoted match found: 'https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab'
[JenkinsApiClient] ✓✓✓ EXTRACTED Portal URL (quoted): https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab
[JenkinsApiClient] URL length: 95
[JenkinsApiClient] ========================================
```

4. **失败情况**：
```
[JenkinsApiClient] Trying quoted pattern...
[JenkinsApiClient] No quoted match found
[JenkinsApiClient] Trying unquoted pattern...
[JenkinsApiClient] No unquoted match found
```

## 如何验证 JAR 包版本

### 方法 1：查看启动日志
1. 启动应用程序
2. 查看控制台第一行输出
3. 确认版本号为 `2026-01-20-16:30`

### 方法 2：查看 Portal URL 提取日志
1. 打开 Stage Log 对话框
2. 切换到 Portal Log 标签页
3. 查看控制台输出
4. 确认看到 `VERSION: 2026-01-20-16:30 - Portal URL Query Params Fix`

### 方法 3：检查提取的 URL
1. 查看控制台中的 `✓✓✓ EXTRACTED Portal URL` 日志
2. 确认 URL 包含完整的查询参数（`/query_one?id=*****`）
3. 确认 URL 长度大于 80（完整 URL 应该在 90-100 字符左右）

## 调试信息说明

### 正常流程日志示例：

```
[JenkinsApiClient] ========================================
[JenkinsApiClient] VERSION: 2026-01-20-16:30 - Portal URL Query Params Fix
[JenkinsApiClient] Extracting Portal URL from stage log (line by line)...
[JenkinsApiClient] ========================================
[JenkinsApiClient] Total lines to parse: 1234
[JenkinsApiClient] Quoted pattern: ['\"]https://portal-gw\.insuremo\.com/[^'\"]*['\"]
[JenkinsApiClient] Unquoted pattern: https://portal-gw\.insuremo\.com/\S+
[JenkinsApiClient] ----------------------------------------
[JenkinsApiClient] Found potential Portal URL line 567
[JenkinsApiClient] Line content: curl -s -L -X GET 'https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab' -H 'Authorization: Bearer xxx'
[JenkinsApiClient] ----------------------------------------
[JenkinsApiClient] Trying quoted pattern...
[JenkinsApiClient] Quoted match found: 'https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab'
[JenkinsApiClient] ✓✓✓ EXTRACTED Portal URL (quoted): https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab
[JenkinsApiClient] URL length: 95
[JenkinsApiClient] ========================================
```

### 如果 URL 不完整的日志示例：

```
[JenkinsApiClient] ✓✓✓ EXTRACTED Portal URL (quoted): https://portal-gw.insuremo.com/eBao/1.0/ops/build
[JenkinsApiClient] URL length: 52
```

**注意**：如果 URL 长度只有 50-60 字符，说明缺少查询参数部分。

## 编译和部署

```bash
mvn clean package
```

生成文件: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

**重要**：确保关闭正在运行的应用程序，然后使用新生成的 JAR 文件启动。

## 相关文件

- `src/main/java/com/gitviewer/GitViewerApp.java` - 应用启动版本信息
- `src/main/java/com/gitviewer/JenkinsApiClient.java` - Portal URL 提取详细日志

## 完成时间

2026-01-20 17:11
