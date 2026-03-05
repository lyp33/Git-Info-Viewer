package com.gitviewer.tools;

import com.gitviewer.GitApiClient;
import com.gitviewer.GitTool;
import com.gitviewer.GitToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

public class GetReleasesTool implements GitTool {
    private GitApiClient client;
    
    public GetReleasesTool(GitApiClient client) {
        this.client = client;
    }
    
    @Override
    public String getName() {
        return "get_releases";
    }
    
    @Override
    public String getDescription() {
        return "获取发布版本";
    }
    
    @Override
    public Map<String, GitToolParameter> getParameters() {
        Map<String, GitToolParameter> params = new LinkedHashMap<>();
        params.put("owner", new GitToolParameter("string", "仓库所有者", true));
        params.put("repo", new GitToolParameter("string", "仓库名称", true));
        return params;
    }
    
    @Override
    public String execute(Map<String, String> params) {
        try {
            String owner = params.get("owner");
            String repo = params.get("repo");
            
            if (owner == null || owner.isEmpty()) {
                return "ERROR: 参数 'owner' 是必需的";
            }
            if (repo == null || repo.isEmpty()) {
                return "ERROR: 参数 'repo' 是必需的";
            }
            
            System.out.println("[GetReleasesTool] Calling API: getReleases(" + owner + ", " + repo + ")");
            return client.getReleases(owner, repo);
            
        } catch (Exception e) {
            System.err.println("[GetReleasesTool] ERROR: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
