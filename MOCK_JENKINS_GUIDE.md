# Mock Jenkins Server 使用指南

## 简介

Mock Jenkins Server 是一个轻量级的 Jenkins 模拟服务器，专门用于本地测试 Git Info Viewer 应用的 Jenkins 功能，无需安装真实的 Jenkins。

## 快速开始

### 1. 编译项目

```bash
mvn clean package
```

### 2. 启动 Mock Server

```bash
start-mock-jenkins.bat
```

或者手动运行：

```bash
java -cp target\git-info-viewer-1.0.0-jar-with-dependencies.jar com.gitviewer.MockJenkinsServer 8888
```

### 3. 配置应用

启动 Git Info Viewer 应用后：

1. 打开 **Tools → Jenkins Browser**
2. 点击 **Settings** 按钮
3. 配置连接信息：
   - **Jenkins URL**: `http://localhost:8888`
   - **Username**: `test`
   - **API Token**: `test123`
4. 点击 **Save**

### 4. 开始测试

现在你可以：
- 浏览 Job 层次结构
- 查看 Job 详情
- 查看构建历史
- 添加收藏
- 测试双击导航功能

## 测试数据结构

Mock Server 提供以下测试数据：

```
gemini/
├── Manual-Build/
│   ├── all-in-one-auto-CI
│   └── backend-deploy
└── Test-Job/
    ├── backend-service
    └── frontend-service
```

## 功能特性

### ✅ 已实现的功能

1. **Job 层次结构**
   - 支持文件夹（Folder）
   - 支持 Job（FreeStyleProject）
   - 多层嵌套结构

2. **Job 详情**
   - Job 名称、描述
   - 构建历史（最近 5 次构建）
   - 构建参数定义

3. **构建信息**
   - 构建编号
   - 构建结果（SUCCESS/FAILURE）
   - 构建时间戳
   - 触发用户
   - 构建参数

4. **API 端点**
   - `/job/{path}/api/json` - Job 信息
   - `/job/{path}/api/json?tree=jobs[...]` - Job 层次结构
   - 支持 Basic Authentication（任意用户名/密码都可以）

### ⚠️ 未实现的功能

以下功能在 Mock Server 中未实现，但不影响基本测试：

- Stage 信息（`/wfapi/describe`）
- Stage 日志（`/execution/node/{id}/wfapi/log`）
- 触发构建（`/build`, `/buildWithParameters`）
- Console 日志

如果需要这些功能，可以扩展 Mock Server。

## 测试场景

### 场景 1：浏览 Job 层次结构

1. 启动 Mock Server
2. 在应用中打开 Jenkins Browser
3. 应该看到 `gemini` 文件夹
4. 展开 `gemini`，看到 `Manual-Build` 和 `Test-Job`
5. 继续展开，看到具体的 Job

### 场景 2：添加和测试收藏

1. 右键点击任意 Job（如 `backend-service`）
2. 选择 "Add to Favorites"
3. 在收藏列表中看到该 Job
4. 双击收藏项
5. 应该看到 "Loading... please wait" 对话框
6. 系统会导航到该 Job 并选中

### 场景 3：查看构建历史

1. 选择任意 Job
2. 在右侧面板查看构建历史
3. 应该看到 5 次构建记录
4. 包含构建编号、结果、时间、触发用户等信息

### 场景 4：查看构建参数

1. 选择任意 Job
2. 点击 "Build with Parameters" 按钮
3. 应该看到参数对话框
4. 包含 BRANCH（字符串）和 ENVIRONMENT（选择）参数

## 故障排查

### 问题 1：端口被占用

**错误信息**：`Address already in use`

**解决方案**：
1. 更改端口号：
   ```bash
   java -cp target\git-info-viewer-1.0.0-jar-with-dependencies.jar com.gitviewer.MockJenkinsServer 9999
   ```
2. 在应用中使用新的 URL：`http://localhost:9999`

### 问题 2：无法连接

**检查清单**：
1. Mock Server 是否正在运行？
2. URL 是否正确？（`http://localhost:8888`）
3. 防火墙是否阻止了连接？

### 问题 3：看不到数据

**检查清单**：
1. 查看 Mock Server 的控制台输出
2. 确认应用发送的请求路径
3. 检查是否有错误日志

## 扩展 Mock Server

如果需要添加更多功能，可以修改 `MockJenkinsServer.java`：

### 添加新的 Job

在 `createFolderResponse()` 方法中添加：

```java
response = createFolderResponse("MyFolder", new String[][]{
    {"my-new-job", "hudson.model.FreeStyleProject"}
});
```

### 添加 Stage 信息

创建新的处理器：

```java
server.createContext("/wfapi/describe", new StageHandler());
```

### 模拟慢速响应

在处理器中添加延迟：

```java
Thread.sleep(2000); // 延迟 2 秒
```

## 与真实 Jenkins 的区别

| 功能 | Mock Server | 真实 Jenkins |
|------|-------------|--------------|
| Job 浏览 | ✅ | ✅ |
| 构建历史 | ✅ | ✅ |
| 构建参数 | ✅ | ✅ |
| 触发构建 | ❌ | ✅ |
| Stage 信息 | ❌ | ✅ |
| Console 日志 | ❌ | ✅ |
| 实时更新 | ❌ | ✅ |
| 权限控制 | ❌ | ✅ |

## 停止服务器

在 Mock Server 的控制台窗口中按 `Ctrl+C`。

## 技术细节

- **实现**: Java HttpServer
- **端口**: 8888（可配置）
- **协议**: HTTP
- **数据格式**: JSON
- **认证**: Basic Auth（任意用户名/密码）

## 下一步

测试完成后：
1. 停止 Mock Server
2. 配置真实的 Jenkins 连接
3. 享受完整功能！

## 相关文档

- `LOCAL_TEST_FAVORITES_GUIDE.md` - 收藏功能测试指南
- `quick-test.md` - 快速测试指南
- `VERSION_VERIFICATION_GUIDE.md` - 版本验证指南
