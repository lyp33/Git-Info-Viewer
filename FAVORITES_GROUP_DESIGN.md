# 收藏夹分组功能设计文档

## 功能概述

在 Build Package 对话框的 "Favorited Applications" 区域添加分组功能，允许用户将收藏的应用组织到不同的分组中，提高管理效率。

## 功能需求

### 核心功能
1. ✅ 创建分组：右键空白处添加新分组
2. ✅ 分组显示：可折叠的分组标题栏
3. ✅ 移动应用：右键应用可移动到指定分组
4. ✅ 批量选择：勾选分组自动勾选/取消勾选该组下所有应用
5. ✅ 拖拽排序：应用在同一分组内可拖拽排序

### 简化版不包含（可后续扩展）
- ❌ 分组之间的拖拽排序
- ❌ 应用跨分组拖拽
- ❌ 分组重命名
- ❌ 分组删除（空分组自动删除）

## 数据结构设计

### 1. FavoriteGroup 类（已创建）

```java
public class FavoriteGroup {
    private String name;              // 分组名称
    private List<String> appNames;    // 应用名称列表
    private boolean expanded;         // 是否展开
    
    // 方法：
    // - addApp(String appName)
    // - removeApp(String appName)
    // - containsApp(String appName)
    // - isEmpty()
    // - size()
}
```

### 2. 存储结构

在 AppSettings 中添加：
```java
// 分组列表（按顺序）
private List<FavoriteGroup> favoriteGroups;

// 未分组的收藏应用
private List<String> ungroupedFavorites;
```

存储格式（JSON）：
```json
{
  "favoriteGroups": [
    {
      "name": "Core Services",
      "appNames": ["thailife-bs", "thailife-fn-bs"],
      "expanded": true
    },
    {
      "name": "BFF Services",
      "appNames": ["thailife-claim-bff", "thailife-uw-bff"],
      "expanded": false
    }
  ],
  "ungroupedFavorites": ["thailife-admin-ui"]
}
```

## UI 设计

### 当前结构
```
Favorited Applications
├── Select All Checkbox
├── JScrollPane
    └── JPanel (BoxLayout.Y_AXIS)
        ├── JCheckBox (app1)
        ├── JCheckBox (app2)
        └── ...
```

### 新结构
```
Favorited Applications
├── Select All Checkbox
├── JScrollPane
    └── JPanel (BoxLayout.Y_AXIS)
        ├── GroupHeaderPanel (可折叠)
        │   ├── JCheckBox (group checkbox)
        │   ├── JLabel (group name)
        │   └── JLabel (expand/collapse icon)
        ├── JPanel (group content, 可隐藏)
        │   ├── JCheckBox (app1)
        │   └── JCheckBox (app2)
        ├── GroupHeaderPanel (另一个分组)
        ├── JPanel (group content)
        └── JPanel (未分组应用)
            └── JCheckBox (ungrouped apps)
```

### GroupHeaderPanel 设计

```java
class GroupHeaderPanel extends JPanel {
    private JCheckBox groupCheckbox;
    private JLabel groupNameLabel;
    private JLabel expandIcon;
    private FavoriteGroup group;
    
    // 特性：
    // - 背景色区分（浅灰色）
    // - 鼠标悬停效果
    // - 点击展开/折叠
    // - 右键菜单（重命名、删除）
}
```

## 交互逻辑

### 1. 创建分组

**触发**：右键点击收藏区域空白处

**流程**：
1. 显示右键菜单，包含 "Add Group" 选项
2. 点击后弹出输入对话框
3. 输入分组名称（验证不为空、不重复）
4. 创建新分组，添加到列表末尾
5. 刷新UI显示

**代码位置**：
```java
// BuildPackageDialog.java
private void showFavoritesContextMenu(MouseEvent e) {
    JPopupMenu menu = new JPopupMenu();
    JMenuItem addGroupItem = new JMenuItem("Add Group");
    addGroupItem.addActionListener(evt -> handleAddGroup());
    menu.add(addGroupItem);
    menu.show(e.getComponent(), e.getX(), e.getY());
}

private void handleAddGroup() {
    String groupName = JOptionPane.showInputDialog(
        this, "Enter group name:", "Add Group", 
        JOptionPane.PLAIN_MESSAGE);
    
    if (groupName != null && !groupName.trim().isEmpty()) {
        // 验证名称不重复
        // 创建新分组
        // 保存到AppSettings
        // 刷新UI
    }
}
```

### 2. 移动应用到分组

**触发**：右键点击应用复选框

