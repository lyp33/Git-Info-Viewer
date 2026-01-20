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
}
