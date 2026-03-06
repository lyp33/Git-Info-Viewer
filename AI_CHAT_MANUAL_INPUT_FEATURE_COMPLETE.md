# AI Chat 手动输入 Git 路径和分支功能完成

## 功能概述

添加了手动输入 Git 项目路径和选择分支的功能，让用户可以查询任意 Git 项目，而不仅限于左侧目录树中的项目。

## 实现日期
2026-02-08

## 功能特性

### 1. Git Path 手动输入
- **输入框**：用户可以手动输入 Git 项目路径
- **格式支持**：
  - 简单格式：`owner/repo`（例如：`facebook/react`）
  - 多级格式：`group/subgroup/project`（例如：`thailife/frontend-facade`）
- **自动填充**：如果从左侧目录树选择了项目，会自动填充当前项目路径
- **Tooltip 提示**：输入框有提示信息，说明正确的格式

### 2. Branch 选择/输入
- **下拉选择**：可以从下拉列表中选择分支
- **手动输入**：支持手动输入分支名称
- **实时筛选**：输入时自动筛选匹配的分支
- **默认分支**：如果没有分支列表，提供常用分支（main, master, dev, develop）
- **自动填充**：如果从左侧目录树选择了项目，会自动填充当前分支

### 3. Apply 按钮
- **应用设置**：点击 Apply 按钮应用新的 Git 路径和分支
- **参数验证**：
  - 检查 Git 路径是否为空
  - 检查 Git 路径格式是否正确（至少包含 owner/repo）
- **错误提示**：如果输入错误，显示友好的错误对话框
- **成功反馈**：应用成功后，在聊天区域显示确认消息

### 4. 上下文更新
- **自动更新**：应用新设置后，自动更新 AI 的上下文信息
- **系统消息更新**：更新聊天历史中的系统消息，包含新的项目信息
- **日志输出**：在控制台输出详细的上下文更新日志

## UI 布局

```
┌─────────────────────────────────────────────────────────────┐
│ AI Chat - Git Assistant                                  [X]│
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────────────────────────────────────────────────┐ │
│ │ Git Path:  [thailife/frontend-facade              ]    │ │
│ │ Branch:    [dev                                    ▼]   │ │
│ │ [Apply]  💡 提示：可以手动输入任意 GitLab 项目路径      │ │
│ └─────────────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  [聊天内容区域]                                              │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│ [输入框                                          ] [Send]   │
└─────────────────────────────────────────────────────────────┘
```

## 技术实现

### 1. UI 组件
- **GridBagLayout**：使用 GridBagLayout 实现灵活的表单布局
- **JTextField**：Git 路径输入框
- **JComboBox**：可编辑的分支下拉框
- **JButton**：Apply 按钮

### 2. 事件处理
```java
// Apply 按钮点击事件
applyButton.addActionListener(e -> {
    // 1. 获取输入值
    String newGitPath = gitPathField.getText().trim();
    String newBranch = (String) branchComboBox.getSelectedItem();
    
    // 2. 验证输入
    if (newGitPath.isEmpty()) { /* 显示错误 */ }
    if (parts.length < 2) { /* 显示错误 */ }
    
    // 3. 解析路径
    // 支持 owner/repo 和 group/subgroup/project 格式
    
    // 4. 更新上下文
    currentOwner = ...;
    currentRepo = ...;
    currentBranch = ...;
    
    // 5. 更新系统消息
    updateSystemMessage();
    
    // 6. 显示成功消息
    appendSystemMessage("✓ 已更新项目上下文...");
});
```

### 3. 路径解析逻辑
```java
String[] parts = newGitPath.split("/");

if (parts.length == 2) {
    // 简单格式：owner/repo
    currentOwner = parts[0];
    currentRepo = parts[1];
} else {
    // 多级格式：group/subgroup/project
    // 将前面的部分作为 owner，最后一部分作为 repo
    currentOwner = String.join("/", Arrays.copyOfRange(parts, 0, parts.length - 1));
    currentRepo = parts[parts.length - 1];
}
```

### 4. 系统消息更新
```java
private void updateSystemMessage() {
    if (!chatHistory.isEmpty() && chatHistory.get(0).role.equals("system")) {
        String contextInfo = "当前项目：" + currentOwner + "/" + currentRepo + "。";
        String systemPrompt = contextInfo + "你是一个友好的 GitHub 助手，请用中文回答用户的问题。";
        chatHistory.set(0, new AIService.ChatMessage("system", systemPrompt));
    }
}
```

## 使用场景

