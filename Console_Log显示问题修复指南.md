# Console Log 显示问题修复指南

## 问题描述
Job Details 对话框中的 Console Log 区域不可见。

## 根本原因
右侧分割面板（Stage View 和 Console Log）的分隔条位置设置不当，导致 Console Log 区域被压缩。

## 已实施的修复

### 代码修改位置
文件：`src/main/java/com/gitviewer/JenkinsJobDetailsDialog.java`

### 修改内容
```java
// 右侧：Stage View（上）+ Console Log（下）
JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
rightSplitPane.setDividerLocation(250); // Stage View 占 250px
rightSplitPane.setResizeWeight(0.4);    // Stage View 占 40%，Console Log 占 60%
rightSplitPane.setBorder(null);
```

**关键改进：**
1. 将分隔条位置从 180px 增加到 250px
2. 添加 `setResizeWeight(0.4)` 确保 Console Log 占据 60% 的空间
3. Stage View 占 40%，Console Log 占 60%

## 重新编译步骤

### 步骤 1：关闭所有 Java 应用程序

**方法 A：使用提供的批处理文件（推荐）**
```bash
close-java-apps.bat
```

**方法 B：手动关闭**
1. 关闭 Git Info Viewer 应用程序窗口
2. 关闭 Mock Jenkins Server 窗口（如果正在运行）
3. 确认所有 Java 进程已关闭：
   ```bash
   tasklist | findstr java.exe
   ```

### 步骤 2：清理并重新编译

```bash
mvn clean package
```

### 步骤 3：运行新版本

```bash
java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
```

## 验证修复

1. 启动 Mock Jenkins Server（如果需要测试）：
   ```bash
   start-mock-jenkins.bat
   ```

2. 在 Git Info Viewer 中：
   - 打开 Jenkins Browser
   - 导航到任意 Job（例如：gemini > Manual-Build > backend-service）
   - 双击打开 Job Details 对话框

3. 检查 Console Log 区域：
   - **位置**：对话框右下角
   - **标题**：应该看到 "Console Log" 标题
   - **内容**：应该显示带时间戳的日志消息
   - **样式**：深色背景（RGB: 30, 30, 30），浅色文字（RGB: 200, 200, 200）

## Console Log 功能说明

### 显示的信息类型
Console Log 会显示以下信息：

1. **初始化消息**
   ```
   [14:58:53] Job Details Dialog initialized for: /job/gemini/job/Manual-Build/job/backend-service
   ```

2. **加载构建历史**
   ```
   [14:58:53] Loading build history for job: /job/gemini/job/Manual-Build/job/backend-service
   [14:58:58] Successfully loaded 5 builds
   [14:58:58] Auto-selected build #1
   ```

3. **加载 Stage View**
   ```
   [14:58:58] Loading module view for build #1
   [14:59:03] Successfully loaded 5 modules
   [14:59:03]   Module: gemini-pa-bs-parent, ID: 6, Status: SUCCESS
   [14:59:03]   Module: bff-parent, ID: 11, Status: SUCCESS
   ```

4. **错误消息**（如果有）
   ```
   [14:59:03] ERROR: Failed to load build history: HTTP error code: 400
   ```

5. **用户操作**
   ```
   [14:59:10] Opening build parameters dialog...
   [14:59:15] Build dialog closed. Click Refresh to update build history.
   ```

### Console Log 的优势
- **无弹窗干扰**：所有错误和信息都记录在 Console Log 中，不会弹出对话框
- **完整历史**：可以查看所有操作的完整历史记录
- **时间戳**：每条消息都带有精确的时间戳
- **自动滚动**：新消息会自动滚动到底部

## 当前状态

### 已修复的问题
1. ✅ 移除了导致循环错误的 `windowGainedFocus` 监听器
2. ✅ 移除了所有错误弹窗对话框
3. ✅ 移除了自动刷新 Timer
4. ✅ 调整了 Console Log 区域的大小和位置

### 待验证
- Console Log 区域是否在 Job Details 对话框中可见
- Console Log 是否正确显示所有日志消息
- 分隔条是否可以手动调整

## 故障排除

### 问题：Console Log 仍然不可见

**可能原因 1：使用了旧的 JAR 文件**
- 确认已关闭所有 Java 应用程序
- 重新运行 `mvn clean package`
- 检查编译时间戳

**可能原因 2：分隔条被拖到底部**
- 在 Stage View 和 Console Log 之间查找分隔条
- 向上拖动分隔条以显示 Console Log

**可能原因 3：窗口太小**
- 调整 Job Details 对话框的大小（默认：1000x700）
- 确保窗口足够大以显示所有组件

### 问题：编译失败（JAR 文件被锁定）

**解决方案：**
```bash
# 方法 1：使用批处理文件
close-java-apps.bat

# 方法 2：手动终止进程
taskkill /F /IM java.exe

# 然后重新编译
mvn clean package
```

## 技术细节

### UI 布局结构
```
JenkinsJobDetailsDialog
├── Top Panel (Job name + Buttons)
├── Main Split Pane (Horizontal)
│   ├── Left: Build History List (250px)
│   └── Right Split Pane (Vertical)
│       ├── Top: Stage View Panel (40% / 250px)
│       └── Bottom: Console Log Panel (60%)
└── Bottom Panel (Close button)
```

### Console Log 配置
- **字体**：Monospaced, Plain, 12pt
- **背景色**：RGB(30, 30, 30) - 深灰色
- **前景色**：RGB(200, 200, 200) - 浅灰色
- **光标色**：白色
- **自动滚动**：启用

## 下一步

1. 关闭所有 Java 应用程序
2. 运行 `close-java-apps.bat` 或手动关闭
3. 重新编译：`mvn clean package`
4. 启动应用程序并验证 Console Log 是否可见
5. 如果仍有问题，请提供截图或详细描述

---

**编译时间**：2026-01-18 14:58:53
**修改文件**：JenkinsJobDetailsDialog.java
**状态**：等待用户关闭 Java 进程后重新编译
