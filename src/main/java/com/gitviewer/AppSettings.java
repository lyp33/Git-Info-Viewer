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

    // 目录历史记录（最多保存5条）
    private List<String> directoryHistory;
    private static final int MAX_HISTORY_SIZE = 5;

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

                // 加载目录历史记录
                directoryHistory.clear();
                for (int i = 0; i < MAX_HISTORY_SIZE; i++) {
                    String historyPath = props.getProperty("directory.history." + i, "");
                    if (!historyPath.isEmpty()) {
                        directoryHistory.add(historyPath);
                    }
                }

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

            // 保存目录历史记录
            for (int i = 0; i < directoryHistory.size(); i++) {
                props.setProperty("directory.history." + i, directoryHistory.get(i));
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
}
