package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本代码模式生成器
 * Utility class for version code pattern validation and generation
 */
public class VersionPatternGenerator {
    private static final Logger logger = LoggerFactory.getLogger(VersionPatternGenerator.class);
    
    // 占位符映射（按长度降序排列，避免替换冲突）
    private static final Map<String, String> PLACEHOLDER_FORMATS = new LinkedHashMap<>();
    
    static {
        // 组合格式（最长的先替换）
        PLACEHOLDER_FORMATS.put("{YYYYMMDDHHMMSS}", "yyyyMMddHHmmss");
        PLACEHOLDER_FORMATS.put("{YYYYMMDD}", "yyyyMMdd");
        PLACEHOLDER_FORMATS.put("{HHMMSS}", "HHmmss");
        
        // 单独的日期时间组件
        PLACEHOLDER_FORMATS.put("{YYYY}", "yyyy");
        PLACEHOLDER_FORMATS.put("{MM}", "MM");
        PLACEHOLDER_FORMATS.put("{DD}", "dd");
        PLACEHOLDER_FORMATS.put("{HH}", "HH");
        PLACEHOLDER_FORMATS.put("{MI}", "mm");
        PLACEHOLDER_FORMATS.put("{SS}", "ss");
    }
    
    // {branch} 占位符单独处理
    private static final String BRANCH_PLACEHOLDER = "{branch}";
    
    /**
     * 生成版本代码
     * Generate version code from pattern
     * 
     * @param pattern 模式字符串（可以为null或空）
     * @param branch 分支名称
     * @param date 日期时间
     * @return 生成的版本代码
     */
    public static String generateVersionCode(String pattern, String branch, Date date) {
        try {
            // 如果没有配置模式，使用默认格式
            if (pattern == null || pattern.trim().isEmpty()) {
                pattern = "{branch}_{YYYYMMDDHHMMSS}";
                logger.debug("Using default pattern: {}", pattern);
            }
            
            String result = pattern;
            
            // 替换日期时间占位符（按长度降序）
            for (Map.Entry<String, String> entry : PLACEHOLDER_FORMATS.entrySet()) {
                String placeholder = entry.getKey();
                String dateFormat = entry.getValue();
                
                if (result.contains(placeholder)) {
                    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
                    String formattedValue = sdf.format(date);
                    result = result.replace(placeholder, formattedValue);
                    logger.trace("Replaced {} with {}", placeholder, formattedValue);
                }
            }
            
            // 替换分支占位符
            if (result.contains(BRANCH_PLACEHOLDER)) {
                String branchValue = (branch != null && !branch.trim().isEmpty()) ? branch : "unknown";
                result = result.replace(BRANCH_PLACEHOLDER, branchValue);
                logger.trace("Replaced {} with {}", BRANCH_PLACEHOLDER, branchValue);
            }
            
            logger.debug("Generated version code: {} from pattern: {}", result, pattern);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to generate version code from pattern: {}", pattern, e);
            // 回退到默认格式
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            String timestamp = sdf.format(date);
            String fallback = (branch != null ? branch : "unknown") + "_" + timestamp;
            logger.warn("Using fallback version code: {}", fallback);
            return fallback;
        }
    }
    
    /**
     * 验证模式是否有效
     * Validate pattern for unrecognized placeholders
     * 
     * @param pattern 模式字符串
     * @return true if valid, false if contains unrecognized placeholders
     */
    public static boolean validatePattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return true;  // 空模式是有效的（使用默认格式）
        }
        
        // 查找所有 {xxx} 格式的占位符
        Pattern placeholderPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = placeholderPattern.matcher(pattern);
        
        Set<String> validPlaceholders = getValidPlaceholderSet();
        List<String> invalidPlaceholders = new ArrayList<>();
        
        while (matcher.find()) {
            String placeholder = "{" + matcher.group(1) + "}";
            if (!validPlaceholders.contains(placeholder)) {
                invalidPlaceholders.add(placeholder);
            }
        }
        
        if (!invalidPlaceholders.isEmpty()) {
            logger.warn("Pattern contains invalid placeholders: {}", invalidPlaceholders);
            return false;
        }
        
        logger.debug("Pattern validation passed: {}", pattern);
        return true;
    }
    
    /**
     * 获取所有可用的占位符列表
     * Get list of all available placeholders
     * 
     * @return 占位符列表
     */
    public static List<String> getAvailablePlaceholders() {
        List<String> placeholders = new ArrayList<>();
        placeholders.add(BRANCH_PLACEHOLDER);
        placeholders.addAll(PLACEHOLDER_FORMATS.keySet());
        return placeholders;
    }
    
    /**
     * 获取有效占位符集合（用于验证）
     * Get set of valid placeholders for validation
     * 
     * @return 占位符集合
     */
    private static Set<String> getValidPlaceholderSet() {
        Set<String> validPlaceholders = new HashSet<>();
        validPlaceholders.add(BRANCH_PLACEHOLDER);
        validPlaceholders.addAll(PLACEHOLDER_FORMATS.keySet());
        return validPlaceholders;
    }
    
    /**
     * 获取验证错误消息
     * Get validation error message with list of valid placeholders
     * 
     * @param pattern 模式字符串
     * @return 错误消息，如果模式有效则返回null
     */
    public static String getValidationErrorMessage(String pattern) {
        if (validatePattern(pattern)) {
            return null;
        }
        
        // 查找无效的占位符
        Pattern placeholderPattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = placeholderPattern.matcher(pattern);
        
        Set<String> validPlaceholders = getValidPlaceholderSet();
        List<String> invalidPlaceholders = new ArrayList<>();
        
        while (matcher.find()) {
            String placeholder = "{" + matcher.group(1) + "}";
            if (!validPlaceholders.contains(placeholder)) {
                invalidPlaceholders.add(placeholder);
            }
        }
        
        StringBuilder message = new StringBuilder();
        message.append("Invalid placeholder(s) found: ");
        message.append(String.join(", ", invalidPlaceholders));
        message.append("\n\nValid placeholders are:\n");
        
        message.append("\nBranch Information:\n");
        message.append("  {branch}  - Git branch name\n");
        
        message.append("\nDate Components:\n");
        message.append("  {YYYY}    - 4-digit year\n");
        message.append("  {MM}      - 2-digit month\n");
        message.append("  {DD}      - 2-digit day\n");
        message.append("  {YYYYMMDD} - Combined date\n");
        
        message.append("\nTime Components:\n");
        message.append("  {HH}      - 2-digit hour (24h)\n");
        message.append("  {MI}      - 2-digit minute\n");
        message.append("  {SS}      - 2-digit second\n");
        message.append("  {HHMMSS}  - Combined time\n");
        
        message.append("\nCombined:\n");
        message.append("  {YYYYMMDDHHMMSS} - Full datetime\n");
        
        return message.toString();
    }
}
