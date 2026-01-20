# Stage View API 信息

## 使用的 API

当双击 Build History 中的某个构建时，系统会调用以下 API 获取 Stage 信息：

### API 端点
```
GET {jenkinsUrl}/{jobPath}/{buildNumber}/wfapi/describe
```

### 示例
```
GET http://localhost:8888/job/gemini/job/Test-Job/job/backend-service/243/wfapi/describe
```

## API 返回的完整数据结构

```json
{
  "_class": "org.jenkinsci.plugins.workflow.job.WorkflowRun",
  "id": "243",
  "name": "#243",
  "status": "SUCCESS",
  "durationMillis": 315000,
  "stages": [
    {
      "id": "6",
      "name": "gemini-pa-bs-parent",
      "status": "SUCCESS",
      "durationMillis": 39000,
      "startTimeMillis": 1737189145000
    },
    {
      "id": "11",
      "name": "bff-parent",
      "status": "SUCCESS",
      "durationMillis": 55000,
      "startTimeMillis": 1737189184000
    }
  ]
}
```

## 当前已使用的字段

### Build 级别（顶层）
- ✅ `_class` - 类型标识
- ✅ `id` - 构建 ID
- ✅ `name` - 构建名称（如 "#243"）
- ✅ `status` - 整体状态
- ✅ `durationMillis` - 总持续时间

### Stage 级别（stages 数组）
- ✅ `id` - Stage ID（用于获取日志）
- ✅ `name` - Stage 名称
- ✅ `status` - Stage 状态（SUCCESS, FAILED, IN_PROGRESS, NOT_EXECUTED, ABORTED）
- ✅ `durationMillis` - Stage 持续时间

## 可以添加到 UI 的额外信息

### 1. 时间信息
- ⭐ **`startTimeMillis`** - Stage 开始时间（毫秒时间戳）
  - **用途**: 显示每个 Stage 的开始时间
  - **UI 展示**: "开始时间: 13:52:26"
  - **实现**: `new SimpleDateFormat("HH:mm:ss").format(new Date(startTimeMillis))`

- ⭐ **计算结束时间** - `startTimeMillis + durationMillis`
  - **用途**: 显示每个 Stage 的结束时间
  - **UI 展示**: "结束时间: 13:53:05"

### 2. Build 级别信息（可显示在顶部）
- ⭐ **`name`** - 构建编号
  - **用途**: 在 Stage View 顶部显示当前查看的构建编号
  - **UI 展示**: "Build #243"

- ⭐ **`status`** - 整体构建状态
  - **用途**: 显示整个构建的最终状态
  - **UI 展示**: 用颜色标识（绿色=SUCCESS，红色=FAILED）

- ⭐ **`durationMillis`** - 总持续时间
  - **用途**: 显示整个构建的总耗时
  - **UI 展示**: "总耗时: 5m 15s"

### 3. Stage 进度信息
- ⭐ **Stage 进度百分比**
  - **计算**: `(stageDuration / totalDuration) * 100`
  - **用途**: 显示每个 Stage 占总时间的百分比
  - **UI 展示**: 进度条或百分比文字 "占比: 12.4%"

- ⭐ **累计时间**
  - **计算**: 前面所有 Stage 的时间总和
  - **用途**: 显示到当前 Stage 为止的累计时间
  - **UI 展示**: "累计: 2m 34s"

### 4. 状态图标和颜色
- ⭐ **状态图标**
  - SUCCESS: ✓ (绿色)
  - FAILED: ✗ (红色)
  - IN_PROGRESS: ⟳ (蓝色)
  - NOT_EXECUTED: ○ (灰色)
  - ABORTED: ⊗ (橙色)

### 5. 时间线视图
- ⭐ **时间轴展示**
  - 使用 `startTimeMillis` 和 `durationMillis` 绘制时间线
  - 显示各个 Stage 的并行/串行关系
  - 可视化展示哪些 Stage 耗时最长

## 推荐的 UI 增强

### 方案 1: 在 Stage 列表中添加时间信息
```
┌─────────────────────────────────────────────────┐
│ Build #243 - SUCCESS - 总耗时: 5m 15s          │
├─────────────────────────────────────────────────┤
│ ✓ gemini-pa-bs-parent                          │
│   开始: 13:52:26 | 耗时: 39s | 占比: 12.4%    │
├─────────────────────────────────────────────────┤
│ ✓ bff-parent                                   │
│   开始: 13:52:32 | 耗时: 55s | 占比: 17.5%    │
├─────────────────────────────────────────────────┤
│ ✓ common-bff                                   │
│   开始: 13:52:42 | 耗时: 2m 10s | 占比: 41.3% │
└─────────────────────────────────────────────────┘
```

