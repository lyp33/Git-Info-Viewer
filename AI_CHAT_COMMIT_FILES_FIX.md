# AI Chat Commit文件变更列表功能修复

## 问题描述

用户在AI Chat中询问某个commit修改了哪些文件时，AI无法直接列出文件列表，而是建议用户使用git命令查看。

**示例问题**：
- "这个commit修改了哪些文件？"
- "commit 84ba36be2ac79b3d140ad697962a8bacc5e3a398 改了什么？"

## 根本原因

`GitApiClient.java`中的`getCommitDetail`方法调用GitLab API时，没有添加`with_stats=true`参数，导致返回的数据中不包含文件变更统计信息。

### GitLab API说明

GitLab提供两种方式获取commit的文件变更：

1. **获取统计信息**（推荐）：
   ```
   GET /projects/:id/repository/commits/:sha?with_stats=true
   ```
   返回每个文件的变更统计（additions/deletions）

2. **获取详细差异**：
   ```
   GET /projects/:id/repository/commits/:sha/diff
   ```
   返回完整的文件差异信息

## 修复方案

### 1. 增强`GitApiClient.java`

#### 修改`getCommitDetail`方法
添加`?with_stats=true`参数，使其返回文件变更统计：

```java
public String getCommitDetail(String owner, String repo, String commitSha) throws IOException {
    if (isGitLab) {
        // GitLab API: GET /projects/:id/repository/commits/:sha?with_stats=true
        // with_stats=true 会返回每个文件的变更统计信息
        String projectId = owner + "%2F" + repo;
        return GitLabApiClient.executeGet(baseUrl + "/projects/" + projectId + "/repository/commits/" + commitSha + "?with_stats=true", token);
    } else {
        // GitHub API: GET /repos/:owner/:repo/commits/:sha
        // GitHub API 默认返回文件变更信息
        return GitHubApiClient.executeGet("/repos/" + owner + "/" + repo + "/commits/" + commitSha, token);
    }
}
```

#### 新增`getCommitDiff`方法
提供获取详细差异的选项：

```java
public String getCommitDiff(String owner, String repo, String commitSha) throws IOException {
    if (isGitLab) {
        // GitLab API: GET /projects/:id/repository/commits/:sha/diff
        String projectId = owner + "%2F" + repo;
        return GitLabApiClient.executeGet(baseUrl + "/projects/" + projectId + "/repository/commits/" + commitSha + "/diff", token);
    } else {
        // GitHub API 的 commit detail 已经包含了 files 信息，不需要单独的 diff endpoint
        return getCommitDetail(owner, repo, commitSha);
    }
}
```

### 2. 更新`AIChatDialog.java`

#### 更新API列表说明
在系统提示中明确说明`get_commit_detail`会返回文件变更列表：

```java
context.append("12. get_commit_detail - 获取单个commit的详细信息，包含文件变更列表和统计（参数：commit_sha）\n");
context.append("    - 返回的数据包含：commit信息、修改的文件列表、每个文件的增删行数统计\n");
context.append("13. get_commit_diff - 获取commit的详细文件差异（参数：commit_sha）\n");
```

#### 添加特别说明
强调AI应该直接调用API而不是建议用户使用命令行：

```java
context.append("- **重要**：用户问\"这个commit修改了哪些文件\"、\"commit改了什么\"时，使用 get_commit_detail API（已包含文件变更列表）\n");
```

#### 添加`get_commit_diff`处理
在`executeApiInstruction`方法中添加新的action处理：

```java
case "get_commit_diff":
    String diffCommitSha = extractJsonValue(instruction, "commit_sha");
    System.out.println("[AI Chat] API Call: gitApiClient.getCommitDiff(" + owner + ", " + repo + ", " + diffCommitSha + ")");
    result = gitApiClient.getCommitDiff(owner, repo, diffCommitSha);
    break;
```

## API返回数据示例

### with_stats=true 返回的数据结构

```json
{
  "id": "84ba36be2ac79b3d140ad697962a8bacc5e3a398",
  "short_id": "84ba36be",
  "title": "Update README",
  "message": "Update README with new features",
  "author_name": "John Doe",
  "author_email": "john@example.com",
  "authored_date": "2024-02-08T10:30:00.000Z",
  "committer_name": "John Doe",
  "committer_email": "john@example.com",
  "committed_date": "2024-02-08T10:30:00.000Z",
  "stats": {
    "additions": 15,
    "deletions": 3,
    "total": 18
  },
  "status": null,
  "web_url": "https://gitlab.example.com/owner/repo/-/commit/84ba36be2ac79b3d140ad697962a8bacc5e3a398"
}
```

**注意**：GitLab API的`with_stats=true`参数返回的是整体统计信息（总的additions/deletions），而不是每个文件的详细列表。

