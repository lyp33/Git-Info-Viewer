package com.gitviewer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock Jenkins Server for testing
 * 模拟 Jenkins 服务器用于本地测试
 */
public class MockJenkinsServer {
    private HttpServer server;
    private int port;
    private static final int DELAY_SECONDS = 10; // 延迟 10 秒，用于测试 Loading 对话框
    
    public MockJenkinsServer(int port) {
        this.port = port;
    }
    
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // 注册各种 API 端点
        server.createContext("/", new RootHandler());
        server.createContext("/job/", new JobHandler());
        server.createContext("/api/json", new ApiHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("========================================");
        System.out.println("Mock Jenkins Server Started!");
        System.out.println("URL: http://localhost:" + port);
        System.out.println("========================================");
        System.out.println();
        System.out.println("测试数据结构:");
        System.out.println("  gemini/");
        System.out.println("    ├── Manual-Build/");
        System.out.println("    │   ├── all-in-one-auto-CI");
        System.out.println("    │   └── backend-deploy");
        System.out.println("    └── Test-Job/");
        System.out.println("        ├── backend-service");
        System.out.println("        └── frontend-service");
        System.out.println();
        System.out.println("在应用中配置:");
        System.out.println("  Jenkins URL: http://localhost:" + port);
        System.out.println("  Username: test");
        System.out.println("  API Token: test123");
        System.out.println();
        System.out.println("⚠️  注意：所有请求将延迟 " + DELAY_SECONDS + " 秒返回");
        System.out.println("   这样可以看到 Loading 对话框和进度条");
        System.out.println();
        System.out.println("按 Ctrl+C 停止服务器");
        System.out.println("========================================");
    }
    
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Mock Jenkins Server Stopped");
        }
    }
    
    /**
     * 根路径处理器
     */
    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "<html><body><h1>Mock Jenkins Server</h1><p>Server is running</p></body></html>";
            sendResponse(exchange, 200, response);
        }
    }
    
    /**
     * API 处理器
     */
    static class ApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            
            System.out.println("API Request: " + path + (query != null ? "?" + query : ""));
            System.out.println("  延迟 " + DELAY_SECONDS + " 秒后返回...");
            
            // 添加延迟，让用户能看到 Loading 对话框
            try {
                Thread.sleep(DELAY_SECONDS * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 根据路径返回不同的响应
            JSONObject response = new JSONObject();
            response.put("_class", "hudson.model.Hudson");
            response.put("mode", "NORMAL");
            
            System.out.println("  返回响应");
            sendJsonResponse(exchange, 200, response.toString());
        }
    }
    
    /**
     * Job 处理器 - 处理所有 /job/ 开头的请求
     */
    static class JobHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();
            
            System.out.println("Job Request: " + path + (query != null ? "?" + query : ""));
            
            // 处理不同类型的请求
            if (path.endsWith("/api/json")) {
                handleApiJson(exchange, path, query);
            } else if (path.contains("/wfapi/describe")) {
                handleWfApiDescribe(exchange, path);
            } else if (path.contains("/wfapi/log")) {
                handleWfApiLog(exchange, path);
            } else if (path.contains("/consoleText")) {
                handleConsoleText(exchange, path);
            } else {
                sendResponse(exchange, 404, "Not Found");
            }
        }
        
        private void handleApiJson(HttpExchange exchange, String path, String query) throws IOException {
            // 添加延迟，让用户能看到 Loading 对话框
            System.out.println("  延迟 " + DELAY_SECONDS + " 秒后返回...");
            try {
                Thread.sleep(DELAY_SECONDS * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 移除 /api/json 后缀和末尾斜杠
            String jobPath = path.replace("/api/json", "").replaceAll("/+$", "");
            
            JSONObject response = new JSONObject();
            
            // 根据路径返回不同的数据
            if (jobPath.equals("/job/gemini")) {
                // 返回 gemini 文件夹的子项
                response = createFolderResponse("gemini", jobPath, new String[][]{
                    {"Manual-Build", "com.cloudbees.hudson.plugins.folder.Folder"},
                    {"Test-Job", "com.cloudbees.hudson.plugins.folder.Folder"}
                });
            } else if (jobPath.equals("/job/gemini/job/Manual-Build")) {
                // 返回 Manual-Build 文件夹的子项
                response = createFolderResponse("Manual-Build", jobPath, new String[][]{
                    {"all-in-one-auto-CI", "hudson.model.FreeStyleProject"},
                    {"backend-deploy", "hudson.model.FreeStyleProject"}
                });
            } else if (jobPath.equals("/job/gemini/job/Test-Job")) {
                // 返回 Test-Job 文件夹的子项
                response = createFolderResponse("Test-Job", jobPath, new String[][]{
                    {"backend-service", "hudson.model.FreeStyleProject"},
                    {"frontend-service", "hudson.model.FreeStyleProject"}
                });
            } else if (jobPath.contains("/job/")) {
                // 返回具体 Job 的详情（包括 build history）
                String jobName = jobPath.substring(jobPath.lastIndexOf("/") + 1);
                response = createJobResponse(jobName, jobPath);
            } else {
                response.put("error", "Unknown path: " + jobPath);
            }
            
            System.out.println("  返回响应: " + jobPath);
            sendJsonResponse(exchange, 200, response.toString());
        }
        
        private JSONObject createFolderResponse(String name, String parentPath, String[][] jobs) {
            JSONObject response = new JSONObject();
            response.put("_class", "com.cloudbees.hudson.plugins.folder.Folder");
            response.put("name", name);
            
            JSONArray jobsArray = new JSONArray();
            for (String[] job : jobs) {
                JSONObject jobObj = new JSONObject();
                jobObj.put("name", job[0]);
                // 构建完整的 URL - 基于父路径
                String jobUrl = "http://localhost:8888" + parentPath + "/job/" + job[0] + "/";
                jobObj.put("url", jobUrl);
                jobObj.put("_class", job[1]);
                jobsArray.put(jobObj);
            }
            response.put("jobs", jobsArray);
            
            return response;
        }
        
        private JSONObject createJobResponse(String name, String fullPath) {
            JSONObject response = new JSONObject();
            response.put("_class", "hudson.model.FreeStyleProject");
            response.put("name", name);
            response.put("fullName", fullPath.replace("/job/", "").replace("/", " » "));
            response.put("url", "http://localhost:8888" + fullPath + "/");
            response.put("description", "Mock job: " + name);
            response.put("buildable", true);
            response.put("color", "blue");
            
            // 添加构建历史
            JSONArray builds = new JSONArray();
            for (int i = 1; i <= 5; i++) {
                JSONObject build = new JSONObject();
                build.put("number", i);
                build.put("url", "http://localhost:8888" + fullPath + "/" + i + "/");
                build.put("result", i % 2 == 0 ? "SUCCESS" : "FAILURE");
                build.put("timestamp", System.currentTimeMillis() - (i * 3600000));
                
                // 添加 actions
                JSONArray actions = new JSONArray();
                
                // 添加触发用户信息
                JSONObject causeAction = new JSONObject();
                JSONArray causes = new JSONArray();
                JSONObject cause = new JSONObject();
                cause.put("userId", "testuser");
                cause.put("userName", "Test User");
                causes.put(cause);
                causeAction.put("causes", causes);
                actions.put(causeAction);
                
                // 添加参数信息
                JSONObject paramAction = new JSONObject();
                JSONArray parameters = new JSONArray();
                JSONObject param1 = new JSONObject();
                param1.put("name", "BRANCH");
                param1.put("value", "master");
                parameters.put(param1);
                paramAction.put("parameters", parameters);
                actions.put(paramAction);
                
                build.put("actions", actions);
                builds.put(build);
            }
            response.put("builds", builds);
            
            // 添加参数定义
            JSONArray property = new JSONArray();
            JSONObject paramProperty = new JSONObject();
            JSONArray paramDefs = new JSONArray();
            
            JSONObject param1 = new JSONObject();
            param1.put("name", "BRANCH");
            param1.put("_class", "hudson.model.StringParameterDefinition");
            param1.put("description", "Git branch to build");
            JSONObject defaultValue1 = new JSONObject();
            defaultValue1.put("value", "master");
            param1.put("defaultParameterValue", defaultValue1);
            paramDefs.put(param1);
            
            JSONObject param2 = new JSONObject();
            param2.put("name", "ENVIRONMENT");
            param2.put("_class", "hudson.model.ChoiceParameterDefinition");
            param2.put("description", "Target environment");
            JSONArray choices = new JSONArray();
            choices.put("dev");
            choices.put("test");
            choices.put("prod");
            param2.put("choices", choices);
            paramDefs.put(param2);
            
            paramProperty.put("parameterDefinitions", paramDefs);
            property.put(paramProperty);
            response.put("property", property);
            
            return response;
        }
        
        /**
         * 处理 wfapi/describe 请求 - 返回 Pipeline Stages 信息
         */
        private void handleWfApiDescribe(HttpExchange exchange, String path) throws IOException {
            System.out.println("  延迟 " + DELAY_SECONDS + " 秒后返回...");
            try {
                Thread.sleep(DELAY_SECONDS * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 创建模拟的 stages 数据
            JSONObject response = new JSONObject();
            response.put("_class", "org.jenkinsci.plugins.workflow.job.WorkflowRun");
            response.put("id", "243");
            response.put("name", "#243");
            response.put("status", "SUCCESS");
            response.put("durationMillis", 315000); // 5分15秒
            
            // 创建 stages 数组
            JSONArray stages = new JSONArray();
            
            // Stage 1: gemini-pa-bs-parent
            JSONObject stage1 = new JSONObject();
            stage1.put("id", "6");
            stage1.put("name", "gemini-pa-bs-parent");
            stage1.put("status", "SUCCESS");
            stage1.put("durationMillis", 39000); // 39秒
            stage1.put("startTimeMillis", System.currentTimeMillis() - 315000);
            stages.put(stage1);
            
            // Stage 2: bff-parent
            JSONObject stage2 = new JSONObject();
            stage2.put("id", "11");
            stage2.put("name", "bff-parent");
            stage2.put("status", "SUCCESS");
            stage2.put("durationMillis", 55000); // 55秒
            stage2.put("startTimeMillis", System.currentTimeMillis() - 276000);
            stages.put(stage2);
            
            // Stage 3: common-bff
            JSONObject stage3 = new JSONObject();
            stage3.put("id", "16");
            stage3.put("name", "common-bff");
            stage3.put("status", "SUCCESS");
            stage3.put("durationMillis", 130000); // 2分10秒
            stage3.put("startTimeMillis", System.currentTimeMillis() - 221000);
            stages.put(stage3);
            
            // Stage 4: pa-bs
            JSONObject stage4 = new JSONObject();
            stage4.put("id", "39");
            stage4.put("name", "pa-bs");
            stage4.put("status", "SUCCESS");
            stage4.put("durationMillis", 154000); // 2分34秒
            stage4.put("startTimeMillis", System.currentTimeMillis() - 91000);
            stages.put(stage4);
            
            // Stage 5: claim-bs
            JSONObject stage5 = new JSONObject();
            stage5.put("id", "41");
            stage5.put("name", "claim-bs");
            stage5.put("status", "SUCCESS");
            stage5.put("durationMillis", 159000); // 2分39秒
            stage5.put("startTimeMillis", System.currentTimeMillis() - 159000);
            stages.put(stage5);
            
            response.put("stages", stages);
            
            System.out.println("  返回 " + stages.length() + " 个 stages");
            sendJsonResponse(exchange, 200, response.toString());
        }
        
        /**
         * 处理 wfapi/log 请求 - 返回 Stage 日志
         */
        private void handleWfApiLog(HttpExchange exchange, String path) throws IOException {
            System.out.println("  延迟 " + DELAY_SECONDS + " 秒后返回...");
            try {
                Thread.sleep(DELAY_SECONDS * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 从路径中提取 stage ID
            String stageId = extractStageId(path);
            
            // 根据 stage ID 返回不同的日志
            String logContent = generateStageLog(stageId);
            
            System.out.println("  返回 stage " + stageId + " 的日志");
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            sendResponse(exchange, 200, logContent);
        }
        
        /**
         * 处理 consoleText 请求 - 返回完整的 Console Log
         */
        private void handleConsoleText(HttpExchange exchange, String path) throws IOException {
            System.out.println("  延迟 " + DELAY_SECONDS + " 秒后返回...");
            try {
                Thread.sleep(DELAY_SECONDS * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            // 生成完整的 console log
            StringBuilder log = new StringBuilder();
            log.append("[13:52:25.508] ===== Build Started =====\n");
            log.append("[13:52:25.508] Build #243 - SUCCESS\n");
            log.append("[13:52:25.508] Triggered by: dttl.kthoo\n");
            log.append("[13:52:25.508] Parameters: [versions: 24.08_thailife_devsdk_v0.056]\n");
            log.append("[13:52:25.508] \n");
            log.append("[13:52:25.508] Module 12: m-bs (ID: 8, Status: SUCCESS)\n");
            log.append("[13:52:25.508] Module 13: m-bs-parent (ID: 3, Status: SUCCESS)\n");
            log.append("[13:52:25.508] Module 14: remove-json (ID: 59, Status: SUCCESS)\n");
            log.append("[13:52:25.509] Cannot load module log: missing API client or module ID\n");
            log.append("[13:52:25.510] Auto-selected first module\n");
            log.append("[13:52:25.510] ===\n");
            log.append("[13:52:25.510] ==== Displaying Modules ====\n");
            log.append("[13:52:25.510] Number of modules: 14\n");
            log.append("[13:52:25.510] Module 1: gemini-pa-bs-parent (ID: 6, Status: SUCCESS)\n");
            log.append("[13:52:25.510] Module 2: bff-parent (ID: 11, Status: SUCCESS)\n");
            log.append("[13:52:25.510] Module 3: common-bff (ID: 16, Status: SUCCESS)\n");
            log.append("[13:52:25.510] Module 4: pa-bs (ID: 39, Status: SUCCESS)\n");
            log.append("[13:52:25.510] Module 5: claim-bs (ID: 41, Status: SUCCESS)\n");
            log.append("[13:52:25.510] \n");
            log.append("[13:52:25.510] ===== Compilation Phase =====\n");
            log.append("[13:52:26.123] Compiling module: gemini-pa-bs-parent\n");
            log.append("[13:52:28.456] [INFO] Building gemini-pa-bs-parent 1.0.0\n");
            log.append("[13:52:28.789] [INFO] Compiling 45 source files to target/classes\n");
            log.append("[13:52:32.012] [INFO] BUILD SUCCESS\n");
            log.append("[13:52:32.345] \n");
            log.append("[13:52:32.678] Compiling module: bff-parent\n");
            log.append("[13:52:35.901] [INFO] Building bff-parent 2.1.0\n");
            log.append("[13:52:36.234] [INFO] Compiling 67 source files to target/classes\n");
            log.append("[13:52:41.567] [INFO] BUILD SUCCESS\n");
            log.append("[13:52:41.890] \n");
            log.append("[13:52:42.123] Compiling module: common-bff\n");
            log.append("[13:52:45.456] [INFO] Building common-bff 1.5.2\n");
            log.append("[13:52:45.789] [INFO] Compiling 123 source files to target/classes\n");
            log.append("[13:52:58.012] [INFO] Running tests...\n");
            log.append("[13:53:05.345] [INFO] Tests passed: 89/89\n");
            log.append("[13:53:05.678] [INFO] BUILD SUCCESS\n");
            log.append("[13:53:05.901] \n");
            log.append("[13:53:06.234] Compiling module: pa-bs\n");
            log.append("[13:53:09.567] [INFO] Building pa-bs 3.2.1\n");
            log.append("[13:53:09.890] [INFO] Compiling 156 source files to target/classes\n");
            log.append("[13:53:25.123] [INFO] Running tests...\n");
            log.append("[13:53:32.456] [INFO] Tests passed: 124/124\n");
            log.append("[13:53:32.789] [INFO] BUILD SUCCESS\n");
            log.append("[13:53:33.012] \n");
            log.append("[13:53:33.345] Compiling module: claim-bs\n");
            log.append("[13:53:36.678] [INFO] Building claim-bs 2.8.0\n");
            log.append("[13:53:36.901] [INFO] Compiling 98 source files to target/classes\n");
            log.append("[13:53:48.234] [INFO] Running tests...\n");
            log.append("[13:53:55.567] [INFO] Tests passed: 76/76\n");
            log.append("[13:53:55.890] [INFO] BUILD SUCCESS\n");
            log.append("[13:53:56.123] \n");
            log.append("[13:53:56.456] ===== Deployment Phase =====\n");
            log.append("[13:53:56.789] Deploying artifacts to repository...\n");
            log.append("[13:53:59.012] [INFO] Uploaded: gemini-pa-bs-parent-1.0.0.jar\n");
            log.append("[13:54:01.345] [INFO] Uploaded: bff-parent-2.1.0.jar\n");
            log.append("[13:54:03.678] [INFO] Uploaded: common-bff-1.5.2.jar\n");
            log.append("[13:54:05.901] [INFO] Uploaded: pa-bs-3.2.1.jar\n");
            log.append("[13:54:08.234] [INFO] Uploaded: claim-bs-2.8.0.jar\n");
            log.append("[13:54:08.567] \n");
            log.append("[13:54:08.890] ===== Build Complete =====\n");
            log.append("[13:54:09.123] Total time: 5m 15s\n");
            log.append("[13:54:09.456] Status: SUCCESS\n");
            log.append("[13:54:09.789] All modules built successfully!\n");
            
            System.out.println("  返回完整 console log");
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
            sendResponse(exchange, 200, log.toString());
        }
        
        /**
         * 从路径中提取 stage ID
         */
        private String extractStageId(String path) {
            // 路径格式: /job/.../123/execution/node/6/wfapi/log
            String[] parts = path.split("/");
            for (int i = 0; i < parts.length - 2; i++) {
                if ("node".equals(parts[i]) && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
            return "unknown";
        }
        
        /**
         * 生成 stage 日志
         */
        private String generateStageLog(String stageId) {
            StringBuilder log = new StringBuilder();
            
            switch (stageId) {
                case "6": // gemini-pa-bs-parent
                    log.append("[13:52:26.123] ===== Stage: gemini-pa-bs-parent =====\n");
                    log.append("[13:52:26.456] Starting compilation...\n");
                    log.append("[13:52:28.456] [INFO] Building gemini-pa-bs-parent 1.0.0\n");
                    log.append("[13:52:28.789] [INFO] Compiling 45 source files to target/classes\n");
                    log.append("[13:52:30.012] [INFO] Compilation successful\n");
                    log.append("[13:52:32.012] [INFO] BUILD SUCCESS\n");
                    log.append("[13:52:32.345] Duration: 39s\n");
                    break;
                    
                case "11": // bff-parent
                    log.append("[13:52:32.678] ===== Stage: bff-parent =====\n");
                    log.append("[13:52:33.901] Starting compilation...\n");
                    log.append("[13:52:35.901] [INFO] Building bff-parent 2.1.0\n");
                    log.append("[13:52:36.234] [INFO] Compiling 67 source files to target/classes\n");
                    log.append("[13:52:39.567] [INFO] Compilation successful\n");
                    log.append("[13:52:41.567] [INFO] BUILD SUCCESS\n");
                    log.append("[13:52:41.890] Duration: 55s\n");
                    break;
                    
                case "16": // common-bff
                    log.append("[13:52:42.123] ===== Stage: common-bff =====\n");
                    log.append("[13:52:43.456] Starting compilation...\n");
                    log.append("[13:52:45.456] [INFO] Building common-bff 1.5.2\n");
                    log.append("[13:52:45.789] [INFO] Compiling 123 source files to target/classes\n");
                    log.append("[13:52:58.012] [INFO] Running tests...\n");
                    log.append("[13:53:00.345] [INFO] Test: testUserAuthentication - PASSED\n");
                    log.append("[13:53:01.678] [INFO] Test: testDataValidation - PASSED\n");
                    log.append("[13:53:03.901] [INFO] Test: testApiEndpoints - PASSED\n");
                    log.append("[13:53:05.345] [INFO] Tests passed: 89/89\n");
                    log.append("[13:53:05.678] [INFO] BUILD SUCCESS\n");
                    log.append("[13:53:05.901] Duration: 2m 10s\n");
                    break;
                    
                case "39": // pa-bs
                    log.append("[13:53:06.234] ===== Stage: pa-bs =====\n");
                    log.append("[13:53:07.567] Starting compilation...\n");
                    log.append("[13:53:09.567] [INFO] Building pa-bs 3.2.1\n");
                    log.append("[13:53:09.890] [INFO] Compiling 156 source files to target/classes\n");
                    log.append("[13:53:25.123] [INFO] Running tests...\n");
                    log.append("[13:53:27.456] [INFO] Test: testPolicyCreation - PASSED\n");
                    log.append("[13:53:28.789] [INFO] Test: testPolicyUpdate - PASSED\n");
                    log.append("[13:53:30.012] [INFO] Test: testPolicyValidation - PASSED\n");
                    log.append("[13:53:32.456] [INFO] Tests passed: 124/124\n");
                    log.append("[13:53:32.789] [INFO] BUILD SUCCESS\n");
                    log.append("[13:53:33.012] Duration: 2m 34s\n");
                    break;
                    
                case "41": // claim-bs
                    log.append("[13:53:33.345] ===== Stage: claim-bs =====\n");
                    log.append("[13:53:34.678] Starting compilation...\n");
                    log.append("[13:53:36.678] [INFO] Building claim-bs 2.8.0\n");
                    log.append("[13:53:36.901] [INFO] Compiling 98 source files to target/classes\n");
                    log.append("[13:53:48.234] [INFO] Running tests...\n");
                    log.append("[13:53:50.567] [INFO] Test: testClaimSubmission - PASSED\n");
                    log.append("[13:53:51.890] [INFO] Test: testClaimProcessing - PASSED\n");
                    log.append("[13:53:53.123] [INFO] Test: testClaimApproval - PASSED\n");
                    log.append("[13:53:55.567] [INFO] Tests passed: 76/76\n");
                    log.append("[13:53:55.890] [INFO] BUILD SUCCESS\n");
                    log.append("[13:53:56.123] Duration: 2m 39s\n");
                    break;
                    
                default:
                    log.append("[13:52:25.508] ===== Stage: ").append(stageId).append(" =====\n");
                    log.append("[13:52:25.508] [INFO] Stage execution started\n");
                    log.append("[13:52:26.508] [INFO] Processing...\n");
                    log.append("[13:52:27.508] [INFO] Stage completed successfully\n");
                    break;
            }
            
            return log.toString();
        }
    }
    
    /**
     * 发送响应
     */
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
    
    /**
     * 发送 JSON 响应
     */
    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        sendResponse(exchange, statusCode, json);
    }
    
    /**
     * 主函数 - 启动服务器
     */
    public static void main(String[] args) {
        int port = 8888;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default: 8888");
            }
        }
        
        MockJenkinsServer server = new MockJenkinsServer(port);
        try {
            server.start();
            
            // 保持服务器运行
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
