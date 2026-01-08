package com.gitviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * GitLab 认证设置对话框
 * 用于配置 GitLab Private Token 和 Username/Password
 */
public class GitLabSettingsDialog extends JDialog {

    private JTextField privateTokenField;
    private JTextField usernameField;
    private JPasswordField passwordField;

    public GitLabSettingsDialog(Frame parent) {
        super(parent, "GitLab Authentication Settings", true);
        initializeUI();
        loadCurrentSettings();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(650, 350);
        setResizable(false);

        // 主面板 - 使用居中对齐
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));

        // 标题 - 居中
        JLabel titleLabel = new JLabel("GitLab Authentication Configuration");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        // 说明文本 - 居中
        JTextArea descriptionArea = new JTextArea(
            "Configure your GitLab authentication credentials. " +
            "The Private Token will be used for authentication."
        );
        descriptionArea.setEditable(false);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setBackground(new Color(240, 248, 255));
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        descriptionArea.setMaximumSize(new Dimension(550, 50));
        descriptionArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(descriptionArea);
        mainPanel.add(Box.createVerticalStrut(20));

        // 创建输入表单面板 - 使用GridBagLayout实现更好的对齐
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Private Token 标签
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel tokenLabel = new JLabel("Private Token:");
        tokenLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(tokenLabel, gbc);

        // Private Token 输入框 - 更长以容纳长token
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        privateTokenField = new JTextField();
        privateTokenField.setFont(new Font("Consolas", Font.PLAIN, 11)); // 使用等宽字体便于查看token
        privateTokenField.setPreferredSize(new Dimension(450, 28));
        privateTokenField.setToolTipText("Enter your GitLab Private Token");
        formPanel.add(privateTokenField, gbc);

        // Username 标签
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(usernameLabel, gbc);

        // Username 输入框
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        usernameField.setPreferredSize(new Dimension(300, 28));
        usernameField.setToolTipText("Enter your GitLab username (optional)");
        formPanel.add(usernameField, gbc);

        // Password 标签
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(passwordLabel, gbc);

        // Password 输入框
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        passwordField.setPreferredSize(new Dimension(300, 28));
        passwordField.setToolTipText("Enter your GitLab password (optional)");
        formPanel.add(passwordField, gbc);

        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // 提示信息 - 居中
        JLabel hintLabel = new JLabel("<html><div style='text-align: center;'>Note: All credentials are saved locally in your user directory.</div></html>");
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        hintLabel.setForeground(new Color(100, 100, 100));
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(hintLabel);

        add(mainPanel, BorderLayout.CENTER);

        // 按钮面板 - 居中对齐
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton okButton = createStyledButton("OK", new Color(25, 84, 166));
        JButton cancelButton = createStyledButton("Cancel", new Color(100, 100, 100));
        JButton clearButton = createStyledButton("Clear All", new Color(200, 80, 80));

        okButton.addActionListener(e -> {
            saveSettings();
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        clearButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to clear all GitLab credentials?",
                "Confirm Clear",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                privateTokenField.setText("");
                usernameField.setText("");
                passwordField.setText("");
                saveSettings();
                JOptionPane.showMessageDialog(
                    this,
                    "GitLab credentials have been cleared.",
                    "Cleared",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(clearButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // 设置默认字体
        Font defaultFont = new Font("Segoe UI", Font.PLAIN, 11);
        applyFontRecursive(this, defaultFont);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(100, 30));
        return button;
    }

    private void loadCurrentSettings() {
        AppSettings settings = AppSettings.getInstance();
        privateTokenField.setText(settings.getGitLabPrivateToken());
        usernameField.setText(settings.getGitLabUsername());
        passwordField.setText(settings.getGitLabPassword());
    }

    private void saveSettings() {
        AppSettings settings = AppSettings.getInstance();
        settings.setGitLabPrivateToken(privateTokenField.getText().trim());
        settings.setGitLabUsername(usernameField.getText().trim());
        settings.setGitLabPassword(new String(passwordField.getPassword()).trim());
        settings.saveSettings();

        JOptionPane.showMessageDialog(
            this,
            "GitLab authentication settings have been saved.",
            "Settings Saved",
            JOptionPane.INFORMATION_MESSAGE
        );
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
