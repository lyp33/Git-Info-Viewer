package com.gitviewer.tools;

import com.gitviewer.GitApiClient;
import com.gitviewer.GitTool;
import com.gitviewer.GitToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

public class GetBranchesTool implements GitTool {
    private GitApiClient client;
    
    public GetBranchesTool(GitApiClient client) {
        this.client = client;
    }
    
    @Override
    public String getName() {
        return "get_branches";
    }
    
    @Override
    public String getDescription() {
        return "获取分支列表";
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
            
            System.out.println("[GetBranchesTool] Calling API: getBranches(" + owner + ", " + repo + ")");
            return client.getBranches(owner, repo);
            
        } catch (Exception e) {
            System.err.println("[GetBranchesTool] ERROR: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
