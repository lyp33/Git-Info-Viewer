# Build Package 过滤器完整修复

## 问题描述

在Build Package对话框中，当用户输入过滤关键字（例如`thailife-*-bff`）后，执行任何拖放操作，左侧列表会刷新并显示所有应用，过滤器失效。

## 根本原因

代码中有**多达15处**调用了`populateApplicationList()`方法来刷新UI，但只有**5处**调用了`filterUnfavoritedApps()`来重新应用过滤器。

`populateApplicationList()`会：
1. 清空左侧和右侧的应用列表
2. 重新创建所有checkbox
3. 重新添加到面板中

这个过程会创建新的checkbox对象，之前的过滤状态（`setVisible(false)`）会丢失。

## 解决方案

在**所有**调用`populateApplicationList()`的地方之后，添加`filterUnfavoritedApps()`调用。

## 修复位置清单

### ✅ 已修复的位置（共15处）

| # | 位置 | 操作 | 行号（约） |
|---|------|------|-----------|
| 1 | 创建新group | 用户点击"New Group"按钮 | ~1028 |
| 2 | 重命名group | 用户重命名分组 | ~1054 |
| 3 | 删除group | 用户删除分组 | ~1074 |
| 4 | 移动应用到其他group | 内部方法调用 | ~1151 |
| 5 | 移动应用（通用） | 内部方法调用 | ~1246 |
| 6 | 从右侧拖回左侧 | 取消收藏 | ~1315 |
| 7 | 从左侧拖到Group Header | 添加到分组 | ~1384 |
| 8 | 从favorited拖到Group Header | 移动到分组 | ~1414 |
| 9 | 从左侧拖到Ungrouped Header | 添加到未分组 | ~1473 |
| 10 | 从Group拖到Ungrouped Header | 移动到未分组 | ~1498 |
| 11 | 从左侧拖到右侧空白处 | 添加到未分组 | ~1560 |
| 12 | 从左侧拖到Group内checkbox | 添加到分组（指定位置） | ~1661 |
| 13 | 跨group拖动 | 移动到其他分组 | ~1694 |
| 14 | Group内重新排序 | 调整顺序 | ~1714 |
| 15 | Ungrouped内重新排序 | 调整顺序 | ~1736 |
| 16 | 批量添加到favorites | 点击"→"按钮 | ~1930 |
| 17 | 批量移除favorites | 点击"←"按钮 | ~1974 |

### 修复模式

所有修复都遵循相同的模式：

```java
// 保存并刷新
saveFavoriteApps();
populateApplicationList();

// 重新应用过滤器（新增）
filterUnfavoritedApps();

logger.info("Operation complete");
```

## 通配符支持

同时实现了通配符（Glob）过滤功能：

### 支持的通配符

- `*` - 匹配任意字符（0个或多个）
- `?` - 匹配单个字符

### 示例

| 输入 | 匹配 |
|------|------|
| `thailife-*-bff` | thailife-claim-bff, thailife-xxx-bff |
| `*-parent` | gemini-bff-parent, thailife-bs-parent |
| `boot-?dmin` | boot-admin, boot-xdmin |
| `thailife` | 所有包含"thailife"的应用（普通文本匹配） |

### 匹配逻辑

```java
if (包含 * 或 ?) {
    // 通配符模式
    转换为正则表达式
    使用 pattern.matcher(appName).matches()  // 完整匹配
} else {
    // 普通文本匹配
    使用 appName.toLowerCase().contains(filterText.toLowerCase())  // 部分匹配
}
```

## 测试场景

### 场景1：从左侧拖到右侧group
1. 输入过滤：`thailife-*-bff`
2. 拖动`thailife-claim-bff`到group1
3. **预期**：左侧仍然只显示匹配`thailife-*-bff`的应用

### 场景2：从右侧拖回左侧
1. 输入过滤：`gemini-*`
2. 从右侧拖动`gemini-bff-parent`回到左侧
3. **预期**：左侧仍然只显示匹配`gemini-*`的应用

