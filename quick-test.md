# 快速测试指南

## 问题
本地无法连接公司 Jenkins，收藏列表为空，无法测试双击功能。

## 解决方案
使用测试工具创建模拟数据。

## 快速步骤

### 1. 创建测试收藏数据
```bash
test-favorites.bat create
```

### 2. 启动应用
```bash
java -jar target\git-info-viewer-1.0.0-jar-with-dependencies.jar
```

### 3. 测试功能
1. 打开 Jenkins Browser（菜单：Tools → Jenkins Browser）
2. 查看收藏列表（应该有 3 个测试项目）
3. 双击任意收藏项
4. 应该看到 "Loading... please wait" 对话框
5. 对话框会自动关闭，并提示找不到任务（正常，因为没有真实连接）

### 4. 单独测试加载对话框
```bash
test-favorites.bat test-dialog
```

### 5. 清理测试数据
```bash
test-favorites.bat delete
```

## 验证版本
- 窗口标题显示编译时间戳
- 控制台输出包含 "VERSION CHECK" 日志

## 详细文档
参见：`LOCAL_TEST_FAVORITES_GUIDE.md`
