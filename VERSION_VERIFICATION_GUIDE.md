# JAR包版本验证指南

## 问题：如何确认使用的是最新编译的JAR包？

### 方法1：检查窗口标题（最直观）

**步骤**:
1. 运行JAR包
2. 打开 "Jenkins Job Browser" 对话框
3. 查看窗口标题

**预期结果**:
```
窗口标题应该显示：Jenkins Job Browser - Build: Sat Jan 18 02:25:11 CST 2026
```

如果标题中的时间戳是最新的编译时间，说明使用的是最新JAR包。

---

### 方法2：检查控制台日志

**步骤**:
1. 运行JAR包
2. 打开 "Jenkins Job Browser" 对话框
3. 双击任意收藏的Job
4. 查看控制台输出（命令行窗口）

**预期输出**:
```
=== navigateToJob called ===
VERSION CHECK: FavoritesPanel compiled at: 2026-01-18T02:25:11.xxx
Job: xxx
Job Path: xxx
Parent Dialog: SET
```

如果看到 "VERSION CHECK" 这一行，说明使用的是最新代码。

---

### 方法3：检查JAR文件时间戳

**Windows命令**:
```cmd
dir target\git-info-viewer-1.0.0-jar-with-dependencies.jar
```

**预期输出**:
```
2026/01/18  02:25    xxxxx git-info-viewer-1.0.0-jar-with-dependencies.jar
```

确认文件的修改时间是最新的编译时间。

---

### 方法4：使用MD5校验（最可靠）

**生成MD5**:
```powershell
Get-FileHash target\git-info-viewer-1.0.0-jar-with-dependencies.jar -Algorithm MD5
```

**当前构建的MD5**:
```
运行上述命令获取当前JAR的MD5值，记录下来
```

每次重新编译后，MD5值会改变。如果运行的JAR的MD5与最新编译的MD5不同，说明使用的是旧版本。

---

## 常见问题排查

### Q1: 双击收藏Job没有弹出Loading对话框

**可能原因**:
1. 使用的是旧版本JAR包
2. JAR包没有正确编译
3. 运行的是错误的JAR文件

**解决方法**:
1. 确认窗口标题中的时间戳是最新的
2. 重新编译：`mvn clean package`
3. 确认运行的是 `target\git-info-viewer-1.0.0-jar-with-dependencies.jar`
4. 关闭所有正在运行的旧实例

---

### Q2: 如何确保运行的是最新JAR？

**最佳实践**:
1. 编译前先清理：`mvn clean`
2. 完整编译：`mvn package`
3. 关闭所有旧的运行实例
4. 使用完整路径运行：
   ```cmd
   java -jar D:\ai\project\git\target\git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```
5. 检查窗口标题确认时间戳

---

### Q3: 为什么代码修改后没有生效？

**检查清单**:
- [ ] 代码已保存
- [ ] 运行了 `mvn clean package`
- [ ] 编译成功（看到 "BUILD SUCCESS"）
- [ ] 关闭了所有旧的运行实例
- [ ] 运行的是 `target` 目录下的JAR文件
- [ ] 窗口标题显示最新时间戳

---

## 快速验证脚本

创建一个批处理文件 `verify-and-run.bat`:

```batch
@echo off
echo ========================================
echo 清理旧的编译文件...
echo ========================================
call mvn clean

echo.
echo ========================================
echo 编译最新代码...
echo ========================================
call mvn package

echo.
echo ========================================
echo 检查JAR文件...
echo ========================================
dir target\git-info-viewer-1.0.0-jar-with-dependencies.jar

echo.
echo ========================================
echo 计算MD5...
echo ========================================
powershell -Command "Get-FileHash target\git-info-viewer-1.0.0-jar-with-dependencies.jar -Algorithm MD5 | Select-Object Hash"

echo.
echo ========================================
echo 启动应用...
echo ========================================
echo 请检查窗口标题中的时间戳！
echo.
pause
java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
```

使用方法：
1. 双击运行 `verify-and-run.bat`
2. 等待编译完成
3. 查看MD5值（记录下来）
4. 按任意键启动应用
5. 检查窗口标题中的时间戳

---

## 本次构建信息

**编译时间**: 2026-01-18 02:25:11
**JAR路径**: `target\git-info-viewer-1.0.0-jar-with-dependencies.jar`

**验证方法**:
1. 打开Jenkins Job Browser
2. 窗口标题应显示：`Jenkins Job Browser - Build: Sat Jan 18 02:25:11 CST 2026`
3. 双击收藏Job，应立即弹出 "Loading... please wait" 对话框

---

## 修改内容总结

### 1. 收藏Job导航修复
- 修复了递归加载问题，现在可以正确加载所有层级
- 添加了Loading对话框，双击后立即显示

### 2. Stage View布局优化
- 移除了外层 "Stage View" 边框
- 只保留 "Module List" 和 "Console Log" 两个内层面板
- 使用分割面板，可以调整大小

### 3. 版本验证
- 窗口标题显示构建时间
- 控制台输出显示版本检查信息
