package com.gitviewer.tools;

import com.gitviewer.GitApiClient;
import com.gitviewer.GitTool;
import com.gitviewer.GitToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 获取 Issues Tool
 */
public class GetIssuesTool implements GitTool {
    
    private GitApiClient client;
    
    public GetIssuesTool(GitApiClient client) {
        this.client = client;
    }
    
    @Override
    public String getName() {
        return "get_issues";
    }
    
    @Override
    public String getDescription() {
        return "获取 issues 列表";
    }
    
    @Override
    public Map<String, GitToolParameter> getParameters() {
        Map<String, GitToolParameter> params = new LinkedHashMap<>();
        params.put("owner", new GitToolParameter("string", "仓库所有者", true));
        params.put("repo", new GitToolParameter("string", "仓库名称", true));
        params.put("state", new GitToolParameter("string", "状态：open/closed/all", false, "open"));
        return params;
    }
    
    @Override
    public String execute(Map<String, String> params) {
        try {
            String owner = params.get("owner");
            String repo = params.get("repo");
            String state = params.getOrDefault("state", "open");
            
            if (owner == null || owner.isEmpty()) {
                return "ERROR: 参数 'owner' 是必需的";
            }
            if (repo == null || repo.isEmpty()) {
                return "ERROR: 参数 'repo' 是必需的";
            }
            
            System.out.println("[GetIssuesTool] Calling API: getIssues(" + owner + ", " + repo + ", " + state + ")");
            return client.getIssues(owner, repo, state);
            
        } catch (Exception e) {
            System.err.println("[GetIssuesTool] ERROR: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
