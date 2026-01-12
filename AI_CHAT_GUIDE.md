# AI Chat 功能使用指南

## 功能概述

Git Info Viewer 现在集成了 AI Chat 功能，可以通过自然语言查询 GitHub 仓库信息。

## 配置步骤

### 1. 配置 GitHub Token

1. 访问 GitHub: https://github.com/settings/tokens
2. 点击 "Generate new token (classic)"
3. 选择权限：
   - `repo` (访问私有仓库，可选)
   - `public_repo` (访问公开仓库)
   - `read:user` (读取用户信息)
4. 生成并复制 Token

### 2. 配置 AI API

在应用中：
1. 打开 **Chat → AI Settings**
2. 填写配置：
   - **GitHub Token**: 粘贴你的 GitHub Token
   - **API URL**: `https://portal.insuremo.com/api/mo-re/ai-qa-service/aiqa/api/chat`
   - **API Key**: 你的 AI API Key
   - **Model**: `qwen-max` (或其他支持的模型)
3. 点击 **OK** 保存

## 使用方法

### 打开聊天窗口

- 方式 1: 菜单 **Chat → Open AI Chat**
- 方式 2: 快捷键 **Ctrl+T**

### 示例查询

#### 1. 查询仓库信息
```
Show me information about facebook/react
```

#### 2. 查看 Issues
```
List open issues in microsoft/vscode
```

#### 3. 查看 Pull Requests
```
Show me pull requests for torvalds/linux
```

#### 4. 查看提交记录
```
Get recent commits from nodejs/node
```

#### 5. 查看分支
```
Show branches in kubernetes/kubernetes
```

#### 6. 搜索仓库
```
Search for machine learning repositories
```

#### 7. 查看用户信息
```
Tell me about user octocat
```

## API 格式说明

### Request 格式
```json
{
  "query": "最后一条用户消息",
  "messages": [
    {
      "role": "user",
      "content": "用户消息"
    },
    {
      "role": "assistant",
      "content": "AI 回复"
    }
  ],
  "temperature": 0.3,
  "llm_code": "qwen-max",
  "stream": "false"
}
```

### Response 格式
```json
{
  "data": "AI 的回复内容",
  "is_tool_calls": false
}
```

## 支持的 GitHub API

### 只读操作（安全）
- ✅ 查询仓库信息
- ✅ 查看 Issues
- ✅ 查看 Pull Requests
- ✅ 查看提交记录
- ✅ 查看分支
- ✅ 查看用户信息
- ✅ 搜索仓库/Issues
- ✅ 查看 Releases

### 不支持的操作
- ❌ 创建/修改 Issues
- ❌ 创建/合并 Pull Requests
- ❌ 修改仓库设置
- ❌ 删除操作

## 工作原理

1. **用户输入** → 发送到聊天窗口
2. **关键词识别** → 检测是否需要调用 GitHub API
3. **API 调用** → 如果需要，自动调用 GitHub API 获取数据
4. **AI 处理** → 将 GitHub 数据和用户问题发送给 AI
5. **结果展示** → AI 分析并返回友好的回答

## 注意事项

1. **GitHub Token 权限**：
   - 只需要读取权限
   - 不要授予写入权限（更安全）

2. **API 速率限制**：
   - 未认证：60 请求/小时
   - 已认证：5000 请求/小时

3. **仓库格式**：
   - 必须使用 `owner/repo` 格式
   - 例如：`facebook/react`，`microsoft/vscode`

4. **AI API 成本**：
   - 根据你的 API 提供商计费
   - 建议设置合理的使用限制

## 故障排除

### 问题 1: "AI API not configured"
**解决**: 检查 Chat → AI Settings 中的配置是否完整

### 问题 2: "GitHub authentication failed"
**解决**: 检查 GitHub Token 是否有效，是否过期

### 问题 3: "GitHub API rate limit exceeded"
**解决**: 等待速率限制重置，或使用认证 Token

### 问题 4: AI 无法理解查询
**解决**: 
- 使用明确的 `owner/repo` 格式
- 包含关键词：repository, issues, pull requests, commits 等

## 技术架构

```
用户输入
    ↓
AIChatDialog (UI)
    ↓
关键词检测 → GitHubApiClient → GitHub API
    ↓                              ↓
AIService ← ← ← ← ← ← ← GitHub 数据
    ↓
AI API (你的服务)
    ↓
显示结果
```

## 配置文件位置

所有配置保存在：`~/.gitviewer.properties`

包含：
- `github.token`
- `ai.api.url`
- `ai.api.key`
- `ai.model`

## 更新日志

### v1.0.0 (2026-01-12)
- ✅ 初始版本
- ✅ GitHub API 集成（只读）
- ✅ AI Chat 对话功能
- ✅ 多轮对话支持
- ✅ 自动 GitHub 数据获取
