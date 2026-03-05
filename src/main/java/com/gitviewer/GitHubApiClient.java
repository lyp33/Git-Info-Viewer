package com.gitviewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * GitHub API 客户端
 * 用于查询 GitHub 仓库信息（只读操作）
 */
public class GitHubApiClient {

    private static final String GITHUB_API_BASE = "https://api.github.com";

    /**
     * 执行 GitHub API GET 请求
     */
    public static String executeGet(String endpoint, String token) throws IOException {
        String url = GITHUB_API_BASE + endpoint;
        System.out.println("[GitHub API] Request: " + url);

        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();

        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);

            // 设置请求头
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("[GitHub API] Response Code: " + responseCode);

            if (responseCode == 401) {
                throw new IOException("GitHub authentication failed. Please check your token.");
            } else if (responseCode == 403) {
                throw new IOException("GitHub API rate limit exceeded or access forbidden.");
            } else if (responseCode == 404) {
                throw new IOException("GitHub resource not found.");
            } else if (responseCode != 200) {
                throw new IOException("GitHub API error: " + responseCode);
            }

            // 读取响应
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * 获取仓库信息
     */
    public static String getRepository(String owner, String repo, String token) throws IOException {
        return executeGet("/repos/" + owner + "/" + repo, token);
    }

    /**
     * 获取仓库的 Issues
     */
    public static String getIssues(String owner, String repo, String state, String token) throws IOException {
        String endpoint = "/repos/" + owner + "/" + repo + "/issues";
        if (state != null && !state.isEmpty()) {
            endpoint += "?state=" + state;
        }
        return executeGet(endpoint, token);
    }

    /**
     * 获取仓库的 Pull Requests
     */
    public static String getPullRequests(String owner, String repo, String state, String token) throws IOException {
        String endpoint = "/repos/" + owner + "/" + repo + "/pulls";
        if (state != null && !state.isEmpty()) {
            endpoint += "?state=" + state;
        }
        return executeGet(endpoint, token);
    }

    /**
     * 获取仓库的提交记录（支持指定分支）
     */
    public static String getCommits(String owner, String repo, String branch, String token) throws IOException {
        String endpoint = "/repos/" + owner + "/" + repo + "/commits?per_page=10";
        if (branch != null && !branch.isEmpty()) {
            endpoint += "&sha=" + branch;
        }
        return executeGet(endpoint, token);
    }

    /**
     * 获取仓库的提交记录（使用默认分支）
     */
    public static String getCommits(String owner, String repo, String token) throws IOException {
        return getCommits(owner, repo, null, token);
    }

    /**
     * 获取仓库的分支
     */
    public static String getBranches(String owner, String repo, String token) throws IOException {
        return executeGet("/repos/" + owner + "/" + repo + "/branches", token);
    }

    /**
     * 获取用户信息
     */
    public static String getUser(String username, String token) throws IOException {
        return executeGet("/users/" + username, token);
    }

    /**
     * 搜索仓库
     */
    public static String searchRepositories(String query, String token) throws IOException {
        return executeGet("/search/repositories?q=" + query + "&per_page=10", token);
    }

    /**
     * 搜索 Issues
     */
    public static String searchIssues(String query, String token) throws IOException {
        return executeGet("/search/issues?q=" + query + "&per_page=10", token);
    }

    /**
     * 搜索代码/文件
     */
    public static String searchCode(String query, String token) throws IOException {
        return executeGet("/search/code?q=" + query + "&per_page=10", token);
    }

    /**
     * 获取文件的提交历史
     */
    public static String getFileCommits(String owner, String repo, String filepath, String branch, String token) throws IOException {
        String endpoint = "/repos/" + owner + "/" + repo + "/commits?path=" + filepath + "&per_page=10";
        if (branch != null && !branch.isEmpty()) {
            endpoint += "&sha=" + branch;
        }
        return executeGet(endpoint, token);
    }

    /**
     * 获取单个 commit 的详细信息（包括 diff）
     */
    public static String getSingleCommit(String owner, String repo, String sha, String token) throws IOException {
        return executeGet("/repos/" + owner + "/" + repo + "/commits/" + sha, token);
    }

    /**
     * 获取仓库的 Releases
     */
    public static String getReleases(String owner, String repo, String token) throws IOException {
        return executeGet("/repos/" + owner + "/" + repo + "/releases", token);
    }

    /**
     * 获取仓库的目录内容
     * @param owner 仓库所有者
     * @param repo 仓库名称
     * @param path 路径（空字符串或 null 表示根目录）
     * @param token GitHub token
     * @return 目录内容的 JSON 数组
     */
    public static String getContents(String owner, String repo, String path, String token) throws IOException {
        String endpoint = "/repos/" + owner + "/" + repo + "/contents";
        if (path != null && !path.isEmpty()) {
            endpoint += "/" + path;
        }
        return executeGet(endpoint, token);
    }
}
