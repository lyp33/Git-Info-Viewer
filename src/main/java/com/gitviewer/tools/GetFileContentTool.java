package com.gitviewer.tools;

import com.gitviewer.GitApiClient;
import com.gitviewer.GitTool;
import com.gitviewer.GitToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

public class GetFileContentTool implements GitTool {
    private GitApiClient client;
    private String currentBranch;
    
    public GetFileContentTool(GitApiClient client, String currentBranch) {
        this.client = client;
        this.currentBranch = currentBranch;
    }
    
    @Override
    public String getName() {
        return "get_file_content";
    }
    
    @Override
    public String getDescription() {
        return "获取文件的完整源代码内容";
    }
    
    @Override
    public Map<String, GitToolParameter> getParameters() {
        Map<String, GitToolParameter> params = new LinkedHashMap<>();
        params.put("owner", new GitToolParameter("string", "仓库所有者", true));
        params.put("repo", new GitToolParameter("string", "仓库名称", true));
        params.put("filepath", new GitToolParameter("string", "文件路径（例如：envs/common/.basic）", true));
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
            
            System.out.println("[GetFileContentTool] Calling API: getFileContent(" + owner + ", " + repo + ", " + filepath + ", " + branch + ")");
            String result = client.getFileContent(owner, repo, filepath, branch);
            
            // 文件内容大小限制：50KB
            if (result != null && result.length() > 50000) {
                System.out.println("[GetFileContentTool] File content too large (" + result.length() + " chars), truncating to 50000");
                result = result.substring(0, 50000) + "\n\n...[文件内容过大，已截断到50000字符]";
            }
            
            return result;
            
        } catch (Exception e) {
            System.err.println("[GetFileContentTool] ERROR: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
