# AI Chat - Get Single Commit 功能完成

## 问题描述

用户在 AI Chat 中询问"某个文件最后一次修改了什么"时，系统会重复调用 `get_file_commits` API 7-8 次，因为该 API 只返回 commit 列表（SHA、message、author），不包含具体的 diff 内容。

## 根本原因

- `get_file_commits` API 只返回 commit 的基本信息（SHA、message、author、date）
- AI 无法从这些信息中获取具体的代码修改内容（diff）
- AI 不知道有其他 API 可以获取 diff，所以反复调用同一个 API

## 解决方案

### 1. 新增 `GetSingleCommitTool`

创建了新的 Tool 来获取单个 commit 的详细信息（包括 diff）：

**文件**: `src/main/java/com/gitviewer/tools/GetSingleCommitTool.java`

```java
public class GetSingleCommitTool implements GitTool {
    @Override
    public String getName() {
        return "get_single_commit";
    }
    
    @Override
    public String getDescription() {
        return "获取单个 commit 的详细信息，包括文件修改内容（diff）。用于查看某个 commit 具体修改了什么";
    }
    
    @Override
    public Map<String, GitToolParameter> getParameters() {
        Map<String, GitToolParameter> params = new LinkedHashMap<>();
        params.put("owner", new GitToolParameter("string", "仓库所有者", true));
        params.put("repo", new GitToolParameter("string", "仓库名称", true));
        params.put("sha", new GitToolParameter("string", "commit SHA", true));
        return params;
    }
}
```

### 2. 实现 GitLab 双 API 调用

GitLab 的 commit API 不包含 diff，需要单独调用 `/diff` endpoint：

**文件**: `src/main/java/com/gitviewer/GitApiClient.java`

```java
public String getSingleCommit(String owner, String repo, String sha) throws IOException {
    if (isGitLab) {
        // GitLab 需要两个 API 调用：
        // 1. 获取 commit 基本信息
        String projectId = owner.replace("/", "%2F") + "%2F" + repo;
        String commitEndpoint = baseUrl + "/projects/" + projectId + "/repository/commits/" + sha;
        String commitInfo = GitLabApiClient.executeGet(commitEndpoint, token);
        
        // 2. 获取 commit diff
        String diffEndpoint = baseUrl + "/projects/" + projectId + "/repository/commits/" + sha + "/diff";
        String diffInfo = GitLabApiClient.executeGet(diffEndpoint, token);
        
        // 合并两个结果
        return "【Commit 信息】\n" + commitInfo + "\n\n【Diff 内容】\n" + diffInfo;
    } else {
        // GitHub API 默认就包含 files 数组和 patch（diff）
        return GitHubApiClient.getSingleCommit(owner, repo, sha, token);
    }
}
```

### 3. 注册新 Tool

在 `AIChatDialog.initializeToolRegistry()` 中注册：

```java
toolRegistry.register(new com.gitviewer.tools.GetSingleCommitTool(gitApiClient));
```

### 4. 更新 Agent 模式提示词

在 `askAIForNextAction()` 方法中添加了完整的步骤指导：

```
【常见问题的分步策略】
• 询问"文件最后一次修改了什么"或"最近的修改内容"：
  步骤1：get_file_commits 获取 commit 列表（返回 SHA、message、author）
  步骤2：从列表中选择最新的 commit SHA
  步骤3：get_single_commit 获取该 commit 的详细修改内容（diff）
  注意：get_file_commits 不包含 diff！必须调用 get_single_commit 才能看到具体修改

【决策规则】
4. **重要**：get_file_commits 只返回 commit 列表（SHA、message、author）！
   如果用户问具体修改了什么，必须再调用 get_single_commit 获取 diff
5. **禁止重复调用同一个 API**：如果上一轮已经调用过某个 API 并获得了数据，
   不要再次调用相同的 API
```

## API 对比

### GitHub API

- `GET /repos/:owner/:repo/commits/:sha`
- 默认包含 `files` 数组和 `patch`（diff）
- 一次调用即可获取完整信息

### GitLab API

- `GET /projects/:id/repository/commits/:sha` - 基本信息（不含 diff）
- `GET /projects/:id/repository/commits/:sha/diff` - diff 内容
- 需要两次调用并合并结果

## 预期效果

用户询问"某个文件最后一次修改了什么"时：

1. **第1轮**: AI 调用 `get_file_commits` 获取 commit 列表
2. **第2轮**: AI 从列表中提取最新的 commit SHA，调用 `get_single_commit` 获取 diff
3. **第3轮**: AI 基于收集的数据生成最终回答

总共 3 轮完成，不再重复调用同一个 API。

## 测试建议

1. 在 AI Chat 中询问："XXX 文件最后一次修改了什么？"
2. 观察控制台日志，确认：
   - 第1轮调用 `get_file_commits`
   - 第2轮调用 `get_single_commit`
   - 第3轮返回 FINISH
3. 验证 AI 的回答包含具体的代码修改内容（diff）

## 相关文件

- `src/main/java/com/gitviewer/tools/GetSingleCommitTool.java` - 新 Tool
- `src/main/java/com/gitviewer/GitApiClient.java` - GitLab 双 API 调用
- `src/main/java/com/gitviewer/GitHubApiClient.java` - GitHub API 实现
- `src/main/java/com/gitviewer/AIChatDialog.java` - Tool 注册和提示词优化

## 编译状态

✅ 编译成功 (mvn clean compile)

## 下一步

建议进行实际测试，验证：
1. GitLab 项目的 diff 是否正确返回
2. GitHub 项目的 diff 是否正确返回
3. AI 是否不再重复调用 `get_file_commits`
4. AI 的最终回答是否包含具体的代码修改内容
