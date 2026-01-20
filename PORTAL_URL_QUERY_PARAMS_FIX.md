# Portal URL 查询参数提取修复

## 问题描述

Portal Log 提取的 URL 不完整，缺少查询参数部分：

**期望的完整 URL**:
```
https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab
```

**实际提取的 URL**:
```
https://portal-gw.insuremo.com/eBao/1.0/ops/build
```

缺少了 `/query_one?id=*****` 部分。

## 原因分析

### 原有正则表达式问题

```java
Pattern pattern = Pattern.compile("https://portal-gw\\.insuremo\\.com/[^\\s'\"]+");
```

**问题**：
- `[^\\s'\"]+` 表示"匹配任何不是空格、单引号或双引号的字符"
- 当 URL 被单引号包围时（如 `'https://...'`），正则会在遇到第一个单引号后就停止匹配
- 导致无法匹配到完整的 URL

### curl 命令格式

从日志中可以看到，curl 命令的格式是：
```bash
curl -s -L -X GET 'https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab' -H '...'
```

URL 被单引号包围，所以需要匹配包括引号在内的完整 URL。

## 解决方案

使用两个正则表达式分别处理带引号和不带引号的情况：

### 1. 带引号的 URL（优先匹配）

```java
Pattern quotedPattern = Pattern.compile("['\"]https://portal-gw\\.insuremo\\.com/[^'\"]*['\"]");
```

**说明**：
- `['\"]` - 匹配开始的单引号或双引号
- `https://portal-gw\\.insuremo\\.com/` - 匹配 URL 前缀
- `[^'\"]*` - 匹配任何不是引号的字符（包括 `/`, `?`, `=`, `&` 等）
- `['\"]` - 匹配结束的单引号或双引号

**匹配结果**：`'https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab'`

然后移除首尾的引号得到完整 URL。

### 2. 不带引号的 URL（备用匹配）

```java
Pattern unquotedPattern = Pattern.compile("https://portal-gw\\.insuremo\\.com/\\S+");
```

**说明**：
- `\\S+` - 匹配任何非空白字符（包括 `/`, `?`, `=`, `&` 等）

### 修改内容

**文件**: `src/main/java/com/gitviewer/JenkinsApiClient.java`

**修改前**:
```java
Pattern pattern = Pattern.compile("https://portal-gw\\.insuremo\\.com/[^\\s'\"]+");

Matcher matcher = pattern.matcher(line);
if (matcher.find()) {
    String url = matcher.group(0);
    return url;
}
```

**修改后**:
```java
// 尝试两种模式
Pattern quotedPattern = Pattern.compile("['\"]https://portal-gw\\.insuremo\\.com/[^'\"]*['\"]");
Pattern unquotedPattern = Pattern.compile("https://portal-gw\\.insuremo\\.com/\\S+");

// 先尝试匹配带引号的 URL
Matcher quotedMatcher = quotedPattern.matcher(line);
if (quotedMatcher.find()) {
    String urlWithQuotes = quotedMatcher.group(0);
    // 移除引号
    String url = urlWithQuotes.substring(1, urlWithQuotes.length() - 1);
    return url;
}

// 如果没有找到带引号的，尝试匹配不带引号的
Matcher unquotedMatcher = unquotedPattern.matcher(line);
if (unquotedMatcher.find()) {
    String url = unquotedMatcher.group(0);
    return url;
}
```

## 匹配示例

### 示例 1：单引号包围
```bash
curl -s -L -X GET 'https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab' -H 'Authorization: Bearer xxx'
```

**匹配结果**：
- `quotedPattern` 匹配到：`'https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab'`
- 移除引号后：`https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab`

### 示例 2：双引号包围
```bash
curl -s -L -X GET "https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab" -H "Authorization: Bearer xxx"
```

**匹配结果**：
- `quotedPattern` 匹配到：`"https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab"`
- 移除引号后：`https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab`

### 示例 3：无引号
```bash
curl -s -L -X GET https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab -H Authorization:Bearer
```

**匹配结果**：
- `quotedPattern` 不匹配
- `unquotedPattern` 匹配到：`https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696e1dd9b547926878e53eab`

## 测试验证

1. 打开应用程序
2. 进入 Jenkins Browser
3. 双击任意 Stage 打开 Stage Log 对话框
4. 切换到 Portal Log 标签页
5. 验证提取的 URL 包含完整的路径和查询参数
6. 验证能够成功调用 Portal API 并获取 `build_output`

## 编译和部署

```bash
mvn clean package
```

生成文件: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 相关文件

- `src/main/java/com/gitviewer/JenkinsApiClient.java` - Portal URL 提取逻辑

## 完成时间

2026-01-20