### 场景3：创建新group
1. 输入过滤：`*-parent`
2. 点击"New Group"创建新分组
3. **预期**：左侧仍然只显示以`-parent`结尾的应用

### 场景4：批量操作
1. 输入过滤：`boot-*`
2. 选中多个应用，点击"→"添加到favorites
3. **预期**：左侧仍然只显示匹配`boot-*`的应用

### 场景5：重新排序
1. 输入过滤：`thailife-*`
2. 在右侧group内拖动应用重新排序
3. **预期**：左侧仍然只显示匹配`thailife-*`的应用

## 技术细节

### filterUnfavoritedApps() 方法

```java
private void filterUnfavoritedApps() {
    String filterText = unfavoritedFilterField.getText().trim();
    
    if (filterText.isEmpty()) {
        // 显示所有应用
        for (JCheckBox checkbox : unfavoritedAppCheckboxes) {
            checkbox.setVisible(true);
        }
        return;
    }
    
    // 检测是否是Glob模式
    boolean isGlob = filterText.contains("*") || filterText.contains("?");
    
    if (isGlob) {
        // 转换为正则表达式并匹配
        String regexPattern = globToRegex(filterText);
        Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        
        for (JCheckBox checkbox : unfavoritedAppCheckboxes) {
            boolean matches = pattern.matcher(checkbox.getText()).matches();
            checkbox.setVisible(matches);
        }
    } else {
        // 普通文本匹配
        for (JCheckBox checkbox : unfavoritedAppCheckboxes) {
            boolean matches = checkbox.getText().toLowerCase()
                                     .contains(filterText.toLowerCase());
            checkbox.setVisible(matches);
        }
    }
    
    unfavoritedAppListPanel.revalidate();
    unfavoritedAppListPanel.repaint();
}
```

### globToRegex() 方法

```java
private String globToRegex(String glob) {
    StringBuilder regex = new StringBuilder("^");
    
    for (char c : glob.toCharArray()) {
        switch (c) {
            case '*': regex.append(".*"); break;
            case '?': regex.append("."); break;
            case '.': case '(': case ')': case '+': 
            case '|': case '^': case '$': case '@':
            case '%': case '[': case ']': case '{':
            case '}': case '\\':
                regex.append("\\").append(c);
                break;
            default:
                regex.append(c);
                break;
        }
    }
    
    regex.append("$");
    return regex.toString();
}
```

## 性能考虑

`filterUnfavoritedApps()`方法非常轻量：
- 只遍历checkbox列表（通常几十到几百个）
- 执行简单的字符串匹配或正则表达式匹配
- 设置可见性属性（不涉及DOM操作）
- 不涉及网络请求或复杂计算

因此在每次拖放后调用不会影响性能。

## 相关文件

- `src/main/java/com/gitviewer/BuildPackageDialog.java`
  - `filterUnfavoritedApps()` - 过滤逻辑
  - `globToRegex()` - Glob转正则表达式
  - `populateApplicationList()` - 填充应用列表
- `BUILD_PACKAGE_FILTER_PERSISTENCE_FIX.md` - 初始修复文档
- `BUILD_PACKAGE_WILDCARD_FILTER.md` - 通配符功能文档

## 版本信息

- 修复日期：2026-02-07
- 版本：1.0.0+
- 修复的调用点：17处
- 支持的通配符：`*` 和 `?`

## 总结

这次修复彻底解决了过滤器失效的问题：

1. **全面覆盖**：找到并修复了所有17处`populateApplicationList()`调用
2. **通配符支持**：添加了Glob模式支持，更直观易用
3. **向后兼容**：普通文本匹配仍然有效
4. **性能优化**：过滤操作轻量快速
5. **用户体验**：拖放操作后过滤器保持有效，无需重新输入

用户现在可以：
- 使用通配符快速过滤应用
- 执行任意拖放操作
- 过滤器始终保持有效
- 无需重新输入过滤关键字
