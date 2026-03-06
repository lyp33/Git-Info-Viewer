# Design Document: Sequential Group Build Queue

## Overview

本功能为 `BuildPackageDialog` 新增顺序化 Group 打包队列能力。当用户勾选的应用跨越多个 `FavoriteGroup` 时，系统不再一次性并发触发所有应用的打包，而是按 group name 字母升序依次触发每个 group，等待当前 group 全部 build 成功后再触发下一个。

整个队列状态通过对话框右侧新增的 `PendingDeployPanel` 实时展示，并持久化到 `~/.gitviewer/pending_build_queue.json`，支持应用重启后恢复监控。

设计遵循最小侵入原则：现有 `submitBuildRequest()` 方法完全不变，新逻辑仅在 `handleBuildPackage()` 入口处通过条件分叉引入。

## Architecture

### 整体结构

```
BuildPackageDialog (已有，扩展)
├── 现有左侧面板 (不变)
├── 现有中间按钮列 (不变)
├── 现有右侧 Favorited Applications 面板 (不变)
└── 新增 PendingDeployPanel (追加到最右侧)

新增类：
├── QueueEntry.java          - 队列条目数据模型
├── BuildQueue.java          - 队列执行引擎
├── PendingDeployPanel.java  - 队列状态展示面板
└── QueuePersistence.java    - JSON 持久化工具
```

### 分叉逻辑（handleBuildPackage 入口）

```
handleBuildPackage()
  └─ validateBuildConfiguration()  [不变]
  └─ 判断是否队列模式：
       selectedGroups.size() >= 2 ?
         YES → showQueueConfirmDialog() → BuildQueue.start()
         NO  → showConfirmationDialog() → submitBuildRequest()  [不变]
```

### 队列执行流程

```
BuildQueue.start()
  └─ 写入 Queue_Persistence_File
  └─ executeNext()
       └─ 取第一个 Pending QueueEntry
       └─ 调用 submitMultiBuild()
       └─ 标记为 Building，启动 javax.swing.Timer (10s)
       └─ Timer 触发 → getBuildResultByPlan()
            ALL SUCCESS → 标记 Success → executeNext()
            ANY FAILURE → 标记 Failed → stopQueue()
```

## Components and Interfaces

### 1. QueueEntry（数据模型）

```java
public class QueueEntry {
    private String groupName;          // FavoriteGroup name，"Ungrouped" 为虚拟组
    private List<String> appNames;     // 该 group 下被勾选的应用
    private String branch;
    private String version;            // planCode / versionCode
    private String tenant;
    private QueueStatus status;        // PENDING / BUILDING / SUCCESS / FAILED / CANCELLED
    private String triggeredAt;        // ISO 8601，API 触发时间
    // 运行时状态（不持久化）
    private Map<String, String> appBuildStatuses; // appName -> buildStatus
}

public enum QueueStatus { PENDING, BUILDING, SUCCESS, FAILED, CANCELLED }
```

### 2. BuildQueue（队列执行引擎）

核心职责：顺序执行 QueueEntry，管理轮询 Timer，通知 UI 更新。

```java
public class BuildQueue {
    // 构造
    BuildQueue(List<QueueEntry> entries, PortalApiClient apiClient,
               String token, String tenant, BuildQueueListener listener)

    // 控制
    void start()           // 开始执行（从第一个 PENDING 开始）
    void cancel()          // 取消：停止后续，Pending → Cancelled
    void resumePolling()   // 恢复监控（从 Building 状态恢复轮询）
    void pausePolling()    // 暂停自动轮询
    void resumeAutoPolling() // 恢复自动轮询
    void manualRefresh()   // 手动触发一次查询

    // 状态
    boolean isRunning()
    List<QueueEntry> getEntries()
}

// 回调接口（在 EDT 上调用）
public interface BuildQueueListener {
    void onEntryStatusChanged(QueueEntry entry);
    void onQueueCompleted(boolean allSuccess);
    void onQueueFailed(QueueEntry failedEntry, String failedApp);
    void onPollingError(String errorMessage);
}
```

### 3. PendingDeployPanel（UI 面板）

```java
public class PendingDeployPanel extends JPanel {
    PendingDeployPanel(BuildQueue queue)  // 绑定队列

    void loadEntries(List<QueueEntry> entries)  // 初始加载（含持久化恢复）
    void refreshEntry(QueueEntry entry)         // 刷新单条目显示
    void clearEntries()                         // 清空面板
}
```

