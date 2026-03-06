# AI Chat Agent Loop 上下文修复 - 设计文档

## 架构概述

### 当前架构（Simple Mode）

```
sendMessage()
  ↓
askAIForApiCall() → 询问需要什么 API
  ↓
executeApiInstruction() → 执行一次 API
  ↓
askAIForFinalAnswer() → 生成最终回答
```

### 新架构（Agent Mode）

```
sendMessage()
  ↓
判断模式
  ├─ Simple Mode → processSimpleMode()
  └─ Agent Mode → processAgentMode()
                    ↓
                    循环开始（最多 N 次）
                      ↓
                      askAIForNextAction() → 询问下一步
                      ↓
                      解析决策
                      ├─ FINISH → 退出循环
                      └─ API 调用 → executeApiInstruction()
                                    ↓
                                    收集数据（带来源标注）
                                    ↓
                                    继续循环
                    ↓
                    askAIForFinalAnswer() → 生成最终回答
```

## 核心类设计

### AIChatDialog 类修改

#### 新增字段

```java
// Agent 模式相关
private int currentIteration = 0;  // 当前循环轮次
private StringBuilder collectedData = new StringBuilder();  // 已收集的数据
```

#### 修改方法

##### 1. sendMessage() - 入口方法

```java
private void sendMessage() {
    String userMessage = inputField.getText().trim();
    if (userMessage.isEmpty()) return;
    
    // 显示用户消息
    appendStyledMessage("You", userMessage, userStyle);
    inputField.setText("");
    
    // 禁用输入
    sendButton.setEnabled(false);
    inputField.setEnabled(false);
    
    // 在后台线程处理
    Thread thread = new Thread(() -> {
        try {
            // 判断模式
            AppSettings settings = AppSettings.getInstance();
            String chatMode = settings.getAiChatMode();
            
            if ("agent".equals(chatMode)) {
                System.out.println("[AI Chat] Chat Mode: agent");
                processAgentMode(userMessage);
            } else {
                System.out.println("[AI Chat] Chat Mode: simple");
                processSimpleMode(userMessage);
            }
            
            // 更新历史记录
            chatHistory.add(new AIService.ChatMessage("user", userMessage));
            
            // 重新启用输入
            SwingUtilities.invokeLater(() -> {
                sendButton.setEnabled(true);
                inputField.setEnabled(true);
                inputField.requestFocus();
            });
            
        } catch (Exception e) {
            handleError(e);
        }
    });
    thread.setDaemon(true);
    thread.start();
}
```

##### 2. processSimpleMode() - 简单模式（现有逻辑）

```java
private void processSimpleMode(String userMessage) {
    System.out.println("========== Simple Mode ==========");
    
    // 显示"正在分析"提示
    SwingUtilities.invokeLater(() -> appendSystemMessage("💭 正在分析问题..."));
    
    // 第一阶段：询问 AI 需要什么 API
    String apiInstruction = askAIForApiCall(userMessage);
    SwingUtilities.invokeLater(() -> removeLastMessage());
    
    String githubData = null;
    
    // 如果需要调用 API
    if (apiInstruction != null && isValidApiInstruction(apiInstruction)) {
        SwingUtilities.invokeLater(() -> appendSystemMessage("🔍 正在查询 Git..."));
        githubData = executeApiInstruction(apiInstruction);
        SwingUtilities.invokeLater(() -> removeLastMessage());
    }
    
    // 第二阶段：生成最终回答
    SwingUtilities.invokeLater(() -> appendSystemMessage("🤖 AI 正在生成回答..."));
    String finalAnswer = askAIForFinalAnswer(userMessage, githubData);
    SwingUtilities.invokeLater(() -> removeLastMessage());
    
    // 显示回答
    String response = finalAnswer;
    SwingUtilities.invokeLater(() -> {
        appendStyledMessage("Assistant", response, assistantStyle);
        chatHistory.add(new AIService.ChatMessage("assistant", response));
    });
    
    System.out.println("========== Simple Mode 完成 ==========\n");
}
```

