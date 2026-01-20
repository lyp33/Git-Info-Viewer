# Mock Jenkins Server 完整功能说明

## ✅ 完成状态

MockJenkinsServer 现在提供完整的 Jenkins 模拟功能，支持所有主要的 UI 页面和交互！

### 编译信息
- **编译时间**：2026-01-18 13:56:40
- **状态**：BUILD SUCCESS
- **JAR 文件**：`target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 🎯 支持的功能

### 1. Job 层次结构浏览
- ✅ 文件夹展开/折叠
- ✅ Job 列表显示
- ✅ 多层级导航

**测试数据：**
```
gemini/
├── Manual-Build/
│   ├── all-in-one-auto-CI
│   └── backend-deploy
└── Test-Job/
    ├── backend-service
    └── frontend-service
```

### 2. 收藏功能
- ✅ 添加收藏
- ✅ 双击导航
- ✅ Loading 对话框
- ✅ 不再报 ClassCastException 错误

### 3. Build History（构建历史）
- ✅ 显示 5 次构建记录
- ✅ 构建编号（#1-#5）
- ✅ 构建状态（SUCCESS/FAILURE）
- ✅ 时间戳
- ✅ 触发用户（testuser）
- ✅ 构建参数（BRANCH: master）

### 4. Stage View（模块列表）
- ✅ 显示 5 个 Pipeline Stages
- ✅ 模块名称
- ✅ 模块 ID
- ✅ 执行状态（SUCCESS）
- ✅ 持续时间（如 "1m 39s"）
- ✅ 状态图标（绿色圆点）

**模拟的 Stages：**
| ID | Module Name | Duration | Status |
|----|-------------|----------|--------|
| 6  | gemini-pa-bs-parent | 39s | SUCCESS |
| 11 | bff-parent | 55s | SUCCESS |
| 16 | common-bff | 2m 10s | SUCCESS |
| 39 | pa-bs | 2m 34s | SUCCESS |
| 41 | claim-bs | 2m 39s | SUCCESS |

### 5. Console Log（构建日志）
- ✅ 完整的构建日志
- ✅ 时间戳格式
- ✅ 编译阶段日志
- ✅ 测试执行日志
- ✅ 部署阶段日志
- ✅ 构建完成信息

**日志内容包括：**
- 构建开始信息
- 模块列表
- 每个模块的编译过程
- 测试执行和结果
- 部署信息
- 构建总结

### 6. Stage Log（模块详细日志）
- ✅ 双击模块打开日志对话框
- ✅ 每个模块的独立日志
- ✅ 编译详情
- ✅ 测试详情
- ✅ 持续时间

### 7. Build Parameters（构建参数）
- ✅ 参数对话框
- ✅ 字符串参数（BRANCH）
- ✅ 选择参数（ENVIRONMENT: dev/test/prod）
- ✅ 默认值

### 8. Loading 对话框
- ✅ 所有请求延迟 10 秒
- ✅ 进度条动画
- ✅ 模态对话框
- ✅ 自动关闭

## 🔌 支持的 API 端点

### Job 相关
```
GET /job/{path}/api/json
→ 返回 Job 详情、构建列表、参数定义
```

### Stage 相关
```
GET /job/{path}/{buildNumber}/wfapi/describe
→ 返回 Pipeline Stages 列表（5 个模块）
```

### Console Log
```
GET /job/{path}/{buildNumber}/consoleText
→ 返回完整的构建日志
```

### Stage Log
```
GET /job/{path}/{buildNumber}/execution/node/{stageId}/wfapi/log
→ 返回单个 Stage 的详细日志
```

## 🚀 使用方法

### 启动 Mock Server
```bash
start-mock-jenkins.bat
```

### 启动应用
```bash
java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
```

### 配置连接
- **Jenkins URL**: `http://localhost:8888`
- **Username**: `test`
- **API Token**: `test123`

## 📊 测试场景

### 场景 1：浏览 Job 层次结构
1. 展开 `gemini` 文件夹
2. ✅ Loading 对话框（10 秒）
3. ✅ 显示 2 个子文件夹

### 场景 2：查看 Job 详情
1. 点击 `all-in-one-auto-CI`
2. ✅ Loading 对话框（10 秒）
3. ✅ 显示 5 次构建历史

### 场景 3：查看 Stage View
1. 选择构建 #243
2. ✅ Loading 对话框（10 秒）
3. ✅ 显示 5 个模块
4. ✅ 显示 Console Log

