# Jenkins Stage Log 功能

## 功能概述

在 Jenkins Job Details 对话框的 Stage View 中，现在可以双击任何 stage 来查看该 stage 的构建日志。

## 实现细节

### 1. 数据模型增强 (JenkinsStage.java)
- 添加了 `id` 字段来唯一标识每个 stage
- 该 ID 用于从 Jenkins API 获取特定 stage 的日志

### 2. API 客户端增强 (JenkinsApiClient.java)
- 在 `fetchBuildStages()` 方法中添加了对 stage ID 的解析
- 新增 `fetchStageLog()` 方法，使用 Jenkins Workflow API 获取 stage 日志
  - API 端点: `{baseUrl}/{jobPath}/{buildNumber}/execution/node/{stageId}/wfapi/log`

### 3. Stage 视图面板增强 (JenkinsStageViewPanel.java)
- 添加了 `setJobInfo()` 方法来存储 API 客户端、作业路径和构建编号
- 在 `createStageBox()` 方法中添加了鼠标监听器：
  - **双击**: 打开 stage 日志对话框
  - **鼠标悬停**: 改变边框颜色和光标样式，提供视觉反馈
  - **鼠标离开**: 恢复原始样式
- 新增 `openStageLogDialog()` 方法来创建和显示日志对话框

### 4. 新建 Stage 日志对话框 (JenkinsStageLogDialog.java)
- 显示 stage 的详细信息（名称、构建编号、状态、持续时间）
- 使用黑色背景的文本区域显示日志，模拟控制台输出
- 提供刷新按钮来重新加载日志
- 使用 SwingWorker 异步加载日志，避免 UI 冻结

### 5. Job Details 对话框更新 (JenkinsJobDetailsDialog.java)
- 在 `loadStageView()` 方法中调用 `stageViewPanel.setJobInfo()` 来传递必要的信息

## 使用方法

1. 打开 Jenkins Browser 对话框
2. 导航到任意作业并打开 Job Details 对话框
3. 在 Build History 中选择一个构建
4. 在 Stage View 中双击任何 stage
5. 将打开一个新对话框显示该 stage 的构建日志

## 技术特性

- **异步加载**: 使用 SwingWorker 在后台线程加载日志，保持 UI 响应
- **视觉反馈**: 鼠标悬停时提供视觉提示，表明 stage 可以点击
- **错误处理**: 如果无法获取日志，显示友好的错误消息
- **控制台样式**: 日志以黑色背景、白色文字显示，模拟真实的控制台输出
- **刷新功能**: 可以手动刷新日志以查看最新内容

## API 依赖

该功能依赖于 Jenkins Workflow API (wfapi)，这是 Jenkins Pipeline 插件提供的标准 API。

## 文件清单

- `src/main/java/com/gitviewer/JenkinsStage.java` (修改)
- `src/main/java/com/gitviewer/JenkinsApiClient.java` (修改)
- `src/main/java/com/gitviewer/JenkinsStageViewPanel.java` (修改)
- `src/main/java/com/gitviewer/JenkinsJobDetailsDialog.java` (修改)
- `src/main/java/com/gitviewer/JenkinsStageLogDialog.java` (新建)

## 构建状态

✅ 编译成功
✅ 无诊断错误
✅ JAR 已生成: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
