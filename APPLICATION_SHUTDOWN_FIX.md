# 应用程序关闭问题修复

## 问题描述

用户报告：通过UI关闭应用后，Java进程没有完全终止，导致无法第二次启动应用，出现JNI错误。

## 根本原因

经过代码审查，发现以下问题：

1. **非守护线程阻止退出**：`AIChatDialog.java` 中创建的后台线程没有设置为守护线程（daemon thread），导致即使主窗口关闭，该线程仍在运行，阻止JVM退出。

2. **缺少强制退出机制**：虽然主窗口设置了 `EXIT_ON_CLOSE`，但如果有非守护线程存在，JVM不会自动退出。

## 修复方案

### 1. 修复 AIChatDialog 的线程问题

**文件**: `src/main/java/com/gitviewer/AIChatDialog.java`

**修改前**:
```java
new Thread(() -> {
    try {
        // ... 处理逻辑
    } catch (Exception e) {
        // ... 错误处理
    }
}).start();
```

**修改后**:
```java
Thread thread = new Thread(() -> {
    try {
        // ... 处理逻辑
    } catch (Exception e) {
        // ... 错误处理
    }
});
thread.setDaemon(true);  // 设置为守护线程，防止阻止应用退出
thread.start();
```

**说明**: 守护线程不会阻止JVM退出。当所有非守护线程结束时，JVM会自动终止所有守护线程并退出。

### 2. 添加窗口关闭监听器

**文件**: `src/main/java/com/gitviewer/GitViewerApp.java`

在 `initializeUI()` 方法中添加了窗口关闭监听器：

```java
// 添加窗口关闭监听器，确保资源清理
addWindowListener(new java.awt.event.WindowAdapter() {
    @Override
    public void windowClosing(java.awt.event.WindowEvent e) {
        System.out.println("Application closing - cleaning up resources...");
        
        // 清理所有可能的资源
        try {
            // 停止所有Timer
            for (java.awt.Window window : java.awt.Window.getWindows()) {
                if (window instanceof JDialog) {
                    window.dispose();
                }
            }
            
            // 强制退出
            System.out.println("Forcing application exit...");
            System.exit(0);
        } catch (Exception ex) {
            System.err.println("Error during cleanup: " + ex.getMessage());
            ex.printStackTrace();
            // 即使清理失败也要退出
            System.exit(1);
        }
    }
});
```

**说明**: 
- 关闭所有打开的对话框，触发它们的 `dispose()` 方法
- `TenantCICDDialog` 的 `dispose()` 方法已经实现了完整的资源清理（停止Timer、取消SwingWorker等）
- 最后调用 `System.exit(0)` 强制退出，确保所有线程都被终止

## 技术细节

### 守护线程 vs 非守护线程

- **非守护线程（User Thread）**: JVM会等待所有非守护线程结束才退出
- **守护线程（Daemon Thread）**: 当所有非守护线程结束时，JVM会自动终止守护线程并退出

### 已有的资源清理机制

`TenantCICDDialog.dispose()` 已经实现了完整的清理：
- 停止 `filterTimer` (防抖Timer)
- 停止 `hoverTimer` (悬停提示Timer)
- 取消正在运行的 `SwingWorker`
- 移除所有事件监听器
- 清空缓存数据

## 测试建议

1. 启动应用程序
2. 打开各种对话框（AI Chat、Tenant CI/CD等）
3. 通过UI关闭主窗口
4. 使用任务管理器确认Java进程已完全终止
5. 再次启动应用程序，确认没有JNI错误

## 构建命令

```bash
mvn clean package
```

生成的JAR文件：`target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 修复日期

2026-02-04
