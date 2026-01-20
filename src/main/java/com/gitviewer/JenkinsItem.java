package com.gitviewer;

import java.util.ArrayList;
import java.util.List;

/**
 * Jenkins 项目（文件夹或作业）
 */
public class JenkinsItem {
    private String name;
    private String url;
    private String className;  // com.cloudbees.hudson.plugins.folder.Folder 或 hudson.model.FreeStyleProject 等
    private boolean isFolder;
    private List<JenkinsItem> children;

    public JenkinsItem() {
        this.children = new ArrayList<>();
    }

    public JenkinsItem(String name, String url, String className) {
        this.name = name;
        this.url = url;
        this.className = className;
        this.isFolder = isFolder(className);
        this.children = new ArrayList<>();
    }

    /**
     * 根据 className 判断是否为文件夹
     */
    private boolean isFolder(String className) {
        return className != null && (
            className.contains("Folder") ||
            className.contains("folder") ||
            className.equals("com.cloudbees.hudson.plugins.folder.Folder") ||
            className.equals("org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject")
        );
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

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
        this.isFolder = isFolder(className);
    }

    public boolean isFolder() {
        return isFolder;
    }

    public void setFolder(boolean folder) {
        isFolder = folder;
    }

    public List<JenkinsItem> getChildren() {
        return children;
    }

    public void setChildren(List<JenkinsItem> children) {
        this.children = children;
    }

    public void addChild(JenkinsItem child) {
        this.children.add(child);
    }

    @Override
    public String toString() {
        return name;
    }
}
