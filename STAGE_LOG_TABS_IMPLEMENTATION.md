# Stage Log 对话框 - 双 Tab 页实现

## 修改时间
2026-01-18

## 功能描述
将 Stage Log 对话框改为双 Tab 页显示：
1. **Jenkins Log** - 显示 Jenkins Console Log
2. **Portal Log** - 显示 Portal API 返回的 build_output 字段

## 已完成的工作

### 1. UI 改造 ✅
- 将单一文本区域改为 `JTabbedPane`
- 创建两个独立的 `JTextArea`：
  - `jenkinsLogTextArea` - Jenkins Log
  - `portalLogTextArea` - Portal Log
- 两个 Tab 都使用黑色背景、白色字体
- 两个 Tab 都支持水平和垂直滚动

### 2. Jenkins Log 加载 ✅
- 保持原有逻辑不变
- 获取 Stage Log 并尝试提取子作业的完整 Console Log
- 在日志前添加 "printing..." 提示

### 3. Portal Log 框架 ✅
- 添加 `loadPortalLog()` 方法
- 添加 `fetchPortalBuildOutput()` 方法到 `JenkinsApiClient`
- 使用 SwingWorker 异步加载
- 解析 JSON 响应并提取 `build_output` 字段

### 4. Refresh 按钮 ✅
- 点击 Refresh 会同时刷新两个 Tab 的内容

## 待完成的工作

### Portal API URL 构建 ⏳
需要实现 `constructPortalApiUrl()` 方法，根据以下信息构建 URL：

**示例 URL**：
```
https://portal.gw.insuremo.com/edBor/a/ops/build/query_one?id=696b58afb5479268788e51f07
```

**需要的信息**：
1. **Portal 基础 URL**: `https://portal.gw.insuremo.com`
2. **路径参数**: 
   - `edBor` - 从哪里获取？
   - `a/ops` - 从哪里获取？
3. **Build ID**: `696b58afb5479268788e51f07` - 从哪里获取？
   - 是否在 Jenkins 构建参数中？
   - 是否需要从其他 API 查询？
   - 是否可以从 jobPath 或 buildNumber 推导？

**当前实现**：
```java
private String constructPortalApiUrl(String jobPath, int buildNumber) {
    // TODO: 需要实现具体逻辑
    // 输入:
    //   - jobPath: job/gemini/job/Manual-Build/job/thailifesdk/...
    //   - buildNumber: 243
    // 输出:
    //   - https://portal.gw.insuremo.com/edBor/a/ops/build/query_one?id=xxx
    
    return null; // 暂时返回 null
}
```

## 修改的文件

### 1. `src/main/java/com/gitviewer/JenkinsStageLogDialog.java`
- 重构为双 Tab 页布局
- 添加 `loadJenkinsLog()` 方法（原 `loadStageLog()`）
- 添加 `loadPortalLog()` 方法
- 两个 Tab 独立加载，互不影响

### 2. `src/main/java/com/gitviewer/JenkinsApiClient.java`
- 添加 `fetchPortalBuildOutput()` 方法
- 添加 `constructPortalApiUrl()` 方法（待实现）
- 解析 JSON 响应并提取 `build_output` 字段

## 编译结果
✅ 编译成功
- JAR 文件位置: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 测试要点
1. ✅ 对话框应该显示两个 Tab：Jenkins Log 和 Portal Log
2. ✅ Jenkins Log Tab 显示 Jenkins Console Log
3. ⏳ Portal Log Tab 显示 Portal API 的 build_output（待 URL 构建完成）
4. ✅ Refresh 按钮同时刷新两个 Tab
5. ✅ 两个 Tab 都有 "printing..." 提示

## 下一步
请提供以下信息以完成 Portal API URL 构建：

1. **Portal 基础 URL** 是固定的还是可配置的？
2. **edBor** 参数从哪里获取？
3. **a/ops** 参数从哪里获取？
4. **Build ID** (`696b58afb5479268788e51f07`) 从哪里获取？
   - 是否在 Jenkins 构建参数中？参数名是什么？
   - 是否需要调用其他 API 查询？
   - 是否有规律可以从 jobPath/buildNumber 推导？

5. **Portal API 认证**：
   - 是否需要认证？
   - 使用什么认证方式？（Bearer Token? Basic Auth?）
   - 认证信息从哪里获取？

## 技术说明

### Tab 页切换
- 使用 `JTabbedPane` 实现
- 两个 Tab 独立加载，不会互相阻塞
- 用户可以在加载过程中切换 Tab

### 异步加载
- 两个 Tab 都使用 `SwingWorker` 异步加载
- 避免 UI 冻结
- 加载失败时显示错误信息，不影响另一个 Tab

### JSON 解析
- 使用 `org.json.JSONObject` 解析响应
- 提取 `build_output` 字段
- 如果字段不存在，显示完整的 JSON 响应供调试
