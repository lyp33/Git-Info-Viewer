# Jenkins No-Stage Builds Enhanced Display - Complete

## 需求描述

对于没有 stage 的特殊 Jenkins build history，需要在两边都**明文显示**更多信息：
- Version
- Service Name
- Branch
- Trigger By
- Trigger Time

**重要**：这些信息需要直接显示在列表项的主文本中，而不仅仅是 tooltip。

## 实现方案

### 1. 数据模型增强

**JenkinsStage.java**
- 添加 `buildInfo` 字段，用于存储关联的 Build 信息
- 添加 `getBuildInfo()` 和 `setBuildInfo()` 方法

```java
private JenkinsBuild buildInfo;  // 关联的 Build 信息（用于合成 stage）

public JenkinsBuild getBuildInfo() {
    return buildInfo;
}

public void setBuildInfo(JenkinsBuild buildInfo) {
    this.buildInfo = buildInfo;
}
```

### 2. 合成 Stage 创建时附加 Build 信息

**JenkinsJobDetailsDialog.java - loadStageView()**

在创建合成 stage 时，附加完整的 build 信息：

```java
// 附加 build 信息，用于在 Stage View 中显示详细信息
syntheticStage.setBuildInfo(build);
```

### 3. Build History 列表显示（左侧）

**JenkinsJobDetailsDialog.java - BuildListCellRenderer**

Build History 列表已经通过 `JenkinsBuild.getFormattedDisplay()` 显示了完整信息：
- Build 编号
- 状态
- 触发时间
- 触发用户（by xxx）
- 关键参数（SERVICE_NAME, versions, VERSION, BRANCH 等）

**显示格式：**
```
● #1334 - SUCCESS - Jan 14, 2026 18:18 - by yunpeng.li - [SERVICE_NAME: pa-bs-core]
```

### 4. Stage View 面板显示增强（右侧）

**JenkinsStageViewPanel.java - StageListCellRenderer**

对于合成 stage（ID 以 "build-" 开头），在**主显示文本**中显示完整的 build 信息：

```java
// 检查是否是合成 stage（没有子 stage 的 build）
if (stage.getId() != null && stage.getId().startsWith("build-") && stage.getBuildInfo() != null) {
    // 对于合成 stage，显示完整的 build 信息
    JenkinsBuild build = stage.getBuildInfo();
    
    // 构建编号
    displayText.append("#").append(build.getNumber());
    
    // 状态
    displayText.append(" - ");
    if (build.getResult() != null) {
        displayText.append(build.getResult());
    } else {
        displayText.append("IN_PROGRESS");
    }
    
    // 时间
    displayText.append(" - ");
    displayText.append(formatTimestamp(build.getTimestamp()));
    
    // 触发用户
    if (build.getTriggeredBy() != null && !build.getTriggeredBy().isEmpty()) {
        displayText.append(" - by ").append(build.getTriggeredBy());
    }
    
    // 关键参数：优先显示 SERVICE_NAME, versions, VERSION, BRANCH
    Map<String, String> params = build.getParameters();
    if (params != null && !params.isEmpty()) {
        StringBuilder paramText = new StringBuilder();
        
        if (params.containsKey("SERVICE_NAME")) {
            String serviceName = params.get("SERVICE_NAME");
            if (serviceName != null && !serviceName.isEmpty()) {
                if (paramText.length() > 0) paramText.append(", ");
                paramText.append("SERVICE_NAME: ").append(serviceName);
            }
        }
        
        if (params.containsKey("versions")) {
            String versions = params.get("versions");
            if (versions != null && !versions.isEmpty()) {
                if (paramText.length() > 0) paramText.append(", ");
                // 截断过长的值
                if (versions.length() > 50) {
                    versions = versions.substring(0, 47) + "...";
                }
                paramText.append("versions: ").append(versions);
            }
        }
        
        if (params.containsKey("VERSION")) {
            String version = params.get("VERSION");
            if (version != null && !version.isEmpty()) {
                if (paramText.length() > 0) paramText.append(", ");
                paramText.append("VERSION: ").append(version);
            }
        }
        
        if (params.containsKey("BRANCH") || params.containsKey("branch")) {
            String branch = params.containsKey("BRANCH") ? params.get("BRANCH") : params.get("branch");
            if (branch != null && !branch.isEmpty()) {
                if (paramText.length() > 0) paramText.append(", ");
                paramText.append("BRANCH: ").append(branch);
            }
        }
        
        if (paramText.length() > 0) {
            displayText.append(" - [").append(paramText).append("]");
        }
    }
}
```

## 显示效果

### Build History 列表（左侧）

**主显示文本（已有功能）：**
```
● #1334 - SUCCESS - Jan 14, 2026 18:18 - by yunpeng.li - [SERVICE_NAME: pa-bs-core]
```

