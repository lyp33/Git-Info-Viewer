# AI Chat Agent 提示词优化 - 分步思考引导

## 优化时间
2026-02-08

## 问题描述

用户反馈：当问"pom文件最后一些修改内容是什么"时，AI 应该分步思考：
- **第1步**：不知道文件路径 → `search_files` 查找文件
- **第2步**：找到路径后 → `get_file_commits` 获取提交历史
- **第3步**：信息足够 → `FINISH`

**当前问题**：AI 可能直接跳到第2步，假设文件路径，导致 API 调用失败。

## 解决方案

### 1. 添加【分步思考】指南

在 `askAIForNextAction()` 方法的提示词中添加：

```
【分步思考】
请按照以下步骤分析用户问题：
1. 理解用户问题的核心需求
2. 分解问题：需要哪些信息才能回答？
3. 检查已收集的数据：缺少哪些关键信息？
4. 确定下一步：需要调用哪个 API 来获取缺失的信息？
```

### 2. 添加【常见问题的分步策略】

提供具体的示例场景：

```
【常见问题的分步策略】
• 询问"某个文件的XXX"：
  步骤1：如果不知道文件路径 → search_files 查找文件
  步骤2：找到路径后 → 根据需求调用 get_file_commits 或 get_file_content

• 询问"最近修改了XXX的commit"：
  步骤1：如果不知道文件路径 → search_files 查找文件
  步骤2：找到路径后 → get_file_commits 获取提交历史

• 询问"对比两个分支"：
  步骤1：get_commits (branch: master)
  步骤2：get_commits (branch: develop)
  步骤3：FINISH（已有足够数据对比）
```

### 3. 添加【决策规则】

明确优先级和决策逻辑：

```
【决策规则】
1. 如果用户提到文件名但没有完整路径，优先使用 search_files 确认文件是否存在
2. 如果已有文件路径，可以直接调用 get_file_commits 或 get_file_content
3. 如果已收集的数据足够回答问题，返回 FINISH
4. 每次只执行一个最关键的步骤，不要跳步
```

### 4. 强制要求 reason 字段

在返回格式中强制要求 AI 解释决策：

```
如果需要更多数据（必须包含 reason 说明为什么需要这个 API）：
{"action": "search_files", "filename": "pom.xml", "reason": "需要先确认 pom 文件的完整路径"}
{"action": "get_file_commits", "filepath": "pom.xml", "reason": "需要查看文件的提交历史"}
{"action": "get_file_content", "filepath": "src/App.java", "reason": "需要查看文件内容"}
```

## 代码修改

### 文件：`src/main/java/com/gitviewer/AIChatDialog.java`

修改了 `askAIForNextAction()` 方法（第 1345-1465 行），在提示词中添加了：
1. 【分步思考】指南
2. 【常见问题的分步策略】示例
3. 【决策规则】明确优先级
4. 强制要求 `reason` 字段

## 预期效果

### 测试场景："pom文件最后一些修改内容是什么"

**优化前**：
- AI 可能直接调用 `get_file_commits(filepath: "pom.xml")`
- 如果文件不在根目录，API 调用失败

**优化后**：
- 第1轮：`search_files(filename: "pom.xml", reason: "需要先确认 pom 文件的完整路径")`
- 第2轮：`get_file_commits(filepath: "找到的路径", reason: "需要查看文件的提交历史")`
- 第3轮：`FINISH(reason: "已收集足够信息")`

## 编译和打包

```bash
# 编译成功
mvn compile

# 打包成功
mvn package -DskipTests
```

生成文件：`target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 测试建议

1. **基础测试**：问"pom文件最后一些修改内容是什么"
   - 验证 AI 是否先调用 `search_files`
   - 验证 AI 是否在找到路径后调用 `get_file_commits`
   - 验证 AI 是否正确返回 `FINISH`

2. **复杂测试**：问"对比 master 和 dev 分支的最新提交"
   - 验证 AI 是否分两步获取两个分支的提交
   - 验证 AI 是否在收集足够数据后返回 `FINISH`

3. **边界测试**：问"src/main/java/App.java 文件的内容是什么"
   - 验证 AI 是否直接调用 `get_file_content`（因为已有完整路径）
   - 验证 AI 是否跳过 `search_files` 步骤

## 关键改进点

1. **明确分步策略**：AI 现在知道应该先搜索文件，再获取详细信息
2. **强制解释决策**：`reason` 字段让 AI 必须思考为什么需要这个 API
3. **提供具体示例**：常见问题的分步策略帮助 AI 理解正确的流程
4. **防止跳步**：决策规则明确指出"每次只执行一个最关键的步骤"

## 下一步

建议用户测试以下场景：
1. 询问文件的修改历史（验证分步思考）
2. 询问文件的内容（验证直接调用）
3. 对比不同分支（验证多步骤收集）

如果发现 AI 仍然跳步或假设路径，可以进一步优化提示词，增加更多约束条件。
