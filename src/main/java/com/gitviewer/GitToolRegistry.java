package com.gitviewer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Git Tool 注册表
 * 管理所有可用的 Git Tools
 */
public class GitToolRegistry {
    
    private Map<String, GitTool> tools;
    
    public GitToolRegistry() {
        this.tools = new LinkedHashMap<>();  // 保持注册顺序
    }
    
    /**
     * 注册一个 Tool
     */
    public void register(GitTool tool) {
        tools.put(tool.getName(), tool);
        System.out.println("[Tool Registry] Registered tool: " + tool.getName());
    }
    
    /**
     * 获取指定名称的 Tool
     */
    public GitTool getTool(String name) {
        return tools.get(name);
    }
    
    /**
     * 检查 Tool 是否存在
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
    
    /**
     * 获取所有 Tool 名称
     */
    public String getToolNames() {
        return String.join(", ", tools.keySet());
    }
    
    /**
     * 生成 Tool 列表描述（用于 AI 提示词）
     */
    public String generateToolsDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("【可用的 Tools】\n");
        
        int index = 1;
        for (GitTool tool : tools.values()) {
            sb.append(index++).append(". ");
            sb.append(tool.getName()).append(" - ");
            sb.append(tool.getDescription());
            
            // 添加参数信息
            if (!tool.getParameters().isEmpty()) {
                sb.append("（参数：");
                boolean first = true;
                for (Map.Entry<String, GitToolParameter> entry : tool.getParameters().entrySet()) {
                    if (!first) {
                        sb.append(", ");
                    }
                    sb.append(entry.getKey());
                    if (entry.getValue().isRequired()) {
                        sb.append("*");  // 必需参数标记
                    }
                    first = false;
                }
                sb.append("）");
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 生成详细的 Tool 文档（用于调试）
     */
    public String generateDetailedDocumentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Git Tools 详细文档 ===\n\n");
        
        for (GitTool tool : tools.values()) {
            sb.append("Tool: ").append(tool.getName()).append("\n");
            sb.append("描述: ").append(tool.getDescription()).append("\n");
            sb.append("参数:\n");
            
            if (tool.getParameters().isEmpty()) {
                sb.append("  (无参数)\n");
            } else {
                for (Map.Entry<String, GitToolParameter> entry : tool.getParameters().entrySet()) {
                    sb.append("  - ").append(entry.getKey()).append(": ");
                    sb.append(entry.getValue().toString()).append("\n");
                }
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 获取 Tool 数量
     */
    public int size() {
        return tools.size();
    }
}