### 方案 2: 添加时间线可视化
```
┌─────────────────────────────────────────────────┐
│ Build #243 Timeline                            │
├─────────────────────────────────────────────────┤
│ 13:52:26 ████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░ │
│ gemini-pa-bs-parent (39s)                      │
│                                                 │
│ 13:52:32 ░░░░░░░░███████████░░░░░░░░░░░░░░░░░ │
│ bff-parent (55s)                               │
│                                                 │
│ 13:52:42 ░░░░░░░░░░░░░░░░████████████████████ │
│ common-bff (2m 10s)                            │
└─────────────────────────────────────────────────┘
```

### 方案 3: 添加统计信息面板
```
┌─────────────────────────────────────────────────┐
│ Build Statistics                               │
├─────────────────────────────────────────────────┤
│ 总 Stages: 5                                   │
│ 成功: 5 | 失败: 0 | 进行中: 0                  │
│ 总耗时: 5m 15s                                 │
│ 平均耗时: 1m 3s                                │
│ 最长 Stage: claim-bs (2m 39s)                  │
│ 最短 Stage: gemini-pa-bs-parent (39s)          │
└─────────────────────────────────────────────────┘
```

## 实现建议

### 1. 更新 JenkinsStage 类
添加 `startTimeMillis` 字段：

```java
public class JenkinsStage {
    private String id;
    private String name;
    private String status;
    private long durationMillis;
    private long startTimeMillis;  // 新增
    
    // 新增方法
    public String getFormattedStartTime() {
        if (startTimeMillis <= 0) {
            return "-";
        }
        return new SimpleDateFormat("HH:mm:ss").format(new Date(startTimeMillis));
    }
    
    public String getFormattedEndTime() {
        if (startTimeMillis <= 0 || durationMillis <= 0) {
            return "-";
        }
        return new SimpleDateFormat("HH:mm:ss").format(new Date(startTimeMillis + durationMillis));
    }
}
```

### 2. 更新 JenkinsApiClient
在 `fetchBuildStages` 方法中解析 `startTimeMillis`：

```java
long startTimeMillis = stageJson.optLong("startTimeMillis", 0);
stage.setStartTimeMillis(startTimeMillis);
```

### 3. 更新 UI 显示
在 `JenkinsStageViewPanel` 中添加更多信息的显示。

## 获取 Stage 级别的 Build ID

### 方法 1: 从上下文获取（推荐）✅

**Build ID 已经在代码中可用！**

当你调用 `fetchBuildStages(jobPath, buildNumber)` 时，`buildNumber` 就是 Build ID（如 243）。

在 `JenkinsStageViewPanel` 中：
```java
public class JenkinsStageViewPanel extends JPanel {
    private int buildNumber;  // 这就是 Build ID！
    
    public void setJobInfo(JenkinsApiClient apiClient, String jobPath, int buildNumber) {
        this.buildNumber = buildNumber;  // 已经存储了
    }
}
```

**使用方式**:
```java
// 在 Stage 列表中显示
String displayText = "Build #" + buildNumber + " - " + stage.getName();

// 在 Stage 详情中显示
logToConsole("Viewing Stage '" + stage.getName() + "' from Build #" + buildNumber);
```

### 方法 2: 从 API 响应获取

API 返回的顶层数据中包含 Build 信息：

```json
{
  "id": "243",           // ← Build ID (字符串格式)
  "name": "#243",        // ← Build 显示名称
  "stages": [...]
}
```

**在 `fetchBuildStages` 方法中解析**:
```java
public List<JenkinsStage> fetchBuildStages(String jobPath, int buildNumber) throws IOException {
    String response = sendGetRequest(apiUrl);
    JSONObject json = new JSONObject(response);
    
    // 解析 Build 信息
    String buildId = json.optString("id", "");        // "243"
    String buildName = json.optString("name", "");    // "#243"
    String buildStatus = json.optString("status", ""); // "SUCCESS"
    
    // 可以返回一个包含 Build 信息的对象
    // 或者将 Build ID 添加到每个 Stage 对象中
}
```

### 方法 3: 将 Build ID 添加到 JenkinsStage 类

