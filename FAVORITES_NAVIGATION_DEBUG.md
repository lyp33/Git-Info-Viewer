# Favorites Navigation Debug Enhancement

## 问题描述

双击收藏的 job 时，系统没有任何提示，也没有自动加载子级目录。

## 添加的调试日志

在 `FavoritesPanel.java` 的 `navigateToJob()` 方法中添加了详细的调试日志：

### 1. 方法入口日志
- 打印被双击的 job 名称和路径
- 打印 parentDialog 是否为 null

### 2. 加载对话框创建日志
- 打印加载对话框是否成功创建

### 3. SwingWorker 执行日志
- 打印 doInBackground() 开始执行
- 打印 navigateToJobPath() 的返回值
- 打印 done() 方法被调用
- 打印导航是否成功
- 打印任何异常信息

### 4. 对话框显示日志
- 打印 SwingWorker 启动
- 打印加载对话框显示
- 打印加载对话框关闭

## 测试步骤

1. 从命令行运行应用：
   ```bash
   java -jar git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```

2. 打开 Jenkins Browser

3. 添加一个 job 到收藏

4. 双击收藏列表中的 job

5. 查看控制台输出

## 预期日志输出

### 正常情况：

```
=== navigateToJob called ===
Job: update-bs-bff-version
Job Path: job/gemini1/job/Manual-Build/job/tools_lock/job/update-bs-bff-version
Parent Dialog: SET
Loading dialog created
Starting SwingWorker...
Showing loading dialog...
SwingWorker: doInBackground started
[JenkinsBrowserDialog] Navigating to job: job/gemini1/job/Manual-Build/job/tools_lock/job/update-bs-bff-version
[JenkinsBrowserDialog] Base job path: job/gemini1
[JenkinsBrowserDialog] Extracted job names: gemini1 -> Manual-Build -> tools_lock -> update-bs-bff-version
[JenkinsBrowserDialog] Base job names: gemini1
[JenkinsBrowserDialog] Skipping base path, starting from index: 1
[JenkinsBrowserDialog] Looking for: Manual-Build at index 1
[JenkinsBrowserDialog] Loading children for folder: gemini1
[JenkinsBrowserDialog] Searching among 5 children
[JenkinsBrowserDialog]   Checking child: Manual-Build
[JenkinsBrowserDialog]   Found match! Continuing to next level...
[JenkinsBrowserDialog] Looking for: tools_lock at index 2
...
SwingWorker: navigateToJobPath returned: true
SwingWorker: done() called
SwingWorker: success = true
Loading dialog closed
```

### 异常情况：

如果 parentDialog 为 null：
```
=== navigateToJob called ===
Job: update-bs-bff-version
Job Path: job/gemini1/job/Manual-Build/job/tools_lock/job/update-bs-bff-version
Parent Dialog: NULL
ERROR: parentDialog is null!
```

如果双击事件没有触发：
```
(没有任何输出)
```

如果 SwingWorker 执行失败：
```
=== navigateToJob called ===
...
SwingWorker: doInBackground started
SwingWorker: Exception in done(): <exception message>
<stack trace>
```

## 可能的问题

### 1. parentDialog 为 null
**原因**：FavoritesPanel 创建时没有正确传入 parentDialog

**解决方案**：检查 JenkinsBrowserDialog 中创建 FavoritesPanel 的代码

### 2. 双击事件没有触发
**原因**：鼠标监听器没有正确注册

**解决方案**：检查 MouseListener 的注册代码

### 3. SwingWorker 执行异常
**原因**：navigateToJobPath 方法抛出异常

**解决方案**：根据异常信息修复

### 4. 加载对话框阻塞
**原因**：模态对话框在 SwingWorker 启动前显示

**解决方案**：确保 worker.execute() 在 setVisible(true) 之前调用

## 下一步

1. 运行调试版本
2. 双击收藏的 job
3. 查看控制台输出
4. 根据日志确定问题所在

## 文件修改

- `src/main/java/com/gitviewer/FavoritesPanel.java` - 添加详细的调试日志

## 日期

2026-01-18
