package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Tenant CI/CD功能的工具方法类
 * Utility methods for Tenant CI/CD feature
 */
public class TenantCICDUtils {
    private static final Logger logger = LoggerFactory.getLogger(TenantCICDUtils.class);
    
    /**
     * 过滤Plan名称，通过匹配"-"分隔符前的前缀
     * Filter plan names by matching prefix before "-" separator
     * 
     * Example: User enters "v202601200722"
     * Plan list: ["v202601200722-20260120113127", "v202601200723-20260120113128", "003-20250629094100"]
     * Returns: "v202601200722-20260120113127" (first match)
     * 
     * @param planNames 完整的plan名称列表
     * @param userInput 用户输入的plan名称前缀
     * @return 第一个匹配的完整plan名称，如果没有匹配则返回null
     */
    public static String filterPlanName(List<String> planNames, String userInput) {
        if (planNames == null || planNames.isEmpty()) {
            logger.debug("filterPlanName: planNames is null or empty");
            return null;
        }
        
        if (userInput == null || userInput.trim().isEmpty()) {
            logger.debug("filterPlanName: userInput is null or empty");
            return null;
        }
        
        String trimmedInput = userInput.trim();
        logger.debug("filterPlanName: Searching for plan matching prefix '{}'", trimmedInput);
        
        for (String planName : planNames) {
            if (planName == null || planName.isEmpty()) {
                continue;
            }
            
            // 按"-"分割plan名称
            String[] parts = planName.split("-");
            if (parts.length > 0 && parts[0].equals(trimmedInput)) {
                logger.info("filterPlanName: Found matching plan '{}' for input '{}'", planName, trimmedInput);
                return planName;  // 返回第一个匹配
            }
        }
        
        logger.info("filterPlanName: No matching plan found for input '{}'", trimmedInput);
        return null;  // 没有找到匹配
    }
    
    /**
     * 解析逗号分隔的tenant codes字符串为列表
     * Parse comma-separated tenant codes string to list
     * 
     * Example: "thailife, tenant2 , tenant3" -> ["thailife", "tenant2", "tenant3"]
     * 
     * @param tenantCodesStr 逗号分隔的tenant codes字符串
     * @return tenant codes列表，如果输入为空则返回空列表
     */
    public static List<String> parseTenantCodes(String tenantCodesStr) {
        if (tenantCodesStr == null || tenantCodesStr.trim().isEmpty()) {
            logger.debug("parseTenantCodes: Input is null or empty");
            return new ArrayList<>();
        }
        
        // 按逗号分割，去除空白，过滤空字符串
        List<String> tenantCodes = Arrays.stream(tenantCodesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        
        logger.debug("parseTenantCodes: Parsed {} tenant codes from input", tenantCodes.size());
        return tenantCodes;
    }
    
    /**
     * 将tenant codes列表格式化为逗号分隔的字符串
     * Format tenant codes list to comma-separated string
     * 
     * Example: ["thailife", "tenant2", "tenant3"] -> "thailife,tenant2,tenant3"
     * 
     * @param tenantCodes tenant codes列表
     * @return 逗号分隔的字符串，如果列表为空则返回空字符串
     */
    public static String formatTenantCodes(List<String> tenantCodes) {
        if (tenantCodes == null || tenantCodes.isEmpty()) {
            logger.debug("formatTenantCodes: Input list is null or empty");
            return "";
        }
        
        String formatted = tenantCodes.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .collect(Collectors.joining(","));
        
        logger.debug("formatTenantCodes: Formatted {} tenant codes to string", tenantCodes.size());
        return formatted;
    }
    
    /**
     * 验证数字输入是否有效（非负整数）
     * Validate numeric input (non-negative integer)
     * 
     * @param input 输入字符串
     * @param fieldName 字段名称（用于日志）
     * @return true if valid, false otherwise
     */
    public static boolean validateNumericInput(String input, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            logger.debug("validateNumericInput: {} is null or empty", fieldName);
            return false;
        }
        
        try {
            int value = Integer.parseInt(input.trim());
            if (value < 0) {
                logger.warn("validateNumericInput: {} value {} is negative", fieldName, value);
                return false;
            }
            logger.debug("validateNumericInput: {} value {} is valid", fieldName, value);
            return true;
        } catch (NumberFormatException e) {
            logger.warn("validateNumericInput: {} value '{}' is not a valid integer", fieldName, input);
            return false;
        }
    }
    
    /**
     * 解析数字输入，返回整数值
     * Parse numeric input and return integer value
     * 
     * @param input 输入字符串
     * @param defaultValue 默认值（如果解析失败）
     * @param fieldName 字段名称（用于日志）
     * @return 解析后的整数值，如果解析失败则返回默认值
     */
    public static int parseNumericInput(String input, int defaultValue, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            logger.debug("parseNumericInput: {} is empty, using default value {}", fieldName, defaultValue);
            return defaultValue;
        }
        
