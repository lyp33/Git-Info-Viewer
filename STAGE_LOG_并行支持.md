# Stage Log 并行 Stage 支持

## 问题

原来的 `extractStageLogFromConsole` 方法只能处理串行 Stage，无法正确提取并行执行的 Stage 日志。

### Console Log 结构

#### 串行 Stage（简单）
```
[Pipeline] stage
[Pipeline] { (gemini-pa-bs-parent)
[Pipeline] build
Starting building: gemini ? Manual-Build ? ... #809
[Pipeline] }
[Pipeline] // stage
```

#### 并行 Stage（复杂）
```
[Pipeline] parallel
[Pipeline] [firstBranch] { (Branch: firstBranch)
[Pipeline] [firstBranch] stage
[Pipeline] [firstBranch] { (pa-bs)
[Pipeline] [firstBranch] build
[firstBranch] Starting building: gemini ? Manual-Build ? ... #578
[Pipeline] [firstBranch] }
[Pipeline] [firstBranch] // stage
[Pipeline] [firstBranch] }
```

## 解决方案

### 新的实现

将 `extractStageLogFromConsole` 方法拆分为三个方法：

1. **extractStageLogFromConsole** - 主方法，尝试两种模式
2. **extractSerialStageLog** - 提取串行 Stage 日志
3. **extractParallelStageLog** - 提取并行 Stage 日志

### 代码实现

#### 主方法
```java
/**
 * 从完整的 Console Log 中提取特定 Stage 的日志
 */
public String extractStageLogFromConsole(String fullConsoleLog, String stageName) {
    if (fullConsoleLog == null || fullConsoleLog.isEmpty() || stageName == null) {
        return "";
    }
    
    // 尝试两种模式：
    // 1. 串行 Stage: [Pipeline] { (stageName)
    // 2. 并行 Stage: [Pipeline] [branchName] { (stageName)
    
    String stageLog = extractSerialStageLog(fullConsoleLog, stageName);
    if (!stageLog.isEmpty()) {
        return stageLog;
    }
    
    stageLog = extractParallelStageLog(fullConsoleLog, stageName);
    if (!stageLog.isEmpty()) {
        return stageLog;
    }
    
    logger.warn("No log found for stage: {}", stageName);
    return "";
}
```

#### 串行 Stage 提取
```java
/**
 * 提取串行 Stage 的日志
 * 格式: [Pipeline] { (stageName)
 */
private String extractSerialStageLog(String fullConsoleLog, String stageName) {
    // 查找 Stage 开始标记：[Pipeline] { (stageName)
    String stageStartMarker = "[Pipeline] { (" + stageName + ")";
    int startIndex = fullConsoleLog.indexOf(stageStartMarker);
    
    if (startIndex == -1) {
        return "";
    }
    
    // 从 Stage 名称之前开始（包含 [Pipeline] stage）
    int stageLineStart = fullConsoleLog.lastIndexOf("[Pipeline] stage", startIndex);
    if (stageLineStart != -1) {
        startIndex = stageLineStart;
    }
    
    // 查找 Stage 结束标记：[Pipeline] // stage
    String stageEndMarker = "[Pipeline] // stage";
    int endIndex = fullConsoleLog.indexOf(stageEndMarker, startIndex);
    
    if (endIndex == -1) {
        return fullConsoleLog.substring(startIndex);
    }
    
    // 包含结束标记
    endIndex = fullConsoleLog.indexOf("\n", endIndex);
    if (endIndex == -1) {
        endIndex = fullConsoleLog.length();
    }
    
    String stageLog = fullConsoleLog.substring(startIndex, endIndex);
    logger.info("Extracted serial stage log for '{}', length: {}", stageName, stageLog.length());
    return stageLog;
}
```

