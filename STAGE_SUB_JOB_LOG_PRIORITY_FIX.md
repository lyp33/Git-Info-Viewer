# Stage Sub-Job Log - Pattern Priority Fix

## 修改时间
2026-01-18

## 问题描述
双击 Stage 后，需要显示子作业的完整 Console Log（类似网页上的显示）。从 Stage Log 中提取作业路径时，需要优先匹配 "Starting building:" 格式。

## 用户提供的示例
```
[firstBranch] Starting building: gemini ? Manual-Build ? thailifesdk ? 24.08_thailife_dev ? CI-Robot ? BS-CI-ROBOT #578
```

## 解决方案

### 1. 更新正则表达式优先级
修改 `JenkinsApiClient.extractJobPathFromStageLog()` 方法，按以下优先级尝试三种模式：

1. **"Starting building: gemini ? xxx #123"** (最高优先级)
   - 这是最常见的格式，包含完整的作业路径和构建 ID
   
2. **"Building gemini ? xxx"** (第二优先级)
   - 备用格式
   
3. **"Scheduling project: gemini ? xxx"** (第三优先级)
   - 另一种备用格式

### 2. 实现细节

```java
public String extractJobPathFromStageLog(String stageLog) {
    // 模式1: Starting building (最常见，优先匹配)
    Pattern pattern1 = Pattern.compile("Starting building:\\s+gemini\\s*\\?\\s*([^#\\n]+?)\\s*#");
    Matcher matcher1 = pattern1.matcher(stageLog);
    if (matcher1.find()) {
        String jobPath = matcher1.group(1).trim();
        logger.info("Extracted job path from 'Starting building': {}", jobPath);
        return jobPath;
    }
    
    // 模式2: Building
    Pattern pattern2 = Pattern.compile("(?:Building)\\s+gemini\\s*\\?\\s*([^\\n#]+?)(?:\\s*#|\\n|$)");
    Matcher matcher2 = pattern2.matcher(stageLog);
    if (matcher2.find()) {
        String jobPath = matcher2.group(1).trim();
        logger.info("Extracted job path from 'Building': {}", jobPath);
        return jobPath;
    }
    
    // 模式3: Scheduling project
    Pattern pattern3 = Pattern.compile("Scheduling project:\\s+gemini\\s*\\?\\s*([^\\n#]+?)(?:\\s*#|\\n|$)");
    Matcher matcher3 = pattern3.matcher(stageLog);
    if (matcher3.find()) {
        String jobPath = matcher3.group(1).trim();
        logger.info("Extracted job path from 'Scheduling project': {}", jobPath);
        return jobPath;
    }
    
    logger.warn("No job path found in stage log");
    return null;
}
```

## 工作流程

1. 用户双击 Stage
2. 获取 Stage Log
3. 使用优先级模式提取作业路径（例如：`Manual-Build ? thailifesdk ? 24.08_thailife_dev ? CI-Robot ? BS-CI-ROBOT`）
4. 提取构建 ID（例如：`578`）
5. 构建 URL：`http://jenkins/job/gemini/job/Manual-Build/job/thailifesdk/job/24.08_thailife_dev/job/CI-Robot/job/BS-CI-ROBOT/578/consoleText`
6. 获取并显示子作业的完整 Console Log

## 修改的文件
- `src/main/java/com/gitviewer/JenkinsApiClient.java`
  - 更新 `extractJobPathFromStageLog()` 方法，按优先级尝试三种模式

## 编译结果
✅ 编译成功
- JAR 文件位置: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 测试建议
1. 双击包含 "Starting building:" 格式的 Stage
2. 验证是否正确提取作业路径和构建 ID
3. 检查控制台日志，确认 URL 构建正确
4. 验证显示的是子作业的完整 Console Log（而不是 Stage 片段）

## 调试日志
代码中包含详细的调试日志：
- 显示尝试的每个模式
- 显示提取的作业路径
- 显示构建的 URL
- 显示获取的日志长度
