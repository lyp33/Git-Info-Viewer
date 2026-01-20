# Build Parameters 调试指南

## 问题

点击"Build with Parameters"按钮时，系统没有显示参数的默认值。

## 当前逻辑（应该是正确的）

1. 点击"Build with Parameters"按钮
2. 获取最新构建的参数值
3. 对 `versions` 参数自动递增版本号
4. 用这些值作为预填充值
5. 对话框优先使用预填充值，如果没有预填充值才使用默认值

## 如何调试

### 1. 从命令行运行应用

```bash
java -jar git-info-viewer-1.0.0-jar-with-dependencies.jar
```

这样可以看到控制台输出的调试信息。

### 2. 打开 Jenkins 作业详情

导航到一个有参数的 Jenkins 作业。

### 3. 点击"Build with Parameters"按钮

观察控制台输出。

### 4. 查看日志输出

#### 在 Job Details Dialog 的 Console Log 中：

```
[01:36:00] Opening build parameters dialog...
[01:36:00] Found latest build #809
[01:36:00] Fetched 5 parameters from latest build
[01:36:00]   Param: branch = master
[01:36:00]   Param: environment = prod
[01:36:00]   Param: versions = 1.2.3
[01:36:00] Incremented versions: 1.2.3 -> 1.2.4
```

#### 在系统控制台中：

```
=== Build Parameters Definition ===
Total parameters: 5
Parameter: branch
  Type: StringParameterDefinition
  Default Value: develop
  Description: Git branch to build
Parameter: environment
  Type: ChoiceParameterDefinition
  Default Value: dev
  Description: Target environment
  Choices: [dev, test, prod]
Parameter: versions
  Type: StringParameterDefinition
  Default Value: 1.0.0
  Description: Version numbers
...
=== Prefilled Parameters ===
  branch = master
  environment = prod
  versions = 1.2.4
===================================
Using prefilled value for branch: master
Using prefilled value for environment: prod
Using prefilled value for versions: 1.2.4
Using default value for param4: default_value
```

## 需要检查的问题

### 1. 参数定义的默认值是否为 null？

查看日志中的 "Default Value" 字段。如果为 null，说明 Jenkins API 没有返回默认值。

### 2. 预填充参数是否覆盖了所有参数？

查看 "Prefilled Parameters" 部分。如果某个参数在这里有值，就会覆盖默认值。

### 3. 最新构建的参数值是否为空字符串？

如果 `fetchBuildParametersForRebuild` 返回的参数值为空字符串，会覆盖默认值。

### 4. API 返回的参数定义格式是否正确？

检查 `fetchBuildParameters` 方法是否正确解析了 `defaultParameterValue`。

## 可能的问题和解决方案

### 问题 1：Jenkins API 没有返回默认值

**原因**：某些 Jenkins 版本或参数类型可能不返回 `defaultParameterValue`。

**解决方案**：修改 API 请求，使用不同的 tree 参数。

### 问题 2：预填充参数包含空字符串

**原因**：`fetchBuildParametersForRebuild` 可能返回空字符串值。

**解决方案**：在 `fetchBuildParametersForRebuild` 中过滤空字符串：

```java
if (value != null && !value.toString().trim().isEmpty()) {
    parameters.put(name, value.toString());
}
```

### 问题 3：参数定义解析错误

**原因**：`defaultParameterValue` 的 JSON 结构可能不同。

**解决方案**：添加更详细的日志，查看原始 JSON 响应。

## 下一步

1. 运行调试版本
2. 查看日志输出
3. 根据日志确定问题所在
4. 实施相应的修复方案

## 文件

- `git-info-viewer-build-params-debug.zip` - 包含调试日志的版本