#### 并行 Stage 提取
```java
/**
 * 提取并行 Stage 的日志
 * 格式: [Pipeline] [branchName] { (stageName)
 */
private String extractParallelStageLog(String fullConsoleLog, String stageName) {
    // 查找并行 Stage 开始标记：[Pipeline] [xxx] { (stageName)
    // 使用正则表达式匹配：\[Pipeline\] \[.*?\] \{ \(stageName\)
    String pattern = "\\[Pipeline\\] \\[([^\\]]+)\\] \\{ \\(" + Pattern.quote(stageName) + "\\)";
    Pattern p = Pattern.compile(pattern);
    Matcher m = p.matcher(fullConsoleLog);
    
    if (!m.find()) {
        return "";
    }
    
    String branchName = m.group(1);  // 提取 branch 名称
    int startIndex = m.start();
    
    logger.info("Found parallel stage '{}' in branch '{}'", stageName, branchName);
    
    // 从 Stage 声明之前开始（包含 [Pipeline] [branchName] stage）
    String branchStageMarker = "[Pipeline] [" + branchName + "] stage";
    int branchStageIndex = fullConsoleLog.lastIndexOf(branchStageMarker, startIndex);
    if (branchStageIndex != -1) {
        startIndex = branchStageIndex;
    }
    
    // 查找 Stage 结束标记：[Pipeline] [branchName] // stage
    String stageEndMarker = "[Pipeline] [" + branchName + "] // stage";
    int endIndex = fullConsoleLog.indexOf(stageEndMarker, startIndex);
    
    if (endIndex == -1) {
        // 尝试查找 branch 结束标记：[Pipeline] [branchName] }
        String branchEndMarker = "[Pipeline] [" + branchName + "] }";
        endIndex = fullConsoleLog.indexOf(branchEndMarker, startIndex);
        
        if (endIndex == -1) {
            return fullConsoleLog.substring(startIndex);
        }
    }
    
    // 包含结束标记
    endIndex = fullConsoleLog.indexOf("\n", endIndex);
    if (endIndex == -1) {
        endIndex = fullConsoleLog.length();
    }
    
    String stageLog = fullConsoleLog.substring(startIndex, endIndex);
    logger.info("Extracted parallel stage log for '{}' (branch: {}), length: {}", stageName, branchName, stageLog.length());
    return stageLog;
}
```

## 提取逻辑

### 串行 Stage
1. 查找 `[Pipeline] { (stageName)`
2. 向前查找 `[Pipeline] stage`
3. 向后查找 `[Pipeline] // stage`
4. 提取这两个标记之间的内容

### 并行 Stage
1. 使用正则表达式查找 `[Pipeline] [branchName] { (stageName)`
2. 提取 `branchName`
3. 向前查找 `[Pipeline] [branchName] stage`
4. 向后查找 `[Pipeline] [branchName] // stage`
5. 提取这两个标记之间的内容

## 示例

### 串行 Stage: gemini-pa-bs-parent

**输入：** stageName = "gemini-pa-bs-parent"

**匹配：**
```
[Pipeline] stage
[Pipeline] { (gemini-pa-bs-parent)
Starting building: gemini ? Manual-Build ? ... #809
[Pipeline] }
[Pipeline] // stage
```

**提取的 Build ID：** #809

### 并行 Stage: pa-bs

**输入：** stageName = "pa-bs"

**匹配：**
```
[Pipeline] [firstBranch] stage
[Pipeline] [firstBranch] { (pa-bs)
[firstBranch] Starting building: gemini ? Manual-Build ? ... #578
[Pipeline] [firstBranch] }
[Pipeline] [firstBranch] // stage
```

**提取的 Build ID：** #578

## 正则表达式说明

```java
String pattern = "\\[Pipeline\\] \\[([^\\]]+)\\] \\{ \\(" + Pattern.quote(stageName) + "\\)";
```

**解释：**
- `\\[Pipeline\\]` - 匹配 "[Pipeline]"
- ` ` - 空格
- `\\[` - 匹配 "["
- `([^\\]]+)` - 捕获组：匹配一个或多个非 "]" 字符（branch 名称）
- `\\]` - 匹配 "]"
- ` \\{ \\(` - 匹配 " { ("
- `Pattern.quote(stageName)` - 转义 stageName 中的特殊字符
- `\\)` - 匹配 ")"

**示例匹配：**
- `[Pipeline] [firstBranch] { (pa-bs)` → branchName = "firstBranch"
- `[Pipeline] [secondBranch] { (claim-bs)` → branchName = "secondBranch"

## 优点

✅ **支持串行 Stage** - 原有功能保持不变  
✅ **支持并行 Stage** - 新增并行 Stage 支持  
✅ **自动识别** - 自动尝试两种模式  
✅ **提取 Branch 名称** - 从日志中提取 branch 名称用于精确匹配  
✅ **日志记录** - 记录提取的 Stage 类型和 branch 名称  

## 编译状态

✅ 编译成功 (mvn compile)

## 测试步骤

1. 关闭正在运行的应用程序
2. 运行 `mvn clean package` 重新打包
3. 启动应用程序
4. 打开 Jenkins Browser
5. 双击某个包含并行 Stage 的 Build
6. 点击并行 Stage（如 pa-bs, claim-bs 等）
7. 观察是否正确提取日志和 Build ID

## 预期结果

- ✅ 串行 Stage 正常工作（如 gemini-pa-bs-parent）
- ✅ 并行 Stage 正常工作（如 pa-bs, claim-bs）
- ✅ 正确提取 Build ID（如 #578, #579）
- ✅ Console Log 显示正确的 Stage 日志
- ✅ 日志中显示 "Found parallel stage 'pa-bs' in branch 'firstBranch'"

## 相关文件

- `src/main/java/com/gitviewer/JenkinsApiClient.java` - 更新了日志提取逻辑
- `STAGE_LOG_修复完成.md` - 之前的修复文档
