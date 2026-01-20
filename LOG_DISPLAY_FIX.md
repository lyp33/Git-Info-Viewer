# Log 显示修复 - 换行和中文乱码

## 修改时间
2026-01-18

## 问题描述
1. **日志不换行**：所有内容挤在一行
2. **中文乱码**：中文字符显示为方框
3. **需要添加提示**：在日志前显示 "printing..."

## 根本原因

### 问题 1：日志不换行
`sendGetRequest()` 方法使用 `BufferedReader.readLine()` 读取每一行，但**没有添加换行符**：

```java
while ((inputLine = in.readLine()) != null) {
    response.append(inputLine);  // ❌ 缺少换行符
}
```

`readLine()` 会**去掉**行尾的换行符，所以必须手动添加回去。

### 问题 2：中文乱码
虽然使用了 `StandardCharsets.UTF_8`，但没有在 HTTP 请求头中明确指定字符集。

## 解决方案

### 1. 修复换行问题
在 `sendGetRequest()` 方法中，每读取一行后添加换行符：

```java
while ((inputLine = in.readLine()) != null) {
    response.append(inputLine);
    response.append("\n");  // ✅ 添加换行符
}
```

### 2. 修复中文乱码
添加 HTTP 请求头：

```java
conn.setRequestProperty("Accept-Charset", "UTF-8");
```

### 3. 添加 "printing..." 提示
在 `JenkinsStageLogDialog.loadStageLog()` 方法中：

```java
String displayLog = "printing...\n\n" + (log != null ? log : "");
logTextArea.setText(displayLog);
```

## 修改的文件

### 1. `src/main/java/com/gitviewer/JenkinsApiClient.java`
- 修改 `sendGetRequest()` 方法
  - 添加 `response.append("\n")` 保留换行符
  - 添加 `Accept-Charset: UTF-8` 请求头

### 2. `src/main/java/com/gitviewer/JenkinsStageLogDialog.java`
- 修改 `loadStageLog()` 方法
  - 在日志前添加 "printing...\n\n" 提示
  - 添加更多调试信息（检查换行符和中文字符）

## 编译结果
✅ 编译成功
- JAR 文件位置: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 测试要点
1. ✅ 日志应该正常换行显示（每行一行）
2. ✅ 中文字符应该正常显示（不是方框）
3. ✅ 日志顶部应该显示 "printing..."
4. ✅ 控制台应该输出换行符数量和是否包含中文的调试信息

## 技术说明

### BufferedReader.readLine() 的行为
- `readLine()` 读取一行文本，**不包括**行终止符（`\n` 或 `\r\n`）
- 如果需要保留原始格式，必须手动添加换行符
- 这是导致所有内容挤在一行的根本原因

### 字符编码
- Jenkins Console Log 使用 UTF-8 编码
- 必须在 HTTP 请求和响应读取时都使用 UTF-8
- `Accept-Charset` 请求头告诉服务器客户端期望的字符集
