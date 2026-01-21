package com.gitviewer;

/**
 * Jenkins Pipeline Stage 信息
 */
public class JenkinsStage {
    private String id;
    private String name;
    private String status;  // SUCCESS, FAILED, IN_PROGRESS, NOT_EXECUTED, ABORTED
    private long durationMillis;
    private Integer stageBuildNumber;  // Stage 的 Build ID
    private JenkinsBuild buildInfo;  // 关联的 Build 信息（用于合成 stage）

    public JenkinsStage() {
    }

    public JenkinsStage(String name, String status, long durationMillis) {
        this.name = name;
        this.status = status;
        this.durationMillis = durationMillis;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    /**
     * 判断 Stage 是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    /**
     * 判断 Stage 是否失败
     */
    public boolean isFailure() {
        return "FAILED".equals(status) || "ABORTED".equals(status);
    }

    /**
     * 判断 Stage 是否正在进行
     */
    public boolean isInProgress() {
        return "IN_PROGRESS".equals(status);
    }

    /**
     * 判断 Stage 是否未执行
     */
    public boolean isNotExecuted() {
        return "NOT_EXECUTED".equals(status) || status == null;
    }

    /**
     * 获取格式化的持续时间
     */
    public String getFormattedDuration() {
        if (durationMillis <= 0) {
            return "-";
        }
        long seconds = durationMillis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * 获取 Stage Build Number
     */
    public Integer getStageBuildNumber() {
        return stageBuildNumber;
    }

    /**
     * 设置 Stage Build Number
     */
    public void setStageBuildNumber(Integer stageBuildNumber) {
        this.stageBuildNumber = stageBuildNumber;
    }

    /**
     * 获取 Stage Build ID 的显示文本
     */
    public String getStageBuildDisplay() {
        if (stageBuildNumber != null && stageBuildNumber > 0) {
            return "#" + stageBuildNumber;
        }
        return "";
    }

    /**
     * 是否有 Stage Build ID
     */
    public boolean hasStageBuildId() {
        return stageBuildNumber != null && stageBuildNumber > 0;
    }

    /**
     * 获取关联的 Build 信息
     */
    public JenkinsBuild getBuildInfo() {
        return buildInfo;
    }

    /**
     * 设置关联的 Build 信息
     */
    public void setBuildInfo(JenkinsBuild buildInfo) {
        this.buildInfo = buildInfo;
    }
}
