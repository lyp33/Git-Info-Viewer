package com.gitviewer;

import java.io.IOException;

/**
 * 统一的 Git API 客户端
 * 自动检测并支持 GitHub 和 GitLab
 */
public class GitApiClient {

    private String baseUrl;
    private String token;
    private boolean isGitLab;

    public GitApiClient(String remoteUrl, String token) {
        this.token = token;
        this.baseUrl = extractApiBaseUrl(remoteUrl);
        this.isGitLab = isGitLabUrl(remoteUrl);
        
        System.out.println("[Git API] Detected platform: " + (isGitLab ? "GitLab" : "GitHub"));
        System.out.println("[Git API] Base URL: " + baseUrl);
    }

    /**
     * 从 remote URL 提取 API base URL
     */
    private String extractApiBaseUrl(String remoteUrl) {
        try {
            // 移除 "origin : " 前缀
            String cleanUrl = remoteUrl;
            if (remoteUrl.contains(" : ")) {
                cleanUrl = remoteUrl.split(" : ")[1].trim();
            }

            // 提取域名
            String domain;
            if (cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) {
                // https://gitlab.insuremo.com/api/v4/groups/stbd/projects
                domain = cleanUrl.split("/")[2];
            } else if (cleanUrl.startsWith("git@")) {
                // git@gitlab.insuremo.com:gemini_core/claim-bs-core.git
                domain = cleanUrl.split("@")[1].split(":")[0];
            } else {
                return "https://api.github.com";
            }

            // 构建 API base URL
            if (isGitLabUrl(remoteUrl)) {
                return "https://" + domain + "/api/v4";
            } else {
                return "https://api.github.com";
            }
        } catch (Exception e) {
            System.err.println("[Git API] Failed to extract base URL: " + e.getMessage());
            return "https://api.github.com";
        }
    }

    /**
     * 判断是否是 GitLab URL
     */
    private boolean isGitLabUrl(String url) {
        return url.contains("gitlab") || url.contains("/api/v4/");
    }

    /**
     * 获取仓库信息
     */
    public String getRepository(String owner, String repo) throws IOException {
        if (isGitLab) {
            // GitLab API: GET /projects/:id
            // 对 owner 中的 / 也进行 URL 编码
            String projectId = owner.replace("/", "%2F") + "%2F" + repo;
            return GitLabApiClient.executeGet(baseUrl + "/projects/" + projectId, token);
        } else {
            // GitHub API: GET /repos/:owner/:repo
            return GitHubApiClient.executeGet("/repos/" + owner + "/" + repo, token);
        }
    }

    /**
     * 获取 Issues
     */
    public String getIssues(String owner, String repo, String state) throws IOException {
        if (isGitLab) {
            // GitLab API: GET /projects/:id/issues
            String projectId = owner.replace("/", "%2F") + "%2F" + repo;
            String endpoint = baseUrl + "/projects/" + projectId + "/issues";
            if (state != null && !state.isEmpty() && !state.equals("all")) {
                endpoint += "?state=" + state;
            }
            return GitLabApiClient.executeGet(endpoint, token);
        } else {
            // GitHub API
            return GitHubApiClient.getIssues(owner, repo, state, token);
        }
    }

    /**
     * 获取 Pull Requests / Merge Requests
     */
    public String getPullRequests(String owner, String repo, String state) throws IOException {
        if (isGitLab) {
            // GitLab API: GET /projects/:id/merge_requests
            String projectId = owner.replace("/", "%2F") + "%2F" + repo;
            String endpoint = baseUrl + "/projects/" + projectId + "/merge_requests";
            if (state != null && !state.isEmpty() && !state.equals("all")) {
                endpoint += "?state=" + state;
            }
            return GitLabApiClient.executeGet(endpoint, token);
        } else {
            // GitHub API
            return GitHubApiClient.getPullRequests(owner, repo, state, token);
        }
    }

    /**
     * 获取提交记录（支持指定分支）
     */
    public String getCommits(String owner, String repo, String branch) throws IOException {
        if (isGitLab) {
            // GitLab API: GET /projects/:id/repository/commits
            String projectId = owner.replace("/", "%2F") + "%2F" + repo;
            String endpoint = baseUrl + "/projects/" + projectId + "/repository/commits?per_page=10";
            if (branch != null && !branch.isEmpty()) {
                endpoint += "&ref_name=" + branch;
            }
            return GitLabApiClient.executeGet(endpoint, token);
        } else {
            // GitHub API
            return GitHubApiClient.getCommits(owner, repo, branch, token);
        }
    }

    /**
     * 获取提交记录（使用默认分支）
     */
    public String getCommits(String owner, String repo) throws IOException {
        return getCommits(owner, repo, null);
    }

