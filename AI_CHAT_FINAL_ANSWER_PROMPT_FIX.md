# AI Chat 最终回答提示词修复

## 问题描述

用户询问文件内容时，虽然后台成功调用了 `get_file_content` API 并获取了数据，但 AI 的最终回答却说"我目前无法直接访问或读取文件内容"。

### 问题现象

从日志可以看出：
1. ✅ **第一阶段成功**：AI 正确识别需要调用 `get_file_content` API
2. ✅ **API 调用成功**：返回了 216 字符的文件内容
3. ❌ **第二阶段失败**：AI 没有意识到已经获取了数据，仍然说无法访问

### 日志示例

```
[AI Service] API URL: https://portal.insuremo.com/api/more/ai-qa/service/aiqa/api/v1/chat
[AI Service] Request body: {"query":"envs/common/.basic 文件的内容", ...}
[AI Service] Response Code: 200
[AI Service] Response body length: 216 chars

AI Chat] Final answer (raw): 看起来你想查看 thailife/frontend-facade 项目中 envs/common/.basic 文件的内容。
不过，我目前无法直接访问或读取文件内容。你可以通过以下方式查看：...
```

## 根本原因

在 `askAIForFinalAnswer()` 方法中，系统提示词不够强调 AI 应该直接使用已获取的数据，导致 AI 的训练习惯（不能访问外部资源）覆盖了实际情况。

### 原提示词问题

```java
systemPrompt.append("你是一个友好的 GitHub 助手。\n");
systemPrompt.append("请用中文友好地回答用户的问题。\n");

messages.add(new AIService.ChatMessage("system", 
    "以下是从 GitHub API 获取的数据，请基于这些数据回答用户的问题：\n\n" + githubData));
```

**问题**：
1. 提示词太弱，没有明确告诉 AI"你已经有数据了"
2. 没有禁止 AI 说"无法访问"之类的话
3. 没有强调"直接分析数据"

## 解决方案

### 改进的提示词

```java
// 如果有 Git 数据，强调 AI 应该直接使用这些数据
if (githubData != null && !githubData.isEmpty()) {
    systemPrompt.append("\n重要提示：\n");
    systemPrompt.append("- 我已经为你获取了相关数据，请直接分析并回答\n");
    systemPrompt.append("- 不要说你无法访问或读取文件\n");
    systemPrompt.append("- 不要建议用户使用其他工具或命令\n");
    systemPrompt.append("- 直接基于提供的数据给出答案\n");
}

messages.add(new AIService.ChatMessage("system", 
    "以下是从 " + platformName + " API 获取的数据，请直接分析这些数据并回答用户的问题：\n\n" + githubData));
```

### 改进点

1. **明确告知**："我已经为你获取了相关数据"
2. **禁止否定**："不要说你无法访问或读取文件"
3. **禁止建议**："不要建议用户使用其他工具或命令"
4. **强调行动**："直接基于提供的数据给出答案"
5. **平台适配**：使用 `platformName`（GitLab/GitHub）而不是硬编码

## 实现细节

### 修改位置
文件：`src/main/java/com/gitviewer/AIChatDialog.java`  
方法：`askAIForFinalAnswer(String userQuestion, String githubData)`

### 完整实现

```java
private String askAIForFinalAnswer(String userQuestion, String githubData) {
    try {
        List<AIService.ChatMessage> messages = new ArrayList<>();
        
        String platformName = (gitApiClient != null) ? gitApiClient.getPlatformName() : "Git";
        
        // 系统提示
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是一个友好的 ").append(platformName).append(" 助手。\n");
        if (currentOwner != null && currentRepo != null) {
            systemPrompt.append("当前项目：").append(currentOwner).append("/").append(currentRepo).append("\n");
        }
        systemPrompt.append("请用中文友好地回答用户的问题。\n");
        
        // 如果有 Git 数据，强调 AI 应该直接使用这些数据
        if (githubData != null && !githubData.isEmpty()) {
            systemPrompt.append("\n重要提示：\n");
            systemPrompt.append("- 我已经为你获取了相关数据，请直接分析并回答\n");
            systemPrompt.append("- 不要说你无法访问或读取文件\n");
            systemPrompt.append("- 不要建议用户使用其他工具或命令\n");
            systemPrompt.append("- 直接基于提供的数据给出答案\n");
        }
        
        messages.add(new AIService.ChatMessage("system", systemPrompt.toString()));
        messages.add(new AIService.ChatMessage("user", userQuestion));
        
        // 如果有 Git 数据，添加到上下文
        if (githubData != null && !githubData.isEmpty()) {
            messages.add(new AIService.ChatMessage("system", 
                "以下是从 " + platformName + " API 获取的数据，请直接分析这些数据并回答用户的问题：\n\n" + githubData));
        }
        
        String response = aiService.chat(messages);
        return response;
        
    } catch (Exception e) {
        return "抱歉，生成回答时出错：" + e.getMessage();
    }
}
```

## 预期效果

### 修复前
```
用户：envs/common/.basic 文件的内容
AI：看起来你想查看 thailife/frontend-facade 项目中 envs/common/.basic 文件的内容。
    不过，我目前无法直接访问或读取文件内容。你可以通过以下方式查看：
    1. 使用 git 命令...
    2. 访问 GitLab 网页...
```

### 修复后
```
用户：envs/common/.basic 文件的内容
AI：这是 thailife/frontend-facade 项目中 envs/common/.basic 文件的内容：
    
    [文件内容]
    SERVICE_NAME=xxx
    VERSION=1.0.0
    ...
    
    这个文件定义了环境变量配置，包括服务名称、版本号等信息。
```

## 测试建议

### 1. 基本功能测试
- [ ] 询问文件内容 → AI 应该直接显示内容
- [ ] 询问文件功能 → AI 应该分析内容并解释
- [ ] 询问配置项 → AI 应该从内容中提取信息

### 2. 各种文件类型
- [ ] 配置文件（.basic, .env, .properties）
- [ ] 代码文件（.java, .js, .py）
- [ ] 文档文件（.md, .txt）

### 3. 不同平台
- [ ] GitLab 项目
- [ ] GitHub 项目

### 4. 边界情况
- [ ] 空文件
- [ ] 大文件（接近 50KB 限制）
- [ ] 不存在的文件（应该返回 404 错误）

## 相关改进

### 1. 日志优化
将所有 "GitHub" 改为动态的 `platformName`：
- `[AI Chat] Has Git data` 而不是 `Has GitHub data`
- `Git data preview` 而不是 `GitHub data preview`

### 2. 一致性
确保整个对话流程中，平台名称使用一致（GitLab/GitHub）

## 编译和部署

```bash
# 编译
mvn compile

# 打包
mvn package -DskipTests

# 运行
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

**编译状态**：✅ 成功  
**打包状态**：✅ 成功

## 总结

这个修复通过**强化提示词**解决了 AI 不使用已获取数据的问题。关键改进：

1. ✅ **明确告知**：AI 知道数据已经获取
2. ✅ **禁止否定**：AI 不会说"无法访问"
3. ✅ **强调行动**：AI 会直接分析数据
4. ✅ **平台适配**：支持 GitLab 和 GitHub

现在 AI Chat 应该能够正确地分析和展示文件内容了！

## 相关文档

- AI_CHAT_GET_FILE_CONTENT_COMPLETE.md - 文件内容功能实现
- AI_CHAT_GET_FILE_CONTENT_FIXES_APPLIED.md - 代码修复记录
- AI_CHAT_MANUAL_INPUT_FEATURE_COMPLETE.md - 手动输入功能
