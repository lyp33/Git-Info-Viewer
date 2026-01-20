# Stage Build ID 解决方案

## 结论

经过验证，`wfapi/describe` API **不包含** Stage 级别的 Build ID（如 #809）。

唯一的获取方式是：**从 Stage Log 中解析**

## 实现方案

### 方案：从 Stage Log 解析 Build ID

当用户点击某个 Stage 时，从日志中提取 Build ID。

#### 步骤 1: 添加解析方法到 JenkinsApiClient

```java
/**
 * 从 Stage Log 中提取 Build ID
 * 
 * @param stageLog Stage 日志内容
 * @return Build ID，如果未找到返回 null
 */
public Integer extractStageBuildId(String stageLog) {
    if (stageLog == null || stageLog.isEmpty()) {
        return null;
    }
    
    // 匹配模式：
    // 1. "of #809"
    // 2. "building: ... #809"
    // 3. "CI-Robot of #809"
    Pattern pattern = Pattern.compile("(?:of|building:.*?)\\s*#(\\d+)");
    Matcher matcher = pattern.matcher(stageLog);
    
    if (matcher.find()) {
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse stage build ID: {}", matcher.group(1));
            return null;
        }
    }
    
    return null;
}
```

#### 步骤 2: 更新 JenkinsStage 类

```java
public class JenkinsStage {
    private String id;
    private String name;
    private String status;
    private long durationMillis;
    private long startTimeMillis;
    private Integer stageBuildNumber;  // 新增：Stage 的 Build ID
    
    // Getter 和 Setter
    public Integer getStageBuildNumber() {
        return stageBuildNumber;
    }
    
    public void setStageBuildNumber(Integer stageBuildNumber) {
        this.stageBuildNumber = stageBuildNumber;
    }
    
    /**
     * 获取 Stage Build ID 的显示文本
     */
    public String getStageBuildDisplay() {
        if (stageBuildNumber != null && stageBuildNumber > 0) {
            return "#" + stageBuildNumber;
        }
        return "";
    }
    
    /**
     * 是否有 Stage Build ID
     */
    public boolean hasStageBuildId() {
        return stageBuildNumber != null && stageBuildNumber > 0;
    }
}
```

#### 步骤 3: 在加载 Stage Log 时提取 Build ID

修改 `JenkinsStageViewPanel.loadStageLogToConsole()` 方法：

```java
private void loadStageLogToConsole(JenkinsStage stage) {
    if (apiClient == null || jobPath == null || stage.getId() == null) {
        logToConsole("Cannot load module log: missing API client or module ID");
        return;
    }
    
    logToConsole("Loading log for module: " + stage.getName());
    
    SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
        @Override
        protected String doInBackground() throws Exception {
            return apiClient.fetchStageLog(jobPath, buildNumber, stage.getId());
        }

        @Override
        protected void done() {
            try {
                String log = get();
                
                // 🆕 尝试从日志中提取 Stage Build ID
                Integer stageBuildId = apiClient.extractStageBuildId(log);
                if (stageBuildId != null) {
                    stage.setStageBuildNumber(stageBuildId);
                    logToConsole("✓ Detected Stage Build ID: #" + stageBuildId);
                    
                    // 刷新 Stage 列表显示
                    stageList.repaint();
                }
                
                // 显示日志到外部 Console Log 区域
                if (externalConsoleLogArea != null) {
                    externalConsoleLogArea.setText(log);
                    externalConsoleLogArea.setCaretPosition(0);
                }
                
                logToConsole("Module log loaded successfully");
            } catch (Exception e) {
                logToConsole("ERROR: Failed to load module log: " + e.getMessage());
                if (externalConsoleLogArea != null) {
                    externalConsoleLogArea.setText("Failed to load log: " + e.getMessage());
                }
            }
        }
    };
    
    worker.execute();
}
```

#### 步骤 4: 在 UI 中显示 Stage Build ID

修改 `StageListCellRenderer`：

