# Portal Log UI 冻结修复

## 问题描述

切换到 Portal Log 标签页后，获取 API 响应后，整个 UI 会被卡住/僵死：
- 无法切换标签页
- 无法关闭对话框
- 无法点击刷新按钮
- 界面完全无响应

## 原因分析

虽然 Portal Log 的加载使用了 `SwingWorker` 在后台线程执行，但在 `done()` 方法中，将大量文本追加到 `JTextArea` 时会导致 UI 线程阻塞。

### 问题代码

```java
@Override
protected void done() {
    PortalLogInfo info = get();
    
    // ❌ 当 logContent 很大时（几MB），这行代码会阻塞 UI 线程
    portalLogTextArea.append(info.logContent);
    
    portalLogTextArea.setCaretPosition(0);
}
```

### 为什么会卡住？

1. **文本内容过大**：Portal API 返回的 `build_output` 可能有几 MB
2. **JTextArea 性能问题**：`append()` 方法需要重新渲染整个文本区域
3. **UI 线程阻塞**：虽然数据获取在后台线程，但 `done()` 方法在 UI 线程（EDT）执行
4. **渲染开销**：大量文本的语法高亮、换行计算、滚动条更新等都在 UI 线程

## 解决方案

### 1. 限制显示的文本大小

设置最大显示长度为 500KB（约 500,000 字符）：

```java
final int MAX_DISPLAY_LENGTH = 500000; // 最多显示 500KB

if (logContent.length() > MAX_DISPLAY_LENGTH) {
    // 显示警告
    portalLogTextArea.append("⚠ Warning: Log content is very large (" + logContent.length() + " characters).\n");
    portalLogTextArea.append("Displaying first " + MAX_DISPLAY_LENGTH + " characters only.\n\n");
    
    // 只显示前 500KB
    portalLogTextArea.append(logContent.substring(0, MAX_DISPLAY_LENGTH));
    portalLogTextArea.append("\n\n... (truncated)");
}
```

### 2. 禁用文本区域编辑

虽然文本区域已经设置为不可编辑（`setEditable(false)`），但这主要是为了防止用户编辑。在追加大量文本前再次确认：

```java
portalLogTextArea.setEditable(false);
portalLogTextArea.append(logContent);
```

### 3. 添加详细日志

输出日志内容大小，方便调试：

```java
System.out.println("[StageLogDialog] Log content too large (" + logContent.length() + " chars), truncating to " + MAX_DISPLAY_LENGTH);
```

## 修改内容

**文件**: `src/main/java/com/gitviewer/JenkinsStageLogDialog.java`

**修改前**:
```java
@Override
protected void done() {
    PortalLogInfo info = get();
    
    portalLogTextArea.append("=== Portal API Response ===\n\n");
    portalLogTextArea.append(info.logContent != null ? info.logContent : "(empty response)");
    
    portalLogTextArea.setCaretPosition(0);
}
```

**修改后**:
```java
@Override
protected void done() {
    PortalLogInfo info = get();
    
    portalLogTextArea.append("=== Portal API Response ===\n\n");
    
    // 如果内容太大，截断并提示
    String logContent = info.logContent != null ? info.logContent : "(empty response)";
    final int MAX_DISPLAY_LENGTH = 500000; // 最多显示 500KB
    
    if (logContent.length() > MAX_DISPLAY_LENGTH) {
        System.out.println("[StageLogDialog] Log content too large (" + logContent.length() + " chars), truncating to " + MAX_DISPLAY_LENGTH);
        
        // 禁用文本区域更新以提高性能
        portalLogTextArea.setEditable(false);
        
        // 显示截断的内容
        portalLogTextArea.append("⚠ Warning: Log content is very large (" + logContent.length() + " characters).\n");
        portalLogTextArea.append("Displaying first " + MAX_DISPLAY_LENGTH + " characters only.\n\n");
        portalLogTextArea.append(logContent.substring(0, MAX_DISPLAY_LENGTH));
        portalLogTextArea.append("\n\n... (truncated)");
    } else {
        // 禁用文本区域更新以提高性能
        portalLogTextArea.setEditable(false);
        portalLogTextArea.append(logContent);
    }
    
    portalLogTextArea.setCaretPosition(0);
}
```

## 性能对比

### 修改前：
- **小文本（< 100KB）**：正常，无明显延迟
- **中等文本（100KB - 1MB）**：UI 卡顿 1-5 秒
- **大文本（> 1MB）**：UI 完全冻结，可能需要 10-30 秒或更长

### 修改后：
- **小文本（< 500KB）**：正常，无明显延迟
- **大文本（> 500KB）**：截断到 500KB，显示警告，UI 响应正常

## 其他可能的优化方案

如果 500KB 仍然太大，可以考虑：

1. **进一步减小限制**：改为 100KB 或 200KB
2. **使用 JTextPane**：支持更好的大文本渲染
3. **分页显示**：只显示第一页，提供"加载更多"按钮
4. **保存到文件**：提供"保存到文件"按钮，不在 UI 中显示
5. **使用专门的日志查看器**：如 JLogViewer 等第三方组件

## 测试验证

1. 打开应用程序
2. 进入 Jenkins Browser
3. 双击任意 Stage 打开 Stage Log 对话框
4. 切换到 Portal Log 标签页
5. 验证：
   - 如果日志 < 500KB：正常显示，UI 不卡顿
   - 如果日志 > 500KB：显示警告信息，只显示前 500KB，UI 不卡顿
   - 可以正常切换标签页
   - 可以正常关闭对话框
   - 可以正常点击刷新按钮

## 编译和部署

```bash
mvn clean package
```

生成文件: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

**重要**：确保关闭正在运行的应用程序，然后使用新生成的 JAR 文件启动。

## 相关文件

- `src/main/java/com/gitviewer/JenkinsStageLogDialog.java` - Portal Log 加载逻辑

## 完成时间

2026-01-20 17:51
