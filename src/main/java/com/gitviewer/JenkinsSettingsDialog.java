package com.gitviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Jenkins 设置对话框
 * 用于配置 Jenkins 服务器连接信息
 */
public class JenkinsSettingsDialog extends JDialog {

    private JTextField urlField;
    private JTextField usernameField;
    private JPasswordField apiTokenField;
    private JTextField defaultJobPathField;

    public JenkinsSettingsDialog(Frame parent) {
        super(parent, "Jenkins Settings", true);
        initializeUI();
        loadCurrentSettings();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(650, 400);
        setResizable(false);

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));

        // 标题
        JLabel titleLabel = new JLabel("Jenkins Server Configuration");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        // 说明文本
        JTextArea descriptionArea = new JTextArea(
            "Configure your Jenkins server connection. " +
            "The API token can be generated from your Jenkins user profile."
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

        // 创建输入表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Jenkins URL 标签
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel urlLabel = new JLabel("Jenkins URL:");
        urlLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(urlLabel, gbc);

        // Jenkins URL 输入框
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        urlField = new JTextField();
        urlField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        urlField.setPreferredSize(new Dimension(450, 28));
        urlField.setToolTipText("Enter Jenkins server URL (e.g., http://172.25.32.166:8080)");
        formPanel.add(urlField, gbc);

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
        usernameField.setToolTipText("Enter your Jenkins username");
        formPanel.add(usernameField, gbc);

        // API Token 标签
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel apiTokenLabel = new JLabel("API Token:");
        apiTokenLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(apiTokenLabel, gbc);

        // API Token 输入框
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        apiTokenField = new JPasswordField();
        apiTokenField.setFont(new Font("Consolas", Font.PLAIN, 11));
        apiTokenField.setPreferredSize(new Dimension(450, 28));
        apiTokenField.setToolTipText("Enter your Jenkins API token");
        formPanel.add(apiTokenField, gbc);

        // Default Job Path 标签
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel jobPathLabel = new JLabel("Default Job Path:");
        jobPathLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(jobPathLabel, gbc);

        // Default Job Path 输入框
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        defaultJobPathField = new JTextField();
        defaultJobPathField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        defaultJobPathField.setPreferredSize(new Dimension(300, 28));
        defaultJobPathField.setToolTipText("Enter default job path (e.g., job/gemini)");
        formPanel.add(defaultJobPathField, gbc);

        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        // Test Connection 按钮
        JButton testButton = new JButton("Test Connection");
        testButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        testButton.addActionListener(this::testConnection);
        buttonPanel.add(testButton);

        // Save 按钮
        JButton saveButton = new JButton("Save");
        saveButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        saveButton.addActionListener(this::saveSettings);
        buttonPanel.add(saveButton);

        // Cancel 按钮
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 加载当前设置
     */
    private void loadCurrentSettings() {
        AppSettings settings = AppSettings.getInstance();
        urlField.setText(settings.getJenkinsUrl());
        usernameField.setText(settings.getJenkinsUsername());
        apiTokenField.setText(settings.getJenkinsApiToken());
        defaultJobPathField.setText(settings.getJenkinsDefaultJobPath());
    }

    /**
     * 测试连接
     */
    private void testConnection(ActionEvent e) {
        String url = urlField.getText().trim();
        String username = usernameField.getText().trim();
        String apiToken = new String(apiTokenField.getPassword()).trim();

        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter Jenkins URL",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 验证 URL 格式
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            JOptionPane.showMessageDialog(this,
                "Invalid Jenkins URL format. Please use http:// or https://",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 测试连接
        try {
            JenkinsApiClient client = new JenkinsApiClient(url, username, apiToken);
            String defaultPath = defaultJobPathField.getText().trim();
            
            // 如果 defaultPath 为空，使用 "job/gemini" 进行测试（但不保存）
            String testPath = defaultPath.isEmpty() ? "job/gemini" : defaultPath;
            
            // 尝试获取作业层次结构
            client.fetchJobHierarchy(testPath);
            
            JOptionPane.showMessageDialog(this,
                "Connection successful!",
                "Test Connection",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Connection failed: " + ex.getMessage(),
                "Test Connection",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 保存设置
     */
    private void saveSettings(ActionEvent e) {
        String url = urlField.getText().trim();
        String username = usernameField.getText().trim();
        String apiToken = new String(apiTokenField.getPassword()).trim();
        String defaultJobPath = defaultJobPathField.getText().trim();

        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter Jenkins URL",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 验证 URL 格式
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            JOptionPane.showMessageDialog(this,
                "Invalid Jenkins URL format. Please use http:// or https://",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 保存设置（允许 defaultJobPath 为空）
        AppSettings settings = AppSettings.getInstance();
        settings.setJenkinsUrl(url);
        settings.setJenkinsUsername(username);
        settings.setJenkinsApiToken(apiToken);
        settings.setJenkinsDefaultJobPath(defaultJobPath); // 允许为空
        settings.saveSettings();

        JOptionPane.showMessageDialog(this,
            "Settings saved successfully!",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);

        dispose();
    }
}
