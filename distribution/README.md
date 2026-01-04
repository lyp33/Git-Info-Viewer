# Git Info Viewer

一个用于查看本地 Git 仓库信息的 Java 桌面应用程序。

## 功能特性

- 左右分栏界面
- 左侧：目录树结构浏览
- 右侧：显示选中目录的详细信息
- 自动识别 Git 仓库
- 显示 Git 仓库信息：
  - Remote URLs
  - 当前分支和所有分支
  - 最后一次提交信息（提交人、时间、消息）
- 显示子目录列表，标记 Git 仓库

## 技术栈

- Java 11+
- Swing UI 框架
- JGit 库（用于 Git 操作）
- Maven（构建工具）

## 环境要求

1. Java 11 或更高版本
2. Maven 3.6+

## 安装 Maven

如果您的系统上还没有安装 Maven，请按以下步骤安装：

### Windows 系统

1. 下载 Maven：https://maven.apache.org/download.cgi
2. 解压到本地目录（例如：C:\Program Files\Apache\maven）
3. 配置环境变量：
   - 添加 `MAVEN_HOME` 环境变量，指向 Maven 安装目录
   - 将 `%MAVEN_HOME%\bin` 添加到 PATH 环境变量
4. 验证安装：在命令行运行 `mvn -version`

### Linux/macOS 系统

```bash
# 使用包管理器安装
# Ubuntu/Debian
sudo apt-get install maven

# macOS (使用 Homebrew)
brew install maven
```

## 构建和运行

### 1. 编译项目

```bash
mvn clean compile
```

### 2. 打包项目

```bash
mvn clean package
```

这将生成一个包含所有依赖的可执行 JAR 文件：
`target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

### 3. 运行程序

```bash
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

或者直接在 Maven 中运行：

```bash
mvn exec:java -Dexec.mainClass="com.gitviewer.GitViewerApp"
```

## 使用说明

1. 启动程序后，点击菜单栏的 `File` -> `Select Root Directory...`
2. 选择要浏览的根目录
3. 在左侧目录树中点击任意目录
4. 右侧面板会显示：
   - 如果是 Git 仓库：显示 remote、branch、最后提交信息
   - 显示所有子目录列表，标注哪些是 Git 仓库

## 项目结构

```
git-info-viewer/
├── pom.xml                                 # Maven 配置文件
├── README.md                               # 项目说明文档
└── src/
    └── main/
        └── java/
            └── com/
                └── gitviewer/
                    ├── GitViewerApp.java           # 主应用程序
                    ├── DirectoryTreePanel.java     # 左侧目录树面板
                    ├── InfoPanel.java              # 右侧信息显示面板
                    └── GitInfoExtractor.java       # Git 信息提取工具类
```

## 常见问题

### Q: Maven 编译失败
A: 请确保：
- Java 版本是 11 或更高
- Maven 正确安装并配置
- 网络连接正常（Maven 需要下载依赖）

### Q: 程序启动后看不到目录树
A: 点击菜单栏 `File` -> `Select Root Directory...` 选择一个根目录

### Q: Git 信息显示不完整
A: 请确保：
- 目录是有效的 Git 仓库（包含 .git 目录）
- Git 仓库有提交历史

## 开发说明

如需修改代码后重新编译：

```bash
mvn clean compile package
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！