##### 3. processAgentMode() - Agent 模式（新增）

```java
private void processAgentMode(String userMessage) {
    AppSettings settings = AppSettings.getInstance();
    int maxIterations = settings.getAiMaxIterations();
    
    System.out.println("========== Agent Mode ==========");
    System.out.println("[AI Chat] Max Iterations: " + maxIterations);
    
    // 重置状态
    currentIteration = 0;
    collectedData = new StringBuilder();
    
    // Agent 循环
    for (currentIteration = 1; currentIteration <= maxIterations; currentIteration++) {
        System.out.println("\n========== Agent循环 第" + currentIteration + "轮 ==========");
        
        // 显示进度
        final int iteration = currentIteration;
        SwingUtilities.invokeLater(() -> 
            appendSystemMessage("🔄 Agent 循环 第" + iteration + "/" + maxIterations + "轮..."));
        
        // 询问 AI 下一步做什么
        String nextAction = askAIForNextAction(userMessage, collectedData.toString(), 
                                               currentIteration, maxIterations);
        
        SwingUtilities.invokeLater(() -> removeLastMessage());
        
        System.out.println("[AI Chat] Agent decision: " + nextAction);
        
        // 解析 AI 决策
        if (nextAction != null && nextAction.contains("\"action\": \"FINISH\"")) {
            System.out.println("[AI Chat] Agent decided to FINISH");
            break;  // AI 认为信息足够，结束循环
        }
        
        // 检查是否是有效的 API 调用
        if (nextAction == null || !isValidApiInstruction(nextAction)) {
            System.out.println("[AI Chat] Invalid API instruction, ending loop");
            break;
        }
        
        // 执行 API 调用
        SwingUtilities.invokeLater(() -> 
            appendSystemMessage("🔍 正在调用 Git API..."));
        
        String apiData = executeApiInstruction(nextAction);
        
        SwingUtilities.invokeLater(() -> removeLastMessage());
        
        // 收集数据（带来源标注）
        if (apiData != null && !apiData.isEmpty()) {
            String apiName = extractJsonValue(nextAction, "action");
            collectedData.append("\n\n【来自 API: ").append(apiName)
                        .append(" (第").append(currentIteration).append("轮)】\n");
            collectedData.append(apiData);
            
            System.out.println("[AI Chat] 第" + currentIteration + "轮数据收集成功");
        } else {
            System.out.println("[AI Chat] 第" + currentIteration + "轮 API 返回空数据");
        }
    }
    
    System.out.println("\n[AI Chat] Agent模式完成，共执行" + currentIteration + "轮");
    
    // 生成最终回答
    SwingUtilities.invokeLater(() -> appendSystemMessage("🤖 AI 正在生成最终回答..."));
    
    String finalAnswer = askAIForFinalAnswer(userMessage, collectedData.toString());
    
    SwingUtilities.invokeLater(() -> removeLastMessage());
    
    // 显示回答
    String response = finalAnswer;
    SwingUtilities.invokeLater(() -> {
        appendStyledMessage("Assistant", response, assistantStyle);
        chatHistory.add(new AIService.ChatMessage("assistant", response));
    });
    
    System.out.println("========== Agent Mode 完成 ==========\n");
}
```

##### 4. askAIForNextAction() - 询问 AI 下一步行动（新增）

