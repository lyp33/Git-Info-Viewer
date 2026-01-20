# Git Info Viewer - FINAL DEBUG VERSION

## 版本信息

- **文件名**: `git-info-viewer-FINAL-DEBUG-VERSION.zip`
- **编译时间**: 2026-01-18 01:50:35
- **JAR 文件大小**: 4,046,169 bytes
- **版本**: 1.0.0

## 确认这是最新代码

✅ 刚刚执行了 `mvn clean package`，完全重新编译
✅ JAR 文件时间戳：2026-01-18 01:50:35
✅ 包含所有最新的调试日志

## 包含的所有调试功能

### 1. Mouse Event Debug (鼠标事件调试) - 最新添加

**文件**: `FavoritesPanel.java`

**日志输出**:
```
=== Mouse Clicked ===
Click count: 1/2
Button: 1 (左键)
Double-click detected! (如果是双击)
Index: <列表索引>
Job retrieved: <job名称> 或 NULL
```

**用途**: 确定双击事件是否触发

### 2. Favorites Navigation Debug (收藏导航调试)

**文件**: `FavoritesPanel.java`

**日志输出**:
```
=== navigateToJob called ===
Job: <名称>
Job Path: <路径>
Parent Dialog: SET/NULL
Loading dialog created
Starting SwingWorker...
SwingWorker: doInBackground started
SwingWorker: navigateToJobPath returned: true/false
```

**用途**: 追踪导航过程

### 3. Build Parameters Debug (构建参数调试)

**文件**: `JenkinsJobDetailsDialog.java`, `JenkinsBuildParametersDialog.java`

**日志输出**:
```
=== Build Parameters Definition ===
Total parameters: X
Parameter: <name>
  Type: <type>
  Default Value: <value>
=== Prefilled Parameters ===
  <name> = <value>
Using prefilled value for <name>: <value>
Using default value for <name>: <value>
```

**用途**: 调试参数默认值问题

### 4. Module Double-Click Debug (模块双击调试)

**文件**: `JenkinsApiClient.java`, `JenkinsStageViewPanel.java`

**日志输出**:
```
[JenkinsApiClient] === Fetching Build Stages ===
[JenkinsApiClient] Stage 1: name='<name>', id='<id>', status='<status>'
[JenkinsApiClient] === Fetching Stage Log ===
[JenkinsApiClient] Constructed API URL: <url>
[StageView] === Double-Click Event ===
[StageView] Stage: <name>
[StageView] Stage ID: <id>
```

**用途**: 调试模块双击错误

## 如何运行

### 从命令行运行（推荐 - 可以看到所有日志）

```bash
java -jar git-info-viewer-1.0.0-jar-with-dependencies.jar
```

### 双击运行（不推荐 - 看不到控制台日志）

直接双击 JAR 文件会运行，但看不到调试输出。

## 测试步骤

### 测试收藏导航功能

1. 从命令行运行应用
2. 打开 Jenkins Browser
3. 添加一个 job 到收藏（如果还没有）
4. **单击**收藏列表中的 job，查看控制台输出：
   ```
   === Mouse Clicked ===
   Click count: 1
   Button: 1
   Single click, ignoring
   ```
5. **双击**收藏列表中的 job，查看控制台输出：
   ```
   === Mouse Clicked ===
   Click count: 2
   Button: 1
   Double-click detected!
   Index: 0
   Job retrieved: <job名称>
   === navigateToJob called ===
   ...
   ```

### 根据输出判断问题

#### 情况 1: 没有任何输出
**问题**: MouseListener 没有注册或 FavoritesPanel 没有初始化
**可能原因**: 
- FavoritesPanel 没有被添加到 JenkinsBrowserDialog
- MouseListener 在 favoritesList 创建前添加

#### 情况 2: 只有单击输出，没有双击
**问题**: 双击没有被识别
**可能原因**:
- 双击速度太慢
- 双击事件被其他组件拦截

#### 情况 3: 有双击输出但 job 为 NULL
**问题**: listModel 数据问题
**可能原因**:
- 收藏列表没有正确加载
- listModel 索引错误

#### 情况 4: 有双击输出但 parentDialog 为 NULL
**问题**: FavoritesPanel 初始化问题
**可能原因**:
- JenkinsBrowserDialog 创建 FavoritesPanel 时没有传入 this

#### 情况 5: 所有日志都正常但没有加载对话框
**问题**: SwingWorker 或对话框创建问题
**可能原因**:
- SwingUtilities.getWindowAncestor(this) 返回 null
- 模态对话框在 worker.execute() 前显示

## 验证代码是最新的

如果你怀疑代码不是最新的，可以检查：

1. **检查 JAR 文件时间戳**:
   ```bash
   # Windows
   dir target\git-info-viewer-1.0.0-jar-with-dependencies.jar
   
   # 应该显示: 2026-01-18 01:50
   ```

2. **解压 JAR 并检查类文件**:
   ```bash
   # 解压 JAR
   jar xf git-info-viewer-1.0.0-jar-with-dependencies.jar
   
   # 查看 FavoritesPanel.class 的时间戳
   dir com\gitviewer\FavoritesPanel.class
   ```

3. **运行并查看日志**:
   - 如果单击收藏列表有 "=== Mouse Clicked ===" 输出，说明代码是最新的
   - 如果没有任何输出，说明可能运行了旧版本

## 如果确认运行的是旧版本

1. **删除旧的 JAR 文件**:
   ```bash
   del git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```

2. **从 zip 中解压新的 JAR**:
   ```bash
   # 解压 git-info-viewer-FINAL-DEBUG-VERSION.zip
   # 得到 git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```

3. **确认文件大小**:
   - 应该是 4,046,169 bytes
   - 如果不是，说明解压错误

4. **重新运行**:
   ```bash
   java -jar git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```

## 相关文档

- `MOUSE_EVENT_DEBUG.md` - 鼠标事件调试详细说明
- `FAVORITES_NAVIGATION_DEBUG.md` - 收藏导航调试说明
- `BUILD_PARAMS_DEBUG_GUIDE.md` - 构建参数调试指南
- `MODULE_DEBUG_LOGGING.md` - 模块双击调试说明

## 下一步

1. 运行这个版本
2. 测试双击收藏功能
3. 复制控制台的所有输出
4. 告诉我输出内容，我们就能确定问题所在

## 编译信息

```
[INFO] Building Git Info Viewer 1.0.0
[INFO] Compiling 38 source files with javac [debug target 17]
[INFO] BUILD SUCCESS
[INFO] Total time:  12.736 s
[INFO] Finished at: 2026-01-18T01:50:35+08:00
```
