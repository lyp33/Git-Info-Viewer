# 修复循环错误和 Mock 数据问题

## 已修复的问题

### 1. 循环弹出错误对话框
**修复内容**:
- 移除了 `JenkinsJobDetailsDialog` 的 `windowGainedFocus` 监听器
- 移除了 `loadBuildHistory()` 和 `loadStageView()` 中的错误对话框
- 错误信息现在只显示在 Console Log 区域，不会循环弹出

**编译时间**: 2026-01-18 14:49:05

## 测试步骤

### 1. 确认 Mock Server 正在运行
打开命令行，运行：
```bash
start-mock-jenkins.bat
```

应该看到：
```
========================================
Mock Jenkins Server Started!
URL: http://localhost:8888
========================================
```

### 2. 测试 Mock Server 是否正常工作
在浏览器中访问以下 URL（每个请求会延迟 10 秒）：

1. **测试根路径**:
   ```
   http://localhost:8888/job/gemini/api/json
   ```
   应该返回 gemini 文件夹的子项（Manual-Build 和 Test-Job）

2. **测试 Manual-Build 文件夹**:
   ```
   http://localhost:8888/job/gemini/job/Manual-Build/api/json
   ```
   应该返回 Manual-Build 的子项（all-in-one-auto-CI 和 backend-deploy）

3. **测试 Test-Job 文件夹**:
   ```
   http://localhost:8888/job/gemini/job/Test-Job/api/json
   ```
   应该返回 Test-Job 的子项（backend-service 和 frontend-service）

4. **测试具体 Job**:
   ```
   http://localhost:8888/job/gemini/job/Test-Job/job/backend-service/api/json
   ```
   应该返回 backend-service 的详细信息，包括 builds 数组

### 3. 查看 Mock Server 日志
在 Mock Server 的命令行窗口中，你应该能看到每个请求的日志：
```
Job Request: /job/gemini/job/Test-Job/job/backend-service/api/json
  延迟 10 秒后返回...
  返回响应: /job/gemini/job/Test-Job/job/backend-service
```

### 4. 测试应用
1. 启动应用（使用新编译的 JAR）
2. 打开 Jenkins Job Browser
3. 等待 10 秒加载完成
4. 展开 gemini → Test-Job
5. 双击 "backend-service"
6. 等待 10 秒
7. 查看 Console Log 区域的输出

**预期结果**:
- 如果成功：Console Log 显示 "Successfully loaded X builds"
- 如果失败：Console Log 显示错误信息，但不会弹出对话框

## 调试 Mock 数据问题

如果 Mock 数据还是无法显示，请检查以下几点：

### 1. 检查请求的 URL
在 Console Log 中查找类似这样的日志：
```
Loading build history for job: job/gemini/job/Test-Job/job/backend-service
```

然后在 Mock Server 的日志中查找对应的请求：
```
Job Request: /job/gemini/job/Test-Job/job/backend-service/api/json?tree=builds[...]
```

### 2. 检查 Mock Server 返回的数据
Mock Server 应该返回包含 `builds` 数组的 JSON：
```json
{
  "_class": "hudson.model.FreeStyleProject",
  "name": "backend-service",
  "builds": [
    {
      "number": 1,
      "result": "FAILURE",
      "timestamp": ...,
      "url": "...",
      "actions": [...]
    },
    ...
  ]
}
```

### 3. 常见问题

**问题 1**: Mock Server 返回 404
- **原因**: 路径不匹配
- **解决**: 检查 `Default Job Path` 设置，确保与 Mock Server 的数据结构匹配

**问题 2**: Mock Server 返回 400
- **原因**: 路径格式错误（可能有多余的斜杠）
- **解决**: 已在代码中修复，确保使用最新编译的 JAR

**问题 3**: 数据加载但不显示
- **原因**: JSON 解析失败
- **解决**: 查看 Console Log 中的详细错误信息

## 临时解决方案

如果 Mock Server 还是有问题，可以暂时：

1. **移除 10 秒延迟**:
   修改 `MockJenkinsServer.java` 中的 `DELAY_SECONDS` 为 0：
   ```java
   private static final int DELAY_SECONDS = 0;
   ```

2. **添加更多日志**:
   在 Mock Server 中添加更详细的日志输出，查看具体返回了什么数据

3. **使用真实 Jenkins**:
   如果有可用的 Jenkins 服务器，可以先用真实服务器测试功能是否正常

## 下一步

请按照上述步骤测试，并告诉我：
1. Mock Server 的日志显示了什么？
2. 应用的 Console Log 显示了什么错误？
3. 浏览器访问 Mock Server 的 URL 能否正常返回数据？

这样我可以更准确地定位问题。
