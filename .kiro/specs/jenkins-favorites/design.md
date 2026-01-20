# Design Document: Jenkins Enhancements

## Overview

本设计文档描述了三个 Jenkins Job Browser 增强功能的技术实现方案：
1. Jenkins Job 收藏功能
2. Build History 详细信息显示
3. Stage View 表格化布局

这些功能将提升用户体验，使 Jenkins 任务管理更加高效便捷。

## Architecture

### Component Overview

```
JenkinsBrowserDialog (主对话框)
├── FavoritesPanel (收藏面板 - 新增)
│   ├── JList<FavoriteJob> (收藏列表)
│   └── FavoriteJobRenderer (自定义渲染器)
├── JTree (任务树)
│   └── FavoriteTreeCellRenderer (带星标的渲染器 - 增强)
├── JList<JenkinsBuild> (构建历史列表 - 增强)
│   └── BuildHistoryRenderer (详细信息渲染器 - 增强)
└── JenkinsStageViewPanel (Stage 视图 - 重构)
    └── JTable (表格布局 - 替代卡片布局)

AppSettings (设置管理)
└── jenkinsFavorites: List<FavoriteJob> (持久化收藏数据)

JenkinsBuild (数据模型 - 增强)
├── triggeredBy: String (触发用户)
└── parameters: Map<String, String> (构建参数)
```

## Components and Interfaces

### 1. FavoriteJob (新增数据模型)

```java
public class FavoriteJob implements Serializable {
    private String jobPath;        // 完整路径: "gemini/job/Manual-Build/..."
    private String displayName;    // 显示名称: "all-in-one-auto-CI"
    private String jobUrl;         // Jenkins URL
    private int order;             // 排序顺序
    
    // Constructors, getters, setters
}
```

### 2. FavoritesPanel (新增 UI 组件)

```java
public class FavoritesPanel extends JPanel {
    private JList<FavoriteJob> favoritesList;
    private DefaultListModel<FavoriteJob> listModel;
    private JenkinsBrowserDialog parentDialog;
    
    public FavoritesPanel(JenkinsBrowserDialog parent);
    public void addFavorite(FavoriteJob job);
    public void removeFavorite(FavoriteJob job);
    public void moveFavoriteUp(int index);
    public void moveFavoriteDown(int index);
    public void loadFavorites(List<FavoriteJob> favorites);
    private void navigateToJob(FavoriteJob job);
}
```

**布局设计：**
- 使用 BorderLayout
- 北部：标题标签 "收藏的任务"
- 中部：JScrollPane 包含 JList
- 南部：管理按钮（可选）
- 高度：约 150-200 像素

### 3. FavoriteTreeCellRenderer (增强树渲染器)

```java
public class FavoriteTreeCellRenderer extends DefaultTreeCellRenderer {
    private Set<String> favoriteJobPaths;
    private ImageIcon starIcon;
    
    @Override
    public Component getTreeCellRendererComponent(...) {
        // 如果节点在收藏列表中，显示星标图标
        // 使用金色星标 (⭐) 或自定义图标
    }
    
    public void setFavorites(Set<String> favorites);
}
```

### 4. Enhanced JenkinsBuild Model

```java
public class JenkinsBuild {
    // 现有字段
    private int number;
    private String result;
    private long timestamp;
    private String url;
    
    // 新增字段
    private String triggeredBy;              // 触发用户
    private Map<String, String> parameters;  // 构建参数
    private String displayInfo;              // 格式化的显示信息
    
    // 新增方法
    public String getFormattedDisplay() {
        // 格式: "#154 - Failed - Jan 13, 2026 21:02 - by yunpeng.li - [VERSION: 2.3.1]"
    }
    
    public String extractKeyParameters() {
        // 提取 VERSION, BRANCH, TAG 等关键参数
    }
}
```

### 5. BuildHistoryRenderer (增强构建历史渲染器)

```java
public class BuildHistoryRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(...) {
        JenkinsBuild build = (JenkinsBuild) value;
        
        // 构建显示文本
        String display = String.format(
            "#%d - %s - %s - by %s - %s",
            build.getNumber(),
            build.getResult(),
            formatTime(build.getTimestamp()),
            build.getTriggeredBy(),
            build.extractKeyParameters()
        );
        
        setText(display);
        // 设置颜色、图标等
    }
}
```

