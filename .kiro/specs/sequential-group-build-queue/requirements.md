# Requirements Document

## Introduction

为 Build Package 对话框新增顺序化 Group 打包队列功能（Sequential Group Build Queue）。

当用户在 Favorited Applications 中勾选了多个 group 并点击 "Build Package" 后，系统按照 group name 的字母顺序依次触发每个 group 的打包。触发某个 group 的打包 API 后，系统通过轮询等待该 group 内所有子项目的 image 全部 build 成功，再触发下一个 group，以此类推。

整个队列状态通过对话框右侧新增的 "Pending Deploy Queue" 面板实时展示，并持久化到本地文件，以便应用重启后恢复未完成的队列。

**设计原则：最小化对现有功能的影响**
- 现有的单次 Build 流程（`submitBuildRequest`）保持完全不变
- 新功能通过在 `handleBuildPackage` 入口处判断是否启用队列模式来分叉，不修改现有方法
- PendingDeployPanel 作为独立面板追加到对话框右侧，不修改现有布局逻辑
- 对话框宽度从 900 扩展到 1300 以容纳新面板，高度不变

## Glossary

- **BuildPackageDialog**: 现有的打包对话框，用于配置并提交多应用打包请求
- **FavoriteGroup**: 用户在 Favorited Applications 中创建的应用分组，包含分组名称和应用列表
- **Build_Queue**: 顺序化打包队列，由多个 QueueEntry 按 group name 字母顺序排列组成
- **QueueEntry**: 队列中的一个条目，对应一个 FavoriteGroup 的打包任务，包含状态、branch、version 等信息
- **PendingDeployPanel**: Build Package 对话框右侧新增的面板，展示当前 Build_Queue 的状态
- **PortalApiClient**: 现有的 Portal API 客户端，负责提交打包请求和查询构建结果
- **BuildResult**: PortalApiClient 返回的单个应用构建结果，包含 app_name、build_status、version 等字段
- **PlanBuildResult**: PortalApiClient 返回的计划级构建结果，包含多个 BuildResult
- **Queue_Persistence_File**: 本地 JSON 文件，专用于持久化 Build_Queue 状态，路径为用户 home 目录下的 `.gitviewer/pending_build_queue.json`，与现有的 AppSettings 配置文件完全独立，不共享
- **Polling_Interval**: 轮询间隔（秒），可由用户在 PendingDeployPanel 中配置，默认值为 10 秒，最小值为 5 秒
- **Polling_Limit**: 每次轮询查询最近记录数，固定为 50 条
- **Build_Success_Status**: 表示构建成功的 build_status 值，为 `"SUCCESS"`
- **Build_Failed_Status**: 表示构建失败的 build_status 值，为 `"FAILURE"` 或 `"ABORTED"`

---

## Requirements

### Requirement 1: 顺序化 Group 打包队列触发

**User Story:** As a developer, I want to trigger build packages for multiple groups sequentially by group name order, so that I can ensure each group's images are ready before the next group starts.

#### Acceptance Criteria

1. WHEN 用户在 BuildPackageDialog 中勾选的应用分属于至少两个不同的 FavoriteGroup（即选中的 item-level checkbox 跨越了多个 group），并点击 "Build Package" 按钮，THE BuildPackageDialog SHALL 进入队列模式，按照 FavoriteGroup name 的字母升序排列所有涉及的 group，生成对应的 QueueEntry 列表。每个 QueueEntry 只包含该 group 下被勾选的应用。

2. WHEN Build_Queue 被创建，THE BuildPackageDialog SHALL 将 Build_Queue 序列化为 JSON 并写入 Queue_Persistence_File，再开始执行队列。

3. WHEN Build_Queue 开始执行，THE Build_Queue SHALL 仅触发排在第一位的 QueueEntry 对应 group 的打包 API（`submitMultiBuild`），该 group 内所有勾选的子项目在同一次 API 调用中并发打包，但不同时触发其他 group 的打包。

4. WHEN 用户勾选的应用全部属于同一个 FavoriteGroup，或者只勾选了 Ungrouped 区域的应用，或者只勾选了 Unfavorited 区域的应用，THE BuildPackageDialog SHALL 沿用现有的 `submitBuildRequest()` 逻辑，不启用顺序队列，现有行为完全不变。

