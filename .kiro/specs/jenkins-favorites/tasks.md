# Implementation Plan: Jenkins Enhancements

## Overview

实现三个 Jenkins Job Browser 增强功能：收藏功能、Build History 详细信息显示、Stage View 表格化布局。

## Tasks

- [x] 1. 创建收藏功能数据模型和持久化
  - 创建 FavoriteJob 数据类
  - 扩展 AppSettings 支持收藏数据存储
  - 实现序列化和反序列化
  - _Requirements: 1.2, 5.1, 5.2, 5.3_

- [x] 2. 实现收藏面板 UI
  - [x] 2.1 创建 FavoritesPanel 组件
    - 创建面板布局（标题 + 列表 + 滚动）
    - 实现 JList 和 ListModel
    - 添加右键菜单（移除、上移、下移）
    - _Requirements: 2.1, 2.2, 2.3, 4.1, 4.2, 4.4_
  
  - [x] 2.2 实现自定义列表渲染器
    - 显示任务名称和路径提示
    - 处理长文本截断
    - _Requirements: 2.4, 2.5_
  
  - [x] 2.3 集成到 JenkinsBrowserDialog
    - 在对话框顶部添加收藏面板
    - 设置合适的高度和布局
    - _Requirements: 2.1_

- [x] 3. 实现树节点收藏标记
  - [x] 3.1 创建 FavoriteTreeCellRenderer
    - 扩展 DefaultTreeCellRenderer
    - 添加星标图标显示逻辑
    - 设置星标颜色和位置
    - _Requirements: 6.1, 6.2, 6.4, 6.5_
  
  - [x] 3.2 添加右键菜单收藏选项
    - 检测叶子节点
    - 添加"添加到收藏"/"从收藏中移除"菜单项
    - 实现菜单项点击处理
    - _Requirements: 1.1, 1.3, 1.5_

- [x] 4. 实现快速定位功能
  - [x] 4.1 实现树节点查找和展开
    - 根据路径查找树节点
    - 递归展开父节点
    - 选中目标节点并滚动到可见区域
    - _Requirements: 3.1, 3.2, 3.4_
  
  - [x] 4.2 处理定位失败情况
    - 显示错误对话框
    - 询问是否从收藏中移除
    - _Requirements: 3.5_

- [x] 5. 增强 JenkinsBuild 数据模型
  - 添加 triggeredBy 字段
  - 添加 parameters 字段
  - 实现 getFormattedDisplay() 方法
  - 实现 extractKeyParameters() 方法
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

- [x] 6. 增强 Jenkins API 客户端
  - [x] 6.1 修改 fetchBuilds() 方法
    - 更新 API URL 包含 actions 参数
    - 解析 causes 获取触发用户
    - 解析 parameters 获取构建参数
    - _Requirements: 7.1, 7.4, 7.5, 7.6_
  
  - [x] 6.2 处理 API 数据缺失
    - 触发用户为空时使用默认值
    - 参数为空时跳过显示
    - _Requirements: 7.7, 7.8_

- [x] 7. 实现 Build History 详细显示
  - [x] 7.1 创建 BuildHistoryRenderer
    - 格式化显示文本
    - 包含编号、状态、时间、用户、参数
    - 设置颜色和图标
    - _Requirements: 7.1, 7.9_
  
  - [x] 7.2 处理参数显示
    - 提取关键参数（VERSION, BRANCH, TAG）
    - 长参数值截断
    - 添加工具提示显示完整值
    - _Requirements: 7.2, 7.3, 7.7_

- [ ] 8. 重构 Stage View 为表格布局
  - [ ] 8.1 创建表格模型
    - 实现 StageTableModel 继承 AbstractTableModel
    - 定义列（Stage, Status, Duration）
    - 实现 getRowCount, getColumnCount, getValueAt
    - _Requirements: 8.2, 8.3_
  
  - [ ] 8.2 创建表格渲染器
    - 实现 StageTableRenderer
    - 根据状态设置背景色（绿/红/蓝）
    - 格式化持续时间显示
    - _Requirements: 8.4, 8.5, 8.6, 8.9_
  
  - [ ] 8.3 替换现有卡片布局
    - 移除旧的 FlowLayout 和卡片组件
    - 创建 JTable 和 JScrollPane
    - 设置表格属性（行高、选择模式）
    - _Requirements: 8.1, 8.7_
  
  - [ ] 8.4 实现行点击事件
    - 添加鼠标监听器
    - 点击行显示 Stage 详细日志
    - _Requirements: 8.8_

- [x] 9. 测试和优化
  - [x] 9.1 单元测试
    - FavoriteJob 序列化测试
    - Build 信息格式化测试
    - Stage 表格模型测试
  
  - [x] 9.2 集成测试
    - 收藏功能端到端测试
    - 树节点定位测试
    - Build History 显示测试
  
  - [x] 9.3 性能优化
    - 收藏列表大小限制
    - 树节点定位缓存
    - 表格渲染优化

- [x] 10. 文档和打包
  - 更新用户文档
  - 创建功能演示截图
  - 编译打包
  - 创建发布包

## Notes

- 任务按功能模块组织，可以并行开发
- 收藏功能（任务 1-4）相对独立
- Build History 增强（任务 5-7）依赖 API 客户端修改
- Stage View 重构（任务 8）可以独立进行
- 所有功能都需要在 JenkinsBrowserDialog 中集成