        try {
            int value = Integer.parseInt(input.trim());
            if (value < 0) {
                logger.warn("parseNumericInput: {} value {} is negative, using default value {}", 
                    fieldName, value, defaultValue);
                return defaultValue;
            }
            logger.debug("parseNumericInput: {} parsed to {}", fieldName, value);
            return value;
        } catch (NumberFormatException e) {
            logger.warn("parseNumericInput: {} value '{}' is not valid, using default value {}", 
                fieldName, input, defaultValue);
            return defaultValue;
        }
    }
    
    /**
     * 解析带有子租户代码的租户代码字符串
     * Parse tenant codes with sub-tenant codes (workspaces)
     * 
     * Supports two formats:
     * 1. Simple format: "tenant1,tenant2,tenant3"
     * 2. With sub-tenants: "tenant{sub1/sub2/sub3},tenant2"
     * 3. Mixed format: "tenant1{sub1/sub2},tenant2,tenant3{sub3/sub4}"
     * 
     * Examples:
     * - "stbd,thailife" -> {"stbd": [], "thailife": []}
     * - "stbd{stbddev/stbdtst}" -> {"stbd": ["stbddev", "stbdtst"]}
     * - "stbd{stbddev/stbdtst},thailife{thailifedev/thailifetest}" -> 
     *   {"stbd": ["stbddev", "stbdtst"], "thailife": ["thailifedev", "thailifetest"]}
     * 
     * @param tenantCodesStr 租户代码字符串
     * @return Map<String, List<String>> - 租户代码 -> 子租户代码列表
     */
    public static java.util.Map<String, List<String>> parseTenantCodesWithSubTenants(String tenantCodesStr) {
        java.util.Map<String, List<String>> result = new java.util.HashMap<>();
        
        if (tenantCodesStr == null || tenantCodesStr.trim().isEmpty()) {
            logger.debug("parseTenantCodesWithSubTenants: Input is null or empty");
            return result;
        }
        
        logger.info("parseTenantCodesWithSubTenants: Parsing tenant codes: {}", tenantCodesStr);
        
        // 按逗号分割
        String[] tenantParts = tenantCodesStr.split(",");
        
        for (String tenantPart : tenantParts) {
            tenantPart = tenantPart.trim();
            
            if (tenantPart.isEmpty()) {
                continue;
            }
            
            // 检查是否包含子租户代码（格式：tenant{sub1/sub2}）
            if (tenantPart.contains("{") && tenantPart.contains("}")) {
                int braceStart = tenantPart.indexOf("{");
                int braceEnd = tenantPart.indexOf("}");
                
                if (braceStart < braceEnd) {
                    // 提取租户代码
                    String tenantCode = tenantPart.substring(0, braceStart).trim();
                    
                    // 提取子租户代码
                    String subTenantsStr = tenantPart.substring(braceStart + 1, braceEnd).trim();
                    
                    // 按斜杠分割子租户代码
                    String[] subTenants = subTenantsStr.split("/");
                    List<String> subTenantList = new ArrayList<>();
                    
                    for (String subTenant : subTenants) {
                        subTenant = subTenant.trim();
                        if (!subTenant.isEmpty()) {
                            subTenantList.add(subTenant);
                        }
                    }
                    
                    result.put(tenantCode, subTenantList);
                    logger.info("parseTenantCodesWithSubTenants: Parsed tenant '{}' with {} sub-tenants: {}", 
                               tenantCode, subTenantList.size(), subTenantList);
                } else {
                    // 格式错误，作为简单租户代码处理
                    logger.warn("parseTenantCodesWithSubTenants: Malformed tenant code '{}', treating as simple tenant", tenantPart);
                    result.put(tenantPart, new ArrayList<>());
                }
            } else {
                // 简单格式，没有子租户代码
                result.put(tenantPart, new ArrayList<>());
                logger.info("parseTenantCodesWithSubTenants: Parsed simple tenant '{}'", tenantPart);
            }
        }
        
        logger.info("parseTenantCodesWithSubTenants: Parsed {} tenants total", result.size());
        return result;
    }
    
    /**
     * 从镜像名称中提取应用名称
     * Extract app name from image name
     * 
     * Supports formats:
     * - "docker-all.repo.ebaotech.com/thailifedev/thailife-bs:24.08.22" -> "thailife-bs"
     * - "registry/workspace/app:version" -> "app"
     * - "workspace/app:version" -> "app"
     * - "app:version" -> "app"
     * - "app" -> "app"
     * 
     * @param imageName 镜像名称
     * @return 应用名称，如果解析失败则返回null
     */
    public static String extractAppNameFromImage(String imageName) {
        if (imageName == null || imageName.trim().isEmpty()) {
            logger.debug("extractAppNameFromImage: imageName is null or empty");
            return null;
        }
        
        String trimmed = imageName.trim();
        logger.debug("extractAppNameFromImage: Parsing image name: {}", trimmed);
        
        try {
            // 移除版本标签（如果存在）
            String withoutVersion = trimmed;
            if (trimmed.contains(":")) {
                withoutVersion = trimmed.substring(0, trimmed.lastIndexOf(":"));
                logger.debug("extractAppNameFromImage: Removed version tag, result: {}", withoutVersion);
            }
            
            // 按斜杠分割，取最后一部分
            String[] parts = withoutVersion.split("/");
            String appName = parts[parts.length - 1];
            
            if (appName.isEmpty()) {
                logger.warn("extractAppNameFromImage: Extracted app name is empty for image: {}", imageName);
                return null;
            }
            
            logger.info("extractAppNameFromImage: Extracted app name '{}' from image '{}'", appName, imageName);
            return appName;
            
        } catch (Exception e) {
            logger.error("extractAppNameFromImage: Failed to parse image name: {}", imageName, e);
            return null;
        }
    }
}
