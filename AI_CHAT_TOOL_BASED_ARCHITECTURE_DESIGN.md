# AI Chat Tool-Based Architecture 设计方案

## 设计理念

**核心原则**：让 AI 完成决策和调度，代码部分只提供 Tool 的能力给 AI。

## 当前架构的问题

### 1. 硬编码的验证逻辑
```java
// ❌ 代码在做决策：判断 API 指令是否"有效"
if (apiInstruction != null && isValidApiInstruction(apiInstruction)) {
    // ...
}

// ❌ 代码在做决策：判断是否应该结束循环
if (nextAction != null && nextAction.contains("\"action\": \"FINISH\"")) {
    break;
}
```

**问题**：代码在替 AI 做决策，限制了 AI 的灵活性。

### 2. 硬编码的 API 路由
```java
// ❌ 代码在做路由决策
switch (action) {
    case "get_repo":
        result = gitApiClient.getRepository(owner, repo);
        break;
    case "get_issues":
        result = gitApiClient.getIssues(owner, repo, issueState);
        break;
    // ...
}
```

**问题**：每次添加新 API 都需要修改代码，不够灵活。

## 改进方案：Tool-Based Architecture

### 架构流程

```
┌─────────────┐
│  用户问题    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────────┐
│  AI (决策中心)                       │
│  - 分析问题                          │
│  - 选择需要的 Tool                   │
│  - 决定是否继续或结束                │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  Tool Registry (工具注册表)          │
│  - 提供可用的 Tool 列表              │
│  - 每个 Tool 有明确的输入/输出定义   │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  Tool Executor (工具执行器)          │
│  - 根据 AI 指令执行对应的 Tool       │
│  - 不做任何决策，只执行              │
└──────┬──────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────┐
│  返回结果给 AI                       │
└─────────────────────────────────────┘
```

### 核心改进点

#### 1. Tool 定义（声明式）

```java
public class GitTool {
    private String name;
    private String description;
    private Map<String, ToolParameter> parameters;
    private Function<Map<String, String>, String> executor;
    
    // Tool 定义示例
    public static GitTool createGetRepoTool() {
        return new GitTool(
            "get_repo",
            "获取仓库基本信息（star数、描述、语言等）",
            Map.of(
                "owner", new ToolParameter("string", "仓库所有者", true),
                "repo", new ToolParameter("string", "仓库名称", true)
            ),
            params -> gitApiClient.getRepository(
                params.get("owner"), 
                params.get("repo")
            )
        );
    }
}
```

#### 2. Tool Registry（工具注册表）

```java
public class GitToolRegistry {
    private Map<String, GitTool> tools = new HashMap<>();
    
    public void registerTool(GitTool tool) {
        tools.put(tool.getName(), tool);
    }
    
    public String getToolsDescription() {
        // 自动生成 Tool 列表描述，发送给 AI
        StringBuilder sb = new StringBuilder();
        sb.append("【可用的 Tools】\n");
        for (GitTool tool : tools.values()) {
            sb.append(tool.getName()).append(" - ")
              .append(tool.getDescription()).append("\n");
            sb.append("  参数：").append(tool.getParametersDescription()).append("\n");
        }
        return sb.toString();
    }
    
    public GitTool getTool(String name) {
        return tools.get(name);
    }
}
```

#### 3. Tool Executor（工具执行器）

```java
public class GitToolExecutor {
    private GitToolRegistry registry;
    
    public String executeTool(String toolName, Map<String, String> params) {
        // ✅ 不做任何决策，只执行
        GitTool tool = registry.getTool(toolName);
        
        if (tool == null) {
            // 返回错误信息给 AI，让 AI 决定下一步
            return "ERROR: Tool '" + toolName + "' not found";
        }
        
        try {
            return tool.execute(params);
        } catch (Exception e) {
            // 返回错误信息给 AI，让 AI 决定下一步
            return "ERROR: " + e.getMessage();
        }
    }
}
```

#### 4. Agent Loop（简化版）

