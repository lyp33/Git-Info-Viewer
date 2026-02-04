# Auto Refresh 功能实现

## 功能描述

在 Tenant CI/CD 对话框中添加了自动刷新功能，允许用户按照指定的时间间隔自动执行搜索操作。

## UI 组件

在对话框底部的按钮面板中添加了以下组件：

1. **Auto Refresh 复选框** - 用于开启/关闭自动刷新功能
2. **刷新间隔输入框** - 输入刷新间隔时间（秒）
3. **"S" 标签** - 表示时间单位为秒

位置：在 "Download CSV" 和 "Copy Image Names" 按钮的左侧

## 功能特性

### 1. 智能刷新控制

- **防止重复搜索**：只有当上一次搜索完成后，才会触发下一次搜索
- **搜索状态标志**：使用 `isSearching` 标志跟踪搜索状态
- **自动跳过**：如果上一次搜索还在进行中（pending），即使达到了间隔时间，也不会触发新的搜索

### 2. 输入验证

- 刷新间隔必须至少为 1 秒
- 输入非法值时会显示警告对话框
- 必须先连接到 tenant 才能启用自动刷新

### 3. UI 状态管理

- 启用自动刷新时，间隔输入框会被禁用（防止运行时修改）
- 停止自动刷新时，间隔输入框会重新启用
- 复选框状态与自动刷新状态同步

### 4. 资源清理

- 对话框关闭时自动停止并清理 `autoRefreshTimer`
- 防止内存泄漏和后台线程残留

## 实现细节

### 新增字段

```java
// Auto Refresh相关
private JCheckBox autoRefreshCheckBox;
private JTextField refreshIntervalField;
private javax.swing.Timer autoRefreshTimer;
private boolean isSearching = false;  // 标记是否正在搜索中
```

### 核心方法

#### 1. handleAutoRefreshToggle()
处理复选框切换事件，启动或停止自动刷新。

#### 2. startAutoRefresh()
- 验证刷新间隔输入
- 检查连接状态
- 创建并启动 Timer
- 禁用间隔输入框

#### 3. stopAutoRefresh()
- 停止并清理 Timer
- 启用间隔输入框

#### 4. handleSearch() 修改
- 添加 `isSearching` 检查，防止重复搜索
- 搜索开始时设置 `isSearching = true`

#### 5. executeQueryByPlan() 和 executeQueryByApp() 修改
- 在 `done()` 方法中重置 `isSearching = false`
- 确保无论成功或失败都会重置标志

### Timer 实现

```java
autoRefreshTimer = new javax.swing.Timer(intervalMillis, e -> {
    // 只有当上一次搜索完成后才触发新的搜索
    if (!isSearching) {
        logger.info("Auto refresh triggered");
        handleSearch();
    } else {
        logger.info("Auto refresh skipped - previous search still in progress");
    }
});
autoRefreshTimer.setRepeats(true);
autoRefreshTimer.start();
```

## 使用流程

1. 连接到 tenant
2. 设置查询条件（Plan Name、App Name、Creator 等）
3. 在刷新间隔输入框中输入时间（秒），默认为 10 秒
4. 勾选 "Auto Refresh" 复选框
5. 系统会按照指定间隔自动执行搜索
6. 取消勾选复选框可停止自动刷新

## 日志输出

系统会记录以下日志：

- `Auto refresh toggled: true/false` - 开关切换
- `Auto refresh started with interval: X seconds` - 启动自动刷新
- `Auto refresh triggered` - 触发自动搜索
- `Auto refresh skipped - previous search still in progress` - 跳过搜索（上次未完成）
- `Auto refresh stopped` - 停止自动刷新
- `Search already in progress, skipping...` - 手动搜索被跳过（上次未完成）

## 注意事项

1. **最小间隔**：刷新间隔最小为 1 秒
2. **连接要求**：必须先连接到 tenant 才能启用自动刷新
3. **搜索保护**：不会在上一次搜索未完成时触发新搜索
4. **资源清理**：对话框关闭时会自动停止 Timer，防止内存泄漏
5. **守护线程**：Timer 是 Swing Timer，会随着应用退出自动清理

## 测试建议

1. 测试正常的自动刷新流程
2. 测试在搜索进行中时是否会跳过新的搜索
3. 测试输入非法间隔值的处理
4. 测试未连接时启用自动刷新的处理
5. 测试关闭对话框后 Timer 是否正确清理
6. 测试修改间隔值后重新启用自动刷新

## 修改文件

- `src/main/java/com/gitviewer/TenantCICDDialog.java`

## 构建命令

```bash
mvn clean package
```

## 修复日期

2026-02-04
