package com.gitviewer.tools;

import com.gitviewer.GitApiClient;
import com.gitviewer.GitTool;
import com.gitviewer.GitToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

public class GetContentsTool implements GitTool {
    private GitApiClient client;
    
    public GetContentsTool(GitApiClient client) {
        this.client = client;
    }
    
    @Override
    public String getName() {
        return "get_contents";
    }
    
    @Override
    public String getDescription() {
        return "获取目录内容";
    }
    
    @Override
    public Map<String, GitToolParameter> getParameters() {
        Map<String, GitToolParameter> params = new LinkedHashMap<>();
        params.put("owner", new GitToolParameter("string", "仓库所有者", true));
        params.put("repo", new GitToolParameter("string", "仓库名称", true));
        params.put("path", new GitToolParameter("string", "目录路径（空表示根目录）", false, ""));
        return params;
    }
    
    @Override
    public String execute(Map<String, String> params) {
        try {
            String owner = params.get("owner");
            String repo = params.get("repo");
            String path = params.getOrDefault("path", "");
            
            if (owner == null || owner.isEmpty()) {
                return "ERROR: 参数 'owner' 是必需的";
            }
            if (repo == null || repo.isEmpty()) {
                return "ERROR: 参数 'repo' 是必需的";
            }
            
            System.out.println("[GetContentsTool] Calling API: getContents(" + owner + ", " + repo + ", \"" + path + "\")");
            return client.getContents(owner, repo, path);
            
        } catch (Exception e) {
            System.err.println("[GetContentsTool] ERROR: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
