# Git Info Viewer v1.0.0 - Release Notes

**发布日期**: 2026-01-20  
**版本**: 1.0.0  
**状态**: ✅ Production Ready

---

## 🎉 新功能

### 1. Portal Log Ctrl+F 搜索功能

在 Stage Log 对话框中添加了完整的文本搜索功能，支持快速查找和导航。

**快捷键**：
- `Ctrl+F` - 打开搜索
- `F3` - 下一个
- `Shift+F3` - 上一个
- `ESC` - 关闭搜索

**特性**：
- ✅ 黄色高亮显示所有匹配项
- ✅ 显示匹配计数（"1 of 5"）
- ✅ 自动滚动到匹配位置
- ✅ 不区分大小写
- ✅ 循环导航
- ✅ 支持 Jenkins Log 和 Portal Log

---

## 🐛 Bug 修复

### 1. Portal API 嵌套 JSON 解析

**问题**: Portal API 返回的 `build_output` 在 `callback` 对象内部，导致解析失败

**修复**: 
- ✅ 支持检查 `callback.build_output` 嵌套结构
- ✅ 向后兼容根级别 `build_output`
- ✅ 添加详细调试日志

### 2. Portal Log UI 冻结

**问题**: 大量日志内容（几 MB）导致 UI 完全冻结

**修复**: 
- ✅ 限制显示内容为 500KB
- ✅ 超过限制时显示警告并截断

### 3. Portal Log 换行符显示

**问题**: JSON 中的 `\n` 显示为字面字符串

**修复**: 
- ✅ 自动解码转义序列（`\n`, `\r`, `\t`, `\uXXXX`）

### 4. Portal URL 提取不完整

**问题**: 
- 第一个 curl URL 缺少查询参数
- URL 提取不支持带引号格式

**修复**: 
- ✅ 使用最后一个匹配的 URL
- ✅ 支持带引号和不带引号的 URL 格式

### 5. Stage Log 中文显示

**问题**: 中文字符显示为方框

**修复**: 
- ✅ 使用 Microsoft YaHei 字体

---

## 📦 下载和安装

### 下载

```
target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

### 运行

```bash
java -jar git-info-viewer-1.0.0-jar-with-dependencies.jar
```

### 系统要求

- Java 17 或更高版本
- Windows / macOS / Linux

---

## 📚 文档

### 新增文档
- `PORTAL_LOG_SEARCH_FEATURE.md` - 搜索功能完整指南
- `PORTAL_METHODS_COMPARISON.md` - API 方法对比
- `PORTAL_BUILD_OUTPUT_NESTED_JSON_FIX.md` - JSON 解析修复说明
- `COMPLETE_FEATURES_SUMMARY.md` - 完整功能总结

### 更新文档
- `README.md` - 项目说明
- 各功能修复文档（*.md）

---

## 🔧 技术改进

### 代码质量
- ✅ 添加详细的调试日志
- ✅ 改进错误处理
- ✅ 优化性能（缓存、限制显示大小）

### 用户体验
- ✅ 快捷键支持
- ✅ 视觉反馈（高亮、计数）
- ✅ 自动滚动
- ✅ 中文支持

---

## 📊 统计信息

### 代码变更
- **修改文件**: 2 个核心文件
  - `JenkinsStageLogDialog.java` - 搜索功能
  - `JenkinsApiClient.java` - JSON 解析修复
- **新增代码**: ~400 行
- **新增文档**: 4 个文档文件

### Git 提交
```
81822b4 feat: Add Ctrl+F search functionality to Portal Log and fix nested JSON parsing
1a9685e Fix Portal Log UI freeze by limiting text size
30c1ee4 Fix Portal Log newline handling - decode escape sequences
00120bf Fix Portal URL extraction to use LAST match
535a504 Add version logging and detailed debug info
```

---

## 🎯 使用场景

### 场景 1: 快速查找错误日志

1. 打开 Stage Log
2. 按 `Ctrl+F`
3. 搜索 "error" 或 "exception"
4. 使用 `F3` 快速浏览所有错误

### 场景 2: 查看 Portal 构建输出

1. 双击 Stage 打开 Stage Log
2. 切换到 "Portal Log" Tab
3. 查看 Portal API 返回的完整构建日志
4. 使用 `Ctrl+F` 搜索关键信息

### 场景 3: 分析构建失败原因

1. 在 Build History 中找到失败的构建
2. 查看 Stage View
3. 双击失败的 Stage
4. 在 Jenkins Log 和 Portal Log 中搜索错误信息

---

## ⚠️ 已知限制

1. **Portal Log 大小限制**: 最多显示 500KB（防止 UI 冻结）
2. **搜索功能**: 不支持正则表达式
3. **搜索选项**: 不支持区分大小写选项

---

## 🚀 未来计划

### v1.1.0 计划功能
- [ ] 正则表达式搜索支持
- [ ] 区分大小写选项
- [ ] 全词匹配选项
- [ ] 搜索历史记录
- [ ] 虚拟滚动支持（更大的日志文件）

---

## 🙏 致谢

感谢所有测试和反馈的用户！

---

## 📞 支持

如有问题或建议：
1. 查看项目文档（*.md 文件）
2. 查看 Git 提交历史
3. 查看功能文档

---

**构建时间**: 2026-01-20 19:19  
**构建状态**: ✅ SUCCESS  
**构建时长**: 17.495 秒

---

## 快速开始

```bash
# 1. 下载 JAR 文件
# target/git-info-viewer-1.0.0-jar-with-dependencies.jar

# 2. 运行应用
java -jar git-info-viewer-1.0.0-jar-with-dependencies.jar

# 3. 打开 Jenkins Browser
# 菜单 -> Jenkins -> Jenkins Browser

# 4. 查看 Stage Log
# 双击 Build -> 双击 Stage

# 5. 使用搜索功能
# 按 Ctrl+F 开始搜索
```

---

**Enjoy! 🎉**
