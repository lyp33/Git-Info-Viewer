# AI Chat Context Overflow 修复完成

## 问题描述

当用户使用 AI Chat 的 search API 时，如果返回的数据量过大，发送给 AI 时会超过 context window 限制，导致 500 错误：

```
{"message":"http exception: inference error"}
```

## 根本原因

- Search API 可能返回大量数据（例如搜索结果包含很多文件）
- 这些数据直接发送给 AI，超过了 AI 模型的 context window 限制
- 导致 AI API 返回 inference error

## 解决方案

在 `AIChatDialog.java` 的 `executeApiInstruction()` 方法中添加了统一的数据大小限制：

### 实现位置

文件：`src/main/java/com/gitviewer/AIChatDialog.java`
行号：780-784

### 代码实现

```java
// 统一的数据大小限制，防止超过AI context window
if (result != null && result.length() > 20000) {
    System.out.println("[AI Chat] API Response too large (" + result.length() + " chars), truncating to 20000");
    result = result.substring(0, 20000) + "\n\n...[数据过多，已截断到20000字符。建议使用更具体的搜索条件]";
}
```

### 限制说明

- **大小限制**：20KB（20000 字符）
- **适用范围**：所有 14 个 Git API 的响应数据
- **超过限制时**：
  - 截断到前 20000 字符
  - 添加提示信息："[数据过多，已截断到20000字符。建议使用更具体的搜索条件]"
  - 在控制台输出日志，显示原始数据大小

## 影响的 API

此限制适用于所有 Git API：

1. get_repo - 获取仓库信息
2. get_issues - 获取 issues 列表
3. get_prs - 获取 pull requests
4. get_commits - 获取提交记录
5. get_branches - 获取分支列表
6. get_releases - 获取发布版本
7. get_contents - 获取目录内容
8. search_repos - 搜索仓库
9. search_issues - 搜索 issues
10. search_files - 搜索文件
11. get_file_commits - 获取文件提交历史
12. get_commit_detail - 获取提交详情
13. get_commit_diff - 获取提交差异
14. get_file_content - 获取文件内容

## 测试建议

1. **测试 search_files API**：
   - 搜索常见的文件名（如 "java", "log"）
   - 验证返回数据不会导致 context overflow
   - 确认 AI 能够正常处理截断后的数据

2. **测试其他可能返回大量数据的 API**：
   - get_commits（大量提交记录）
   - get_contents（大目录）
   - search_repos（搜索结果很多）

3. **验证用户体验**：
   - 确认截断提示信息清晰易懂
   - AI 能够基于截断后的数据给出有用的回答
   - 如果数据不足，AI 会建议用户使用更具体的搜索条件

## 编译和部署

```bash
# 编译
mvn compile

# 打包
mvn package -DskipTests

# 运行
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

## 日志输出

当数据被截断时，控制台会输出：

```
[AI Chat] API Response too large (45678 chars), truncating to 20000
```

这有助于开发者了解哪些 API 调用返回了大量数据。

## 后续优化建议

如果 20KB 的限制仍然不够或过于严格，可以考虑：

1. **动态调整限制**：根据不同的 API 设置不同的限制
2. **智能截断**：保留最相关的数据，而不是简单截断前 N 个字符
3. **分页支持**：对于大量数据，支持分页查询
4. **数据摘要**：对大量数据进行摘要处理，只发送关键信息给 AI

## 完成时间

2026-02-08

## 相关文档

- AI_CHAT_FILE_CONTENT_OPTIMIZATIONS_COMPLETE.md - 之前的文件内容优化
- AI_CHAT_SEARCH_FILES_PARAMETER_FIX.md - search_files 参数修复
