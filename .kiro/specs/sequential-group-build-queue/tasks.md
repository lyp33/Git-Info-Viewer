# Implementation Plan: Sequential Group Build Queue

## Overview

按实现顺序分四个阶段：数据模型 → 核心队列引擎 → UI 面板 → 集成到 BuildPackageDialog。
所有新增代码位于 `src/main/java/com/gitviewer/` 包下，不修改 AppSettings.java、PortalApiClient.java 及其他现有文件。

## Tasks

- [x] 1. 创建 QueueEntry 数据模型
  - [x] 1.1 新建 `QueueEntry.java`，包含所有持久化字段和运行时字段
    - 字段：groupName、appNames、branch、version、tenant、status（QueueStatus 枚举）、triggeredAt
    - 运行时字段（不持久化）：`Map<String, String> appBuildStatuses`
    - 提供标准 getter/setter，以及 `QueueStatus` 枚举（PENDING / BUILDING / SUCCESS / FAILED / CANCELLED）
    - _Requirements: 4.2_

  - [ ]* 1.2 为 QueueEntry 编写单元测试
    - 验证枚举值、字段默认值、getter/setter 正确性
    - _Requirements: 4.2_

- [x] 2. 实现 QueuePersistence 持久化工具
  - [x] 2.1 新建 `QueuePersistence.java`，实现 JSON 读写
    - 文件路径：`~/.gitviewer/pending_build_queue.json`
    - 顶层 JSON 格式：`{ "pollingIntervalSeconds": 10, "entries": [...] }`
    - 实现 `save(List<QueueEntry> entries, int pollingIntervalSeconds)`、`load()` 返回 `QueuePersistence.Data`（含 entries 和 pollingIntervalSeconds）、`delete()`、`hasUnfinished()`
    - 使用 org.json 或手写 JSON 序列化（项目已有依赖），目录不存在时自动创建
    - _Requirements: 4.1, 4.2, 4.5_

  - [ ]* 2.2 为 QueuePersistence 编写属性测试：round-trip 一致性
    - **Property 1: 任意合法 QueueEntry 列表经 save 后 load，entries 内容与原始数据完全一致**
    - **Validates: Requirements 4.1, 4.2**

  - [ ]* 2.3 为 QueuePersistence 编写单元测试
    - 测试文件不存在时 load() 返回空列表、pollingIntervalSeconds 默认 10
    - 测试 hasUnfinished() 在不同状态组合下的返回值
    - _Requirements: 4.1, 4.5_

- [x] 3. 实现 BuildQueueListener 接口与 BuildQueue 队列引擎
  - [x] 3.1 在 `BuildQueue.java` 中定义 `BuildQueueListener` 内部接口
    - 方法：`onEntryStatusChanged(QueueEntry)`、`onQueueCompleted(boolean allSuccess)`、`onQueueFailed(QueueEntry, String failedApp)`、`onPollingError(String)`
    - 所有回调须在 EDT（Event Dispatch Thread）上调用
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 3.6_

  - [x] 3.2 实现 BuildQueue 核心执行逻辑
    - 构造函数接收 `List<QueueEntry>`、`PortalApiClient`、`token`、`tenant`、`BuildQueueListener`
    - `start()`：从第一个 PENDING 条目开始，调用 `executeNext()`
    - `executeNext()`：调用 `submitMultiBuild()`，标记为 BUILDING，启动 `javax.swing.Timer`
    - Timer 触发时调用 `getBuildResultByPlan(planTitle=entry.getVersion(), limit=50)`
    - 全部 SUCCESS（version 匹配）→ 标记 Success → `executeNext()`；任意 FAILURE/ABORTED → 标记 Failed → `stopQueue()`
    - 每次状态变更后调用 `QueuePersistence.save()`
    - _Requirements: 1.3, 3.1, 3.2, 3.3, 4.1_

  - [x] 3.3 实现 BuildQueue 控制方法
    - `cancel()`：停止 Timer，将所有 PENDING 条目标记为 CANCELLED，持久化，不中断当前 BUILDING 条目
    - `pausePolling()` / `resumeAutoPolling()`：暂停/恢复 Timer
    - `manualRefresh()`：立即触发一次轮询查询，不受 Auto Polling 开关影响
    - `resumePolling()`：从第一个 BUILDING 条目恢复轮询（用于持久化恢复场景）
    - `isRunning()` / `getEntries()`
    - _Requirements: 5.3, 5.4, 6.2, 6.3, 6.4_

  - [ ]* 3.4 为 BuildQueue 轮询判断逻辑编写属性测试
    - **Property 2: 当所有 appNames 均在 buildResults 中有 version 匹配且 status=SUCCESS 的记录时，allSuccess 为 true**
    - **Validates: Requirements 3.2**
    - **Property 3: 只要有任意 app 出现 FAILURE 或 ABORTED 记录，anyFailed 为 true**
    - **Validates: Requirements 3.3**

  - [ ]* 3.5 为 BuildQueue 编写单元测试
    - 测试 cancel() 后 PENDING 条目变为 CANCELLED、BUILDING 条目不受影响
    - 测试 isRunning() 在各状态下的返回值
    - _Requirements: 5.3, 5.4_

