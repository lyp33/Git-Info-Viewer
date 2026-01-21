package com.gitviewer;

import java.util.ArrayList;
import java.util.List;

/**
 * 租户配置数据模型
 * Represents tenant configuration data from Portal API
 */
public class TenantConfig {
    private String id;
    private String userName;
    private String defaultBranch;
    private List<String> branchList;
    
    /**
     * 构造函数 - 使用null-safe默认值
     */
    public TenantConfig() {
        this.id = "";
        this.userName = "";
        this.defaultBranch = "";
        this.branchList = new ArrayList<>();
    }
    
    // Getters and setters with null checks
    public String getId() {
        return id != null ? id : "";
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserName() {
        return userName != null ? userName : "";
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getDefaultBranch() {
        return defaultBranch != null ? defaultBranch : "";
    }
    
    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }
    
    public List<String> getBranchList() {
        return branchList != null ? branchList : new ArrayList<>();
    }
    
    public void setBranchList(List<String> branchList) {
        this.branchList = branchList != null ? branchList : new ArrayList<>();
    }
    
    @Override
    public String toString() {
        return "TenantConfig{" +
                "id='" + id + '\'' +
                ", userName='" + userName + '\'' +
                ", defaultBranch='" + defaultBranch + '\'' +
                ", branchList=" + branchList +
                '}';
    }
}