### 6. Refactored JenkinsStageViewPanel

```java
public class JenkinsStageViewPanel extends JPanel {
    private JTable stageTable;
    private StageTableModel tableModel;
    
    // 表格列
    private static final String[] COLUMN_NAMES = {
        "Stage", "Status", "Duration"
    };
    
    public void displayStages(List<JenkinsStage> stages) {
        tableModel.setStages(stages);
    }
    
    // 内部类：表格模型
    private class StageTableModel extends AbstractTableModel {
        private List<JenkinsStage> stages;
        
        @Override
        public int getRowCount() { return stages.size(); }
        
        @Override
        public int getColumnCount() { return 3; }
        
        @Override
        public Object getValueAt(int row, int col) {
            JenkinsStage stage = stages.get(row);
            switch (col) {
                case 0: return stage.getName();
                case 1: return stage.getStatus();
                case 2: return formatDuration(stage.getDurationMillis());
            }
        }
    }
    
    // 内部类：表格渲染器
    private class StageTableRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(...) {
            JenkinsStage stage = stages.get(row);
            
            // 根据状态设置背景色
            switch (stage.getStatus()) {
                case "SUCCESS":
                    setBackground(new Color(200, 255, 200)); // 浅绿色
                    break;
                case "FAILED":
                    setBackground(new Color(255, 200, 200)); // 浅红色
                    break;
                case "IN_PROGRESS":
                    setBackground(new Color(200, 220, 255)); // 浅蓝色
                    break;
            }
            
            return this;
        }
    }
}
```

## Data Models

### FavoriteJob Storage Format

```json
{
  "jenkinsFavorites": [
    {
      "jobPath": "gemini/job/Manual-Build/job/thailifesdk/job/24.08_thailife_sit/job/CI-Robot/job/all-in-one-auto-CI",
      "displayName": "all-in-one-auto-CI",
      "jobUrl": "http://172.25.32.166:8080/job/gemini/...",
      "order": 0
    }
  ]
}
```

### Enhanced Build JSON Response

需要从 Jenkins API 获取额外信息：

```json
{
  "number": 154,
  "result": "FAILURE",
  "timestamp": 1705154520000,
  "url": "http://...",
  "actions": [
    {
      "_class": "hudson.model.CauseAction",
      "causes": [
        {
          "userId": "yunpeng.li",
          "userName": "Yunpeng Li"
        }
      ]
    },
    {
      "_class": "hudson.model.ParametersAction",
      "parameters": [
        {
          "name": "VERSION",
          "value": "2.3.1"
        },
        {
          "name": "BRANCH",
          "value": "master"
        }
      ]
    }
  ]
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: 收藏持久化一致性
*For any* 收藏操作（添加或删除），保存到 AppSettings 后重新加载，应该得到相同的收藏列表

**Validates: Requirements 5.1, 5.2, 5.3**

### Property 2: 收藏列表唯一性
*For any* 任务路径，在收藏列表中最多出现一次

**Validates: Requirements 1.3**

### Property 3: 树节点定位正确性
*For any* 收藏的任务，点击后应该能在树中找到并选中对应的节点

**Validates: Requirements 3.1, 3.2, 3.4**

### Property 4: 构建信息完整性
*For any* 构建记录，如果 Jenkins API 返回了触发用户和参数信息，则显示文本中应该包含这些信息

**Validates: Requirements 7.1, 7.4, 7.5**

### Property 5: Stage 表格行数一致性
*For any* Stage 列表，表格显示的行数应该等于 Stage 数量

**Validates: Requirements 8.2, 8.3**

### Property 6: 收藏顺序保持性
*For any* 收藏列表调整顺序操作，保存后重新加载应该保持相同的顺序

**Validates: Requirements 4.4, 4.5**

## Error Handling

### 收藏功能错误处理

1. **任务不存在**
   - 场景：点击收藏但任务已被删除
   - 处理：显示错误对话框，询问是否从收藏中移除
   - 日志：记录警告信息

2. **持久化失败**
   - 场景：保存收藏数据到文件失败
   - 处理：显示错误提示，但保持内存中的收藏列表
   - 日志：记录错误堆栈

3. **加载失败**
   - 场景：启动时加载收藏数据失败
   - 处理：使用空列表，记录错误日志
   - 用户体验：不影响其他功能使用

### Build History 错误处理

1. **API 数据缺失**
   - 场景：Jenkins API 未返回触发用户或参数
   - 处理：显示 "Unknown" 或 "N/A"
   - 不影响其他信息显示

2. **参数解析失败**
   - 场景：参数格式异常
   - 处理：跳过该参数，继续显示其他参数
   - 日志：记录警告

### Stage View 错误处理

1. **Stage 数据为空**
   - 场景：构建没有 Stage 信息
   - 处理：显示空表格和提示信息 "No stages available"

2. **持续时间计算异常**
   - 场景：时间戳异常
   - 处理：显示 "N/A"

## Testing Strategy

### Unit Tests

1. **FavoriteJob 序列化测试**
   - 测试 JSON 序列化和反序列化
   - 测试字段完整性

2. **Build 信息格式化测试**
   - 测试各种参数组合的显示格式
   - 测试触发用户信息提取

3. **Stage 表格模型测试**
   - 测试行列数计算
   - 测试数据获取

### Integration Tests

1. **收藏功能端到端测试**
   - 添加收藏 → 保存 → 重启 → 验证加载

2. **树节点定位测试**
   - 点击收藏 → 验证树展开和选中

3. **Build History 显示测试**
   - 模拟 API 响应 → 验证显示格式

### Property-Based Tests

每个 Correctness Property 都应该有对应的 property-based test，运行至少 100 次迭代。

## Implementation Notes

### Jenkins API 调用增强

需要修改 `JenkinsApiClient.fetchBuilds()` 方法，获取额外信息：

```java
// 添加 actions 参数到 API 请求
String apiUrl = jobUrl + "/api/json?tree=builds[number,result,timestamp,url,actions[causes[userId,userName],parameters[name,value]]]";
```

### AppSettings 扩展

```java
public class AppSettings {
    // 新增字段
    private List<FavoriteJob> jenkinsFavorites = new ArrayList<>();
    
