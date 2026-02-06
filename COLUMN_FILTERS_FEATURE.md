# 列过滤功能 - Column Filters Feature

## 概述 / Overview

为 Tenant CI/CD 对话框的结果表格添加了三个列的过滤功能，支持精确匹配和模糊匹配。

Added filtering functionality for three columns in the Tenant CI/CD dialog results table, supporting exact and fuzzy matching.

## 完成时间 / Completion Date

**2026-02-06**

## 实现的功能 / Implemented Features

### 1. Image Name 列过滤 🆕
- ✅ 点击列头的 [F] 图标打开过滤对话框
- ✅ 支持模糊匹配（不区分大小写）
- ✅ 输入关键词即可过滤
- ✅ 支持部分匹配

**示例：**
- 输入 `thailife` 可以匹配 `docker.all.repo.ebaotech.com/thailife/thailife-bs-dev`
- 输入 `admin` 可以匹配所有包含 admin 的镜像名称

### 2. Version 列过滤 🆕
- ✅ 点击列头的 [F] 图标打开过滤对话框
- ✅ 支持模糊匹配（不区分大小写）
- ✅ 输入关键词即可过滤
- ✅ 支持部分匹配

**示例：**
- 输入 `dev_2026` 可以匹配所有包含 dev_2026 的版本
- 输入 `20260206` 可以匹配特定日期的版本

### 3. Git Branch 列过滤（已有功能）
- ✅ 点击列头的 [F] 图标打开过滤对话框
- ✅ 下拉列表选择（精确匹配）
- ✅ 支持输入过滤分支列表
- ✅ 支持自动完成

### 4. 组合过滤 🆕
- ✅ 支持同时应用多个过滤器
- ✅ 过滤器之间是 AND 关系
- ✅ 状态栏显示所有活动的过滤器
- ✅ 过滤图标颜色指示：
  - 灰色 [F] - 未应用过滤
  - 蓝色 [F] - 已应用过滤

## 技术实现 / Technical Implementation

### 新增文件 / New Files

1. **TextFilterDialog.java**
   - 通用文本过滤对话框
   - 支持模糊匹配
   - 简洁的 UI 设计
   - Clear/OK/Cancel 按钮

### 修改文件 / Modified Files

1. **TenantCICDDialog.java**
   - 添加 `currentVersionFilter` 字段
   - 添加 `currentImageNameFilter` 字段
   - 重构 `addBranchFilterIcon()` 为 `addColumnFilterIcons()`
   - 添加 `addFilterIconToColumn()` 方法
   - 添加 `handleVersionFilter()` 方法
   - 添加 `handleImageNameFilter()` 方法
   - 重构 `applyBranchFilter()` 为 `applyFilters()`
   - 支持组合过滤逻辑

### 过滤逻辑 / Filter Logic

```java
// 1. 分支过滤（精确匹配）
if (currentBranchFilter != null) {
    filteredResults = filteredResults.stream()
        .filter(result -> currentBranchFilter.equals(result.getGitBranch()))
        .collect(Collectors.toList());
}

// 2. 版本过滤（模糊匹配，不区分大小写）
if (currentVersionFilter != null) {
    String lowerFilter = currentVersionFilter.toLowerCase();
    filteredResults = filteredResults.stream()
        .filter(result -> {
            String version = result.getVersion();
            return version != null && version.toLowerCase().contains(lowerFilter);
        })
        .collect(Collectors.toList());
}

// 3. 镜像名称过滤（模糊匹配，不区分大小写）
if (currentImageNameFilter != null) {
    String lowerFilter = currentImageNameFilter.toLowerCase();
    filteredResults = filteredResults.stream()
        .filter(result -> {
            String imageName = result.getImageName();
            return imageName != null && imageName.toLowerCase().contains(lowerFilter);
        })
        .collect(Collectors.toList());
}
```

## 使用指南 / User Guide

### 使用单个过滤器

