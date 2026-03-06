# AI Chat 提示词强化 V2

## 问题确认

从日志分析确认：

### ✅ GitLab API 调用成功
```
[GitLab API] GET Request: https://gitlab.insuremo.com/api/v4/projects/thailife%2Ffrontend-facade/repository/files/envs%2Fcommon%2F.basic/raw?ref=dev
[GitLab API] Response Code: 200
[GitLab API] Response body length: 1290 chars
[AI Chat] Response preview: # environment# default true, disable all env automatic guess=CFG_APP_ENV_STRICT=false...
```

**结论**：GitLab API 成功返回了 1290 字符的文件内容，内容是真实的配置文件。

### ❌ AI 仍然说"无法读取"

虽然文件内容已经获取，但 AI 的回答仍然是：
> "看起来你想查看 thailife/frontend-facade 项目中 envs/common/.basic 文件的内容。不过，我目前无法直接访问或读取文件内容..."

**结论**：问题不在 API 调用，而在于 AI 的提示词不够强。

## 解决方案：强化提示词 V2

### 改进策略

1. **使用更强的语气**：从"建议"改为"严格禁止"
2. **明确角色定位**：告诉 AI 它"可以"读取文件
3. **列出具体禁止项**：明确列出不能说的话
4. **强调数据所有权**：告诉 AI"你现在拥有这些数据"

### 实现代码

#### 修改 1：系统提示词强化

**位置**：`src/main/java/com/gitviewer/AIChatDialog.java` - `askAIForFinalAnswer()` 方法

**修改前**：
```java
if (githubData != null && !githubData.isEmpty()) {
    systemPrompt.append("\n重要提示：\n");
    systemPrompt.append("- 我已经为你获取了相关数据，请直接分析并回答\n");
    systemPrompt.append("- 不要说你无法访问或读取文件\n");
    systemPrompt.append("- 不要建议用户使用其他工具或命令\n");
    systemPrompt.append("- 直接基于提供的数据给出答案\n");
}
```

**修改后**：
```java
if (githubData != null && !githubData.isEmpty()) {
    systemPrompt.append("\n【重要】你的能力和任务：\n");
    systemPrompt.append("1. 你可以直接读取和分析文件内容\n");
    systemPrompt.append("2. 我已经为你获取了完整的文件内容\n");
    systemPrompt.append("3. 你必须直接展示和分析这些内容\n");
    systemPrompt.append("\n【严格禁止】：\n");
    systemPrompt.append("- 禁止说\"无法访问\"、\"无法读取\"、\"不能直接查看\"\n");
    systemPrompt.append("- 禁止建议用户使用 git 命令、curl 命令或访问网页\n");
    systemPrompt.append("- 禁止说需要其他工具或权限\n");
    systemPrompt.append("\n【你应该做的】：\n");
    systemPrompt.append("- 直接展示文件内容\n");
    systemPrompt.append("- 分析文件的作用和配置项\n");
    systemPrompt.append("- 回答用户关于文件的任何问题\n");
}
```

**改进点**：
- ✅ 使用【重要】【严格禁止】等强调标记
- ✅ 明确告诉 AI"你可以"读取文件
- ✅ 使用"必须"而不是"请"
- ✅ 列出具体禁止的话术
- ✅ 明确列出应该做的事情

#### 修改 2：数据传递方式强化

**修改前**：
```java
messages.add(new AIService.ChatMessage("system", 
    "以下是从 " + platformName + " API 获取的数据，请直接分析这些数据并回答用户的问题：\n\n" + githubData));
```

**修改后**：
```java
messages.add(new AIService.ChatMessage("system", 
    "【文件内容】以下是我从 " + platformName + " 获取的完整文件内容，你现在拥有这些数据，请直接展示和分析：\n\n" + githubData));
```

**改进点**：
- ✅ 添加【文件内容】标记，让 AI 明确知道这是文件内容
- ✅ 强调"你现在拥有这些数据"
- ✅ 使用"展示和分析"而不是"分析并回答"

