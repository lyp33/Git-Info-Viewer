# 测试 Loading 对话框和进度条指南

## ✅ 已完成配置

MockJenkinsServer 已配置为**延迟 10 秒**返回所有请求，这样你就能清楚地看到：
- ✅ "Loading... please wait" 对话框
- ✅ 进度条动画
- ✅ 完整的加载过程

## 🚀 测试步骤

### 第 1 步：启动 Mock Jenkins Server

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

⚠️  注意：所有请求将延迟 10 秒返回
   这样可以看到 Loading 对话框和进度条

按 Ctrl+C 停止服务器
========================================
```

**保持这个窗口打开！**

### 第 2 步：启动应用

打开另一个命令行窗口：
```bash
java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
```

### 第 3 步：配置 Jenkins 连接

1. 点击菜单 **Tools → Jenkins Browser**
2. 点击 **Settings** 按钮
3. 输入配置：
   - **Jenkins URL**: `http://localhost:8888`
   - **Username**: `test`
   - **API Token**: `test123`
4. 点击 **Save**

### 第 4 步：测试 Loading 对话框

#### 测试场景 1：展开文件夹（10 秒延迟）
1. 在左侧树中点击展开 `gemini` 文件夹
2. ✅ **立即看到 Loading 对话框**
3. ✅ **看到进度条动画**
4. 等待 10 秒
5. ✅ 对话框自动关闭，显示子项

#### 测试场景 2：选择 Job（10 秒延迟）
1. 展开 `gemini → Test-Job`
2. 点击 `backend-service`
3. ✅ **立即看到 Loading 对话框**
4. ✅ **看到进度条动画**
5. 等待 10 秒
6. ✅ 对话框自动关闭，显示 Job 详情

#### 测试场景 3：收藏功能（10 秒延迟）
1. 右键点击 `backend-service`
2. 选择 **"Add to Favorites"**
3. 在收藏面板中看到该 Job
4. **双击收藏项**
5. ✅ **立即看到 Loading 对话框**
6. ✅ **看到进度条动画**
7. 等待 10 秒
8. ✅ 对话框自动关闭，导航到 Job

## 📊 观察要点

### Loading 对话框应该显示：
- ✅ 标题："Loading"
- ✅ 消息："Loading... please wait"
- ✅ 进度条（不确定模式，持续动画）
- ✅ 模态对话框（阻止其他操作）

### 时间线：
```
0 秒  → 点击操作
0 秒  → Loading 对话框立即显示
0-10秒 → 进度条动画运行
10秒  → Mock Server 返回数据
10秒  → Loading 对话框自动关闭
10秒  → 显示结果
```

## 🔍 Mock Server 日志

在 Mock Server 窗口中，你会看到：

```
Job Request: /job/gemini/job/Test-Job/job/backend-service/api/json
  延迟 10 秒后返回...
  返回响应: /job/gemini/job/Test-Job/job/backend-service
```

这确认了延迟正在工作。

## ⚙️ 调整延迟时间

如果你想修改延迟时间，编辑 `MockJenkinsServer.java`：

```java
private static final int DELAY_SECONDS = 10; // 修改这个值
```

然后重新编译：
```bash
mvn clean package
```

## 🛑 停止测试

测试完成后：
1. 关闭应用
2. 在 Mock Server 窗口按 `Ctrl+C`

## ✅ 验证清单

- [ ] Loading 对话框显示
- [ ] 进度条动画运行
- [ ] 延迟 10 秒后自动关闭
- [ ] 不再出现 ClassCastException 错误
- [ ] 正确导航到目标 Job
- [ ] 构建历史正确显示

## 📝 问题报告

如果遇到问题，请提供：
1. 具体的错误信息
2. 操作步骤
3. Mock Server 窗口的日志
4. Loading 对话框是否显示

---

**现在可以完整测试 Loading 对话框了！** 🎉
