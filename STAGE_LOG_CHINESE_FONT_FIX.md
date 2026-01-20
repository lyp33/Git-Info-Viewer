# Stage Log 中文字体修复

## 问题描述

Stage Log 对话框中的 Jenkins Log 和 Portal Log 标签页显示中文时出现乱码（显示为方框）。

## 原因分析

文本区域使用的字体是 `Consolas`，这是一个等宽英文字体，不支持中文字符显示。

## 解决方案

将两个标签页的文本区域字体从 `Consolas` 改为 `Microsoft YaHei`（微软雅黑），这是一个支持中文的等宽字体。

### 修改内容

**文件**: `src/main/java/com/gitviewer/JenkinsStageLogDialog.java`

#### Jenkins Log 标签页
```java
// 修改前
jenkinsLogTextArea.setFont(new Font("Consolas", Font.PLAIN, 11));

// 修改后
jenkinsLogTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
```

#### Portal Log 标签页
```java
// 修改前
portalLogTextArea.setFont(new Font("Consolas", Font.PLAIN, 11));

// 修改后
portalLogTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
```

## 字体选择说明

- **Microsoft YaHei** (微软雅黑): Windows 系统自带，支持中英文混合显示，清晰易读
- 保持 11 号字体大小不变
- 保持黑色背景、白色文字的配色方案

## 测试验证

1. 打开应用程序
2. 进入 Jenkins Browser
3. 双击任意 Stage 打开 Stage Log 对话框
4. 验证 Jenkins Log 标签页中的中文正常显示
5. 切换到 Portal Log 标签页，验证中文正常显示

## 编译和部署

```bash
mvn clean package
```

生成文件: `target/git-info-viewer-1.0.0-jar-with-dependencies.jar`

## 相关文件

- `src/main/java/com/gitviewer/JenkinsStageLogDialog.java` - Stage Log 对话框

## 完成时间

2026-01-20
