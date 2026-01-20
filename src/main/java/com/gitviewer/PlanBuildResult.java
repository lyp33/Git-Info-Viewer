package com.gitviewer;

import java.util.ArrayList;
import java.util.List;

/**
 * Plan构建结果数据模型
 * Represents plan-based query result from Portal API
 */
public class PlanBuildResult {
    private String title;
    private List<BuildResult> appBuildHistories;
    
    /**
     * 构造函数 - 使用null-safe默认值
     */
    public PlanBuildResult() {
        this.title = "";
        this.appBuildHistories = new ArrayList<>();
    }
    
    // Getters and setters with null checks
    public String getTitle() {
        return title != null ? title : "";
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public List<BuildResult> getAppBuildHistories() {
        return appBuildHistories != null ? appBuildHistories : new ArrayList<>();
    }
    
    public void setAppBuildHistories(List<BuildResult> appBuildHistories) {
        this.appBuildHistories = appBuildHistories != null ? appBuildHistories : new ArrayList<>();
    }
    
    @Override
    public String toString() {
        return "PlanBuildResult{" +
                "title='" + title + '\'' +
                ", appBuildHistories=" + (appBuildHistories != null ? appBuildHistories.size() + " items" : "null") +
                '}';
    }
}
