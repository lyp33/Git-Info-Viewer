package com.gitviewer.tools;

import com.gitviewer.GitApiClient;
import com.gitviewer.GitTool;
import com.gitviewer.GitToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 搜索文件 Tool
 */
public class SearchFilesTool implements GitTool {
    
    private GitApiClient client;
    
    public SearchFilesTool(GitApiClient client) {
        this.client = client;
    }
    
    @Override
    public String getName() {
        return "search_files";
    }
    
    @Override
    public String getDescription() {
        return "搜索文件名和文件内容（返回匹配的代码片段，不是完整文件。如需完整内容，请先用此API找到文件路径，再用get_file_content获取）";
    }
    
    @Override
    public Map<String, GitToolParameter> getParameters() {
        Map<String, GitToolParameter> params = new LinkedHashMap<>();
        params.put("owner", new GitToolParameter("string", "仓库所有者", true));
        params.put("repo", new GitToolParameter("string", "仓库名称", true));
        params.put("filename", new GitToolParameter("string", "搜索关键词：可以是文件名（如 pom.xml）、代码内容（如 interest settlement）、业务术语等", true));
        params.put("ref", new GitToolParameter("string", "分支名或标签（可选，默认为默认分支）", false));
        return params;
    }
    
    @Override
    public String execute(Map<String, String> params) {
        try {
            String owner = params.get("owner");
            String repo = params.get("repo");
            String filename = params.get("filename");
            String ref = params.get("ref");  // 可选参数
            
            if (owner == null || owner.isEmpty()) {
                return "ERROR: 参数 'owner' 是必需的";
            }
            if (repo == null || repo.isEmpty()) {
                return "ERROR: 参数 'repo' 是必需的";
            }
            if (filename == null || filename.isEmpty()) {
                return "ERROR: 参数 'filename' 是必需的";
            }
            
            String refInfo = (ref != null && !ref.isEmpty()) ? ", ref=" + ref : "";
            System.out.println("[SearchFilesTool] Calling API: searchFiles(" + owner + ", " + repo + ", " + filename + refInfo + ")");
            return client.searchFiles(owner, repo, filename, ref);
            
        } catch (Exception e) {
            System.err.println("[SearchFilesTool] ERROR: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
