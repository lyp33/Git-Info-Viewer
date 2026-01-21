# Tenant CI/CD Tooltip - Enhanced Debug Logging

## 目的
为了诊断提示框字段缺失问题，添加了详细的控制台日志输出，以便追踪数据流向。

## 修改内容

### 1. PortalApiClient.java - parseBuildResultFromJson()

添加了详细的日志输出，包括：

#### 输出完整的原始 JSON
```java
System.out.println("========================================");
System.out.println("=== parseBuildResultFromJson: RAW JSON ===");
System.out.println("========================================");
System.out.println(json.toString(2));  // 格式化输出，缩进2个空格
```

#### 逐字段提取日志
每个字段提取后都会输出：
- `id`: 记录ID
- `queue_id`: 队列ID（包括原始long值）
- `app_name`: 应用名称
- `image_name`: 镜像名称
- `create_time`: 创建时间
- `modify_time`: 修改时间
- `creator`: 创建者 ⭐
- `package_title`: 包标题 ⭐
- `build_status`: 构建状态（从callback节点）
- `version`: 版本（从request_parameters节点）
- `git_branch`: Git分支（从request_parameters节点）

#### 汇总输出
```java
System.out.println("========================================");
System.out.println("=== SUMMARY ===");
System.out.println("  id: " + buildResult.getId());
System.out.println("  queueId: " + buildResult.getQueueId());
System.out.println("  appName: " + buildResult.getAppName());
System.out.println("  creator: " + buildResult.getCreator());
System.out.println("  packageTitle: " + buildResult.getPackageTitle());
// ... 其他字段
```

### 2. TenantCICDDialog.java - showRowTooltip()

添加了详细的提示框显示日志：

#### 方法调用信息
```java
System.out.println("========================================");
System.out.println("=== TOOLTIP: showRowTooltip called ===");
System.out.println("  viewRow: " + viewRow);
System.out.println("  modelRow: " + modelRow);
System.out.println("  Total results: " + results.size());
```

#### 原始 JSON 输出
```java
System.out.println("========================================");
System.out.println("=== TOOLTIP: RAW JSON FROM BuildResult ===");
System.out.println(rawJson);
System.out.println("========================================");
```

#### 逐字段提取日志
每个字段从 JSON 提取后都会输出：
```java
System.out.println("  Extracted id: [" + id + "]");
System.out.println("  Extracted queue_id: [" + queueId + "]");
System.out.println("  Extracted creator: [" + creator + "]");
System.out.println("  Extracted package_title: [" + packageTitle + "]");
// ... 其他字段
```

#### 最终值汇总
```java
System.out.println("========================================");
System.out.println("=== TOOLTIP: FINAL VALUES ===");
System.out.println("  ID: " + id);
System.out.println("  Queue ID: " + queueId);
System.out.println("  Creator: " + creator);
System.out.println("  Package Title: " + packageTitle);
// ... 其他字段
```

## 日志输出示例

### 查询时的日志（parseBuildResultFromJson）
```
========================================
=== parseBuildResultFromJson: RAW JSON ===
========================================
{
  "id": "696f6790da7ff2f6fdfc2de8",
  "queue_id": 1785762,
  "app_name": "thailife-bs",
  "creator": "yunpeng.li@insuremo.com",
  "package_title": "v202601200722-20260120113127",
  ...
}
========================================
Raw JSON stored, length: 2345
Extracted id: [696f6790da7ff2f6fdfc2de8]
Extracted queue_id: [1785762] (raw long: 1785762)
Extracted app_name: [thailife-bs]
Extracted creator: [yunpeng.li@insuremo.com]
Extracted package_title: [v202601200722-20260120113127]
...
========================================
=== SUMMARY ===
  id: 696f6790da7ff2f6fdfc2de8
  queueId: 1785762
  creator: yunpeng.li@insuremo.com
  packageTitle: v202601200722-20260120113127
========================================
```

### 鼠标悬停时的日志（showRowTooltip）
```
========================================
=== TOOLTIP: showRowTooltip called ===
  viewRow: 0
  modelRow: 0
  Total results: 10
  BuildResult retrieved
  Raw JSON length: 2345
========================================
=== TOOLTIP: RAW JSON FROM BuildResult ===
{
  "id": "696f6790da7ff2f6fdfc2de8",
  "queue_id": 1785762,
  "creator": "yunpeng.li@insuremo.com",
  ...
}
========================================
  JSON parsed successfully
  Extracted id: [696f6790da7ff2f6fdfc2de8]
  Extracted queue_id: [1785762]
  Extracted creator: [yunpeng.li@insuremo.com]
  Extracted package_title: [v202601200722-20260120113127]
...
========================================
=== TOOLTIP: FINAL VALUES ===
  ID: 696f6790da7ff2f6fdfc2de8
  Queue ID: 1785762
  Creator: yunpeng.li@insuremo.com
  Package Title: v202601200722-20260120113127
========================================
```

## 诊断步骤

1. **启动应用**：`java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

2. **查询数据**：
   - 连接到租户
   - 执行查询
   - 观察控制台输出的 `parseBuildResultFromJson` 日志
   - 检查原始 JSON 是否包含 `creator` 和 `package_title` 字段

3. **测试提示框**：
   - 将鼠标悬停在结果表格的某一行上
   - 等待 1 秒
   - 观察控制台输出的 `showRowTooltip` 日志
   - 检查从 JSON 提取的字段值

4. **对比数据**：
   - 对比 `parseBuildResultFromJson` 和 `showRowTooltip` 中的字段值
   - 确认数据是否在传递过程中丢失

## 可能的问题场景

### 场景 1：API 响应中字段为空
如果在 `parseBuildResultFromJson` 的原始 JSON 中就看到：
```
Extracted creator: []
Extracted package_title: []
```
说明 API 返回的数据本身就没有这些字段。

### 场景 2：JSON 存储失败
如果在 `showRowTooltip` 中看到：
```
WARNING: No raw JSON data, using BuildResult fields
```
说明原始 JSON 没有被正确存储到 BuildResult 对象中。

### 场景 3：JSON 解析失败
如果在 `showRowTooltip` 中看到：
```
ERROR: Failed to parse raw JSON
```
说明存储的 JSON 字符串无法被解析。

### 场景 4：字段提取失败
如果原始 JSON 中有值，但提取后为空：
```
=== TOOLTIP: RAW JSON FROM BuildResult ===
{ "creator": "yunpeng.li@insuremo.com", ... }
...
Extracted creator: []
```
说明字段名称或提取逻辑有问题。

## 下一步

根据控制台日志输出，我们可以准确定位问题所在：
1. 如果是 API 返回数据问题 → 检查 API 调用参数
2. 如果是存储问题 → 检查 `setRawJsonData()` 调用
3. 如果是解析问题 → 检查 JSON 格式和解析逻辑
4. 如果是提取问题 → 检查字段名称是否匹配

## 文件修改
- `src/main/java/com/gitviewer/PortalApiClient.java` - 添加详细解析日志
- `src/main/java/com/gitviewer/TenantCICDDialog.java` - 添加详细提示框日志

## 编译状态
✅ 编译成功
✅ 打包完成：`target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
