# Build Package 通配符过滤功能

## 功能说明

Build Package对话框的左侧"Unfavorited Applications"列表支持通配符（Wildcard/Glob）过滤，让用户可以使用更直观的方式过滤应用列表。

## 支持的匹配模式

### 1. 通配符模式（Glob Pattern）

当输入包含 `*` 或 `?` 时，自动识别为通配符模式：

- `*` - 匹配任意字符（0个或多个）
- `?` - 匹配单个字符

**示例**：

| 输入 | 匹配 | 说明 |
|------|------|------|
| `thailife-*-bff` | thailife-xxx-bff, thailife-claim-bff | 匹配thailife-开头，-bff结尾的应用 |
| `*-bff` | gemini-bff, thailife-bff | 匹配所有以-bff结尾的应用 |
| `thailife-*` | thailife-bff, thailife-claim | 匹配所有以thailife-开头的应用 |
| `*bff*` | gemini-bff-parent, bff-service | 匹配包含bff的所有应用 |
| `boot-?dmin` | boot-admin, boot-xdmin | 匹配boot-后跟单个字符再跟dmin |
| `gemini-??-*` | gemini-bs-parent, gemini-bff-service | 匹配gemini-后跟两个字符再跟任意内容 |

### 2. 普通文本匹配

当输入**不包含**通配符时，使用普通文本匹配（不区分大小写）：

**示例**：

| 输入 | 匹配 | 说明 |
|------|------|------|
| `thailife` | thailife-bff, thailife-claim-bff | 匹配包含"thailife"的应用 |
| `bff` | gemini-bff, thailife-bff-parent | 匹配包含"bff"的应用 |
| `GEMINI` | gemini-bff, Gemini-Claim | 不区分大小写 |

## 使用场景

### 场景1：精确匹配特定模式
```
输入：thailife-*-bff
结果：只显示 thailife-xxx-bff 格式的应用
```

### 场景2：匹配所有某类应用
```
输入：*-parent
结果：显示所有以-parent结尾的应用
```

### 场景3：模糊匹配
```
输入：gemini
结果：显示所有包含"gemini"的应用（不需要通配符）
```

### 场景4：组合使用
```
输入：thailife-*-*
结果：匹配 thailife-xxx-yyy 格式的应用
```

## 技术实现

### Glob转正则表达式

通配符模式会被转换为正则表达式：

| Glob | 正则表达式 | 说明 |
|------|-----------|------|
| `*` | `.*` | 匹配任意字符 |
| `?` | `.` | 匹配单个字符 |
| `.` | `\.` | 转义点号 |
| `-` | `-` | 保持原样 |

**转换示例**：
- `thailife-*-bff` → `^thailife-.*-bff$`
- `*-parent` → `^.*-parent$`
- `boot-?dmin` → `^boot-.dmin$`

### 匹配逻辑

```java
if (包含 * 或 ?) {
    // 通配符模式
    转换为正则表达式
    使用 pattern.matcher(appName).matches()
} else {
    // 普通文本匹配
    使用 appName.toLowerCase().contains(filterText.toLowerCase())
}
```

### 关键特性

1. **不区分大小写**：所有匹配都不区分大小写
2. **完整匹配**：通配符模式使用`^...$`，必须匹配整个应用名称
3. **部分匹配**：普通文本使用`contains()`，匹配应用名称的任意部分
4. **实时过滤**：输入时立即过滤，无需按回车
5. **持久化**：拖放操作后自动重新应用过滤器

## 与正则表达式的区别

| 特性 | 通配符（Glob） | 正则表达式（Regex） |
|------|---------------|-------------------|
| 语法 | 简单直观 | 复杂强大 |
| `*` | 匹配任意字符 | 量词（需要前置字符） |
| `?` | 匹配单个字符 | 量词（0或1次） |
| `.` | 字面点号 | 匹配任意字符 |
| 学习曲线 | 低 | 高 |
| 适用场景 | 文件名匹配 | 复杂模式匹配 |

**为什么选择通配符而不是正则表达式？**

1. **用户友好**：大多数用户熟悉 `*.txt` 这样的通配符
2. **简单直观**：不需要学习正则表达式语法
3. **避免错误**：正则表达式容易出现语法错误
4. **符合习惯**：Windows、Linux命令行都使用通配符

## 常见问题

### Q: 为什么 `thailife-*-bff` 不匹配 `thailife-bff`？
A: 通配符是完整匹配，`thailife-*-bff` 要求中间必须有内容。如果要匹配可选内容，使用 `thailife*bff`。

### Q: 如何匹配包含特殊字符的应用名？
A: 特殊字符会被自动转义，直接输入即可。例如 `app-v1.0` 会正确匹配。

### Q: 通配符区分大小写吗？
A: 不区分。`THAILIFE-*` 和 `thailife-*` 效果相同。

### Q: 可以使用多个通配符吗？
A: 可以。例如 `*-*-*` 匹配至少包含两个连字符的应用名。

### Q: 如何清除过滤？
A: 清空过滤框即可显示所有应用。

## 测试用例

### 测试1：基本通配符
```
应用列表：
- thailife-bff-parent
- thailife-claim-bff
- gemini-bff-parent

输入：thailife-*-bff
预期：只显示 thailife-claim-bff
```

### 测试2：多个通配符
```
应用列表：
- gemini-bs-model
- gemini-bff-parent
- boot-admin

输入：*-*-*
预期：显示 gemini-bs-model, gemini-bff-parent
```

### 测试3：单字符通配符
```
应用列表：
- boot-admin
- boot-xdmin
- boot-admin-service

输入：boot-?dmin
预期：显示 boot-admin, boot-xdmin
```

### 测试4：普通文本
```
应用列表：
- thailife-bff-parent
- gemini-bff-parent
- boot-admin

输入：bff
预期：显示 thailife-bff-parent, gemini-bff-parent
```

### 测试5：拖放后保持过滤
```
1. 输入：thailife-*
2. 拖动 thailife-bff 到右侧
3. 预期：左侧仍然只显示包含 thailife- 的应用
```

## 相关文件

- `src/main/java/com/gitviewer/BuildPackageDialog.java`
  - `filterUnfavoritedApps()` - 过滤逻辑
  - `globToRegex()` - Glob转正则表达式
- `BUILD_PACKAGE_FILTER_PERSISTENCE_FIX.md` - 拖放后过滤器持久化

## 版本信息

- 实现日期：2026-02-07
- 版本：1.0.0+
- 支持的通配符：`*` 和 `?`

## 总结

通配符过滤功能提供了简单直观的应用过滤方式，特别适合：
- 快速查找特定模式的应用
- 批量选择某类应用
- 减少列表滚动，提高效率

用户无需学习复杂的正则表达式语法，使用熟悉的 `*` 和 `?` 即可完成大部分过滤需求。
