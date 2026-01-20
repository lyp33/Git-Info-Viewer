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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Jenkins REST API 客户端
 */
public class JenkinsApiClient {
    private static final Logger logger = LoggerFactory.getLogger(JenkinsApiClient.class);
    
    private String baseUrl;
    private String username;
    private String apiToken;

    public JenkinsApiClient(String baseUrl, String username, String apiToken) {
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "";  // 移除末尾的斜杠
        this.username = username;
        this.apiToken = apiToken;
    }

    /**
     * 构建 Basic Authentication 头
     */
    private String getAuthHeader() {
        if (username == null || username.isEmpty() || apiToken == null || apiToken.isEmpty()) {
            return null;
        }
        String auth = username + ":" + apiToken;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 发送 GET 请求
     */
    private String sendGetRequest(String urlString) throws IOException {
        logger.info("GET request to: {}", urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Charset", "UTF-8");
        
        String authHeader = getAuthHeader();
        if (authHeader != null) {
            conn.setRequestProperty("Authorization", authHeader);
        }
        
        int responseCode = conn.getResponseCode();
        logger.info("Response code: {}", responseCode);
        
        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
            throw new IOException("Authentication failed. Please check your Jenkins credentials.");
        }
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: " + responseCode);
        }
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
            response.append("\n");  // 添加换行符！
        }
        in.close();
        
