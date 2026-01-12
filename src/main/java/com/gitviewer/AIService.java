package com.gitviewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 服务类
 * 用于与 AI API 交互
 */
public class AIService {

    private String apiUrl;
    private String apiKey;
    private String model;

    public AIService(String apiUrl, String apiKey, String model) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * 发送聊天请求到 AI API
     */
    public String chat(List<ChatMessage> messages) throws IOException {
        System.out.println("\n---------- AI Service Request ----------");
        System.out.println("[AI Service] API URL: " + apiUrl);
        System.out.println("[AI Service] Model: " + model);
        System.out.println("[AI Service] Messages count: " + messages.size());

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            conn.setDoOutput(true);

            // 设置请求头
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);

            // 构建请求体
            String requestBody = buildRequestBody(messages);
            System.out.println("[AI Service] Request body length: " + requestBody.length() + " chars");
            System.out.println("[AI Service] Request body: " + requestBody);

            // 发送请求
            System.out.println("[AI Service] Sending request...");
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("[AI Service] Response Code: " + responseCode);

            if (responseCode != 200) {
                String errorMsg = readErrorStream(conn);
                System.err.println("[AI Service] ERROR Response: " + errorMsg);
                throw new IOException("AI API error (" + responseCode + "): " + errorMsg);
            }

            // 读取响应
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                String responseBody = response.toString();
                System.out.println("[AI Service] Response body length: " + responseBody.length() + " chars");
                System.out.println("[AI Service] Response body: " + responseBody);
                
                String parsedData = parseResponse(responseBody);
                System.out.println("[AI Service] Parsed data: " + parsedData);
                System.out.println("---------- AI Service Response Complete ----------\n");
                
                return parsedData;
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
     * 构建请求体（自定义格式）
     */
    private String buildRequestBody(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        
        // 获取最后一条用户消息作为 query
        String query = "";
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).role)) {
                query = messages.get(i).content;
                break;
            }
        }
        
        sb.append("\"query\":\"").append(escapeJson(query)).append("\",");
        sb.append("\"messages\":[");

        // **修复：包含所有消息，包括 system 消息**
        boolean first = true;
        for (ChatMessage msg : messages) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            
            sb.append("{");
            sb.append("\"role\":\"").append(escapeJson(msg.role)).append("\",");
            sb.append("\"content\":\"").append(escapeJson(msg.content)).append("\"");
            sb.append("}");
        }

        sb.append("],");
        sb.append("\"temperature\":0.3,");
        sb.append("\"llm_code\":\"").append(escapeJson(model)).append("\",");
        sb.append("\"stream\":\"false\"");
        sb.append("}");

        return sb.toString();
    }

    /**
     * 解析响应（自定义格式）
     */
    private String parseResponse(String responseBody) {
        // 解析 JSON，提取 data 字段
        try {
            int dataStart = responseBody.indexOf("\"data\"");
            if (dataStart == -1) {
                return "Error: Unable to parse AI response";
            }

            int valueStart = responseBody.indexOf(":", dataStart) + 1;
            valueStart = responseBody.indexOf("\"", valueStart) + 1;
            
            // 找到对应的结束引号（考虑转义）
            int valueEnd = valueStart;
            boolean escaped = false;
            while (valueEnd < responseBody.length()) {
                char c = responseBody.charAt(valueEnd);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break;
                }
                valueEnd++;
            }

            if (valueEnd >= responseBody.length()) {
                return "Error: Unable to parse AI response";
            }

            String content = responseBody.substring(valueStart, valueEnd);
            return unescapeJson(content);
        } catch (Exception e) {
            System.err.println("[AI Service] Parse error: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 读取错误流
     */
    private String readErrorStream(HttpURLConnection conn) {
        try {
            java.io.InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                    StringBuilder error = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        error.append(line);
                    }
                    return error.toString();
                }
            }
        } catch (IOException e) {
            // Ignore
        }
        return "Unknown error";
    }

    /**
     * JSON 字符串转义
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * JSON 字符串反转义
     */
    private String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    /**
     * 聊天消息类
     */
    public static class ChatMessage {
        public String role;
        public String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
