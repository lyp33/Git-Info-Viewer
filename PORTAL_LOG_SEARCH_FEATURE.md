# Portal Log 搜索功能实现

## 功能概述

为 Jenkins Stage Log 对话框（包括 Jenkins Log 和 Portal Log 两个 Tab）添加了 **Ctrl+F 文本搜索功能**，支持在日志内容中快速查找文本并高亮显示。

---

## 功能特性

### ✅ 已实现功能

1. **Ctrl+F 快捷键**：打开搜索面板
2. **ESC 快捷键**：关闭搜索面板
3. **F3 快捷键**：查找下一个匹配项
4. **Shift+F3 快捷键**：查找上一个匹配项
5. **不区分大小写搜索**：自动忽略大小写
6. **全文高亮**：所有匹配项用黄色背景高亮显示
7. **当前匹配项高亮**：当前查看的匹配项会被选中
8. **匹配计数**：显示 "X of Y" 格式的匹配数量
9. **自动滚动**：自动滚动到当前匹配项位置
10. **跨 Tab 支持**：Jenkins Log 和 Portal Log 都支持搜索

---

## 使用方法

### 方法 1：使用快捷键

1. 打开 Stage Log 对话框
2. 按 **Ctrl+F** 打开搜索面板
3. 输入搜索文本
4. 按 **Enter** 或点击 "Find" 按钮开始搜索
5. 使用 **F3** 跳转到下一个匹配项
6. 使用 **Shift+F3** 跳转到上一个匹配项
7. 按 **ESC** 关闭搜索面板

### 方法 2：使用按钮

1. 打开 Stage Log 对话框
2. 按 **Ctrl+F** 打开搜索面板
3. 输入搜索文本
4. 点击 **"Find"** 按钮开始搜索
5. 点击 **"Next"** 按钮跳转到下一个匹配项
6. 点击 **"Previous"** 按钮跳转到上一个匹配项
7. 点击 **"✕"** 按钮关闭搜索面板

---

## UI 界面

### 搜索面板布局

```
┌─────────────────────────────────────────────────────────────────────┐
│ Find: [搜索文本框]  [Find]  [Next]  [Previous]  (1 of 5)      [✕] │
└─────────────────────────────────────────────────────────────────────┘
```

- **Find 输入框**：输入要搜索的文本
- **Find 按钮**：开始搜索
- **Next 按钮**：跳转到下一个匹配项
- **Previous 按钮**：跳转到上一个匹配项
- **匹配计数**：显示当前匹配项位置和总数（例如 "1 of 5"）
- **关闭按钮 (✕)**：关闭搜索面板

### 搜索面板位置

搜索面板位于对话框**底部**，在 Tab 页和 Close 按钮之间。

---

## 快捷键列表

| 快捷键 | 功能 |
|--------|------|
| **Ctrl+F** | 打开搜索面板 |
| **ESC** | 关闭搜索面板 |
| **Enter** | 开始搜索（在搜索框中） |
| **F3** | 查找下一个匹配项 |
| **Shift+F3** | 查找上一个匹配项 |

---

## 搜索行为

### 1. 不区分大小写

搜索时自动忽略大小写：
- 搜索 "error" 会匹配 "Error", "ERROR", "error"

### 2. 全文高亮

所有匹配项都会用**黄色半透明背景**高亮显示：
- 高亮颜色：`rgba(255, 255, 0, 0.4)` （黄色，40% 透明度）

### 3. 当前匹配项选中

当前查看的匹配项会被**选中**（文本选择状态），便于识别。

### 4. 自动滚动

跳转到匹配项时，日志窗口会**自动滚动**到该位置，确保匹配项可见。

### 5. 循环查找

- 按 "Next" 到达最后一个匹配项后，会循环回到第一个
- 按 "Previous" 到达第一个匹配项后，会循环到最后一个

### 6. 跨 Tab 搜索

- 在 Jenkins Log Tab 中搜索，只搜索 Jenkins Log 内容
- 在 Portal Log Tab 中搜索，只搜索 Portal Log 内容
- 切换 Tab 后，搜索面板保持打开状态，但需要重新搜索

---

## 搜索结果提示

### 成功找到匹配项

```
(1 of 5)
```
显示当前是第 1 个匹配项，总共有 5 个匹配项。

### 未找到匹配项

```
No matches found
```

### 内容为空

```
No content to search
```

### 未输入搜索文本

```
Please enter search text
```

---

## 实现细节

### 核心类和方法

