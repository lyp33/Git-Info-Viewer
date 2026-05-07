package com.gitviewer;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏夹分组数据模型
 * Favorite group data model for organizing applications
 */
public class FavoriteGroup {
    private String name;
    private List<String> appNames;
    private boolean expanded;
    private boolean selected;  // group 级别的选中状态（独立于 UI checkbox）
    
    /**
     * 构造函数
     * 
     * @param name 分组名称
     */
    public FavoriteGroup(String name) {
        this.name = name;
        this.appNames = new ArrayList<>();
        this.expanded = true;  // 默认展开
    }
    
    /**
     * 构造函数（用于反序列化）
     * 
     * @param name 分组名称
     * @param appNames 应用名称列表
     * @param expanded 是否展开
     */
    public FavoriteGroup(String name, List<String> appNames, boolean expanded) {
        this.name = name;
        this.appNames = appNames != null ? new ArrayList<>(appNames) : new ArrayList<>();
        this.expanded = expanded;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public List<String> getAppNames() {
        return appNames;
    }
    
    public void setAppNames(List<String> appNames) {
        this.appNames = appNames != null ? new ArrayList<>(appNames) : new ArrayList<>();
    }
    
    public boolean isExpanded() {
        return expanded;
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    /**
     * 添加应用到分组
     * 
     * @param appName 应用名称
     */
    public void addApp(String appName) {
        if (!appNames.contains(appName)) {
            appNames.add(appName);
        }
    }
    
    /**
     * 从分组中移除应用
     * 
     * @param appName 应用名称
     */
    public void removeApp(String appName) {
        appNames.remove(appName);
    }
    
    /**
     * 检查分组是否包含指定应用
     * 
     * @param appName 应用名称
     * @return 是否包含
     */
    public boolean containsApp(String appName) {
        return appNames.contains(appName);
    }
    
    /**
     * 检查分组是否为空
     * 
     * @return 是否为空
     */
    public boolean isEmpty() {
        return appNames.isEmpty();
    }
    
    /**
     * 获取分组中应用的数量
     * 
     * @return 应用数量
     */
    public int size() {
        return appNames.size();
    }
    
    @Override
    public String toString() {
        return "FavoriteGroup{" +
                "name='" + name + '\'' +
                ", appNames=" + appNames +
                ", expanded=" + expanded +
                '}';
    }
}
