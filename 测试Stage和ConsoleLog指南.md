# 测试 Stage View 和 Console Log 指南

## ✅ 新增功能

MockJenkinsServer 现在支持完整的 Job 详情页面数据：

1. ✅ **Build History** - 构建历史列表
2. ✅ **Stage View (Module List)** - Pipeline 阶段/模块列表
3. ✅ **Console Log** - 完整的构建日志
4. ✅ **Stage Log** - 每个阶段的详细日志

## 📊 模拟数据说明

### Stage/Module 列表

MockJenkinsServer 提供 5 个模拟的 Pipeline Stages：

| ID | Module Name | Status | Duration |
|----|-------------|--------|----------|
| 6  | gemini-pa-bs-parent | SUCCESS | 39s |
| 11 | bff-parent | SUCCESS | 55s |
| 16 | common-bff | SUCCESS | 2m 10s |
| 39 | pa-bs | SUCCESS | 2m 34s |
| 41 | claim-bs | SUCCESS | 2m 39s |

### Console Log

提供完整的构建日志，包括：
- 构建开始信息
- 模块列表
- 编译阶段日志
- 测试执行日志
- 部署阶段日志
- 构建完成信息

### Stage Log

每个 Stage 都有独立的详细日志，包括：
- 编译过程
- 测试执行
- 测试结果
- 持续时间

## 🚀 测试步骤

### 第 1 步：启动 Mock Server

```bash
start-mock-jenkins.bat
```

看到：
```
========================================
Mock Jenkins Server Started!
URL: http://localhost:8888
========================================

⚠️  注意：所有请求将延迟 10 秒返回
   这样可以看到 Loading 对话框和进度条
```

### 第 2 步：启动应用并配置

```bash
java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
```

配置连接：
- Jenkins URL: `http://localhost:8888`
- Username: `test`
- API Token: `test123`

### 第 3 步：测试 Job 详情页面

#### 测试 1：查看 Build History
1. 展开 `gemini → Manual-Build`
2. 点击 `all-in-one-auto-CI`
3. ✅ 看到 Loading 对话框（10 秒）
4. ✅ 右侧显示 5 次构建历史
5. ✅ 每次构建显示：
   - 编号（#1, #2, #3, #4, #5）
   - 状态（SUCCESS/FAILURE）
   - 时间戳
   - 触发用户（testuser）
   - 参数（BRANCH: master）

#### 测试 2：查看 Stage View（Module List）
1. 选择任意构建（如 #243）
2. ✅ 看到 Loading 对话框（10 秒）
3. ✅ 下方显示 "Stage View" 标签页
4. ✅ 左侧 "Module List" 显示 5 个模块：
   ```
   ● gemini-pa-bs-parent [ID: 6] - 1m 39s - SUCCESS
   ● bff-parent [ID: 11] - 1m 55s - SUCCESS
   ● common-bff [ID: 16] - 2m 10s - SUCCESS
   ● pa-bs [ID: 39] - 2m 54s - SUCCESS
   ● claim-bs [ID: 41] - 2m 59s - SUCCESS
   ```

#### 测试 3：查看 Console Log
1. 在 Stage View 下方
2. ✅ 看到 "Console Log" 区域
3. ✅ 显示完整的构建日志：
   ```
   [13:52:25.508] ===== Build Started =====
   [13:52:25.508] Build #243 - SUCCESS
   [13:52:25.508] Triggered by: dttl.kthoo
   [13:52:25.508] Parameters: [versions: 24.08_thailife_devsdk_v0.056]
   ...
   [13:54:09.789] All modules built successfully!
   ```

#### 测试 4：查看 Stage 详细日志
1. 在 Module List 中选择一个模块（如 `bff-parent`）
2. 双击该模块
3. ✅ 看到 Loading 对话框（10 秒）
4. ✅ 弹出 "Stage Log" 对话框
5. ✅ 显示该模块的详细日志：
   ```
   [13:52:32.678] ===== Stage: bff-parent =====
   [13:52:33.901] Starting compilation...
   [13:52:35.901] [INFO] Building bff-parent 2.1.0
   [13:52:36.234] [INFO] Compiling 67 source files to target/classes
   [13:52:39.567] [INFO] Compilation successful
   [13:52:41.567] [INFO] BUILD SUCCESS
   [13:52:41.890] Duration: 55s
   ```

## 🔍 Mock Server 日志

在 Mock Server 窗口中，你会看到：

```
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

## 📊 支持的 API 端点

MockJenkinsServer 现在支持以下端点：

1. **Job 信息**
   - `/job/{path}/api/json` - Job 详情和构建列表

2. **Stage 信息**
   - `/job/{path}/{buildNumber}/wfapi/describe` - Pipeline Stages 列表

3. **Console Log**
   - `/job/{path}/{buildNumber}/consoleText` - 完整构建日志

4. **Stage Log**
   - `/job/{path}/{buildNumber}/execution/node/{stageId}/wfapi/log` - 单个 Stage 日志

## ✅ 验证清单

测试时请确认：

- [ ] Build History 正确显示（5 次构建）
- [ ] 每次构建显示状态、时间、用户、参数
- [ ] Stage View 显示 5 个模块
- [ ] 每个模块显示名称、ID、持续时间、状态
- [ ] Console Log 显示完整日志
- [ ] 双击模块弹出 Stage Log 对话框
- [ ] Stage Log 显示该模块的详细日志
- [ ] 所有请求都有 10 秒延迟
- [ ] Loading 对话框正确显示

## 🎯 测试重点

### 重点 1：Stage View 数据完整性
- ✅ 5 个模块都显示
- ✅ 模块名称正确
- ✅ 状态图标正确（绿色圆点 = SUCCESS）
- ✅ 持续时间格式正确（如 "1m 39s"）

### 重点 2：Console Log 可读性
- ✅ 日志格式清晰
- ✅ 时间戳正确
- ✅ 包含所有阶段信息
- ✅ 可以滚动查看

### 重点 3：Stage Log 对话框
- ✅ 双击模块触发
- ✅ Loading 对话框显示
- ✅ 日志内容正确
- ✅ 对话框可关闭

## 🛑 停止测试

测试完成后：
1. 关闭应用
2. 在 Mock Server 窗口按 `Ctrl+C`

---

**现在可以完整测试 Job 详情页面了！** 🎉

**编译时间**：2026-01-18 13:56:40  
**状态**：BUILD SUCCESS  
**新增功能**：Stage View + Console Log + Stage Log
