package com.gitviewer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Jenkins 构建信息
 */
public class JenkinsBuild {
    private int number;
    private String result;  // SUCCESS, FAILURE, ABORTED, UNSTABLE, null (进行中)
    private long timestamp;
    private String url;
    private String triggeredBy;  // 触发用户
    private Map<String, String> parameters;  // 构建参数

    public JenkinsBuild() {
        this.parameters = new HashMap<>();
    }

    public JenkinsBuild(int number, String result, long timestamp, String url) {
        this.number = number;
        this.result = result;
        this.timestamp = timestamp;
        this.url = url;
        this.parameters = new HashMap<>();
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTriggeredBy() {
        return triggeredBy;
    }

    public void setTriggeredBy(String triggeredBy) {
        this.triggeredBy = triggeredBy;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters != null ? parameters : new HashMap<>();
    }

    public void addParameter(String name, String value) {
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }
        this.parameters.put(name, value);
    }

    /**
     * 判断构建是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(result);
    }

    /**
     * 判断构建是否失败
     */
    public boolean isFailure() {
        return "FAILURE".equals(result) || "ABORTED".equals(result) || "UNSTABLE".equals(result);
    }

    /**
     * 判断构建是否正在进行
     */
    public boolean isInProgress() {
        return result == null;
    }

    /**
     * 获取格式化的显示信息
     * 格式: "#154 - Failed - Jan 13, 2026 21:02 - by yunpeng.li - [VERSION: 2.3.1]"
     */
    public String getFormattedDisplay() {
        StringBuilder sb = new StringBuilder();
        
        // 构建编号
        sb.append("#").append(number);
        
        // 状态
        sb.append(" - ");
        if (result != null) {
            sb.append(result);
        } else {
            sb.append("IN_PROGRESS");
        }
        
        // 时间
        sb.append(" - ");
        sb.append(formatTimestamp(timestamp));
        
        // 触发用户
        if (triggeredBy != null && !triggeredBy.isEmpty()) {
            sb.append(" - by ").append(triggeredBy);
        }
        
        // 关键参数
        String keyParams = extractKeyParameters();
        if (!keyParams.isEmpty()) {
            sb.append(" - ").append(keyParams);
        }
        
        return sb.toString();
    }

    /**
     * 提取关键参数（SERVICE_NAME, VERSIONS, VERSION, BRANCH, TAG）
     * 优先显示 SERVICE_NAME 和 versions 参数（如果存在）
     */
    public String extractKeyParameters() {
        if (parameters == null || parameters.isEmpty()) {
            return "";
        }
        
        // 调试日志：输出所有参数
        System.out.println("[JenkinsBuild] extractKeyParameters for build #" + number);
        System.out.println("[JenkinsBuild] Total parameters: " + parameters.size());
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            System.out.println("[JenkinsBuild]   " + entry.getKey() + " = " + entry.getValue());
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 优先显示 SERVICE_NAME
        if (parameters.containsKey("SERVICE_NAME")) {
            String serviceName = parameters.get("SERVICE_NAME");
            if (serviceName != null && !serviceName.isEmpty()) {
                sb.append("SERVICE_NAME: ").append(serviceName);
                System.out.println("[JenkinsBuild] Added SERVICE_NAME: " + serviceName);
            }
        }
        
        // 显示 versions/VERSIONS 参数（完整版本信息）
        if (parameters.containsKey("versions") || parameters.containsKey("VERSIONS")) {
            String versions = parameters.containsKey("versions") ? parameters.get("versions") : parameters.get("VERSIONS");
            if (versions != null && !versions.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                // 截断过长的值
                if (versions.length() > 50) {
                    versions = versions.substring(0, 47) + "...";
                }
                sb.append("VERSIONS: ").append(versions);
                System.out.println("[JenkinsBuild] Added VERSIONS: " + versions);
            }
        }
        
        // 显示 VERSION 参数
        if (parameters.containsKey("VERSION")) {
            String version = parameters.get("VERSION");
            System.out.println("[JenkinsBuild] Found VERSION parameter: " + version);
            if (version != null && !version.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("VERSION: ").append(version);
                System.out.println("[JenkinsBuild] Added VERSION: " + version);
            } else {
                System.out.println("[JenkinsBuild] VERSION is null or empty");
            }
        } else {
            System.out.println("[JenkinsBuild] VERSION parameter not found");
        }
        
        // 显示 BRANCH 参数
        if (parameters.containsKey("BRANCH") || parameters.containsKey("branch")) {
            String branch = parameters.containsKey("BRANCH") ? parameters.get("BRANCH") : parameters.get("branch");
            if (branch != null && !branch.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append("BRANCH: ").append(branch);
                System.out.println("[JenkinsBuild] Added BRANCH: " + branch);
            }
        }
        
        // 如果已经有任何参数，返回
        if (sb.length() > 0) {
            String result = "[" + sb.toString() + "]";
            System.out.println("[JenkinsBuild] Final result: " + result);
            return result;
        }
        
        // 否则查找其他关键参数
        String[] keyNames = {"TAG", "version", "tag", "TENANT_NAME"};
        
        for (String key : keyNames) {
            if (parameters.containsKey(key)) {
                String value = parameters.get(key);
                if (value != null && !value.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    // 截断过长的值
                    if (value.length() > 30) {
                        value = value.substring(0, 27) + "...";
                    }
                    sb.append(key).append(": ").append(value);
                }
            }
        }
        
        if (sb.length() > 0) {
            return "[" + sb.toString() + "]";
        }
        
        return "";
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        return sdf.format(new Date(timestamp));
    }
}
