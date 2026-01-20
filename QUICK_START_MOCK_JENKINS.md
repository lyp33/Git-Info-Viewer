# Mock Jenkins 快速启动指南

## 3 步开始测试

### 步骤 1：启动 Mock Jenkins Server

```bash
start-mock-jenkins.bat
```

你会看到：
```
========================================
Mock Jenkins Server Started!
URL: http://localhost:8888
========================================

测试数据结构:
  gemini/
    ├── Manual-Build/
    │   ├── all-in-one-auto-CI
    │   └── backend-deploy
    └── Test-Job/
        ├── backend-service
        └── frontend-service

在应用中配置:
  Jenkins URL: http://localhost:8888
  Username: test
  API Token: test123
```

### 步骤 2：配置应用

1. 启动应用：
   ```bash
   java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```

2. 打开 **Tools → Jenkins Browser**

3. 点击 **Settings** 按钮，配置：
   - Jenkins URL: `http://localhost:8888`
   - Username: `test`
   - API Token: `test123`

4. 点击 **Save**

### 步骤 3：开始测试

现在你可以：

✅ **浏览 Job 层次结构**
- 展开 `gemini` 文件夹
- 查看 `Manual-Build` 和 `Test-Job`
- 查看具体的 Job

✅ **测试收藏功能**
- 右键点击 `backend-service`
- 选择 "Add to Favorites"
- 双击收藏项
- 看到 "Loading... please wait" 对话框
- 系统导航到该 Job

✅ **查看构建历史**
- 选择任意 Job
- 查看右侧的构建历史
- 看到 5 次构建记录

✅ **查看构建参数**
- 点击 "Build with Parameters"
- 看到参数对话框

## 测试完成

测试完成后：
1. 在 Mock Server 窗口按 `Ctrl+C` 停止服务器
2. 或者直接关闭窗口

## 详细文档

参见：`MOCK_JENKINS_GUIDE.md`
