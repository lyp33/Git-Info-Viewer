# 最终修复汇总

## 修改时间
2026-01-19 20:00

## 本次修复的问题

### 1. Git Info Panel 的 Status 字体过大
**问题**：Directory Information 页面的 "Status:" 标签字体太大，导致后面的文字显示不完整

**修改文件**：`src/main/java/com/gitviewer/InfoPanel.java`

**修改内容**：
- 将 "Status:" 标签的字体大小从默认 11px 减小到 10px
- 修改了两处相同的代码（第428行和第2098行）

**修改前**：
```html
<b style='color: #1967D2;'>Status:</b>
```

**修改后**：
```html
<b style='color: #1967D2; font-size: 10px;'>Status:</b>
```

### 2. Portal URL 提取逻辑优化
**问题**：`extractPortalUrl` 方法在整个 Stage Log 文本中搜索，效率较低且可能匹配到错误的内容

**修改文件**：`src/main/java/com/gitviewer/JenkinsApiClient.java`

**修改内容**：
- 改为逐行解析 Stage Log
- 只处理包含 "curl" 和 "portal-gw.insuremo.com" 的行
- 添加详细的调试日志输出

**修改前**：
```java
Pattern pattern = Pattern.compile("https://portal-gw\\.insuremo\\.com/[^\\s'\"\\n]+");
Matcher matcher = pattern.matcher(stageLog);
if (matcher.find()) {
    return matcher.group(0);
}
```

**修改后**：
```java
String[] lines = stageLog.split("\n");
Pattern pattern = Pattern.compile("https://portal-gw\\.insuremo\\.com/[^\\s'\"]+");

for (int i = 0; i < lines.length; i++) {
    String line = lines[i];
    if (line.contains("curl") && line.contains("portal-gw.insuremo.com")) {
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(0);
        }
    }
}
```

**优势**：
- 更精确：只在包含 curl 命令的行中查找
- 更高效：跳过不相关的行
- 更易调试：输出找到 URL 的行号和内容

### 3. Stage Log 对话框字体调整
**问题**：Stage Log 对话框顶部 Status 标签字体过大

**修改文件**：`src/main/java/com/gitviewer/JenkinsStageLogDialog.java`

**修改内容**：
- 字体大小：12 → 11
- 字体样式：BOLD → PLAIN

**修改前**：
```java
stageLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
```

**修改后**：
```java
stageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
```

## 技术细节

### 逐行解析的优势

1. **性能优化**：
   - 不需要在整个大文本中搜索
   - 可以提前终止（找到第一个匹配就返回）

2. **精确匹配**：
   - 只在包含 curl 命令的行中查找
   - 避免误匹配日志中其他地方的 URL

3. **调试友好**：
   - 输出总行数
   - 输出找到 URL 的行号
   - 输出该行的前100个字符

### 调试日志示例

```
[JenkinsApiClient] Extracting Portal URL from stage log (line by line)...
[JenkinsApiClient] Total lines to parse: 1523
[JenkinsApiClient] Found potential Portal URL line 342: + curl -s -L -X GET 'https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696af560b5479...
[JenkinsApiClient] ✓ Extracted Portal URL: https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=696af560b547926878e51c82
```

## 相关文件

- `src/main/java/com/gitviewer/InfoPanel.java` - Git 信息面板
- `src/main/java/com/gitviewer/JenkinsApiClient.java` - Jenkins API 客户端
- `src/main/java/com/gitviewer/JenkinsStageLogDialog.java` - Stage Log 对话框

## 编译状态

✅ 编译成功
✅ 打包成功

生成的 JAR 文件：
- `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 测试建议

1. **Git Info Panel**：
   - 打开一个 Git 仓库
   - 查看 Directory Information 面板
   - 验证 "Status:" 标签字体大小合适，后面的文字完整显示

2. **Portal Log**：
   - 双击 Jenkins Stage 打开对话框
   - 切换到 Portal Log tab
   - 验证能正确提取 Portal API URL
   - 检查控制台日志，确认逐行解析工作正常

3. **Stage Log 对话框**：
   - 验证顶部 Status 标签字体大小合适
   - 确认所有信息完整显示
