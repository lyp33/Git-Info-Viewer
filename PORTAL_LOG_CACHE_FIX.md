# Portal Log 缓存修复 - 使用子任务日志

## 问题描述

Portal Log 无法找到 Portal API 的 curl 命令，显示错误：
```
Could not find Portal API URL in stage log
```

## 原因分析

通过日志分析发现：

1. **Stage Log** 只包含 Pipeline stage 级别的日志（简短的 stage 执行信息）
2. **Sub-Job Log** 包含子任务的完整控制台日志（包含 curl 命令）
3. Portal API 的 curl 命令实际上在 **Sub-Job Log** 中，而不是在 Stage Log 中

### 原有逻辑问题

```java
// 获取 Stage Log
String stageLog = apiClient.fetchStageLog(...);

// 缓存 Stage Log（错误！curl 命令不在这里）
cachedStageLog = stageLog;

// 获取 Sub-Job Log（包含 curl 命令）
String subJobLog = apiClient.fetchSubJobConsoleLog(stageLog);

// 显示 Sub-Job Log
jenkinsLogTextArea.setText(subJobLog);

// Portal Log 使用 cachedStageLog（找不到 curl 命令！）
```

**问题**：
- Jenkins Log 标签页显示的是 `subJobLog`（包含 curl 命令）
- Portal Log 使用的是 `cachedStageLog`（不包含 curl 命令）
- 导致 Portal Log 无法提取 Portal API URL

## 解决方案

修改缓存逻辑，缓存 **Sub-Job Log** 而不是 Stage Log：

```java
// 获取 Stage Log
String stageLog = apiClient.fetchStageLog(...);

// 获取 Sub-Job Log（包含 curl 命令）
String subJobLog = apiClient.fetchSubJobConsoleLog(stageLog);

// 缓存 Sub-Job Log 供 Portal Log 使用（正确！）
cachedStageLog = (subJobLog != null && !subJobLog.isEmpty()) ? subJobLog : stageLog;

// 显示 Sub-Job Log
jenkinsLogTextArea.setText(subJobLog);

// Portal Log 使用 cachedStageLog（现在包含 curl 命令了！）
```

### 修改内容

**文件**: `src/main/java/com/gitviewer/JenkinsStageLogDialog.java`

**修改前**:
```java
// 保存 Stage Log 供 Portal 使用（但不自动加载）
cachedStageLog = stageLog;

// 尝试从 Stage Log 中提取子作业路径和构建 ID，并获取子作业的完整日志
String subJobLog = apiClient.fetchSubJobConsoleLog(stageLog);

// 如果成功获取子作业日志，返回它；否则返回原始 Stage Log
return subJobLog;
```

**修改后**:
```java
// 尝试从 Stage Log 中提取子作业路径和构建 ID，并获取子作业的完整日志
String subJobLog = apiClient.fetchSubJobConsoleLog(stageLog);

// 缓存子作业日志供 Portal Log 使用（因为 curl 命令在子作业日志中）
// 如果成功获取子作业日志，缓存它；否则缓存原始 Stage Log
cachedStageLog = (subJobLog != null && !subJobLog.isEmpty()) ? subJobLog : stageLog;

// 返回子作业日志用于显示
return subJobLog;
```

## 逻辑流程

1. **加载 Jenkins Log**:
   - 获取 Stage Log（用于提取子任务路径和 Build ID）
   - 使用 Stage Log 提取子任务信息
   - 获取 Sub-Job 的完整控制台日志
   - **缓存 Sub-Job Log**（包含 curl 命令）
   - 在 Jenkins Log 标签页显示 Sub-Job Log

2. **加载 Portal Log**（用户切换到 Portal Log 标签页时）:
   - 使用 `cachedStageLog`（现在是 Sub-Job Log）
   - 从中提取 Portal API URL 和 headers
   - 调用 Portal API 获取 `build_output`
   - 显示 Portal Log

## 测试验证

1. 打开应用程序
2. 进入 Jenkins Browser
3. 双击任意 Stage 打开 Stage Log 对话框
4. 查看 Jenkins Log 标签页（应显示完整的子任务日志）
5. 切换到 Portal Log 标签页
6. 验证能够成功提取 Portal API URL
7. 验证能够成功显示 Portal Log 内容

## 编译和部署

```bash
mvn clean package
```

生成文件: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 相关文件

- `src/main/java/com/gitviewer/JenkinsStageLogDialog.java` - Stage Log 对话框
- `src/main/java/com/gitviewer/JenkinsApiClient.java` - API 客户端（Portal URL 提取）

## 完成时间

2026-01-20
