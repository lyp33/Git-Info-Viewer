package com.gitviewer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Portal API客户端
 * REST API client for Portal integration
 */
public class PortalApiClient {
    private static final Logger logger = LoggerFactory.getLogger(PortalApiClient.class);
    
    private static final String BASE_URL = "https://portal.insuremo.com";
    private static final int CONNECT_TIMEOUT = 10000;  // 10 seconds
    private static final int READ_TIMEOUT = 30000;     // 30 seconds
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY = 1000;       // 1 second
    
    public PortalApiClient() {
        logger.info("PortalApiClient initialized with BASE_URL: {}", BASE_URL);
    }
    
    /**
     * 获取认证Token
     * Get authentication token from Portal
     * 
     * @param username Portal用户名
     * @param password Portal密码
     * @param tenantCode 租户代码
     * @return TokenResponse对象
     * @throws IOException 网络错误或API错误
     */
    public TokenResponse getToken(String username, String password, String tenantCode) throws IOException {
        logger.info("=== Getting Token ===");
        logger.info("Username: {}, TenantCode: {}", username, tenantCode);
        
        String url = BASE_URL + "/cas/get-token";
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("x-mo-user-source-id", "platform");
        headers.put("x-mo-tenant-id", tenantCode);
        headers.put("x-mo-client-id", "key");
        
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("username", username);
        requestBody.put("password", password);
        
        String jsonBody = requestBody.toString();
        
        // 发送请求
        String response = sendPostRequest(url, headers, jsonBody);
        
        // 解析响应
        return parseTokenResponse(response);
    }
    
    /**
     * 获取应用列表
     * Get application list from Portal
     * 
     * @param tenantCode 租户代码
     * @param token 认证Token
     * @return Application列表
     * @throws IOException 网络错误或API错误
     */
    public List<Application> getApplicationList(String tenantCode, String token) throws IOException {
        logger.info("=== Getting Application List ===");
        logger.info("TenantCode: {}", tenantCode);
        
        String url = BASE_URL + "/api/mo-fo/1.0/ops/app";
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-tenant", tenantCode);
        headers.put("authorization", "Bearer " + token);
        headers.put("Accept", "application/json");
        
        // 发送请求
        String response = sendGetRequest(url, headers);
        
        // 解析响应
        return parseApplicationList(response);
    }
    
    /**
     * 获取Plan名称列表
     * Get plan names from Portal
     * 
     * @param tenantCode 租户代码
     * @param token 认证Token
     * @return Plan名称列表
     * @throws IOException 网络错误或API错误
     */
    public List<String> getPlanNames(String tenantCode, String token) throws IOException {
        String logMsg = "=== Getting Plan Names ===";
        logger.info(logMsg);
        System.out.println(logMsg);
        
        logMsg = "TenantCode: " + tenantCode;
        logger.info(logMsg);
        System.out.println(logMsg);
        
        String url = BASE_URL + "/api/mo-fo/1.0/ops/multi_build/title_list";
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-tenant", tenantCode);
        headers.put("authorization", "Bearer " + token);
        headers.put("Accept", "application/json");
        
        // 发送请求
        String response = sendGetRequest(url, headers);
        
        // 解析响应
        return parsePlanNames(response);
    }
    
    /**
     * 根据Plan获取构建结果
     * Get build result by plan from Portal
     * 
     * @param tenantCode 租户代码
     * @param token 认证Token
     * @param planTitle 完整的Plan标题
     * @return PlanBuildResult对象
     * @throws IOException 网络错误或API错误
     */
    public PlanBuildResult getBuildResultByPlan(String tenantCode, String token, String planTitle) throws IOException {
        String logMsg = "=== Getting Build Result by Plan ===";
        logger.info(logMsg);
        System.out.println(logMsg);
        
        logMsg = "TenantCode: " + tenantCode + ", PlanTitle: " + planTitle;
        logger.info(logMsg);
        System.out.println(logMsg);
        
        String url = BASE_URL + "/api/mo-fo/1.0/ops/multi_build?package_title=" + 
                     java.net.URLEncoder.encode(planTitle, StandardCharsets.UTF_8);
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-tenant", tenantCode);
        headers.put("authorization", "Bearer " + token);
        headers.put("Accept", "application/json");
        
        // 发送请求
        String response = sendGetRequest(url, headers);
        
        // 解析响应
        return parsePlanBuildResult(response);
    }
    
