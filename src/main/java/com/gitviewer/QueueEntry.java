package com.gitviewer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 队列条目数据模型
 * 代表顺序打包队列中的一个 FavoriteGroup 打包任务
 */
public class QueueEntry {

    /**
     * 队列条目状态枚举
     */
    public enum QueueStatus {
        PENDING,    // 等待执行
        BUILDING,   // 正在打包（已触发 API，轮询中）
        SUCCESS,    // 全部应用打包成功
        FAILED,     // 有应用打包失败
        CANCELLED   // 已取消
    }

    // ===== 持久化字段 =====
    /** FavoriteGroup 名称；虚拟组固定为 "Ungrouped" */
    private String groupName;

    /** 该 group 下被勾选的应用名称列表 */
    private List<String> appNames;

    /** 打包分支 */
    private String branch;

    /** planCode / versionCode，用于轮询时匹配 */
    private String version;

    /** 租户代码 */
    private String tenant;

    /** 当前状态 */
    private QueueStatus status;

    /** ISO 8601 格式的 API 触发时间；PENDING 时为空 */
    private String triggeredAt;

    // ===== 运行时字段（不持久化）=====
    /** 各应用当前 build_status，key=appName, value=buildStatus */
    private transient Map<String, String> appBuildStatuses;

    /**
     * 默认构造函数
     */
    public QueueEntry() {
        this.appNames = new ArrayList<>();
        this.status = QueueStatus.PENDING;
        this.triggeredAt = "";
        this.appBuildStatuses = new HashMap<>();
    }

    /**
     * 完整构造函数
     *
     * @param groupName group 名称
     * @param appNames  应用名称列表
     * @param branch    分支
     * @param version   版本号/planCode
     * @param tenant    租户代码
     */
    public QueueEntry(String groupName, List<String> appNames, String branch, String version, String tenant) {
        this.groupName = groupName;
        this.appNames = appNames != null ? new ArrayList<>(appNames) : new ArrayList<>();
        this.branch = branch;
        this.version = version;
        this.tenant = tenant;
        this.status = QueueStatus.PENDING;
        this.triggeredAt = "";
        this.appBuildStatuses = new HashMap<>();
    }

    // ===== Getters / Setters =====

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public List<String> getAppNames() { return appNames; }
    public void setAppNames(List<String> appNames) {
        this.appNames = appNames != null ? new ArrayList<>(appNames) : new ArrayList<>();
    }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getTenant() { return tenant; }
    public void setTenant(String tenant) { this.tenant = tenant; }

    public QueueStatus getStatus() { return status; }
    public void setStatus(QueueStatus status) { this.status = status; }

    public String getTriggeredAt() { return triggeredAt; }
    public void setTriggeredAt(String triggeredAt) { this.triggeredAt = triggeredAt; }

    /**
     * 获取运行时各应用构建状态（不持久化）
     */
    public Map<String, String> getAppBuildStatuses() {
        if (appBuildStatuses == null) {
            appBuildStatuses = new HashMap<>();
        }
        return appBuildStatuses;
    }

    public void setAppBuildStatuses(Map<String, String> appBuildStatuses) {
        this.appBuildStatuses = appBuildStatuses != null ? appBuildStatuses : new HashMap<>();
    }

    /**
     * 更新单个应用的构建状态
     *
     * @param appName     应用名称
     * @param buildStatus 构建状态字符串
     */
    public void updateAppBuildStatus(String appName, String buildStatus) {
        if (appBuildStatuses == null) {
            appBuildStatuses = new HashMap<>();
        }
        appBuildStatuses.put(appName, buildStatus);
    }

    @Override
    public String toString() {
        return "QueueEntry{groupName='" + groupName + "', apps=" + appNames +
               ", branch='" + branch + "', version='" + version + "', status=" + status + "}";
    }
}
