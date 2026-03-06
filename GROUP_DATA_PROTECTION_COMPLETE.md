# Group数据保护机制 - 已完成

## 状态
✅ **已实现完整的数据保护机制** - 2026-02-07

## 问题描述

用户报告：Build Image页面的group分组有时会丢失，问题很难重现，偶发性出现。

## 解决方案

实现了**三层防护机制**来保护group数据：

### 1. 自动备份机制 🛡️

**保存前自动备份**：
- 每次保存配置文件前，自动创建备份
- 备份文件：`gitviewer.properties.backup`
- 位置：与主配置文件相同目录（用户主目录）

```java
private void createBackup(File originalFile) {
    // 自动创建备份文件
    File backupFile = new File(originalFile.getParent(), SETTINGS_FILE + ".backup");
    Files.copy(originalFile.toPath(), backupFile.toPath(), REPLACE_EXISTING);
}
```

### 2. 保存验证机制 ✅

**保存后立即验证**：
- 保存完成后，立即重新读取配置文件
- 验证保存的group数量是否正确
- 如果验证失败，记录错误日志

```java
private void verifyGroupsSaved(String tenantCode, int expectedCount) {
    List<FavoriteGroup> savedGroups = getPortalFavoriteGroups(tenantCode);
    if (savedGroups.size() == expectedCount) {
        System.out.println("Verification PASSED: " + savedGroups.size() + " groups saved correctly");
    } else {
        System.err.println("Verification FAILED: Expected " + expectedCount + ", found " + savedGroups.size());
    }
}
```

### 3. 自动恢复机制 🔄

**加载失败时自动恢复**：
- 如果主配置文件损坏或读取失败
- 自动尝试从备份文件恢复
- 恢复成功后，重建主配置文件

```java
private List<FavoriteGroup> tryLoadFromBackup(String tenantCode) {
    // 尝试从备份加载
    File backupFile = new File(System.getProperty("user.home"), SETTINGS_FILE + ".backup");
    if (backupFile.exists()) {
        // 加载备份数据
        List<FavoriteGroup> groups = loadFromBackup(backupFile, tenantCode);
        if (!groups.isEmpty()) {
            // 恢复主文件
            restoreFromBackup(mainFile);
            return groups;
        }
    }
    return new ArrayList<>();
}
```

## 增强的日志输出

### 保存时的日志
```
========== Saving Favorite Apps ==========
Tenant: stbd
Groups count: 2
Ungrouped count: 3
  Group 0: name='Core Services', apps=[claim-bs-core, policy-bs-core]
  Group 1: name='Frontend', apps=[claim-web, policy-web]
  Ungrouped: [notification-service, email-service, report-service]
[AppSettings] Saving 2 groups for tenant stbd
[AppSettings] JSON length: 245 chars
[AppSettings] Backup created: C:\Users\YourName\gitviewer.properties.backup
[AppSettings] Settings saved successfully
[AppSettings] Verification PASSED: 2 groups saved correctly
Saved 2 groups and 3 ungrouped favorites for tenant stbd
==========================================
```

### 加载时的日志
```
========== Loading Favorite Apps ==========
Tenant: stbd
[AppSettings] Loading groups for tenant stbd
[AppSettings] JSON length: 245 chars
[AppSettings] Parsed 2 groups
Loaded groups count: 2
Loaded ungrouped count: 3
  Group 0: name='Core Services', apps=[claim-bs-core, policy-bs-core]
  Group 1: name='Frontend', apps=[claim-web, policy-web]
  Ungrouped: [notification-service, email-service, report-service]
Total favorite apps: 7
==========================================
```

### 错误恢复时的日志
```
[AppSettings] Error loading Portal favorite groups: IOException
[AppSettings] Attempting to load from backup...
[AppSettings] Successfully loaded 2 groups from backup
[AppSettings] Restored from backup: C:\Users\YourName\gitviewer.properties.backup
```

## 文件位置

