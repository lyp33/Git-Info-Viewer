package com.gitviewer.tools;

import com.gitviewer.GitApiClient;
import com.gitviewer.GitTool;
import com.gitviewer.GitToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 获取单个 Commit 详细信息 Tool（包括 diff）
 */
public class GetSingleCommitTool implements GitTool {
    
    private GitApiClient client;
    
    public GetSingleCommitTool(GitApiClient client) {
        this.client = client;
    }
    
    @Override
    public String getName() {
        return "get_single_commit";
    }
    
    @Override
    public String getDescription() {
        return "获取单个 commit 的详细信息，包括文件修改内容（diff）。用于查看某个 commit 具体修改了什么";
    }
    
    @Override
    public Map<String, GitToolParameter> getParameters() {
        Map<String, GitToolParameter> params = new LinkedHashMap<>();
        params.put("owner", new GitToolParameter("string", "仓库所有者", true));
        params.put("repo", new GitToolParameter("string", "仓库名称", true));
        params.put("sha", new GitToolParameter("string", "commit SHA（从 get_file_commits 或 get_commits 的返回结果中获取）", true));
        return params;
    }
    
    @Override
    public String execute(Map<String, String> params) {
        try {
            String owner = params.get("owner");
            String repo = params.get("repo");
            String sha = params.get("sha");
            
            if (owner == null || owner.isEmpty()) {
                return "ERROR: 参数 'owner' 是必需的";
            }
            if (repo == null || repo.isEmpty()) {
                return "ERROR: 参数 'repo' 是必需的";
            }
            if (sha == null || sha.isEmpty()) {
                return "ERROR: 参数 'sha' 是必需的";
            }
            
            System.out.println("[GetSingleCommitTool] Calling API: getSingleCommit(" + owner + ", " + repo + ", " + sha + ")");
            return client.getSingleCommit(owner, repo, sha);
            
        } catch (Exception e) {
            System.err.println("[GetSingleCommitTool] ERROR: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
