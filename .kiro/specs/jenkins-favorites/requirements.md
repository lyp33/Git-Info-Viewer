# Requirements Document

## Introduction

为 Jenkins Job Browser 添加收藏功能，允许用户收藏常用的 Jenkins Job（叶子节点），并提供快速访问入口，提升用户体验和工作效率。

## Glossary

- **Jenkins_Job_Browser**: Jenkins 任务浏览器对话框，用于浏览和管理 Jenkins 任务
- **Favorite_Job**: 用户收藏的 Jenkins 任务（叶子节点）
- **Leaf_Node**: Jenkins 任务树中的叶子节点，代表具体的可执行任务
- **Favorites_Panel**: 显示收藏任务列表的面板
- **Job_Path**: Jenkins 任务的完整路径，例如 "gemini/job/Manual-Build/job/thailifesdk/job/24.08_thailife_sit"

## Requirements

### Requirement 1: 收藏任务

**User Story:** 作为用户，我想要收藏常用的 Jenkins Job，这样我可以快速访问它们而不需要每次都在树中查找。

#### Acceptance Criteria

1. WHEN 用户在 Jenkins Job Browser 中右键点击一个叶子节点 THEN 系统应该显示"添加到收藏"菜单项
2. WHEN 用户点击"添加到收藏"菜单项 THEN 系统应该将该任务添加到收藏列表
3. WHEN 用户尝试收藏一个已经收藏的任务 THEN 系统应该显示"从收藏中移除"菜单项而不是"添加到收藏"
4. WHEN 用户收藏一个任务 THEN 系统应该在该节点上显示收藏标记（例如星标图标）
5. WHEN 用户右键点击非叶子节点（文件夹） THEN 系统不应该显示收藏相关的菜单项

### Requirement 2: 显示收藏列表

**User Story:** 作为用户，我想要看到我收藏的所有任务列表，这样我可以快速了解我的常用任务。

#### Acceptance Criteria

1. WHEN Jenkins Job Browser 打开 THEN 系统应该在界面顶部显示收藏面板
2. WHEN 收藏列表为空 THEN 系统应该显示提示信息"暂无收藏任务"
3. WHEN 收藏列表不为空 THEN 系统应该显示所有收藏任务的名称和路径
4. WHEN 显示收藏任务 THEN 系统应该显示任务的简短名称和完整路径提示
5. WHEN 收藏列表超过可见区域 THEN 系统应该提供滚动功能

### Requirement 3: 快速定位到收藏任务

**User Story:** 作为用户，我想要点击收藏列表中的任务，这样我可以快速定位到树中的对应节点。

#### Acceptance Criteria

1. WHEN 用户点击收藏列表中的任务 THEN 系统应该在树中展开并选中该任务节点
2. WHEN 用户点击收藏列表中的任务 THEN 系统应该自动滚动树视图使该节点可见
3. WHEN 用户点击收藏列表中的任务 THEN 系统应该在右侧面板显示该任务的详细信息
4. WHEN 任务路径中的父节点未展开 THEN 系统应该自动展开所有父节点
5. WHEN 定位失败（任务不存在） THEN 系统应该显示错误提示并询问是否从收藏中移除

### Requirement 4: 管理收藏任务

**User Story:** 作为用户，我想要管理我的收藏任务，这样我可以移除不再需要的收藏或调整顺序。

#### Acceptance Criteria

1. WHEN 用户在收藏列表中右键点击任务 THEN 系统应该显示"从收藏中移除"菜单项
2. WHEN 用户点击"从收藏中移除" THEN 系统应该将该任务从收藏列表中移除
3. WHEN 用户移除收藏 THEN 系统应该移除树节点上的收藏标记
4. WHEN 用户在收藏列表中右键点击任务 THEN 系统应该显示"上移"和"下移"菜单项（如果适用）
5. WHEN 用户调整收藏顺序 THEN 系统应该立即更新收藏列表显示