    /**
     * 根据应用获取构建结果
     * Get build result by application from Portal
     * 
     * @param tenantCode 租户代码
     * @param token 认证Token
     * @param appName 应用名称（可为null）
     * @param creator 创建者（可为null）
     * @param pageNumber 页码
     * @param pageSize 每页大小
     * @return AppBuildResult对象
     * @throws IOException 网络错误或API错误
     */
    public AppBuildResult getBuildResultByApp(String tenantCode, String token, String appName, 
                                              String creator, int pageNumber, int pageSize) throws IOException {
        logger.info("=== Getting Build Result by App ===");
        logger.info("TenantCode: {}, AppName: {}, Creator: {}, PageNumber: {}, PageSize: {}", 
                   tenantCode, appName, creator, pageNumber, pageSize);
        
        // 构建URL和查询参数
        StringBuilder urlBuilder = new StringBuilder(BASE_URL + "/api/mo-fo/1.0/ops/build?");
        urlBuilder.append("page_number=").append(pageNumber);
        urlBuilder.append("&page_size=").append(pageSize);
        
        if (appName != null && !appName.trim().isEmpty()) {
            urlBuilder.append("&app_name=").append(java.net.URLEncoder.encode(appName, StandardCharsets.UTF_8));
        }
        
        if (creator != null && !creator.trim().isEmpty()) {
            urlBuilder.append("&creator=").append(java.net.URLEncoder.encode(creator, StandardCharsets.UTF_8));
        }
        
        String url = urlBuilder.toString();
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-tenant", tenantCode);
        headers.put("authorization", "Bearer " + token);
        headers.put("Accept", "application/json");
        
        // 发送请求
        String response = sendGetRequest(url, headers);
        
        // 解析响应
        return parseAppBuildResult(response);
    }

    /**
     * 根据ID查询单个构建记录
     * Query single build record by ID
     * 
     * @param tenantCode 租户代码
     * @param token 认证Token
     * @param buildId 构建记录ID
     * @return 构建输出内容
     * @throws IOException 网络错误或API错误
     */
    public String getBuildOutputById(String tenantCode, String token, String buildId) throws IOException {
        return getBuildOutputById(tenantCode, token, buildId, false);
    }
    
    /**
     * 根据ID查询单个构建记录（支持指定API类型）
     * Query single build record by ID (with API type selection)
     * 
     * @param tenantCode 租户代码
     * @param token 认证Token
     * @param buildId 构建记录ID
     * @param useBuildStart 是否使用 check_status API（true表示Build Start状态）
     * @return 构建输出内容
     * @throws IOException 网络错误或API错误
     */
    public String getBuildOutputById(String tenantCode, String token, String buildId, boolean useBuildStart) throws IOException {
        logger.info("=== Getting Build Output by ID ===");
        logger.info("TenantCode: {}, BuildId: {}, UseBuildStart: {}", tenantCode, buildId, useBuildStart);
        
        // 根据 useBuildStart 标志选择不同的 API
        String url;
        if (useBuildStart) {
            // Build Start 状态使用 check_status API
            url = "https://portal.insuremo.com/api/mo-fo/1.0/ops/build/history/check_status?id=" + 
                  java.net.URLEncoder.encode(buildId, StandardCharsets.UTF_8);
        } else {
            // 其他状态使用 query_one API（注意：使用 portal-gw.insuremo.com）
            url = "https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=" + 
                  java.net.URLEncoder.encode(buildId, StandardCharsets.UTF_8);
        }
        
        logger.info("Full URL: {}", url);
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-tenant", tenantCode);
        headers.put("authorization", "Bearer " + token);
        headers.put("Accept", "application/json");
        
        logger.info("Request headers: x-mo-target-tenant={}, authorization=Bearer {}...", 
                   tenantCode, token != null && token.length() > 8 ? 
                   token.substring(0, 4) + "..." + token.substring(token.length() - 4) : "[INVALID]");
        
        // 发送请求
        String response = sendGetRequest(url, headers);
        
        logger.info("Response received, length: {}", response != null ? response.length() : 0);
        logger.info("Response preview (first 500 chars): {}", 
                   response != null && response.length() > 500 ? 
                   response.substring(0, 500) + "..." : response);
        
        // 解析响应，提取 callback.build_output
        return parseBuildOutput(response);
    }

