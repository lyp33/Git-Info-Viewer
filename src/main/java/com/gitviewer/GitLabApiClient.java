package com.gitviewer;

import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitLab API客户端工具类
 * 用于调用GitLab API获取Group项目列表
 */
public class GitLabApiClient {

    /**
     * 判断URL是否为Group URL
     * Group URL不以.git结尾
     *
     * @param url 待判断的URL
     * @return true if URL does NOT end with .git
     */
    public static boolean isGroupUrl(String url) {
        return url != null && !url.trim().isEmpty() && !url.trim().endsWith(".git");
    }

    /**
     * 从Group URL提取Group路径
     * 例如：https://gitlab.insuremo.com/thailife/thailife_sdk -> thailife%2fthailife_sdk
     * 例如：https://gitlab.insuremo.com/stbd -> stbd
     *
     * @param groupUrl Group URL
     * @return group path (URL encoded)
     */
    public static String extractGroupPath(String groupUrl) {
        String url = groupUrl.trim();
        
        // 移除协议部分 (http:// 或 https://)
        if (url.startsWith("http://")) {
            url = url.substring(7);
        } else if (url.startsWith("https://")) {
            url = url.substring(8);
        }
        
        // 移除末尾的斜杠
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        
        // 找到第一个 / 的位置（域名后的第一个斜杠）
        int firstSlash = url.indexOf('/');
        if (firstSlash >= 0) {
            // 获取第一个 / 后面的所有内容
            String groupPath = url.substring(firstSlash + 1);
            // 将 group path 中的 / 替换为 %2f (URL编码)
            return groupPath.replace("/", "%2f");
        }
        
        return url;
    }

    /**
     * 从Group URL提取基础URL
     * 例如：https://gitlab.insuremo.com/thailife/thailife_sdk -> https://gitlab.insuremo.com
     * 例如：https://gitlab.insuremo.com/stbd -> https://gitlab.insuremo.com
     *
     * @param groupUrl Group URL
     * @return base URL
     */
    public static String extractBaseUrl(String groupUrl) {
        String url = groupUrl.trim();
        
        // 保存协议部分
        String protocol = "";
        if (url.startsWith("http://")) {
            protocol = "http://";
            url = url.substring(7);
        } else if (url.startsWith("https://")) {
            protocol = "https://";
            url = url.substring(8);
        }
        
        // 移除末尾的斜杠
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        
        // 找到第一个斜杠的位置（域名后的第一个斜杠）
        int firstSlash = url.indexOf('/');
        if (firstSlash >= 0) {
            // 返回协议 + 域名部分
            return protocol + url.substring(0, firstSlash);
        }
        
        return protocol + url;
    }

    /**
     * 构建GitLab API URL
     * 例如：https://gitlab.insuremo.com + stbd -> https://gitlab.insuremo.com/api/v4/groups/stbd/projects
     *
     * @param baseUrl  基础URL
     * @param groupPath Group路径
     * @return API URL
     */
    public static String buildApiUrl(String baseUrl, String groupPath) {
        return baseUrl + "/api/v4/groups/" + groupPath + "/projects?per_page=100";
    }

    /**
     * 获取Group下的所有项目
     *
     * @param groupUrl Group URL (e.g., https://gitlab.insuremo.com/stbd)
     * @param credentialsProvider 认证信息
     * @return 项目列表
     * @throws IOException 网络或API错误
     */
    public static List<GitLabProject> fetchGroupProjects(
            String groupUrl,
            CredentialsProvider credentialsProvider
    ) throws IOException {

        String baseUrl = extractBaseUrl(groupUrl);
        String groupPath = extractGroupPath(groupUrl);
        String apiUrl = buildApiUrl(baseUrl, groupPath);

        // 获取用户名和密码
        String username = "";
        String password = "";
        if (credentialsProvider instanceof UsernamePasswordCredentialsProvider) {
            // 注意：UsernamePasswordCredentialsProvider没有公开的getUsername()方法
            // 我们需要从其他地方获取或使用反射，这里简化处理
            // 实际使用时，可能需要调整认证信息的获取方式
            password = getPasswordFromCredentialsProvider(credentialsProvider);
            // 对于GitLab，username可以使用token或者任意值，主要看password（token）
            username = "gitlab-user"; // 占位符，实际GitLab可能需要用户名或token
        }

        String jsonResponse = executeHttpCall(apiUrl, username, password);
        return parseProjectsJson(jsonResponse);
    }

