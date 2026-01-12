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
            String projectId = owner + "%2F" + repo;  // URL encode "owner/repo"
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
            String projectId = owner + "%2F" + repo;
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
            String projectId = owner + "%2F" + repo;
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
            String projectId = owner + "%2F" + repo;
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
            String projectId = owner + "%2F" + repo;
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
            String projectId = owner + "%2F" + repo;
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
            String projectId = owner + "%2F" + repo;
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
    public String searchFiles(String owner, String repo, String filename) throws IOException {
        if (isGitLab) {
            // GitLab API: GET /projects/:id/search?scope=blobs&search=:filename
            String projectId = owner + "%2F" + repo;
            return GitLabApiClient.executeGet(baseUrl + "/projects/" + projectId + "/search?scope=blobs&search=" + filename, token);
        } else {
            // GitHub API: GET /search/code?q=filename:xxx+repo:owner/repo
            String query = "filename:" + filename + "+repo:" + owner + "/" + repo;
            return GitHubApiClient.searchCode(query, token);
        }
    }

    /**
     * 获取文件的提交历史
     */
    public String getFileCommits(String owner, String repo, String filepath, String branch) throws IOException {
        if (isGitLab) {
            // GitLab API: GET /projects/:id/repository/commits?path=:filepath
            String projectId = owner + "%2F" + repo;
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
