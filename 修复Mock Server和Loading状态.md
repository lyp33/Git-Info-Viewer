# 修复 Mock Server 和 Loading 状态

## 修复内容

### 1. 修复 Mock Server 的 400 错误
**问题**: 打开 Job Details 对话框时，循环提示 HTTP 400 错误

**原因**: Mock Server 的路径解析不完整，无法正确处理深层嵌套的 job 路径（如 `/job/gemini/job/Manual-Build/job/backend-service/api/json`）

**修复**:
- 在 `handleApiJson` 方法中添加了对末尾斜杠的处理
- 修复了路径匹配逻辑，支持带斜杠和不带斜杠的路径
- 确保 `createJobResponse` 能正确处理所有深度的 job 路径

### 2. 添加 Loading 状态检查
**问题**: 用户打开 Jenkins Browser 后立即双击收藏的 job，会提示 "Cannot find job"

**原因**: 第一层 job 数据还在加载中，树节点尚未构建完成

**修复**:
- 在 `JenkinsBrowserDialog` 中添加 `volatile boolean isLoading` 字段
- 在 `loadJobHierarchy()` 开始时设置 `isLoading = true`
- 在加载完成（成功或失败）后设置 `isLoading = false`
- 在 `FavoritesPanel.navigateToJob()` 中检查 loading 状态
- 如果正在加载，显示友好提示："Jenkins is loading now, please wait..."

## 编译信息
- 编译时间: 2026-01-18 14:28:44
- JAR 文件: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 测试步骤

### 1. 启动 Mock Jenkins Server
```bash
start-mock-jenkins.bat
```

应该看到：
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
```

### 2. 配置应用
1. 打开应用
2. 点击 "Tenant CI/CD"
3. 点击 "Jenkins Settings"
4. 配置：
   - Jenkins URL: `http://localhost:8888`
   - Username: `test`
   - API Token: `test123`
   - Default Job Path: `job/gemini` （或留空）
5. 点击 "Test Connection" 测试连接（会等待 10 秒）
6. 点击 "Save" 保存

### 3. 测试 Loading 状态检查
1. 点击 "Jenkins Job Browser"
2. **立即**双击收藏列表中的任何 job（在 10 秒加载完成之前）
3. 应该看到提示："Jenkins is loading now, please wait..."
4. 等待 10 秒后，树加载完成
5. 再次双击收藏的 job
6. 应该能正常导航到该 job

### 4. 测试 Job Details 对话框
1. 等待 Jenkins Browser 加载完成（10 秒）
2. 展开 gemini → Manual-Build
3. 双击 "all-in-one-auto-CI" 或 "backend-deploy"
4. 等待 10 秒，应该看到 Job Details 对话框
5. 应该显示：
   - Build History（左侧）：5 个构建记录
   - Stage View（右上）：5 个 Pipeline Stages
   - Console Log（右下）：完整的构建日志
6. **不应该再出现 400 错误**

### 5. 测试 Stage View 和 Console Log
1. 在 Build History 中选择不同的构建
2. Stage View 应该显示 5 个模块：
   - gemini-pa-bs-parent (39s)
   - bff-parent (55s)
   - common-bff (2m10s)
   - pa-bs (2m34s)
   - claim-bs (2m39s)
3. Console Log 应该显示完整的构建日志
4. 双击任意 Stage，应该打开该 Stage 的详细日志

## 预期结果
- ✅ 不再出现 HTTP 400 错误
- ✅ Job Details 对话框能正常打开
- ✅ Build History 正常显示
- ✅ Stage View 正常显示
- ✅ Console Log 正常显示
- ✅ 在 Jenkins 加载期间双击收藏，显示友好提示
- ✅ 加载完成后，收藏导航功能正常

## 注意事项
1. Mock Server 的所有请求都有 10 秒延迟，这是为了测试 Loading 对话框
2. 如果需要更快的响应，可以修改 `MockJenkinsServer.java` 中的 `DELAY_SECONDS` 常量
3. Mock Server 运行在 8888 端口，确保该端口未被占用
4. 测试完成后，按 Ctrl+C 停止 Mock Server