```java
private void processAgentMode(String userMessage) {
    // 初始化
    GitToolRegistry registry = initializeToolRegistry();
    GitToolExecutor executor = new GitToolExecutor(registry);
    StringBuilder collectedData = new StringBuilder();
    
    for (int iteration = 1; iteration <= maxIterations; iteration++) {
        // 询问 AI 下一步做什么
        String aiDecision = askAIForNextAction(
            userMessage, 
            collectedData.toString(),
            registry.getToolsDescription(),  // 提供 Tool 列表
            iteration, 
            maxIterations
        );
        
        // ✅ 不做决策，直接解析 AI 的指令
        AIDecision decision = parseAIDecision(aiDecision);
        
        // ✅ AI 决定是否结束
        if (decision.isFinish()) {
            break;
        }
        
        // ✅ 执行 AI 选择的 Tool
        String result = executor.executeTool(
            decision.getToolName(), 
            decision.getParameters()
        );
        
        // 收集数据
        collectedData.append("\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        collectedData.append("📊 Tool: ").append(decision.getToolName())
                    .append(" (第").append(iteration).append("轮)\n");
        collectedData.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        collectedData.append(result);
    }
    
    // 生成最终回答
    String finalAnswer = askAIForFinalAnswer(userMessage, collectedData.toString());
    displayAnswer(finalAnswer);
}
```

## 关键改进点对比

### 改进前（硬编码决策）

```java
// ❌ 代码在做决策
if (apiInstruction != null && isValidApiInstruction(apiInstruction)) {
    githubData = executeApiInstruction(apiInstruction);
}

// ❌ 代码在判断是否结束
if (nextAction.contains("\"action\": \"FINISH\"")) {
    break;
}

// ❌ 代码在做路由
switch (action) {
    case "get_repo": ...
    case "get_issues": ...
}
```

### 改进后（AI 决策）

```java
// ✅ AI 决定使用哪个 Tool
AIDecision decision = parseAIDecision(aiDecision);

// ✅ AI 决定是否结束
if (decision.isFinish()) {
    break;
}

// ✅ 代码只负责执行
String result = executor.executeTool(
    decision.getToolName(), 
    decision.getParameters()
);
```

## 优势

### 1. **灵活性**
- 添加新 Tool 只需注册，不需要修改核心逻辑
- AI 可以自由组合 Tool，不受代码限制

### 2. **可扩展性**
```java
// 添加新 Tool 非常简单
registry.registerTool(GitTool.createSearchCodeTool());
registry.registerTool(GitTool.createGetWorkflowsTool());
registry.registerTool(GitTool.createGetDependenciesTool());
```

### 3. **AI 主导**
- AI 完全控制决策流程
- 代码只提供能力，不做判断
- AI 可以根据上下文灵活调整策略

### 4. **错误处理**
```java
// ✅ 错误信息返回给 AI，让 AI 决定下一步
if (tool == null) {
    return "ERROR: Tool '" + toolName + "' not found. Available tools: " + 
           registry.getToolNames();
}
```

AI 可以：
- 选择其他 Tool
- 调整参数重试
- 决定放弃并返回 FINISH

## 实施步骤

### Phase 1: 重构 Tool 系统
1. 创建 `GitTool` 类
2. 创建 `GitToolRegistry` 类
3. 创建 `GitToolExecutor` 类
4. 将现有的 12 个 API 转换为 Tool 定义

### Phase 2: 简化 Agent Loop
1. 移除 `isValidApiInstruction()` 方法
2. 移除硬编码的 `switch-case` 路由
3. 使用 `GitToolExecutor` 执行 Tool
4. 让 AI 完全控制循环流程

### Phase 3: 优化提示词
1. 提供 Tool 的完整定义（包括参数类型、是否必需）
2. 告诉 AI 可以自由组合 Tool
3. 告诉 AI 错误信息会返回，可以根据错误调整策略

### Phase 4: 扩展 Tool 生态
1. 添加更多 Git 相关 Tool
2. 添加文件操作 Tool（如果需要）
3. 添加数据分析 Tool（如果需要）

## 示例：AI 的决策流程

### 用户问题："pom文件最后一些修改内容是什么"

#### 第1轮
```json
{
  "tool": "search_files",
  "parameters": {
    "filename": "pom.xml"
  },
  "reason": "需要先找到 pom.xml 的完整路径"
}
```

#### 第2轮
```json
{
  "tool": "get_file_commits",
  "parameters": {
    "filepath": "pom.xml"
  },
  "reason": "已找到文件路径，现在获取提交历史"
}
```

#### 第3轮
```json
{
  "action": "FINISH",
  "reason": "已收集足够信息，可以回答用户问题"
}
```

## 总结

**核心理念**：
- ✅ AI = 大脑（决策中心）
- ✅ Code = 手脚（执行工具）
- ❌ Code ≠ 决策者

**实施后的效果**：
- 代码更简洁、更灵活
- AI 有更大的自主权
- 更容易扩展新功能
- 更符合 AI Agent 的设计理念