**流程**：
1. 显示右键菜单，包含 "Move to Group" 子菜单
2. 子菜单列出所有分组 + "Ungrouped"
3. 选择目标分组
4. 从当前位置移除应用
5. 添加到目标分组
6. 保存并刷新UI

**代码位置**：
```java
private void showAppContextMenu(MouseEvent e, JCheckBox appCheckbox) {
    JPopupMenu menu = new JPopupMenu();
    JMenu moveToMenu = new JMenu("Move to Group");
    
    // 添加"Ungrouped"选项
    JMenuItem ungroupedItem = new JMenuItem("Ungrouped");
    ungroupedItem.addActionListener(evt -> 
        moveAppToGroup(appCheckbox.getText(), null));
    moveToMenu.add(ungroupedItem);
    
    // 添加所有分组
    for (FavoriteGroup group : favoriteGroups) {
        JMenuItem groupItem = new JMenuItem(group.getName());
        groupItem.addActionListener(evt -> 
            moveAppToGroup(appCheckbox.getText(), group));
        moveToMenu.add(groupItem);
    }
    
    menu.add(moveToMenu);
    menu.show(e.getComponent(), e.getX(), e.getY());
}
```

### 3. 分组勾选联动

**触发**：点击分组复选框

**逻辑**：
- 勾选分组 → 勾选该组下所有应用
- 取消勾选分组 → 取消勾选该组下所有应用
- 应用部分勾选 → 分组显示为"部分选中"状态（可选）

**代码位置**：
```java
private void onGroupCheckboxChanged(FavoriteGroup group, boolean selected) {
    // 找到该组下所有应用的复选框
    for (String appName : group.getAppNames()) {
        JCheckBox appCheckbox = findAppCheckbox(appName);
        if (appCheckbox != null) {
            appCheckbox.setSelected(selected);
        }
    }
}
```

### 4. 展开/折叠分组

**触发**：点击分组标题栏

**逻辑**：
- 切换 expanded 状态
- 显示/隐藏该组的应用面板
- 更新展开图标（▼ / ▶）
- 保存状态到 AppSettings

**代码位置**：
```java
private void toggleGroupExpanded(FavoriteGroup group) {
    group.setExpanded(!group.isExpanded());
    refreshFavoritedAppList();
    saveFavoriteGroups();
}
```

### 5. 拖拽排序

**范围**：仅支持同一分组内的应用排序

**实现**：
- 使用现有的 TransferHandler
- 限制拖拽目标为同一分组
- 更新分组内应用顺序
- 保存到 AppSettings

## 存储方案

### AppSettings 修改

```java
// 添加字段
private List<FavoriteGroup> favoriteGroups = new ArrayList<>();
private List<String> ungroupedFavorites = new ArrayList<>();

// 添加方法
public List<FavoriteGroup> getFavoriteGroups() {
    return new ArrayList<>(favoriteGroups);
}

public void setFavoriteGroups(List<FavoriteGroup> groups) {
    this.favoriteGroups = new ArrayList<>(groups);
    saveSettings();
}

public List<String> getUngroupedFavorites() {
    return new ArrayList<>(ungroupedFavorites);
}

public void setUngroupedFavorites(List<String> apps) {
    this.ungroupedFavorites = new ArrayList<>(apps);
    saveSettings();
}

// 序列化/反序列化
private void saveFavoriteGroupsToProperties() {
    try {
        JSONArray groupsArray = new JSONArray();
        for (FavoriteGroup group : favoriteGroups) {
            JSONObject groupObj = new JSONObject();
            groupObj.put("name", group.getName());
            groupObj.put("appNames", new JSONArray(group.getAppNames()));
            groupObj.put("expanded", group.isExpanded());
            groupsArray.put(groupObj);
        }
        properties.setProperty("favoriteGroups", groupsArray.toString());
        properties.setProperty("ungroupedFavorites", 
            new JSONArray(ungroupedFavorites).toString());
    } catch (Exception e) {
        logger.error("Failed to save favorite groups", e);
    }
}

private void loadFavoriteGroupsFromProperties() {
    try {
        String groupsJson = properties.getProperty("favoriteGroups", "[]");
        JSONArray groupsArray = new JSONArray(groupsJson);
        
        favoriteGroups.clear();
        for (int i = 0; i < groupsArray.length(); i++) {
            JSONObject groupObj = groupsArray.getJSONObject(i);
            String name = groupObj.getString("name");
            JSONArray appsArray = groupObj.getJSONArray("appNames");
            boolean expanded = groupObj.optBoolean("expanded", true);
            
            List<String> appNames = new ArrayList<>();
            for (int j = 0; j < appsArray.length(); j++) {
                appNames.add(appsArray.getString(j));
            }
            
            favoriteGroups.add(new FavoriteGroup(name, appNames, expanded));
        }
        
        String ungroupedJson = properties.getProperty("ungroupedFavorites", "[]");
        JSONArray ungroupedArray = new JSONArray(ungroupedJson);
        ungroupedFavorites.clear();
        for (int i = 0; i < ungroupedArray.length(); i++) {
            ungroupedFavorites.add(ungroupedArray.getString(i));
        }
    } catch (Exception e) {
        logger.error("Failed to load favorite groups", e);
        favoriteGroups.clear();
        ungroupedFavorites.clear();
    }
}
```

