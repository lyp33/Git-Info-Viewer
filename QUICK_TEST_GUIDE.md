# AI Chat 功能快速测试指南

## ✅ 应用已启动

应用程序已成功打包并启动！

## 🔧 配置步骤

### 1. 配置 AI 和 GitHub 设置

1. 在应用中点击菜单：**Chat → AI Settings**
2. 填写以下信息：

**GitHub Token**:
- 访问：https://github.com/settings/tokens
- 生成新 token（只需要 `public_repo` 权限）
- 复制并粘贴到 GitHub Token 字段

**AI API 配置**:
- **API URL**: `https://portal.insuremo.com/api/mo-re/ai-qa-service/aiqa/api/chat`
- **API Key**: 你的 AI API Key
- **Model**: `qwen-max`

3. 点击 **OK** 保存

### 2. 选择一个 Git 项目

1. 点击菜单：**File → Select Root Directory**
2. 选择一个包含 Git 仓库的目录
3. 或者在左侧树中选择一个 Git 项目目录

### 3. 打开 AI Chat

- 方式 1: 点击菜单 **Chat → Open AI Chat**
- 方式 2: 按快捷键 **Ctrl+T**

## 🧪 测试用例

### 测试 1: 自动上下文识别

**前提**: 选择了一个 Git 项目（例如你的项目）

**测试输入**:
```
这个项目有多少 star？
```

**预期行为**:
1. AI 识别当前项目上下文
2. 返回 JSON: `{"action": "get_repo"}`
3. 系统调用 GitHub API
4. AI 基于真实数据回答

**检查点**:
- ✅ 聊天窗口标题显示当前项目
- ✅ 控制台输出显示 API 调用
- ✅ 返回真实的 star 数量

### 测试 2: 查询 Issues

**测试输入**:
```
有多少个开放的 issue？
```

**预期行为**:
1. AI 返回: `{"action": "get_issues", "state": "open"}`
2. 调用 GitHub API 获取 issues
3. AI 分析并回答

### 测试 3: 查询其他项目

**测试输入**:
```
帮我看看 facebook/react 的信息
```

**预期行为**:
1. AI 解析出 owner/repo
2. 返回: `{"action": "get_repo", "owner": "facebook", "repo": "react"}`
3. 获取 React 项目信息

### 测试 4: 查询提交记录

**测试输入**:
```
最近有什么更新？
```

**预期行为**:
1. AI 返回: `{"action": "get_commits"}`
2. 获取最近 10 条提交
3. AI 总结提交内容

### 测试 5: 搜索功能

**测试输入**:
```
搜索一下机器学习相关的项目
```

**预期行为**:
1. AI 返回: `{"action": "search_repos", "query": "machine learning"}`
2. 搜索 GitHub 仓库
3. 显示搜索结果

## 🔍 调试信息

### 查看控制台输出

应用会在控制台输出详细的调试信息：

```
[AI Chat] Current context: facebook/react
[AI Service] Sending request to: https://...
[AI Service] Request body: {...}
[AI Service] Response Code: 200
[AI Chat] Executing API instruction: {"action": "get_repo"}
[GitHub API] Request: https://api.github.com/repos/facebook/react
[GitHub API] Response Code: 200
```

### 常见问题排查

**问题 1: "AI API not configured"**
- 检查 Chat → AI Settings 是否填写完整
- 确认 API URL、API Key、Model 都已填写

**问题 2: "GitHub authentication failed"**
- 检查 GitHub Token 是否有效
- 确认 Token 有 `public_repo` 权限

**问题 3: AI 没有返回 JSON 指令**
- 检查 AI API 是否正常工作
- 查看控制台的 AI 响应内容
- 可能需要调整 system prompt

**问题 4: 无法识别当前项目**
- 确保选中的目录是 Git 仓库
- 检查 Git remote URL 是否配置
- 查看控制台的 "Current context" 输出

## 📊 预期的完整对话流程

```
System: AI Chat - GitHub Assistant
System: Current repository context: your-owner/your-repo

You: 这个项目有多少 star？

[控制台输出]
[AI Chat] Current context: your-owner/your-repo
[AI Service] Sending request...
[AI Service] Response: {"action": "get_repo"}
[AI Chat] Executing API instruction: {"action": "get_repo"}
[GitHub API] Request: https://api.github.com/repos/your-owner/your-repo
[GitHub API] Response Code: 200
[AI Service] Sending request with GitHub data...