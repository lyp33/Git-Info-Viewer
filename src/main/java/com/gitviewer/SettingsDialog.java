package com.gitviewer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 设置对话框
 */
public class SettingsDialog extends JDialog {

    private JComboBox<String> leftFontNameCombo;
    private JComboBox<String> leftFontStyleCombo;
    private JComboBox<Integer> leftFontSizeCombo;

    private JComboBox<String> rightFontNameCombo;
    private JComboBox<String> rightFontStyleCombo;
    private JComboBox<Integer> rightFontSizeCombo;

    // 可用字体
    private static final String[] FONT_NAMES = {
            "Arial", "Segoe UI", "Microsoft YaHei", "SimSun", "Consolas",
            "Courier New", "Tahoma", "Verdana", "Helvetica", "Times New Roman"
    };

    private static final String[] FONT_STYLES = {
            "Plain", "Bold", "Italic", "Bold Italic"
    };

    private static final Integer[] FONT_SIZES = {
            10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24
    };

    public SettingsDialog(Frame parent) {
        super(parent, "Settings", true);
        initializeUI();
        loadCurrentSettings();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(500, 450);

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 左侧面板字体设置
        JPanel leftPanel = createFontSettingsPanel("Left Panel (Tree)", true);
        mainPanel.add(leftPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // 右侧面板字体设置
        JPanel rightPanel = createFontSettingsPanel("Right Panel (Info)", false);
        mainPanel.add(rightPanel);

        add(mainPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");
        JButton applyButton = new JButton("Switch");

        okButton.addActionListener(e -> {
            applySettings();
            saveSettings();
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        applyButton.addActionListener(e -> {
            applySettings();
            saveSettings();
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(applyButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // 所有组件创建完成后，设置对话框的默认字体为 11 号
        Font defaultFont = new Font("Segoe UI", Font.PLAIN, 11);
        applyFontRecursive(this, defaultFont);
    }

    private JPanel createFontSettingsPanel(String title, boolean isLeft) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // 创建带标题的边框，并设置标题字体
        TitledBorder border = BorderFactory.createTitledBorder(title);
        border.setTitleFont(new Font("Segoe UI", Font.PLAIN, 10)); // 更小的标题字体
        panel.setBorder(border);

        // 字体名称
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        namePanel.add(new JLabel("Font:"));
        JComboBox<String> fontNameCombo = new JComboBox<>(FONT_NAMES);
        fontNameCombo.setPreferredSize(new Dimension(200, 25));
        namePanel.add(fontNameCombo);
        panel.add(namePanel);

        // 字体样式
        JPanel stylePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stylePanel.add(new JLabel("Style:"));
        JComboBox<String> fontStyleCombo = new JComboBox<>(FONT_STYLES);
        fontStyleCombo.setPreferredSize(new Dimension(200, 25));
        stylePanel.add(fontStyleCombo);
        panel.add(stylePanel);

        // 字体大小
        JPanel sizePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sizePanel.add(new JLabel("Size:"));
        JComboBox<Integer> fontSizeCombo = new JComboBox<>(FONT_SIZES);
        fontSizeCombo.setPreferredSize(new Dimension(200, 25));
        fontSizeCombo.setSelectedItem(12); // 默认选中 12
        sizePanel.add(fontSizeCombo);
        panel.add(sizePanel);

        // 保存引用
        if (isLeft) {
            leftFontNameCombo = fontNameCombo;
            leftFontStyleCombo = fontStyleCombo;
            leftFontSizeCombo = fontSizeCombo;
        } else {
            rightFontNameCombo = fontNameCombo;
            rightFontStyleCombo = fontStyleCombo;
            rightFontSizeCombo = fontSizeCombo;
        }

        return panel;
    }

    private void loadCurrentSettings() {
        AppSettings settings = AppSettings.getInstance();

        // 加载左侧面板字体
        Font leftFont = settings.getLeftPanelFont();
        leftFontNameCombo.setSelectedItem(leftFont.getName());
        leftFontStyleCombo.setSelectedIndex(leftFont.getStyle());
        leftFontSizeCombo.setSelectedItem(leftFont.getSize());

        // 加载右侧面板字体
        Font rightFont = settings.getRightPanelFont();
        rightFontNameCombo.setSelectedItem(rightFont.getName());
        rightFontStyleCombo.setSelectedIndex(rightFont.getStyle());
        rightFontSizeCombo.setSelectedItem(rightFont.getSize());
    }

    private Font getFontFromCombo(boolean isLeft) {
        String name = isLeft ?
                (String) leftFontNameCombo.getSelectedItem() :
                (String) rightFontNameCombo.getSelectedItem();

        int style = isLeft ?
                leftFontStyleCombo.getSelectedIndex() :
                rightFontStyleCombo.getSelectedIndex();

        int size = isLeft ?
                (Integer) leftFontSizeCombo.getSelectedItem() :
                (Integer) rightFontSizeCombo.getSelectedItem();

        return new Font(name, style, size);
    }

    private void applySettings() {
        AppSettings settings = AppSettings.getInstance();

        Font leftFont = getFontFromCombo(true);
        Font rightFont = getFontFromCombo(false);

        settings.setLeftPanelFont(leftFont);
        settings.setRightPanelFont(rightFont);

        // 通知所有监听器
        settings.notifyFontChanged();
    }

    private void saveSettings() {
        AppSettings.getInstance().saveSettings();
    }

    /**
     * 递归设置容器及其所有子组件的字体
     */
    private void applyFontRecursive(Container container, Font font) {
        for (Component comp : container.getComponents()) {
            comp.setFont(font);
            if (comp instanceof Container) {
                applyFontRecursive((Container) comp, font);
            }
        }
    }
}
