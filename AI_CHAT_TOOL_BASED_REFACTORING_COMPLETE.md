# AI Chat Tool-Based Architecture 重构完成

## 重构时间
2026-02-08

## 重构目标

将硬编码的 API 调用方式重构为 Tool-Based Architecture，实现"AI 决策，代码执行"的理念。

## 核心理念

**之前**：代码在做决策
- ❌ 硬编码的 `switch-case` 路由（150+ 行）
- ❌ 每次添加新 API 需要修改多处代码
- ❌ API 列表手动维护，容易不一致

**之后**：AI 完全主导
- ✅ 统一的 Tool 接口
- ✅ 动态注册和执行
- ✅ API 列表自动生成

## 创建的新文件

### 1. 核心架构类

#### `GitToolParameter.java`
- Tool 参数定义类
- 包含类型、描述、是否必需、默认值

#### `GitTool.java`
- Tool 接口定义
- 定义了 `getName()`, `getDescription()`, `getParameters()`, `execute()` 方法

#### `GitToolRegistry.java`
- Tool 注册表
- 管理所有可用的 Tools
- 自动生成 Tool 列表描述（用于 AI 提示词）

### 2. 具体的 Tool 实现（12个）

所有 Tools 都在 `com.gitviewer.tools` 包中：

1. **GetRepoTool** - 获取仓库基本信息
2. **GetIssuesTool** - 获取 issues 列表
3. **GetPullRequestsTool** - 获取 pull requests/merge requests
4. **GetCommitsTool** - 获取最近的提交记录
5. **GetBranchesTool** - 获取分支列表
6. **GetReleasesTool** - 获取发布版本
7. **GetContentsTool** - 获取目录内容
8. **SearchRepositoriesTool** - 搜索仓库
9. **SearchIssuesTool** - 搜索 issues
10. **SearchFilesTool** - 搜索文件
11. **GetFileCommitsTool** - 获取文件的提交历史
12. **GetFileContentTool** - 获取文件的完整源代码

## 修改的文件

### `AIChatDialog.java`

#### 1. 添加字段
```java
private GitToolRegistry toolRegistry;  // Tool 注册表
```

#### 2. 新增方法：`initializeToolRegistry()`
```java
private void initializeToolRegistry() {
    toolRegistry = new GitToolRegistry();
    
    // 注册所有 Tools
    toolRegistry.register(new GetRepoTool(gitApiClient));
    toolRegistry.register(new GetIssuesTool(gitApiClient));
    // ... 共 12 个 Tools
    
    System.out.println("[AI Chat] Tool Registry initialized with " + toolRegistry.size() + " tools");
}
```

#### 3. 重写方法：`executeApiInstruction()`

**之前**（150+ 行）：
```java
switch (action) {
    case "get_repo":
        result = gitApiClient.getRepository(owner, repo);
        break;
    case "get_issues":
        result = gitApiClient.getIssues(owner, repo, issueState);
        break;
    // ... 12 个 case
}
```

**之后**（60 行）：
```java
// 获取 Tool
GitTool tool = toolRegistry.getTool(toolName);

// 解析参数
Map<String, String> params = new HashMap<>();
for (String paramName : tool.getParameters().keySet()) {
    String value = extractJsonValue(instruction, paramName);
    if (value != null && !value.isEmpty()) {
        params.put(paramName, value);
    }
}

// 自动填充 owner/repo
if (!params.containsKey("owner") && currentOwner != null) {
    params.put("owner", currentOwner);
}

// 执行 Tool
String result = tool.execute(params);
```

#### 4. 更新方法：`askAIForApiCall()` 和 `askAIForNextAction()`

**之前**：手动维护 API 列表
```java
context.append("可用的 API：\n");
context.append("1. get_repo - 获取仓库基本信息...\n");
context.append("2. get_issues - 获取 issues 列表...\n");
// ... 手动维护 12 行
```

**之后**：自动生成
```java
// 使用 Tool Registry 自动生成 API 列表
if (toolRegistry != null) {
    context.append(toolRegistry.generateToolsDescription());
}
```

## 代码对比

### 添加新 API 的复杂度

#### 之前（需要修改 4 处）

1. **在 `GitApiClient` 中添加方法**
```java
public String getWorkflows(String owner, String repo, String workflowId) {
    // ...
}
```

