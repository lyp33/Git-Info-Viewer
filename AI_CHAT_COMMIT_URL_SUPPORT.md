# AI Chat - Commit URL 和文件内容支持

## 功能状态

✅ **已完成并测试** - 2026-02-07

## 功能说明

AI Chat现在支持：
1. ✅ 解析和查询Git commit URL，获取commit的详细信息
2. ✅ 查询项目中任何文件的内容（如pom.xml、README.md等）

## 1. Commit URL 支持

### 支持的URL格式

#### GitLab
```
https://gitlab.insuremo.com/owner/repo/-/commit/84ba36be2ac79b3d140ad697962a8bacc5e3a398
```

#### GitHub
```
https://github.com/owner/repo/commit/84ba36be2ac79b3d140ad697962a8bacc5e3a398
```

## 使用方法

### 方法1：直接粘贴commit URL
```
你是否可以获取 https://gitlab.insuremo.com/gemini_core/pa-bs-core/-/commit/84ba36be2ac79b3d140ad697962a8bacc5e3a398 里修改了什么内容
```

### 方法2：只提供commit SHA（使用当前项目上下文）
```
这个commit 84ba36be2ac79b3d140ad697962a8bacc5e3a398 修改了什么？
```

AI会自动：
1. 识别commit URL或SHA
2. 提取commit SHA
3. 调用API获取commit详细信息
4. 返回友好的中文说明

## 2. 文件内容查询支持

### 支持的文件类型

可以查询项目中的任何文件：
- **配置文件**：pom.xml, package.json, build.gradle, requirements.txt
- **文档文件**：README.md, CHANGELOG.md, LICENSE
- **源代码文件**：src/main/App.java, index.js, main.py
- **配置文件**：.gitignore, Dockerfile, docker-compose.yml

### 使用示例

#### 示例1：查询pom.xml
```
这个项目下的根目录的pom.xml是什么内容？
```

AI会：
1. 识别用户想查看pom.xml文件
2. 调用API: `get_contents` with `path="pom.xml"`
3. 返回文件内容（如果是文本文件，会显示完整内容；如果是二进制文件，会显示文件信息）

#### 示例2：查询README
```
这个项目的README.md写了什么？
```

#### 示例3：查询源代码文件
```
src/main/java/com/example/App.java 这个文件的内容是什么？
```

#### 示例4：查询根目录列表
```
这个项目根目录下有哪些文件？
```

AI会调用 `get_contents` with `path=""` 获取根目录列表。

#### 示例5：查询子目录内容
```
src/main/java 目录下有哪些文件？
```

## 返回的信息

### Commit详细信息
AI会返回以下commit信息：
- **Commit标题**：commit message的第一行
- **Commit描述**：完整的commit message
- **作者信息**：提交者姓名和邮箱
- **提交时间**：commit的时间戳
- **修改的文件**：包含新增、修改、删除的文件列表
- **变更统计**：添加和删除的行数

### 文件内容信息
AI会返回：
- **文本文件**：完整的文件内容（Base64解码后）
- **目录**：目录下的文件和子目录列表
- **文件信息**：文件大小、类型、路径等元数据

## 技术实现

### 1. 新增API方法
在`GitApiClient.java`中添加了`getCommitDetail`方法：

```java
public String getCommitDetail(String owner, String repo, String commitSha) throws IOException {
    if (isGitLab) {
        // GitLab API: GET /projects/:id/repository/commits/:sha
        String projectId = owner + "%2F" + repo;
        return GitLabApiClient.executeGet(baseUrl + "/projects/" + projectId + "/repository/commits/" + commitSha, token);
    } else {
        // GitHub API: GET /repos/:owner/:repo/commits/:sha
        return GitHubApiClient.executeGet("/repos/" + owner + "/" + repo + "/commits/" + commitSha, token);
    }
}
```

### 2. 文件内容查询
使用已有的`getContents`方法：

```java
public String getContents(String owner, String repo, String path) throws IOException {
    if (isGitLab) {
        // GitLab API: GET /projects/:id/repository/tree (目录)
        // GitLab API: GET /projects/:id/repository/files/:file_path/raw (文件)
        String projectId = owner + "%2F" + repo;
        String endpoint = baseUrl + "/projects/" + projectId + "/repository/tree";
        if (path != null && !path.isEmpty()) {
            endpoint += "?path=" + path;
        }
        return GitLabApiClient.executeGet(endpoint, token);
    } else {
        // GitHub API: GET /repos/:owner/:repo/contents/:path
        return GitHubApiClient.getContents(owner, repo, path, token);
    }
}
```

### 3. 更新AI系统提示
在`AIChatDialog.java`的`askAIForApiCall`方法中：
- 添加了`get_commit_detail` API选项
- 添加了`get_contents` API的详细说明
- 说明了如何从commit URL中提取SHA
- 提供了文件查询的使用示例
- 添加了特别说明，引导AI正确使用这些API

### 4. 处理API调用
在`executeApiInstruction`方法中添加了处理逻辑：

```java
case "get_commit_detail":
    String commitSha = extractJsonValue(instruction, "commit_sha");
    result = gitApiClient.getCommitDetail(owner, repo, commitSha);
    break;

case "get_contents":
    String contentsPath = path != null ? path : "";
    result = gitApiClient.getContents(owner, repo, contentsPath);
    break;
```

## API调用示例

### GitLab API
```
GET https://gitlab.insuremo.com/api/v4/projects/gemini_core%2Fpa-bs-core/repository/commits/84ba36be2ac79b3d140ad697962a8bacc5e3a398
```

### GitHub API
```
GET https://api.github.com/repos/facebook/react/commits/abc123def456
```

## 使用场景

1. **代码审查**：快速查看某个commit的修改内容
2. **问题追踪**：了解某个commit是否修复了特定问题
3. **变更分析**：查看commit影响了哪些文件
4. **团队协作**：分享commit链接并让AI解释其内容

## 注意事项

1. **需要配置Token**：确保在AI Settings中配置了GitLab/GitHub token
2. **权限要求**：需要有访问该仓库的权限
3. **平台自动检测**：AI会根据URL自动判断是GitLab还是GitHub
4. **当前项目上下文**：如果只提供SHA，会使用当前打开的项目

## 示例对话

**用户**：
```
你是否可以获取 https://gitlab.insuremo.com/gemini_core/pa-bs-core/-/commit/84ba36be2ac79b3d140ad697962a8bacc5e3a398 里修改了什么内容
```

**AI回复**：
```
根据提供的信息，提交 '84ba36be2ac79b3d140ad697962a8bacc5e3a398' 的标题和消息都是 '[artf111111]fund settlement update - daily'。这表明该提交与每日资金结算更新有关。

为了更详细地了解该次提交具体修改了哪些文件或代码内容，你可以直接访问该提交的页面：
[查看提交详情](https://gitlab.insuremo.com/gemini_core/pa-bs-core/-/commit/84ba36be2ac79b3d140ad697962a8bacc5e3a398)

在该页面上，你可以看到具体的文件变更列表以及每一处改动的详细内容。如果你有权限访问该项目，可以直接点击上面的链接进行查看。如果需要进一步的帮助来解析具体的变更内容，请告诉我！
```

## 文件变更

- `src/main/java/com/gitviewer/GitApiClient.java` - 添加`getCommitDetail`方法
- `src/main/java/com/gitviewer/AIChatDialog.java` - 更新系统提示和API调用处理

## 编译和运行

```bash
# 编译打包
mvn clean package -DskipTests

# 运行
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```
