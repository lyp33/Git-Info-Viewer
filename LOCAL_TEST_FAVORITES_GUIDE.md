# 本地测试收藏功能指南

## 问题说明

在本地测试时，如果无法连接到公司 Jenkins，收藏列表会显示为空。这是因为：

1. Jenkins 收藏数据存储在独立的序列化文件中：`gitviewer-jenkins-favorites.dat`
2. 该文件位于用户主目录：`C:\Users\<你的用户名>\gitviewer-jenkins-favorites.dat`
3. 不能通过手动编辑 properties 文件来添加收藏

## 解决方案：使用测试工具

我们创建了一个测试工具 `TestFavoritesUtil`，可以在不连接 Jenkins 的情况下创建测试数据。

### 方法 1：使用批处理脚本（推荐）

#### 步骤 1：编译项目
```bash
mvn clean package
```

#### 步骤 2：创建测试收藏数据
```bash
test-favorites.bat create
```

这会创建 3 个测试收藏：
- all-in-one-auto-CI
- backend-service
- production-deploy

#### 步骤 3：查看创建的收藏
```bash
test-favorites.bat show
```

#### 步骤 4：启动应用测试
```bash
java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
```

打开 Jenkins Browser，你应该能看到收藏列表中有 3 个测试项目。

#### 步骤 5：测试双击功能

双击任意收藏项，应该会：
1. 弹出 "Loading... please wait" 模态对话框
2. 尝试在树中定位该任务（可能找不到，因为没有真实 Jenkins 连接）
3. 如果找不到，会提示是否删除该收藏

### 方法 2：单独测试加载对话框

如果只想测试加载对话框的显示效果：

```bash
test-favorites.bat test-dialog
```

这会打开一个测试窗口，点击按钮可以看到加载对话框的效果（模拟 3 秒加载时间）。

### 方法 3：使用 Java 命令（高级）

如果批处理脚本不工作，可以直接使用 Java 命令：

```bash
# 创建测试数据
java -cp target\git-info-viewer-1.0.0-jar-with-dependencies.jar com.gitviewer.TestFavoritesUtil create

# 显示当前数据
java -cp target\git-info-viewer-1.0.0-jar-with-dependencies.jar com.gitviewer.TestFavoritesUtil show

# 删除数据
java -cp target\git-info-viewer-1.0.0-jar-with-dependencies.jar com.gitviewer.TestFavoritesUtil delete

# 测试对话框
java -cp target\git-info-viewer-1.0.0-jar-with-dependencies.jar com.gitviewer.TestFavoritesUtil test-dialog
```

## 清理测试数据

如果想删除测试数据，重新开始：

```bash
test-favorites.bat delete
```

或者手动删除文件：
```
C:\Users\<你的用户名>\gitviewer-jenkins-favorites.dat
```

## 验证版本

确保使用最新编译的 JAR：

1. 查看 Jenkins Browser 窗口标题，应该显示最新的编译时间戳
2. 控制台输出应该包含 "VERSION CHECK" 日志

## 预期行为

### 正常情况（有 Jenkins 连接）：
1. 双击收藏项
2. 显示 "Loading... please wait" 对话框
3. 递归加载所有子目录层级
4. 定位到目标任务
5. 关闭加载对话框

### 测试情况（无 Jenkins 连接）：
1. 双击收藏项
2. 显示 "Loading... please wait" 对话框
3. 尝试定位（会失败，因为树中没有数据）
4. 关闭加载对话框
5. 提示 "Cannot find job"，询问是否删除收藏

## 故障排查

### 问题：收藏列表仍然为空

**检查点 1：** 确认测试数据已创建
```bash
test-favorites.bat show
```
应该显示 3 个收藏项。

**检查点 2：** 确认文件位置
```bash
dir %USERPROFILE%\gitviewer-jenkins-favorites.dat
```
文件应该存在。

**检查点 3：** 重新启动应用
关闭应用后重新启动，收藏数据在启动时加载。

### 问题：双击没有反应

**检查点 1：** 查看控制台输出
应该看到 "Mouse Clicked" 和 "Double-click detected!" 日志。

**检查点 2：** 确认版本
窗口标题应该显示最新的编译时间戳。

**检查点 3：** 重新编译
```bash
mvn clean package
```

### 问题：加载对话框没有显示

**检查点 1：** 查看控制台日志
应该看到 "Loading dialog created" 和 "SwingWorker" 相关日志。

**检查点 2：** 单独测试对话框
```bash
test-favorites.bat test-dialog
```
如果这个能显示，说明对话框代码正常。

## 文件位置

- **收藏数据文件**: `%USERPROFILE%\gitviewer-jenkins-favorites.dat`
- **配置文件**: `%USERPROFILE%\gitviewer.properties`
- **测试工具**: `src/main/java/com/gitviewer/TestFavoritesUtil.java`
- **批处理脚本**: `test-favorites.bat`

## 技术细节

### 数据格式

收藏数据使用 Java 序列化存储：
```java
List<FavoriteJob> favorites = [
    FavoriteJob {
        jobPath: "gemini/job/Manual-Build/job/all-in-one-auto-CI",
        displayName: "all-in-one-auto-CI",
        jobUrl: "https://ci.jenkins.io/...",
        order: 0
    },
    ...
]
```

### 加载流程

1. `AppSettings.loadJenkinsFavorites()` - 启动时从文件加载
2. `FavoritesPanel.loadFavorites()` - 显示在 UI 中
3. 双击触发 `navigateToJob()` - 显示加载对话框并导航

### 导航流程

1. 创建模态对话框（阻止用户操作）
2. 启动 SwingWorker 后台线程
3. 调用 `JenkinsBrowserDialog.navigateToJobPath()`
4. 递归加载所有子目录层级
5. 定位到目标节点
6. 关闭对话框

## 下一步

测试完成后，如果一切正常：
1. 删除测试数据：`test-favorites.bat delete`
2. 配置真实的 Jenkins 连接
3. 添加真实的收藏项
4. 享受快速导航功能！