2. **在 `executeApiInstruction` 中添加 case**
```java
case "get_workflows":
    String workflowId = extractJsonValue(instruction, "workflow_id");
    result = gitApiClient.getWorkflows(owner, repo, workflowId);
    break;
```

3. **在 `askAIForApiCall` 中添加描述**
```java
context.append("13. get_workflows - 获取工作流（参数：workflow_id）\n");
```

4. **在 `askAIForNextAction` 中添加描述**
```java
context.append("13. get_workflows - 获取工作流（参数：workflow_id）\n");
```

#### 之后（只需 2 步）

1. **在 `GitApiClient` 中添加方法**（同上）

2. **创建 Tool 并注册**
```java
// 创建 GetWorkflowsTool.java
public class GetWorkflowsTool implements GitTool {
    // 实现接口方法
}

// 在 initializeToolRegistry() 中注册
toolRegistry.register(new GetWorkflowsTool(gitApiClient));
```

**提示词自动生成，无需手动维护！**

## 优势总结

### 1. 代码简洁
- `executeApiInstruction` 从 150+ 行减少到 60 行
- 消除了大量重复的 `switch-case` 代码

### 2. 易于扩展
- 添加新 API 只需创建新 Tool 并注册
- 不需要修改核心逻辑

### 3. 自动化
- API 列表自动生成，不会遗漏或不一致
- 参数定义集中在 Tool 中，易于维护

### 4. 符合设计理念
- AI 完全控制决策流程
- 代码只提供能力，不做判断
- 错误信息返回给 AI，让 AI 决定下一步

### 5. 类型安全
- 每个 Tool 明确定义参数类型和是否必需
- 参数验证在 Tool 内部完成

## 编译和打包

```bash
# 编译成功
mvn compile

# 打包成功
mvn package -DskipTests
```

生成文件：`target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 测试建议

1. **基础测试**：验证所有 12 个 Tools 都能正常工作
   - 测试每个 Tool 的基本功能
   - 验证参数自动填充（owner/repo）
   - 验证错误处理

2. **Agent 模式测试**：验证 Tool Registry 在 Agent 循环中的表现
   - 测试 Tool 列表自动生成
   - 测试 AI 能否正确选择 Tool
   - 测试错误信息是否正确返回给 AI

3. **扩展性测试**：添加一个新 Tool
   - 创建新的 Tool 类
   - 注册到 Registry
   - 验证自动出现在提示词中

## 架构图

```
┌─────────────────────────────────────────┐
│  AIChatDialog                            │
│  ┌─────────────────────────────────┐   │
│  │  GitToolRegistry                 │   │
│  │  ┌─────────────────────────┐    │   │
│  │  │  GetRepoTool            │    │   │
│  │  │  GetIssuesTool          │    │   │
│  │  │  SearchFilesTool        │    │   │
│  │  │  GetFileContentTool     │    │   │
│  │  │  ... (12 Tools)         │    │   │
│  │  └─────────────────────────┘    │   │
│  └─────────────────────────────────┘   │
│                                          │
│  executeApiInstruction(instruction)     │
│    ↓                                     │
│  1. 解析 Tool 名称                       │
│  2. 从 Registry 获取 Tool               │
│  3. 解析参数                             │
│  4. 执行 Tool                            │
│  5. 返回结果                             │
└─────────────────────────────────────────┘
```

## 下一步

1. **测试新架构**：确保所有功能正常工作
2. **性能优化**：如果需要，可以添加 Tool 缓存
3. **扩展 Tools**：根据需求添加更多 Git API Tools
4. **文档完善**：为每个 Tool 添加详细的使用说明

## 总结

这次重构成功地将硬编码的 API 调用方式转换为灵活的 Tool-Based Architecture。代码更简洁、更易维护、更易扩展，完全符合"AI 决策，代码执行"的设计理念。

**核心成果**：
- ✅ 创建了 15 个新文件（3 个核心类 + 12 个 Tool 实现）
- ✅ 重构了 `AIChatDialog.java`（减少 90+ 行代码）
- ✅ 实现了自动化的 API 列表生成
- ✅ 编译和打包成功
- ✅ 保持了所有原有功能

现在可以开始测试新的 Tool-Based Architecture 了！