    /**
     * 获取分支列表
     */
    public String getBranches(String owner, String repo) throws IOException {
        if (isGitLab) {
            // GitLab API: GET /projects/:id/repository/branches
            String projectId = owner.replace("/", "%2F") + "%2F" + repo;
            return GitLabApiClient.executeGet(baseUrl + "/projects/" + projectId + "/repository/branches", token);
        } else {
            // GitHub API
            return GitHubApiClient.getBranches(owner, repo, token);
        }
    }

    /**
     * 获取发布版本
     */
    public String getReleases(String owner, String repo) throws IOException {
        if (isGitLab) {
            // GitLab API: GET /projects/:id/releases
            String projectId = owner.replace("/", "%2F") + "%2F" + repo;
            return GitLabApiClient.executeGet(baseUrl + "/projects/" + projectId + "/releases", token);
        } else {
            // GitHub API
            return GitHubApiClient.getReleases(owner, repo, token);
        }
    }

    /**
     * 获取目录内容
     */
    public String getContents(String owner, String repo, String path) throws IOException {
        if (isGitLab) {
            // GitLab API: GET /projects/:id/repository/tree
            String projectId = owner.replace("/", "%2F") + "%2F" + repo;
            String endpoint = baseUrl + "/projects/" + projectId + "/repository/tree";
            if (path != null && !path.isEmpty()) {
                endpoint += "?path=" + path;
            }
            return GitLabApiClient.executeGet(endpoint, token);
        } else {
            // GitHub API
            return GitHubApiClient.getContents(owner, repo, path, token);
        }
    }

    /**
     * 搜索仓库
     */
    public String searchRepositories(String query) throws IOException {
        if (isGitLab) {
            // GitLab API: GET /projects?search=:query
            return GitLabApiClient.executeGet(baseUrl + "/projects?search=" + query + "&per_page=10", token);
        } else {
            // GitHub API
            return GitHubApiClient.searchRepositories(query, token);
        }
    }

    /**
     * 搜索 Issues
     */
    public String searchIssues(String query) throws IOException {
        if (isGitLab) {
            // GitLab 没有全局 issue 搜索，返回提示
            return "{\"message\": \"GitLab does not support global issue search\"}";
        } else {
            // GitHub API
            return GitHubApiClient.searchIssues(query, token);
        }
    }

    /**
     * 搜索文件
     */
    public String searchFiles(String owner, String repo, String filename, String ref) throws IOException {
        if (isGitLab) {
            // GitLab API: GET /projects/:id/search?scope=blobs&search=:filename&ref=:branch
            // scope=blobs 会搜索文件名和文件内容
            String projectId = owner.replace("/", "%2F") + "%2F" + repo;
            String endpoint = baseUrl + "/projects/" + projectId + "/search?scope=blobs&search=" + filename;
            if (ref != null && !ref.isEmpty()) {
                endpoint += "&ref=" + ref;
            }
            return GitLabApiClient.executeGet(endpoint, token);
        } else {
            // GitHub API: GET /search/code?q=xxx+repo:owner/repo
            // 不使用 filename: 前缀，这样会搜索文件名和文件内容
            String query = filename + "+repo:" + owner + "/" + repo;
            return GitHubApiClient.searchCode(query, token);
        }
    }

    /**
     * 获取文件的提交历史
     */
    public String getFileCommits(String owner, String repo, String filepath, String branch) throws IOException {
        if (isGitLab) {
            // GitLab API: GET /projects/:id/repository/commits?path=:filepath
            String projectId = owner.replace("/", "%2F") + "%2F" + repo;
            String endpoint = baseUrl + "/projects/" + projectId + "/repository/commits?path=" + filepath + "&per_page=10";
            if (branch != null && !branch.isEmpty()) {
                endpoint += "&ref_name=" + branch;
            }
            return GitLabApiClient.executeGet(endpoint, token);
        } else {
            // GitHub API: GET /repos/:owner/:repo/commits?path=:filepath
            return GitHubApiClient.getFileCommits(owner, repo, filepath, branch, token);
        }
    }

    /**
     * 获取单个 commit 的详细信息（包括 diff）
     * 
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param sha commit SHA
     * @return commit 详细信息（JSON 格式，包含 diff）
     * @throws IOException 网络错误
     */
    public String getSingleCommit(String owner, String repo, String sha) throws IOException {
        if (isGitLab) {
            // GitLab 需要两个 API 调用：
            // 1. 获取 commit 基本信息
            // 2. 获取 commit diff
            String projectId = owner.replace("/", "%2F") + "%2F" + repo;
            
            // 获取 commit 基本信息
            String commitEndpoint = baseUrl + "/projects/" + projectId + "/repository/commits/" + sha;
            String commitInfo = GitLabApiClient.executeGet(commitEndpoint, token);
            
            // 获取 commit diff
            String diffEndpoint = baseUrl + "/projects/" + projectId + "/repository/commits/" + sha + "/diff";
            String diffInfo = GitLabApiClient.executeGet(diffEndpoint, token);
            
            // 合并两个结果
            return "【Commit 信息】\n" + commitInfo + "\n\n【Diff 内容】\n" + diffInfo;
        } else {
            // GitHub API: GET /repos/:owner/:repo/commits/:sha
            // GitHub 的这个 API 默认就包含 files 数组和 patch（diff）
            return GitHubApiClient.getSingleCommit(owner, repo, sha, token);
        }
    }

