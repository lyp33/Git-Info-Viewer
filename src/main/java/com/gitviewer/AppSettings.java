package com.gitviewer;

import java.awt.Font;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 应用设置管理类
 */
public class AppSettings {
    private static final String SETTINGS_FILE = "gitviewer.properties";
    private static AppSettings instance;

    // 字体设置
    private Font leftPanelFont;
    private Font rightPanelFont;

    // GitLab 认证配置
    private String gitLabPrivateToken;
    private String gitLabUsername;
    private String gitLabPassword;

    // GitHub 配置
    private String githubToken;

    // AI Chat 配置
    private String aiApiUrl;
    private String aiApiKey;
    private String aiModel;
    private String aiChatMode;  // "simple" 或 "agent"
    private int aiMaxIterations;  // Agent模式的最大循环次数

    // Jenkins 配置
    private String jenkinsUrl;
    private String jenkinsUsername;
    private String jenkinsApiToken;
    private String jenkinsDefaultJobPath;

    // 目录历史记录（最多保存5条）
    private List<String> directoryHistory;
    private static final int MAX_HISTORY_SIZE = 5;

    // Jenkins 收藏任务列表
    private List<FavoriteJob> jenkinsFavorites;
    private static final int MAX_FAVORITES_SIZE = 50;

    // Portal 配置（Tenant CI/CD功能）
    private String portalUsername;
    private String portalPassword;  // 加密存储
    private List<String> portalTenantCodes;
    
    // Portal 收藏分组（Build Package功能）
    // 使用租户代码作为key，存储每个租户的分组配置
    // 格式：Map<tenantCode, List<FavoriteGroup>>
    // 注意：这里不直接存储Map，而是在需要时从properties文件加载/保存

    // 默认字体
    private static final Font DEFAULT_LEFT_FONT = new Font("Arial", Font.PLAIN, 12);
    private static final Font DEFAULT_RIGHT_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    // 从属性文件加载时的默认值（如果没有保存的设置）
    private static final String DEFAULT_RIGHT_FONT_NAME = "Segoe UI";
    private static final int DEFAULT_RIGHT_FONT_SIZE = 12;

    // 字体变化监听器
    private List<FontChangeListener> fontChangeListeners = new ArrayList<>();

    public interface FontChangeListener {
        void onFontChanged(Font leftFont, Font rightFont);
    }

    private AppSettings() {
        directoryHistory = new ArrayList<>();
        jenkinsFavorites = new ArrayList<>();
        portalTenantCodes = new ArrayList<>();
        loadSettings();
    }

    public static AppSettings getInstance() {
        if (instance == null) {
            instance = new AppSettings();
        }
        return instance;
    }

    public void addFontChangeListener(FontChangeListener listener) {
        fontChangeListeners.add(listener);
    }

    public void removeFontChangeListener(FontChangeListener listener) {
        fontChangeListeners.remove(listener);
    }

    public void notifyFontChanged() {
        for (FontChangeListener listener : fontChangeListeners) {
            listener.onFontChanged(leftPanelFont, rightPanelFont);
        }
    }

    /**
     * 加载设置
     */
    private void loadSettings() {
        Properties props = new Properties();
        File file = new File(System.getProperty("user.home"), SETTINGS_FILE);

        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);

                // 加载左侧面板字体
                String leftName = props.getProperty("left.font.name", "Arial");
                int leftStyle = Integer.parseInt(props.getProperty("left.font.style", "0"));
                int leftSize = Integer.parseInt(props.getProperty("left.font.size", "12"));
                leftPanelFont = new Font(leftName, leftStyle, leftSize);

                // 加载右侧面板字体
                String rightName = props.getProperty("right.font.name", DEFAULT_RIGHT_FONT_NAME);
                int rightStyle = Integer.parseInt(props.getProperty("right.font.style", "0"));
                int rightSize = Integer.parseInt(props.getProperty("right.font.size", String.valueOf(DEFAULT_RIGHT_FONT_SIZE)));
                rightPanelFont = new Font(rightName, rightStyle, rightSize);

                // 加载GitLab认证配置
                gitLabPrivateToken = props.getProperty("gitlab.private.token", "");
                gitLabUsername = props.getProperty("gitlab.username", "");
                gitLabPassword = props.getProperty("gitlab.password", "");

                // 加载GitHub配置
                githubToken = props.getProperty("github.token", "");