```java
private String askAIForNextAction(String userQuestion, String collectedData, 
                                  int currentIteration, int maxIterations) {
    try {
        System.out.println("\n========== 询问 AI 下一步行动 ==========");
        System.out.println("[AI Chat] Current iteration: " + currentIteration + "/" + maxIterations);
        System.out.println("[AI Chat] Collected data length: " + collectedData.length() + " chars");
        
        List<AIService.ChatMessage> messages = new ArrayList<>();
        
        String platformName = (gitApiClient != null) ? gitApiClient.getPlatformName() : "Git";
        
        // 构建完整上下文
        StringBuilder context = new StringBuilder();
        context.append("你是一个智能 ").append(platformName).append(" Agent。\n\n");
        
        // 当前状态
        context.append("【当前状态】\n");
        context.append("- 用户问题：").append(userQuestion).append("\n");
        context.append("- 当前轮次：").append(currentIteration).append("/").append(maxIterations).append("\n");
        if (currentOwner != null && currentRepo != null) {
            context.append("- 当前项目：").append(currentOwner).append("/").append(currentRepo).append("\n");
            context.append("- 当前分支：").append(currentBranch).append("\n");
        }
        context.append("\n");
        
        // 已收集的数据
        if (collectedData != null && !collectedData.isEmpty()) {
            context.append("【已收集的数据】\n");
            context.append(collectedData);
            context.append("\n\n");
        }
        
        // 可用的 API 列表（每轮都包含）
        context.append("【可用的 API】\n");
        context.append("1. get_repo - 获取仓库基本信息（star数、描述、语言等）\n");
        context.append("2. get_issues - 获取 issues 列表（参数：state=open/closed/all）\n");
        context.append("3. get_prs - 获取 pull requests/merge requests（参数：state=open/closed/all）\n");
        context.append("4. get_commits - 获取最近的提交记录\n");
        context.append("5. get_branches - 获取分支列表\n");
        context.append("6. get_releases - 获取发布版本\n");
        context.append("7. get_contents - 获取目录内容（参数：path）\n");
        context.append("8. search_repos - 搜索仓库（参数：query）\n");
        context.append("9. search_issues - 搜索 issues（参数：query）\n");
        context.append("10. search_files - 搜索文件（参数：filename）\n");
        context.append("11. get_file_commits - 获取文件的提交历史（参数：filepath）\n");
        context.append("12. get_file_content - 获取文件的完整源代码（参数：filepath，可选：branch）\n");
        context.append("\n");
        
        // 决策指南
        context.append("【请分析】\n");
        context.append("1. 已收集的数据是否足够回答用户问题？\n");
        context.append("2. 如果不够，下一步应该调用哪个 API？\n");
        context.append("3. 为什么需要这个 API？\n");
        context.append("\n");
        
        context.append("【返回格式】\n");
        context.append("如果数据足够：\n");
        context.append("{\"action\": \"FINISH\", \"reason\": \"已收集足够信息\"}\n");
        context.append("\n");
        context.append("如果需要更多数据：\n");
        context.append("{\"action\": \"get_commits\", \"reason\": \"需要查看提交历史\"}\n");
        context.append("{\"action\": \"get_file_content\", \"filepath\": \"pom.xml\", \"reason\": \"需要查看文件内容\"}\n");
        context.append("\n");
        context.append("注意：只返回 JSON，不要有其他文字。\n");
        
        messages.add(new AIService.ChatMessage("system", context.toString()));
        messages.add(new AIService.ChatMessage("user", "下一步应该做什么？"));
        
        System.out.println("[AI Chat] Sending request to AI API...");
        System.out.println("[AI Chat] Context length: " + context.length() + " chars");
        
        String response = aiService.chat(messages);
        
        System.out.println("[AI Chat] AI Response: " + response);
        System.out.println("========== 询问完成 ==========\n");
        
        return response.trim();
        
    } catch (Exception e) {
        System.err.println("[AI Chat] ERROR in askAIForNextAction: " + e.getMessage());
        e.printStackTrace();
        return null;
    }
}
```

## 数据流设计

### Agent 循环数据流