面板布局：
```
┌─────────────────────────────────────┐
│ Pending Deploy Queue                │
│ [✓] Auto Polling    [Refresh]       │
│ [error banner - 仅在出错时显示]      │
├─────────────────────────────────────┤
│ ● Group-A (3 apps) [Building ...]   │
│   app1: SUCCESS                     │
│   app2: Building                    │
│   app3: Pending                     │
├─────────────────────────────────────┤
│ ○ Group-B (2 apps) [Pending]        │
│ ○ Ungrouped (1 app) [Pending]       │
│                          [×] [×]    │
└─────────────────────────────────────┘
```

### 4. QueuePersistence（持久化工具）

```java
public class QueuePersistence {
    static final String FILE_PATH =
        System.getProperty("user.home") + "/.gitviewer/pending_build_queue.json";

    static void save(List<QueueEntry> entries)   // 覆盖写入
    static List<QueueEntry> load()               // 读取，文件不存在返回空列表
    static void delete()                         // 删除文件
    static boolean hasUnfinished()               // 是否存在 Pending/Building 条目
}
```

JSON 格式：
```json
[
  {
    "groupName": "Group-A",
    "appNames": ["app1", "app2"],
    "branch": "master",
    "version": "master_20250101120000",
    "tenant": "tenant01",
    "status": "BUILDING",
    "triggeredAt": "2025-01-01T12:00:00Z"
  }
]
```

### 5. BuildPackageDialog 修改点

仅在以下位置做最小修改：

| 修改点 | 内容 |
|--------|------|
| `setSize(900, 750)` | 改为 `setSize(1300, 750)` |
| `initializeUI()` | 在 `createApplicationSection()` 后追加 `PendingDeployPanel` |
| `handleBuildPackage()` | 在验证通过后，判断是否队列模式并分叉 |
| `createButtonPanel()` | 追加 `cancelQueueButton`（初始隐藏） |
| `dispose()` | 增加队列运行中的关闭确认逻辑 |
| 构造函数 | 加载持久化文件，若有未完成条目则提示恢复 |

新增辅助方法（私有）：
- `getSelectedGroupedApps()` → `Map<String, List<String>>` 按 group 分组的选中应用
- `isQueueMode()` → `boolean` 判断是否需要进入队列模式
- `startBuildQueue(Map<String, List<String>> groupedApps)` → 创建并启动 BuildQueue
- `showQueueConfirmDialog(List<QueueEntry> entries)` → 队列确认对话框

## Data Models

### QueueEntry 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `groupName` | String | FavoriteGroup 名称；虚拟组固定为 `"Ungrouped"` |
| `appNames` | List\<String\> | 该 group 下被勾选的应用名称列表 |
| `branch` | String | 打包分支 |
| `version` | String | planCode / versionCode，用于轮询时匹配 |
| `tenant` | String | 租户代码 |
| `status` | QueueStatus | 当前状态枚举 |
| `triggeredAt` | String | ISO 8601 格式的 API 触发时间；PENDING 时为空 |
| `appBuildStatuses` | Map\<String,String\> | 运行时各应用 build_status，不持久化 |

### 队列模式判断逻辑

```java
private boolean isQueueMode() {
    // 统计选中应用所属的 FavoriteGroup 数量
    Set<String> involvedGroups = new HashSet<>();
    for (JCheckBox cb : favoritedAppCheckboxes) {
        if (!cb.isSelected()) continue;
        String appName = cb.getText();
        for (FavoriteGroup g : favoriteGroups) {
            if (g.containsApp(appName)) {
                involvedGroups.add(g.getName());
                break;
            }
        }
    }
    return involvedGroups.size() >= 2;
}
```

### 轮询成功判断逻辑

```java
// 对于某个 QueueEntry，判断是否全部成功
boolean allSuccess = entry.getAppNames().stream().allMatch(appName ->
    buildResults.stream().anyMatch(r ->
        r.getAppName().equals(appName) &&
        "SUCCESS".equals(r.getBuildStatus()) &&
        entry.getVersion().equals(r.getVersion())
    )
);

// 判断是否有失败
boolean anyFailed = entry.getAppNames().stream().anyMatch(appName ->
    buildResults.stream().anyMatch(r ->
        r.getAppName().equals(appName) &&
        ("FAILURE".equals(r.getBuildStatus()) || "ABORTED".equals(r.getBuildStatus()))
    )
);
```
