package com.gitviewer;

import java.io.Serializable;
import java.util.Objects;

/**
 * Jenkins 收藏任务数据模型
 * 用于存储用户收藏的 Jenkins Job 信息
 */
public class FavoriteJob implements Serializable {
    private static final long serialVersionUID = 2L;  // 增加版本号，因为添加了新字段
    
    private String jobPath;        // 完整路径: "gemini/job/Manual-Build/..."
    private String displayName;    // 显示名称: "all-in-one-auto-CI"
    private String jobUrl;         // Jenkins URL
    private int order;             // 排序顺序
    private String alias;          // 用户自定义别名
    
    public FavoriteJob() {
    }
    
    public FavoriteJob(String jobPath, String displayName, String jobUrl, int order) {
        this.jobPath = jobPath;
        this.displayName = displayName;
        this.jobUrl = jobUrl;
        this.order = order;
    }
    
    public String getJobPath() {
        return jobPath;
    }
    
    public void setJobPath(String jobPath) {
        this.jobPath = jobPath;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public String getJobUrl() {
        return jobUrl;
    }
    
    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }
    
    public int getOrder() {
        return order;
    }
    
    public void setOrder(int order) {
        this.order = order;
    }
    
    public String getAlias() {
        return alias;
    }
    
    public void setAlias(String alias) {
        this.alias = alias;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FavoriteJob that = (FavoriteJob) o;
        return Objects.equals(jobPath, that.jobPath);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(jobPath);
    }
    
    @Override
    public String toString() {
        return displayName + " (" + jobPath + ")";
    }
}
