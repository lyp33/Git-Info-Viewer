package com.gitviewer;

/**
 * GitLab项目数据类
 * 用于表示GitLab API返回的项目信息
 */
public class GitLabProject {
    public String id;              // 项目ID
    public String name;            // 项目名称
    public String path;            // 项目路径
    public String pathWithNamespace; // 完整路径（包含命名空间）
    public String httpUrlToRepo;   // Git仓库HTTP URL
    public boolean selected;       // 是否被选中下载

    public GitLabProject() {
        this.selected = false;
    }

    @Override
    public String toString() {
        return "GitLabProject{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", httpUrlToRepo='" + httpUrlToRepo + '\'' +
                ", selected=" + selected +
                '}';
    }
}
