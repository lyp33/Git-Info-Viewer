package com.gitviewer;

/**
 * Jenkins 作业信息
 */
public class JenkinsJob {
    private String name;
    private String url;
    private String fullName;
    private String description;

    public JenkinsJob() {
    }

    public JenkinsJob(String name, String url, String fullName, String description) {
        this.name = name;
        this.url = url;
        this.fullName = fullName;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
