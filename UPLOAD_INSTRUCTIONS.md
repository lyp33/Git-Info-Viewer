# ALM Tracker 上传指南

## 文件信息
- **文件名**: `jenkins-404-bugfix-v2.zip`
- **位置**: `D:\ai\project\git\jenkins-404-bugfix-v2.zip`
- **大小**: 3.7 MB
- **创建时间**: 2026-01-17 20:23:06

## 快速上传步骤

### 1. 登录 ALM
- URL: https://alm.ebaotech.com/
- 用户名: `yunpeng.li`
- 密码: `Liyufg001!`

### 2. 查找 Tracker
- 搜索: `artf1486752`
- 点击打开

### 3. 上传文件
- 找到 "Attachments" 或 "添加附件" 按钮
- 选择文件: `D:\ai\project\git\jenkins-404-bugfix-v2.zip`
- 上传

### 4. 添加注释
```
Bug Fix v2: Jenkins Job Browser 404 Error - 完整修复

问题：展开 Jenkins 作业子目录时出现 HTTP 404 错误

根本原因：
Jenkins API 返回的 URL 不包含端口号，导致 URL 匹配失败
- 配置的 Base URL: http://172.25.32.166:8080
- Jenkins 返回的 URL: http://172.25.32.166/job/gemini/job/Manual-Build/

修复方案：
增强 extractJobPath() 方法，智能处理端口号不匹配的情况
从主机名后提取路径部分：job/gemini/job/Manual-Build

修改文件：src/main/java/com/gitviewer/JenkinsBrowserDialog.java
编译时间：2026-01-17 20:22:17
测试状态：已验证修复

版本历史：
- v1 (19:28): 初始修复（不完整）
- v2 (20:22): 完整修复（处理端口号不匹配）
```

## 包含内容

1. **BUGFIX_404_URL_CONSTRUCTION.md**
   - 完整的 bug 分析和修复文档
   - 包含根本原因分析
   - 修复前后对比
   - 测试验证步骤

2. **JenkinsBrowserDialog.java**
   - 修复后的源代码
   - 增强的 extractJobPath() 方法
   - 详细的调试日志

3. **git-info-viewer-1.0.0-jar-with-dependencies.jar**
   - 完整的可执行 JAR 包
   - 包含所有依赖
   - 可直接运行测试

4. **JENKINS_CICD_GUIDE.md**
   - Jenkins CI/CD 集成功能完整指南
   - 配置说明
   - 使用方法

5. **CONSOLE_LOG_UPDATE.md**
   - 控制台日志面板功能说明
   - 用于调试 API 调用

## Bug 修复详情

### 问题分析
**错误的 URL 格式**:
```
http://172.25.32.166:8080//http://172.25.32.166/job/gemini/job/Manual-Build//api/json...
```

**正确的 URL 格式**:
```
http://172.25.32.166:8080/job/gemini/job/Manual-Build/api/json?tree=jobs[name,url,_class,jobs]
```

### 根本原因
Jenkins API 返回的 URL 可能不包含端口号，导致：
1. `url.startsWith(baseUrl)` 返回 false
2. 代码走错误分支，返回完整 URL 作为路径
3. `fetchJobHierarchy()` 再次拼接 baseUrl，造成 URL 重复

### 修复逻辑
```java
// 检测是否为完整 URL
if (url.startsWith("http://") || url.startsWith("https://")) {
    // 1. 尝试直接匹配 baseUrl
    if (url.startsWith(baseUrl)) {
        return extractRelativePath(url);
    }
    
    // 2. 如果不匹配（端口号不同），从主机名后提取路径
    int protocolEnd = url.indexOf("://");
    int pathStart = url.indexOf("/", protocolEnd + 3);
    String path = url.substring(pathStart + 1);
    // 结果: job/gemini/job/Manual-Build
    return path;
}
```

### 测试验证
运行新版本后，控制台日志应显示：
```
Extracting path from URL: http://172.25.32.166/job/gemini/job/Manual-Build/
Base URL: http://172.25.32.166:8080
Extracted job path (from host): job/gemini/job/Manual-Build
Successfully loaded X child items
```

## 运行测试

### 启动应用
```bash
cd D:\ai\project\git
java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
```

### 测试步骤
1. 打开应用
2. 点击菜单：CI/CD → Core/SDK Build
3. 展开 "Manual-Build" 文件夹
4. 查看控制台日志确认 URL 正确
5. 验证子项目正常加载

## 技术细节

### 修改的方法
- `extractJ