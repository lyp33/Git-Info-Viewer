package com.gitviewer;

import java.util.ArrayList;
import java.util.List;

/**
 * App构建结果数据模型
 * Represents app-based query result from Portal API
 */
public class AppBuildResult {
    private List<BuildResult> data;
    private int total;
    
    /**
     * 构造函数 - 使用null-safe默认值
     */
    public AppBuildResult() {
        this.data = new ArrayList<>();
        this.total = 0;
    }
    
    // Getters and setters with null checks
    public List<BuildResult> getData() {
        return data != null ? data : new ArrayList<>();
    }
    
    public void setData(List<BuildResult> data) {
        this.data = data != null ? data : new ArrayList<>();
    }
    
    public int getTotal() {
        return total;
    }
    
    public void setTotal(int total) {
        this.total = total;
    }
    
    @Override
    public String toString() {
        return "AppBuildResult{" +
                "data=" + (data != null ? data.size() + " items" : "null") +
                ", total=" + total +
                '}';
    }
}