    /**
     * 从CredentialsProvider提取密码
     * 由于CredentialsProvider接口限制，这里使用简化方式
     */
    private static String getPasswordFromCredentialsProvider(CredentialsProvider cp) {
        // 这是一个简化的实现
        // 实际使用中，可能需要通过其他方式传递认证信息
        // 或者修改接口以直接传递用户名和密码
        return ""; // 占位符，需要从外部传入
    }

    /**
     * 执行HTTP GET请求
     *
     * @param apiUrl  API URL
     * @param username 用户名
     * @param password 密码/Token
     * @return 响应JSON字符串
     * @throws IOException 网络错误
     */
    public static String executeHttpCall(
            String apiUrl,
            String username,
            String password
    ) throws IOException {
        System.out.println("[GitLab API] Request URL: " + apiUrl);
        System.out.println("[GitLab API] Username: " + (username != null && !username.isEmpty() ? username : "(none)"));
        System.out.println("[GitLab API] Password: " + (password != null && !password.isEmpty() ? "***" : "(none)"));

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();

        try {
            // 设置请求方法和超时
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);  // 15秒
            conn.setReadTimeout(30000);     // 30秒

            // HTTP Basic认证
            if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                String auth = username + ":" + password;
                String encodedAuth = Base64.getEncoder()
                        .encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
                System.out.println("[GitLab API] Authorization header set: Basic " + encodedAuth.substring(0, 10) + "...");
            }

            System.out.println("[GitLab API] Connecting...");

            // 检查响应码
            int responseCode = conn.getResponseCode();
            System.out.println("[GitLab API] Response Code: " + responseCode);

            // 打印响应头
            System.out.println("[GitLab API] Response Headers:");
            conn.getHeaderFields().forEach((key, value) -> {
                System.out.println("  " + key + ": " + value);
            });

            if (responseCode == 401) {
                String errorResponse = readErrorStream(conn);
                System.out.println("[GitLab API] Error Response: " + errorResponse);
                throw new IOException("Authentication failed (401). Please check your credentials. Details: " + errorResponse);
            } else if (responseCode == 403) {
                String errorResponse = readErrorStream(conn);
                System.out.println("[GitLab API] Error Response: " + errorResponse);
                throw new IOException("Access denied (403). You may not have permission to access this group. Details: " + errorResponse);
            } else if (responseCode == 404) {
                String errorResponse = readErrorStream(conn);
                System.out.println("[GitLab API] Error Response: " + errorResponse);
                throw new IOException("Group not found (404). Please check the group URL. Details: " + errorResponse);
            } else if (responseCode != 200) {
                String errorResponse = readErrorStream(conn);
                System.out.println("[GitLab API] Error Response: " + errorResponse);
                throw new IOException("HTTP error: " + responseCode + ". Details: " + errorResponse);
            }

            // 读取响应
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {

                StringBuilder response = new StringBuilder();
                String line;
                int lineCount = 0;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                    lineCount++;
                    // 只打印前5行，避免日志过长
                    if (lineCount <= 5) {
                        System.out.println("[GitLab API] Response Line " + lineCount + ": " + line);
                    }
                }
                System.out.println("[GitLab API] Total response lines: " + lineCount);
                System.out.println("[GitLab API] Response length: " + response.length() + " characters");

                String jsonResponse = response.toString();
                // 打印JSON的摘要信息
                if (jsonResponse.startsWith("[")) {
                    System.out.println("[GitLab API] Response format: JSON Array");
                } else if (jsonResponse.startsWith("{")) {
                    System.out.println("[GitLab API] Response format: JSON Object");
                } else {
                    System.out.println("[GitLab API] Response format: Unknown");
                }

