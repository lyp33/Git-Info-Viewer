# Mouse Event Debug Enhancement

## 问题描述

双击收藏的 job 时：
- 没有 "Loading..." 提示出现
- 没有进度条
- 系统没有自动加载子级目录
- 控制台没有任何输出

这说明双击事件根本没有触发，或者在触发后立即失败。

## 添加的调试日志

在 `FavoritesPanel.java` 的 MouseListener 中添加了详细的日志：

### mouseClicked 方法
- 打印每次点击事件
- 打印点击次数（单击=1，双击=2）
- 打印鼠标按钮（1=左键，2=中键，3=右键）
- 打印双击检测结果
- 打印列表索引
- 打印获取的 job 对象

### mousePressed 方法
- 打印鼠标按下事件
- 打印按钮和 isPopupTrigger 状态

### mouseReleased 方法
- 打印鼠标释放事件
- 打印按钮和 isPopupTrigger 状态

## 预期日志输出

### 正常双击：

```
=== Mouse Clicked ===
Click count: 1
Button: 1
Single click, ignoring
=== Mouse Clicked ===
Click count: 2
Button: 1
Double-click detected!
Index: 0
Job retrieved: update-bs-bff-version
=== navigateToJob called ===
Job: update-bs-bff-version
Job Path: job/gemini1/job/Manual-Build/job/tools_lock/job/update-bs-bff-version
Parent Dialog: SET
Loading dialog created
Starting SwingWorker...
Showing loading dialog...
```

### 如果双击事件没有触发：

```
(没有任何输出)
```

或者只有单击输出：

```
=== Mouse Clicked ===
Click count: 1
Button: 1
Single click, ignoring
```

### 如果 job 为 null：

```
=== Mouse Clicked ===
Click count: 2
Button: 1
Double-click detected!
Index: 0
Job retrieved: NULL
ERROR: Job is null!
```

### 如果索引无效：

```
=== Mouse Clicked ===
Click count: 2
Button: 1
Double-click detected!
Index: -1
ERROR: Invalid index!
```

## 可能的问题和解决方案

### 1. 没有任何输出
**原因**: MouseListener 没有正确注册到 favoritesList

**检查**:
- FavoritesPanel 是否正确初始化
- favoritesList 是否正确创建
- MouseListener 是否在 favoritesList 创建后添加

### 2. 只有单击输出，没有双击
**原因**: 
- 双击速度太慢，被识别为两次单击
- 或者双击事件被其他组件拦截

**解决方案**:
- 尝试快速双击
- 检查是否有其他组件覆盖在列表上

### 3. 双击检测到但 job 为 null
**原因**: listModel 中没有数据或索引错误

**解决方案**:
- 检查收藏列表是否正确加载
- 检查 listModel 是否有数据

### 4. 双击检测到但 parentDialog 为 null
**原因**: FavoritesPanel 创建时没有传入 parentDialog

**解决方案**:
- 检查 JenkinsBrowserDialog 中创建 FavoritesPanel 的代码
- 确保传入了正确的 this 引用

## 测试步骤

1. 从命令行运行应用：
   ```bash
   java -jar git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```

2. 打开 Jenkins Browser

3. 添加一个 job 到收藏（如果还没有）

4. 在收藏列表中：
   - 先单击一次，查看控制台输出
   - 再双击，查看控制台输出

5. 根据输出确定问题：
   - 如果没有任何输出 → MouseListener 没有注册
   - 如果只有单击输出 → 双击没有被识别
   - 如果有双击输出但 job 为 null → 数据问题
   - 如果有双击输出但 parentDialog 为 null → 初始化问题

## 文件修改

- `src/main/java/com/gitviewer/FavoritesPanel.java` - 在 MouseListener 的所有方法中添加详细日志

## 日期

2026-01-18