5. WHEN 队列模式被触发，且用户同时勾选了 Ungrouped 区域的应用，THE Build_Queue SHALL 将这些 Ungrouped 应用作为一个名为 "Ungrouped" 的虚拟 QueueEntry 追加到队列末尾（排在所有 FavoriteGroup 之后）。

6. WHEN 队列模式被触发，THE BuildPackageDialog SHALL 弹出确认对话框，列出所有将要按顺序执行的 group 名称及其包含的应用数量，用户确认后才开始执行队列。

---

### Requirement 2: Pending Deploy 面板展示

**User Story:** As a developer, I want to see the current build queue status in a dedicated panel, so that I can monitor which groups are pending, building, or completed.

#### Acceptance Criteria

1. THE BuildPackageDialog SHALL 在对话框右侧新增一个 PendingDeployPanel，与现有的 Favorited Applications 面板并排显示。

2. WHEN Build_Queue 中存在 QueueEntry，THE PendingDeployPanel SHALL 按照队列顺序展示每个 QueueEntry 的信息，包括：group name、包含的应用数量、当前状态（Pending / Building / Success / Failed）、branch 和 version。

3. WHEN 某个 QueueEntry 的状态为 "Building"，THE PendingDeployPanel SHALL 在该条目旁显示一个动态进度指示器（spinner 或动态省略号）。

4. THE PendingDeployPanel SHALL 不允许用户手动拖拽或修改 QueueEntry 的排列顺序。

5. WHEN Build_Queue 为空，THE PendingDeployPanel SHALL 显示占位文本 "No pending builds"。

6. WHEN 应用启动时 Queue_Persistence_File 存在且包含未完成的 QueueEntry，THE PendingDeployPanel SHALL 在 BuildPackageDialog 打开时自动加载并展示这些条目。

---

### Requirement 3: 轮询等待 Group Build 完成

**User Story:** As a developer, I want the system to poll the build status after triggering a group's build, so that the next group only starts after all apps in the current group have successfully built their images.

#### Acceptance Criteria

1. WHEN 某个 QueueEntry 对应的打包 API 调用成功后，THE Build_Queue SHALL 以用户配置的 Polling_Interval 为间隔，循环调用 `getBuildResultByPlan` API 查询该 group 对应 version 的构建状态，每次查询最近 Polling_Limit（50条）记录。

2. WHEN 轮询结果中，该 QueueEntry 所包含的所有应用均出现 build_status 为 Build_Success_Status（"SUCCESS"）的记录，且对应 version 与 QueueEntry 中记录的 version 一致，THE Build_Queue SHALL 将该 QueueEntry 标记为 "Success" 并触发下一个 QueueEntry 的打包。

3. WHEN 轮询结果中，该 QueueEntry 所包含的任意应用出现 build_status 为 Build_Failed_Status（"FAILURE" 或 "ABORTED"）的记录，THE Build_Queue SHALL 将该 QueueEntry 标记为 "Failed" 并停止整个队列的继续执行。

4. WHEN 某个 QueueEntry 处于轮询等待状态，THE PendingDeployPanel SHALL 实时更新该条目下每个应用的构建状态（显示各应用当前的 build_status）。

5. WHEN Build_Queue 因某个 QueueEntry 失败而停止，THE BuildPackageDialog SHALL 弹出错误提示对话框，说明哪个 group 的哪个应用构建失败。

6. WHEN 所有 QueueEntry 均执行完毕且状态为 "Success"，THE BuildPackageDialog SHALL 弹出成功提示对话框，并清空 Queue_Persistence_File。

---

### Requirement 4: 队列状态持久化与恢复

**User Story:** As a developer, I want the build queue state to be saved locally, so that I can resume monitoring if the application is restarted during a long-running build queue.

#### Acceptance Criteria

1. THE Build_Queue SHALL 在每次 QueueEntry 状态变更时，将完整的 Build_Queue 状态序列化为 JSON 并覆盖写入 Queue_Persistence_File。Queue_Persistence_File 是独立于现有 AppSettings 配置文件的新文件，不得修改现有配置文件的读写逻辑。

