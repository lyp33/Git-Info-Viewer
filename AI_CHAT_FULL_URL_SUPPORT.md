# AI Chat 完整 Git URL 支持

## 需求

用户希望在 AI Chat 对话框中：
1. **自动显示完整的 Git 远程 URL**（而不是简化的 owner/repo）
2. **支持手动输入完整的 Git URL**（支持 HTTPS 和 SSH 格式）

### 示例

**之前**：
- Git Path 显示：`thailife/thailife_sdk`
- 用户只能输入：`owner/repo` 或 `group/subgroup/project`

**现在**：
- Git Path 显示：`https://gitlab.insuremo.com/thailife/thailife_sdk/gemini-bff-parent.git`
- 用户可以输入：
  - 完整 HTTPS URL：`https://gitlab.insuremo.com/group/project.git`
  - 完整 SSH URL：`git@gitlab.insuremo.com:group/project.git`
  - 简化路径：`owner/repo` 或 `group/subgroup/project`

## 实现细节

### 1. 初始显示完整 URL

**位置**：`src/main/java/com/gitviewer/AIChatDialog.java` - `initializeUI()` 方法

**修改**：
```java
// 设置初始值 - 显示完整的远程 URL
if (currentRemoteUrl != null && !currentRemoteUrl.isEmpty()) {
    // 移除 "origin : " 前缀（如果有）
    String displayUrl = currentRemoteUrl;
    if (displayUrl.contains(" : ")) {
        displayUrl = displayUrl.split(" : ")[1].trim();
    }
    gitPathField.setText(displayUrl);
} else if (currentOwner != null && currentRepo != null) {
    gitPathField.setText(currentOwner + "/" + currentRepo);
}
```

**说明**：
- 优先显示完整的 `currentRemoteUrl`
- 如果没有 remote URL，才显示简化的 `owner/repo`
- 自动移除 JGit 返回的 "origin : " 前缀

### 2. 支持多种输入格式

**位置**：`src/main/java/com/gitviewer/AIChatDialog.java` - Apply 按钮的 ActionListener

**支持的格式**：

#### 格式 1：完整 HTTPS URL
```
https://gitlab.insuremo.com/thailife/thailife_sdk/gemini-bff-parent.git
https://github.com/facebook/react.git
```

#### 格式 2：完整 SSH URL
```
git@gitlab.insuremo.com:thailife/thailife_sdk/gemini-bff-parent.git
git@github.com:facebook/react.git
```

#### 格式 3：简化路径（2 级）
```
facebook/react
thailife/frontend-facade
```

#### 格式 4：简化路径（多级）
```
thailife/thailife_sdk/gemini-bff-parent
group/subgroup/project
```

### 3. URL 解析逻辑

```java
// 判断是完整 URL 还是简化路径
if (newGitPath.startsWith("https://") || newGitPath.startsWith("http://") || newGitPath.startsWith("git@")) {
    // 完整的 Git URL
    currentRemoteUrl = newGitPath;
    
    // 从 URL 提取 owner/repo
    String[] parts = extractOwnerRepoFromUrl(newGitPath);
    if (parts != null) {
        currentOwner = parts[0];
        currentRepo = parts[1];
    } else {
        // 解析失败，显示错误
        JOptionPane.showMessageDialog(this, 
            "无法解析 Git URL\n\n请确保 URL 格式正确", 
            "解析错误", 
            JOptionPane.WARNING_MESSAGE);
        return;
    }
} else {
    // 简化的路径格式
    String[] parts = newGitPath.split("/");
    if (parts.length < 2) {
        // 格式错误
        JOptionPane.showMessageDialog(this, 
            "Git 路径格式错误\n\n正确格式：\n" +
            "- 完整 URL: https://gitlab.com/group/project.git\n" +
            "- 简化路径: owner/repo 或 group/subgroup/project", 
            "输入错误", 
            JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    // 解析简化路径
    if (parts.length == 2) {
        currentOwner = parts[0];
        currentRepo = parts[1];
    } else {
        // 多级路径：将前面的部分作为 owner
        currentOwner = String.join("/", java.util.Arrays.copyOfRange(parts, 0, parts.length - 1));
        currentRepo = parts[parts.length - 1];
    }
}
```

### 4. 重新初始化 Git API 客户端

```java
// 重新初始化 Git API 客户端
if (currentRemoteUrl != null && !currentRemoteUrl.isEmpty()) {
    gitApiClient = new GitApiClient(currentRemoteUrl, gitToken);
}
```

**说明**：
- 当用户输入完整 URL 时，使用新的 URL 重新初始化 `GitApiClient`
- `GitApiClient` 会自动检测是 GitLab 还是 GitHub
- 自动提取 API base URL

### 5. 更新提示信息

**Tooltip 更新**：
```java
gitPathField.setToolTipText("输入完整的 Git 远程 URL，例如：https://gitlab.com/group/project.git 或 git@gitlab.com:group/project.git");
```