### /diff 端点返回的数据结构

```json
[
  {
    "diff": "@@ -1,3 +1,5 @@\n # Project\n+\n+New feature added\n",
    "new_path": "README.md",
    "old_path": "README.md",
    "a_mode": "100644",
    "b_mode": "100644",
    "new_file": false,
    "renamed_file": false,
    "deleted_file": false
  },
  {
    "diff": "@@ -10,6 +10,8 @@\n public class App {\n+    // New method\n+    public void newMethod() {}\n",
    "new_path": "src/main/java/App.java",
    "old_path": "src/main/java/App.java",
    "a_mode": "100644",
    "b_mode": "100644",
    "new_file": false,
    "renamed_file": false,
    "deleted_file": false
  }
]
```

这个端点返回每个文件的详细差异信息，包括：
- `new_path`: 文件路径
- `diff`: 具体的代码差异
- `new_file`: 是否是新文件
- `deleted_file`: 是否被删除
- `renamed_file`: 是否被重命名

## 使用场景

### 场景1：查询commit修改的文件列表
**用户问**："这个commit 84ba36be2ac79b3d140ad697962a8bacc5e3a398 修改了哪些文件？"

**AI行为**：
1. 识别需要调用`get_commit_detail` API
2. 调用`gitApiClient.getCommitDetail(owner, repo, "84ba36be2ac79b3d140ad697962a8bacc5e3a398")`
3. 从返回的JSON中提取文件列表
4. 友好地回答用户："这个commit修改了以下文件：README.md, src/main/java/App.java"

### 场景2：查询commit的详细差异
**用户问**："这个commit具体改了什么代码？"

**AI行为**：
1. 识别需要调用`get_commit_diff` API
2. 调用`gitApiClient.getCommitDiff(owner, repo, commitSha)`
3. 解析返回的diff数据
4. 总结每个文件的主要变更

## 测试验证

### 测试步骤

1. **启动应用**：
   ```bash
   mvn clean package
   java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```

2. **打开AI Chat**：
   - 选择一个Git项目
   - 点击菜单：Chat -> AI Chat

3. **测试查询commit文件列表**：
   ```
   用户输入："这个commit 84ba36be2ac79b3d140ad697962a8bacc5e3a398 修改了哪些文件？"
   ```

4. **查看控制台日志**：
   ```
   [AI Chat] API Call: gitApiClient.getCommitDetail(owner, repo, 84ba36be2ac79b3d140ad697962a8bacc5e3a398)
   [AI Chat] API Response received, length: xxx chars
   ```

5. **验证AI回答**：
   - AI应该直接列出修改的文件
   - 不应该建议用户使用git命令

### 预期结果

**修复前**：
```
AI: 要查看这个commit修改了哪些文件，你可以使用以下git命令：
git show --name-only 84ba36be2ac79b3d140ad697962a8bacc5e3a398
```

**修复后**：
```
AI: 这个commit修改了以下文件：
1. README.md - 新增15行，删除3行
2. src/main/java/App.java - 新增8行，删除2行

总共修改了2个文件，新增23行，删除5行。
```

## 技术细节

### GitLab API参数说明

- `with_stats=true`: 返回commit的统计信息（additions, deletions, total）
- 默认情况下，GitLab API不返回文件列表，需要额外调用`/diff`端点

### GitHub API对比

GitHub的commit detail API默认就包含`files`数组，每个文件包含：
- `filename`: 文件名
- `additions`: 新增行数
- `deletions`: 删除行数
- `changes`: 总变更行数
- `status`: 文件状态（added/modified/removed）
- `patch`: 具体的diff内容

因此GitHub不需要额外的参数或端点。

## 相关文件

- `src/main/java/com/gitviewer/GitApiClient.java` - Git API客户端
- `src/main/java/com/gitviewer/AIChatDialog.java` - AI聊天对话框
- `src/main/java/com/gitviewer/GitLabApiClient.java` - GitLab API底层调用
- `src/main/java/com/gitviewer/GitHubApiClient.java` - GitHub API底层调用

## 参考资料

- [GitLab Commits API Documentation](https://docs.gitlab.com/api/commits/)
- [GitHub Commits API Documentation](https://docs.github.com/en/rest/commits/commits)
- GitLab API: `with_stats` parameter for commit statistics
- GitLab API: `/commits/:sha/diff` endpoint for detailed file changes

## 总结

通过添加`?with_stats=true`参数和新增`getCommitDiff`方法，AI Chat现在可以：
1. 直接获取commit的文件变更统计信息
2. 列出修改的文件列表
3. 显示每个文件的增删行数
4. 提供更详细的代码差异（如果需要）

用户不再需要手动使用git命令查看commit的文件变更，AI可以直接提供这些信息。
