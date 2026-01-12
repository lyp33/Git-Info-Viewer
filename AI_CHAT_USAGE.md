# AI Chat 使用说明 - 方案 1 实现

## 🎯 核心改进

### 1. AI 驱动的 API 调用
不再使用简单的关键词匹配，而是让 AI 理解用户意图并返回结构化的 API 调用指令。

### 2. 自动上下文识别
聊天窗口会自动识别当前选中的 Git 项目，无需手动输入 owner/repo。

## 🔄 工作流程

```
用户: "这个项目有多少 star？"
    ↓
AI 理解: 用户想查询当前项目的仓库信息
    ↓
AI 返回: {"action": "get_repo", "owner": "facebook", "repo": "react"}
    ↓
系统调用: GitHubApiClient.getRepository("facebook", "react", token)
    ↓
获取数据: {"stargazers_count": 220000, "forks_count": 45000, ...}
    ↓
AI 分析: 基于真实数据生成友好回答
    ↓
显示: "这个项目有 220,000 个 star 和 45,000 个 fork..."
```

## 📝 使用示例

### 场景 1: 查询当前项目
```
前提: 在左侧树中选择了 facebook/react 项目
用户: "这个项目有多少 star？"
AI: {"action": "get_repo"}  // 自动使用当前上下文
结果: 显示 react 项目的详细信息
```

### 场景 2: 查询其他项目
```
用户: "帮我看看 microsoft/vscode 的 issues"
AI: {"action": "get_issues", "owner": "microsoft", "repo": "vscode"}
结果: 显示 vscode 的 issues 列表
```

### 场景 3: 搜索功能
```
用户: "搜索一下机器学习相关的热门项目"
AI: {"action": "search_repos", "query": "machine learning"}
结果: 显示搜索结果
```

### 场景 4: 自然语言查询
```
用户: "最近有什么新功能？"
AI: {"action": "get_commits"}  // 使用当前上下文
结果: 显示最近的提交记录

用户: "有哪些分支？"
AI: {"action": "get_branches"}
结果: 显示所有分支

用户: "有没有待处理的 PR？"
AI: {"action": "get_prs", "state": "open"}
结果: 显示开放的 Pull Requests
```

## 🔧 支持的 API 操作

### 1. get_repo - 获取仓库信息
```json
{"action": "get_repo", "owner": "facebook", "repo": "react"}
```
返回: stars, forks, 描述, 语言等

### 2. get_issues - 获取 Issues
```json
{"action": "get_issues", "owner": "microsoft", "repo": "vscode", "state": "open"}
```
state 可选: open, closed, all

### 3. get_prs - 获取 Pull Requests
```json
{"action": "get_prs", "owner": "torvalds", "repo": "linux", "state": "open"}
```

### 4. get_commits - 获取提交记录
```json
{"action": "get_commits", "owner": "nodejs", "repo": "node"}
```
返回最近 10 条提交

### 5. get_branches - 获取分支
```json
{"action": "get_branches", "owner": "kubernetes", "repo": "kubernetes"}
```

### 6. get_user - 获取用户信息
```json
{"action": "get_user", "username": "octocat"}
```

### 7. search_repos - 搜索仓库
```json
{"action": "search_repos", "query": "machine learning"}
```

### 8. search_issues - 搜索 Issues
```json
{"action": "search_issues", "query": "bug label:critical"}
```

### 9. get_releases - 获取 Releases
```json
{"action": "get_releases", "owner": "golang", "repo": "go"}
```

## 🎨 上下文自动识别

### Git Remote URL 解析

系统会自动从当前选中的目录提取 Git 信息：

**支持的 URL 格式：**
```
HTTPS: https://github.com/facebook/react.git
SSH:   git@github.com:facebook/react.git
```

**提取结果：**
```
owner: facebook
repo: react
```

**在 System Prompt 中的体现：**
```
"Current repository context: facebook/react. ..."
```

AI 会知道当前上下文，用户可以直接问：
- "这个项目怎么样？" → 自动查询 facebook/react
- "有多少 issues？" → 自动查询 facebook/react 的 issues

