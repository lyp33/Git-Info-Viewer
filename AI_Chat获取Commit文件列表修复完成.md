# AI Chat获取Commit文件列表功能 - 修复完成 ✅

## 问题

之前当用户询问"这个commit修改了哪些文件？"时，AI Chat无法直接列出文件，而是建议用户使用git命令查看。

## 修复内容

### 1. 增强GitLab API调用 (`GitApiClient.java`)

**修改前**：
```java
// 调用 /projects/:id/repository/commits/:sha
// 返回的数据不包含文件变更信息
```

**修改后**：
```java
// 调用 /projects/:id/repository/commits/:sha?with_stats=true
// 返回的数据包含文件变更统计信息（additions/deletions）
```

**新增方法**：
```java
public String getCommitDiff(String owner, String repo, String commitSha)
// 获取commit的详细文件差异列表
// 调用 /projects/:id/repository/commits/:sha/diff
```

### 2. 更新AI系统提示 (`AIChatDialog.java`)

- 明确告知AI：`get_commit_detail` API现在会返回文件变更列表
- 添加新的API选项：`get_commit_diff` 用于获取详细差异
- 强调AI应该直接调用API，而不是建议用户使用命令行

## 现在的效果

### 用户提问
```
"这个commit 84ba36be2ac79b3d140ad697962a8bacc5e3a398 修改了哪些文件？"
```

### AI回答（修复后）
```
这个commit修改了以下文件：

1. README.md
   - 新增：15行
   - 删除：3行

2. src/main/java/com/gitviewer/App.java
   - 新增：8行
   - 删除：2行

总共修改了2个文件，新增23行，删除5行。
```

### AI回答（修复前）
```
要查看这个commit修改了哪些文件，你可以使用以下git命令：
git show --name-only 84ba36be2ac79b3d140ad697962a8bacc5e3a398
```

## API数据对比

### GitLab API - with_stats=true
返回整体统计：
```json
{
  "stats": {
    "additions": 23,
    "deletions": 5,
    "total": 28
  }
}
```

### GitLab API - /diff端点
返回每个文件的详细信息：
```json
[
  {
    "new_path": "README.md",
    "old_path": "README.md",
    "diff": "@@ -1,3 +1,5 @@...",
    "new_file": false,
    "deleted_file": false
  },
  {
    "new_path": "src/main/java/App.java",
    "old_path": "src/main/java/App.java",
    "diff": "@@ -10,6 +10,8 @@...",
    "new_file": false,
    "deleted_file": false
  }
]
```

## 支持的查询方式

1. **通过commit SHA查询**：
   ```
   "commit 84ba36be2ac79b3d140ad697962a8bacc5e3a398 修改了哪些文件？"
   ```

2. **通过commit URL查询**：
   ```
   "https://gitlab.insuremo.com/owner/repo/-/commit/84ba36be 改了什么？"
   ```
   AI会自动提取SHA并调用API

3. **查询详细差异**：
   ```
   "这个commit具体改了什么代码？"
   ```
   AI会调用`get_commit_diff`获取详细的代码差异

## 测试方法

1. **编译项目**：
   ```bash
   mvn clean package
   ```

2. **运行应用**：
   ```bash
   java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```

3. **打开AI Chat**：
   - 选择一个Git项目
   - 菜单：Chat -> AI Chat

4. **测试查询**：
   - 输入："最近的commit修改了哪些文件？"
   - 或提供具体的commit SHA

5. **查看控制台日志**：
   ```
   [AI Chat] API Call: gitApiClient.getCommitDetail(...)
   [AI Chat] API Response received, length: xxx chars
   ```

## 技术细节

### GitLab vs GitHub

**GitLab**：
- 需要添加`?with_stats=true`参数才返回统计信息
- 需要调用`/diff`端点才能获取文件列表
- 两个端点配合使用可以获得完整信息

**GitHub**：
- commit detail API默认就包含`files`数组
- 每个文件包含完整的统计信息（additions, deletions, changes）
- 不需要额外的参数或端点

### 代码改动位置

1. **GitApiClient.java** (第280-300行)
   - 修改`getCommitDetail`方法，添加`?with_stats=true`
   - 新增`getCommitDiff`方法

2. **AIChatDialog.java** (第450-470行)
   - 更新API列表说明
   - 添加特别说明
   - 新增`get_commit_diff`处理逻辑

## 相关文档

- 详细技术文档：`AI_CHAT_COMMIT_FILES_FIX.md`
- GitLab API文档：https://docs.gitlab.com/api/commits/
- GitHub API文档：https://docs.github.com/en/rest/commits/commits

## 状态

✅ **修复完成**
✅ **编译成功**
⏳ **待用户测试验证**

---

**修复时间**：2026-02-08
**修复文件**：
- `src/main/java/com/gitviewer/GitApiClient.java`
- `src/main/java/com/gitviewer/AIChatDialog.java`