2. THE Queue_Persistence_File SHALL 存储每个 QueueEntry 的以下字段：group name、应用名称列表、branch、version（即 versionCode/planCode）、tenant（租户代码）、当前状态（Pending / Building / Success / Failed / Cancelled）、打包 API 触发时间（ISO 8601 格式）。

3. WHEN BuildPackageDialog 打开时，THE BuildPackageDialog SHALL 尝试读取 Queue_Persistence_File，IF 文件存在且包含状态为 "Pending" 或 "Building" 的 QueueEntry，THEN THE BuildPackageDialog SHALL 加载这些条目到 PendingDeployPanel 并提示用户是否恢复监控（仅恢复轮询查询，不重新触发打包 API）。

4. WHEN 用户选择恢复监控，THE Build_Queue SHALL 从第一个状态为 "Building" 的 QueueEntry 开始恢复轮询，使用当前对话框传入的 token（token 长期有效）和 tenant 进行 API 查询。IF 没有 "Building" 状态的条目，则从第一个 "Pending" 状态的条目开始，但不自动触发打包 API，仅展示状态供用户决策。

5. WHEN 所有 QueueEntry 均为终态（Success、Failed 或 Cancelled），THE Build_Queue SHALL 删除 Queue_Persistence_File。

---

### Requirement 5: 队列执行中的 UI 交互控制

**User Story:** As a developer, I want the UI to reflect the queue's running state, so that I don't accidentally submit duplicate builds or close the dialog mid-queue.

#### Acceptance Criteria

1. WHILE Build_Queue 正在执行，THE BuildPackageDialog SHALL 禁用 "Build Package" 按钮，防止重复提交。

2. WHILE Build_Queue 正在执行，THE BuildPackageDialog SHALL 在 "Build Package" 按钮旁显示一个 "Cancel Queue" 按钮。

3. WHEN 用户点击 "Cancel Queue" 按钮，THE Build_Queue SHALL 停止触发后续 QueueEntry 的打包 API，将所有剩余 "Pending" 状态的 QueueEntry 标记为 "Cancelled"，并将 Queue_Persistence_File 中对应条目更新为 "Cancelled" 状态。

4. WHEN 用户点击 "Cancel Queue" 按钮，THE Build_Queue SHALL 不中断当前正在轮询等待的 QueueEntry，允许其自然完成或失败。

5. WHILE Build_Queue 正在执行，IF 用户尝试关闭 BuildPackageDialog，THEN THE BuildPackageDialog SHALL 弹出确认对话框，提示队列仍在运行，询问用户是否确认关闭（关闭后队列将停止执行但状态已持久化）。

6. THE PendingDeployPanel SHALL 允许用户手动删除 PendingDeployPanel 中的任意 QueueEntry 条目（无论其状态），删除后同步更新 Queue_Persistence_File。

---

### Requirement 6: 轮询开关与手动刷新

**User Story:** As a developer, I want to control whether the system polls automatically, and be able to manually trigger a status refresh, so that I have flexibility over network usage and can check results on demand.

#### Acceptance Criteria

1. THE PendingDeployPanel SHALL 在面板顶部提供一个 "Auto Polling" checkbox，默认为勾选状态（启用自动轮询），以及一个可编辑的轮询间隔输入框（单位：秒，默认 10，最小 5），用户修改后立即生效，新的间隔值持久化保存到 Queue_Persistence_File 中。

2. WHEN "Auto Polling" checkbox 被取消勾选，THE Build_Queue SHALL 暂停自动轮询计时器，不再定时调用 `getBuildResultByPlan` API，但不影响当前队列的执行状态记录。

3. WHEN "Auto Polling" checkbox 被重新勾选，THE Build_Queue SHALL 立即恢复自动轮询，以当前配置的 Polling_Interval 为间隔继续查询。

4. THE PendingDeployPanel SHALL 在 "Auto Polling" checkbox 旁提供一个 "Refresh" 超链接，点击后立即触发一次 `getBuildResultByPlan` API 查询并更新当前 Building 状态的 QueueEntry 的构建结果，不受 Auto Polling 开关状态影响。

5. WHEN 系统无法连接到 API（网络错误），THE PendingDeployPanel SHALL 在面板顶部显示错误提示，不中断队列状态记录，并在下次轮询或手动刷新时重试。