### Requirement 5: 持久化收藏数据

**User Story:** 作为用户，我想要我的收藏在应用重启后仍然保留，这样我不需要重新设置收藏。

#### Acceptance Criteria

1. WHEN 用户添加或移除收藏 THEN 系统应该立即保存收藏数据到本地文件
2. WHEN 应用启动 THEN 系统应该从本地文件加载收藏数据
3. WHEN 保存收藏数据 THEN 系统应该保存任务的完整路径和显示名称
4. WHEN 加载收藏数据失败 THEN 系统应该使用空的收藏列表并记录错误日志
5. WHEN 收藏数据文件不存在 THEN 系统应该创建新的空收藏列表

### Requirement 6: 收藏任务的视觉反馈

**User Story:** 作为用户，我想要在树中看到哪些任务已被收藏，这样我可以快速识别我的常用任务。

#### Acceptance Criteria

1. WHEN 任务被收藏 THEN 系统应该在树节点图标旁显示星标图标
2. WHEN 任务从收藏中移除 THEN 系统应该移除星标图标
3. WHEN 树刷新或重新加载 THEN 系统应该保持收藏标记的显示
4. WHEN 鼠标悬停在收藏标记上 THEN 系统应该显示"已收藏"提示
5. WHEN 收藏标记显示 THEN 系统应该使用醒目的颜色（例如金色或黄色）


### Requirement 7: 增强 Build History 显示

**User Story:** 作为用户，我想要在 Build History 列表中看到更详细的构建信息（如版本号、触发用户等），这样我可以更容易识别和区分不同的构建。

#### Acceptance Criteria

1. WHEN 显示 Build History 列表 THEN 系统应该显示构建编号、状态、时间、触发用户和关键参数
2. WHEN 构建包含版本号参数 THEN 系统应该在列表项中显示版本号
3. WHEN 构建包含多个参数 THEN 系统应该显示最重要的参数（如 VERSION, BRANCH, TAG）
4. WHEN 构建由用户触发 THEN 系统应该显示触发用户的名称
5. WHEN 构建由定时任务触发 THEN 系统应该显示"定时任务"或"Timer"
6. WHEN 构建由 SCM 变更触发 THEN 系统应该显示"SCM 变更"或提交者信息
7. WHEN 参数值过长 THEN 系统应该截断并显示省略号，鼠标悬停时显示完整值
8. WHEN 构建没有参数 THEN 系统应该只显示构建编号、状态、时间和触发用户
9. WHEN 显示格式 THEN 系统应该使用清晰的布局，例如："#154 - Failed - Jan 13, 2026 21:02 - by yunpeng.li - [VERSION: 2.3.1]"

### Requirement 8: Stage View 表格化显示

**User Story:** 作为用户，我想要 Stage View 使用表格行样式显示，这样可以在有限空间内显示更多的 modules 和 stages。

#### Acceptance Criteria

1. WHEN 显示 Stage View THEN 系统应该使用表格布局而不是卡片布局
2. WHEN 使用表格布局 THEN 系统应该显示列标题（Stage 名称、状态、持续时间）
3. WHEN 显示多个 stages THEN 系统应该每个 stage 占一行，紧凑显示
4. WHEN stage 状态为成功 THEN 系统应该使用绿色背景色标识该行
5. WHEN stage 状态为失败 THEN 系统应该使用红色背景色标识该行
6. WHEN stage 状态为进行中 THEN 系统应该使用蓝色背景色标识该行
7. WHEN 表格行数超过可见区域 THEN 系统应该提供垂直滚动功能
8. WHEN 用户点击表格行 THEN 系统应该显示该 stage 的详细日志
9. WHEN 显示 stage 持续时间 THEN 系统应该使用易读的格式（如 "2m 30s"）
10. WHEN 表格列宽不足 THEN 系统应该截断文本并提供工具提示显示完整内容
