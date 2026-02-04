# Rebuild 功能实现

## 功能描述

在 Tenant CI/CD 对话框中添加了右键菜单的 Rebuild 功能，允许用户对失败的构建记录进行重新构建。

## 功能特性

### 1. 右键菜单触发

- 在构建结果表格中右键点击任意行
- 只有 **Build Fail** 状态的记录才会显示 Rebuild 选项
- 其他状态（Build Success、Build Start等）不显示菜单

### 2. 智能数据提取

从失败的构建记录中自动提取以下信息：
- **App Name**: 应用名称
- **Version**: 版本号
- **Git Branch**: Git分支
- **Tenant**: 租户代码

### 3. 确认对话框

点击 Rebuild 后会显示确认对话框，展示将要重新构建的信息：
```
Rebuild the following build?

App Name: thailife-bs
Version: v20260120194501
Git Branch: dev
```

用户可以选择确认或取消操作。

### 4. API 调用

使用 Portal API 的单应用构建接口：
```
POST /api/mo-fo/1.0/ops/v2/build?clear_job=true&silences=true&force=false
```

请求体包含：
- `app_name`: 应用名称
- `version`: 版本号
- `git_branch`: Git分支
- `build_type`: "build_only"
- `user_name`: 租户代码
- 其他必要参数

### 5. 自动刷新

Rebuild 请求提交成功后：
- 显示成功提示对话框
- 自动触发搜索刷新，更新构建结果列表
- 用户可以看到新提交的构建记录

## 实现细节

### UI 修改

**文件**: `src/main/java/com/gitviewer/TenantCICDDialog.java`

#### 1. 添加右键菜单监听器

```java
resultsTable.addMouseListener(new java.awt.event.MouseAdapter() {
    @Override
    public void mousePressed(java.awt.event.MouseEvent e) {
        if (e.isPopupTrigger()) {
            showContextMenu(e);
        }
    }
    
    @Override
    public void mouseReleased(java.awt.event.MouseEvent e) {
        if (e.isPopupTrigger()) {
            showContextMenu(e);
        }
    }
});
```

#### 2. showContextMenu() 方法

- 检查点击位置是否在有效行上
- 获取该行的构建结果数据
- 检查构建状态是否为 "Build Fail"
- 创建并显示包含 "Rebuild" 选项的弹出菜单

#### 3. handleRebuild() 方法

- 显示确认对话框
- 检查连接状态
- 构建 JSON 请求体
- 异步调用 API
- 处理成功/失败响应
- 自动刷新结果

### API 客户端修改

**文件**: `src/main/java/com/gitviewer/PortalApiClient.java`

#### 新增方法: submitSingleBuild()

```java
public String submitSingleBuild(String tenantCode, String token, String requestBody) 
    throws IOException {
    String url = BASE_URL + "/api/mo-fo/1.0/ops/v2/build?clear_job=true&silences=true&force=false";
    
    Map<String, String> headers = new HashMap<>();
    headers.put("x-mo-target-tenant", tenantCode);
    headers.put("authorization", "Bearer " + token);
    headers.put("Content-Type", "application/json");
    
    String response = sendPostRequest(url, headers, requestBody);
    return response;
}
```

## 使用流程

1. 连接到 tenant
2. 执行搜索，查看构建结果
3. 找到状态为 "Build Fail" 的记录
4. 右键点击该记录
5. 在弹出菜单中选择 "Rebuild"
6. 在确认对话框中查看信息并确认
7. 等待 API 调用完成
8. 查看成功提示
9. 结果列表自动刷新，显示新的构建记录

## 日志输出

系统会记录以下日志：

- `User Action: Rebuild` - 用户触发rebuild
- `App: {appName}, Version: {version}, Branch: {branch}` - 构建信息
- `Rebuild request body: {json}` - 请求体内容
- `Rebuild submitted successfully` - 提交成功
- `Rebuild cancelled by user` - 用户取消
- `Rebuild failed` - 提交失败

## 错误处理

1. **未连接**: 提示用户先连接到 tenant
2. **API 调用失败**: 显示错误对话框，包含详细错误信息
3. **用户取消**: 记录日志，不执行任何操作
4. **无效数据**: 跳过显示菜单

## 注意事项

1. **状态限制**: 只有 "Build Fail" 状态的记录才能 rebuild
2. **版本保持**: Rebuild 使用原记录的相同版本号
3. **分支保持**: Rebuild 使用原记录的相同 Git 分支
4. **异步操作**: Rebuild 请求在后台线程执行，不阻塞 UI
5. **自动刷新**: 成功后自动刷新，但如果正在搜索中则跳过

## 测试建议

1. 测试右键点击 Build Fail 记录显示菜单
2. 测试右键点击其他状态记录不显示菜单
3. 测试确认对话框的信息显示
4. 测试取消操作
5. 测试 API 调用成功的情况
6. 测试 API 调用失败的错误处理
7. 测试未连接时的提示
8. 测试自动刷新功能

## 相关文件

- `src/main/java/com/gitviewer/TenantCICDDialog.java` - UI 和业务逻辑
- `src/main/java/com/gitviewer/PortalApiClient.java` - API 客户端
- `src/main/java/com/gitviewer/BuildResult.java` - 数据模型

## API 参考

### Build API

```
POST https://portal.insuremo.com/api/mo-fo/1.0/ops/v2/build?clear_job=true&silences=true&force=false

Headers:
- x-mo-target-tenant: {tenant_code}
- authorization: Bearer {token}
- Content-Type: application/json

Request Body:
{
    "app_name": "thailife-bs",
    "build_args": "",
    "build_type": "build_only",
    "change_log": "",
    "git_branch": "dev",
    "issues": [],
    "plan_id": "",
    "popconVisible": false,
    "user_name": "thailife",
    "version": "v20260120194501"
}
```

## 构建命令

```bash
mvn clean package
```

## 修复日期

2026-02-04
