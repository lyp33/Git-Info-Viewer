package com.gitviewer.tools;

import com.gitviewer.GitApiClient;
import com.gitviewer.GitTool;
import com.gitviewer.GitToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

public class SearchRepositoriesTool implements GitTool {
    private GitApiClient client;
    
    public SearchRepositoriesTool(GitApiClient client) {
        this.client = client;
    }
    
    @Override
    public String getName() {
        return "search_repos";
    }
    
    @Override
    public String getDescription() {
        return "搜索仓库";
    }
    
    @Override
    public Map<String, GitToolParameter> getParameters() {
        Map<String, GitToolParameter> params = new LinkedHashMap<>();
        params.put("query", new GitToolParameter("string", "搜索关键词", true));
        return params;
    }
    
    @Override
    public String execute(Map<String, String> params) {
        try {
            String query = params.get("query");
            
            if (query == null || query.isEmpty()) {
                return "ERROR: 参数 'query' 是必需的";
            }
            
            System.out.println("[SearchRepositoriesTool] Calling API: searchRepositories(" + query + ")");
            return client.searchRepositories(query);
            
        } catch (Exception e) {
            System.err.println("[SearchRepositoriesTool] ERROR: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
