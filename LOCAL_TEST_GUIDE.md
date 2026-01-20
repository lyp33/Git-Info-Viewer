# 本地测试指南（无需Jenkins连接）

## 问题
本地无法连接到公司Jenkins，但想测试最新的修改。

## 解决方案

### 方案1：使用公共Jenkins实例（最简单）

使用公开的Jenkins测试服务器：

**Jenkins URL**: `https://ci.jenkins.io`
**说明**: 这是Jenkins官方的CI服务器，公开访问，无需认证

**配置步骤**:
1. 启动应用
2. 打开 Settings → Jenkins Settings
3. 填写：
   - Jenkins URL: `https://ci.jenkins.io`
   - Username: 留空
   - API Token: 留空
   - Default Job Path: `job/Infra`
4. 保存
5. 打开 Jenkins Job Browser
6. 测试功能

---

### 方案2：本地Docker运行Jenkins（推荐用于完整测试）

**步骤1: 安装Docker**
- 下载并安装 Docker Desktop for Windows
- 启动 Docker Desktop

**步骤2: 运行Jenkins容器**
```cmd
docker run -d -p 8080:8080 -p 50000:50000 --name jenkins jenkins/jenkins:lts
```

**步骤3: 获取初始密码**
```cmd
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

**步骤4: 配置Jenkins**
1. 打开浏览器访问 `http://localhost:8080`
2. 输入初始密码
3. 选择 "Install suggested plugins"
4. 创建管理员账户
5. 创建几个测试Job（可以是简单的Pipeline）

**步骤5: 配置应用**
1. 启动应用
2. 打开 Settings → Jenkins Settings
3. 填写：
   - Jenkins URL: `http://localhost:8080`
   - Username: 你创建的用户名
   - API Token: 在Jenkins中生成（User → Configure → API Token）
   - Default Job Path: `job`
4. 保存并测试

---

### 方案3：添加测试模式（无需真实Jenkins）

我可以添加一个"测试模式"，使用模拟数据测试UI功能。

**需要添加的功能**:
1. 在Settings中添加"Test Mode"复选框
2. 启用后使用模拟的Jenkins数据
3. 可以测试所有UI交互，包括：
   - 收藏Job导航
   - Loading对话框
   - Stage View布局
   - Build参数对话框

**是否需要我实现这个测试模式？**

---

### 方案4：验证窗口标题和基本UI（最快）

即使不连接Jenkins，你也可以验证：

1. **窗口标题时间戳**
   ```cmd
   java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```
   - 打开应用
   - 查看主窗口标题（应该显示Git Info Viewer）
   - 尝试打开Jenkins Browser（会提示配置错误，但可以看到窗口标题）

2. **Stage View布局**
   - 虽然无法连接Jenkins，但可以看到UI布局
   - 确认没有多余的外层边框

3. **控制台输出**
   - 查看命令行窗口的日志输出
   - 确认有"VERSION CHECK"等新增的日志

---

## 推荐测试流程

### 快速验证（5分钟）
1. 使用方案1（公共Jenkins实例）
2. 配置 `https://ci.jenkins.io`
3. 打开Jenkins Browser
4. 查看窗口标题时间戳
5. 测试基本功能

### 完整测试（30分钟）
1. 使用方案2（本地Docker Jenkins）
2. 创建多层嵌套的测试Job
3. 添加到收藏
4. 测试双击导航
5. 验证Loading对话框
6. 检查Stage View布局

---

## 我可以帮你做什么？

1. **添加测试模式** - 实现方案3，无需真实Jenkins
2. **创建Docker配置** - 提供完整的Docker Compose配置
3. **提供测试脚本** - 自动化测试流程
4. **其他建议** - 根据你的需求定制

你想用哪个方案？或者需要我实现测试模式？
