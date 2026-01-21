# Jenkins CSRF Token (Crumb) 支持修复

## 问题描述

用户在使用 Jenkins Build 和 Rebuild 功能时遇到认证失败错误：
```
Authentication failed. Please check your Jenkins credentials.
```

但其他功能（查看 job、build history、console log 等）都正常工作。

## 根本原因

Jenkins 默认启用了 **CSRF Protection（跨站请求伪造保护）**：
- **GET 请求**（查看数据）：只需要基本的 username + API token 认证
- **POST 请求**（触发构建）：除了认证外，还需要额外的 **Jenkins Crumb**（CSRF Token）

应用之前的代码在发送 POST 请求时没有包含 Crumb，导致 Jenkins 返回 403 Forbidden 错误。

## 解决方案

### 1. 添加 Crumb 缓存字段

在 `JenkinsApiClient` 类中添加：
```java
// CSRF Token (Crumb) 缓存
private String cachedCrumb = null;
private String cachedCrumbField = null;
```

### 2. 实现 Crumb 获取方法（带详细日志）

```java
private boolean fetchCrumb() {
    try {
        String crumbUrl = baseUrl + "/crumbIssuer/api/json";
        System.out.println("[JenkinsApiClient] === Fetching Jenkins Crumb ===");
        System.out.println("[JenkinsApiClient] Crumb URL: " + crumbUrl);
        
        String response = sendGetRequest(crumbUrl);
        System.out.println("[JenkinsApiClient] Crumb response: " + response);
        
        JSONObject json = new JSONObject(response);
        
        if (json.has("crumb") && json.has("crumbRequestField")) {
            cachedCrumb = json.getString("crumb");
            cachedCrumbField = json.getString("crumbRequestField");
            System.out.println("[JenkinsApiClient] ✓ Crumb field: " + cachedCrumbField);
            System.out.println("[JenkinsApiClient] ✓ Crumb value: " + cachedCrumb);
            return true;
        }
        return false;
    } catch (Exception e) {
        System.out.println("[JenkinsApiClient] ✗ Failed to fetch crumb: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}
```

### 3. 修改 POST 请求方法（增强日志）

在 `sendPostRequest()` 方法中：
1. **自动获取 Crumb**：如果缓存中没有 Crumb，先调用 `fetchCrumb()`
2. **添加 Crumb Header**：在 HTTP 请求头中添加 `Jenkins-Crumb: <value>`
3. **自动重试机制**：如果收到 403 错误，尝试刷新 Crumb 并重试一次
4. **详细日志输出**：每一步都输出到控制台，方便调试

```java
System.out.println("[JenkinsApiClient] === POST Request ===");
System.out.println("[JenkinsApiClient] POST URL: " + urlString);

// 如果还没有获取 Crumb，先获取
if (cachedCrumb == null) {
    System.out.println("[JenkinsApiClient] No cached crumb, fetching...");
    fetchCrumb();
} else {
    System.out.println("[JenkinsApiClient] Using cached crumb: " + cachedCrumb);
}

// 添加 CSRF Token (Crumb)
if (cachedCrumb != null && cachedCrumbField != null) {
    conn.setRequestProperty(cachedCrumbField, cachedCrumb);
    System.out.println("[JenkinsApiClient] ✓ Added crumb header: " + cachedCrumbField + "=" + cachedCrumb);
} else {
    System.out.println("[JenkinsApiClient] ✗ No crumb available to add to request");
}

System.out.println("[JenkinsApiClient] Response code: " + responseCode);
```

## 调试日志示例

当你触发 Build 时，控制台会输出：

```
[JenkinsApiClient] === POST Request ===
[JenkinsApiClient] POST URL: http://jenkins-server/job/my-job/build
[JenkinsApiClient] No cached crumb, fetching...
[JenkinsApiClient] === Fetching Jenkins Crumb ===
[JenkinsApiClient] Crumb URL: http://jenkins-server/crumbIssuer/api/json
[JenkinsApiClient] Crumb response: {"crumb":"a1b2c3d4...","crumbRequestField":"Jenkins-Crumb"}
[JenkinsApiClient] ✓ Crumb field: Jenkins-Crumb
[JenkinsApiClient] ✓ Crumb value: a1b2c3d4...
[JenkinsApiClient] ✓ Added Authorization header
[JenkinsApiClient] ✓ Added crumb header: Jenkins-Crumb=a1b2c3d4...
[JenkinsApiClient] POST data length: 0
[JenkinsApiClient] Response code: 201
[JenkinsApiClient] ✓ POST request successful
```