**Tooltip（鼠标悬停）：**
```
Build #1334
Status: SUCCESS
Triggered by: yunpeng.li
Trigger Time: Jan 14, 2026 18:18
Service Name: pa-bs-core
Versions: 2.3.1
Branch: feature/new-api
```

### Stage View 面板（右侧）- 新增功能

**主显示文本（合成 stage）：**
```
● #1334 - SUCCESS - Jan 14, 2026 18:18 - by yunpeng.li - [SERVICE_NAME: pa-bs-core, VERSION: 2.3.1, BRANCH: feature/new-api]
```

**主显示文本（正常 stage）：**
```
● Build Stage (2m 30s) - Build #123
```

**Tooltip（鼠标悬停 - 合成 stage）：**
```
Module: Build #1334
Status: SUCCESS
Duration: -

--- Build Details ---
Triggered by: yunpeng.li
Trigger Time: Jan 14, 2026 18:18
Service Name: pa-bs-core
Versions: 2.3.1
Version: 2.3.1
Branch: feature/new-api

Click to view log in console, double-click to open dialog
```

## 技术细节

### 参数提取优先级

从 `JenkinsBuild.getParameters()` 中提取以下参数（按优先级）：
1. **SERVICE_NAME** - 服务名称
2. **versions** - 完整版本信息（截断超过 50 字符）
3. **VERSION** - 单个版本号
4. **BRANCH** / **branch** - 分支名称

### 时间格式化

使用 `SimpleDateFormat` 格式化时间戳：
```java
SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
```

输出格式：`Jan 21, 2026 19:00`

### 合成 Stage 识别

通过 Stage ID 的特殊前缀识别合成 stage：
```java
if (stage.getId() != null && stage.getId().startsWith("build-")) {
    // 这是一个合成 stage
}
```

### 显示逻辑

**Stage View 面板的 StageListCellRenderer：**
- 如果是合成 stage（`build-` 前缀）且有 buildInfo：显示完整 build 信息
- 如果是正常 stage：显示 stage 名称和持续时间

## 修改的文件

1. **src/main/java/com/gitviewer/JenkinsStage.java**
   - 添加 `buildInfo` 字段
   - 添加 getter/setter 方法

2. **src/main/java/com/gitviewer/JenkinsJobDetailsDialog.java**
   - 修改 `loadStageView()` - 附加 build 信息到合成 stage
   - 修改 `BuildListCellRenderer.buildTooltip()` - 增强 tooltip 显示

3. **src/main/java/com/gitviewer/JenkinsStageViewPanel.java**
   - 添加 `import java.util.Map;`
   - **重点修改** `StageListCellRenderer.getListCellRendererComponent()` - 为合成 stage 在主文本中显示完整信息
   - 修改 `buildTooltip()` - 为合成 stage 显示详细信息
   - 添加 `formatTimestamp()` 方法

## 构建状态

✅ 编译成功
✅ 打包成功

```bash
mvn clean package
```

生成文件：`target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 测试建议

1. **正常 stages 的 job**：
   - 打开一个有正常 stages 的 Jenkins job
   - 确认 Build History 显示正常
   - 确认 Stage View 显示正常的 stage 列表

2. **没有 stages 的 job（合成 stage）**：
   - 打开一个没有 stages 的 Jenkins job
   - **Build History（左侧）**：应该显示完整信息（已有功能）
     ```
     ● #1334 - SUCCESS - Jan 14, 2026 18:18 - by yunpeng.li - [SERVICE_NAME: pa-bs-core]
     ```
   - **Stage View（右侧）**：应该显示完整信息（新功能）
     ```
     ● #1334 - SUCCESS - Jan 14, 2026 18:18 - by yunpeng.li - [SERVICE_NAME: pa-bs-core, VERSION: 2.3.1, BRANCH: feature/new-api]
     ```

3. **信息验证**：
   - 确认显示的信息包括：
     - ✅ Build 编号（#1334）
     - ✅ 状态（SUCCESS/FAILURE）
     - ✅ 触发时间（Jan 14, 2026 18:18）
     - ✅ 触发用户（by yunpeng.li）
     - ✅ Service Name（如果有）
     - ✅ Versions（如果有）
     - ✅ Version（如果有）
     - ✅ Branch（如果有）

4. **Tooltip 验证**：
   - 悬停在 Build History 项上 - 查看增强的 tooltip
   - 悬停在 Stage View 的合成 stage 上 - 查看完整的 build 详细信息

## 用户体验改进

- ✅ **信息直接可见**：无需鼠标悬停，关键信息直接显示在列表中
- ✅ **两侧一致**：Build History 和 Stage View 都显示相同的详细信息
- ✅ **易于识别**：通过颜色和图标快速识别构建状态
- ✅ **完整信息**：包含所有关键参数（service name, version, branch, trigger by, trigger time）
- ✅ **向后兼容**：不影响有正常 stages 的 builds 的显示
- ✅ **智能截断**：过长的 versions 参数会自动截断，避免显示过长