### 场景 4：查看 Stage 详细日志
1. 双击模块 `bff-parent`
2. ✅ Loading 对话框（10 秒）
3. ✅ 弹出 Stage Log 对话框
4. ✅ 显示该模块的详细日志

### 场景 5：测试收藏功能
1. 右键 `backend-service` → Add to Favorites
2. 双击收藏项
3. ✅ Loading 对话框（10 秒）
4. ✅ 正确导航到 Job
5. ✅ 不报错

### 场景 6：查看构建参数
1. 点击 "Build with Parameters"
2. ✅ 显示参数对话框
3. ✅ BRANCH 参数（默认：master）
4. ✅ ENVIRONMENT 参数（dev/test/prod）

## ⚙️ 配置选项

### 修改延迟时间

编辑 `src/main/java/com/gitviewer/MockJenkinsServer.java`：

```java
private static final int DELAY_SECONDS = 10; // 修改这个值
```

然后重新编译：
```bash
mvn clean package
```

### 修改测试数据

在 `MockJenkinsServer.java` 中修改：
- `createFolderResponse()` - 文件夹结构
- `createJobResponse()` - Job 详情和构建历史
- `handleWfApiDescribe()` - Stage 列表
- `generateStageLog()` - Stage 日志内容
- `handleConsoleText()` - Console Log 内容

## 🔍 Mock Server 日志示例

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

Job Request: /job/gemini/job/Manual-Build/job/all-in-one-auto-CI/api/json
  延迟 10 秒后返回...
  返回响应: /job/gemini/job/Manual-Build/job/all-in-one-auto-CI

Job Request: /job/gemini/job/Manual-Build/job/all-in-one-auto-CI/243/wfapi/describe
  延迟 10 秒后返回...
  返回 5 个 stages

Job Request: /job/gemini/job/Manual-Build/job/all-in-one-auto-CI/243/consoleText
  延迟 10 秒后返回...
  返回完整 console log

Job Request: /job/gemini/job/Manual-Build/job/all-in-one-auto-CI/243/execution/node/11/wfapi/log
  延迟 10 秒后返回...
  返回 stage 11 的日志
```

## ✅ 验证清单

完整测试时请确认：

- [ ] Mock Server 启动成功
- [ ] 应用连接成功
- [ ] 文件夹展开显示 Loading 对话框
- [ ] Job 列表正确显示
- [ ] Build History 显示 5 次构建
- [ ] Stage View 显示 5 个模块
- [ ] Console Log 显示完整日志
- [ ] 双击模块弹出 Stage Log
- [ ] 收藏功能正常工作
- [ ] 双击收藏不报错
- [ ] 构建参数对话框正常
- [ ] 所有 Loading 对话框都显示
- [ ] 进度条动画正常
- [ ] 10 秒后自动关闭

## 📚 相关文档

- **快速开始**：`开始测试.txt`
- **Loading 测试**：`测试Loading对话框指南.md`
- **Stage 测试**：`测试Stage和ConsoleLog指南.md`
- **完整配置**：`LOADING_DIALOG_TEST_COMPLETE.md`
- **Mock Server 指南**：`MOCK_JENKINS_GUIDE.md`
- **快速启动**：`QUICK_START_MOCK_JENKINS.md`

## 🎯 测试目标

通过这个完整的 Mock Server，你可以：

1. ✅ **测试所有 UI 功能**
   - 无需真实 Jenkins 服务器
   - 完整的数据支持

2. ✅ **验证 Loading 对话框**
   - 10 秒延迟足够观察
   - 所有异步操作都有 Loading

3. ✅ **测试收藏功能修复**
   - 不再出现 ClassCastException
   - 正确导航到 Job

4. ✅ **验证 Stage View 功能**
   - 模块列表正确显示
   - Console Log 正确显示
   - Stage Log 对话框正常

5. ✅ **测试用户体验**
   - 加载过程流畅
   - 对话框自动关闭
   - 无错误提示

## 🛑 停止测试

测试完成后：
1. 关闭应用窗口
2. 在 Mock Server 窗口按 `Ctrl+C`
3. 或直接关闭 Mock Server 窗口

---

**Mock Server 功能完整！现在可以测试所有功能了！** 🎉

**编译时间**：2026-01-18 13:56:40  
**状态**：BUILD SUCCESS  
**支持功能**：Job 浏览 + Build History + Stage View + Console Log + Stage Log + 收藏 + 参数
