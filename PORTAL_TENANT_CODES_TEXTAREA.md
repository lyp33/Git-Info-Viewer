# Portal Settings - Tenant Codes Field Enhancement

## 修改内容 (Changes)

将Portal设置对话框中的"Tenant Codes"字段从单行文本框（JTextField）改为多行文本区域（JTextArea），以便更好地显示和编辑长的租户代码列表。

Changed the "Tenant Codes" field in Portal Settings dialog from a single-line text field (JTextField) to a multi-line text area (JTextArea) for better display and editing of long tenant code lists.

## 技术实现 (Technical Implementation)

### 修改的文件 (Modified Files)
- `src/main/java/com/gitviewer/PortalSettingsDialog.java`

### 具体改动 (Specific Changes)

1. **字段声明** (Field Declaration)
   - 从 `JTextField tenantCodesField` 改为 `JTextArea tenantCodesField`

2. **UI组件创建** (UI Component Creation)
   - 创建4行30列的JTextArea：`new JTextArea(4, 30)`
   - 启用自动换行：`setLineWrap(true)` 和 `setWrapStyleWord(true)`
   - 添加JScrollPane包装，支持垂直滚动
   - 设置滚动面板大小：400x80像素
   - 垂直滚动条：按需显示
   - 水平滚动条：禁用（因为启用了自动换行）

3. **布局调整** (Layout Adjustments)
   - 标签对齐方式从 `EAST` 改为 `NORTHEAST`（顶部对齐）
   - 以适应多行文本区域的高度

## 功能特性 (Features)

- ✅ 支持多行显示长的租户代码列表
- ✅ 自动换行，无需水平滚动
- ✅ 垂直滚动条按需显示
- ✅ 保持原有的逗号分隔格式解析功能
- ✅ 保持原有的 `tenant{sub1/sub2}` 格式支持

## 使用示例 (Usage Example)

现在可以更清晰地输入和查看多个租户代码：

```
stbd,thailife,thailifemaindev,thailifetest,thailifepresit/th,tenant1{sub1/sub2},tenant2,tenant3
```

在JTextArea中会自动换行显示，更易于阅读和编辑。

## 编译和打包 (Build)

```bash
mvn clean compile
mvn package -DskipTests
```

生成的JAR文件：
- `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 测试建议 (Testing Recommendations)

1. 打开Portal Settings对话框
2. 在Tenant Codes字段中输入长的租户代码列表
3. 验证自动换行功能正常工作
4. 验证滚动条在内容超过4行时出现
5. 保存设置并重新打开，验证内容正确保存和加载
6. 验证逗号分隔的解析功能仍然正常工作

---

**修改日期**: 2026-01-21  
**版本**: 1.0.0