    /**
     * 发送POST请求
     * Send POST request
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @param jsonBody 请求体（JSON格式）
     * @return 响应内容
     * @throws IOException 网络错误或API错误
     */
    private String sendPostRequest(String url, Map<String, String> headers, String jsonBody) throws IOException {
        logRequest("POST", url, headers, jsonBody);
        
        HttpURLConnection conn = null;
        try {
            URL urlObj = new URL(url);
            conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("POST");
            configureConnection(conn);
            
            // 设置请求头
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            
            // 发送请求体
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // 读取响应
            int statusCode = conn.getResponseCode();
            String responseBody = readResponse(conn, statusCode);
            
            logResponse(statusCode, responseBody);
            
            // 检查HTTP状态码
            if (statusCode >= 400) {
                throw new IOException("HTTP " + statusCode + ": " + responseBody);
            }
            
            return responseBody;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * 发送GET请求
     * Send GET request
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @return 响应内容
     * @throws IOException 网络错误或API错误
     */
    private String sendGetRequest(String url, Map<String, String> headers) throws IOException {
        logRequest("GET", url, headers, null);
        
        HttpURLConnection conn = null;
        try {
            URL urlObj = new URL(url);
            conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            configureConnection(conn);
            
            // 设置请求头
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
            
            // 读取响应
            int statusCode = conn.getResponseCode();
            String responseBody = readResponse(conn, statusCode);
            
            logResponse(statusCode, responseBody);
            
            // 检查HTTP状态码
            if (statusCode >= 400) {
                throw new IOException("HTTP " + statusCode + ": " + responseBody);
            }
            
            return responseBody;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    /**
     * 带重试的请求发送（仅用于网络错误）
     * Send request with retry (for network errors only)
     * 
     * @param url 请求URL
     * @param method 请求方法（GET或POST）
     * @param headers 请求头
     * @param body 请求体（POST时使用）
     * @return 响应内容
     * @throws IOException 网络错误或API错误
     */
    private String sendRequestWithRetry(String url, String method, Map<String, String> headers, String body) throws IOException {
        IOException lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (method.equals("POST")) {
                    return sendPostRequest(url, headers, body);
                } else {
                    return sendGetRequest(url, headers);
                }
            } catch (IOException e) {
                lastException = e;
                logger.warn("Request attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Request interrupted", ie);
                    }
                }
            }
        }
        
        throw new IOException("Request failed after " + MAX_RETRIES + " attempts", lastException);
    }
    
    /**
     * 配置HTTP连接
     * Configure HTTP connection
     * 
     * @param conn HTTP连接对象
     */
    private void configureConnection(HttpURLConnection conn) {
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Charset", "UTF-8");
    }
    
    /**
     * 读取HTTP响应
     * Read HTTP response
     * 
     * @param conn HTTP连接对象
     * @param statusCode HTTP状态码
     * @return 响应内容
     * @throws IOException 读取错误
     */
    private String readResponse(HttpURLConnection conn, int statusCode) throws IOException {
        BufferedReader reader = null;
        try {
            if (statusCode >= 400) {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            
            return response.toString();
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
    
    /**
     * 记录请求日志
     * Log request
     * 
     * @param method 请求方法
     * @param url 请求URL
     * @param headers 请求头
     * @param body 请求体
     */
    private void logRequest(String method, String url, Map<String, String> headers, String body) {
        String logMsg = "=== " + method + " Request ===";
        logger.info(logMsg);
        System.out.println(logMsg);
        
        logMsg = "URL: " + url;
        logger.info(logMsg);
        System.out.println(logMsg);
        
        // 记录请求头，屏蔽敏感值
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (key.equalsIgnoreCase("authorization")) {
                value = maskToken(value);
            }
            
            logMsg = "Header: " + key + " = " + value;
            logger.info(logMsg);
            System.out.println(logMsg);
        }
        
        // 不记录包含密码的请求体
        if (body != null && !body.contains("password")) {
            logMsg = "Body: " + body;
            logger.info(logMsg);
            System.out.println(logMsg);
        } else if (body != null) {
            logMsg = "Body: [REDACTED - contains sensitive data]";
            logger.info(logMsg);
            System.out.println(logMsg);
        }
    }
    
    /**
     * 记录响应日志
     * Log response
     * 
     * @param statusCode HTTP状态码
     * @param responseBody 响应体
     */
    private void logResponse(int statusCode, String responseBody) {
        if (statusCode >= 400) {
            String logMsg = "=== Response (Error) ===";
            logger.error(logMsg);
            System.err.println(logMsg);
            
            logMsg = "Status Code: " + statusCode;
            logger.error(logMsg);
            System.err.println(logMsg);
            
            logMsg = "Body: " + responseBody;
            logger.error(logMsg);
            System.err.println(logMsg);
        } else {
            String logMsg = "=== Response (Success) ===";
            logger.info(logMsg);
            System.out.println(logMsg);
            
            logMsg = "Status Code: " + statusCode;
            logger.info(logMsg);
            System.out.println(logMsg);
            
            // 只记录响应体的摘要（前500个字符）
            if (responseBody.length() > 500) {
                logMsg = "Body: " + responseBody.substring(0, 500) + "... (truncated, total length: " + responseBody.length() + ")";
                logger.info(logMsg);
                System.out.println(logMsg);
            } else {
                logMsg = "Body: " + responseBody;
                logger.info(logMsg);
                System.out.println(logMsg);
            }
        }
    }
    
    /**
     * 屏蔽Token（只显示前4位和后4位）
     * Mask token (show only first and last 4 characters)
     * 
     * @param authHeader Authorization头的值
     * @return 屏蔽后的值
     */
    private String maskToken(String authHeader) {
        if (authHeader == null || authHeader.length() < 20) {
            return "[MASKED]";
        }
        
        String[] parts = authHeader.split(" ");
        if (parts.length == 2) {
            String token = parts[1];
            if (token.length() > 8) {
                return parts[0] + " " + token.substring(0, 4) + "..." + token.substring(token.length() - 4);
            }
        }
        
        return "[MASKED]";
    }

    /**
     * 解析Token响应
     * Parse token response
     * 
     * @param response JSON响应字符串
     * @return TokenResponse对象
     */
    private TokenResponse parseTokenResponse(String response) {
        logger.debug("Parsing token response");
        
        TokenResponse tokenResponse = new TokenResponse();
        
        try {
            JSONObject json = new JSONObject(response);
            
            tokenResponse.setAccessToken(json.optString("access_token", ""));
            tokenResponse.setExpireIn(json.optLong("expire_in", 0));
            tokenResponse.setMessage(json.optString("message", ""));
            tokenResponse.setErrCode(json.optString("err_code", ""));
            tokenResponse.setAuthResult(json.optBoolean("authResult", false));
            
            logger.info("Token response parsed: authResult={}, expireIn={}", 
                       tokenResponse.isAuthResult(), tokenResponse.getExpireIn());
        } catch (Exception e) {
            logger.error("Failed to parse token response", e);
            throw new RuntimeException("Failed to parse token response: " + e.getMessage(), e);
        }
        
        return tokenResponse;
    }
    
    /**
     * 解析应用列表响应
     * Parse application list response
     * 
     * @param response JSON响应字符串
     * @return Application列表
     */
    private List<Application> parseApplicationList(String response) {
        logger.debug("Parsing application list response");
        
        List<Application> applications = new ArrayList<>();
        
        try {
            JSONArray jsonArray = new JSONArray(response);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject appJson = jsonArray.getJSONObject(i);
                
                Application app = new Application();
                app.setId(appJson.optString("id", ""));
                app.setAppName(appJson.optString("app_name", ""));
                app.setUserName(appJson.optString("user_name", ""));
                
                applications.add(app);
            }
            
            logger.info("Parsed {} applications", applications.size());
        } catch (Exception e) {
            logger.error("Failed to parse application list response", e);
            throw new RuntimeException("Failed to parse application list: " + e.getMessage(), e);
        }
        
        return applications;
    }
    
    /**
     * 解析Plan名称列表响应
     * Parse plan names response
     * 
     * @param response JSON响应字符串
     * @return Plan名称列表
     */
    private List<String> parsePlanNames(String response) {
        String logMsg = "Parsing plan names response";
        logger.debug(logMsg);
        System.out.println(logMsg);
        
        List<String> planNames = new ArrayList<>();
        
        try {
            JSONArray jsonArray = new JSONArray(response);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                String planName = jsonArray.getString(i);
                planNames.add(planName);
            }
            
            logMsg = "Parsed " + planNames.size() + " plan names";
            logger.info(logMsg);
            System.out.println(logMsg);
            
            // 输出所有 plan 名称
            for (int i = 0; i < planNames.size(); i++) {
                logMsg = "  Plan[" + i + "]: " + planNames.get(i);
                System.out.println(logMsg);
            }
        } catch (Exception e) {
            logMsg = "Failed to parse plan names response: " + e.getMessage();
            logger.error(logMsg, e);
            System.err.println(logMsg);
            e.printStackTrace();
            throw new RuntimeException("Failed to parse plan names: " + e.getMessage(), e);
        }
        
        return planNames;
    }
    