    // 新增方法
    public void addJenkinsFavorite(FavoriteJob job);
    public void removeJenkinsFavorite(String jobPath);
    public List<FavoriteJob> getJenkinsFavorites();
    public void saveJenkinsFavorites(List<FavoriteJob> favorites);
}
```

### UI 布局调整

**JenkinsBrowserDialog 布局变更：**

```
┌─────────────────────────────────────────┐
│ Jenkins Job Browser                      │
├─────────────────────────────────────────┤
│ ⭐ 收藏的任务                            │
│ ┌─────────────────────────────────────┐ │
│ │ all-in-one-auto-CI                  │ │
│ │ clean_workspace                     │ │
│ └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│ ┌─────────┬───────────────────────────┐ │
│ │ Tree    │ Build History             │ │
│ │         │ #154 - Failed - Jan 13... │ │
│ │         │   by yunpeng.li           │ │
│ │         │   [VERSION: 2.3.1]        │ │
│ │         ├───────────────────────────┤ │
│ │         │ Stage View (Table)        │ │
│ │         │ ┌──────┬────────┬────────┐│ │
│ │         │ │Stage │Status  │Duration││ │
│ │         │ ├──────┼────────┼────────┤│ │
│ │         │ │clean │SUCCESS │1m 20s  ││ │
│ │         │ │build │FAILED  │3m 45s  ││ │
│ │         │ └──────┴────────┴────────┘│ │
│ └─────────┴───────────────────────────┘ │
└─────────────────────────────────────────┘
```

### Performance Considerations

1. **收藏列表大小限制**
   - 建议最多 50 个收藏
   - 超过时显示警告

2. **树节点定位优化**
   - 使用路径缓存避免重复遍历
   - 异步展开节点避免 UI 冻结

3. **表格渲染优化**
   - 使用虚拟滚动（如果 Stage 数量很大）
   - 缓存渲染组件

## Migration Strategy

由于这是增强现有功能，需要考虑向后兼容：

1. **AppSettings 兼容性**
   - 新增字段使用默认值
   - 旧版本配置文件可以正常加载

2. **UI 渐进式增强**
   - 收藏面板可折叠
   - 用户可以选择是否显示

3. **API 兼容性**
   - 如果 Jenkins API 不返回新字段，使用默认值
   - 不影响现有功能