1. 在 Tenant CI/CD 对话框中执行搜索
2. 点击列头的 [F] 图标（Image Name、Version 或 Git Branch）
3. 在弹出的对话框中输入过滤条件
4. 点击 OK 应用过滤
5. 表格只显示匹配的结果

### 使用组合过滤器

1. 先应用第一个过滤器（例如：Git Branch = "dev"）
2. 再应用第二个过滤器（例如：Version 包含 "20260206"）
3. 可选：应用第三个过滤器（例如：Image Name 包含 "thailife"）
4. 表格显示同时满足所有条件的结果
5. 状态栏显示：`X results displayed (filtered by branch: dev, version: 20260206, image: thailife)`

### 清除过滤器

**方法 1：** 点击过滤对话框中的 "Clear" 按钮
**方法 2：** 在过滤对话框中删除所有文本，点击 OK

### 过滤器状态指示

- **灰色 [F]** - 该列未应用过滤
- **蓝色 [F]** - 该列已应用过滤

## UI 截图说明 / UI Description

### 表头过滤图标

```
┌─────────────────────────────────────────────────────────────────┐
│ Select │ App Name │ Image Name [F] │ ... │ Version [F] │ Git Branch [F] │
├─────────────────────────────────────────────────────────────────┤
│   ☐    │ app-bff  │ docker.../app:v1│ ... │ dev_2026... │ dev            │
└─────────────────────────────────────────────────────────────────┘
```

### 文本过滤对话框

```
┌──────────────────────────────────────────┐
│  Filter by Version                   [X] │
├──────────────────────────────────────────┤
│  Version (Fuzzy Match)                   │
│  ┌────────────────────────────────────┐  │
│  │ 20260206                           │  │
│  └────────────────────────────────────┘  │
│                                           │
│  Enter keyword to filter                 │
│  (case-insensitive, partial match)       │
│                                           │
│              [Clear] [OK] [Cancel]       │
└──────────────────────────────────────────┘
```

## 测试场景 / Test Scenarios

### ✅ 已验证的场景

1. **单个过滤器**
   - ✅ Image Name 过滤
   - ✅ Version 过滤
   - ✅ Git Branch 过滤

2. **组合过滤器**
   - ✅ Branch + Version
   - ✅ Branch + Image Name
   - ✅ Version + Image Name
   - ✅ Branch + Version + Image Name

3. **模糊匹配**
   - ✅ 部分匹配
   - ✅ 不区分大小写
   - ✅ 特殊字符

4. **清除过滤器**
   - ✅ Clear 按钮
   - ✅ 删除文本后 OK
   - ✅ Cancel 保持原状态

5. **UI 反馈**
   - ✅ 图标颜色变化
   - ✅ 状态栏显示
   - ✅ 结果数量更新

## 性能考虑 / Performance Considerations

- 过滤操作在内存中进行，速度快
- 使用 Java Stream API 进行高效过滤
- 不需要重新查询 API
- 适用于大量结果（1000+ 行）

## 兼容性 / Compatibility

- ✅ 与现有的 Git Branch 过滤功能兼容
- ✅ 与自动刷新功能兼容
- ✅ 与 CSV 导出功能兼容
- ✅ 与复制镜像名称功能兼容
- ✅ 与重新构建功能兼容

## 未来增强 / Future Enhancements

可选的改进方向：

1. **保存过滤器配置**
   - 记住用户最后使用的过滤器
   - 跨会话保持过滤器状态

2. **快速过滤**
   - 在列头直接输入过滤文本
   - 无需打开对话框

3. **正则表达式支持**
   - 高级用户可以使用正则表达式
   - 更强大的匹配能力

4. **过滤器历史**
   - 记录最近使用的过滤条件
   - 快速重新应用

## 总结 / Summary

成功为 Tenant CI/CD 对话框添加了三列的过滤功能，支持精确匹配和模糊匹配，可以组合使用多个过滤器。功能已编译、打包并可以投入使用。用户可以通过点击列头的 [F] 图标来快速过滤结果，提高工作效率。
