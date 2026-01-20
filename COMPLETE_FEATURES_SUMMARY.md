# Git Info Viewer - 完整功能总结

## 项目概述

Git Info Viewer 是一个 Java Swing 桌面应用程序，用于查看和管理 Git 仓库信息，并集成了 Jenkins CI/CD 功能。

---

## 最新完成的功能（2026-01-20）

### 1. ✅ Portal Log Ctrl+F 搜索功能

**功能描述**：为 Stage Log 对话框添加完整的文本搜索功能

**实现内容**：
- ✅ Ctrl+F 快捷键打开搜索面板
- ✅ ESC 快捷键关闭搜索面板
- ✅ F3 / Shift+F3 快速导航（下一个/上一个）
- ✅ 不区分大小写搜索
- ✅ 黄色高亮显示所有匹配项
- ✅ 显示匹配计数（"1 of 5"）
- ✅ 自动滚动到当前匹配位置
- ✅ 循环导航（到最后一个后回到第一个）
- ✅ 跨 Tab 支持（Jenkins Log + Portal Log）

**文件修改**：
- `src/main/java/com/gitviewer/JenkinsStageLogDialog.java`

**文档**：
- `PORTAL_LOG_SEARCH_FEATURE.md`

---

### 2. ✅ Portal API 嵌套 JSON 解析修复

**问题描述**：Portal API 返回的 JSON 中 `build_output` 字段在 `callback` 对象内部，原代码只检查根级别导致解析失败

**解决方案**：
- ✅ 支持检查根级别 `build_output`
- ✅ 支持检查 `callback.build_output` 嵌套结构
- ✅ 添加详细调试日志
- ✅ 自动解码转义序列（`\n`, `\r`, `\t`, `\uXXXX`）

**Portal API JSON 结构**：
```json
{
  "id": "...",
  "callback": {
    "build_output": "日志内容..."
  }
}
```

**文件修改**：
- `src/main/java/com/gitviewer/JenkinsApiClient.java`

**文档**：
- `PORTAL_BUILD_OUTPUT_NESTED_JSON_FIX.md`
- `PORTAL_METHODS_COMPARISON.md`

---

## 之前完成的功能

### 3. ✅ Portal Log UI 冻结修复

**问题**：大量日志内容（几 MB）导致 UI 完全冻结

**解决方案**：
- 限制显示内容为 500KB
- 超过限制时显示警告并截断
- 添加内容大小日志

**文档**：`PORTAL_LOG_UI_FREEZE_FIX.md`

---

### 4. ✅ Portal Log 换行符处理

**问题**：JSON 中的 `\n` 显示为字面字符串，不换行

**解决方案**：
- 解码所有转义序列：`\n`, `\r`, `\t`, `\\`, `\"`, `\uXXXX`
- 添加换行符计数日志

**文档**：`PORTAL_LOG_NEWLINE_FIX.md`

---

### 5. ✅ Portal URL 提取修复（使用最后一个匹配）

**问题**：日志中有多个 curl 命令，第一个 URL 不完整

**解决方案**：
- 遍历所有行，保存最后一个匹配的 URL
- 最后一个 curl 命令包含完整的查询参数

**文档**：`PORTAL_URL_LAST_MATCH_FIX.md`

---

### 6. ✅ Portal URL 查询参数支持

**问题**：URL 提取不完整，缺少 `/query_one?id=*****` 部分

**解决方案**：
- 支持带引号和不带引号的 URL 格式
- 正确提取包含查询参数的完整 URL

**文档**：`PORTAL_URL_QUERY_PARAMS_FIX.md`

---

### 7. ✅ Portal Log 缓存修复

**问题**：Portal Log 使用 Stage Log 而不是 Sub-Job Log，找不到 curl 命令

**解决方案**：
- 缓存 Sub-Job Log 而不是 Stage Log
- curl 命令在 Sub-Job Log 中

**文档**：`PORTAL_LOG_CACHE_FIX.md`

---

### 8. ✅ Stage Log 中文字体修复

**问题**：中文字符显示为方框

