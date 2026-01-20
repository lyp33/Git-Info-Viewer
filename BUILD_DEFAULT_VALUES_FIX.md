# Build Parameters Debug Enhancement

## 问题描述

点击"Build with Parameters"按钮时，系统没有显示参数的默认值。

## 当前逻辑

点击"Build with Parameters"按钮时：
1. 获取最新构建的参数值
2. 对 `versions` 参数自动递增版本号
3. 用这些值作为预填充值传给对话框
4. 对话框优先使用预填充值，如果没有预填充值才使用默认值

这个逻辑是正确的，但需要调试为什么默认值没有显示。

## 添加的调试日志

### JenkinsJobDetailsDialog.java

在 `openBuildParametersDialog()` 方法中添加：
- 打印获取到的参数数量
- 打印每个参数的名称和值
- 打印 versions 参数递增前后的值

### JenkinsBuildParametersDialog.java

在 `loadParametersAndInitUI()` 方法中添加：
- 打印参数定义信息（名称、类型、默认值、描述、选项）
- 打印预填充参数信息

在 `getParameterValue()` 方法中添加：
- 打印每个参数使用的是预填充值还是默认值
- 打印实际使用的值

## 测试步骤

1. 从命令行运行应用：`java -jar git-info-viewer-1.0.0-jar-with-dependencies.jar`
2. 打开一个 Jenkins 作业详情
3. 点击"Build with Parameters"按钮
4. 查看控制台输出的调试信息：
   - 参数定义（包括默认值）
   - 预填充参数（从最新构建获取）
   - 每个参数实际使用的值

## 预期日志输出

```
[01:36:00] Opening build parameters dialog...
[01:36:00] Found latest build #809
[01:36:00] Fetched 5 parameters from latest build
[01:36:00]   Param: branch = master
[01:36:00]   Param: environment = prod
[01:36:00]   Param: versions = 1.2.3
[01:36:00] Incremented versions: 1.2.3 -> 1.2.4
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

## 下一步

根据日志输出，我们可以确定：
1. 参数定义是否正确获取（包括默认值）
2. 预填充参数是否正确获取
3. 哪些参数使用了预填充值，哪些使用了默认值
4. 是否有参数既没有预填充值也没有默认值

## 文件修改

- `src/main/java/com/gitviewer/JenkinsJobDetailsDialog.java` - 添加参数获取日志
- `src/main/java/com/gitviewer/JenkinsBuildParametersDialog.java` - 添加参数定义和值选择日志

## 日期

2026-01-18