        return response.toString();
    }

    /**
     * 发送 POST 请求
     */
    private String sendPostRequest(String urlString, String postData) throws IOException {
        logger.info("POST request to: {}", urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        
        String authHeader = getAuthHeader();
        if (authHeader != null) {
            conn.setRequestProperty("Authorization", authHeader);
        }
        
        if (postData != null && !postData.isEmpty()) {
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }
        
        int responseCode = conn.getResponseCode();
        logger.info("Response code: {}", responseCode);
        
        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
            throw new IOException("Authentication failed. Please check your Jenkins credentials.");
        }
        
        if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_CREATED) {
            throw new IOException("HTTP error code: " + responseCode);
        }
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        
        return response.toString();
    }

    /**
     * 获取作业层次结构
     */
    public List<JenkinsItem> fetchJobHierarchy(String jobPath) throws IOException {
        String apiUrl = baseUrl + "/" + jobPath + "/api/json?tree=jobs[name,url,_class,jobs]";
        logger.info("Fetching job hierarchy from: {}", apiUrl);
        System.out.println("Fetching job hierarchy from: " + apiUrl);
        
        String response = sendGetRequest(apiUrl);
        
        JSONObject json = new JSONObject(response);
        List<JenkinsItem> items = new ArrayList<>();
        
        if (json.has("jobs")) {
            JSONArray jobs = json.getJSONArray("jobs");
            for (int i = 0; i < jobs.length(); i++) {
                JSONObject jobJson = jobs.getJSONObject(i);
                String name = jobJson.optString("name", "");
                String url = jobJson.optString("url", "");
                String className = jobJson.optString("_class", "");
                
                JenkinsItem item = new JenkinsItem(name, url, className);
                items.add(item);
            }
        }
        
        return items;
    }

    /**
     * 获取作业详情
     */
    public JenkinsJob fetchJobDetails(String jobPath) throws IOException {
        String apiUrl = baseUrl + "/" + jobPath + "/api/json";
        String response = sendGetRequest(apiUrl);
        
        JSONObject json = new JSONObject(response);
        
        JenkinsJob job = new JenkinsJob();
        job.setName(json.optString("name", ""));
        job.setUrl(json.optString("url", ""));
        job.setFullName(json.optString("fullName", ""));
        job.setDescription(json.optString("description", ""));
        
        return job;
    }

    /**
     * 获取构建历史
     */
    public List<JenkinsBuild> fetchBuildHistory(String jobPath, int limit) throws IOException {
        // 添加 actions 参数到 API 请求，获取触发用户和参数信息
        String apiUrl = baseUrl + "/" + jobPath + "/api/json?tree=builds[number,result,timestamp,url,actions[causes[userId,userName],parameters[name,value]]]{0," + limit + "}";
        String response = sendGetRequest(apiUrl);
        
        JSONObject json = new JSONObject(response);
        List<JenkinsBuild> builds = new ArrayList<>();
        
        if (json.has("builds")) {
            JSONArray buildsArray = json.getJSONArray("builds");
            for (int i = 0; i < buildsArray.length(); i++) {
                JSONObject buildJson = buildsArray.getJSONObject(i);
                
                int number = buildJson.optInt("number", 0);
                String result = buildJson.optString("result", null);
                long timestamp = buildJson.optLong("timestamp", 0);
                String url = buildJson.optString("url", "");
                
                JenkinsBuild build = new JenkinsBuild(number, result, timestamp, url);
                
                // 解析 actions 获取触发用户和参数
                if (buildJson.has("actions")) {
                    JSONArray actions = buildJson.getJSONArray("actions");
                    parseActions(actions, build);
                }
                
                builds.add(build);
            }
        }
        
        return builds;
    }
    
    /**
     * 解析 actions 获取触发用户和参数
     */
    private void parseActions(JSONArray actions, JenkinsBuild build) {
        for (int i = 0; i < actions.length(); i++) {
            JSONObject action = actions.getJSONObject(i);
            
            // 解析触发用户（CauseAction）
            if (action.has("causes")) {
                JSONArray causes = action.getJSONArray("causes");
                if (causes.length() > 0) {
                    JSONObject cause = causes.getJSONObject(0);
                    
                    // 用户触发
                    if (cause.has("userId")) {
                        String userId = cause.optString("userId", "");
                        if (!userId.isEmpty()) {
                            build.setTriggeredBy(userId);
                        }
                    } else if (cause.has("userName")) {
                        String userName = cause.optString("userName", "");
                        if (!userName.isEmpty()) {
                            build.setTriggeredBy(userName);
                        }
                    }
                    
                    // 定时任务触发
                    if (cause.has("_class")) {
                        String causeClass = cause.optString("_class", "");
                        if (causeClass.contains("TimerTrigger")) {
                            build.setTriggeredBy("Timer");
                        } else if (causeClass.contains("SCMTrigger")) {
                            build.setTriggeredBy("SCM");
                        }
                    }
                }
            }
            
            // 解析构建参数（ParametersAction）
            if (action.has("parameters")) {
                JSONArray parameters = action.getJSONArray("parameters");
                for (int j = 0; j < parameters.length(); j++) {
                    JSONObject param = parameters.getJSONObject(j);
                    String name = param.optString("name", "");
                    String value = param.optString("value", "");
                    
                    if (!name.isEmpty()) {
                        build.addParameter(name, value);
                    }
                }
            }
        }
        
        // 如果没有找到触发用户，设置默认值
        if (build.getTriggeredBy() == null || build.getTriggeredBy().isEmpty()) {
            build.setTriggeredBy("Unknown");
        }
    }

    /**
     * 获取构建参数定义
     */
    public List<JenkinsBuildParameter> fetchBuildParameters(String jobPath) throws IOException {
        String apiUrl = baseUrl + "/" + jobPath + "/api/json?tree=property[parameterDefinitions[*]]";
        String response = sendGetRequest(apiUrl);
        
        JSONObject json = new JSONObject(response);
        List<JenkinsBuildParameter> parameters = new ArrayList<>();
        
        if (json.has("property")) {
            JSONArray properties = json.getJSONArray("property");
            for (int i = 0; i < properties.length(); i++) {
                JSONObject property = properties.getJSONObject(i);
                if (property.has("parameterDefinitions")) {
                    JSONArray paramDefs = property.getJSONArray("parameterDefinitions");
                    for (int j = 0; j < paramDefs.length(); j++) {
                        JSONObject paramDef = paramDefs.getJSONObject(j);
                        
                        String name = paramDef.optString("name", "");
                        String type = paramDef.optString("_class", "");
                        String description = paramDef.optString("description", "");
                        Object defaultValue = paramDef.opt("defaultParameterValue");
                        
                        // 提取默认值
                        Object actualDefaultValue = null;
                        if (defaultValue instanceof JSONObject) {
                            JSONObject defaultObj = (JSONObject) defaultValue;
                            actualDefaultValue = defaultObj.opt("value");
                        }
                        
                        JenkinsBuildParameter param = new JenkinsBuildParameter(name, type, description, actualDefaultValue);
                        
                        // 如果是选择参数，提取选项列表
                        if (paramDef.has("choices")) {
                            JSONArray choices = paramDef.getJSONArray("choices");
                            List<String> choiceList = new ArrayList<>();
                            for (int k = 0; k < choices.length(); k++) {
                                choiceList.add(choices.getString(k));
                            }
                            param.setChoices(choiceList);
                        }
                        
                        parameters.add(param);
                    }
                }
            }
        }
        
        return parameters;
    }

    /**
     * 触发构建
     */
    public String triggerBuild(String jobPath, Map<String, String> parameters) throws IOException {
        StringBuilder postData = new StringBuilder();
        
        if (parameters != null && !parameters.isEmpty()) {
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (postData.length() > 0) {
                    postData.append("&");
                }
                postData.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                postData.append("=");
                postData.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }
        
        String apiUrl;
        if (parameters != null && !parameters.isEmpty()) {
            apiUrl = baseUrl + "/" + jobPath + "/buildWithParameters";
        } else {
            apiUrl = baseUrl + "/" + jobPath + "/build";
        }
        
        String response = sendPostRequest(apiUrl, postData.toString());
        return "Build triggered successfully";
    }

    /**
     * 获取构建的 Stage 信息
     */
    public List<JenkinsStage> fetchBuildStages(String jobPath, int buildNumber) throws IOException {
        String apiUrl = baseUrl + "/" + jobPath + "/" + buildNumber + "/wfapi/describe";
        
        logger.info("=== Fetching Build Stages ===");
        logger.info("Job Path: {}", jobPath);
        logger.info("Build Number: {}", buildNumber);
        logger.info("API URL: {}", apiUrl);
        
        try {
            String response = sendGetRequest(apiUrl);
            logger.info("API Response received, length: {}", response.length());
            
            JSONObject json = new JSONObject(response);
            List<JenkinsStage> stages = new ArrayList<>();
            
            if (json.has("stages")) {
                JSONArray stagesArray = json.getJSONArray("stages");
                logger.info("Number of stages found: {}", stagesArray.length());
                
                for (int i = 0; i < stagesArray.length(); i++) {
                    JSONObject stageJson = stagesArray.getJSONObject(i);
                    
                    String id = stageJson.optString("id", "");
                    String name = stageJson.optString("name", "");
                    String status = stageJson.optString("status", "");
                    long durationMillis = stageJson.optLong("durationMillis", 0);
                    
                    logger.info("Stage {}: name='{}', id='{}', status='{}', duration={}ms", 
                               i+1, name, id, status, durationMillis);
                    
                    JenkinsStage stage = new JenkinsStage(name, status, durationMillis);
                    stage.setId(id);
                    stages.add(stage);
                }
            } else {
                logger.warn("No 'stages' field found in API response");
            }
            
            logger.info("=== Fetch Complete: {} stages ===", stages.size());
            return stages;
        } catch (IOException e) {
            // 如果获取 Stage 信息失败，返回空列表
            logger.error("Failed to fetch stage information: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // 缓存 Console Log 以提高性能
    private String cachedConsoleLog = null;
    private int cachedBuildNumber = -1;
    private String cachedJobPath = null;

    /**
     * 获取构建的完整 Console Log
     */
    public String fetchBuildConsoleLog(String jobPath, int buildNumber) throws IOException {
        String apiUrl = baseUrl + "/" + jobPath + "/" + buildNumber + "/consoleText";
        
        logger.info("=== Fetching Build Console Log ===");
        logger.info("API URL: {}", apiUrl);
        
        try {
            String response = sendGetRequest(apiUrl);
            logger.info("Console log fetched successfully, length: {}", response.length());
            return response;
        } catch (IOException e) {
            logger.error("Failed to fetch console log: {}", e.getMessage());
            throw new IOException("Failed to fetch console log: " + e.getMessage());
        }
    }

    /**
     * 从完整的 Console Log 中提取特定 Stage 的日志
     */
    public String extractStageLogFromConsole(String fullConsoleLog, String stageName) {
        if (fullConsoleLog == null || fullConsoleLog.isEmpty() || stageName == null) {
            return "";
        }
        
        // 尝试两种模式：
        // 1. 串行 Stage: [Pipeline] { (stageName)
        // 2. 并行 Stage: [Pipeline] [branchName] { (stageName)
        
        String stageLog = extractSerialStageLog(fullConsoleLog, stageName);
        if (!stageLog.isEmpty()) {
            return stageLog;
        }
        
        stageLog = extractParallelStageLog(fullConsoleLog, stageName);
        if (!stageLog.isEmpty()) {
            return stageLog;
        }
        
        logger.warn("No log found for stage: {}", stageName);
        return "";
    }
    
    /**
     * 提取串行 Stage 的日志
     * 格式: [Pipeline] { (stageName)
     */
    private String extractSerialStageLog(String fullConsoleLog, String stageName) {
        // 查找 Stage 开始标记：[Pipeline] { (stageName)
        String stageStartMarker = "[Pipeline] { (" + stageName + ")";
        int startIndex = fullConsoleLog.indexOf(stageStartMarker);
        
        if (startIndex == -1) {
            return "";
        }
        
        // 从 Stage 名称之前开始（包含 [Pipeline] stage）
        int stageLineStart = fullConsoleLog.lastIndexOf("[Pipeline] stage", startIndex);
        if (stageLineStart != -1) {
            startIndex = stageLineStart;
        }
        
        // 查找 Stage 结束标记：[Pipeline] // stage
        String stageEndMarker = "[Pipeline] // stage";
        int endIndex = fullConsoleLog.indexOf(stageEndMarker, startIndex);
        
        if (endIndex == -1) {
            // 返回从开始到文件末尾的内容
            return fullConsoleLog.substring(startIndex);
        }
        
        // 包含结束标记
        endIndex = fullConsoleLog.indexOf("\n", endIndex);
        if (endIndex == -1) {
            endIndex = fullConsoleLog.length();
        }
        
        String stageLog = fullConsoleLog.substring(startIndex, endIndex);
        logger.info("Extracted serial stage log for '{}', length: {}", stageName, stageLog.length());
        return stageLog;
    }
    
    /**
     * 提取并行 Stage 的日志
     * 格式: [Pipeline] [branchName] { (stageName)
     */
    private String extractParallelStageLog(String fullConsoleLog, String stageName) {
        // 查找并行 Stage 开始标记：[Pipeline] [xxx] { (stageName)
        // 使用正则表达式匹配：\[Pipeline\] \[.*?\] \{ \(stageName\)
        String pattern = "\\[Pipeline\\] \\[([^\\]]+)\\] \\{ \\(" + Pattern.quote(stageName) + "\\)";
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(fullConsoleLog);
        
        if (!m.find()) {
            return "";
        }
        
        String branchName = m.group(1);  // 提取 branch 名称
        int startIndex = m.start();
        
        logger.info("Found parallel stage '{}' in branch '{}'", stageName, branchName);
        
        // 从 Stage 声明之前开始（包含 [Pipeline] [branchName] stage）
        String branchStageMarker = "[Pipeline] [" + branchName + "] stage";
        int branchStageIndex = fullConsoleLog.lastIndexOf(branchStageMarker, startIndex);
        if (branchStageIndex != -1) {
            startIndex = branchStageIndex;
        }
        
        // 查找 Stage 结束标记：[Pipeline] [branchName] // stage
        String stageEndMarker = "[Pipeline] [" + branchName + "] // stage";
        int endIndex = fullConsoleLog.indexOf(stageEndMarker, startIndex);
        
        if (endIndex == -1) {
            // 尝试查找 branch 结束标记：[Pipeline] [branchName] }
            String branchEndMarker = "[Pipeline] [" + branchName + "] }";
            endIndex = fullConsoleLog.indexOf(branchEndMarker, startIndex);
            
            if (endIndex == -1) {
                // 返回从开始到文件末尾的内容
                return fullConsoleLog.substring(startIndex);
            }
        }
        
        // 包含结束标记
        endIndex = fullConsoleLog.indexOf("\n", endIndex);
        if (endIndex == -1) {
            endIndex = fullConsoleLog.length();
        }
        
        String stageLog = fullConsoleLog.substring(startIndex, endIndex);
        logger.info("Extracted parallel stage log for '{}' (branch: {}), length: {}", stageName, branchName, stageLog.length());
        return stageLog;
    }

    /**
     * 获取 Stage 日志（使用缓存优化）
     */
    public String fetchStageLog(String jobPath, int buildNumber, String stageId, String stageName) throws IOException {
        logger.info("=== Fetching Stage Log ===");
        logger.info("Job Path: {}", jobPath);
        logger.info("Build Number: {}", buildNumber);
        logger.info("Stage Name: {}", stageName);
        
        // 检查缓存
        if (cachedBuildNumber == buildNumber && 
            jobPath.equals(cachedJobPath) && 
            cachedConsoleLog != null) {
            logger.info("Using cached console log");
            return extractStageLogFromConsole(cachedConsoleLog, stageName);
        }
        
        // 获取完整的 Console Log
        logger.info("Fetching full console log...");
        String fullLog = fetchBuildConsoleLog(jobPath, buildNumber);
        
        // 缓存
        cachedConsoleLog = fullLog;
        cachedBuildNumber = buildNumber;
        cachedJobPath = jobPath;
        
        // 提取特定 Stage 的日志
        return extractStageLogFromConsole(fullLog, stageName);
    }
    
    /**
     * 获取 Stage 日志（兼容旧接口）
     */
    public String fetchStageLog(String jobPath, int buildNumber, String stageId) throws IOException {
        // 这个方法需要 stageName，但旧接口没有提供
        // 返回提示信息
        return "Please use fetchStageLog(jobPath, buildNumber, stageId, stageName) instead";
    }

    /**
     * 获取构建参数（用于 rebuild）
     */
    public Map<String, String> fetchBuildParametersForRebuild(String jobPath, int buildNumber) throws IOException {
        String apiUrl = baseUrl + "/" + jobPath + "/" + buildNumber + "/api/json?tree=actions[parameters[name,value]]";
        String response = sendGetRequest(apiUrl);
        
        JSONObject json = new JSONObject(response);
        Map<String, String> parameters = new HashMap<>();
        
        if (json.has("actions")) {
            JSONArray actions = json.getJSONArray("actions");
            for (int i = 0; i < actions.length(); i++) {
                JSONObject action = actions.getJSONObject(i);
                if (action.has("parameters")) {
                    JSONArray params = action.getJSONArray("parameters");
                    for (int j = 0; j < params.length(); j++) {
                        JSONObject param = params.getJSONObject(j);
                        String name = param.optString("name", "");
                        Object value = param.opt("value");
                        if (value != null) {
                            parameters.put(name, value.toString());
                        }
                    }
                }
            }
        }
        
        return parameters;
    }

    /**
     * 从 Stage Log 中提取 Build ID
     * 
     * @param stageLog Stage 日志内容
     * @return Build ID，如果未找到返回 null
     */
    public Integer extractStageBuildId(String stageLog) {
        if (stageLog == null || stageLog.isEmpty()) {
            return null;
        }
        
        // 匹配模式：
        // 1. "of #809"
        // 2. "building: ... #809"
        // 3. "CI-Robot of #809"
        Pattern pattern = Pattern.compile("(?:of|building:.*?)\\s*#(\\d+)");
        Matcher matcher = pattern.matcher(stageLog);
        
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse stage build ID: {}", matcher.group(1));
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * 从 Stage Log 中提取作业路径
     * 例如: "Starting building: gemini » Manual-Build » thailifesdk » 24.08_thailife_dev » CI-Robot » BS-CI-ROBOT #578"
     * 注意：分隔符是 » (U+00BB)，不是 ?
     * 
     * @param stageLog Stage 日志内容
     * @return 作业路径（不包含 gemini 前缀），如果未找到返回 null
     */
    public String extractJobPathFromStageLog(String stageLog) {
        System.out.println("[JenkinsApiClient] extractJobPathFromStageLog() called");
        
        if (stageLog == null || stageLog.isEmpty()) {
            System.out.println("[JenkinsApiClient] Stage log is null or empty");
            return null;
        }
        
        System.out.println("[JenkinsApiClient] Searching for job path pattern in log...");
        System.out.println("[JenkinsApiClient] Log preview (first 500 chars): " + stageLog.substring(0, Math.min(500, stageLog.length())));
        
        // 匹配模式（按优先级顺序尝试）:
        // 1. "Starting building: gemini » xxx » yyy » zzz #123"
        // 2. "Building gemini » xxx » yyy » zzz"
        // 3. "Scheduling project: gemini » xxx » yyy » zzz"
        // 注意：分隔符是 » (右双尖括号, U+00BB)
        
        // 模式1: Starting building (最常见，优先匹配)
        Pattern pattern1 = Pattern.compile("Starting building:\\s+gemini\\s*»\\s*([^#\\n]+?)\\s*#");
        Matcher matcher1 = pattern1.matcher(stageLog);
        if (matcher1.find()) {
            String jobPath = matcher1.group(1).trim();
            logger.info("Extracted job path from 'Starting building': {}", jobPath);
            System.out.println("[JenkinsApiClient] ✓ Found job path (Starting building): " + jobPath);
            return jobPath;
        }
        
        // 模式2: Building
        Pattern pattern2 = Pattern.compile("(?:Building)\\s+gemini\\s*»\\s*([^\\n#]+?)(?:\\s*#|\\n|$)");
        Matcher matcher2 = pattern2.matcher(stageLog);
        if (matcher2.find()) {
            String jobPath = matcher2.group(1).trim();
            logger.info("Extracted job path from 'Building': {}", jobPath);
            System.out.println("[JenkinsApiClient] ✓ Found job path (Building): " + jobPath);
            return jobPath;
        }
        
        // 模式3: Scheduling project
        Pattern pattern3 = Pattern.compile("Scheduling project:\\s+gemini\\s*»\\s*([^\\n#]+?)(?:\\s*#|\\n|$)");
        Matcher matcher3 = pattern3.matcher(stageLog);
        if (matcher3.find()) {
            String jobPath = matcher3.group(1).trim();
            logger.info("Extracted job path from 'Scheduling project': {}", jobPath);
            System.out.println("[JenkinsApiClient] ✓ Found job path (Scheduling project): " + jobPath);
            return jobPath;
        }
        
        logger.warn("No job path found in stage log");
        System.out.println("[JenkinsApiClient] ✗ No job path found in stage log");
        return null;
    }
    
    /**
     * 构建子作业的 URL
     * 将 "Manual-Build » thailifesdk » 24.08_thailife_dev" 转换为
     * "/job/Manual-Build/job/thailifesdk/job/24.08_thailife_dev"
     * 注意：分隔符是 » (U+00BB)
     * 
     * @param jobPath 作业路径（使用 » 分隔）
     * @param buildNumber 构建编号
     * @return 完整的子作业 URL
     */
    public String constructSubJobUrl(String jobPath, int buildNumber) {
        System.out.println("[JenkinsApiClient] constructSubJobUrl() called");
        System.out.println("[JenkinsApiClient] Input job path: " + jobPath);
        System.out.println("[JenkinsApiClient] Input build number: " + buildNumber);
        
        if (jobPath == null || jobPath.isEmpty()) {
            System.out.println("[JenkinsApiClient] Job path is null or empty");
            return null;
        }
        
        // 分割路径并构建 URL (分隔符是 »)
        String[] parts = jobPath.split("\\s*»\\s*");
        System.out.println("[JenkinsApiClient] Split into " + parts.length + " parts");
        
        StringBuilder urlPath = new StringBuilder("/job/gemini");
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            System.out.println("[JenkinsApiClient]   Part " + i + ": " + part);
            if (!part.isEmpty()) {
                urlPath.append("/job/").append(part);
            }
        }
        
        urlPath.append("/").append(buildNumber).append("/consoleText");
        
        String fullUrl = baseUrl + urlPath.toString();
        logger.info("Constructed sub-job URL: {}", fullUrl);
        System.out.println("[JenkinsApiClient] ✓ Constructed URL: " + fullUrl);
        return fullUrl;
    }
    
    /**
     * 获取子作业的 Console Log
     * 从 Stage Log 中提取作业路径和构建 ID，然后获取该子作业的完整日志
     * 
     * @param stageLog Stage 日志内容
     * @return 子作业的 Console Log，如果无法获取返回原始 Stage Log
     */
    public String fetchSubJobConsoleLog(String stageLog) {
        System.out.println("[JenkinsApiClient] fetchSubJobConsoleLog() called");
        System.out.println("[JenkinsApiClient] Stage log length: " + (stageLog != null ? stageLog.length() : 0));
        
        if (stageLog == null || stageLog.isEmpty()) {
            System.out.println("[JenkinsApiClient] Stage log is null or empty, returning as-is");
            return stageLog;
        }
        
        try {
            // 提取作业路径
            System.out.println("[JenkinsApiClient] Extracting job path from stage log...");
            String jobPath = extractJobPathFromStageLog(stageLog);
            System.out.println("[JenkinsApiClient] Extracted job path: " + jobPath);
            
            if (jobPath == null) {
                logger.warn("Cannot extract job path from stage log, returning original log");
                System.out.println("[JenkinsApiClient] Cannot extract job path, returning original log");
                return stageLog;
            }
            
            // 提取构建 ID
            System.out.println("[JenkinsApiClient] Extracting build ID from stage log...");
            Integer buildId = extractStageBuildId(stageLog);
            System.out.println("[JenkinsApiClient] Extracted build ID: " + buildId);
            
            if (buildId == null) {
                logger.warn("Cannot extract build ID from stage log, returning original log");
                System.out.println("[JenkinsApiClient] Cannot extract build ID, returning original log");
                return stageLog;
            }
            
            // 构建 URL
            System.out.println("[JenkinsApiClient] Constructing sub-job URL...");
            String subJobUrl = constructSubJobUrl(jobPath, buildId);
            System.out.println("[JenkinsApiClient] Constructed URL: " + subJobUrl);
            
            if (subJobUrl == null) {
                logger.warn("Cannot construct sub-job URL, returning original log");
                System.out.println("[JenkinsApiClient] Cannot construct URL, returning original log");
                return stageLog;
            }
            
            // 获取子作业日志
            logger.info("Fetching sub-job console log from: {}", subJobUrl);
            System.out.println("[JenkinsApiClient] Fetching sub-job console log from: " + subJobUrl);
            String subJobLog = sendGetRequest(subJobUrl);
            logger.info("Successfully fetched sub-job console log, length: {}", subJobLog.length());
            System.out.println("[JenkinsApiClient] Successfully fetched sub-job log, length: " + subJobLog.length());
            
            return subJobLog;
            
        } catch (Exception e) {
            logger.error("Failed to fetch sub-job console log: {}", e.getMessage(), e);
            System.err.println("[JenkinsApiClient] ERROR: Failed to fetch sub-job console log: " + e.getMessage());
            e.printStackTrace();
            // 如果失败，返回原始 Stage Log
            System.out.println("[JenkinsApiClient] Returning original stage log due to error");
            return stageLog;
        }
    }
    
    /**
     * 获取 Portal Build Output
     * 从 Stage Log 中提取 curl 命令的 URL 和 headers，然后调用 Portal API
     * 
     * @param stageLog Stage 日志内容
     * @return build_output 内容，如果失败返回错误信息
     */
    public String fetchPortalBuildOutput(String stageLog) {
        System.out.println("[JenkinsApiClient] fetchPortalBuildOutput() called");
        
        if (stageLog == null || stageLog.isEmpty()) {
            return "Stage log is empty, cannot extract Portal API information";
        }
        
        try {
            // 从 Stage Log 中提取 Portal API URL
            String portalUrl = extractPortalUrl(stageLog);
            if (portalUrl == null) {
                return "Could not find Portal API URL in stage log";
            }
            
            System.out.println("[JenkinsApiClient] Extracted Portal URL: " + portalUrl);
            
            // 从 Stage Log 中提取 headers
            Map<String, String> headers = extractCurlHeaders(stageLog);
            System.out.println("[JenkinsApiClient] Extracted " + headers.size() + " headers");
            
            // 调用 Portal API
            String jsonResponse = sendGetRequestWithHeaders(portalUrl, headers);
            System.out.println("[JenkinsApiClient] Portal API response length: " + jsonResponse.length());
            
            // 解析 JSON 响应，提取 build_output 字段
            JSONObject json = new JSONObject(jsonResponse);
            
            if (json.has("build_output")) {
                String buildOutput = json.getString("build_output");
                System.out.println("[JenkinsApiClient] Extracted build_output, length: " + buildOutput.length());
                return buildOutput;
            } else {
                System.out.println("[JenkinsApiClient] No build_output field in response");
                return "No build_output field found in Portal API response.\n\nFull response:\n" + jsonResponse;
            }
            
        } catch (Exception e) {
            logger.error("Failed to fetch Portal build output: {}", e.getMessage(), e);
            System.err.println("[JenkinsApiClient] ERROR: Failed to fetch Portal build output: " + e.getMessage());
            e.printStackTrace();
            return "Failed to fetch Portal build output:\n" + e.getMessage();
        }
    }
    
    /**
     * 从 Stage Log 中提取 Portal API URL
     * 查找 curl 命令中的 https://portal-gw.insuremo.com/... URL
     * 
     * @param stageLog Stage 日志内容
     * @return Portal API URL，如果未找到返回 null
     */
    private String extractPortalUrl(String stageLog) {
        System.out.println("[JenkinsApiClient] ========================================");
        System.out.println("[JenkinsApiClient] VERSION: 2026-01-20-17:20 - Find LAST Portal URL Match");
        System.out.println("[JenkinsApiClient] Extracting Portal URL from stage log (line by line)...");
        System.out.println("[JenkinsApiClient] ========================================");
        
        if (stageLog == null || stageLog.isEmpty()) {
            System.out.println("[JenkinsApiClient] Stage log is null or empty");
            return null;
        }
        
        // 逐行解析
        String[] lines = stageLog.split("\n");
        System.out.println("[JenkinsApiClient] Total lines to parse: " + lines.length);
        
        // 匹配 https://portal-gw.insuremo.com/ 开头的 URL（包括查询参数）
        // 支持多种格式：
        // 1. curl ... 'https://portal-gw.insuremo.com/...' ... (单引号包围)
        // 2. curl ... "https://portal-gw.insuremo.com/..." ... (双引号包围)
        // 3. curl ... https://portal-gw.insuremo.com/... ... (无引号)
        
        // 尝试两种模式
        Pattern quotedPattern = Pattern.compile("['\"]https://portal-gw\\.insuremo\\.com/[^'\"]*['\"]");
        Pattern unquotedPattern = Pattern.compile("https://portal-gw\\.insuremo\\.com/\\S+");
        
        System.out.println("[JenkinsApiClient] Quoted pattern: " + quotedPattern.pattern());
        System.out.println("[JenkinsApiClient] Unquoted pattern: " + unquotedPattern.pattern());
        
        // 保存最后一个找到的 URL
        String lastFoundUrl = null;
        int lastFoundLineNumber = -1;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            
            // 只处理包含 curl 和 portal-gw 的行
            if (line.contains("curl") && line.contains("portal-gw.insuremo.com")) {
                System.out.println("[JenkinsApiClient] ----------------------------------------");
                System.out.println("[JenkinsApiClient] Found potential Portal URL line " + (i + 1));
                System.out.println("[JenkinsApiClient] Line content: " + line);
                System.out.println("[JenkinsApiClient] ----------------------------------------");
                
                String foundUrl = null;
                
                // 先尝试匹配带引号的 URL
                System.out.println("[JenkinsApiClient] Trying quoted pattern...");
                Matcher quotedMatcher = quotedPattern.matcher(line);
                if (quotedMatcher.find()) {
                    String urlWithQuotes = quotedMatcher.group(0);
                    System.out.println("[JenkinsApiClient] Quoted match found: " + urlWithQuotes);
                    // 移除引号
                    foundUrl = urlWithQuotes.substring(1, urlWithQuotes.length() - 1);
                    System.out.println("[JenkinsApiClient] Extracted URL (quoted): " + foundUrl);
                    System.out.println("[JenkinsApiClient] URL length: " + foundUrl.length());
                } else {
                    System.out.println("[JenkinsApiClient] No quoted match found");
                    
                    // 如果没有找到带引号的，尝试匹配不带引号的
                    System.out.println("[JenkinsApiClient] Trying unquoted pattern...");
                    Matcher unquotedMatcher = unquotedPattern.matcher(line);
                    if (unquotedMatcher.find()) {
                        foundUrl = unquotedMatcher.group(0);
                        System.out.println("[JenkinsApiClient] Extracted URL (unquoted): " + foundUrl);
                        System.out.println("[JenkinsApiClient] URL length: " + foundUrl.length());
                    } else {
                        System.out.println("[JenkinsApiClient] No unquoted match found");
                    }
                }
                
                // 如果找到了 URL，保存为最后一个
                if (foundUrl != null) {
                    lastFoundUrl = foundUrl;
                    lastFoundLineNumber = i + 1;
                    System.out.println("[JenkinsApiClient] Saved as last found URL (line " + lastFoundLineNumber + ")");
                }
            }
        }
        
        // 返回最后一个找到的 URL
        if (lastFoundUrl != null) {
            System.out.println("[JenkinsApiClient] ========================================");
            System.out.println("[JenkinsApiClient] ✓✓✓ RETURNING LAST Portal URL (line " + lastFoundLineNumber + "):");
            System.out.println("[JenkinsApiClient] " + lastFoundUrl);
            System.out.println("[JenkinsApiClient] URL length: " + lastFoundUrl.length());
            System.out.println("[JenkinsApiClient] ========================================");
            return lastFoundUrl;
        }
        
        System.out.println("[JenkinsApiClient] ✗✗✗ No Portal URL found in any line");
        System.out.println("[JenkinsApiClient] ========================================");
        return null;
    }
    
    /**
     * 从 Stage Log 中提取 curl 命令的 headers
     * 查找 -H 'key: value' 格式的 headers
     * 
     * @param stageLog Stage 日志内容
     * @return headers map
     */
    private Map<String, String> extractCurlHeaders(String stageLog) {
        System.out.println("[JenkinsApiClient] Extracting curl headers from stage log...");
        
        Map<String, String> headers = new HashMap<>();
        
        // 匹配 -H 'key: value' 或 -H "key: value" 格式
        // 例如: -H 'x-mo-target-tenant: thailife'
        Pattern pattern = Pattern.compile("-H\\s+['\"]([^:]+):\\s*([^'\"]+)['\"]");
        Matcher matcher = pattern.matcher(stageLog);
        
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            headers.put(key, value);
            System.out.println("[JenkinsApiClient]   Header: " + key + " = " + value);
        }
        
        System.out.println("[JenkinsApiClient] Extracted " + headers.size() + " headers");
        return headers;
    }
    
    /**
     * 发送带自定义 headers 的 GET 请求
     * 
     * @param urlString URL
     * @param headers 自定义 headers
     * @return 响应内容
     * @throws IOException 如果请求失败
     */
    private String sendGetRequestWithHeaders(String urlString, Map<String, String> headers) throws IOException {
        logger.info("GET request with custom headers to: {}", urlString);
        System.out.println("[JenkinsApiClient] Sending GET request to: " + urlString);
        
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Accept-Charset", "UTF-8");
        
        // 添加自定义 headers
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
                System.out.println("[JenkinsApiClient]   Setting header: " + entry.getKey() + " = " + entry.getValue());
            }
        }
        
        int responseCode = conn.getResponseCode();
        logger.info("Response code: {}", responseCode);
        System.out.println("[JenkinsApiClient] Response code: " + responseCode);
        
        if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED || responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
            throw new IOException("Authentication failed. Response code: " + responseCode);
        }
        
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: " + responseCode);
        }
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
            response.append("\n");
        }
        in.close();
        
        return response.toString();
    }
    
    /**
     * 公开方法：从 Stage Log 中提取 Portal API URL
     * 供 UI 层调用以显示调试信息
     * 
     * @param stageLog Stage 日志内容
     * @return Portal API URL，如果未找到返回 null
     */
    public String extractPortalUrlPublic(String stageLog) {
        return extractPortalUrl(stageLog);
    }
    
    /**
     * 公开方法：从 Stage Log 中提取 curl 命令的 headers
     * 供 UI 层调用以显示调试信息
     * 
     * @param stageLog Stage 日志内容
     * @return headers map
     */
    public Map<String, String> extractCurlHeadersPublic(String stageLog) {
        return extractCurlHeaders(stageLog);
    }
    
    /**
     * 获取 Portal Build Output（带详细信息）
     * 使用已提取的 URL 和 headers 调用 Portal API
     * 
     * @param stageLog Stage 日志内容（用于错误处理）
     * @param portalUrl Portal API URL
     * @param headers HTTP headers
     * @return build_output 内容，如果失败返回错误信息
     */
    public String fetchPortalBuildOutputWithInfo(String stageLog, String portalUrl, Map<String, String> headers) {
        System.out.println("[JenkinsApiClient] fetchPortalBuildOutputWithInfo() called");
        
        try {
            // 调用 Portal API
            String jsonResponse = sendGetRequestWithHeaders(portalUrl, headers);
            System.out.println("[JenkinsApiClient] Portal API response length: " + jsonResponse.length());
            
            // 解析 JSON 响应，提取 build_output 字段
            JSONObject json = new JSONObject(jsonResponse);
            
            if (json.has("build_output")) {
                String buildOutput = json.getString("build_output");
                System.out.println("[JenkinsApiClient] Extracted build_output, length: " + buildOutput.length());
                
                // 检查是否包含转义序列（Unicode 或常见转义符）
                // 使用 indexOf 避免编译器将 \\u 解释为 Unicode 转义
                boolean hasEscapes = buildOutput.indexOf('\\') >= 0 && 
                                    (buildOutput.contains("\\n") || buildOutput.contains("\\r") || 
                                     buildOutput.contains("\\t") || buildOutput.indexOf("\\u") >= 0);
                
                if (hasEscapes) {
                    System.out.println("[JenkinsApiClient] Detected escape sequences, decoding...");
                    buildOutput = decodeUnicodeEscapes(buildOutput);
                    System.out.println("[JenkinsApiClient] After decoding, length: " + buildOutput.length());
                    
                    // 检查解码后是否包含换行符
                    int newlineCount = buildOutput.split("\n").length - 1;
                    System.out.println("[JenkinsApiClient] Newline count after decoding: " + newlineCount);
                }
                
                return buildOutput;
            } else {
                System.out.println("[JenkinsApiClient] No build_output field in response");
                return "No build_output field found in Portal API response.\n\nFull response:\n" + jsonResponse;
            }
            
        } catch (Exception e) {
            logger.error("Failed to fetch Portal build output: {}", e.getMessage(), e);
            System.err.println("[JenkinsApiClient] ERROR: Failed to fetch Portal build output: " + e.getMessage());
            e.printStackTrace();
            return "Failed to fetch Portal build output:\n" + e.getMessage();
        }
    }
    
    /**
     * 解码 Unicode 转义序列（例如 \\u4e2d\\u6587）
     * 
     * @param input 包含 Unicode 转义的字符串
     * @return 解码后的字符串
     */
    private String decodeUnicodeEscapes(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        
        StringBuilder result = new StringBuilder();
        int i = 0;
        
        while (i < input.length()) {
            if (i < input.length() - 1 && input.charAt(i) == '\\') {
                char nextChar = input.charAt(i + 1);
                
                // 处理常见的转义序列
                switch (nextChar) {
                    case 'n':  // 换行符
                        result.append('\n');
                        i += 2;
                        break;
                    case 'r':  // 回车符
                        result.append('\r');
                        i += 2;
                        break;
                    case 't':  // 制表符
                        result.append('\t');
                        i += 2;
                        break;
                    case '\\': // 反斜杠
                        result.append('\\');
                        i += 2;
                        break;
                    case '"':  // 双引号
                        result.append('"');
                        i += 2;
                        break;
                    case 'u':  // Unicode 转义 (backslash-u-XXXX format)
                        if (i < input.length() - 5) {
                            try {
                                String hex = input.substring(i + 2, i + 6);
                                int codePoint = Integer.parseInt(hex, 16);
                                result.append((char) codePoint);
                                i += 6;
                            } catch (NumberFormatException e) {
                                // 如果不是有效的十六进制，保留原样
                                result.append(input.charAt(i));
                                i++;
                            }
                        } else {
                            result.append(input.charAt(i));
                            i++;
                        }
                        break;
                    default:
                        // 其他情况保留原样
                        result.append(input.charAt(i));
                        i++;
                        break;
                }
            } else {
                result.append(input.charAt(i));
                i++;
            }
        }
        
        return result.toString();
    }
}
