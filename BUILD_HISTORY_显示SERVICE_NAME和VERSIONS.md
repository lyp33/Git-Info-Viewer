# Build History 显示 SERVICE_NAME 和 VERSIONS

## 功能说明

在 Build History 列表中显示构建参数 `SERVICE_NAME` 和 `versions`（如果存在）。

## 修改内容

### JenkinsBuild.java

更新了 `extractKeyParameters()` 方法，优先显示 `SERVICE_NAME` 和 `versions` 参数。

#### 修改前
```java
/**
 * 提取关键参数（VERSION, BRANCH, TAG）
 * 优先显示 versions 参数（如果存在）
 */
public String extractKeyParameters() {
    // 优先查找 versions 参数
    if (parameters.containsKey("versions")) {
        // 显示 versions
        return "[versions: " + versions + "]";
    }
    
    // 查找其他关键参数
    String[] keyNames = {"VERSION", "BRANCH", "TAG", ...};
    // ...
}
```

#### 修改后
```java
/**
 * 提取关键参数（SERVICE_NAME, VERSIONS, VERSION, BRANCH, TAG）
 * 优先显示 SERVICE_NAME 和 versions 参数（如果存在）
 */
public String extractKeyParameters() {
    StringBuilder sb = new StringBuilder();
    
    // 1. 优先显示 SERVICE_NAME
    if (parameters.containsKey("SERVICE_NAME")) {
        String serviceName = parameters.get("SERVICE_NAME");
        if (serviceName != null && !serviceName.isEmpty()) {
            sb.append("SERVICE_NAME: ").append(serviceName);
        }
    }
    
    // 2. 显示 versions 参数
    if (parameters.containsKey("versions")) {
        String versions = parameters.get("versions");
        if (versions != null && !versions.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            // 截断过长的值
            if (versions.length() > 50) {
                versions = versions.substring(0, 47) + "...";
            }
            sb.append("versions: ").append(versions);
        }
    }
    
    // 3. 如果已经有 SERVICE_NAME 或 versions，直接返回
    if (sb.length() > 0) {
        return "[" + sb.toString() + "]";
    }
    
    // 4. 否则查找其他关键参数
    String[] keyNames = {"VERSION", "BRANCH", "TAG", "version", "branch", "tag", "TENANT_NAME"};
    // ...
}
```

## 显示优先级

1. **SERVICE_NAME** - 最高优先级
2. **versions** - 第二优先级
3. **VERSION, BRANCH, TAG** - 如果前两者都不存在，才显示这些

## 显示格式

### 示例 1：同时存在 SERVICE_NAME 和 versions
```
● #2074 - SUCCESS - Jan 17, 2026 17:38 - by dttl.kthoo - [SERVICE_NAME: gemini-claim-bff, versions: 24.08_thailife_devsdk_v0.056]
```

### 示例 2：只有 SERVICE_NAME
```
● #2073 - SUCCESS - Jan 17, 2026 16:54 - by dttl.kthoo - [SERVICE_NAME: gemini-claim-bff]
```

### 示例 3：只有 versions
```
● #2072 - SUCCESS - Jan 17, 2026 11:31 - by dttl.kthoo - [versions: 24.08_thailife_devsdk_v0.056]
```

### 示例 4：都不存在，显示其他参数
```
● #2071 - SUCCESS - Jan 17, 2026 10:37 - by Unknown - [BRANCH: 24.08_thailife_dev]
```

## 长度限制

为了避免显示过长，对参数值进行了截断：

- **versions**: 最多显示 50 个字符，超过则截断并添加 "..."
- **其他参数**: 最多显示 30 个字符，超过则截断并添加 "..."

## 编译状态

✅ 编译成功 (mvn compile)

## 测试步骤

1. 关闭正在运行的应用程序
2. 运行 `mvn clean package` 重新打包
3. 启动应用程序
4. 打开 Jenkins Browser
5. 双击某个 Job
6. 查看 Build History 列表

## 预期结果

- ✅ 如果 Build 有 `SERVICE_NAME` 参数，显示在列表中
- ✅ 如果 Build 有 `versions` 参数，显示在列表中
- ✅ 如果两者都有，同时显示（用逗号分隔）
- ✅ 如果都没有，显示其他关键参数（VERSION, BRANCH, TAG 等）
- ✅ 过长的参数值会被截断

## 相关文件

- `src/main/java/com/gitviewer/JenkinsBuild.java` - 修改了 `extractKeyParameters()` 方法
- `src/main/java/com/gitviewer/JenkinsJobDetailsDialog.java` - 使用 `getFormattedDisplay()` 显示 Build 信息

## 注意事项

1. **参数名称大小写敏感**：`SERVICE_NAME` 和 `service_name` 是不同的参数
2. **参数必须存在**：只有在 Build 参数中实际存在这些参数时才会显示
3. **显示顺序**：SERVICE_NAME 在前，versions 在后
4. **长度限制**：过长的值会被自动截断