                // 加载AI Chat配置
                aiApiUrl = props.getProperty("ai.api.url", "");
                aiApiKey = props.getProperty("ai.api.key", "");
                aiModel = props.getProperty("ai.model", "gpt-3.5-turbo");
                aiChatMode = props.getProperty("ai.chat.mode", "simple");
                aiMaxIterations = Integer.parseInt(props.getProperty("ai.max.iterations", "5"));

                // 加载Jenkins配置
                jenkinsUrl = props.getProperty("jenkins.url", "");
                jenkinsUsername = props.getProperty("jenkins.username", "");
                jenkinsApiToken = props.getProperty("jenkins.api.token", "");
                jenkinsDefaultJobPath = props.getProperty("jenkins.default.job.path", "job/gemini");

                // 加载目录历史记录
                directoryHistory.clear();
                for (int i = 0; i < MAX_HISTORY_SIZE; i++) {
                    String historyPath = props.getProperty("directory.history." + i, "");
                    if (!historyPath.isEmpty()) {
                        directoryHistory.add(historyPath);
                    }
                }

                // 加载 Jenkins 收藏数据
                loadJenkinsFavorites();

                // 加载Portal配置
                portalUsername = props.getProperty("portal.username", "");
                portalPassword = props.getProperty("portal.password", "");
                String tenantCodesStr = props.getProperty("portal.tenant.codes", "");
                portalTenantCodes = TenantCICDUtils.parseTenantCodes(tenantCodesStr);

            } catch (IOException e) {
                System.err.println("Error loading settings: " + e.getMessage());
                setDefaultFonts();
            }
        } else {
            setDefaultFonts();
        }
    }

    /**
     * 保存设置
     */
    public void saveSettings() {
        Properties props = new Properties();
        File file = new File(System.getProperty("user.home"), SETTINGS_FILE);

        // 先加载现有设置，避免覆盖其他模块保存的数据（如 favorite groups）
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading existing settings before save: " + e.getMessage());
            }
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            // 保存左侧面板字体
            props.setProperty("left.font.name", leftPanelFont.getName());
            props.setProperty("left.font.style", String.valueOf(leftPanelFont.getStyle()));
            props.setProperty("left.font.size", String.valueOf(leftPanelFont.getSize()));

            // 保存右侧面板字体
            props.setProperty("right.font.name", rightPanelFont.getName());
            props.setProperty("right.font.style", String.valueOf(rightPanelFont.getStyle()));
            props.setProperty("right.font.size", String.valueOf(rightPanelFont.getSize()));

            // 保存GitLab认证配置
            if (gitLabPrivateToken != null && !gitLabPrivateToken.isEmpty()) {
                props.setProperty("gitlab.private.token", gitLabPrivateToken);
            }
            if (gitLabUsername != null && !gitLabUsername.isEmpty()) {
                props.setProperty("gitlab.username", gitLabUsername);
            }
            if (gitLabPassword != null && !gitLabPassword.isEmpty()) {
                props.setProperty("gitlab.password", gitLabPassword);
            }

            // 保存GitHub配置
            if (githubToken != null && !githubToken.isEmpty()) {
                props.setProperty("github.token", githubToken);
            }

            // 保存AI Chat配置
            if (aiApiUrl != null && !aiApiUrl.isEmpty()) {
                props.setProperty("ai.api.url", aiApiUrl);
            }
            if (aiApiKey != null && !aiApiKey.isEmpty()) {
                props.setProperty("ai.api.key", aiApiKey);
            }
            if (aiModel != null && !aiModel.isEmpty()) {
                props.setProperty("ai.model", aiModel);
            }
            if (aiChatMode != null && !aiChatMode.isEmpty()) {
                props.setProperty("ai.chat.mode", aiChatMode);
            }
            props.setProperty("ai.max.iterations", String.valueOf(aiMaxIterations));

            // 保存Jenkins配置
            if (jenkinsUrl != null && !jenkinsUrl.isEmpty()) {
                props.setProperty("jenkins.url", jenkinsUrl);
            }
            if (jenkinsUsername != null && !jenkinsUsername.isEmpty()) {
                props.setProperty("jenkins.username", jenkinsUsername);
            }
            if (jenkinsApiToken != null && !jenkinsApiToken.isEmpty()) {
                props.setProperty("jenkins.api.token", jenkinsApiToken);
            }
            // 允许保存空的 defaultJobPath（用于清除之前的值）
            if (jenkinsDefaultJobPath != null) {
                if (jenkinsDefaultJobPath.isEmpty()) {
                    props.remove("jenkins.default.job.path"); // 移除属性
                } else {
                    props.setProperty("jenkins.default.job.path", jenkinsDefaultJobPath);
                }
            }

            // 保存目录历史记录
            for (int i = 0; i < directoryHistory.size(); i++) {
                props.setProperty("directory.history." + i, directoryHistory.get(i));
            }

            // 保存Portal配置
            if (portalUsername != null && !portalUsername.isEmpty()) {
                props.setProperty("portal.username", portalUsername);
            }
            if (portalPassword != null && !portalPassword.isEmpty()) {
                props.setProperty("portal.password", portalPassword);
            }
            if (portalTenantCodes != null && !portalTenantCodes.isEmpty()) {
                String tenantCodesStr = TenantCICDUtils.formatTenantCodes(portalTenantCodes);
                props.setProperty("portal.tenant.codes", tenantCodesStr);
            }

            props.store(fos, "Git Info Viewer Settings");

        } catch (IOException e) {
            System.err.println("Error saving settings: " + e.getMessage());
        }
    }

    private void setDefaultFonts() {
        leftPanelFont = DEFAULT_LEFT_FONT;
        rightPanelFont = DEFAULT_RIGHT_FONT;
    }

    public Font getLeftPanelFont() {
        return leftPanelFont;
    }

    public void setLeftPanelFont(Font font) {
        this.leftPanelFont = font;
    }

    public Font getRightPanelFont() {
        return rightPanelFont;
    }

    public void setRightPanelFont(Font font) {
        this.rightPanelFont = font;
    }

    // GitLab 认证配置的 getter 和 setter
    public String getGitLabPrivateToken() {
        return gitLabPrivateToken != null ? gitLabPrivateToken : "";
    }

    public void setGitLabPrivateToken(String token) {
        this.gitLabPrivateToken = token;
    }

    public String getGitLabUsername() {
        return gitLabUsername != null ? gitLabUsername : "";
    }

    public void setGitLabUsername(String username) {
        this.gitLabUsername = username;
    }

    public String getGitLabPassword() {
        return gitLabPassword != null ? gitLabPassword : "";
    }

    public void setGitLabPassword(String password) {
        this.gitLabPassword = password;
    }

    // 目录历史记录的 getter 和 setter
    public List<String> getDirectoryHistory() {
        return new ArrayList<>(directoryHistory);
    }

    /**
     * 添加目录到历史记录
     * 如果目录已存在，将其移到最前面
     * 保持最多5条记录
     */
    public void addDirectoryToHistory(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            return;
        }

        // 移除已存在的相同路径
        directoryHistory.remove(directoryPath);

        // 添加到列表开头
        directoryHistory.add(0, directoryPath);

        // 保持最多5条记录
        while (directoryHistory.size() > MAX_HISTORY_SIZE) {
            directoryHistory.remove(directoryHistory.size() - 1);
        }

        // 保存设置
        saveSettings();
    }

    /**
     * 清除目录历史记录
     */
    public void clearDirectoryHistory() {
        directoryHistory.clear();
        saveSettings();
    }

    // GitHub 配置的 getter 和 setter
    public String getGithubToken() {
        return githubToken != null ? githubToken : "";
    }

    public void setGithubToken(String token) {
        this.githubToken = token;
    }

    // AI Chat 配置的 getter 和 setter
    public String getAiApiUrl() {
        return aiApiUrl != null ? aiApiUrl : "";
    }

    public void setAiApiUrl(String url) {
        this.aiApiUrl = url;
    }

    public String getAiApiKey() {
        return aiApiKey != null ? aiApiKey : "";
    }

    public void setAiApiKey(String key) {
        this.aiApiKey = key;
    }

    public String getAiModel() {
        return aiModel != null ? aiModel : "gpt-3.5-turbo";
    }

    public void setAiModel(String model) {
        this.aiModel = model;
    }

    public String getAiChatMode() {
        return aiChatMode != null ? aiChatMode : "simple";
    }

    public void setAiChatMode(String mode) {
        if (mode != null && !"simple".equals(mode) && !"agent".equals(mode)) {
            System.err.println("[AppSettings] Invalid chat mode: " + mode + ", using 'simple'");
            this.aiChatMode = "simple";
        } else {
            this.aiChatMode = mode;
        }
    }

    public int getAiMaxIterations() {
        return aiMaxIterations > 0 ? aiMaxIterations : 5;
    }

    public void setAiMaxIterations(int maxIterations) {
        if (maxIterations < 1 || maxIterations > 10) {
            System.err.println("[AppSettings] Invalid max iterations: " + maxIterations + ", using 5");
            this.aiMaxIterations = 5;
        } else {
            this.aiMaxIterations = maxIterations;
        }
    }

    // Jenkins 配置的 getter 和 setter
    public String getJenkinsUrl() {
        return jenkinsUrl != null ? jenkinsUrl : "";
    }

    public void setJenkinsUrl(String url) {
        this.jenkinsUrl = url;
    }

    public String getJenkinsUsername() {
        return jenkinsUsername != null ? jenkinsUsername : "";
    }

    public void setJenkinsUsername(String username) {
        this.jenkinsUsername = username;
    }

    public String getJenkinsApiToken() {
        return jenkinsApiToken != null ? jenkinsApiToken : "";
    }

    public void setJenkinsApiToken(String token) {
        this.jenkinsApiToken = token;
    }

    public String getJenkinsDefaultJobPath() {
        return jenkinsDefaultJobPath != null ? jenkinsDefaultJobPath : "job/gemini";
    }

    public void setJenkinsDefaultJobPath(String path) {
        this.jenkinsDefaultJobPath = path;
    }

    // Jenkins 收藏功能的 getter 和 setter
    public List<FavoriteJob> getJenkinsFavorites() {
        return new ArrayList<>(jenkinsFavorites);
    }

    public void setJenkinsFavorites(List<FavoriteJob> favorites) {
        this.jenkinsFavorites = new ArrayList<>(favorites);
    }

    /**
     * 添加 Jenkins 收藏任务
     */
    public void addJenkinsFavorite(FavoriteJob job) {
        if (job == null || job.getJobPath() == null) {
            return;
        }

        // 检查是否已存在
        for (FavoriteJob existing : jenkinsFavorites) {
            if (existing.getJobPath().equals(job.getJobPath())) {
                return; // 已存在，不重复添加
            }
        }

        // 检查数量限制
        if (jenkinsFavorites.size() >= MAX_FAVORITES_SIZE) {
            System.err.println("收藏数量已达上限: " + MAX_FAVORITES_SIZE);
            return;
        }

        // 设置顺序
        job.setOrder(jenkinsFavorites.size());
        jenkinsFavorites.add(job);
        saveJenkinsFavorites();
    }

    /**
     * 移除 Jenkins 收藏任务
     */
    public void removeJenkinsFavorite(String jobPath) {
        jenkinsFavorites.removeIf(job -> job.getJobPath().equals(jobPath));
        // 重新设置顺序
        for (int i = 0; i < jenkinsFavorites.size(); i++) {
            jenkinsFavorites.get(i).setOrder(i);
        }
        saveJenkinsFavorites();
    }

    /**
     * 检查任务是否已收藏
     */
    public boolean isJobFavorited(String jobPath) {
        return jenkinsFavorites.stream()
                .anyMatch(job -> job.getJobPath().equals(jobPath));
    }

    /**
     * 上移收藏任务
     */
    public void moveFavoriteUp(int index) {
        if (index > 0 && index < jenkinsFavorites.size()) {
            FavoriteJob job = jenkinsFavorites.remove(index);
            jenkinsFavorites.add(index - 1, job);
            // 重新设置顺序
            for (int i = 0; i < jenkinsFavorites.size(); i++) {
                jenkinsFavorites.get(i).setOrder(i);
            }
            saveJenkinsFavorites();
        }
    }

    /**
     * 下移收藏任务
     */
    public void moveFavoriteDown(int index) {
        if (index >= 0 && index < jenkinsFavorites.size() - 1) {
            FavoriteJob job = jenkinsFavorites.remove(index);
            jenkinsFavorites.add(index + 1, job);
            // 重新设置顺序
            for (int i = 0; i < jenkinsFavorites.size(); i++) {
                jenkinsFavorites.get(i).setOrder(i);
            }
            saveJenkinsFavorites();
        }
    }
    
    /**
     * 更新收藏任务的别名
     */
    public void updateJenkinsFavoriteAlias(String jobPath, String alias) {
        for (FavoriteJob job : jenkinsFavorites) {
            if (job.getJobPath().equals(jobPath)) {
                job.setAlias(alias);
                saveJenkinsFavorites();
                break;
            }
        }
    }

    /**
     * 保存 Jenkins 收藏数据到文件
     */
    private void saveJenkinsFavorites() {
        File file = new File(System.getProperty("user.home"), "gitviewer-jenkins-favorites.dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(jenkinsFavorites);
        } catch (IOException e) {
            System.err.println("Error saving Jenkins favorites: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 从文件加载 Jenkins 收藏数据
     */
    @SuppressWarnings("unchecked")
    private void loadJenkinsFavorites() {
        File file = new File(System.getProperty("user.home"), "gitviewer-jenkins-favorites.dat");
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                jenkinsFavorites = (List<FavoriteJob>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading Jenkins favorites: " + e.getMessage());
                jenkinsFavorites = new ArrayList<>();
            }
        }
    }

    // Portal 配置的 getter 和 setter
    public String getPortalUsername() {
        return portalUsername != null ? portalUsername : "";
    }

    public void setPortalUsername(String username) {
        this.portalUsername = username;
    }

    /**
     * 获取Portal密码（解密后）
     * Get Portal password (decrypted)
     * 
     * @return 解密后的密码
     */
    public String getPortalPassword() {
        if (portalPassword == null || portalPassword.isEmpty()) {
            return "";
        }
        
        // 解密密码
        String decrypted = PasswordEncryption.decrypt(portalPassword);
        return decrypted != null ? decrypted : "";
    }

    /**
     * 设置Portal密码（加密后存储）
     * Set Portal password (encrypted before storage)
     * 
     * @param password 明文密码
     */
    public void setPortalPassword(String password) {
        if (password == null || password.isEmpty()) {
            this.portalPassword = "";
            return;
        }
        
        // 加密密码
        String encrypted = PasswordEncryption.encrypt(password);
        this.portalPassword = encrypted != null ? encrypted : "";
    }

    public List<String> getPortalTenantCodes() {
        return portalTenantCodes != null ? new ArrayList<>(portalTenantCodes) : new ArrayList<>();
    }
    
    public String getPortalTenantCodesString() {
        // 从内存中的portalTenantCodes列表格式化为字符串
        return TenantCICDUtils.formatTenantCodes(portalTenantCodes);
    }

    public void setPortalTenantCodes(List<String> tenantCodes) {
        this.portalTenantCodes = tenantCodes != null ? new ArrayList<>(tenantCodes) : new ArrayList<>();
    }
    
    /**
     * 获取Portal收藏的应用列表（按租户）- 兼容旧版本
     * Get Portal favorite applications for a specific tenant - backward compatible
     * 
     * 注意：此方法用于向后兼容，新版本使用分组功能
     * 如果存在分组数据，则返回所有分组中的应用 + 未分组应用
     * 如果不存在分组数据，则返回旧格式的收藏列表
     * 
     * @param tenantCode 租户代码
     * @return 收藏的应用名称列表
     */
    public List<String> getPortalFavoriteApps(String tenantCode) {
        if (tenantCode == null || tenantCode.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 先尝试加载分组数据
        List<FavoriteGroup> groups = getPortalFavoriteGroups(tenantCode);
        List<String> ungrouped = getPortalUngroupedFavorites(tenantCode);
        
        // 如果有分组数据，返回所有应用
        if (!groups.isEmpty() || !ungrouped.isEmpty()) {
            List<String> allFavorites = new ArrayList<>();
            for (FavoriteGroup group : groups) {
                allFavorites.addAll(group.getAppNames());
            }
            allFavorites.addAll(ungrouped);
            return allFavorites;
        }
        
        // 否则加载旧格式数据
        Properties props = new Properties();
        File file = new File(System.getProperty("user.home"), SETTINGS_FILE);
        
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                String key = "portal.favorites." + tenantCode;
                String favoritesStr = props.getProperty(key, "");
                
                if (favoritesStr.isEmpty()) {
                    return new ArrayList<>();
                }
                
                // 解析逗号分隔的应用名称
                return TenantCICDUtils.parseTenantCodes(favoritesStr);
            } catch (IOException e) {
                System.err.println("Error loading Portal favorites: " + e.getMessage());
            }
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 设置Portal收藏的应用列表（按租户）- 兼容旧版本
     * Set Portal favorite applications for a specific tenant - backward compatible
     * 
     * 注意：此方法用于向后兼容，新版本使用分组功能
     * 调用此方法会清除分组数据，将所有应用放入未分组列表
     * 
     * @param tenantCode 租户代码
     * @param favoriteApps 收藏的应用名称列表
     */
    public void setPortalFavoriteApps(String tenantCode, List<String> favoriteApps) {
        if (tenantCode == null || tenantCode.isEmpty()) {
            return;
        }
        
        // 清除分组数据，将所有应用放入未分组列表
        setPortalFavoriteGroups(tenantCode, new ArrayList<>());
        setPortalUngroupedFavorites(tenantCode, favoriteApps != null ? favoriteApps : new ArrayList<>());
    }
    
    /**
     * 获取Portal收藏分组列表（按租户）
     * Get Portal favorite groups for a specific tenant
     * 
     * @param tenantCode 租户代码
     * @return 分组列表
     */
    public List<FavoriteGroup> getPortalFavoriteGroups(String tenantCode) {
        if (tenantCode == null || tenantCode.isEmpty()) {
            return new ArrayList<>();
        }
        
        Properties props = new Properties();
        File file = new File(System.getProperty("user.home"), SETTINGS_FILE);
        
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                String key = "portal.favorite.groups." + tenantCode;
                String groupsJson = props.getProperty(key, "");
                
                if (groupsJson.isEmpty()) {
                    System.out.println("[AppSettings] No groups found for tenant " + tenantCode);
                    return new ArrayList<>();
                }
                
                System.out.println("[AppSettings] Loading groups for tenant " + tenantCode);
                System.out.println("[AppSettings] JSON length: " + groupsJson.length() + " chars");
                
                // 解析JSON
                List<FavoriteGroup> groups = parseFavoriteGroupsJson(groupsJson);
                System.out.println("[AppSettings] Parsed " + groups.size() + " groups");
                
                return groups;
            } catch (IOException e) {
                System.err.println("[AppSettings] Error loading Portal favorite groups: " + e.getMessage());
                e.printStackTrace();
                
                // 尝试从备份恢复
                return tryLoadFromBackup(tenantCode);
            } catch (Exception e) {
                System.err.println("[AppSettings] Error parsing groups JSON: " + e.getMessage());
                e.printStackTrace();
                
                // JSON解析失败，尝试从备份恢复
                return tryLoadFromBackup(tenantCode);
            }
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 尝试从备份文件加载分组数据
     * Try to load groups from backup file
     */
    private List<FavoriteGroup> tryLoadFromBackup(String tenantCode) {
        System.out.println("[AppSettings] Attempting to load from backup...");
        
        File backupFile = new File(System.getProperty("user.home"), SETTINGS_FILE + ".backup");
        if (!backupFile.exists()) {
            System.err.println("[AppSettings] No backup file found");
            return new ArrayList<>();
        }
        
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(backupFile)) {
            props.load(fis);
            String key = "portal.favorite.groups." + tenantCode;
            String groupsJson = props.getProperty(key, "");
            
            if (!groupsJson.isEmpty()) {
                List<FavoriteGroup> groups = parseFavoriteGroupsJson(groupsJson);
                System.out.println("[AppSettings] Successfully loaded " + groups.size() + " groups from backup");
                
                // 恢复主文件
                File mainFile = new File(System.getProperty("user.home"), SETTINGS_FILE);
                restoreFromBackup(mainFile);
                
                return groups;
            }
        } catch (Exception e) {
            System.err.println("[AppSettings] Failed to load from backup: " + e.getMessage());
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 设置Portal收藏分组列表（按租户）
     * Set Portal favorite groups for a specific tenant
     * 
     * @param tenantCode 租户代码
     * @param groups 分组列表
     */
    public void setPortalFavoriteGroups(String tenantCode, List<FavoriteGroup> groups) {
        if (tenantCode == null || tenantCode.isEmpty()) {
            return;
        }
        
        Properties props = new Properties();
        File file = new File(System.getProperty("user.home"), SETTINGS_FILE);
        
        // 加载现有设置
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading settings: " + e.getMessage());
            }
        }
        
        // 更新分组列表
        String key = "portal.favorite.groups." + tenantCode;
        if (groups != null && !groups.isEmpty()) {
            String groupsJson = formatFavoriteGroupsJson(groups);
            props.setProperty(key, groupsJson);
            System.out.println("[AppSettings] Saving " + groups.size() + " groups for tenant " + tenantCode);
            System.out.println("[AppSettings] JSON length: " + groupsJson.length() + " chars");
        } else {
            props.remove(key);  // 如果列表为空，移除属性
            System.out.println("[AppSettings] Removing groups for tenant " + tenantCode + " (empty list)");
        }
        
        // 创建备份（在保存前）
        createBackup(file);
        
        // 保存设置
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "Git Info Viewer Settings");
            System.out.println("[AppSettings] Settings saved successfully");
            
            // 验证保存是否成功
            if (groups != null && !groups.isEmpty()) {
                verifyGroupsSaved(tenantCode, groups.size());
            }
        } catch (IOException e) {
            System.err.println("[AppSettings] ERROR saving Portal favorite groups: " + e.getMessage());
            e.printStackTrace();
            
            // 尝试从备份恢复
            restoreFromBackup(file);
        }
    }
    
    /**
     * 获取Portal未分组的收藏应用（按租户）
     * Get Portal ungrouped favorite applications for a specific tenant
     * 
     * @param tenantCode 租户代码
     * @return 未分组的应用名称列表
     */
    public List<String> getPortalUngroupedFavorites(String tenantCode) {
        if (tenantCode == null || tenantCode.isEmpty()) {
            return new ArrayList<>();
        }
        
        Properties props = new Properties();
        File file = new File(System.getProperty("user.home"), SETTINGS_FILE);
        
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                String key = "portal.ungrouped.favorites." + tenantCode;
                String ungroupedStr = props.getProperty(key, "");
                
                if (ungroupedStr.isEmpty()) {
                    return new ArrayList<>();
                }
                
                // 解析逗号分隔的应用名称
                return TenantCICDUtils.parseTenantCodes(ungroupedStr);
            } catch (IOException e) {
                System.err.println("Error loading Portal ungrouped favorites: " + e.getMessage());
            }
        }
        
        return new ArrayList<>();
    }
    
    /**
     * 设置Portal未分组的收藏应用（按租户）
     * Set Portal ungrouped favorite applications for a specific tenant
     * 
     * @param tenantCode 租户代码
     * @param ungroupedApps 未分组的应用名称列表
     */
    public void setPortalUngroupedFavorites(String tenantCode, List<String> ungroupedApps) {
        if (tenantCode == null || tenantCode.isEmpty()) {
            return;
        }
        
        Properties props = new Properties();
        File file = new File(System.getProperty("user.home"), SETTINGS_FILE);
        
        // 加载现有设置
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading settings: " + e.getMessage());
            }
        }
        
        // 更新未分组列表
        String key = "portal.ungrouped.favorites." + tenantCode;
        if (ungroupedApps != null && !ungroupedApps.isEmpty()) {
            String ungroupedStr = TenantCICDUtils.formatTenantCodes(ungroupedApps);
            props.setProperty(key, ungroupedStr);
        } else {
            props.remove(key);  // 如果列表为空，移除属性
        }
        
        // 保存设置
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "Git Info Viewer Settings");
        } catch (IOException e) {
            System.err.println("Error saving Portal ungrouped favorites: " + e.getMessage());
        }
    }
    
    /**
     * 解析分组JSON字符串
     * Parse favorite groups from JSON string
     * 
     * @param json JSON字符串
     * @return 分组列表
     */
    private List<FavoriteGroup> parseFavoriteGroupsJson(String json) {
        List<FavoriteGroup> groups = new ArrayList<>();
        
        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                org.json.JSONObject groupObj = jsonArray.getJSONObject(i);
                String name = groupObj.getString("name");
                boolean expanded = groupObj.optBoolean("expanded", true);
                
                List<String> appNames = new ArrayList<>();
                org.json.JSONArray appsArray = groupObj.getJSONArray("appNames");
                for (int j = 0; j < appsArray.length(); j++) {
                    appNames.add(appsArray.getString(j));
                }
                
                groups.add(new FavoriteGroup(name, appNames, expanded));
            }
        } catch (Exception e) {
            System.err.println("Error parsing favorite groups JSON: " + e.getMessage());
        }
        
        return groups;
    }
    
    /**
     * 格式化分组为JSON字符串
     * Format favorite groups to JSON string
     * 
     * @param groups 分组列表
     * @return JSON字符串
     */
    private String formatFavoriteGroupsJson(List<FavoriteGroup> groups) {
        try {
            org.json.JSONArray jsonArray = new org.json.JSONArray();
            for (FavoriteGroup group : groups) {
                org.json.JSONObject groupObj = new org.json.JSONObject();
                groupObj.put("name", group.getName());
                groupObj.put("expanded", group.isExpanded());
                groupObj.put("appNames", new org.json.JSONArray(group.getAppNames()));
                jsonArray.put(groupObj);
            }
            return jsonArray.toString();
        } catch (Exception e) {
            System.err.println("Error formatting favorite groups JSON: " + e.getMessage());
            return "[]";
        }
    }
    
    /**
     * 获取租户的版本代码模式
     * Get version code pattern for tenant
     * 
     * @param tenantCode 租户代码
     * @return 版本代码模式，如果未配置则返回空字符串
     */
    public String getPortalVersionPattern(String tenantCode) {
        if (tenantCode == null || tenantCode.isEmpty()) {
            return "";
        }
        
        Properties props = new Properties();
        File file = new File(System.getProperty("user.home"), SETTINGS_FILE);
        
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        String key = "portal.tenant." + tenantCode + ".versionPattern";
        return props.getProperty(key, "");
    }
    
    /**
     * 设置租户的版本代码模式
     * Set version code pattern for tenant
     * 
     * @param tenantCode 租户代码
     * @param pattern 版本代码模式（null或空字符串表示删除配置）
     */
    public void setPortalVersionPattern(String tenantCode, String pattern) {
        if (tenantCode == null || tenantCode.isEmpty()) {
            return;
        }
        
        Properties props = new Properties();
        File file = new File(System.getProperty("user.home"), SETTINGS_FILE);
        
        // 先加载现有配置
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        String key = "portal.tenant." + tenantCode + ".versionPattern";
        
        if (pattern == null || pattern.trim().isEmpty()) {
            // 删除配置
            props.remove(key);
            System.out.println("Removed version pattern for tenant: " + tenantCode);
        } else {
            // 保存配置
            props.setProperty(key, pattern.trim());
            System.out.println("Saved version pattern for tenant " + tenantCode + ": " + pattern.trim());
        }
        
        // 保存到文件
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "Git Viewer Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取租户的版本代码历史记录
     * Get version code history for tenant (newest first, max 20)
     */
    public List<String> getVersionCodeHistory(String tenantCode) {
        List<String> history = new ArrayList<>();
        if (tenantCode == null || tenantCode.isEmpty()) {
            return history;
        }

        Properties props = new Properties();
        File file = new File(System.getProperty("user.home"), SETTINGS_FILE);

        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String key = "portal.tenant." + tenantCode + ".versionHistory";
        String value = props.getProperty(key, "");
        if (!value.isEmpty()) {
            String[] parts = value.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    history.add(trimmed);
                }
            }
        }

        return history;
    }

    /**
     * 添加版本代码到历史记录
     * Add version code to history (dedup, prepend, trim to 20)
     */
    public void addVersionCodeHistory(String tenantCode, String versionCode) {
        if (tenantCode == null || tenantCode.isEmpty() || versionCode == null || versionCode.trim().isEmpty()) {
            return;
        }

        versionCode = versionCode.trim();

        Properties props = new Properties();
        File file = new File(System.getProperty("user.home"), SETTINGS_FILE);

        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String key = "portal.tenant." + tenantCode + ".versionHistory";
        List<String> history = new ArrayList<>();

        String value = props.getProperty(key, "");
        if (!value.isEmpty()) {
            for (String part : value.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && !trimmed.equals(versionCode)) {
                    history.add(trimmed);
                }
            }
        }

        history.add(0, versionCode);
        if (history.size() > 20) {
            history = history.subList(0, 20);
        }

        props.setProperty(key, String.join(",", history));

        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "Git Viewer Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 创建配置文件备份
     * Create backup of settings file
     */
    private void createBackup(File originalFile) {
        if (!originalFile.exists()) {
            return;
        }
        
        try {
            File backupFile = new File(originalFile.getParent(), SETTINGS_FILE + ".backup");
            java.nio.file.Files.copy(originalFile.toPath(), backupFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[AppSettings] Backup created: " + backupFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[AppSettings] Failed to create backup: " + e.getMessage());
        }
    }
    
    /**
     * 从备份恢复配置文件
     * Restore settings file from backup
     */
    private void restoreFromBackup(File originalFile) {
        File backupFile = new File(originalFile.getParent(), SETTINGS_FILE + ".backup");
        if (!backupFile.exists()) {
            System.err.println("[AppSettings] No backup file found");
            return;
        }
        
        try {
            java.nio.file.Files.copy(backupFile.toPath(), originalFile.toPath(), 
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[AppSettings] Restored from backup: " + backupFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[AppSettings] Failed to restore from backup: " + e.getMessage());
        }
    }
    
    /**
     * 验证分组数据是否成功保存
     * Verify that groups were saved successfully
     */
    private void verifyGroupsSaved(String tenantCode, int expectedCount) {
        try {
            // 重新读取并验证
            List<FavoriteGroup> savedGroups = getPortalFavoriteGroups(tenantCode);
            if (savedGroups.size() == expectedCount) {
                System.out.println("[AppSettings] Verification PASSED: " + savedGroups.size() + " groups saved correctly");
            } else {
                System.err.println("[AppSettings] Verification FAILED: Expected " + expectedCount + 
                    " groups, but found " + savedGroups.size());
            }
        } catch (Exception e) {
            System.err.println("[AppSettings] Verification ERROR: " + e.getMessage());
        }
    }
}

