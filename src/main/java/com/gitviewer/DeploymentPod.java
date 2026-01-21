package com.gitviewer;

/**
 * 部署Pod数据模型
 * Deployment Pod data model
 */
public class DeploymentPod {
    private String name;
    private String namespace;
    private String creationTimestamp;
    private String app;
    private String realStatus;
    
    /**
     * 构造函数
     */
    public DeploymentPod() {
    }
    
    /**
     * 构造函数
     * 
     * @param name Pod名称
     * @param namespace 命名空间
     * @param creationTimestamp 创建时间
     * @param app 应用名称
     * @param realStatus 实际状态
     */
    public DeploymentPod(String name, String namespace, String creationTimestamp, String app, String realStatus) {
        this.name = name;
        this.namespace = namespace;
        this.creationTimestamp = creationTimestamp;
        this.app = app;
        this.realStatus = realStatus;
    }
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getNamespace() {
        return namespace;
    }
    
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
    
    public String getCreationTimestamp() {
        return creationTimestamp;
    }
    
    public void setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }
    
    public String getApp() {
        return app;
    }
    
    public void setApp(String app) {
        this.app = app;
    }
    
    public String getRealStatus() {
        return realStatus;
    }
    
    public void setRealStatus(String realStatus) {
        this.realStatus = realStatus;
    }
    
    @Override
    public String toString() {
        return "DeploymentPod{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                ", creationTimestamp='" + creationTimestamp + '\'' +
                ", app='" + app + '\'' +
                ", realStatus='" + realStatus + '\'' +
                '}';
    }
}
