# AI Chat Agent Loop 测试指南

## 测试准备

### 1. 启动应用

```bash
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

或使用批处理文件：
```bash
run-app.bat
```

### 2. 配置 AI Settings

1. 打开菜单：`Chat -> AI Settings`
2. 配置 AI API：
   - API URL: `https://api.openai.com/v1/chat/completions`（或其他兼容的 API）
   - API Key: 你的 API Key
   - Model: `gpt-3.5-turbo` 或 `gpt-4`

3. 选择 Chat Mode：
   - **Simple Mode**: 快速测试
   - **Agent Mode**: 多轮推理测试

4. 设置 Max Iterations（仅 Agent Mode）：
   - 建议：5 次
   - 测试范围：1-10 次

### 3. 选择测试项目

在左侧目录树中选择一个 Git 项目，或者在 AI Chat 中手动输入项目路径。

## 测试场景

### 场景 1: Simple Mode - 基础功能测试

**目的**：验证 Simple Mode 仍然正常工作

**步骤**：
1. 在 AI Settings 中选择 `Simple Mode`
2. 打开 AI Chat（`Chat -> AI Chat`）
3. 输入问题：`这个项目有多少 star？`

**预期结果**：
```
后台日志：
[AI Chat] Chat Mode: simple
========== 第一阶段：询问 AI 需要调用哪个 API ==========
[AI Chat] AI returned instruction: {"action": "get_repo"}
[AI Chat] Valid API instruction detected
========== 第二阶段：生成友好回答 ==========
[AI Chat] Final answer generated
========== Simple Mode 完成 ==========

UI 显示：
Assistant: 这个项目有 XXX 个 star...
```

**验收标准**：
- ✅ 只调用一次 API
- ✅ 生成友好的中文回答
- ✅ 耗时 3-6 秒

---

### 场景 2: Agent Mode - 简单查询

**目的**：验证 Agent Mode 基础循环功能

**步骤**：
1. 在 AI Settings 中选择 `Agent Mode`，Max Iterations = 5
2. 打开 AI Chat
3. 输入问题：`最近有什么更新？`

**预期结果**：
```
后台日志：
[AI Chat] Chat Mode: agent
========== Agent Mode ==========
[AI Chat] Max Iterations: 5

========== Agent循环 第1轮 ==========
[AI Chat] Agent decision: {"action": "get_commits"}
[AI Chat] 第1轮数据收集成功

========== Agent循环 第2轮 ==========
[AI Chat] Agent decision: {"action": "FINISH", "reason": "已有足够信息"}
[AI Chat] Agent decided to FINISH

[AI Chat] Agent模式完成，共执行2轮
========== 生成最终回答 ==========
========== Agent Mode 完成 ==========

UI 显示：
🔄 Agent 循环 第1/5轮...
🔍 正在调用 Git API...
🔄 Agent 循环 第2/5轮...
🤖 AI 正在生成最终回答...
Assistant: 最近的更新包括...
```

**验收标准**：
- ✅ 执行 2 轮循环
- ✅ AI 主动返回 FINISH
- ✅ 每轮都显示进度提示
- ✅ 生成友好的中文回答

---

### 场景 3: Agent Mode - 复杂查询（核心测试）

**目的**：验证多轮推理和上下文传递

**步骤**：
1. 在 AI Settings 中选择 `Agent Mode`，Max Iterations = 5
2. 打开 AI Chat
3. 输入问题：`找出最近修改了 pom.xml 的 commit`

**预期结果**：
```
后台日志：
========== Agent循环 第1轮 ==========
========== 询问 AI 下一步行动 ==========
[AI Chat] Context length: 1234 chars
[AI Chat] AI Response: {"action": "get_commits"}
[AI Chat] 第1轮数据收集成功
[AI Chat] 已收集数据总长度: 5678 chars

========== Agent循环 第2轮 ==========
========== 询问 AI 下一步行动 ==========
[AI Chat] Context length: 6789 chars  ← 包含第1轮数据
[AI Chat] AI Response: {"action": "get_file_commits", "filepath": "pom.xml"}
[AI Chat] 第2轮数据收集成功
[AI Chat] 已收集数据总长度: 8901 chars

========== Agent循环 第3轮 ==========
========== 询问 AI 下一步行动 ==========
[AI Chat] Context length: 9012 chars  ← 包含第1、2轮数据
[AI Chat] AI Response: {"action": "FINISH"}
[AI Chat] Agent decided to FINISH

[AI Chat] Agent模式完成，共执行3轮
```

**验收标准**：
- ✅ 执行 3 轮循环
- ✅ 第 2 轮上下文包含第 1 轮数据
- ✅ 第 3 轮上下文包含第 1、2 轮数据
- ✅ 每轮上下文都包含完整的 API 列表
- ✅ 数据带有来源标注：`📊 数据来源: get_commits (第1轮)`
- ✅ AI 找到正确答案后主动 FINISH

---