```java
private class StageListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        
        if (value instanceof JenkinsStage) {
            JenkinsStage stage = (JenkinsStage) value;
            
            // 构建显示文本
            StringBuilder displayText = new StringBuilder();
            
            // Stage 名称
            displayText.append(stage.getName());
            
            // 持续时间
            displayText.append(" (").append(stage.getFormattedDuration()).append(")");
            
            // 🆕 如果有 Stage Build ID，显示它
            if (stage.hasStageBuildId()) {
                displayText.append(" - Build ").append(stage.getStageBuildDisplay());
            }
            
            setText(displayText.toString());
            
            // 设置状态颜色
            if (!isSelected) {
                if (stage.isSuccess()) {
                    setForeground(new Color(0, 128, 0));  // 绿色
                } else if (stage.isFailure()) {
                    setForeground(new Color(255, 0, 0));  // 红色
                } else if (stage.isInProgress()) {
                    setForeground(new Color(0, 0, 255));  // 蓝色
                }
            }
            
            // 设置 Tooltip
            setToolTipText(buildTooltip(stage));
        }
        
        return this;
    }
    
    private String buildTooltip(JenkinsStage stage) {
        StringBuilder tooltip = new StringBuilder("<html>");
        tooltip.append("<b>").append(stage.getName()).append("</b><br>");
        tooltip.append("<b>Status:</b> ").append(stage.getStatus()).append("<br>");
        tooltip.append("<b>Duration:</b> ").append(stage.getFormattedDuration()).append("<br>");
        
        if (stage.hasStageBuildId()) {
            tooltip.append("<b>Build ID:</b> ").append(stage.getStageBuildDisplay()).append("<br>");
        }
        
        if (stage.getStartTimeMillis() > 0) {
            String startTime = new SimpleDateFormat("HH:mm:ss").format(new Date(stage.getStartTimeMillis()));
            tooltip.append("<b>Start Time:</b> ").append(startTime).append("<br>");
        }
        
        tooltip.append("</html>");
        return tooltip.toString();
    }
}
```

## 效果展示

### 显示效果

```
┌─────────────────────────────────────────────────┐
│ Module List                                    │
├─────────────────────────────────────────────────┤
│ ✓ gemini-pa-bs-parent (39s) - Build #809      │
│ ✓ bff-parent (55s) - Build #810                │
│ ✓ common-bff (2m 10s) - Build #811             │
│ ✓ pa-bs (2m 34s) - Build #812                  │
│ ✓ claim-bs (2m 39s) - Build #813               │
└─────────────────────────────────────────────────┘
```

### Tooltip 显示

```
┌─────────────────────────────┐
│ gemini-pa-bs-parent        │
│ Status: SUCCESS            │
│ Duration: 39s              │
│ Build ID: #809             │
│ Start Time: 13:52:26       │
└─────────────────────────────┘
```

## 工作流程

1. 用户双击 Build History 中的某个构建
2. 加载 Stage 列表（此时没有 Build ID）
3. 用户点击某个 Stage
4. 加载 Stage Log
5. 从 Log 中解析出 Build ID（如 #809）
6. 更新 Stage 对象的 `stageBuildNumber` 字段
7. 刷新 UI 显示，显示 Build ID

## 优点

- ✅ 不需要额外的 API 调用
- ✅ 从已有的 Log 数据中提取
- ✅ 用户点击 Stage 时自动获取
- ✅ 提取后缓存在 Stage 对象中

## 缺点

- ⚠️ 需要用户先点击 Stage 才能看到 Build ID
- ⚠️ 依赖日志格式（但这个格式很稳定）

## 改进建议

### 可选：预加载所有 Stage 的 Build ID

如果想在显示 Stage 列表时就显示 Build ID，可以在 `displayStages()` 后自动加载所有 Stage 的日志：

```java
public void displayStages(List<JenkinsStage> stages) {
    stageListModel.clear();
    
    if (stages != null && !stages.isEmpty()) {
        for (JenkinsStage stage : stages) {
            stageListModel.addElement(stage);
        }
        
        // 🆕 可选：预加载所有 Stage 的 Build ID
        if (autoLoadStageBuildIds) {
            preloadStageBuildIds(stages);
        }
    }
}

private void preloadStageBuildIds(List<JenkinsStage> stages) {
    for (JenkinsStage stage : stages) {
        SwingWorker<Integer, Void> worker = new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                String log = apiClient.fetchStageLog(jobPath, buildNumber, stage.getId());
                return apiClient.extractStageBuildId(log);
            }

            @Override
            protected void done() {
                try {
                    Integer buildId = get();
                    if (buildId != null) {
                        stage.setStageBuildNumber(buildId);
                        stageList.repaint();  // 刷新显示
                    }
                } catch (Exception e) {
                    // 忽略错误，不影响主流程
                }
            }
        };
        
        worker.execute();
    }
}
```

**注意**：这会增加 API 调用次数，可能影响性能。建议作为可选功能。

## 总结

**最佳方案**：从 Stage Log 中解析 Build ID

**实现步骤**：
1. ✅ 添加 `extractStageBuildId()` 方法到 `JenkinsApiClient`
2. ✅ 在 `JenkinsStage` 类中添加 `stageBuildNumber` 字段
3. ✅ 在加载 Stage Log 时提取 Build ID
4. ✅ 在 UI 中显示 Build ID

**用户体验**：
- 用户点击 Stage 后，自动提取并显示 Build ID
- Build ID 显示在 Stage 名称后面：`gemini-pa-bs-parent (39s) - Build #809`
- Tooltip 中也显示 Build ID

你想让我实现这个方案吗？
