# Portal Log 换行符处理修复

## 问题描述

Portal API 返回的 `build_output` 内容显示在一行上，没有正确处理换行符。

## 原因分析

Portal API 返回的 JSON 中，`build_output` 字段可能包含转义的换行符（如 `\n`）和其他转义序列。虽然 `JSONObject.getString()` 会自动解码一些转义序列，但在某些情况下（如双重转义），需要手动处理。

### 常见转义序列

- `\n` - 换行符
- `\r` - 回车符
- `\t` - 制表符
- `\\` - 反斜杠
- `\"` - 双引号
- `\uXXXX` - Unicode 字符

## 解决方案

增强 `decodeUnicodeEscapes` 方法，使其能够处理所有常见的转义序列，不仅仅是 Unicode 转义。

### 修改内容

**文件**: `src/main/java/com/gitviewer/JenkinsApiClient.java`

#### 1. 增强 `decodeUnicodeEscapes` 方法

**修改前**（只处理 Unicode 转义）：
```java
private String decodeUnicodeEscapes(String input) {
    // 只处理 \uXXXX 格式
    if (input.charAt(i) == '\\' && input.charAt(i + 1) == 'u') {
        // 解码 Unicode
    }
}
```

**修改后**（处理所有转义序列）：
```java
private String decodeUnicodeEscapes(String input) {
    while (i < input.length()) {
        if (input.charAt(i) == '\\') {
            char nextChar = input.charAt(i + 1);
            switch (nextChar) {
                case 'n':  // 换行符
                    result.append('\n');
                    break;
                case 'r':  // 回车符
                    result.append('\r');
                    break;
                case 't':  // 制表符
                    result.append('\t');
                    break;
                case '\\': // 反斜杠
                    result.append('\\');
                    break;
                case '"':  // 双引号
                    result.append('"');
                    break;
                case 'u':  // Unicode 转义
                    // 解码 \uXXXX
                    break;
            }
        }
    }
}
```

#### 2. 更新检测逻辑

**修改前**：
```java
if (buildOutput.contains("\\u")) {
    buildOutput = decodeUnicodeEscapes(buildOutput);
}
```

**修改后**：
```java
// 检查是否包含任何转义序列
boolean hasEscapes = buildOutput.indexOf('\\') >= 0 && 
                    (buildOutput.contains("\\n") || buildOutput.contains("\\r") || 
                     buildOutput.contains("\\t") || buildOutput.indexOf("\\u") >= 0);

if (hasEscapes) {
    buildOutput = decodeUnicodeEscapes(buildOutput);
    
    // 输出换行符数量用于调试
    int newlineCount = buildOutput.split("\n").length - 1;
    System.out.println("[JenkinsApiClient] Newline count after decoding: " + newlineCount);
}
```

## 处理流程

1. **从 Portal API 获取 JSON 响应**
2. **提取 `build_output` 字段**（`JSONObject.getString()` 会进行第一次解码）
3. **检查是否包含转义序列**（`\n`, `\r`, `\t`, `\u`）
4. **如果包含，调用 `decodeUnicodeEscapes` 进行第二次解码**
5. **显示解码后的内容**（包含正确的换行符）

## 示例

### 输入（Portal API 返回）：
```json
{
  "build_output": "Line 1\\nLine 2\\nLine 3\\n中文测试\\u4e2d\\u6587"
}
```

### 第一次解码（JSONObject.getString）：
```
Line 1\nLine 2\nLine 3\n中文测试\u4e2d\u6587
```

### 第二次解码（decodeUnicodeEscapes）：
```
Line 1
Line 2
Line 3
中文测试中文
```

## 调试日志

```
[JenkinsApiClient] Extracted build_output, length: 45
[JenkinsApiClient] Detected escape sequences, decoding...
[JenkinsApiClient] After decoding, length: 42
[JenkinsApiClient] Newline count after decoding: 3
```

## 注意事项

### Java 编译器的 Unicode 转义

Java 编译器会在编译时处理源代码中的 `\uXXXX` 序列，包括在注释和字符串字面量中。因此：

**错误写法**（编译失败）：
```java
// Unicode 转义 \uXXXX  ❌ 编译器会尝试解释 \uXXXX
```

**正确写法**：
```java
// Unicode 转义 (backslash-u-XXXX format)  ✓ 使用文字描述
```

## 编译和部署

```bash
mvn clean package
```

生成文件: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 测试验证

1. 打开应用程序
2. 进入 Jenkins Browser
3. 双击任意 Stage 打开 Stage Log 对话框
4. 切换到 Portal Log 标签页
5. 验证 `build_output` 内容：
   - 应该有多行显示
   - 换行符应该正确处理
   - 中文字符应该正确显示
6. 查看控制台日志：
   - 应该看到 "Detected escape sequences, decoding..."
   - 应该看到 "Newline count after decoding: X"

## 相关文件

- `src/main/java/com/gitviewer/JenkinsApiClient.java` - 转义序列解码逻辑

## 完成时间

2026-01-20 17:38
