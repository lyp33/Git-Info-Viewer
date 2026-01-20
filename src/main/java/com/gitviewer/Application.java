package com.gitviewer;

/**
 * 应用程序数据模型
 * Represents an application in the tenant from Portal API
 */
public class Application {
    private String id;
    private String appName;
    private String userName;
    
    /**
     * 构造函数 - 使用null-safe默认值
     */
    public Application() {
        this.id = "";
        this.appName = "";
        this.userName = "";
    }
    
    // Getters and setters with null checks
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
    
    public String getUserName() {
        return userName != null ? userName : "";
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    @Override
    public String toString() {
        return "Application{" +
                "id='" + id + '\'' +
                ", appName='" + appName + '\'' +
                ", userName='" + userName + '\'' +
                '}';
    }
}