**文件**: `src/main/java/com/gitviewer/JenkinsStageLogDialog.java`

#### 新增字段

```java
// 搜索相关
private JPanel searchPanel;
private JTextField searchField;
private JLabel searchResultLabel;
private int currentSearchIndex = -1;
private java.util.List<Integer> searchPositions = new java.util.ArrayList<>();
```

#### 核心方法

1. **createSearchPanel()**: 创建搜索面板 UI
2. **registerSearchShortcut()**: 注册快捷键
3. **showSearchPanel()**: 显示搜索面板
4. **hideSearchPanel()**: 隐藏搜索面板
5. **performSearch()**: 执行搜索
6. **findNext()**: 查找下一个
7. **findPrevious()**: 查找上一个
8. **getCurrentTextArea()**: 获取当前活动的文本区域
9. **highlightAllMatches()**: 高亮所有匹配项
10. **highlightCurrentMatch()**: 高亮当前匹配项
11. **clearHighlights()**: 清除所有高亮

### 高亮实现

使用 Swing 的 `Highlighter` API：

```java
Highlighter highlighter = textArea.getHighlighter();
Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(
    new Color(255, 255, 0, 100)  // 黄色半透明
);
highlighter.addHighlight(startPos, endPos, painter);
```

### 快捷键注册

使用 `KeyStroke` 和 `registerKeyboardAction`：

```java
KeyStroke ctrlF = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK);
getRootPane().registerKeyboardAction(
    e -> showSearchPanel(),
    ctrlF,
    JComponent.WHEN_IN_FOCUSED_WINDOW
);
```

---

## 测试场景

### 场景 1：基本搜索

1. 打开 Stage Log 对话框
2. 按 Ctrl+F
3. 输入 "error"
4. 按 Enter
5. **预期结果**：所有 "error" 文本被黄色高亮，显示 "1 of X"

### 场景 2：跳转匹配项

1. 执行基本搜索
2. 按 F3 多次
3. **预期结果**：依次跳转到每个匹配项，计数器更新

### 场景 3：循环查找

1. 执行基本搜索
2. 按 F3 直到最后一个匹配项
3. 再按 F3
4. **预期结果**：循环回到第一个匹配项

### 场景 4：跨 Tab 搜索

1. 在 Jenkins Log Tab 中搜索 "building"
2. 切换到 Portal Log Tab
3. 搜索 "success"
4. **预期结果**：每个 Tab 独立搜索，互不影响

### 场景 5：关闭搜索面板

1. 打开搜索面板
2. 按 ESC
3. **预期结果**：搜索面板关闭，高亮清除

### 场景 6：未找到匹配项

1. 搜索 "xyz123notfound"
2. **预期结果**：显示 "No matches found"

---

## 编译和部署

### 编译

```bash
mvn clean compile
```

### 打包

```bash
mvn clean package
```

### 生成文件

```
target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

### 运行

```bash
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

---

## 编译状态

✅ **编译成功** (2026-01-20 19:19)

```
[INFO] BUILD SUCCESS
[INFO] Total time:  17.495 s
```

---

## 相关文件

- `src/main/java/com/gitviewer/JenkinsStageLogDialog.java` - Stage Log 对话框（包含搜索功能）

---

## 功能对比

### 之前

❌ 不支持 Ctrl+F 搜索
❌ 无法在日志中快速查找文本
❌ 需要手动滚动查看内容

### 现在

✅ 支持 Ctrl+F 快捷键
✅ 支持 F3/Shift+F3 快速跳转
✅ 全文高亮显示匹配项
✅ 自动滚动到匹配位置
✅ 显示匹配计数
✅ 不区分大小写搜索
✅ 跨 Tab 支持（Jenkins Log + Portal Log）

---

## 用户体验改进

1. **快速定位**：在大量日志中快速找到关键信息
2. **高亮显示**：一目了然地看到所有匹配项
3. **便捷导航**：使用快捷键快速跳转
4. **计数反馈**：清楚知道有多少个匹配项
5. **自动滚动**：无需手动滚动查找

---

## 未来改进建议

1. **正则表达式支持**：支持正则表达式搜索
2. **区分大小写选项**：添加复选框控制是否区分大小写
3. **全词匹配选项**：只匹配完整单词
4. **搜索历史**：记住最近的搜索文本
5. **替换功能**：支持查找和替换（如果需要编辑功能）

---

**功能实现时间**: 2026-01-20 19:20
**状态**: ✅ 完成并测试通过
