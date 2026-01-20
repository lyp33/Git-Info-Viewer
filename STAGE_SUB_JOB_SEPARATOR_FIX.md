# Stage Sub-Job Log - 分隔符修复

## 修改时间
2026-01-18

## 问题描述
之前代码使用 `?` 作为分隔符，但实际 Jenkins 日志中使用的是 **`»`**（右双尖括号，Unicode: U+00BB）

## 实际格式
```
Starting building: gemini » Manual-Build » thailifesdk » 24.08_thailife_dev » CI-Robot » BS-CI-ROBOT #578
```

**注意**：分隔符是 `»` (U+00BB)，不是 `?`

## 解决方案

### 1. 更新正则表达式
将所有匹配模式中的 `\?` 替换为 `»`

#### 修改前：
```java
Pattern pattern1 = Pattern.compile("Starting building:\\s+gemini\\s*\\?\\s*([^#\\n]+?)\\s*#");
```

#### 修改后：
```java
Pattern pattern1 = Pattern.compile("Starting building:\\s+gemini\\s*»\\s*([^#\\n]+?)\\s*#");
```

### 2. 更新路径分割
将分割符从 `?` 改为 `»`

#### 修改前：
```java
String[] parts = jobPath.split("\\s*\\?\\s*");
```

#### 修改后：
```java
String[] parts = jobPath.split("\\s*»\\s*");
```

## 修改的文件
- `src/main/java/com/gitviewer/JenkinsApiClient.java`
  - `extractJobPathFromStageLog()` 方法：更新三个正则表达式模式
  - `constructSubJobUrl()` 方法：更新路径分割逻辑

## 示例

### 输入（Stage Log）：
```
Starting building: gemini » Manual-Build » thailifesdk » 24.08_thailife_dev » CI-Robot » BS-CI-ROBOT #578
```

### 提取结果：
- **作业路径**：`Manual-Build » thailifesdk » 24.08_thailife_dev » CI-Robot » BS-CI-ROBOT`
- **构建 ID**：`578`

### 构建的 URL：
```
http://jenkins/job/gemini/job/Manual-Build/job/thailifesdk/job/24.08_thailife_dev/job/CI-Robot/job/BS-CI-ROBOT/578/consoleText
```

## 编译结果
✅ 编译成功
- JAR 文件位置: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 字符说明
- **`»`** = 右双尖括号 (Right-Pointing Double Angle Quotation Mark)
- Unicode: U+00BB
- HTML Entity: `&raquo;`
- 在某些字体/编码下可能显示为 `?`（乱码）

## 测试建议
1. 双击包含 `»` 分隔符的 Stage
2. 检查控制台日志，确认正确提取作业路径
3. 验证 URL 构建正确
4. 确认显示的是子作业的完整 Console Log
