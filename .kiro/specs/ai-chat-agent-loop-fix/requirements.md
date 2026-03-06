# AI Chat Agent Loop 上下文修复 - 需求文档

## 问题描述

当前 AI Chat 的 Agent 模式虽然在文档中有描述，但实际代码中**并未实现真正的 Agent 循环**。现有实现只是简单的两阶段模式：
1. 询问 AI 需要什么 API
2. 执行一次 API
3. 生成最终回答

这导致了以下问题：
- **无法处理复杂的多步查询**
- **AI 无法根据第一次结果调整策略**
- **缺少真正的推理循环**

## 用户反馈的具体问题

用户测试后发现：
1. **第一轮对话正常**（包含完整 API 列表）
2. **第二轮对话缺少 API 列表信息**（上下文丢失）
3. **API 返回结果没有说明来源**（AI 不知道数据从哪来）
4. **没有继续询问 AI 是否需要更多数据**（循环中断）

## 期望的正确流程

```
循环开始（最多 N 次）:
  1. 提供完整上下文（API 列表 + 已获取的数据）
  2. 询问 AI：下一步需要什么？
  3. AI 返回：
     - 需要调用 API X → 执行并继续循环
     - 已有足够信息 → 生成最终回答并退出
  4. 如果达到最大循环次数 → 强制生成回答
循环结束
```

## 功能需求

### FR-1: 实现真正的 Agent 循环

**描述**: 实现多轮推理循环，每轮都包含完整上下文

**验收标准**:
- [ ] 支持最多 N 次循环（从配置读取 `ai.max.iterations`）
- [ ] 每轮循环都询问 AI 下一步行动
- [ ] AI 可以返回 `FINISH` 提前结束循环
- [ ] 达到最大次数时强制结束并生成回答

### FR-2: 完整上下文传递

**描述**: 每轮循环都包含完整的 API 列表和已收集的数据

**验收标准**:
- [ ] 第一轮：包含完整 API 列表
- [ ] 第二轮及之后：包含 API 列表 + 第一轮收集的数据
- [ ] 第 N 轮：包含 API 列表 + 前 N-1 轮收集的所有数据
- [ ] 上下文格式清晰，易于 AI 理解

### FR-3: 数据来源标注

**描述**: 每次 API 调用的结果都要标注来源

**验收标准**:
- [ ] 格式：`【来自 API: get_commits】\n<数据内容>`
- [ ] 包含 API 名称和参数信息
- [ ] 多次调用同一 API 时能区分（如：第1次、第2次）

### FR-4: AI 决策机制

**描述**: AI 在每轮循环中做出明确决策

**验收标准**:
- [ ] AI 返回 JSON 格式决策
- [ ] 支持两种决策：
  - `{"action": "FINISH", "reason": "原因"}` - 结束循环
  - `{"action": "get_xxx", "参数": "值", "reason": "原因"}` - 继续调用 API
- [ ] 解析 AI 决策并执行相应操作

### FR-5: 调试日志

**描述**: 详细的循环日志，便于调试

**验收标准**:
- [ ] 每轮循环开始时输出：`========== Agent循环 第N轮 ==========`
- [ ] 输出 AI 决策内容
- [ ] 输出 API 调用结果（带来源标注）
- [ ] 循环结束时输出总轮数

## 非功能需求

### NFR-1: 性能

- 每轮循环的 AI 调用应在 3-5 秒内完成
- 总循环时间不超过 30 秒（假设最多 5 轮）

### NFR-2: 可配置性

- 最大循环次数可配置（1-10 次）
- 默认值：5 次

### NFR-3: 错误处理

- API 调用失败时，记录错误但继续循环
- AI 返回无效决策时，提示用户并结束循环
- 达到最大循环次数时，强制生成回答（即使数据不完整）

## 用户故事

### US-1: 复杂查询 - 查找修改特定文件的 commit

**作为** 开发者  
**我想要** 查找最近修改了 `pom.xml` 的 commit  
**以便** 了解依赖变更历史

**场景**:
```
用户输入："找出最近修改了 pom.xml 的 commit"

第1轮：
  AI 决策：先获取最近的 commits 列表
  执行：get_commits
  收集：最近 10 个 commit 的基本信息

第2轮：
  AI 决策：需要查看第 1 个 commit 的文件列表
  执行：get_commit_detail (commit_sha: abc123)
  收集：该 commit 的文件列表（没有 pom.xml）

第3轮：
  AI 决策：继续查看第 2 个 commit
  执行：get_commit_detail (commit_sha: def456)
  收集：该 commit 的文件列表（包含 pom.xml！）

第4轮：
  AI 决策：找到了，信息足够
  返回：{"action": "FINISH", "reason": "已找到修改 pom.xml 的 commit"}

生成回答：
  "找到了！commit def456 修改了 pom.xml 文件。
   作者：John Doe
   时间：2024-02-07
   修改内容：更新了依赖版本..."
```

### US-2: 对比查询 - 对比两个分支

**作为** 开发者  
**我想要** 对比 master 和 develop 分支的最近提交  
**以便** 了解分支差异

**场景**:
```
用户输入："对比 master 和 develop 分支最近的提交"

第1轮：
  AI 决策：先获取 master 分支的 commits
  执行：get_commits (branch: master)
  收集：master 的最近 10 个 commit

第2轮：
  AI 决策：再获取 develop 分支的 commits
  执行：get_commits (branch: develop)
  收集：develop 的最近 10 个 commit

第3轮：
  AI 决策：数据足够，可以对比了
  返回：{"action": "FINISH", "reason": "已收集两个分支的数据"}

生成回答：
  "master 和 develop 分支的对比：
   
   master 分支最新提交：
   - abc123: 修复登录 bug (2024-02-07)
   - def456: 更新依赖 (2024-02-06)
   
   develop 分支最新提交：
   - ghi789: 新增支付功能 (2024-02-08)
   - jkl012: 优化性能 (2024-02-07)
   
   develop 分支领先 master 2 个提交..."
```

## 技术约束

1. **必须兼容现有配置**: 使用 `AppSettings` 中的 `aiChatMode` 和 `aiMaxIterations`
2. **必须保持向后兼容**: Simple Mode 仍然可用
3. **必须使用现有 AI Service**: 不改变 `AIService` 接口
4. **必须使用现有 Git API Client**: 不改变 `GitApiClient` 接口

## 成功标准

1. **功能完整**: 所有功能需求都已实现
2. **测试通过**: 用户故事场景都能正确执行
3. **日志清晰**: 调试日志能清楚展示循环过程
4. **用户满意**: 用户确认问题已解决

## 优先级

- **P0 (必须)**: FR-1, FR-2, FR-3, FR-4
- **P1 (重要)**: FR-5, NFR-3
- **P2 (可选)**: NFR-1, NFR-2

## 相关文档

- `AI_CHAT_AGENT_MODE_COMPLETE.md` - Agent 模式设计文档（但未实现）
- `src/main/java/com/gitviewer/AIChatDialog.java` - 当前实现（仅 Simple Mode）
- `src/main/java/com/gitviewer/AppSettings.java` - 配置管理
- `src/main/java/com/gitviewer/AISettingsDialog.java` - AI 设置界面
