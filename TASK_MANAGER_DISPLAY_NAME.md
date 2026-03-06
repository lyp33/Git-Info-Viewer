# 任务管理器显示名称设置

## 问题描述

Git Info Viewer启动后，在Windows任务管理器中只显示为"Java(TM) Platform SE binary"，无法快速识别应用。

## 解决方案

### 方法1：使用Launch4j打包为EXE（推荐）⭐

**优点**：
- ✅ 任务管理器显示自定义名称："Git Info Viewer"
- ✅ 可以设置自定义图标
- ✅ 看起来像原生Windows应用
- ✅ 可以双击exe直接运行
- ✅ 不需要用户知道是Java应用

**步骤**：

#### 1. 下载Launch4j
访问：https://launch4j.sourceforge.net/
下载并安装Launch4j

#### 2. 使用配置文件
已创建配置文件：`launch4j-config.xml`

#### 3. 生成EXE
```bash
# 方法A：使用Launch4j GUI
1. 打开Launch4j
2. 加载配置文件：launch4j-config.xml
3. 点击"Build wrapper"按钮
4. 生成：Git-Info-Viewer.exe

# 方法B：使用命令行
launch4jc.exe launch4j-config.xml
```

#### 4. 运行EXE
```bash
# 直接双击运行
Git-Info-Viewer.exe

# 或在命令行运行
.\Git-Info-Viewer.exe
```

**任务管理器显示**：
```
名称                      CPU    内存
Git Info Viewer          0.2%   169.7 MB
```

---

### 方法2：使用启动脚本（简单）

**优点**：
- ✅ 不需要额外工具
- ✅ 快速实现
- ✅ 可以设置窗口标题

**缺点**：
- ❌ 任务管理器仍显示"Java(TM) Platform SE binary"
- ❌ 但命令行窗口标题会显示"Git Info Viewer"

**使用方法**：

#### 选项A：带控制台窗口
```bash
# 使用 start-git-viewer.bat
start-git-viewer.bat
```

#### 选项B：无控制台窗口
```bash
# 使用 run-git-viewer.bat
run-git-viewer.bat
```

---

### 方法3：使用Maven插件自动打包（高级）

在`pom.xml`中添加Launch4j插件：

```xml
<build>
    <plugins>
        <!-- 现有插件... -->
        
        <!-- Launch4j Maven Plugin -->
        <plugin>
            <groupId>com.akathist.maven.plugins.launch4j</groupId>
            <artifactId>launch4j-maven-plugin</artifactId>
            <version>2.3.3</version>
            <executions>
                <execution>
                    <id>l4j-gui</id>
                    <phase>package</phase>
                    <goals>
                        <goal>launch4j</goal>
                    </goals>
                    <configuration>
                        <headerType>gui</headerType>
                        <jar>${project.build.directory}/${project.artifactId}-${project.version}-jar-with-dependencies.jar</jar>
                        <outfile>${project.build.directory}/Git-Info-Viewer.exe</outfile>
                        <downloadUrl>https://www.oracle.com/java/technologies/downloads/</downloadUrl>
                        <classPath>
                            <mainClass>com.gitviewer.GitViewerApp</mainClass>
                        </classPath>
                        <jre>
                            <minVersion>17</minVersion>
                            <jdkPreference>preferJre</jdkPreference>
                            <runtimeBits>64/32</runtimeBits>
                        </jre>
                        <versionInfo>
                            <fileVersion>1.0.0.0</fileVersion>
                            <txtFileVersion>1.0.0</txtFileVersion>
                            <fileDescription>Git Info Viewer - Git Repository Management Tool</fileDescription>
                            <copyright>2026</copyright>
                            <productVersion>1.0.0.0</productVersion>
                            <txtProductVersion>1.0.0</txtProductVersion>
                            <productName>Git Info Viewer</productName>
                            <internalName>git-info-viewer</internalName>
                            <originalFilename>Git-Info-Viewer.exe</originalFilename>
                        </versionInfo>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

然后运行：
```bash
mvn clean package
```

会自动生成：`target/Git-Info-Viewer.exe`

---

## 对比表

| 方法 | 任务管理器显示 | 需要工具 | 难度 | 推荐度 |
|------|---------------|---------|------|--------|
| Launch4j (GUI) | ✅ Git Info Viewer | Launch4j | 简单 | ⭐⭐⭐⭐⭐ |
| Launch4j (Maven) | ✅ Git Info Viewer | Maven插件 | 中等 | ⭐⭐⭐⭐ |
| 启动脚本 | ❌ Java(TM)... | 无 | 很简单 | ⭐⭐ |
| 直接运行JAR | ❌ Java(TM)... | 无 | 很简单 | ⭐ |

---

## 推荐方案

### 开发阶段
使用启动脚本：
```bash
run-git-viewer.bat
```

### 发布阶段
使用Launch4j打包为EXE：
```bash
1. 编译JAR：mvn clean package
2. 使用Launch4j生成EXE
3. 分发：Git-Info-Viewer.exe
```

---

## 任务管理器显示效果

### 使用Launch4j后
```
┌─────────────────────────────────────────────────────┐
│ 名称                    CPU      内存      磁盘      │
├─────────────────────────────────────────────────────┤
│ Git Info Viewer        0.2%    169.7 MB    0 MB/s   │
│ Chrome                 2.1%    523.4 MB    0 MB/s   │
│ Visual Studio Code     1.5%    312.8 MB    0 MB/s   │
└─────────────────────────────────────────────────────┘
```

### 不使用Launch4j
```
┌─────────────────────────────────────────────────────┐
│ 名称                         CPU      内存      磁盘 │
├─────────────────────────────────────────────────────┤
│ Java(TM) Platform SE binary  0.2%    169.7 MB  0 MB/s│
│ Chrome                       2.1%    523.4 MB  0 MB/s│
│ Visual Studio Code           1.5%    312.8 MB  0 MB/s│
└─────────────────────────────────────────────────────┘
```

---

## 额外优化

### 1. 添加应用图标

创建或下载一个`.ico`文件（推荐256x256），然后在Launch4j配置中添加：

```xml
<icon>path/to/git-viewer-icon.ico</icon>
```

### 2. 设置启动画面

可以添加启动画面（splash screen）：

```xml
<splash>
  <file>path/to/splash.bmp</file>
  <waitForWindow>true</waitForWindow>
  <timeout>60</timeout>
  <timeoutErr>true</timeoutErr>
</splash>
```

### 3. 单实例运行

防止多次启动：

```xml
<singleInstance>
  <mutexName>GitInfoViewerMutex</mutexName>
  <windowTitle>Git Info Viewer</windowTitle>
</singleInstance>
```

---

## 相关文件

- `launch4j-config.xml` - Launch4j配置文件
- `start-git-viewer.bat` - 带控制台的启动脚本
- `run-git-viewer.bat` - 无控制台的启动脚本

---

## 快速开始

### 临时解决（立即可用）
```bash
# 使用启动脚本
run-git-viewer.bat
```

### 永久解决（推荐）
```bash
# 1. 下载Launch4j
# 2. 打开Launch4j
# 3. 加载 launch4j-config.xml
# 4. 点击 Build wrapper
# 5. 运行生成的 Git-Info-Viewer.exe
```

---

## 总结

要在任务管理器中显示"Git Info Viewer"而不是"Java(TM) Platform SE binary"，最好的方法是使用**Launch4j**将JAR打包为EXE。这样不仅显示名称正确，还能提供更好的用户体验。
