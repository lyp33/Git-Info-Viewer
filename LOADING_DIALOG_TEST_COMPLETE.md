# Loading 对话框测试环境配置完成

## ✅ 完成状态

MockJenkinsServer 已配置 **10 秒延迟**，现在可以完整测试 Loading 对话框和进度条！

### 修改内容

1. ✅ **添加延迟常量**
   ```java
   private static final int DELAY_SECONDS = 10;
   ```

2. ✅ **API 请求延迟**
   - 所有 `/api/json` 请求延迟 10 秒
   - 所有 `/job/` 请求延迟 10 秒

3. ✅ **日志输出**
   - 显示延迟信息
   - 显示返回时间

4. ✅ **启动提示**
   - 服务器启动时显示延迟警告
   - 提醒用户可以看到 Loading 对话框

### 编译状态
- ✅ 编译成功
- ✅ 时间：2026-01-18 13:37:39
- ✅ JAR：`target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 🚀 快速开始

### 方法 1：使用测试脚本
```bash
test-loading-dialog.bat
```

### 方法 2：手动启动

**窗口 1 - Mock Server：**
```bash
start-mock-jenkins.bat
```

**窗口 2 - 应用：**
```bash
java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
```

## 📊 测试场景

### 场景 1：展开文件夹
1. 点击展开 `gemini`
2. ✅ 看到 Loading 对话框（10 秒）
3. ✅ 看到进度条动画
4. ✅ 10 秒后自动关闭

### 场景 2：选择 Job
1. 点击 `backend-service`
2. ✅ 看到 Loading 对话框（10 秒）
3. ✅ 看到进度条动画
4. ✅ 10 秒后显示详情

### 场景 3：双击收藏
1. 添加收藏
2. 双击收藏项
3. ✅ 看到 Loading 对话框（10 秒）
4. ✅ 看到进度条动画
5. ✅ 10 秒后导航到 Job
6. ✅ 不再报错

## 🔍 预期效果

### Loading 对话框
```
┌─────────────────────────┐
│       Loading           │
├─────────────────────────┤
│ Loading... please wait  │
│                         │
│ [████████░░░░░░░░░░░]  │ ← 进度条动画
│                         │
└─────────────────────────┘
```

### Mock Server 日志
```
Job Request: /job/gemini/job/Test-Job/job/backend-service/api/json
  延迟 10 秒后返回...
  [等待 10 秒]
  返回响应: /job/gemini/job/Test-Job/job/backend-service
```

## ⚙️ 配置说明

### 当前配置
- **延迟时间**：10 秒
- **适用范围**：所有 API 请求
- **目的**：测试 Loading 对话框

### 修改延迟时间

编辑 `src/main/java/com/gitviewer/MockJenkinsServer.java`：

```java
private static final int DELAY_SECONDS = 5;  // 改为 5 秒
```

然后重新编译：
```bash
mvn clean package
```

### 禁用延迟

将延迟设置为 0：
```java
private static final int DELAY_SECONDS = 0;  // 无延迟
```

## 📚 相关文档

- **测试指南**：`测试Loading对话框指南.md`
- **快速开始**：`测试环境就绪.md`
- **Mock Server 指南**：`MOCK_JENKINS_GUIDE.md`
- **收藏功能修复**：`FAVORITES_NAVIGATION_FIX.md`

## ✅ 验证清单

测试时请确认：

- [ ] Mock Server 启动成功
- [ ] 应用连接到 Mock Server
- [ ] 展开文件夹时显示 Loading 对话框
- [ ] 进度条有动画效果
- [ ] 延迟 10 秒后对话框自动关闭
- [ ] 双击收藏不报错
- [ ] 正确导航到目标 Job
- [ ] Mock Server 日志显示延迟信息

## 🎯 测试目标

通过这个配置，你可以：

1. ✅ **看到 Loading 对话框**
   - 有足够时间观察
   - 确认对话框正确显示

2. ✅ **看到进度条动画**
   - 10 秒足够看清动画
   - 确认进度条工作正常

3. ✅ **验证收藏功能修复**
   - 不再出现 ClassCastException
   - 正确导航到 Job

4. ✅ **测试用户体验**
   - 加载过程流畅
   - 对话框自动关闭
   - 无错误提示

## 🛑 停止测试

测试完成后：
1. 关闭应用窗口
2. 在 Mock Server 窗口按 `Ctrl+C`
3. 或直接关闭 Mock Server 窗口

---

**一切就绪！现在可以完整测试 Loading 对话框了！** 🎉

**编译时间**：2026-01-18 13:37:39  
**状态**：BUILD SUCCESS  
**延迟时间**：10 秒