**更新 JenkinsStage 类**:
```java
public class JenkinsStage {
    private String id;
    private String name;
    private String status;
    private long durationMillis;
    private long startTimeMillis;
    private int buildNumber;      // 新增：Build ID
    private String buildName;     // 新增：Build 显示名称（如 "#243"）
    
    // Getter 和 Setter
    public int getBuildNumber() {
        return buildNumber;
    }
    
    public void setBuildNumber(int buildNumber) {
        this.buildNumber = buildNumber;
    }
    
    public String getBuildName() {
        return buildName;
    }
    
    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }
}
```

**在 `fetchBuildStages` 中设置**:
```java
public List<JenkinsStage> fetchBuildStages(String jobPath, int buildNumber) throws IOException {
    String response = sendGetRequest(apiUrl);
    JSONObject json = new JSONObject(response);
    
    String buildName = json.optString("name", "#" + buildNumber);
    List<JenkinsStage> stages = new ArrayList<>();
    
    if (json.has("stages")) {
        JSONArray stagesArray = json.getJSONArray("stages");
        for (int i = 0; i < stagesArray.length(); i++) {
            JSONObject stageJson = stagesArray.getJSONObject(i);
            
            JenkinsStage stage = new JenkinsStage(...);
            stage.setBuildNumber(buildNumber);  // 设置 Build ID
            stage.setBuildName(buildName);      // 设置 Build 名称
            stages.add(stage);
        }
    }
    
    return stages;
}
```

### 推荐方案

**🥇 方案 1（最简单）**: 直接使用 `JenkinsStageViewPanel` 中已有的 `buildNumber` 字段

**优点**:
- 不需要修改任何代码
- Build ID 已经在上下文中可用
- 可以直接在 UI 中显示

**使用示例**:
```java
// 在 JenkinsStageViewPanel 中
private void displayStageInfo(JenkinsStage stage) {
    String info = String.format("Build #%d - Stage: %s", 
                                buildNumber, stage.getName());
    // 显示在 UI 上
}
```

**🥈 方案 2（更灵活）**: 将 Build ID 添加到 `JenkinsStage` 对象中

**优点**:
- Stage 对象自包含所有信息
- 便于传递和显示
- 不依赖外部上下文

**缺点**:
- 需要修改 `JenkinsStage` 类
- 需要在解析时设置 Build ID

### 实现建议

如果你想在 Stage 列表中显示 Build ID，最简单的方法是：

**在 `StageListCellRenderer` 中使用 `buildNumber`**:

```java
private class StageListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(...) {
        super.getListCellRendererComponent(...);
        
        if (value instanceof JenkinsStage) {
            JenkinsStage stage = (JenkinsStage) value;
            
            // 显示 Build ID 和 Stage 名称
            String displayText = String.format("#%d - %s (%s)", 
                buildNumber,           // 使用外部类的 buildNumber
                stage.getName(), 
                stage.getFormattedDuration());
            
            setText(displayText);
        }
        
        return this;
    }
}
```

### 总结

**回答你的问题**: 

✅ **是的，可以获取 Stage 级别的 Build ID！**

**最简单的方法**: 
- Build ID 已经存储在 `JenkinsStageViewPanel.buildNumber` 中
- 直接使用即可，无需额外 API 调用

**如果需要从 API 获取**:
- API 响应的顶层包含 `id` 和 `name` 字段
- `id`: "243" (字符串)
- `name`: "#243" (显示格式)

**推荐做法**:
1. 使用已有的 `buildNumber` 字段（最简单）
2. 或者将 Build ID 添加到 `JenkinsStage` 类中（更灵活）

## 获取 Stage 级别的 Build ID

**当前使用的 API**: `{jenkinsUrl}/{jobPath}/{buildNumber}/wfapi/describe`

**已使用的字段**:
- Build 级别: `_class`, `id`, `name`, `status`, `durationMillis`
- Stage 级别: `id`, `name`, `status`, `durationMillis`

**可以添加的字段**:
- ⭐ `startTimeMillis` - 开始时间（最有价值）
- ⭐ 计算的结束时间 - `startTimeMillis + durationMillis`
- ⭐ 进度百分比 - `(stageDuration / totalDuration) * 100`
- ⭐ Build 整体信息 - 显示在顶部

**推荐优先级**:
1. 🥇 添加开始时间和结束时间显示
2. 🥈 添加 Build 整体信息（编号、状态、总耗时）
3. 🥉 添加进度百分比或时间占比
4. 💡 考虑添加时间线可视化（高级功能）
