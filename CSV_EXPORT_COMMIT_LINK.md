# CSV Export Enhancement - Commit Link Column

## 实现概述

为 Commit Search Results 对话框的 CSV 导出功能添加了 "Commit Link" 列，显示每个提交的完整 URL 链接。

## 实现细节

### 1. 数据模型更新
**文件**: `src/main/java/com/gitviewer/InfoPanel.java`

在 `CommitSearchResult` 类中添加了新字段：
```java
String repoPath;  // 仓库路径，用于构建commit链接
```

### 2. 数据收集
在搜索提交时保存仓库路径：
```java
result.repoPath = repoDir.getAbsolutePath();
```

### 3. CSV 导出增强

#### 3.1 CSV 头部更新
```csv
Project Name,Branch,Commit Code,Date,Author,Message,Changed Files,Commit Link
```

#### 3.2 添加 `buildCommitLink()` 方法
参考 `FileDiffDialog.java` 的实现，构建 commit URL：

**功能**:
- 从仓库路径获取 Git 远程 URL
- 清理 URL 格式（移除 "origin : " 前缀、.git 后缀）
- 转换 SSH 格式为 HTTPS 格式
- 构建 GitLab 格式的 commit URL: `{remoteUrl}/-/commit/{commitId}`

**示例输出**:
```
https://gitlab.example.com/group/project/-/commit/abc123def456
```

#### 3.3 UTF-8 编码支持
使用 `OutputStreamWriter` 和 `StandardCharsets.UTF_8` 确保 CSV 文件正确处理中文字符：
```java
new java.io.OutputStreamWriter(
    new java.io.FileOutputStream(fileChooser.getSelectedFile()), 
    java.nio.charset.StandardCharsets.UTF_8)
```

## CSV 输出格式

每行包含以下列：
1. **Project Name**: 项目名称
2. **Branch**: 分支名称
3. **Commit Code**: 提交哈希值
4. **Date**: 提交日期时间
5. **Author**: 提交作者
6. **Message**: 提交消息
7. **Changed Files**: 修改的文件列表
8. **Commit Link**: 提交的完整 URL（新增）

## URL 构建逻辑

1. 从仓库目录获取 Git 远程 URL
2. 清理 URL：
   - 移除 "origin : " 前缀
   - 移除 .git 后缀
   - 转换 SSH 格式 (git@host:path) 为 HTTPS (https://host/path)
3. 拼接 commit ID: `{cleanUrl}/-/commit/{commitId}`
4. 如果无法获取远程 URL，该列为空

## 编译和部署

```bash
mvn clean package
```

生成文件: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 测试建议

1. 打开应用程序
2. 进入 Commit Search 功能
3. 搜索提交记录
4. 点击 "Export to CSV" 按钮
5. 验证导出的 CSV 文件：
   - 包含 "Commit Link" 列
   - Commit Link 格式正确
   - 中文字符显示正常
   - 可以直接点击链接访问 GitLab/GitHub

## 相关文件

- `src/main/java/com/gitviewer/InfoPanel.java` - 主要实现文件
- `src/main/java/com/gitviewer/FileDiffDialog.java` - 参考的 URL 构建逻辑
- `src/main/java/com/gitviewer/GitInfoExtractor.java` - Git 信息提取工具

## 完成时间

2026-01-20