### 场景 4: Agent Mode - 最大循环次数限制

**目的**：验证循环次数限制

**步骤**：
1. 在 AI Settings 中选择 `Agent Mode`，Max Iterations = 3
2. 打开 AI Chat
3. 输入一个复杂问题（需要 5 步才能完成）

**预期结果**：
```
后台日志：
========== Agent循环 第1轮 ==========
[AI Chat] Agent decision: {"action": "get_commits"}

========== Agent循环 第2轮 ==========
[AI Chat] Agent decision: {"action": "get_file_commits", "filepath": "pom.xml"}

========== Agent循环 第3轮 ==========
[AI Chat] Agent decision: {"action": "get_file_content", "filepath": "pom.xml"}

[AI Chat] Agent模式完成，共执行3轮  ← 达到最大次数
========== 生成最终回答 ==========
```

**验收标准**：
- ✅ 执行 3 轮后强制结束
- ✅ 即使 AI 想继续，也会停止
- ✅ 基于已收集的数据生成回答

---

### 场景 5: Agent Mode - 数据来源标注

**目的**：验证数据来源标注清晰

**步骤**：
1. 在 AI Settings 中选择 `Agent Mode`
2. 打开 AI Chat
3. 输入问题：`对比 master 和 develop 分支的最近提交`

**预期结果**：

查看后台日志中的 `collectedData`：

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 数据来源: get_commits (第1轮)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[master 分支的 commits...]

━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 数据来源: get_commits (第2轮)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[develop 分支的 commits...]
```

**验收标准**：
- ✅ 每个数据块都有清晰的分隔符
- ✅ 包含 API 名称
- ✅ 包含轮次信息
- ✅ 格式统一，易于识别

---

### 场景 6: Agent Mode - 错误处理

**目的**：验证 API 调用失败时的处理

**步骤**：
1. 在 AI Settings 中选择 `Agent Mode`
2. 打开 AI Chat
3. 输入一个会导致 API 失败的问题（如：查询不存在的文件）

**预期结果**：
```
后台日志：
========== Agent循环 第1轮 ==========
[AI Chat] Agent decision: {"action": "get_file_content", "filepath": "not-exist.txt"}
[AI Chat] 第1轮 API 返回空数据

收集数据：
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
⚠️ API 调用失败 (第1轮)
API: get_file_content
━━━━━━━━━━━━━━━━━━━━━━━━━━━━

========== Agent循环 第2轮 ==========
[AI Chat] Agent decision: {"action": "FINISH"}  ← AI 根据失败信息做出决策
```

**验收标准**：
- ✅ API 失败时记录错误
- ✅ 继续执行下一轮循环
- ✅ AI 能根据失败信息调整策略
- ✅ 最终生成合理的回答

---

### 场景 7: Agent Mode - 无效决策处理

**目的**：验证 AI 返回无效决策时的处理

**步骤**：
1. 在 AI Settings 中选择 `Agent Mode`
2. 打开 AI Chat
3. 输入一个可能导致 AI 返回无效 JSON 的问题

**预期结果**：
```
后台日志：
========== Agent循环 第1轮 ==========
[AI Chat] Agent decision: 这是一段无效的 JSON
[AI Chat] Invalid API instruction, ending loop

UI 显示：
⚠️ AI 返回无效决策，提前结束循环
```

**验收标准**：
- ✅ 检测到无效决策
- ✅ 显示警告提示
- ✅ 提前结束循环
- ✅ 基于已有数据生成回答

---

### 场景 8: 上下文大小限制

**目的**：验证上下文大小管理

**步骤**：
1. 在 AI Settings 中选择 `Agent Mode`，Max Iterations = 10
2. 打开 AI Chat
3. 输入一个会产生大量数据的问题

**预期结果**：
```
后台日志：
[AI Chat] 已收集数据总长度: 95000 chars
[AI Chat] 已收集数据总长度: 105000 chars
[AI Chat] 上下文过大，截断早期数据
[AI Chat] 已收集数据总长度: 85000 chars  ← 截断后
```

**验收标准**：
- ✅ 检测到上下文过大（> 100KB）
- ✅ 自动截断早期数据
- ✅ 保留最近 80KB 数据
- ✅ 添加省略提示

---

## 关键检查点

### 1. 上下文传递检查

在每轮循环的日志中，检查 `askAIForNextAction()` 的输入：

```
【当前状态】
- 用户问题：XXX
- 当前轮次：2/5
- 当前项目：XXX

【已收集的数据】  ← 必须包含第1轮的数据
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 数据来源: get_commits (第1轮)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[数据...]

【可用的 API】  ← 必须包含完整的 API 列表
1. get_repo
2. get_commits
...
```

### 2. 数据来源标注检查

在 `collectedData` 中，检查每个数据块：

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━  ← 分隔符
📊 数据来源: get_commits (第1轮)  ← API 名称 + 轮次
━━━━━━━━━━━━━━━━━━━━━━━━━━━━  ← 分隔符
[数据内容...]
```