**成功消息更新**：
```java
String displayPath = (currentRemoteUrl != null && !currentRemoteUrl.isEmpty()) 
    ? currentRemoteUrl 
    : currentOwner + "/" + currentRepo;
appendSystemMessage("✓ 已更新项目上下文: " + displayPath + " (分支: " + currentBranch + ")");
```

## 使用场景

### 场景 1：从目录树选择项目

1. 用户在左侧目录树选择 `gemini-bff-parent` 项目
2. 打开 AI Chat 对话框
3. Git Path 自动显示：`https://gitlab.insuremo.com/thailife/thailife_sdk/gemini-bff-parent.git`
4. 用户可以直接使用，无需修改

### 场景 2：手动输入完整 HTTPS URL

1. 用户清空 Git Path 输入框
2. 输入：`https://github.com/facebook/react.git`
3. 点击 Apply
4. 系统自动：
   - 设置 `currentRemoteUrl = "https://github.com/facebook/react.git"`
   - 解析出 `currentOwner = "facebook"`, `currentRepo = "react"`
   - 初始化 GitHub API 客户端

### 场景 3：手动输入完整 SSH URL

1. 用户输入：`git@gitlab.insuremo.com:thailife/frontend-facade.git`
2. 点击 Apply
3. 系统自动：
   - 设置 `currentRemoteUrl = "git@gitlab.insuremo.com:thailife/frontend-facade.git"`
   - 解析出 `currentOwner = "thailife"`, `currentRepo = "frontend-facade"`
   - 初始化 GitLab API 客户端

### 场景 4：手动输入简化路径

1. 用户输入：`facebook/react`
2. 点击 Apply
3. 系统自动：
   - 设置 `currentOwner = "facebook"`, `currentRepo = "react"`
   - 保持原有的 `currentRemoteUrl`（不更新）
   - 使用原有的 API 客户端

## 错误处理

### 错误 1：空输入
```
用户输入：（空）
错误提示：请输入 Git 远程 URL 或项目路径
```

### 错误 2：无法解析的 URL
```
用户输入：https://invalid-url
错误提示：无法解析 Git URL
         请确保 URL 格式正确
```

### 错误 3：简化路径格式错误
```
用户输入：invalid
错误提示：Git 路径格式错误
         
         正确格式：
         - 完整 URL: https://gitlab.com/group/project.git
         - 简化路径: owner/repo 或 group/subgroup/project
```

## 日志输出

```
[AI Chat] Manual context updated:
[AI Chat]   Remote URL: https://gitlab.insuremo.com/thailife/thailife_sdk/gemini-bff-parent.git
[AI Chat]   Owner: thailife/thailife_sdk
[AI Chat]   Repo: gemini-bff-parent
[AI Chat]   Branch: 24.08_thailife_dev
```

## 兼容性

### 向后兼容
- ✅ 仍然支持简化路径输入（`owner/repo`）
- ✅ 如果没有 remote URL，显示简化路径
- ✅ 现有的 API 调用逻辑不受影响

### 新功能
- ✅ 支持完整 HTTPS URL
- ✅ 支持完整 SSH URL
- ✅ 自动显示完整 URL（如果可用）
- ✅ 自动重新初始化 API 客户端

## 测试建议

### 测试 1：自动显示完整 URL
1. 选择一个有 remote URL 的项目
2. 打开 AI Chat
3. 验证 Git Path 显示完整 URL

### 测试 2：输入 HTTPS URL
1. 输入：`https://github.com/facebook/react.git`
2. 点击 Apply
3. 验证成功消息显示完整 URL
4. 测试 API 调用（如查询 repo 信息）

### 测试 3：输入 SSH URL
1. 输入：`git@gitlab.insuremo.com:group/project.git`
2. 点击 Apply
3. 验证成功消息
4. 测试 API 调用

### 测试 4：输入简化路径
1. 输入：`facebook/react`
2. 点击 Apply
3. 验证成功消息
4. 测试 API 调用

### 测试 5：错误处理
1. 输入空字符串 → 验证错误提示
2. 输入无效 URL → 验证错误提示
3. 输入单个单词 → 验证错误提示

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

本次更新实现了：

1. ✅ **自动显示完整 Git URL** - 从 `currentRemoteUrl` 获取
2. ✅ **支持多种输入格式** - HTTPS、SSH、简化路径
3. ✅ **智能解析** - 自动识别 URL 类型并提取 owner/repo
4. ✅ **自动初始化 API 客户端** - 根据 URL 重新初始化
5. ✅ **友好的错误提示** - 明确告诉用户正确格式
6. ✅ **向后兼容** - 仍然支持简化路径输入

用户现在可以：
- 看到完整的 Git 远程 URL
- 手动输入任意格式的 Git URL
- 快速切换不同的 Git 项目

## 相关文档

- AI_CHAT_MANUAL_INPUT_FEATURE_COMPLETE.md - 手动输入功能实现
- AI_CHAT_PROMPT_ENHANCEMENT_V2.md - 提示词强化 V2

