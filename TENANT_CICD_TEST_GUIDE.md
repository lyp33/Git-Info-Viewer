# Tenant CI/CD 功能测试指南

## 测试日期
2026-01-20

## 编译状态
✅ **编译成功** - 无错误，无警告（除了已知的deprecated API）
✅ **打包成功** - JAR文件已生成

## 生成的文件
- `target/git-info-viewer-1.0.0-jar-with-dependencies.jar` (4.1 MB)
- `target/git-info-viewer-1.0.0.jar` (468 KB)

---

## 运行应用

### 方式1: 直接运行JAR
```bash
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

### 方式2: 使用批处理文件（如果存在）
```bash
run-with-console.bat
```

---

## 测试清单

### 第一步: 配置Portal设置

1. **打开Portal Settings**
   - 启动应用
   - 点击菜单: `CI/CD` → `Portal Settings...`
   
2. **输入配置信息**
   - Username: `[你的Portal用户名]`
   - Password: `[你的Portal密码]`
   - Tenant Codes: `thailife` (或其他tenant，用逗号分隔多个)
   
3. **保存设置**
   - 点击 `Save` 按钮
   - 确认看到 "Portal settings saved successfully" 消息
   
4. **验证密码加密**
   - 关闭并重新打开Portal Settings
   - 确认密码字段显示加密后的密码（不是明文）

---

### 第二步: 测试连接功能

1. **打开Tenant CI/CD对话框**
   - 点击菜单: `CI/CD` → `Tenant CI/CD...`
   
2. **选择Tenant**
   - 从下拉框选择一个tenant（例如: thailife）
   
3. **点击Connect按钮**
   - 观察Loading指示器显示 "Connecting..."
   - 等待连接完成
   
4. **验证连接成功**
   - ✅ 状态标签显示: "Connected successfully to [tenant]" (绿色)
   - ✅ Search按钮变为可用
   - ✅ App Name下拉框被填充
   
5. **测试连接失败场景**（可选）
   - 修改Portal Settings中的密码为错误值
   - 尝试连接
   - ✅ 确认看到错误消息
   - ✅ 状态标签显示 "Connection failed" (红色)

---

### 第三步: 测试Plan查询

1. **输入Plan Name**
   - 在 "Plan Name" 字段输入一个plan前缀
   - 例如: `v202601200722` 或 `003`
   
2. **点击Search按钮**
   - 观察Loading指示器显示 "Searching by plan..."
   - 等待查询完成
   
3. **验证结果显示**
   - ✅ 表格显示构建结果
   - ✅ 6列数据正确显示: App Name, Image Name, Build Status, Create Time, Version, Git Branch
   - ✅ Build Status列有颜色编码:
     - 绿色 = Success
     - 红色 = Failed
     - 橙色 = Running
   - ✅ 状态标签显示结果数量
   
4. **测试无结果场景**
   - 输入不存在的plan名称
   - ✅ 确认看到 "No plan found matching the entered name" 消息

---

### 第四步: 测试App查询

1. **清空Plan Name字段**
   
2. **选择App Name**
   - 点击App Name下拉框
   - 选择一个应用，或输入关键字过滤
   
3. **测试实时过滤功能**
   - 在App Name下拉框中输入几个字符
   - ✅ 确认下拉列表实时过滤（300ms延迟）
   - ✅ 只显示包含输入关键字的应用
   
4. **设置查询参数**（可选）
   - Creator: 保持默认或修改
   - Page Size: 默认10，可修改
   - Page Number: 默认0，可修改
   
5. **点击Search按钮**
   - 观察Loading指示器
   - 等待查询完成
   
6. **验证结果**
   - ✅ 表格显示构建结果
   - ✅ 数据格式正确
   - ✅ 可以点击列标题排序

---

### 第五步: 测试CSV导出

1. **执行查询获取结果**
   
2. **点击 "Download CSV" 按钮**
   - 选择保存位置
   - 文件名默认为: `tenant-cicd-results-[timestamp].csv`
   
3. **验证CSV文件**
   - ✅ 文件成功保存
   - ✅ 打开CSV文件，确认:
     - 表头正确: App Name,Image Name,Build Status,Create Time,Version,Git Branch
     - 数据行正确
     - 特殊字符（逗号、引号、换行）正确转义
   
4. **测试空结果场景**
   - 清空结果表格
   - 点击 "Download CSV"
   - ✅ 确认看到 "No results to export" 消息

---

### 第六步: 测试复制镜像名称

1. **执行查询获取结果**
   
2. **点击 "Copy Image Names" 按钮**
   - ✅ 确认看到成功消息，显示复制的数量
   
3. **验证剪贴板内容**
   - 粘贴到文本编辑器
   - ✅ 确认每行一个镜像名称
   - ✅ 格式正确，用换行符分隔

---

### 第七步: 测试Build按钮

1. **连接到tenant**
   
2. **点击 "Build" 按钮**
   - ✅ 确认看到 "Not implemented" 消息
   - ✅ 按钮功能正常（占位符）

---

### 第八步: 测试资源清理和内存管理

1. **多次打开/关闭对话框**
   - 打开Tenant CI/CD对话框
   - 连接到tenant
   - 执行查询
   - 关闭对话框
   - 重复5-10次
   
2. **验证内存管理**
   - ✅ 应用响应正常，无卡顿
   - ✅ 无内存泄漏迹象
   - ✅ 每次打开对话框都能正常工作

3. **测试异步操作取消**
   - 点击Connect按钮
   - 在连接完成前关闭对话框
   - ✅ 确认无异常或错误
   - 重新打开对话框
   - ✅ 功能正常

---

### 第九步: 测试大数据集性能

1. **查询大量结果**
   - 使用App查询，不指定app name
   - 设置较大的Page Size（例如: 100）
   
2. **验证性能**
   - ✅ 如果结果>100条，确认看到警告对话框
   - ✅ 表格加载流畅
   - ✅ 排序功能正常
   
3. **测试App Name过滤性能**
   - 连接后等待应用列表加载
   - 在App Name下拉框快速输入
   - ✅ 确认防抖机制工作（300ms延迟）
   - ✅ UI不冻结

---

### 第十步: 测试错误处理

1. **网络错误**
   - 断开网络连接
   - 尝试连接
   - ✅ 确认看到友好的错误消息
   - ✅ 日志记录错误详情
   
2. **认证错误**
   - 使用错误的密码
   - 尝试连接
   - ✅ 确认看到认证失败消息
   - ✅ 状态标签更新为 "Connection failed"
   
3. **API错误**
   - 使用无效的查询参数
   - ✅ 确认错误被正确处理和显示

---

## 日志验证

### 检查日志输出

1. **启动应用时查看控制台**
   - ✅ 确认看到 "PortalApiClient initialized" 日志
   
2. **执行操作时**
   - ✅ Connect操作: 看到 "=== Getting Token ===" 日志
   - ✅ 查询操作: 看到 "=== Executing Plan/App Query ===" 日志
   - ✅ API请求: 看到请求URL和响应状态码
   - ✅ 敏感信息被屏蔽（密码、token部分隐藏）

---

## 与现有功能的兼容性测试

### 验证不影响其他功能

1. **Git功能**
   - ✅ 打开Git仓库
   - ✅ 查看提交历史
   - ✅ 切换分支
   - ✅ 所有Git功能正常
   
2. **Jenkins功能**
   - ✅ 打开Jenkins Browser
   - ✅ 查看构建历史
   - ✅ 查看Stage Log
   - ✅ 所有Jenkins功能正常
   
3. **其他功能**
   - ✅ 设置对话框正常
   - ✅ 字体设置正常
   - ✅ 文件搜索正常

---

## 已知问题和限制

### 正常行为
1. **Build按钮**: 显示 "Not implemented" - 这是预期行为，功能待后续实现
2. **Deprecated API警告**: 编译时的警告来自JGit库，不影响功能

### P2优先级问题（轻微，不影响使用）
1. BASE_URL硬编码（可后续配置化）
2. 部分日志可能包含敏感信息（已屏蔽关键部分）
3. 时区显示可能需要更明确的说明

---

## 测试结果记录

### 测试环境
- 操作系统: Windows
- Java版本: 17
- Maven版本: 3.6+
- 应用版本: 1.0.0

### 测试日期
- 编译测试: 2026-01-20 ✅
- 功能测试: [待填写]

### 测试人员
- [待填写]

### 测试结果
- [ ] 第一步: Portal Settings配置
- [ ] 第二步: 连接功能
- [ ] 第三步: Plan查询
- [ ] 第四步: App查询
- [ ] 第五步: CSV导出
- [ ] 第六步: 复制镜像名称
- [ ] 第七步: Build按钮
- [ ] 第八步: 资源清理
- [ ] 第九步: 大数据集性能
- [ ] 第十步: 错误处理
- [ ] 兼容性测试

### 发现的问题
[在此记录测试中发现的任何问题]

---

## 快速测试命令

```bash
# 编译
mvn clean compile

# 打包
mvn package

# 运行
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar

# 查看日志（如果使用批处理文件）
run-with-console.bat
```

---

## 支持和文档

### 相关文档
- [需求文档](.kiro/specs/tenant-cicd/requirements.md)
- [设计文档](.kiro/specs/tenant-cicd/design.md)
- [任务列表](.kiro/specs/tenant-cicd/tasks.md)
- [实现完成文档](TENANT_CICD_IMPLEMENTATION_COMPLETE.md)
- [关键修复文档](TENANT_CICD_CRITICAL_FIXES.md)

### 问题反馈
如果在测试中发现问题，请记录：
1. 问题描述
2. 重现步骤
3. 预期行为
4. 实际行为
5. 日志输出（如果有）

---

## 总结

Tenant CI/CD功能已完成开发和代码审查，所有关键问题已修复。
现在可以进行完整的功能测试和用户验收测试。

**祝测试顺利！** 🎉
