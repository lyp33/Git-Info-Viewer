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
}
