# Jenkins Favorites 导航模态对话框阻塞问题修复

## 问题描述

用户报告：双击收藏的Jenkins job后，系统能正常查找到job并进入detail页面，但是Loading进度框却没有关闭，一直显示loading，然后30秒后超时显示"Cannot find job"提示框。

## 根本原因

问题的根本原因是**模态对话框阻塞**：

1. 用户双击收藏的job
2. `FavoritesPanel.navigateToJob()` 在后台线程中调用 `navigateToJobPath()`
3. `navigateToJobPath()` 成功找到job后，调用 `openJobDetails(item)`
4. **关键问题**：`openJobDetails()` 打开的是**模态对话框**（Build History Dialog）
5. 模态对话框会**阻塞当前线程**，导致 `navigateToJobPath()` 无法返回
6. `SwingWorker.get()` 一直等待，直到30秒超时
7. 超时后显示"Cannot find job"错误

### 代码流程图

```
用户双击收藏
    ↓
FavoritesPanel.navigateToJob()
    ↓
SwingWorker.doInBackground()
    ↓
parentDialog.navigateToJobPath(jobPath)
    ↓
找到目标节点，选中并滚动到可见位置
    ↓
openJobDetails(item)  ← 打开模态对话框
    ↓
【阻塞！】模态对话框等待用户关闭
    ↓
无法返回 true
    ↓
SwingWorker.get() 等待超时（30秒）
    ↓
显示 "Cannot find job" 错误
```

## 解决方案

**分离导航和打开详情的操作**：

1. `navigateToJobPath()` 只负责导航到节点，不打开详情对话框
2. 导航成功后，在 `FavoritesPanel` 中使用 `SwingUtilities.invokeLater()` 异步打开详情对话框
3. 这样 `navigateToJobPath()` 可以立即返回 `true`，Loading对话框正常关闭
4. 然后在EDT线程中打开详情对话框，不会阻塞

## 修改内容

### 1. JenkinsBrowserDialog.java

#### 修改 `navigateToJobPath()` 方法
- **移除**：自动调用 `openJobDetails(item)` 的代码
- **原因**：避免模态对话框阻塞导航线程

#### 新增 `openSelectedJobDetails()` 方法
```java
public void openSelectedJobDetails() {
    TreePath selectedPath = tree.getSelectionPath();
    if (selectedPath != null) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
        if (node != null && node.getUserObject() instanceof JenkinsItem) {
            JenkinsItem item = (JenkinsItem) node.getUserObject();
            if (!item.isFolder()) {
                openJobDetails(item);
            }
        }
    }
}
```

### 2. FavoritesPanel.java

#### 修改 `CancellableWorker.done()` 方法
```java
@Override
protected void done() {
    loadingDialog.dispose();
    try {
        boolean success = get();
        if (success) {
            // 导航成功，延迟一下再打开详情对话框
            SwingUtilities.invokeLater(() -> {
                System.out.println("[FavoritesPanel] Navigation successful, opening job details...");
                parentDialog.openSelectedJobDetails();
            });
        } else if (!cancelled) {
            // 导航失败，询问是否移除收藏
            // ...
        }
    } catch (Exception e) {
        // ...
    }
}
```

## 调试日志增强

为了更好地诊断问题，添加了详细的调试日志：

### FavoritesPanel.java
- 打印收藏任务的详细信息（Display Name, Job Path, Job URL）
- 记录导航成功后打开详情对话框的操作

### JenkinsBrowserDialog.java
- `navigateToJobPath()`: 打印输入路径、清理后的路径、分割结果
- `findNodeByJobNames()`: 打印每一步的查找过程
- `ensureChildrenLoaded()`: 打印子节点加载状态

## 测试步骤

1. 启动应用程序
2. 打开 Jenkins Browser
3. 收藏一个job
4. 双击收藏的job
5. **预期结果**：
   - Loading对话框显示
   - 系统导航到job并选中
   - Loading对话框关闭
   - Build History对话框自动打开
   - **不应该**出现"Cannot find job"错误

## 技术要点

### 模态对话框的阻塞特性
- `JDialog` 设置为 `ModalityType.APPLICATION_MODAL` 时会阻塞所有窗口
- 模态对话框会阻塞调用线程，直到对话框关闭
- 在后台线程中打开模态对话框会导致该线程无法继续执行

### SwingUtilities.invokeLater()
- 将任务放入EDT（Event Dispatch Thread）队列
- 不会阻塞当前线程
- 适合在后台任务完成后更新UI或打开对话框

### SwingWorker 生命周期
- `doInBackground()`: 在后台线程执行
- `done()`: 在EDT线程执行
- `get()`: 阻塞等待后台任务完成

## 相关文件

- `src/main/java/com/gitviewer/JenkinsBrowserDialog.java`
- `src/main/java/com/gitviewer/FavoritesPanel.java`
- `LOADING_DIALOG_FREEZE_FIX.md` (之前修复的Loading对话框卡死问题)

## 版本信息

- 修复日期：2026-02-07
- 影响版本：1.0.0
- 修复版本：1.0.0+