                return jsonResponse;
            }
        } finally {
            conn.disconnect();
            System.out.println("[GitLab API] Connection closed");
        }
    }

    /**
     * 读取错误流内容
     */
    private static String readErrorStream(HttpURLConnection conn) {
        try {
            java.io.InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    return errorResponse.toString();
                }
            }
        } catch (IOException e) {
            // Ignore
        }
        return "(no error details)";
    }

    /**
     * 解析GitLab API响应的JSON数组
     * 新的API返回格式示例:
     * [
     *   {
     *     "id": 2194,
     *     "name": "bs-initialdb",
     *     "path": "bs-initialdb",
     *     "path_with_namespace": "stbd/bs-initialdb",
     *     "http_url_to_repo": "https://gitlab.insuremo.com/stbd/bs-initialdb.git"
     *   }
     * ]
     * 使用简单的字符串处理，不依赖第三方JSON库
     *
     * @param json JSON字符串
     * @return 项目列表
     */
    public static List<GitLabProject> parseProjectsJson(String json) {
        System.out.println("[GitLab API] Starting JSON parsing...");
        List<GitLabProject> projects = new ArrayList<>();

        if (json == null || json.trim().isEmpty()) {
            System.out.println("[GitLab API] JSON is null or empty");
            return projects;
        }

        String content = json.trim();
        System.out.println("[GitLab API] JSON content length: " + content.length());

        // 检查是否为数组格式
        if (!content.startsWith("[") || !content.endsWith("]")) {
            System.out.println("[GitLab API] ERROR: JSON is not an array format");
            System.out.println("[GitLab API] JSON starts with: " + content.substring(0, Math.min(50, content.length())));
            return projects;
        }

        System.out.println("[GitLab API] JSON is an array, parsing objects...");

        // 移除开头和结尾的[]
        content = content.substring(1, content.length() - 1);

        // 分割每个项目对象
        List<String> projectObjects = splitJsonObjects(content);
        System.out.println("[GitLab API] Found " + projectObjects.size() + " objects in array");

        int projectCount = 0;
        int errorCount = 0;

        for (int i = 0; i < projectObjects.size(); i++) {
            String obj = projectObjects.get(i);
            if (obj.trim().isEmpty()) {
                continue;
            }

            try {
                GitLabProject project = new GitLabProject();
                project.id = extractJsonValue(obj, "id");
                project.name = extractJsonValue(obj, "name");
                project.path = extractJsonValue(obj, "path");
                project.pathWithNamespace = extractJsonValue(obj, "path_with_namespace");
                project.httpUrlToRepo = extractJsonValue(obj, "http_url_to_repo");

                System.out.println("[GitLab API] Found project: " + project.name + " (id: " + project.id + ")");
                System.out.println("[GitLab API]   path: " + project.path);
                System.out.println("[GitLab API]   path_with_namespace: " + project.pathWithNamespace);
                System.out.println("[GitLab API]   http_url_to_repo: " + project.httpUrlToRepo);

                project.selected = false;

                // 验证必要字段
                if (project.name != null && !project.name.isEmpty()) {
                    projects.add(project);
                    projectCount++;
                    System.out.println("[GitLab API] Added project to list: " + project.name);
                } else {
                    System.out.println("[GitLab API] WARNING: Project name is empty, skipping");
                }
            } catch (Exception e) {
                // 解析失败时跳过该项目
                errorCount++;
                System.err.println("[GitLab API] Failed to parse object " + (i + 1) + ": " + obj);
                e.printStackTrace();
            }
        }

        System.out.println("[GitLab API] Parsing complete. Projects: " + projectCount +
                ", Errors: " + errorCount);
        return projects;
    }

    /**
     * 分割JSON对象数组
     * 处理嵌套的大括号，正确分割顶层对象
     *
     * @param content JSON数组内容（不包含外层的[]）
     * @return JSON对象字符串列表
     */
    private static List<String> splitJsonObjects(String content) {
        List<String> objects = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int braceCount = 0;
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                current.append(c);
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                current.append(c);
                continue;
            }

            if (!inString) {
                if (c == '{') {
                    braceCount++;
                    current.append(c);
                } else if (c == '}') {
                    braceCount--;
                    current.append(c);
                    if (braceCount == 0) {
                        objects.add(current.toString());
                        current = new StringBuilder();
                        // 跳过后续的逗号和空格
                        while (i + 1 < content.length()) {
                            char next = content.charAt(i + 1);
                            if (next == ',' || next == ' ' || next == '\n' || next == '\t' || next == '\r') {
                                i++;
                            } else {
                                break;
                            }
                        }
                    }
                } else {
                    if (braceCount > 0) {
                        current.append(c);
                    }
                }
            } else {
                current.append(c);
            }
        }

        // 如果还有剩余内容（处理边界情况）
        if (current.length() > 0) {
            objects.add(current.toString());
        }

        return objects;
    }

    /**
     * 从JSON对象中提取字段值
     * 支持字符串和数字类型
     *
     * @param obj JSON对象字符串
     * @param key 字段名
     * @return 字段值
     */
    private static String extractJsonValue(String obj, String key) {
        // 使用正则表达式匹配 "key":value
        String pattern = "\"" + Pattern.quote(key) + "\"\\s*:\\s*";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(obj);

        if (m.find()) {
            int start = m.end();

            // 跳过空格
            while (start < obj.length() && Character.isWhitespace(obj.charAt(start))) {
                start++;
            }

            if (start >= obj.length()) {
                return "";
            }

            // 检查是否为字符串值（以引号开头）
            if (obj.charAt(start) == '"') {
                // 找到结束的引号（考虑转义）
                int end = start + 1;
                boolean escaped = false;
                while (end < obj.length()) {
                    char c = obj.charAt(end);
                    if (escaped) {
                        escaped = false;
                    } else if (c == '\\') {
                        escaped = true;
                    } else if (c == '"') {
                        break;
                    }
                    end++;
                }
                if (end < obj.length()) {
                    return obj.substring(start + 1, end);
                }
            } else if (obj.charAt(start) == '[' || obj.charAt(start) == '{') {
                // 对象或数组，跳过
                return "";
            } else {
                // 数字值或布尔值，找到下一个逗号或结束符
                int end = start;
                while (end < obj.length()) {
                    char c = obj.charAt(end);
                    if (c == ',' || c == '}') {
                        break;
                    }
                    end++;
                }
                return obj.substring(start, end).trim();
            }
        }

        return "";
    }

    /**
     * 便捷方法：使用用户名和密码获取Group项目
     *
     * @param groupUrl Group URL
     * @param username 用户名
     * @param password 密码
     * @return 项目列表
     * @throws IOException 网络或API错误
     */
    public static List<GitLabProject> fetchGroupProjects(
            String groupUrl,
            String username,
            String password
    ) throws IOException {
        String baseUrl = extractBaseUrl(groupUrl);
        String groupPath = extractGroupPath(groupUrl);
        String apiUrl = buildApiUrl(baseUrl, groupPath);

        String jsonResponse = executeHttpCall(apiUrl, username, password);
        return parseProjectsJson(jsonResponse);
    }

    /**
     * 智能获取Group项目（优先使用配置的认证信息）
     * 认证优先级：
     * 1. GitLab Private Token（必须配置）
     * 2. 如果没有Token，抛出异常提示用户配置
     *
     * @param groupUrl Group URL
     * @param parentFrame 父窗口，用于弹窗提示（可为null）
     * @return 项目列表
     * @throws IOException 网络或API错误
     */
    public static List<GitLabProject> fetchGroupProjectsWithAuth(
            String groupUrl,
            java.awt.Frame parentFrame
    ) throws IOException {
        String baseUrl = extractBaseUrl(groupUrl);
        String groupPath = extractGroupPath(groupUrl);
        String apiUrl = buildApiUrl(baseUrl, groupPath);

        AppSettings settings = AppSettings.getInstance();

        // 1. 检查是否配置了 Private Token
        String token = settings.getGitLabPrivateToken();
        if (token.isEmpty()) {
            String errorMsg = "GitLab Private Token is not configured.\n\n" +
                    "Please configure your GitLab Private Token first:\n" +
                    "1. Go to 'File' -> 'GitLab Authentication...' menu\n" +
                    "2. Enter your GitLab Private Token\n" +
                    "3. Click OK to save\n\n" +
                    "You can generate a Private Token in GitLab:\n" +
                    "GitLab -> Settings -> Access Tokens";

            System.out.println("[GitLab API] ERROR: Private Token not configured");

            // 如果有父窗口，显示对话框提示
            if (parentFrame != null) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parentFrame,
                            errorMsg,
                            "GitLab Authentication Required",
                            JOptionPane.WARNING_MESSAGE);
                });
            }

            throw new IOException("GitLab Private Token is not configured. Please configure it first.");
        }

        // 2. 使用 Private Token 进行认证
        System.out.println("[GitLab API] Using configured Private Token...");
        try {
            String jsonResponse = executeHttpCallWithToken(apiUrl, token);
            System.out.println("[GitLab API] Authentication with Private Token succeeded");
            return parseProjectsJson(jsonResponse);
        } catch (IOException e) {
            System.out.println("[GitLab API] Private Token authentication failed: " + e.getMessage());

            // Token认证失败，给出更详细的错误提示
            String errorDetail = "Failed to authenticate with GitLab using your Private Token.\n\n" +
                    "Possible reasons:\n" +
                    "1. The token is invalid or has expired\n" +
                    "2. The token doesn't have permission to access this group\n" +
                    "3. The group URL is incorrect\n\n" +
                    "Error details: " + e.getMessage();

            if (parentFrame != null) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(parentFrame,
                            errorDetail,
                            "GitLab Authentication Failed",
                            JOptionPane.ERROR_MESSAGE);
                });
            }

            throw new IOException("Authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * 使用 Private Token 执行 HTTP GET 请求
     *
     * @param apiUrl API URL
     * @param token GitLab Private Token
     * @return 响应JSON字符串
     * @throws IOException 网络错误
     */
    public static String executeHttpCallWithToken(
            String apiUrl,
            String token
    ) throws IOException {
        System.out.println("[GitLab API] Request URL: " + apiUrl);
        System.out.println("[GitLab API] Using Private Token authentication");

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();

        try {
            // 设置请求方法和超时
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);  // 15秒
            conn.setReadTimeout(30000);     // 30秒

            // GitLab Private Token 认证（使用 PRIVATE-TOKEN header）
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("PRIVATE-TOKEN", token);
                System.out.println("[GitLab API] PRIVATE-TOKEN header set");
            }

            System.out.println("[GitLab API] Connecting...");

            // 检查响应码
            int responseCode = conn.getResponseCode();
            System.out.println("[GitLab API] Response Code: " + responseCode);

            if (responseCode == 401) {
                String errorResponse = readErrorStream(conn);
                System.out.println("[GitLab API] Error Response: " + errorResponse);
                throw new IOException("Authentication failed (401). Invalid or expired token. Details: " + errorResponse);
            } else if (responseCode == 403) {
                String errorResponse = readErrorStream(conn);
                System.out.println("[GitLab API] Error Response: " + errorResponse);
                throw new IOException("Access denied (403). You may not have permission to access this group. Details: " + errorResponse);
            } else if (responseCode == 404) {
                String errorResponse = readErrorStream(conn);
                System.out.println("[GitLab API] Error Response: " + errorResponse);
                throw new IOException("Group not found (404). Please check the group URL. Details: " + errorResponse);
            } else if (responseCode != 200) {
                String errorResponse = readErrorStream(conn);
                System.out.println("[GitLab API] Error Response: " + errorResponse);
                throw new IOException("HTTP error: " + responseCode + ". Details: " + errorResponse);
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
            System.out.println("[GitLab API] Connection closed");
        }
    }
}
