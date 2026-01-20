# Portal Log 详细输出功能完成

## 修改时间
2026-01-19 00:27

## 修改内容

### 问题描述
用户要求在切换到 "Portal Log" tab 时，显示详细的调试信息，包括：
1. 清空 log
2. 输出要访问的 Portal API 完整 URL
3. 输出 HTTP Headers 信息
4. 显示 "Loading..."
5. 输出访问 API 后获取的日志内容

### 实现方案

#### 1. 修改 `JenkinsStageLogDialog.java`

**修改 `loadPortalLogOnDemand()` 方法**：
- 使用 `SwingWorker<PortalLogInfo, String>` 支持渐进式输出
- 使用 `publish()` 和 `process()` 方法逐步显示信息
- 添加内部类 `PortalLogInfo` 存储 API 信息和日志内容

**输出流程**：
```
1. 清空文本区域
2. 提取 Portal API URL 和 Headers
3. 发布 API 请求信息（URL + Headers）
4. 发布 "Loading..." 消息
5. 调用 Portal API
6. 发布 API 响应内容
```

**显示格式**：
```
=== Portal API Request Info ===

URL:
https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=xxx

Headers:
  x-mo-target-tenant: thailife
  x-mo-source-system: jenkins
  ...

Loading...

=== Portal API Response ===

(build_output 内容)
```

#### 2. 修改 `JenkinsApiClient.java`

**添加公开方法**：
- `extractPortalUrlPublic(String stageLog)` - 提取 Portal API URL
- `extractCurlHeadersPublic(String stageLog)` - 提取 HTTP Headers
- `fetchPortalBuildOutputWithInfo(String stageLog, String portalUrl, Map<String, String> headers)` - 使用已提取的信息调用 API

**设计原因**：
- 原有的 `extractPortalUrl()` 和 `extractCurlHeaders()` 是私有方法
- UI 层需要先提取信息显示，再调用 API
- 添加公开方法避免修改原有私有方法的访问权限

### 技术细节

#### SwingWorker 渐进式输出
```java
SwingWorker<PortalLogInfo, String> worker = new SwingWorker<PortalLogInfo, String>() {
    @Override
    protected PortalLogInfo doInBackground() throws Exception {
        // 后台线程：提取信息、调用 API
        publish("API Info...");  // 发布中间结果
        publish("Loading...");
        return result;
    }
    
    @Override
    protected void process(List<String> chunks) {
        // UI 线程：逐步追加到文本区域
        for (String chunk : chunks) {
            portalLogTextArea.append(chunk);
        }
    }
    
    @Override
    protected void done() {
        // UI 线程：追加最终结果
        portalLogTextArea.append(finalResult);
    }
};
```

#### 信息提取流程
1. **URL 提取**：使用正则 `https://portal-gw\.insuremo\.com/[^\s'"\n]+`
2. **Headers 提取**：使用正则 `-H\s+['"]([^:]+):\s*([^'"]+)['"]`
3. **API 调用**：使用 `sendGetRequestWithHeaders()` 发送带自定义 headers 的 GET 请求
4. **JSON 解析**：提取 `build_output` 字段

### 用户体验改进

**之前**：
- 切换到 Portal Log tab 时只显示 "Loading..."
- 看不到正在访问哪个 API
- 看不到使用了哪些 Headers
- 无法调试 API 调用问题

**现在**：
- 清晰显示完整的 API URL
- 显示所有 HTTP Headers
- 显示加载状态
- 便于调试和问题排查

### 测试建议

1. **正常流程**：
   - 双击 Stage 打开对话框
   - 切换到 "Portal Log" tab
   - 验证显示顺序：API Info → Loading → Response

2. **错误处理**：
   - 测试 Stage Log 中没有 Portal API URL 的情况
   - 测试 API 调用失败的情况
   - 验证错误信息正确显示

3. **性能**：
   - 验证 UI 不会卡顿
   - 验证信息逐步显示（不是一次性显示）

## 相关文件

- `src/main/java/com/gitviewer/JenkinsStageLogDialog.java` - UI 对话框
- `src/main/java/com/gitviewer/JenkinsApiClient.java` - API 客户端

## 编译状态

✅ 编译成功
✅ 打包成功

生成的 JAR 文件：
- `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