## 💡 AI System Prompt

```
Current repository context: facebook/react. 

You are a helpful GitHub assistant. When users ask about GitHub information, 
you should first determine what GitHub API to call, then respond with a JSON instruction.

Available API actions:
1. get_repo - Get repository information
2. get_issues - Get repository issues (state: open/closed/all)
3. get_prs - Get pull requests (state: open/closed/all)
4. get_commits - Get recent commits
5. get_branches - Get repository branches
6. get_user - Get user information
7. search_repos - Search repositories
8. search_issues - Search issues
9. get_releases - Get repository releases

When you need to call an API, respond with JSON format:
{"action": "get_repo", "owner": "facebook", "repo": "react"}
{"action": "get_issues", "owner": "microsoft", "repo": "vscode", "state": "open"}

If the user doesn't specify owner/repo and we have a current context, use the current repository.
If you don't need to call any API, just answer the question directly.
```

## 🔍 实现细节

### 1. 两阶段 AI 调用

**第一阶段：获取 API 指令**
```java
String aiResponse = aiService.chat(chatHistory);
// AI 返回: {"action": "get_repo", "owner": "facebook", "repo": "react"}
```

**第二阶段：基于数据生成回答**
```java
String githubData = executeApiInstruction(aiResponse);
chatHistory.add(new ChatMessage("system", "GitHub API Response: " + githubData));
String finalResponse = aiService.chat(chatHistory);
// AI 返回: "这个项目有 220,000 个 star..."
```

### 2. JSON 指令解析

```java
private String executeApiInstruction(String instruction) {
    String action = extractJsonValue(instruction, "action");
    String owner = extractJsonValue(instruction, "owner");
    String repo = extractJsonValue(instruction, "repo");
    
    // 如果没有指定，使用当前上下文
    if (owner == null && currentOwner != null) {
        owner = currentOwner;
    }
    
    switch (action) {
        case "get_repo":
            return GitHubApiClient.getRepository(owner, repo, token);
        // ...
    }
}
```

### 3. Git 信息提取

```java
private void extractGitInfo() {
    File gitRepo = findGitRepository(currentDirectory);
    GitRepositoryInfo repoInfo = GitInfoExtractor.getRepositoryInfo(gitRepo);
    String remoteUrl = repoInfo.getRemoteUrls().get(0);
    String[] parts = extractOwnerRepoFromUrl(remoteUrl);
    currentOwner = parts[0];
    currentRepo = parts[1];
}
```

## 🚀 优势

### vs 关键词匹配
- ✅ 理解自然语言，不需要特定关键词
- ✅ 可以处理复杂的查询
- ✅ 自动推断用户意图
- ✅ 更好的用户体验

### vs Function Calling
- ✅ 不需要 AI API 支持 Function Calling
- ✅ 使用标准的 chat completion API
- ✅ 更灵活，可以自定义行为
- ✅ 降低对 AI API 的依赖

## 📊 示例对话

```
User: 打开 Chat (Ctrl+T)
System: 检测到当前项目: facebook/react

User: "这个项目有多少 star？"
AI: {"action": "get_repo"}
System: 调用 GitHub API...
AI: "React 项目目前有 220,000 个 star 和 45,000 个 fork，是一个非常受欢迎的前端框架。"

User: "最近有什么更新？"
AI: {"action": "get_commits"}
System: 调用 GitHub API...
AI: "最近的更新包括：
1. [feat] 添加新的 Hook API
2. [fix] 修复内存泄漏问题
3. [docs] 更新文档
..."

User: "有多少个开放的 issue？"
AI: {"action": "get_issues", "state": "open"}
System: 调用 GitHub API...
AI: "目前有 1,234 个开放的 issues，主要集中在性能优化和 bug 修复方面。"
```

## 🎯 下一步优化

1. **缓存机制**: 避免重复调用相同的 API
2. **错误重试**: API 调用失败时自动重试
3. **更多 API**: 支持更多 GitHub API 端点
4. **多轮对话**: 记住之前的查询结果
5. **可视化**: 将数据以图表形式展示