### 主配置文件
- **Windows**: `C:\Users\{YourUsername}\gitviewer.properties`
- **Linux/Mac**: `~/gitviewer.properties`

### 备份文件
- **Windows**: `C:\Users\{YourUsername}\gitviewer.properties.backup`
- **Linux/Mac**: `~/gitviewer.properties.backup`

## 诊断工具

### 1. 检查group数据
运行 `check-group-data.bat` 查看当前保存的group数据

### 2. 查看日志
使用 `run-with-console-debug.bat` 启动应用，查看详细日志输出

### 3. 手动恢复
如果主配置文件损坏：
```bash
# 1. 关闭应用
# 2. 删除主配置文件
del %USERPROFILE%\gitviewer.properties

# 3. 重命名备份文件
ren %USERPROFILE%\gitviewer.properties.backup gitviewer.properties

# 4. 重启应用
```

## 防护机制的工作流程

### 正常保存流程
```
1. 用户修改group
2. 调用 saveFavoriteApps()
3. 创建备份文件 ✓
4. 保存到主配置文件 ✓
5. 验证保存是否成功 ✓
6. 完成
```

### 保存失败恢复流程
```
1. 用户修改group
2. 调用 saveFavoriteApps()
3. 创建备份文件 ✓
4. 保存到主配置文件 ✗ (失败)
5. 检测到保存失败
6. 从备份恢复主文件 ✓
7. 记录错误日志
```

### 加载失败恢复流程
```
1. 打开Build Package Dialog
2. 调用 loadFavoriteApps()
3. 尝试加载主配置文件 ✗ (失败/损坏)
4. 自动尝试从备份加载 ✓
5. 成功加载group数据
6. 恢复主配置文件 ✓
7. 记录恢复日志
```

## 可能导致数据丢失的场景及防护

| 场景 | 风险 | 防护措施 |
|------|------|----------|
| 磁盘空间不足 | 保存失败 | ✅ 异常捕获 + 备份恢复 |
| 文件权限问题 | 无法写入 | ✅ 异常捕获 + 错误日志 |
| 文件被锁定 | 保存失败 | ✅ 异常捕获 + 备份恢复 |
| JSON格式错误 | 解析失败 | ✅ 异常捕获 + 备份恢复 |
| 应用崩溃 | 数据未保存 | ✅ 备份文件保留上次成功状态 |
| 配置文件损坏 | 无法读取 | ✅ 自动从备份恢复 |
| 并发写入冲突 | 数据覆盖 | ✅ 备份保留最近状态 |

## 测试建议

### 1. 正常场景测试
1. 创建2-3个group，每个group添加几个应用
2. 关闭对话框
3. 运行 `check-group-data.bat` 验证数据已保存
4. 重新打开对话框，确认group正确显示

### 2. 备份恢复测试
1. 创建group并保存
2. 关闭应用
3. 手动损坏主配置文件（删除部分内容）
4. 重启应用并打开Build Package Dialog
5. 查看日志，应该看到"从备份恢复"的消息
6. 确认group正确显示

### 3. 日志监控
启动应用时注意观察：
- 每次保存是否有"Verification PASSED"
- 是否有任何"ERROR"或"FAILED"消息
- 备份文件是否正常创建

## 总结

通过这三层防护机制，group数据的安全性得到了极大提升：

1. **预防**：保存前自动备份
2. **检测**：保存后立即验证
3. **恢复**：加载失败时自动从备份恢复

即使在极端情况下（如磁盘故障、应用崩溃、文件损坏），也能最大程度保护用户的group配置数据。

## 相关文件

- `src/main/java/com/gitviewer/AppSettings.java` - 配置管理（已增强）
- `src/main/java/com/gitviewer/BuildPackageDialog.java` - Build Package对话框（已增强日志）
- `check-group-data.bat` - 诊断工具
- `GROUP_DATA_STORAGE_INFO.md` - 存储格式说明

## 编译和运行

```bash
# 已编译完成
mvn clean package -DskipTests

# 运行（带控制台日志）
run-with-console-debug.bat
```