- [-] 4. 实现 PendingDeployPanel UI 面板
  - [x] 4.1 新建 `PendingDeployPanel.java`，实现面板基础布局
    - 顶部控制栏：标题 "Pending Deploy Queue"、"Auto Polling" checkbox（默认勾选）、轮询间隔输入框（默认 10，最小 5）、"Refresh" 超链接按钮
    - 错误横幅区域（默认隐藏，出错时显示）
    - 中部滚动列表区域，空时显示 "No pending builds"
    - 构造函数接收 `BuildQueue`（可为 null，初始无队列时）
    - _Requirements: 2.1, 2.5, 6.1, 6.5_

  - [x] 4.2 实现 PendingDeployPanel 条目渲染
    - 每个 QueueEntry 渲染为一行：group name、应用数量、状态标签、branch、version
    - BUILDING 状态显示动态省略号动画（使用 javax.swing.Timer 每 500ms 切换）
    - 每个条目右侧提供 [×] 删除按钮，点击后从列表移除并调用 `QueuePersistence.save()`
    - _Requirements: 2.2, 2.3, 2.4, 5.6_

  - [x] 4.3 实现 PendingDeployPanel 与 BuildQueue 的联动
    - `loadEntries(List<QueueEntry>)`：初始加载条目列表
    - `refreshEntry(QueueEntry)`：刷新单条目显示（由 BuildQueueListener 回调触发）
    - `clearEntries()`：清空面板
    - Auto Polling checkbox 变更时调用 `queue.pausePolling()` / `queue.resumeAutoPolling()`
    - 轮询间隔输入框变更时更新 queue 的 Timer 间隔并持久化
    - Refresh 按钮点击时调用 `queue.manualRefresh()`
    - _Requirements: 2.2, 6.1, 6.2, 6.3, 6.4_

- [x] 5. Checkpoint - 确保新增四个类可编译通过
  - 确保所有新增类无编译错误，ask the user if questions arise.

- [x] 6. 集成到 BuildPackageDialog
  - [x] 6.1 修改 `setSize`：将 `setSize(900, 750)` 改为 `setSize(1300, 750)`
    - _Requirements: 2.1_

  - [x] 6.2 修改 `initializeUI()`：在 `createApplicationSection()` 调用后追加 `PendingDeployPanel`
    - 实例化 `pendingDeployPanel` 字段，追加到主布局最右侧
    - _Requirements: 2.1_

  - [x] 6.3 新增私有辅助方法
    - `getSelectedGroupedApps()` → `Map<String, List<String>>`：按 FavoriteGroup 分组返回选中应用，Ungrouped 应用归入 key `"Ungrouped"`
    - `isQueueMode()` → `boolean`：统计涉及的 FavoriteGroup 数量 >= 2 则返回 true
    - `startBuildQueue(Map<String, List<String>> groupedApps)`：按 group name 字母升序创建 QueueEntry 列表，Ungrouped 追加末尾，创建 BuildQueue 并调用 start()
    - `showQueueConfirmDialog(List<QueueEntry> entries)` → `boolean`：弹出确认对话框，列出 group 名称及应用数量
    - _Requirements: 1.1, 1.3, 1.5, 1.6_

  - [x] 6.4 修改 `handleBuildPackage()`：在验证通过后加入队列模式分叉
    - `isQueueMode()` 为 true → `showQueueConfirmDialog()` → `startBuildQueue()`
    - `isQueueMode()` 为 false → 原有 `showConfirmationDialog()` → `submitBuildRequest()`（不变）
    - _Requirements: 1.1, 1.2, 1.4_

  - [x] 6.5 修改 `createButtonPanel()`：追加 `cancelQueueButton`（初始隐藏）
    - 队列运行时显示并启用，点击调用 `buildQueue.cancel()`
    - 队列结束后隐藏，同时恢复 Build Package 按钮可用
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 6.6 修改 `dispose()`：增加队列运行中的关闭确认
    - 若 `buildQueue != null && buildQueue.isRunning()`，弹出确认对话框
    - 用户确认关闭后正常 dispose，状态已持久化
    - _Requirements: 5.5_

  - [x] 6.7 修改构造函数：加载持久化文件并提示恢复
    - 构造函数末尾调用 `QueuePersistence.load()`
    - 若存在 PENDING 或 BUILDING 条目，弹出提示询问是否恢复监控
    - 用户确认后调用 `buildQueue.resumePolling()` 并在 PendingDeployPanel 中展示条目
    - _Requirements: 4.3, 4.4, 2.6_

- [x] 7. Final Checkpoint - 确保所有测试通过
  - 确保所有测试通过，ask the user if questions arise.

## Notes

- 标有 `*` 的子任务为可选测试任务，可跳过以加快 MVP 交付
- `getBuildResultByPlan` 的 `planTitle` 参数传入 `QueueEntry.version`（即 versionCode/planCode）
- 轮询判断需同时满足：version 字段匹配 + buildStatus 判断
- QueuePersistence JSON 顶层为对象格式：`{ "pollingIntervalSeconds": 10, "entries": [...] }`
- 不修改 AppSettings.java、PortalApiClient.java 及其他现有文件
