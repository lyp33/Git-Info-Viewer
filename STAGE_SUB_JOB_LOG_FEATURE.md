# Stage Sub-Job Console Log Feature

## 功能说明

当用户双击 Stage 时，系统会自动：
1. 从 Stage Log 中提取子作业路径（例如：`Manual-Build ? thailifesdk ? 24.08_thailife_dev ? CI-Robot ? BFF-CI-ROBOT`）
2. 从 Stage Log 中提取 Stage Build ID（例如：`#578`）
3. 构建子作业的 URL 并获取该子作业的完整 Console Log
4. 在对话框中显示子作业的 Console Log

## 实现细节

### 1. 提取作业路径 (`extractJobPathFromStageLog`)

从 Stage Log 中提取作业路径，支持以下格式：
- `Building gemini ? Manual-Build ? thailifesdk ? 24.08_thailife_dev ? CI-Robot ? BFF-CI-ROBOT`
- `Scheduling project: gemini ? Manual-Build ? thailifesdk ? ...`

使用正则表达式：`(?:Building|Scheduling project:)\s+gemini\s*\?\s*([^\n#]+?)(?:\s*#|\n|$)`

### 2. 构建子作业 URL (`constructSubJobUrl`)

将 `?` 分隔的路径转换为 Jenkins URL 格式：
- 输入：`Manual-Build ? thailifesdk ? 24.08_thailife_dev ? CI-Robot ? BFF-CI-ROBOT`
- 输出：`/job/gemini/job/Manual-Build/job/thailifesdk/job/24.08_thailife_dev/job/CI-Robot/job/BFF-CI-ROBOT/578/consoleText`

### 3. 获取子作业日志 (`fetchSubJobConsoleLog`)

完整流程：
1. 调用 `extractJobPathFromStageLog()` 提取作业路径
2. 调用 `extractStageBuildId()` 提取构建 ID
3. 调用 `constructSubJobUrl()` 构建 URL
4. 调用 `sendGetRequest()` 获取子作业的完整 Console Log
5. 如果任何步骤失败，返回原始 Stage Log

### 4. 对话框集成 (`JenkinsStageLogDialog`)

修改 `loadStageLog()` 方法：
- 首先获取 Stage Log
- 调用 `fetchSubJobConsoleLog()` 尝试获取子作业日志
- 显示子作业日志（如果成功）或原始 Stage Log（如果失败）

## 使用方法

1. 在 Job Details 对话框中，点击 "View Modules" 按钮
2. 在 Module 列表中双击任意 Stage
3. 系统会自动提取子作业信息并显示完整的 Console Log

## 错误处理

- 如果无法提取作业路径，返回原始 Stage Log
- 如果无法提取 Build ID，返回原始 Stage Log
- 如果 HTTP 请求失败，返回原始 Stage Log
- 所有错误都会记录到日志中，不会中断用户操作

## 日志示例

```
[INFO] Extracted job path from stage log: Manual-Build ? thailifesdk ? 24.08_thailife_dev ? CI-Robot ? BFF-CI-ROBOT
[INFO] Constructed sub-job URL: http://localhost:8888/job/gemini/job/Manual-Build/job/thailifesdk/job/24.08_thailife_dev/job/CI-Robot/job/BFF-CI-ROBOT/578/consoleText
[INFO] Fetching sub-job console log from: http://localhost:8888/job/gemini/...
[INFO] Successfully fetched sub-job console log, length: 12345
```

## 修改的文件

1. **JenkinsApiClient.java**
   - 新增 `extractJobPathFromStageLog()` 方法
   - 新增 `constructSubJobUrl()` 方法
   - 新增 `fetchSubJobConsoleLog()` 方法

2. **JenkinsStageLogDialog.java**
   - 修改 `loadStageLog()` 方法，集成子作业日志获取功能

## 编译状态

✅ 编译成功 (2026-01-18 18:33:26)

## 测试建议

1. 测试串行 Stage 的子作业日志获取
2. 测试并行 Stage 的子作业日志获取
3. 测试无子作业的 Stage（应显示原始 Stage Log）
4. 测试网络错误情况（应优雅降级到原始 Stage Log）
