# AI Chat 获取文件内容功能完成

## 问题描述

用户询问："通过api访问 envs/common/.basic 文件里的内容"时，AI Chat 无法直接返回文件内容，而是建议用户使用 curl 命令或去网页查看。

**根本原因**：缺少 `get_file_content` API，AI 无法直接获取文件的完整源代码。

## 解决方案

添加了 `get_file_content` API，让 AI 能够直接读取并分析文件内容。

## 实现详情

### 1. 在 GitApiClient.java 中添加 getFileContent 方法

**文件**：`src/main/java/com/gitviewer/GitApiClient.java`

**新增方法**：
```java
public String getFileContent(String owner, String repo, String filepath, String branch) throws IOException
```

**支持的平台**：
- **GitLab**：使用 `/projects/:id/repository/files/:file_path/raw?ref=:branch` API
- **GitHub**：使用 `/repos/:owner/:repo/contents/:path?ref=:branch` API，自动解析 base64 编码的内容

**特性**：
- 支持指定分支（可选参数）
- 自动处理 GitHub 的 base64 编码
- 返回纯文本内容，便于 AI 分析

### 2. 在 AIChatDialog.java 中添加 API 调用支持

**文件**：`src/main/java/com/gitviewer/AIChatDialog.java`

**修改位置**：
1. `executeApiInstruction()` 方法 - 添加 `get_file_content` case
2. `askAIForApiCall()` 方法 - 添加 API 说明和示例

**文件大小限制**：50KB（50000 字符）
- 超过限制时截断并提示
- 独立于其他 API 的 20KB 限制（因为文件内容通常需要更多空间）

### 3. AI 提示词优化

在第一阶段的 AI 提示中添加了：

```
12. get_file_content - 获取文件的完整源代码内容（参数：filepath，例如：envs/common/.basic；可选参数：branch）

特别说明：
- 当用户询问某个文件的内容、文件是做什么的、文件里有什么代码时，使用 get_file_content API
- get_file_content 会返回文件的完整源代码，你可以直接分析并回答用户的问题
- 不要建议用户使用 curl 命令或去网页查看，直接使用 get_file_content API 获取内容
```

**示例**：
```json
{"action": "get_file_content", "filepath": "envs/common/.basic"}
{"action": "get_file_content", "filepath": "src/App.java", "branch": "dev"}
```

## API 参数说明

| 参数 | 类型 | 必需 | 说明 | 示例 |
|------|------|------|------|------|
| filepath | String | 是 | 文件路径（相对于仓库根目录） | `envs/common/.basic` |
| branch | String | 否 | 分支名称（默认使用当前分支） | `dev`, `main` |

## 使用场景

现在 AI Chat 可以回答以下类型的问题：

1. **查看文件内容**
   - "envs/common/.basic 文件里有什么内容？"
   - "显示 src/main/App.java 的代码"

2. **分析文件功能**
   - "envs/common/.basic 是做什么的？"
   - "这个配置文件定义了哪些环境变量？"

3. **代码审查**
   - "src/utils/Helper.java 里有哪些方法？"
   - "这个文件的代码质量如何？"

4. **跨分支比较**
   - "dev 分支的 config.json 和 main 分支有什么不同？"（需要两次调用）

## 技术细节

### GitLab API
```
GET /api/v4/projects/:id/repository/files/:file_path/raw?ref=:branch
```
- 直接返回文件的原始内容（纯文本）
- 文件路径需要 URL 编码（`/` → `%2F`）

### GitHub API
```
GET /repos/:owner/:repo/contents/:path?ref=:branch
```
- 返回 JSON 格式，包含 base64 编码的内容
- 需要解析 JSON 并解码 base64
- 自动处理换行符

### 错误处理

- **404**：文件不存在
- **403**：没有权限访问
- **401**：认证失败
- **文件过大**：超过 50KB 时截断并提示

## 测试建议

1. **测试基本功能**
   ```
   用户：envs/common/.basic 文件里有什么内容？
   预期：AI 调用 get_file_content API，返回文件内容并分析
   ```

2. **测试分支参数**
   ```
   用户：dev 分支的 config.json 是什么？
   预期：AI 调用 get_file_content API，指定 branch=dev
   ```

3. **测试文件分析**
   ```
   用户：src/main/App.java 是做什么的？
   预期：AI 获取文件内容，分析并解释文件功能
   ```

4. **测试大文件**
   ```
   用户：显示一个超过 50KB 的文件
   预期：返回前 50KB 内容并提示已截断
   ```

## 与其他 API 的区别

| API | 用途 | 返回内容 |
|-----|------|----------|
| get_contents | 列出目录内容 | 文件和文件夹列表 |
| get_file_commits | 查看文件历史 | 提交记录列表 |
| get_file_content | 读取文件内容 | 文件的完整源代码 |
| search_files | 搜索文件名 | 匹配的文件列表 |

## 完成时间

2026-02-08

## 相关文档

- AI_CHAT_CONTEXT_OVERFLOW_FIX.md - 数据大小限制修复
- AI_CHAT_FILE_CONTENT_OPTIMIZATIONS_COMPLETE.md - 之前的优化（未实现 get_file_content）
- AI_CHAT_SEARCH_FILES_PARAMETER_FIX.md - search_files 参数修复

## 编译和部署

```bash
# 编译
mvn compile

# 打包
mvn package -DskipTests

# 运行
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

## 总结

现在 AI Chat 拥有完整的文件访问能力：
- ✅ 搜索文件（search_files）
- ✅ 查看文件历史（get_file_commits）
- ✅ 读取文件内容（get_file_content）← **新增**
- ✅ 列出目录内容（get_contents）

用户可以直接询问任何文件的内容，AI 会自动调用 API 获取并分析，无需手动使用 curl 或访问网页。