**解决方案**：
- 将字体从 `Consolas` 改为 `Microsoft YaHei`
- 支持中文字符显示

**文档**：`STAGE_LOG_CHINESE_FONT_FIX.md`

---

### 9. ✅ Portal Log 双 Tab 界面

**功能**：
- Tab 1: Jenkins Log（显示 Jenkins Console Log）
- Tab 2: Portal Log（显示 Portal API build_output）
- 按需加载（切换到 Portal Log Tab 时才加载）
- 显示 API 请求信息（URL + Headers）

**文档**：`STAGE_LOG_TABS_IMPLEMENTATION.md`

---

### 10. ✅ Stage Sub-Job Log 功能

**功能**：
- 从 Stage Log 中提取 job path 和 build ID
- 构建 Sub-Job URL
- 获取 Sub-Job 的完整 Console Log
- 支持 `»` (U+00BB) 分隔符

**文档**：`STAGE_SUB_JOB_LOG_FEATURE.md`

---

### 11. ✅ Stage Build ID 提取和显示

**功能**：
- 从 Stage Log 中提取 Build ID（例如 #809）
- 在 Stage 列表中显示 Build ID
- 格式：`Stage名称 (持续时间) - Build #809`

**文档**：`STAGE_BUILD_ID_SOLUTION.md`

---

### 12. ✅ Build History 显示 SERVICE_NAME 和 versions

**功能**：
- 在 Build History 列表中显示 `SERVICE_NAME` 和 `versions` 参数
- 优先级：SERVICE_NAME > versions > VERSION/BRANCH/TAG
- 格式：`[SERVICE_NAME: xxx, versions: yyy]`

**文档**：`BUILD_HISTORY_显示SERVICE_NAME和VERSIONS.md`

---

### 13. ✅ Stage Log 并行支持

**功能**：
- 支持串行 Stage：`[Pipeline] { (stageName)`
- 支持并行 Stage：`[Pipeline] [branchName] { (stageName)`
- 自动识别并提取正确的 Stage Log

**文档**：`STAGE_LOG_并行支持.md`

---

### 14. ✅ CSV 导出 Commit Link

**功能**：
- 在 Commit Search Results 中添加 "Commit Link" 列
- 自动构建 GitLab commit URL
- 格式：`{remoteUrl}/-/commit/{commitId}`

**文档**：`CSV_EXPORT_COMMIT_LINK.md`

---

## 技术栈

### 核心技术
- **Java 17**
- **Maven 3.6+**
- **Swing UI**
- **JGit 6.10.0** - Git 操作
- **SLF4J 2.0.7** - 日志框架
- **JSON** - API 数据解析

