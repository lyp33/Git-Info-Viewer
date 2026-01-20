# Tenant CI/CD UI 优化修复

## 修复日期
2026-01-20

## 修复内容

### 1. 字体统一修复

#### 问题描述
Tenant CI/CD对话框和Portal Settings对话框的字体与应用中其他对话框不一致，导致UI风格不协调。

#### 问题分析
检查了其他对话框（如JenkinsStageLogDialog、SettingsDialog、DirectoryTreePanel等），发现应用统一使用：
- **标准字体**: `Segoe UI, Font.PLAIN, 11`
- **标题字体**: `Segoe UI, Font.BOLD, 12`（用于 TitledBorder）
- **表头字体**: `Segoe UI, Font.BOLD, 11`（用于表格列标题）

而Tenant CI/CD相关对话框使用的是默认系统字体，导致不一致。

#### 修复方案

**TenantCICDDialog.java**:
```java
// 在initializeUI()方法中
Font defaultFont = new Font("Segoe UI", Font.PLAIN, 11);
mainPanel.setFont(defaultFont);

// 递归应用字体到所有组件
applyFontRecursive(this, defaultFont);

// 在createConnectionPanel()中设置TitledBorder字体
javax.swing.border.TitledBorder border = BorderFactory.createTitledBorder("Connection");
border.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
panel.setBorder(border);

// 在createQueryPanel()中设置TitledBorder字体
javax.swing.border.TitledBorder border = BorderFactory.createTitledBorder("Query");
border.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
panel.setBorder(border);

// 在createResultsPanel()中设置TitledBorder字体和表格字体
javax.swing.border.TitledBorder border = BorderFactory.createTitledBorder("Results");
border.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
panel.setBorder(border);

// 设置表格和表头字体
resultsTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
resultsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
```

**PortalSettingsDialog.java**:
```java
// 在initializeUI()方法中
Font defaultFont = new Font("Segoe UI", Font.PLAIN, 11);
mainPanel.setFont(defaultFont);

// 递归应用字体到所有组件
applyFontRecursive(this, defaultFont);
```

### 2. 移除 Page Number 字段

#### 问题描述
查询面板中的 "Page Size" 和 "Page Number" 字段功能重复，用户反馈只需要 Page Size（每页显示多少条记录）即可。

#### 修复方案

**UI 层修改**:
- 移除 `pageNumberField` 字段声明
- 从 Query 面板中移除 "Page Number" 标签和输入框
- 调整 Search 按钮的位置（从 gridx=6 改为 gridx=4）

**逻辑层修改**:
- 在 `handleSearch()` 方法中，将 `pageNumber` 固定为 0（始终查询第一页）
- 移除对 `pageNumberField` 的引用
- 简化日志输出，移除 pageNumber 参数

**后端 API 调用**:
- `PortalApiClient.getBuildResultByApp()` 方法保持不变
- 仍然传递 `pageNumber` 参数给后端 API（固定为 0）
- API URL: `/api/mo-fo/1.0/ops/build?page_number=0&page_size={size}`

## 修改的文件
1. `src/main/java/com/gitviewer/TenantCICDDialog.java`
   - 添加默认字体设置
   - 添加 `applyFontRecursive()` 方法
   - 为所有 TitledBorder 设置标题字体
   - 为表格和表头设置字体
   - 移除 `pageNumberField` 字段
   - 移除 Page Number UI 组件
   - 将 `pageNumber` 固定为 0

2. `src/main/java/com/gitviewer/PortalSettingsDialog.java`
   - 添加默认字体设置
   - 添加 `applyFontRecursive()` 方法

## 编译结果
```
✅ mvn clean compile - SUCCESS
✅ mvn package - SUCCESS
```

## 效果
- ✅ Tenant CI/CD对话框所有组件字体统一为 Segoe UI 11
- ✅ 所有 TitledBorder 标题字体为 Segoe UI Bold 12
- ✅ 表格表头字体为 Segoe UI Bold 11
- ✅ Portal Settings对话框字体统一为 Segoe UI 11
- ✅ 与应用其他对话框风格完全一致
- ✅ UI更加协调美观
- ✅ 移除冗余的 Page Number 字段，简化查询界面
- ✅ 查询逻辑保持正确（pageNumber=0，始终查询第一页）

## 测试建议
1. 重新启动应用
2. 打开 `CI/CD` → `Portal Settings...`
   - 检查所有字体是否为 Segoe UI 11
3. 打开 `CI/CD` → `Tenant CI/CD...`
   - 检查所有UI组件字体是否一致
   - 检查 Connection、Query、Results 标题字体是否为 Segoe UI Bold 12
   - 检查表格表头字体是否为 Segoe UI Bold 11
   - 确认 Query 面板中只有 Page Size 字段，没有 Page Number 字段
4. 测试查询功能
   - 连接到租户
   - 执行查询（只需填写 Page Size）
   - 验证查询结果正确返回

## 注意事项
- 字体设置会递归应用到所有子组件
- 包括标签、文本框、按钮、下拉框等
- TitledBorder 标题需要单独设置字体
- 表格和表头字体需要分别设置
- Page Number 已从 UI 移除，但后端 API 调用仍然传递该参数（固定为 0）

---

**修复完成！请重新启动应用查看效果。**