## 技术细节

### Jenkins Crumb API

**请求**：
```
GET /crumbIssuer/api/json
Authorization: Basic <base64(username:token)>
```

**响应**：
```json
{
  "crumb": "a1b2c3d4e5f6...",
  "crumbRequestField": "Jenkins-Crumb"
}
```

### POST 请求示例

**修复前**（失败）：
```
POST /job/my-job/build
Authorization: Basic <credentials>
Content-Type: application/x-www-form-urlencoded
```

**修复后**（成功）：
```
POST /job/my-job/build
Authorization: Basic <credentials>
Jenkins-Crumb: a1b2c3d4e5f6...
Content-Type: application/x-www-form-urlencoded
```

## 兼容性

该修复方案具有良好的兼容性：

1. **启用 CSRF 的 Jenkins**：自动获取并使用 Crumb
2. **未启用 CSRF 的 Jenkins**：获取 Crumb 失败时不会报错，继续正常执行
3. **Crumb 过期**：自动检测 403 错误并刷新 Crumb 重试

## 影响范围

修改影响以下功能：
- ✅ **Build**：触发新构建
- ✅ **Rebuild**：重新构建历史版本
- ✅ 所有其他 POST 请求（如果将来添加）

不影响现有功能：
- ✅ 查看 Job 列表
- ✅ 查看 Build History
- ✅ 查看 Console Log
- ✅ 查看 Stage 信息

## 测试建议

1. **重新运行应用**（使用最新的 JAR）：
   ```bash
   java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```

2. **测试 Build 功能**：
   - 打开 Jenkins Browser
   - 选择一个 Job
   - 点击 "Build" 按钮
   - **查看控制台输出**，应该看到详细的 Crumb 获取日志
   - 验证构建成功触发

3. **测试 Rebuild 功能**：
   - 打开 Build History
   - 选择一个历史构建
   - 点击 "Rebuild" 按钮
   - **查看控制台输出**
   - 验证构建成功触发

4. **检查日志**：
   - 应该看到 `✓ Crumb field: Jenkins-Crumb`
   - 应该看到 `✓ Crumb value: <token>`
   - 应该看到 `✓ Added crumb header`
   - 应该看到 `Response code: 201` 或 `200`

## 如果还是失败

如果看到日志输出：
- `✗ Failed to fetch crumb` - 说明无法获取 Crumb，可能是网络问题或 Jenkins 配置问题
- `✗ No crumb available` - 说明 Crumb 获取失败，但请求继续发送
- `Response code: 403` - 说明即使有 Crumb 也被拒绝，可能是权限问题

请将**完整的控制台日志**发给我，我可以帮你诊断具体问题。

## 文件修改

- **修改文件**：`src/main/java/com/gitviewer/JenkinsApiClient.java`
- **修改内容**：
  - 添加 Crumb 缓存字段（2 个字段）
  - 添加 `fetchCrumb()` 方法（约 40 行，含详细日志）
  - 修改 `sendPostRequest()` 方法（添加约 40 行，含详细日志）

## 编译和部署

```bash
# 关闭正在运行的应用
taskkill /F /IM java.exe

# 编译
mvn clean compile

# 打包
mvn package -DskipTests

# 运行（使用最新的 JAR）
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

## 总结

通过添加 Jenkins CSRF Token (Crumb) 支持和详细的调试日志，现在可以：

- ✅ 自动获取和缓存 Crumb
- ✅ 自动在 POST 请求中添加 Crumb
- ✅ 支持 Crumb 过期自动刷新
- ✅ 兼容未启用 CSRF 的 Jenkins
- ✅ 不影响现有功能
- ✅ **详细的控制台日志输出，方便调试**
- ✅ 编译和打包成功

**请确保使用最新打包的 JAR 文件，并查看控制台日志输出！**

---

**修复日期**：2026-01-21  
**修复版本**：1.0.0  
**更新**：增强日志输出，便于调试