```
第1轮：
  输入：
    - 用户问题
    - API 列表
    - 已收集数据：（空）
  
  AI 决策：
    {"action": "get_commits"}
  
  执行 API：
    get_commits() → 返回 10 个 commit
  
  收集数据：
    【来自 API: get_commits (第1轮)】
    [commit 数据...]

第2轮：
  输入：
    - 用户问题
    - API 列表
    - 已收集数据：
      【来自 API: get_commits (第1轮)】
      [commit 数据...]
  
  AI 决策：
    {"action": "get_file_content", "filepath": "pom.xml"}
  
  执行 API：
    get_file_content() → 返回文件内容
  
  收集数据：
    【来自 API: get_commits (第1轮)】
    [commit 数据...]
    
    【来自 API: get_file_content (第2轮)】
    [文件内容...]

第3轮：
  输入：
    - 用户问题
    - API 列表
    - 已收集数据：
      【来自 API: get_commits (第1轮)】
      [commit 数据...]
      
      【来自 API: get_file_content (第2轮)】
      [文件内容...]
  
  AI 决策：
    {"action": "FINISH", "reason": "信息足够"}
  
  → 退出循环

生成最终回答：
  输入：
    - 用户问题
    - 所有收集的数据（带来源标注）
  
  输出：
    友好的中文回答
```

## 提示词设计

### Agent 决策提示词

```
你是一个智能 Git Agent。

【当前状态】
- 用户问题：找出最近修改了 pom.xml 的 commit
- 当前轮次：2/5
- 当前项目：facebook/react
- 当前分支：main

【已收集的数据】
【来自 API: get_commits (第1轮)】
[
  {"sha": "abc123", "message": "Fix bug", "author": "John"},
  {"sha": "def456", "message": "Update deps", "author": "Jane"},
  ...
]

【可用的 API】
1. get_repo - 获取仓库基本信息
2. get_commits - 获取提交记录
3. get_file_content - 获取文件内容
...

【请分析】
1. 已收集的数据是否足够回答用户问题？
2. 如果不够，下一步应该调用哪个 API？
3. 为什么需要这个 API？

【返回格式】
如果数据足够：
{"action": "FINISH", "reason": "已收集足够信息"}

如果需要更多数据：
{"action": "get_file_commits", "filepath": "pom.xml", "reason": "需要查看 pom.xml 的提交历史"}

注意：只返回 JSON，不要有其他文字。
```

### 最终回答提示词

```
你是一个友好的 Git 助手。
当前项目：facebook/react

请用中文友好地回答用户的问题。

【重要】你的能力和任务：
1. 你可以直接读取和分析数据
2. 我已经为你获取了完整的数据
3. 你必须直接展示和分析这些数据

【严格禁止】：
- 禁止说"无法访问"、"无法读取"、"不能直接查看"
- 禁止建议用户使用 git 命令、curl 命令或访问网页
- 禁止说需要其他工具或权限

【你应该做的】：
- 直接展示数据内容
- 分析数据的含义
- 回答用户的问题

用户问题：找出最近修改了 pom.xml 的 commit

【已收集的数据】
【来自 API: get_commits (第1轮)】
[commit 数据...]

【来自 API: get_file_commits (第2轮)】
[pom.xml 的提交历史...]

请基于以上数据回答用户的问题。
```

## 错误处理

### 1. API 调用失败

```java
String apiData = executeApiInstruction(nextAction);
if (apiData == null || apiData.isEmpty()) {
    // 记录错误但继续循环
    collectedData.append("\n\n【API 调用失败】\n");
    collectedData.append("API: ").append(extractJsonValue(nextAction, "action")).append("\n");
    collectedData.append("原因：返回空数据\n");
    
    // 如果是最后一轮，强制结束
    if (currentIteration == maxIterations) {
        break;
    }
    // 否则继续下一轮
    continue;
}
```

### 2. AI 返回无效决策

```java
if (nextAction == null || !isValidApiInstruction(nextAction)) {
    System.out.println("[AI Chat] Invalid decision, ending loop");
    
    // 显示警告
    SwingUtilities.invokeLater(() -> 
        appendSystemMessage("⚠️ AI 返回无效决策，提前结束循环"));
    
    break;
}
```

### 3. 达到最大循环次数