### 场景 1：查询其他项目
用户想查询一个不在本地的 Git 项目：
1. 在 Git Path 输入框输入：`facebook/react`
2. 在 Branch 下拉框选择或输入：`main`
3. 点击 Apply 按钮
4. 开始询问关于 facebook/react 项目的问题

### 场景 2：切换分支
用户想查询当前项目的不同分支：
1. Git Path 保持不变（已自动填充）
2. 在 Branch 下拉框选择：`dev`
3. 点击 Apply 按钮
4. 询问关于 dev 分支的问题

### 场景 3：GitLab 多级项目
用户想查询 GitLab 的多级项目：
1. 在 Git Path 输入框输入：`thailife/frontend/facade`
2. 在 Branch 下拉框输入：`feature/new-ui`
3. 点击 Apply 按钮
4. 开始询问关于该项目的问题

## 用户体验改进

### 1. 自动填充
- 从左侧目录树选择项目时，自动填充 Git Path 和 Branch
- 减少手动输入，提高效率

### 2. 实时筛选
- 在 Branch 下拉框输入时，自动筛选匹配的分支
- 快速找到目标分支

### 3. 错误提示
- 输入格式错误时，显示清晰的错误提示
- 告诉用户正确的格式

### 4. 成功反馈
- 应用成功后，在聊天区域显示确认消息
- 让用户知道设置已生效

### 5. Tooltip 提示
- 输入框和按钮都有 Tooltip 提示
- 帮助用户理解功能

## 兼容性

### 支持的平台
- ✅ GitHub
- ✅ GitLab
- ✅ 其他 Git 平台（只要有 API 支持）

### 支持的路径格式
- ✅ `owner/repo`（GitHub 标准格式）
- ✅ `group/project`（GitLab 简单格式）
- ✅ `group/subgroup/project`（GitLab 多级格式）
- ✅ `group/subgroup/subgroup2/project`（GitLab 深层嵌套）

## 测试建议

### 1. 基本功能测试
- [ ] 输入简单格式的 Git 路径（owner/repo）
- [ ] 输入多级格式的 Git 路径（group/subgroup/project）
- [ ] 选择分支
- [ ] 手动输入分支名称
- [ ] 点击 Apply 按钮

### 2. 验证测试
- [ ] 输入空的 Git 路径 → 应该显示错误
- [ ] 输入错误格式的 Git 路径（只有一个部分）→ 应该显示错误
- [ ] 输入不存在的项目 → API 调用时会返回 404

### 3. 集成测试
- [ ] 应用新设置后，询问项目信息 → AI 应该使用新的上下文
- [ ] 应用新设置后，查询文件内容 → 应该从新项目获取
- [ ] 切换分支后，查询文件内容 → 应该从新分支获取

### 4. UI 测试
- [ ] 从左侧目录树选择项目 → Git Path 和 Branch 应该自动填充
- [ ] 在 Branch 下拉框输入 → 应该实时筛选分支
- [ ] 点击 Apply 按钮 → 应该显示成功消息

## 已知限制

1. **Remote URL 依赖**：
   - 如果没有 remote URL，无法重新初始化 Git API 客户端
   - 但仍然可以更新 owner/repo/branch 信息

2. **分支列表**：
   - 如果没有从本地 Git 仓库获取分支列表，只显示常用分支
   - 用户仍然可以手动输入任何分支名称

3. **API 验证**：
   - 不会在 Apply 时验证项目是否存在
   - 只有在实际调用 API 时才会发现项目不存在

## 后续优化建议

1. **实时验证**：
   - 在 Apply 时调用 API 验证项目是否存在
   - 提前发现错误，提供更好的用户体验

2. **历史记录**：
   - 保存最近使用的 Git 路径
   - 提供快速切换功能

3. **自动补全**：
   - 在输入 Git 路径时，提供自动补全建议
   - 基于 GitLab/GitHub API 搜索项目

4. **分支刷新**：
   - 添加刷新按钮，重新获取分支列表
   - 支持查询远程分支

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

## 相关文档

- AI_CHAT_GET_FILE_CONTENT_COMPLETE.md - 文件内容获取功能
- AI_CHAT_GET_FILE_CONTENT_FIXES_APPLIED.md - 代码修复记录
- AI_CHAT_CONTEXT_OVERFLOW_FIX.md - 数据大小限制修复

## 总结

这个功能极大地提升了 AI Chat 的灵活性，用户现在可以：
- ✅ 查询任意 Git 项目，不受本地目录限制
- ✅ 快速切换项目和分支
- ✅ 支持 GitLab 的多级项目路径
- ✅ 享受自动填充和实时筛选的便利

用户体验得到了显著改善，AI Chat 变得更加强大和易用！
