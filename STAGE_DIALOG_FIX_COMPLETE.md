# Stage Dialog 修复完成

## 问题描述

双击 Stage 后没有弹出日志对话框，也没有显示任何信息。

## 问题根源

`JenkinsStageViewPanel` 被嵌入在 `JenkinsJobDetailsDialog`（Dialog）中，而不是直接在 `Frame` 中。代码尝试将 parent 强制转换为 `Frame` 类型时抛出 `ClassCastException`：

```
ERROR: Exception while getting parent frame: class com.gitviewer.JenkinsJobDetailsDialog cannot be cast to class java.awt.Frame
```

## 修复方案

### 1. 修改 JenkinsStageViewPanel.java

将 parent 类型从 `Frame` 改为 `Window`（`Window` 是 `Frame` 和 `Dialog` 的共同父类）：

```java
// 修改前
Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);

// 修改后
Window parentWindow = SwingUtilities.getWindowAncestor(this);
```

### 2. 修改 JenkinsStageLogDialog.java

修改构造函数接受 `Window` 参数而不是 `Frame`：

```java
// 修改前
public JenkinsStageLogDialog(Frame parent, JenkinsApiClient apiClient, 
                              String jobPath, int buildNumber, JenkinsStage stage) {
    super(parent, "Stage Log: " + stage.getName(), true);
    ...
}

// 修改后
public JenkinsStageLogDialog(Window parent, JenkinsApiClient apiClient, 
                              String jobPath, int buildNumber, JenkinsStage stage) {
    super(parent, "Stage Log: " + stage.getName(), Dialog.ModalityType.APPLICATION_MODAL);
    ...
}
```

### 3. 增加对话框尺寸

同时增大了两个对话框的尺寸以显示更多内容：

- **Jenkins Job Browser**: 从 800x700 增大到 **1200x900**
- **Job Details Dialog**: 从 1000x700 增大到 **1400x900**

## 功能说明

双击 Stage 后，系统会：

1. 获取 Stage Log（从主 Pipeline 的 console log 中提取该 Stage 的片段）
2. 从 Stage Log 中提取子作业路径（例如：`Manual-Build ? thailifesdk ? 24.08_thailife_dev ? CI-Robot ? BS-CI-ROBOT`）
3. 从 Stage Log 中提取 Build ID（例如：`#578`）
4. 构建子作业的 URL：
   ```
   http://jenkins/job/gemini/job/Manual-Build/job/thailifesdk/job/24.08_thailife_dev/job/CI-Robot/job/BS-CI-ROBOT/578/consoleText
   ```
5. 获取子作业的完整 Console Log 并显示在对话框中

## 调试日志

添加了详细的调试日志，包括：

- Stage ID 检查
- Parent window 获取
- 对话框创建过程
- 子作业路径提取
- URL 构建
- Console log 获取

## 测试状态

✅ 对话框能够正常弹出
✅ 显示 Stage 日志内容
⏳ 需要验证是否成功获取子作业的完整日志

## 下一步

如果对话框显示的还是 Stage 片段日志而不是子作业的完整日志，需要检查：

1. 控制台日志中是否有 `[JenkinsApiClient] fetchSubJobConsoleLog()` 相关的输出
2. 是否成功提取了作业路径和 Build ID
3. 构建的 URL 是否正确
4. HTTP 请求是否成功

## 编译状态

✅ 编译成功 (最后编译时间: 2026-01-18)

## 修改的文件

1. `src/main/java/com/gitviewer/JenkinsStageViewPanel.java`
   - 修改 `openStageLogDialog()` 方法
   - 将 parent 类型从 Frame 改为 Window
   - 添加详细的调试日志

2. `src/main/java/com/gitviewer/JenkinsStageLogDialog.java`
   - 修改构造函数参数类型
   - 使用 Dialog.ModalityType.APPLICATION_MODAL

3. `src/main/java/com/gitviewer/JenkinsBrowserDialog.java`
   - 增大对话框尺寸到 1200x900

4. `src/main/java/com/gitviewer/JenkinsJobDetailsDialog.java`
   - 增大对话框尺寸到 1400x900

5. `src/main/java/com/gitviewer/JenkinsApiClient.java`
   - 添加详细的调试日志到所有相关方法