### 构建和运行
```bash
# 编译
mvn clean compile

# 打包
mvn clean package

# 运行
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

---

## 项目结构

```
git-info-viewer/
├── src/main/java/com/gitviewer/
│   ├── GitViewerApp.java              # 主应用程序
│   ├── JenkinsApiClient.java          # Jenkins API 客户端
│   ├── JenkinsBrowserDialog.java      # Jenkins 浏览器对话框
│   ├── JenkinsStageLogDialog.java     # Stage Log 对话框（含搜索功能）
│   ├── JenkinsStageViewPanel.java     # Stage 视图面板
│   ├── InfoPanel.java                 # 信息面板
│   ├── DirectoryTreePanel.java        # 目录树面板
│   └── ...
├── target/
│   └── git-info-viewer-1.0.0-jar-with-dependencies.jar
├── pom.xml
└── *.md                               # 功能文档
```

---

## 快捷键列表

### 全局快捷键
- **Ctrl+F** - 文件搜索（主窗口）

### Stage Log 对话框快捷键
- **Ctrl+F** - 打开文本搜索面板
- **ESC** - 关闭搜索面板
- **F3** - 查找下一个匹配项
- **Shift+F3** - 查找上一个匹配项
- **Enter** - 开始搜索（在搜索框中）

---

## Git 提交历史

### 最新提交（2026-01-20）

```
81822b4 feat: Add Ctrl+F search functionality to Portal Log and fix nested JSON parsing
1a9685e Fix Portal Log UI freeze by limiting text size
30c1ee4 Fix Portal Log newline handling - decode escape sequences
00120bf Fix Portal URL extraction to use LAST match
535a504 Add version logging and detailed debug info
```

---

## 功能对比

### Portal Log 功能演进

| 版本 | 功能 | 状态 |
|------|------|------|
| v1.0 | 基础 Portal Log 显示 | ✅ |
| v1.1 | 双 Tab 界面（Jenkins + Portal） | ✅ |
| v1.2 | 中文字体支持 | ✅ |
| v1.3 | Portal URL 提取修复 | ✅ |
| v1.4 | 查询参数支持 | ✅ |
| v1.5 | 使用最后一个 URL 匹配 | ✅ |
| v1.6 | 换行符解码 | ✅ |
| v1.7 | UI 冻结修复（500KB 限制） | ✅ |
| v1.8 | 嵌套 JSON 解析 | ✅ |
| v1.9 | **Ctrl+F 搜索功能** | ✅ |

---

## 已知问题和限制

### 当前限制
1. Portal Log 显示限制为 500KB（防止 UI 冻结）
2. 搜索功能不支持正则表达式
3. 搜索功能不支持区分大小写选项

### 未来改进建议
1. 添加正则表达式搜索支持
2. 添加区分大小写选项
3. 添加全词匹配选项
4. 添加搜索历史记录
5. 支持更大的日志文件（虚拟滚动）

---

## 测试指南

### 测试 Portal Log 搜索功能

1. 打开应用程序
2. 进入 Jenkins Browser
3. 选择一个 Job 并查看 Build History
4. 双击一个 Build 查看 Stage View
5. 双击一个 Stage 打开 Stage Log 对话框
6. 按 **Ctrl+F** 打开搜索面板
7. 输入搜索文本（例如 "error"）
8. 按 **Enter** 或点击 "Find"
9. 验证：
   - ✅ 所有匹配项被黄色高亮
   - ✅ 显示匹配计数（"1 of X"）
   - ✅ 自动滚动到第一个匹配项
10. 按 **F3** 跳转到下一个匹配项
11. 按 **Shift+F3** 跳转到上一个匹配项
12. 切换到 Portal Log Tab，重复搜索测试

### 测试 Portal API 嵌套 JSON 解析

1. 打开 Stage Log 对话框
2. 切换到 Portal Log Tab
3. 等待加载完成
4. 验证：
   - ✅ 显示 API 请求信息（URL + Headers）
   - ✅ 显示 "Loading..."
   - ✅ 成功显示 build_output 内容
   - ✅ 控制台输出 "Found build_output in callback object"
   - ✅ 换行符正确显示
   - ✅ 中文字符正确显示

---

## 相关文档

### 功能文档
- `PORTAL_LOG_SEARCH_FEATURE.md` - Ctrl+F 搜索功能
- `PORTAL_METHODS_COMPARISON.md` - 方法对比
- `PORTAL_BUILD_OUTPUT_NESTED_JSON_FIX.md` - 嵌套 JSON 解析
- `PORTAL_LOG_UI_FREEZE_FIX.md` - UI 冻结修复
- `PORTAL_LOG_NEWLINE_FIX.md` - 换行符处理
- `PORTAL_URL_LAST_MATCH_FIX.md` - URL 提取修复
- `PORTAL_URL_QUERY_PARAMS_FIX.md` - 查询参数支持
- `PORTAL_LOG_CACHE_FIX.md` - 缓存修复
- `STAGE_LOG_CHINESE_FONT_FIX.md` - 中文字体修复
- `CSV_EXPORT_COMMIT_LINK.md` - CSV 导出功能

### 项目文档
- `README.md` - 项目说明
- `product.md` - 产品功能
- `structure.md` - 项目结构
- `tech.md` - 技术栈

---

## 联系和支持

如有问题或建议，请通过以下方式联系：
- 查看项目文档
- 查看 Git 提交历史
- 查看功能文档（*.md 文件）

---

**最后更新时间**: 2026-01-20 19:30
**版本**: 1.0.0
**状态**: ✅ 所有功能已完成并测试通过