### 3. AI 决策检查

在每轮循环中，检查 AI 的决策：

```
有效决策：
{"action": "get_commits"}
{"action": "get_file_content", "filepath": "pom.xml"}
{"action": "FINISH", "reason": "信息足够"}

无效决策：
这是一段文字，不是 JSON
{"action": "unknown_api"}
```

### 4. 循环次数检查

```
设置：Max Iterations = 5

情况1：AI 主动 FINISH
  第1轮 → 第2轮 → 第3轮 → FINISH
  实际执行：3 轮 ✅

情况2：达到最大次数
  第1轮 → 第2轮 → 第3轮 → 第4轮 → 第5轮 → 强制结束
  实际执行：5 轮 ✅
```

## 性能测试

### 测试 1: Simple Mode 性能

**步骤**：
1. 选择 Simple Mode
2. 输入 10 个简单问题
3. 记录每个问题的耗时

**预期**：
- 平均耗时：3-6 秒
- AI 调用次数：2 次/问题

### 测试 2: Agent Mode 性能

**步骤**：
1. 选择 Agent Mode，Max Iterations = 5
2. 输入 5 个复杂问题
3. 记录每个问题的耗时和循环次数

**预期**：
- 平均耗时：15-25 秒
- 平均循环次数：3-4 轮
- AI 调用次数：4-5 次/问题

## 常见问题排查

### 问题 1: Agent 循环没有执行

**症状**：选择了 Agent Mode，但日志显示 Simple Mode

**排查**：
1. 检查 AI Settings 中的 Chat Mode 是否保存
2. 检查配置文件：`~/.gitviewer/settings.properties`
   ```properties
   ai.chat.mode=agent  ← 应该是 agent
   ```
3. 重启应用

### 问题 2: 上下文没有传递

**症状**：第 2 轮循环的日志中看不到第 1 轮的数据

**排查**：
1. 检查 `collectedData` 是否正确累积
2. 检查 `askAIForNextAction()` 的输入参数
3. 查看日志中的 `Context length`，应该逐轮增加

### 问题 3: 数据来源标注缺失

**症状**：收集的数据没有来源标注

**排查**：
1. 检查 `processAgentMode()` 中的数据收集代码
2. 确认使用了正确的格式：
   ```java
   collectedData.append("\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
   collectedData.append("📊 数据来源: ").append(apiName)...
   ```

### 问题 4: AI 一直不返回 FINISH

**症状**：Agent 循环执行到最大次数才结束

**排查**：
1. 检查 AI 模型是否理解 FINISH 指令
2. 查看 AI 的决策日志，分析原因
3. 尝试使用更高级的 AI 模型（如 GPT-4）

### 问题 5: 循环次数不正确

**症状**：设置了 Max Iterations = 5，但只执行了 3 轮

**排查**：
1. 检查是否 AI 主动返回了 FINISH
2. 检查是否有 API 调用失败导致提前结束
3. 查看日志中的 `Agent decided to FINISH` 消息

## 测试报告模板

```markdown
# AI Chat Agent Loop 测试报告

## 测试环境
- 应用版本：1.0.0
- Java 版本：17
- AI 模型：gpt-3.5-turbo
- 测试日期：2026-02-08

## 测试结果

### Simple Mode
- ✅ 场景 1: 基础功能测试 - 通过
- 耗时：4.2 秒
- 备注：无

### Agent Mode
- ✅ 场景 2: 简单查询 - 通过
  - 循环次数：2 轮
  - 耗时：8.5 秒
  
- ✅ 场景 3: 复杂查询 - 通过
  - 循环次数：3 轮
  - 耗时：15.3 秒
  - 上下文传递：正常
  - 数据来源标注：清晰
  
- ✅ 场景 4: 最大循环次数限制 - 通过
  - 设置：3 轮
  - 实际：3 轮
  
- ✅ 场景 5: 数据来源标注 - 通过
  - 格式：正确
  - 轮次信息：准确
  
- ✅ 场景 6: 错误处理 - 通过
  - API 失败：正常处理
  - 继续循环：正常
  
- ✅ 场景 7: 无效决策处理 - 通过
  - 检测：正常
  - 提示：清晰
  
- ✅ 场景 8: 上下文大小限制 - 通过
  - 截断：正常
  - 保留数据：80KB

## 性能测试
- Simple Mode 平均耗时：4.5 秒
- Agent Mode 平均耗时：18.2 秒
- Agent Mode 平均循环次数：3.4 轮

## 问题和建议
- 无

## 总体评价
✅ 所有测试场景通过
✅ 功能完整，性能良好
✅ 可以发布
```

## 下一步

测试通过后：
1. ✅ 更新用户文档
2. ✅ 创建发布说明
3. ✅ 通知用户新功能
4. ✅ 收集用户反馈

---

**测试负责人**：AI Assistant
**文档版本**：1.0
**最后更新**：2026-02-08
