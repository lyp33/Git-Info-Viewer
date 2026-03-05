package com.gitviewer.tools;

import com.gitviewer.GitApiClient;
import com.gitviewer.GitTool;
import com.gitviewer.GitToolParameter;

import java.util.LinkedHashMap;
import java.util.Map;

public class SearchIssuesTool implements GitTool {
    private GitApiClient client;
    
    public SearchIssuesTool(GitApiClient client) {
        this.client = client;
    }
    
    @Override
    public String getName() {
        return "search_issues";
    }
    
    @Override
    public String getDescription() {
        return "搜索 issues";
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
            
            System.out.println("[SearchIssuesTool] Calling API: searchIssues(" + query + ")");
            return client.searchIssues(query);
            
        } catch (Exception e) {
            System.err.println("[SearchIssuesTool] ERROR: " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }
}