    /**
     * 获取文件内容（原始文本）
     * 注意：仅用于文本文件，不要用于二进制文件
     * 
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param filepath 文件路径（例如：src/main/App.java）
     * @param branch 分支名称（可选，null 表示使用默认分支）
     * @return 文件内容（纯文本）
     * @throws IOException 网络错误或文件不存在
     */
    public String getFileContent(String owner, String repo, String filepath, String branch) throws IOException {
        System.out.println("[Git API] getFileContent called:");
        System.out.println("[Git API]   owner: " + owner);
        System.out.println("[Git API]   repo: " + repo);
        System.out.println("[Git API]   filepath: " + filepath);
        System.out.println("[Git API]   branch: " + branch);
        System.out.println("[Git API]   isGitLab: " + isGitLab);
        
        if (isGitLab) {
            // GitLab API: GET /projects/:id/repository/files/:file_path/raw?ref=:branch
            String projectId = owner.replace("/", "%2F") + "%2F" + repo;
            
            // URL encode file path - 使用完整的 URL 编码
            String encodedPath;
            try {
                encodedPath = java.net.URLEncoder.encode(filepath, "UTF-8")
                    .replace("+", "%20");  // URLEncoder 把空格编码为 +，需要替换为 %20
            } catch (java.io.UnsupportedEncodingException e) {
                // UTF-8 总是支持的，这个异常不应该发生
                encodedPath = filepath.replace("/", "%2F");
            }
            
            String endpoint = baseUrl + "/projects/" + projectId + "/repository/files/" + encodedPath + "/raw";
            if (branch != null && !branch.isEmpty()) {
                endpoint += "?ref=" + branch;
            }
            
            System.out.println("[Git API] GitLab API endpoint: " + endpoint);
            System.out.println("[Git API] Calling GitLabApiClient.executeGet...");
            
            try {
                String result = GitLabApiClient.executeGet(endpoint, token);
                System.out.println("[Git API] File content retrieved successfully, length: " + 
                    (result != null ? result.length() : 0) + " chars");
                if (result != null && result.length() > 0) {
                    System.out.println("[Git API] Content preview: " + 
                        result.substring(0, Math.min(100, result.length())));
                }
                return result;
            } catch (IOException e) {
                System.err.println("[Git API] ERROR: Failed to get file content: " + e.getMessage());
                throw e;
            }
        } else {
            // GitHub API: GET /repos/:owner/:repo/contents/:path?ref=:branch
            // 返回的是 JSON，需要解析 base64 编码的 content 字段
            String endpoint = "/repos/" + owner + "/" + repo + "/contents/" + filepath;
            if (branch != null && !branch.isEmpty()) {
                endpoint += "?ref=" + branch;
            }
            
            System.out.println("[Git API] GitHub API endpoint: " + endpoint);
            
            String jsonResponse = GitHubApiClient.executeGet(endpoint, token);
            
            // 解析 JSON 并解码 base64 content
            return decodeGitHubFileContent(jsonResponse);
        }
    }

    /**
     * 从 GitHub API 响应中解码文件内容
     * GitHub 返回的 JSON 格式：
     * {
     *   "name": "file.txt",
     *   "path": "path/to/file.txt",
     *   "content": "base64_encoded_content",
     *   "encoding": "base64"
     * }
     */
    private String decodeGitHubFileContent(String jsonResponse) throws IOException {
        try {
            // 提取 content 字段 - 使用更宽松的正则，支持多行 base64 内容
            // [\s\S]*? 可以匹配任意字符（包括换行符），? 表示非贪婪匹配
            String pattern = "\"content\"\\s*:\\s*\"([\\s\\S]*?)\"(?=\\s*,|\\s*})";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(jsonResponse);
            
            if (m.find()) {
                String base64Content = m.group(1);
                // 移除所有换行符和转义符（GitHub 的 base64 内容包含 \n 转义符）
                base64Content = base64Content.replace("\\n", "")
                                             .replace("\\r", "")
                                             .replace("\n", "")
                                             .replace("\r", "")
                                             .replace(" ", "");
                
                // Base64 解码
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Content);
                return new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            } else {
                throw new IOException("Failed to extract content from GitHub API response");
            }
        } catch (IllegalArgumentException e) {
            throw new IOException("Failed to decode base64 content: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Failed to decode GitHub file content: " + e.getMessage(), e);
        }
    }

    /**
     * 判断是否是 GitLab
     */
    public boolean isGitLab() {
        return isGitLab;
    }

    /**
     * 获取平台名称
     */
    public String getPlatformName() {
        return isGitLab ? "GitLab" : "GitHub";
    }
}
