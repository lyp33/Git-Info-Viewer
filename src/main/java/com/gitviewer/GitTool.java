package com.gitviewer;

import java.util.Map;

/**
 * Git Tool 接口
 * 每个 Tool 代表一个可以被 AI 调用的 Git API 功能
 */
public interface GitTool {
    
    /**
     * 获取 Tool 名称（用于 AI 调用）
     * 例如：get_repo, search_files, get_commits
     */
    String getName();
    
    /**
     * 获取 Tool 描述（用于生成提示词）
     * 例如：获取仓库基本信息（star数、描述、语言等）
     */
    String getDescription();
    
    /**
     * 获取参数定义（用于生成提示词和验证）
     * Key: 参数名称（例如：owner, repo, state）
     * Value: 参数定义（类型、描述、是否必需）
     */
    Map<String, GitToolParameter> getParameters();
    
    /**
     * 执行 Tool
     * @param params 参数 Map（Key: 参数名称, Value: 参数值）
     * @return API 返回的 JSON 字符串，或错误信息
     */
    String execute(Map<String, String> params);
    
    /**
     * 获取参数描述（用于生成提示词）
     */
    default String getParametersDescription() {
        if (getParameters().isEmpty()) {
            return "无参数";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, GitToolParameter> entry : getParameters().entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(entry.getKey());
            if (entry.getValue().isRequired()) {
                sb.append("*");
            }
            first = false;
        }
        return sb.toString();
    }
}
