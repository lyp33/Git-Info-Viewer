# Build Package 拖放后过滤器失效问题修复

## 问题描述

在Build Package对话框中，当用户在左侧"Unfavorited Applications"列表中输入过滤关键字（例如"thailife"）后，从左侧拖动应用到右侧（或从右侧拖回左侧）时，过滤器会失效，所有应用又重新显示出来。

## 问题场景

1. 用户在左侧过滤框输入"thailife"
2. 左侧列表只显示包含"thailife"的应用
3. 用户拖动某个应用到右侧收藏区
4. **问题**：左侧列表重新显示所有应用，过滤失效

## 根本原因

在所有拖放操作完成后，代码调用了`populateApplicationList()`重新填充应用列表，但是**没有重新应用过滤器**。

### 问题代码流程

```java
// 拖放完成后
saveFavoriteApps();
populateApplicationList();  // 重新填充列表，显示所有应用
// ❌ 缺少：filterUnfavoritedApps();  // 应该重新应用过滤器
```

## 解决方案

在所有调用`populateApplicationList()`的拖放操作后，添加`filterUnfavoritedApps()`调用，重新应用过滤器。

## 修改位置

### 1. setupUnfavoritedPanelDropTarget() - 从右侧拖回左侧

**位置**：BuildPackageDialog.java 第1316行附近

**修改前**：
```java
// 保存并刷新
saveFavoriteApps();
populateApplicationList();

logger.info("Unfavorite complete");
```

**修改后**：
```java
// 保存并刷新
saveFavoriteApps();
populateApplicationList();

// 重新应用过滤器（如果有的话）
filterUnfavoritedApps();

logger.info("Unfavorite complete");
```

### 2. setupFavoritedPanelDropTarget() - 从左侧拖到右侧空白处

**位置**：BuildPackageDialog.java 第1550行附近

**修改前**：
```java
// 保存并刷新
saveFavoriteApps();
populateApplicationList();

logger.info("Drop onto empty space complete, added to Ungrouped");
```

**修改后**：
```java
// 保存并刷新
saveFavoriteApps();
populateApplicationList();

// 重新应用过滤器（如果有的话）
filterUnfavoritedApps();

logger.info("Drop onto empty space complete, added to Ungrouped");
```

### 3. setupGroupHeaderDropTarget() - 从左侧拖到Group Header

**位置**：BuildPackageDialog.java 第1387行附近

**修改前**：
```java
// 保存并刷新
saveFavoriteApps();
populateApplicationList();

logger.info("Drop from unfavorited to group complete");
```

**修改后**：
```java
// 保存并刷新
saveFavoriteApps();
populateApplicationList();

// 重新应用过滤器（如果有的话）
filterUnfavoritedApps();

logger.info("Drop from unfavorited to group complete");
```

### 4. setupUngroupedHeaderDropTarget() - 从左侧拖到Ungrouped Header

**位置**：BuildPackageDialog.java 第1475行和第1498行附近

**修改前**（两处）：
```java
// 保存并刷新
saveFavoriteApps();
populateApplicationList();

logger.info("Drop from unfavorited to ungrouped complete");
// 或
logger.info("Drop onto Ungrouped complete");
```

**修改后**（两处）：
```java
// 保存并刷新
saveFavoriteApps();
populateApplicationList();

// 重新应用过滤器（如果有的话）
filterUnfavoritedApps();

logger.info("Drop from unfavorited to ungrouped complete");
// 或
logger.info("Drop onto Ungrouped complete");
```

## filterUnfavoritedApps() 方法说明

该方法的作用：
1. 读取过滤文本框的内容
2. 遍历所有左侧应用的checkbox
3. 根据应用名称是否包含过滤关键字，设置checkbox的可见性

```java
private void filterUnfavoritedApps() {
    String filterText = unfavoritedFilterField.getText().toLowerCase().trim();
    
    logger.debug("Filtering unfavorited apps with text: {}", filterText);
    
    for (JCheckBox checkbox : unfavoritedAppCheckboxes) {
        String appName = checkbox.getText().toLowerCase();
        boolean matches = filterText.isEmpty() || appName.contains(filterText);
        checkbox.setVisible(matches);
    }
}
```

## 测试场景

### 场景1：从左侧拖到右侧
1. 在左侧过滤框输入"thailife"
2. 拖动"thailife-bff-parent"到右侧group1
3. **预期**：左侧仍然只显示包含"thailife"的应用

### 场景2：从右侧拖回左侧
1. 在左侧过滤框输入"gemini"
2. 从右侧拖动"gemini-bff-parent"回到左侧
3. **预期**：左侧仍然只显示包含"gemini"的应用

### 场景3：清空过滤器
1. 在左侧过滤框输入"boot"
2. 拖动应用
3. 清空过滤框
4. **预期**：显示所有未收藏的应用

### 场景4：多次拖放
1. 输入过滤关键字
2. 连续拖动多个应用
3. **预期**：每次拖放后过滤器都保持有效

## 技术要点

### 为什么需要重新应用过滤器？

`populateApplicationList()`方法会：
1. 清空左侧和右侧的应用列表
2. 重新创建所有checkbox
3. 重新添加到面板中

这个过程会创建新的checkbox对象，之前的过滤状态（`setVisible(false)`）会丢失。因此需要在重新填充后，再次调用`filterUnfavoritedApps()`来恢复过滤状态。

### 过滤器的工作原理

过滤器不是删除或移除checkbox，而是通过`setVisible()`方法控制显示/隐藏：
- `checkbox.setVisible(true)` - 显示匹配的应用
- `checkbox.setVisible(false)` - 隐藏不匹配的应用

### 性能考虑

`filterUnfavoritedApps()`方法非常轻量：
- 只是遍历checkbox列表
- 执行简单的字符串匹配
- 设置可见性属性
- 不涉及网络请求或复杂计算

因此在每次拖放后调用不会影响性能。

## 相关文件

- `src/main/java/com/gitviewer/BuildPackageDialog.java`
- `BUILD_PACKAGE_DRAG_DROP_ORDERING.md` (拖放排序功能)
- `BUILD_PACKAGE_FAVORITES_COMPLETE.md` (收藏功能)

## 版本信息

- 修复日期：2026-02-07
- 影响版本：1.0.0
- 修复版本：1.0.0+

## 总结

这是一个典型的"状态丢失"问题：
1. UI状态（过滤器）存储在组件的可见性属性中
2. 重新创建组件时，状态丢失
3. 解决方案：在重新创建后，重新应用状态

修复后，用户体验得到显著改善，过滤器在拖放操作后保持有效，无需重新输入过滤关键字。
