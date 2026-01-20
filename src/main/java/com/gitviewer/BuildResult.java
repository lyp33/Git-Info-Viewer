package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 构建结果数据模型
 * Represents a single build record from Portal API
 */
public class BuildResult {
    private static final Logger logger = LoggerFactory.getLogger(BuildResult.class);
    
    private String id;  // Build record ID from Portal API
    private String appName;
    private String imageName;
    private String buildStatus;
    private String createTime;
    private String version;
    private String gitBranch;
    
    /**
     * 构造函数 - 使用null-safe默认值
     */
    public BuildResult() {
        this.id = "";
        this.appName = "";
        this.imageName = "";
        this.buildStatus = "Unknown";
        this.createTime = "";
        this.version = "";
        this.gitBranch = "";
    }
    
    // Getters with null checks
    public String getId() {
        return id != null ? id : "";
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getAppName() {
        return appName != null ? appName : "";
    }
    
    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    public String getImageName() {
        return imageName != null ? imageName : "";
    }
    
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
    
    public String getBuildStatus() {
        return buildStatus != null ? buildStatus : "Unknown";
    }
    
    public void setBuildStatus(String buildStatus) {
        this.buildStatus = buildStatus;
    }
    
    public String getCreateTime() {
        return createTime != null ? createTime : "";
    }
    
    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
    
    public String getVersion() {
        return version != null ? version : "";
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getGitBranch() {
        return gitBranch != null ? gitBranch : "";
    }
    
    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }
    
    /**
     * 格式化创建时间用于显示
     * 将 ISO 8601 格式转换为可读格式
     * 例如: "2026-01-20T11:31:28.804Z" -> "2026-01-20 11:31:28"
     * 
     * @return 格式化后的时间字符串，如果解析失败则返回原始字符串
     */
    public String getFormattedCreateTime() {
        if (createTime == null || createTime.isEmpty()) {
            return "";
        }
        
        try {
            // 解析 ISO 8601 格式
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(createTime);
            
            // 格式化为显示格式
            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return displayFormat.format(date);
        } catch (ParseException e) {
            logger.warn("Failed to parse create time: {}", createTime);
            return createTime;  // 解析失败时返回原始值
        }
    }
    
    @Override
    public String toString() {
        return "BuildResult{" +
                "id='" + id + '\'' +
                ", appName='" + appName + '\'' +
                ", imageName='" + imageName + '\'' +
                ", buildStatus='" + buildStatus + '\'' +
                ", createTime='" + createTime + '\'' +
                ", version='" + version + '\'' +
                ", gitBranch='" + gitBranch + '\'' +
                '}';
    }
}