## 预期效果

### 修复前
```
用户：envs/common/.basic 文件的内容
AI：看起来你想查看 thailife/frontend-facade 项目中 envs/common/.basic 文件的内容。
    不过，我目前无法直接访问或读取文件内容。你可以通过以下方式查看：
    1. 使用 git 命令...
    2. 访问 GitLab 网页...
```

### 修复后（预期）
```
用户：envs/common/.basic 文件的内容
AI：这是 thailife/frontend-facade 项目中 envs/common/.basic 文件的内容：

    # environment# default true, disable all env automatic guess=CFG_APP_ENV_STRICT=false
    # default true, set type orm step datasource by default, see CFG_APP_DATASOURCE_DEFAULT
    #CFG_APP_ENV_REPRESS_TIPFOR# ...
    
    这个文件是环境配置文件，主要包含以下配置项：
    - CFG_APP_ENV_STRICT: 控制环境自动检测
    - CFG_APP_DATASOURCE_DEFAULT: 数据源配置
    ...
```

## 测试步骤

1. **启动应用**：
   ```bash
   java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```

2. **打开 AI Chat**：
   - 选择一个 Git 项目
   - 打开 Chat -> AI Chat

3. **测试文件内容读取**：
   ```
   问题：envs/common/.basic 文件的内容
   ```

4. **观察 AI 回答**：
   - ✅ 应该直接展示文件内容
   - ✅ 应该分析文件的作用
   - ❌ 不应该说"无法访问"
   - ❌ 不应该建议使用 git 命令

5. **查看控制台日志**：
   ```
   [GitLab API] Response Code: 200
   [GitLab API] Response body length: 1290 chars
   [AI Chat] API Response received, length: 1290 chars
   ```

## 如果仍然失败

如果 AI 仍然说"无法访问"，可以考虑以下方案：

### 方案 A：在用户消息中直接包含内容

```java
// 在 askAIForFinalAnswer 中
if (githubData != null && !githubData.isEmpty()) {
    String enhancedQuestion = userQuestion + "\n\n【文件内容如下】：\n" + githubData;
    messages.add(new AIService.ChatMessage("user", enhancedQuestion));
} else {
    messages.add(new AIService.ChatMessage("user", userQuestion));
}
```

### 方案 B：使用 assistant 角色预填充

```java
// 让 AI 认为它已经开始回答了
messages.add(new AIService.ChatMessage("user", userQuestion));
messages.add(new AIService.ChatMessage("assistant", "好的，我已经获取了文件内容，让我为你分析：\n\n"));
messages.add(new AIService.ChatMessage("system", "请继续完成回答，展示文件内容：\n" + githubData));
```

### 方案 C：更换 AI 模型

如果当前模型过于保守，可以考虑：
- 使用更新的模型版本
- 调整 temperature 参数（如果 API 支持）
- 使用不同的 AI 服务提供商

## 编译和部署

```bash
# 编译
mvn clean compile

# 打包
mvn package -DskipTests

# 运行
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

**编译状态**：✅ 成功  
**打包状态**：✅ 成功  
**JAR 文件**：`target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 总结

本次修复通过**大幅强化提示词**来解决 AI 不使用已获取数据的问题：

1. ✅ **明确能力**：告诉 AI"你可以"读取文件
2. ✅ **严格禁止**：列出具体不能说的话
3. ✅ **强调所有权**：告诉 AI"你现在拥有这些数据"
4. ✅ **明确任务**：列出应该做的事情

如果这次修复仍然无效，说明问题可能在于：
- AI 模型本身的限制
- 需要更激进的提示词策略（方案 A/B）
- 需要更换 AI 模型或服务

## 相关文档

- AI_CHAT_FILE_CONTENT_DIAGNOSIS.md - 诊断指南
- AI_CHAT_FINAL_ANSWER_PROMPT_FIX.md - 第一次提示词修复
- AI_CHAT_GET_FILE_CONTENT_COMPLETE.md - 文件内容功能实现