```java
if (currentIteration == maxIterations) {
    System.out.println("[AI Chat] Reached max iterations");
    
    // 显示提示
    SwingUtilities.invokeLater(() -> 
        appendSystemMessage("ℹ️ 已达到最大循环次数，生成回答..."));
}
```

## 性能优化

### 1. 数据大小限制

```java
// 每个 API 返回的数据限制为 20KB（除了 get_file_content 为 50KB）
if (result != null && result.length() > 20000) {
    result = result.substring(0, 20000) + "\n\n...[数据过多，已截断]";
}
```

### 2. 上下文大小限制

```java
// 如果收集的数据过大，只保留最近的数据
if (collectedData.length() > 100000) {
    // 保留最后 80KB 的数据
    String data = collectedData.toString();
    collectedData = new StringBuilder();
    collectedData.append("...[早期数据已省略]\n\n");
    collectedData.append(data.substring(data.length() - 80000));
}
```

### 3. 并发处理

```java
// 使用守护线程，防止阻止应用退出
Thread thread = new Thread(() -> {
    // Agent 循环处理
});
thread.setDaemon(true);
thread.start();
```

## 测试策略

### 单元测试

1. **测试 processAgentMode()**
   - 验证循环次数限制
   - 验证 FINISH 提前退出
   - 验证数据收集和标注

2. **测试 askAIForNextAction()**
   - 验证上下文包含 API 列表
   - 验证上下文包含已收集数据
   - 验证轮次信息正确

3. **测试数据来源标注**
   - 验证格式：`【来自 API: xxx (第N轮)】`
   - 验证多次调用的区分

### 集成测试

1. **Simple Mode 兼容性**
   - 验证 Simple Mode 仍然正常工作
   - 验证模式切换正确

2. **Agent Mode 完整流程**
   - 验证多轮循环
   - 验证数据累积
   - 验证最终回答生成

### 用户验收测试

1. **复杂查询场景**
   - 查找修改特定文件的 commit
   - 对比两个分支
   - 统计提交数

2. **日志验证**
   - 验证每轮循环日志清晰
   - 验证数据来源标注正确
   - 验证决策过程可追踪

## 实施计划

### Phase 1: 核心实现（2小时）
- [ ] 实现 `processAgentMode()` 方法
- [ ] 实现 `askAIForNextAction()` 方法
- [ ] 修改 `sendMessage()` 支持模式判断

### Phase 2: 数据标注（30分钟）
- [ ] 实现数据来源标注
- [ ] 实现轮次标记

### Phase 3: 错误处理（30分钟）
- [ ] 实现 API 失败处理
- [ ] 实现无效决策处理
- [ ] 实现最大循环次数处理

### Phase 4: 测试和调试（1小时）
- [ ] 单元测试
- [ ] 集成测试
- [ ] 用户验收测试

### Phase 5: 文档更新（30分钟）
- [ ] 更新 `AI_CHAT_AGENT_MODE_COMPLETE.md`
- [ ] 创建测试报告
- [ ] 更新用户指南

## 风险和缓解

### 风险1: AI 决策不稳定

**描述**: AI 可能返回无效或不一致的决策

**缓解**:
- 严格的 JSON 格式验证
- 提供清晰的决策示例
- 失败时提前结束循环

### 风险2: 上下文过大

**描述**: 多轮循环后上下文可能超过 AI 限制

**缓解**:
- 每个 API 返回数据限制 20KB
- 总上下文限制 100KB
- 超过限制时截断早期数据

### 风险3: 性能问题

**描述**: 多轮循环可能导致响应时间过长

**缓解**:
- 限制最大循环次数（默认 5 次）
- 使用后台线程处理
- 显示进度提示

## 成功指标

1. **功能指标**
   - Agent 循环正确执行
   - 每轮都包含完整上下文
   - 数据来源标注清晰

2. **性能指标**
   - 单轮循环 < 5 秒
   - 总循环时间 < 30 秒

3. **用户满意度**
   - 用户确认问题已解决
   - 复杂查询能正确处理
   - 日志清晰易懂
