package com.gitviewer.tools;

import com.gitviewer.GitApiClient;
import com.gitviewer.GitTool;
import com.gitviewer.GitToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

public class GetFileCommitsTool implements GitTool {
    private GitApiClient client;
    private String currentBranch;
    
    public GetFileCommitsTool(GitApiClient client, String currentBranch) {
        this.client = client;
        this.currentBranch = currentBranch;
    }
    
    @Override
    public String getName() {
        return "get_file_commits";
    }
    
    @Override
    public String getDescription() {
        return "获取文件的提交历史";
    }
    
    @Override
    public Map<String, GitToolParameter> getParameters() {
        Map<String, GitToolParameter> params = new LinkedHashMap<>();
        params.put("owner", new GitToolParameter("string", "仓库所有者", true));
        params.put("repo", new GitToolParameter("string", "仓库名称", true));
        params.put("filepath", new GitToolParameter("string", "文件路径（例如：src/main/App.java）", true));
        params.put("branch", new GitToolParameter("string", "分支名称", false, currentBranch));
        return params;
    }
    
    @Override
    public String execute(Map<String, String> params) {
        try {
            String owner = params.get("owner");
            String repo = params.get("repo");
            String filepath = params.get("filepath");
            String branch = params.getOrDefault("branch", currentBranch);
            
            if (owner == null || owner.isEmpty()) {
                return "ERROR: 参数 'owner' 是必需的";
            }
            if (repo == null || repo.isEmpty()) {
                return "ERROR: 参数 'repo' 是必需的";
            }
            if (filepath == null || filepath.isEmpty()) {
                return "ERROR: 参数 'filepath' 是必需的";
            }
            
            System.out.println("[GetFileCommitsTool] Calling API: getFileCommits(" + owner + ", " + repo + ", " + filepath + ", " + branch + ")");
            return client.getFileCommits(owner, repo, filepath, branch);
            
        } catch (Exception e) {
            System.err.println("[GetFileCommitsTool] ERROR: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
