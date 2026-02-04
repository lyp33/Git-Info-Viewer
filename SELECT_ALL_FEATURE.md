# Git 仓库列表全选/反选功能

## 功能说明

在右侧 Git 仓库列表的 Select 列表头添加了全选/反选复选框功能。

## 实现内容

### 1. 表头复选框
- 在 Select 列的表头添加了一个复选框
- 复选框状态会根据当前所有 Git 仓库的选中状态自动更新
- 当所有 Git 仓库都被选中时，表头复选框显示为选中状态
- 当有任何 Git 仓库未被选中时，表头复选框显示为未选中状态

### 2. 全选/反选逻辑
- **点击表头复选框**：
  - 如果当前有任何 Git 仓库被选中 → 取消所有 Git 仓库的选中状态（反选）
  - 如果当前没有任何 Git 仓库被选中 → 选中所有 Git 仓库（全选）
- **只影响 Git 仓库行**：普通目录行不受影响

### 3. UI 设计
- 使用扁平化设计风格，与现有复选框样式保持一致
- 表头复选框带有 tooltip 提示："Click to select/deselect all Git repositories"
- 复选框图标使用蓝色主题色（PRIMARY_COLOR）

## 修改的文件

- `src/main/java/com/gitviewer/InfoPanel.java`
  - 添加了 `SelectAllHeaderRenderer` 类：表头复选框渲染器
  - 添加了 `toggleSelectAll()` 方法：全选/反选逻辑
  - 修改了表格初始化代码：设置表头渲染器和点击监听器

## 使用方法

1. 运行应用：`java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
2. 在左侧目录树中选择包含多个 Git 仓库的目录
3. 右侧会显示 Git 仓库列表
4. 点击 Select 列的表头复选框即可全选或反选所有 Git 仓库
5. 选中的仓库可以进行批量操作（如批量切换分支）

## 技术细节

### SelectAllHeaderRenderer 类
```java
private class SelectAllHeaderRenderer extends JCheckBox implements TableCellRenderer {
    // 渲染表头复选框
    // 根据所有 Git 仓库的选中状态更新复选框状态
}
```

### toggleSelectAll() 方法
```java
private void toggleSelectAll() {
    // 检查当前是否有任何选中的 Git 仓库
    // 如果有选中的，则全部取消选中
    // 如果没有选中的，则全部选中
}
```

## 测试建议

1. **基本功能测试**：
   - 点击表头复选框，验证所有 Git 仓库是否被选中
   - 再次点击表头复选框，验证所有 Git 仓库是否被取消选中

2. **混合状态测试**：
   - 手动选中部分 Git 仓库
   - 点击表头复选框，验证是否取消所有选中
   - 再次点击，验证是否全部选中

3. **普通目录测试**：
   - 验证普通目录行不受表头复选框影响
   - 验证只有 Git 仓库行的复选框可以被操作

4. **批量操作测试**：
   - 使用表头复选框全选所有 Git 仓库
   - 执行批量切换分支操作
   - 验证所有选中的仓库都被正确处理

## 版本信息

- 实现日期：2026-02-01
- 版本：1.0.0
- 编译状态：✓ 成功
- 打包状态：✓ 成功