    /**
     * 解析Plan构建结果响应
     * Parse plan build result response
     * 
     * @param response JSON响应字符串
     * @return PlanBuildResult对象
     */
    private PlanBuildResult parsePlanBuildResult(String response) {
        String logMsg = "Parsing plan build result response";
        logger.debug(logMsg);
        System.out.println(logMsg);
        
        PlanBuildResult result = new PlanBuildResult();
        
        try {
            JSONObject json = new JSONObject(response);
            
            result.setTitle(json.optString("title", ""));
            logMsg = "Plan title: " + result.getTitle();
            System.out.println(logMsg);
            
            JSONArray histories = json.optJSONArray("app_build_histories");
            if (histories != null) {
                List<BuildResult> buildResults = new ArrayList<>();
                
                logMsg = "Found " + histories.length() + " build histories";
                System.out.println(logMsg);
                
                for (int i = 0; i < histories.length(); i++) {
                    JSONObject historyJson = histories.getJSONObject(i);
                    BuildResult buildResult = parseBuildResultFromJson(historyJson);
                    buildResults.add(buildResult);
                    
                    logMsg = "  Build[" + i + "]: app=" + buildResult.getAppName() + 
                            ", status=" + buildResult.getBuildStatus() + 
                            ", id=" + buildResult.getId();
                    System.out.println(logMsg);
                }
                
                result.setAppBuildHistories(buildResults);
                logMsg = "Parsed plan build result with " + buildResults.size() + " build histories";
                logger.info(logMsg);
                System.out.println(logMsg);
            } else {
                logMsg = "No app_build_histories found in response";
                logger.warn(logMsg);
                System.err.println(logMsg);
            }
        } catch (Exception e) {
            logMsg = "Failed to parse plan build result response: " + e.getMessage();
            logger.error(logMsg, e);
            System.err.println(logMsg);
            e.printStackTrace();
            throw new RuntimeException("Failed to parse plan build result: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 解析App构建结果响应
     * Parse app build result response
     * 
     * @param response JSON响应字符串
     * @return AppBuildResult对象
     */
    private AppBuildResult parseAppBuildResult(String response) {
        logger.debug("Parsing app build result response");
        
        AppBuildResult result = new AppBuildResult();
        
        try {
            JSONObject json = new JSONObject(response);
            
            result.setTotal(json.optInt("total", 0));
            
            JSONArray dataArray = json.optJSONArray("data");
            if (dataArray != null) {
                List<BuildResult> buildResults = new ArrayList<>();
                
                for (int i = 0; i < dataArray.length(); i++) {
                    JSONObject dataJson = dataArray.getJSONObject(i);
                    BuildResult buildResult = parseBuildResultFromJson(dataJson);
                    buildResults.add(buildResult);
                }
                
                result.setData(buildResults);
                logger.info("Parsed app build result with {} build records (total: {})", 
                           buildResults.size(), result.getTotal());
            } else {
                logger.warn("No data array found in response");
            }
        } catch (Exception e) {
            logger.error("Failed to parse app build result response", e);
            throw new RuntimeException("Failed to parse app build result: " + e.getMessage(), e);
        }
        
        return result;
    }
    
    /**
     * 从JSON对象解析BuildResult
     * Parse BuildResult from JSON object
     * 
     * @param json JSON对象
     * @return BuildResult对象
     */
    private BuildResult parseBuildResultFromJson(JSONObject json) {
        BuildResult buildResult = new BuildResult();
        
        // ========== 详细日志：输出完整的原始 JSON ==========
        System.out.println("========================================");
        System.out.println("=== parseBuildResultFromJson: RAW JSON ===");
        System.out.println("========================================");
        System.out.println(json.toString(2));  // 格式化输出，缩进2个空格
        System.out.println("========================================");
        
        // 存储原始JSON数据
        // Store raw JSON data for tooltip display
        String rawJsonString = json.toString();
        buildResult.setRawJsonData(rawJsonString);
        System.out.println("Raw JSON stored, length: " + rawJsonString.length());
        
        // 提取基本字段
        String id = json.optString("id", "");
        buildResult.setId(id);
        System.out.println("Extracted id: [" + id + "]");
        
        // Queue ID - 处理可能不存在的情况
        long queueIdLong = json.optLong("queue_id", 0);
        String queueId = "";
        if (queueIdLong > 0) {
            queueId = String.valueOf(queueIdLong);
            buildResult.setQueueId(queueId);
        } else {
            buildResult.setQueueId("");
        }
        System.out.println("Extracted queue_id: [" + queueId + "] (raw long: " + queueIdLong + ")");
        
        String appName = json.optString("app_name", "");
        buildResult.setAppName(appName);
        System.out.println("Extracted app_name: [" + appName + "]");
        
        String imageName = json.optString("image_name", "");
        buildResult.setImageName(imageName);
        System.out.println("Extracted image_name: [" + imageName + "]");
        
        String createTime = json.optString("create_time", "");
        buildResult.setCreateTime(createTime);
        System.out.println("Extracted create_time: [" + createTime + "]");
        
        String modifyTime = json.optString("modify_time", "");
        buildResult.setModifyTime(modifyTime);
        System.out.println("Extracted modify_time: [" + modifyTime + "]");
        
        String creator = json.optString("creator", "");
        buildResult.setCreator(creator);
        System.out.println("Extracted creator: [" + creator + "]");
        
        String packageTitle = json.optString("package_title", "");
        buildResult.setPackageTitle(packageTitle);
        System.out.println("Extracted package_title: [" + packageTitle + "]");
        
        // 从callback节点获取build_status
        JSONObject callback = json.optJSONObject("callback");
        String buildStatus = "Unknown";
        if (callback != null) {
            buildStatus = callback.optString("build_status", "Unknown");
            buildResult.setBuildStatus(buildStatus);
            System.out.println("Extracted build_status from callback: [" + buildStatus + "]");
        } else {
            buildResult.setBuildStatus("Unknown");
            System.out.println("No callback object found, build_status set to: Unknown");
        }
        
        // 从request_parameters节点获取version和git_branch
        JSONObject requestParams = json.optJSONObject("request_parameters");
        if (requestParams != null) {
            String version = requestParams.optString("version", "");
            String gitBranch = requestParams.optString("git_branch", "");
            buildResult.setVersion(version);
            buildResult.setGitBranch(gitBranch);
            System.out.println("Extracted version from request_parameters: [" + version + "]");
            System.out.println("Extracted git_branch from request_parameters: [" + gitBranch + "]");
        } else {
            System.out.println("No request_parameters object found");
        }
        
        // 汇总日志输出
        System.out.println("========================================");
        System.out.println("=== SUMMARY ===");
        System.out.println("  id: " + buildResult.getId());
        System.out.println("  queueId: " + buildResult.getQueueId());
        System.out.println("  appName: " + buildResult.getAppName());
        System.out.println("  creator: " + buildResult.getCreator());
        System.out.println("  packageTitle: " + buildResult.getPackageTitle());
        System.out.println("  createTime: " + buildResult.getCreateTime());
        System.out.println("  modifyTime: " + buildResult.getModifyTime());
        System.out.println("  buildStatus: " + buildResult.getBuildStatus());
        System.out.println("========================================");
        
        return buildResult;
    }
    
    /**
     * 解析构建输出
     * Parse build output from query_one response
     * 
     * @param response JSON响应字符串
     * @return build_output内容
     */
    private String parseBuildOutput(String response) {
        logger.debug("Parsing build output response");
        
        try {
            logger.info("Attempting to parse JSON response...");
            JSONObject json = new JSONObject(response);
            
            logger.info("JSON parsed successfully, looking for callback object...");
            
            // 提取 callback.build_output
            JSONObject callback = json.optJSONObject("callback");
            if (callback != null) {
                String buildOutput = callback.optString("build_output", "");
                logger.info("Build output extracted, length: {}", buildOutput.length());
                return buildOutput;
            } else {
                logger.warn("No callback object found in response");
                logger.warn("Available keys in response: {}", json.keySet());
                return "(No callback object found in response)\n\nRaw response:\n" + response;
            }
        } catch (org.json.JSONException e) {
            logger.error("Failed to parse JSON response", e);
            logger.error("Response content: {}", response);
            return "(Failed to parse JSON response)\n\nError: " + e.getMessage() + 
                   "\n\nRaw response:\n" + response;
        } catch (Exception e) {
            logger.error("Unexpected error parsing build output", e);
            return "(Unexpected error)\n\nError: " + e.getMessage() + 
                   "\n\nRaw response:\n" + response;
        }
    }
    
    /**
     * 获取租户配置（包含分支列表）
     * Get tenant configuration including branch list
     * 
     * @param tenantCode 租户代码
     * @param token 认证Token
     * @return TenantConfig对象
     * @throws IOException 网络错误或API错误
     */
    public TenantConfig getTenantConfiguration(String tenantCode, String token) throws IOException {
        logger.info("=== Getting Tenant Configuration ===");
        logger.info("TenantCode: {}", tenantCode);
        
        String url = BASE_URL + "/api/mo-fo/1.0/ops/tenantconfig";
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-tenant", tenantCode);
        headers.put("authorization", "Bearer " + token);
        headers.put("Accept", "application/json");
        
        // 发送请求
        String response = sendGetRequest(url, headers);
        
        // 解析响应
        return parseTenantConfig(response);
    }
    
    /**
     * 提交多应用构建请求
     * Submit multi-application build request
     * 
     * @param tenantCode 租户代码
     * @param token 认证Token
     * @param requestBody JSON请求体
     * @throws IOException 网络错误或API错误
     */
    public void submitMultiBuild(String tenantCode, String token, String requestBody) throws IOException {
        logger.info("=== Submitting Multi-Build Request ===");
        logger.info("TenantCode: {}", tenantCode);
        
        String url = BASE_URL + "/api/mo-fo/1.0/ops/multi_build";
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-tenant", tenantCode);
        headers.put("authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json");
        
        // 发送请求
        String response = sendPostRequest(url, headers, requestBody);
        
        logger.info("Multi-build request submitted successfully");
    }
    
    /**
     * 提交单应用构建请求（用于rebuild）
     * Submit single application build request
     * 
     * @param tenantCode 租户代码
     * @param token 认证Token
     * @param requestBody 请求体JSON字符串
     * @return API响应
     * @throws IOException 网络错误或API错误
     */
    public String submitSingleBuild(String tenantCode, String token, String requestBody) throws IOException {
        logger.info("=== Submitting Single Build Request ===");
        logger.info("TenantCode: {}", tenantCode);
        logger.info("Request Body: {}", requestBody);
        
        String url = BASE_URL + "/api/mo-fo/1.0/ops/v2/build?clear_job=true&silences=true&force=false";
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-tenant", tenantCode);
        headers.put("authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json");
        
        // 发送请求
        String response = sendPostRequest(url, headers, requestBody);
        
        logger.info("Single build request submitted successfully");
        return response;
    }
    
    /**
     * 获取环境列表
     * Get environment list using new API
     * 
     * @param workspaceToken 工作空间Token
     * @return 环境名称列表
     * @throws IOException 网络错误或API错误
     */
    public List<String> getEnvironments(String workspaceToken) throws IOException {
        logger.info("=== Getting Environments ===");
        
        // 构建URL
        String url = BASE_URL + "/api/mo-fo/1.0/ops/env?status=&all=false";
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer " + workspaceToken);
        headers.put("Content-Type", "application/json");
        
        // 发送GET请求
        String response = sendGetRequest(url, headers);
        
        // 解析响应
        List<String> environments = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(response);
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject envObj = jsonArray.getJSONObject(i);
                String envName = envObj.optString("env_name", "");
                
                if (!envName.isEmpty()) {
                    environments.add(envName);
                    logger.debug("Found environment: {}", envName);
                }
            }
            
            logger.info("Loaded {} environments", environments.size());
            
        } catch (Exception e) {
            logger.error("Failed to parse environments response", e);
            throw new IOException("Failed to parse environments: " + e.getMessage(), e);
        }
        
        return environments;
    }
    
    /**
     * 部署镜像到指定环境
     * Deploy image to specified environment
     * 
     * @param workspace 工作空间（子租户代码）
     * @param environment 环境名称
     * @param workspaceToken 工作空间Token
     * @param imageName 镜像名称
     * @param appName 应用名称
     * @return 部署响应消息
     * @throws IOException 网络错误或API错误
     */
    public String deployImage(String workspace, String environment, String workspaceToken, 
                             String imageName, String appName) throws IOException {
        logger.info("=== Deploying Image ===");
        logger.info("Workspace: {}, Environment: {}, AppName: {}, ImageName: {}", 
                   workspace, environment, appName, imageName);
        
        // 构建URL with query parameters
        String url = BASE_URL + "/api/mo-fo/1.0/ops/v2/deployment?clear_job=true&silences=true&force=true";
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-env", environment);
        headers.put("x-mo-target-tenant", workspace);
        headers.put("authorization", "Bearer " + workspaceToken);
        headers.put("Content-Type", "application/json");
        
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("user_name", workspace);
        requestBody.put("app_name", appName);
        requestBody.put("image_name", imageName);
        requestBody.put("params", JSONObject.NULL);
        
        String jsonBody = requestBody.toString();
        
        // 发送请求
        String response = sendPostRequest(url, headers, jsonBody);
        
        // 解析响应检查是否成功
        try {
            JSONObject responseJson = new JSONObject(response);
            String code = responseJson.optString("code", "");
            String message = responseJson.optString("message", "");
            
            if ("i_common_success".equals(code)) {
                logger.info("Deployment successful: {}", message);
                return message;
            } else {
                logger.error("Deployment failed with code: {}, message: {}", code, message);
                throw new IOException("Deployment failed: " + message);
            }
        } catch (Exception e) {
            logger.error("Failed to parse deployment response", e);
            throw new IOException("Failed to parse deployment response: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析租户配置响应
     * Parse tenant configuration response
     * 
     * @param response JSON响应字符串
     * @return TenantConfig对象
     */
    private TenantConfig parseTenantConfig(String response) {
        logger.debug("Parsing tenant configuration response");
        System.out.println("[PortalApiClient] === Parsing Tenant Configuration ===");
        System.out.println("[PortalApiClient] Response: " + response);
        
        TenantConfig config = new TenantConfig();
        
        try {
            JSONObject json = new JSONObject(response);
            System.out.println("[PortalApiClient] Parsed JSON successfully");
            
            config.setId(json.optString("id", ""));
            config.setUserName(json.optString("user_name", ""));
            config.setDefaultBranch(json.optString("default_branch", ""));
            
            System.out.println("[PortalApiClient] Basic fields parsed: id=" + config.getId() + 
                             ", userName=" + config.getUserName() + 
                             ", defaultBranch=" + config.getDefaultBranch());
            
            // 解析分支列表
            System.out.println("[PortalApiClient] Parsing branch_list...");
            JSONArray branchArray = json.optJSONArray("branch_list");
            System.out.println("[PortalApiClient] branch_list type: " + 
                             (branchArray != null ? "JSONArray" : "null"));
            
            if (branchArray != null) {
                System.out.println("[PortalApiClient] branch_list length: " + branchArray.length());
                List<String> branches = new ArrayList<>();
                for (int i = 0; i < branchArray.length(); i++) {
                    String branch = branchArray.getString(i);
                    branches.add(branch);
                    System.out.println("[PortalApiClient] Branch[" + i + "]: " + branch);
                }
                config.setBranchList(branches);
                logger.info("Loaded {} branches for tenant {}", branches.size(), config.getUserName());
                System.out.println("[PortalApiClient] ✓ Loaded " + branches.size() + " branches");
            } else {
                logger.warn("No branch_list found in response");
                System.out.println("[PortalApiClient] ⚠ No branch_list found in response");
            }
            
            System.out.println("[PortalApiClient] ✓ Tenant configuration parsed successfully");
        } catch (Exception e) {
            logger.error("Failed to parse tenant configuration response", e);
            System.out.println("[PortalApiClient] ✗ Failed to parse tenant configuration: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to parse tenant configuration: " + e.getMessage(), e);
        }
        
        return config;
    }

    
    /**
     * 查询部署Pod列表
     * Query deployment pod list
     * 
     * @param workspace 工作空间（子租户代码）
     * @param environment 环境名称
     * @param workspaceToken 工作空间Token
     * @param appName 应用名称（可选，为空则查询所有）
     * @return Pod列表
     * @throws IOException 网络错误或API错误
     */
    public List<DeploymentPod> getDeploymentPods(String workspace, String environment, String workspaceToken, String appName) throws IOException {
        logger.info("=== Getting Deployment Pods ===");
        logger.info("Workspace: {}, Environment: {}, AppName: {}", workspace, environment, appName);
        
        String url = BASE_URL + "/api/mo-fo/1.0/ops/pod";
        if (appName != null && !appName.trim().isEmpty()) {
            url += "?app_name=" + appName;
        }
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-env", environment);
        headers.put("x-mo-target-tenant", workspace);
        headers.put("authorization", "Bearer " + workspaceToken);
        headers.put("Accept", "application/json");
        
        // 发送请求
        String response = sendGetRequest(url, headers);
        
        // 解析响应
        return parseDeploymentPods(response);
    }
    
    /**
     * 查询Pod日志
     * Query pod logs
     * 
     * @param workspace 工作空间（子租户代码）
     * @param environment 环境名称
     * @param workspaceToken 工作空间Token
     * @param podName Pod名称
     * @param appName 应用名称
     * @return 日志内容
     * @throws IOException 网络错误或API错误
     */
    public String getDeploymentPodLogs(String workspace, String environment, String workspaceToken, String podName, String appName) throws IOException {
        logger.info("=== Getting Deployment Pod Logs ===");
        logger.info("Workspace: {}, Environment: {}, PodName: {}, AppName: {}", workspace, environment, podName, appName);
        
        String url = BASE_URL + "/api/mo-fo/1.0/ops/pod_logs?pod_name=" + podName + "&app_name=" + appName + "&previous=false";
        
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-env", environment);
        headers.put("x-mo-target-tenant", workspace);
        headers.put("authorization", "Bearer " + workspaceToken);
        headers.put("Accept", "application/json");
        
        // 发送请求
        String response = sendGetRequest(url, headers);
        
        // 解析响应
        return parsePodLogs(response);
    }
    
    /**
     * 解析部署Pod列表响应
     * Parse deployment pods response
     * 
     * @param response JSON响应字符串
     * @return Pod列表
     */
    private List<DeploymentPod> parseDeploymentPods(String response) {
        List<DeploymentPod> pods = new ArrayList<>();
        
        try {
            JSONObject json = new JSONObject(response);
            JSONArray items = json.optJSONArray("items");
            
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    JSONObject metadata = item.optJSONObject("metadata");
                    
                    if (metadata != null) {
                        DeploymentPod pod = new DeploymentPod();
                        
                        // 提取基本信息
                        pod.setName(metadata.optString("name", ""));
                        pod.setNamespace(metadata.optString("namespace", ""));
                        pod.setCreationTimestamp(metadata.optString("creationTimestamp", ""));
                        
                        // 提取annotations
                        JSONObject annotations = metadata.optJSONObject("annotations");
                        if (annotations != null) {
                            pod.setApp(annotations.optString("app", ""));
                            pod.setRealStatus(annotations.optString("real_status", ""));
                        }
                        
                        // 提取spec.containers[0].image
                        JSONObject spec = item.optJSONObject("spec");
                        if (spec != null) {
                            JSONArray containers = spec.optJSONArray("containers");
                            if (containers != null && containers.length() > 0) {
                                JSONObject firstContainer = containers.getJSONObject(0);
                                String image = firstContainer.optString("image", "");
                                pod.setImage(image);
                                logger.debug("Extracted image: {}", image);
                            }
                        }
                        
                        pods.add(pod);
                        logger.debug("Parsed pod: {}", pod.getName());
                    }
                }
                
                logger.info("Parsed {} pods from response", pods.size());
            } else {
                logger.warn("No items array found in response");
            }
        } catch (Exception e) {
            logger.error("Failed to parse deployment pods response", e);
        }
        
        return pods;
    }
    
    /**
     * 解析Pod日志响应
     * Parse pod logs response
     * 
     * @param response JSON响应字符串
     * @return 日志内容
     */
    private String parsePodLogs(String response) {
        try {
            JSONObject json = new JSONObject(response);
            String data = json.optString("data", "");
            String status = json.optString("status", "");
            
            logger.info("Pod logs status: {}, data length: {}", status, data.length());
            
            if ("SUCCESS".equals(status)) {
                return data;
            } else {
                logger.warn("Pod logs query returned non-SUCCESS status: {}", status);
                return "Failed to retrieve logs. Status: " + status;
            }
        } catch (Exception e) {
            logger.error("Failed to parse pod logs response", e);
            return "Error parsing logs: " + e.getMessage();
        }
    }
}
