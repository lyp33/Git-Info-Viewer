# Portal URL 提取 - 使用最后一个匹配

## 问题描述

日志中有多个 curl 命令调用 Portal API：
1. **第一个 curl 命令**：URL 不包含查询参数（`/query_one?id=*****`）
2. **最后一个 curl 命令**：URL 包含完整的查询参数

原有代码在找到第一个匹配后就立即返回，导致获取的 URL 不完整。

## 示例日志

```bash
# 第一个 curl（不完整）
+ curl -s -L -X POST https://portal-gw.insuremo.com/eBao/1.0/ops/build -H 'x-mo-target-tenant: thailife' ...

# 中间可能有其他命令
+ sleep 60
+ jq .id common-bff-ci-260119200440.log -r

# 最后一个 curl（完整，包含查询参数）
+ curl -s -L -X GET 'https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab' -H 'x-mo-target-tenant: thailife' ...
```

## 原有逻辑问题

```java
for (int i = 0; i < lines.length; i++) {
    if (line.contains("curl") && line.contains("portal-gw.insuremo.com")) {
        // 找到匹配
        if (quotedMatcher.find()) {
            return url;  // ❌ 立即返回第一个匹配
        }
    }
}
```

**问题**：找到第一个匹配就返回，无法获取最后一个（完整的）URL。

## 解决方案

修改逻辑为：**遍历所有行，保存最后一个找到的 URL，最后返回**。

```java
String lastFoundUrl = null;
int lastFoundLineNumber = -1;

for (int i = 0; i < lines.length; i++) {
    if (line.contains("curl") && line.contains("portal-gw.insuremo.com")) {
        // 找到匹配
        if (quotedMatcher.find()) {
            String foundUrl = ...;
            lastFoundUrl = foundUrl;  // ✓ 保存为最后一个
            lastFoundLineNumber = i + 1;
            // 继续循环，不返回
        }
    }
}

// 循环结束后返回最后一个找到的 URL
return lastFoundUrl;
```

## 修改内容

**文件**: `src/main/java/com/gitviewer/JenkinsApiClient.java`

### 主要变化：

1. **添加变量保存最后一个匹配**：
```java
String lastFoundUrl = null;
int lastFoundLineNumber = -1;
```

2. **找到匹配时不立即返回，而是保存**：
```java
if (foundUrl != null) {
    lastFoundUrl = foundUrl;
    lastFoundLineNumber = i + 1;
    System.out.println("[JenkinsApiClient] Saved as last found URL (line " + lastFoundLineNumber + ")");
}
```

3. **循环结束后返回最后一个**：
```java
if (lastFoundUrl != null) {
    System.out.println("[JenkinsApiClient] ✓✓✓ RETURNING LAST Portal URL (line " + lastFoundLineNumber + "):");
    System.out.println("[JenkinsApiClient] " + lastFoundUrl);
    return lastFoundUrl;
}
```

## 日志输出示例

### 找到多个匹配时：

```
[JenkinsApiClient] ========================================
[JenkinsApiClient] VERSION: 2026-01-20-17:20 - Find LAST Portal URL Match
[JenkinsApiClient] ========================================
[JenkinsApiClient] Total lines to parse: 1234

[JenkinsApiClient] Found potential Portal URL line 567
[JenkinsApiClient] Line content: curl -s -L -X POST https://portal-gw.insuremo.com/eBao/1.0/ops/build ...
[JenkinsApiClient] Extracted URL (quoted): https://portal-gw.insuremo.com/eBao/1.0/ops/build
[JenkinsApiClient] URL length: 52
[JenkinsApiClient] Saved as last found URL (line 567)

[JenkinsApiClient] Found potential Portal URL line 789
[JenkinsApiClient] Line content: curl -s -L -X GET 'https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab' ...
[JenkinsApiClient] Extracted URL (quoted): https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab
[JenkinsApiClient] URL length: 95
[JenkinsApiClient] Saved as last found URL (line 789)

[JenkinsApiClient] ========================================
[JenkinsApiClient] ✓✓✓ RETURNING LAST Portal URL (line 789):
[JenkinsApiClient] https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab
[JenkinsApiClient] URL length: 95
[JenkinsApiClient] ========================================
```

## 版本信息

**应用启动时显示**：
```
========================================
Git Info Viewer - Version 2026-01-20-17:20
Build: Find LAST Portal URL Match
========================================
```

**Portal URL 提取时显示**：
```
[JenkinsApiClient] VERSION: 2026-01-20-17:20 - Find LAST Portal URL Match
```

## 验证方法

1. 启动应用程序，确认版本为 `2026-01-20-17:20`
2. 打开 Stage Log 对话框
3. 切换到 Portal Log 标签页
4. 查看控制台输出：
   - 应该看到多个 "Found potential Portal URL line" 日志
   - 应该看到多个 "Saved as last found URL" 日志
   - 最后应该看到 "RETURNING LAST Portal URL" 日志
   - 返回的 URL 应该包含 `/query_one?id=*****` 部分
   - URL 长度应该在 90-100 字符左右

## 编译和部署

```bash
mvn clean package
```

生成文件: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

**重要**：确保关闭正在运行的应用程序，然后使用新生成的 JAR 文件启动。

## 相关文件

- `src/main/java/com/gitviewer/JenkinsApiClient.java` - Portal URL 提取逻辑
- `src/main/java/com/gitviewer/GitViewerApp.java` - 应用启动版本信息

## 完成时间

2026-01-20 17:31