## 实现步骤

### Phase 1: 数据层（1-2小时）
1. ✅ 创建 FavoriteGroup 类
2. ⬜ 修改 AppSettings 添加分组存储
3. ⬜ 实现序列化/反序列化
4. ⬜ 添加分组管理方法

### Phase 2: UI层（2-3小时）
1. ⬜ 创建 GroupHeaderPanel 组件
2. ⬜ 修改 refreshFavoritedAppList() 方法
3. ⬜ 实现分组展开/折叠
4. ⬜ 添加分组复选框联动

### Phase 3: 交互层（2-3小时）
1. ⬜ 实现右键菜单（空白处添加分组）
2. ⬜ 实现右键菜单（应用移动到分组）
3. ⬜ 实现拖拽排序限制
4. ⬜ 实现自动保存

### Phase 4: 测试和优化（1-2小时）
1. ⬜ 测试分组创建和删除
2. ⬜ 测试应用移动
3. ⬜ 测试勾选联动
4. ⬜ 测试数据持久化
5. ⬜ UI优化和bug修复

## 关键代码片段

### 刷新收藏列表（核心方法）

```java
private void refreshFavoritedAppList() {
    favoritedAppListPanel.removeAll();
    favoritedAppCheckboxes.clear();
    
    // 加载分组和应用
    List<FavoriteGroup> groups = AppSettings.getInstance().getFavoriteGroups();
    List<String> ungrouped = AppSettings.getInstance().getUngroupedFavorites();
    
    // 渲染每个分组
    for (FavoriteGroup group : groups) {
        // 添加分组标题
        JPanel groupHeader = createGroupHeader(group);
        favoritedAppListPanel.add(groupHeader);
        
        // 添加分组内容（如果展开）
        if (group.isExpanded()) {
            JPanel groupContent = createGroupContent(group);
            favoritedAppListPanel.add(groupContent);
        }
    }
    
    // 添加未分组应用
    if (!ungrouped.isEmpty()) {
        JLabel ungroupedLabel = new JLabel("Ungrouped");
        ungroupedLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        favoritedAppListPanel.add(ungroupedLabel);
        
        for (String appName : ungrouped) {
            JCheckBox checkbox = createAppCheckbox(appName);
            favoritedAppListPanel.add(checkbox);
            favoritedAppCheckboxes.add(checkbox);
        }
    }
    
    favoritedAppListPanel.revalidate();
    favoritedAppListPanel.repaint();
}
```

## 注意事项

1. **向后兼容**：需要处理旧版本的收藏列表数据迁移
2. **性能**：大量应用时UI刷新性能
3. **用户体验**：拖拽时的视觉反馈
4. **数据一致性**：确保应用不会同时存在于多个分组
5. **错误处理**：分组名称验证、空分组处理

## 测试用例

1. 创建分组
   - 输入有效名称
   - 输入空名称（应拒绝）
   - 输入重复名称（应拒绝）

2. 移动应用
   - 从未分组移动到分组
   - 从分组移动到另一个分组
   - 从分组移动到未分组

3. 勾选联动
   - 勾选分组，验证所有子应用被勾选
   - 取消勾选分组，验证所有子应用被取消
   - 手动勾选部分应用，验证分组状态

4. 展开/折叠
   - 点击分组标题切换状态
   - 重启应用后状态保持

5. 数据持久化
   - 创建分组后重启应用
   - 移动应用后重启应用
   - 验证数据正确加载

## 预估工作量

- **简化版实现**：6-10小时
- **完整版实现**：15-20小时（包括所有高级功能）
- **测试和优化**：3-5小时

## 建议

由于这是一个较大的功能改动，建议：
1. 先实现数据层和基本UI
2. 逐步添加交互功能
3. 充分测试后再发布
4. 考虑添加"导入/导出分组配置"功能
5. 未来可以添加分组颜色标记、图标等

## 参考资料

- Java Swing JTree 文档
- TransferHandler 拖拽实现
- JSON 序列化最佳实践
