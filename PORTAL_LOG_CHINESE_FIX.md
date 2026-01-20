# Portal Log 中文乱码修复

## 修改时间
2026-01-19 00:32

## 问题描述
Portal Log 中显示的中文字符显示为方框（乱码），无法正常阅读。

## 问题原因
Portal API 返回的 JSON 响应中，中文字符被编码为 Unicode 转义序列（例如 `\u4e2d\u6587`），而 Java 的 `JSONObject.getString()` 方法不会自动解码这些转义序列。

## 解决方案

### 1. 添加 Unicode 解码方法

在 `JenkinsApiClient.java` 中添加 `decodeUnicodeEscapes()` 方法：

```java
/**
 * 解码 Unicode 转义序列（例如 \\u4e2d\\u6587）
 * 
 * @param input 包含 Unicode 转义的字符串
 * @return 解码后的字符串
 */
private String decodeUnicodeEscapes(String input) {
    if (input == null || input.isEmpty()) {
        return input;
    }
    
    StringBuilder result = new StringBuilder();
    int i = 0;
    
    while (i < input.length()) {
        if (i < input.length() - 5 && input.charAt(i) == '\\' && input.charAt(i + 1) == 'u') {
            // 找到 \\uXXXX 格式的 Unicode 转义
            try {
                String hex = input.substring(i + 2, i + 6);
                int codePoint = Integer.parseInt(hex, 16);
                result.append((char) codePoint);
                i += 6;
            } catch (NumberFormatException e) {
                // 如果不是有效的十六进制，保留原样
                result.append(input.charAt(i));
                i++;
            }
        } else {
            result.append(input.charAt(i));
            i++;
        }
    }
    
    return result.toString();
}
```

### 2. 修改 `fetchPortalBuildOutputWithInfo()` 方法

在提取 `build_output` 后，检查是否包含 Unicode 转义序列，如果有则解码：

```java
if (json.has("build_output")) {
    String buildOutput = json.getString("build_output");
    System.out.println("[JenkinsApiClient] Extracted build_output, length: " + buildOutput.length());
    
    // 检查是否包含 Unicode 转义序列
    if (buildOutput.contains("\\u")) {
        System.out.println("[JenkinsApiClient] Detected Unicode escape sequences, decoding...");
        buildOutput = decodeUnicodeEscapes(buildOutput);
        System.out.println("[JenkinsApiClient] After decoding, length: " + buildOutput.length());
    }
    
    return buildOutput;
}
```

## 技术细节

### Unicode 转义格式
- 格式：`\uXXXX`（其中 XXXX 是 4 位十六进制数）
- 例如：
  - `\u4e2d` → 中
  - `\u6587` → 文
  - `\u6784` → 构
  - `\u5efa` → 建

### 解码算法
1. 遍历字符串
2. 检测 `\u` 序列
3. 提取后面的 4 位十六进制数
4. 将十六进制转换为整数（Unicode 码点）
5. 将码点转换为字符
6. 追加到结果字符串

### 错误处理
- 如果十六进制格式无效，保留原始字符
- 如果字符串长度不足，保留原始字符

## 编译注意事项

⚠️ **重要**：Java 编译器会在编译时处理源代码中的 Unicode 转义序列，因此在注释中使用 `\u` 时需要转义为 `\\u`，否则会导致编译错误：

```
[ERROR] 非法的 Unicode 转义
```

## 测试验证

### 测试步骤
1. 双击 Stage 打开对话框
2. 切换到 "Portal Log" tab
3. 等待加载完成
4. 检查中文字符是否正常显示

### 预期结果
- ✅ 中文字符正常显示（不再是方框）
- ✅ 日志内容完整可读
- ✅ 不影响其他字符的显示

## 相关文件

- `src/main/java/com/gitviewer/JenkinsApiClient.java` - 添加 Unicode 解码方法

## 编译状态

✅ 编译成功
✅ 打包成功

生成的 JAR 文件：
- `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`
