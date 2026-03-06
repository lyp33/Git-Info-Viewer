# Group数据存储说明

## 存储位置

Group信息存储在用户主目录下的配置文件中：

**Windows**: `C:\Users\{YourUsername}\gitviewer.properties`
**Linux/Mac**: `~/gitviewer.properties`

## 存储格式

### 1. 分组数据
```properties
portal.favorite.groups.{tenantCode}=[JSON数组]
```

JSON格式：
```json
[
  {
    "name": "Group1",
    "expanded": true,
    "appNames": ["app1", "app2", "app3"]
  },
  {
    "name": "Group2",
    "expanded": false,
    "appNames": ["app4", "app5"]
  }
]
```

### 2. 未分组的收藏应用
```properties
portal.ungrouped.favorites.{tenantCode}=app6,app7,app8
```

## 示例

假设tenant是`stbd`，配置文件中会有：

```properties
portal.favorite.groups.stbd=[{"name":"Core Services","expanded":true,"appNames":["claim-bs-core","policy-bs-core"]},{"name":"Frontend","expanded":true,"appNames":["claim-web","policy-web"]}]
portal.ungrouped.favorites.stbd=notification-service,email-service
```

## 诊断工具

运行 `check-group-data.bat` 可以查看当前保存的group数据。

## 可能导致Group丢失的原因

### 1. 文件写入失败
- 磁盘空间不足
- 文件权限问题
- 文件被其他程序锁定

### 2. JSON格式错误
- 应用名称包含特殊字符（如双引号、反斜杠）
- JSON序列化失败

### 3. 并发问题
- 多个对话框同时保存配置
- 保存过程中被中断

### 4. 代码逻辑问题
- `saveFavoriteApps()`未被正确调用
- `favoriteGroups`列表在保存前被清空

## 调试建议

### 1. 检查日志输出
在控制台查看以下日志：
```
Loaded X groups and Y ungrouped favorites for tenant Z
Saved X groups and Y ungrouped favorites for tenant Z
```

### 2. 检查配置文件
运行 `check-group-data.bat` 查看实际保存的数据

### 3. 手动验证
1. 创建一个group并添加应用
2. 关闭对话框
3. 立即运行 `check-group-data.bat`
4. 检查group数据是否已保存

### 4. 添加调试日志
在关键位置添加日志输出：
- `saveFavoriteApps()` 方法开始和结束
- `setPortalFavoriteGroups()` 方法中的JSON序列化
- 文件写入操作

## 修复建议

如果group经常丢失，可以考虑：

1. **增加保存确认**：在保存后立即读取验证
2. **添加备份机制**：保存时创建备份文件
3. **改进错误处理**：捕获并记录所有异常
4. **添加自动恢复**：检测到数据丢失时从备份恢复

## 临时解决方案

如果group已经丢失，可以：

1. 手动编辑 `gitviewer.properties` 文件
2. 添加或修复group数据
3. 重启应用

示例手动添加：
```properties
portal.favorite.groups.stbd=[{"name":"My Group","expanded":true,"appNames":["app1","app2"]}]
portal.ungrouped.favorites.stbd=app3,app4
```
