package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Portal设置对话框
 * Configuration dialog for Portal credentials and tenant codes
 */
public class PortalSettingsDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(PortalSettingsDialog.class);
    
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField tenantCodesField;
    private JButton saveButton;
    private JButton cancelButton;
    
    /**
     * 构造函数
     * 
     * @param parent 父窗口
     */
    public PortalSettingsDialog(Frame parent) {
        super(parent, "Portal Settings", true);
        logger.info("Opening Portal Settings Dialog");
        
        initializeUI();
        loadSettings();
        
        setLocationRelativeTo(parent);
    }
    
    /**
     * 资源清理
     * Resource cleanup
     */
    @Override
    public void dispose() {
        logger.debug("Disposing Portal Settings Dialog");
        
        // 清除密码字段（安全考虑）
        if (passwordField != null) {
            passwordField.setText("");
        }
        
        super.dispose();
    }
    
    /**
     * 初始化UI
     * Initialize UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(650, 350);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);
        
        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));
        mainPanel.setBackground(Color.WHITE);
        
        // 标题
        JLabel titleLabel = new JLabel("Portal Configuration");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        
        // 说明文本
        JTextArea descriptionArea = new JTextArea(
            "Configure your Portal credentials and tenant codes. " +
            "Multiple tenant codes can be separated by commas. " +
            "Use format tenant{sub1/sub2} to specify sub-tenant codes (workspaces)."
        );
        descriptionArea.setEditable(false);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setBackground(new Color(240, 248, 255));
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        descriptionArea.setMaximumSize(new Dimension(550, 60));
        descriptionArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(descriptionArea);
        mainPanel.add(Box.createVerticalStrut(20));
        
        // 创建输入表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Username 标签
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(usernameLabel, gbc);
        
        // Username 输入框
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        usernameField.setPreferredSize(new Dimension(400, 28));
        usernameField.setToolTipText("Enter your Portal username");
        formPanel.add(usernameField, gbc);
        
        // Password 标签
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(passwordLabel, gbc);
        
        // Password 输入框
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Consolas", Font.PLAIN, 11));
        passwordField.setPreferredSize(new Dimension(400, 28));
        passwordField.setToolTipText("Enter your Portal password");
        formPanel.add(passwordField, gbc);
        
        // Tenant Codes 标签
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel tenantCodesLabel = new JLabel("Tenant Codes:");
        tenantCodesLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(tenantCodesLabel, gbc);
        
        // Tenant Codes 输入框
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        tenantCodesField = new JTextField();
        tenantCodesField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tenantCodesField.setPreferredSize(new Dimension(400, 28));
        tenantCodesField.setToolTipText("Enter tenant codes (comma-separated, e.g., thailife,tenant2 or tenant{sub1/sub2},tenant2)");
        formPanel.add(tenantCodesField, gbc);
        
        // Tenant Codes 提示文本
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel tenantCodesHint = new JLabel("<html><i>Format: tenant1,tenant2 or tenant{sub1/sub2},tenant2</i></html>");
        tenantCodesHint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        tenantCodesHint.setForeground(Color.GRAY);
        formPanel.add(tenantCodesHint, gbc);
        
        mainPanel.add(formPanel);
        mainPanel.add(Box.createVerticalStrut(20));
        
        add(mainPanel, BorderLayout.CENTER);
        
        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 15));
        buttonPanel.setBackground(Color.WHITE);
        
        saveButton = new JButton("Save");
        saveButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveButton.setPreferredSize(new Dimension(100, 35));
        saveButton.setBackground(new Color(66, 133, 244));
        saveButton.setForeground(Color.WHITE);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveButton.addActionListener(e -> handleSave());
        
        cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.setBackground(new Color(95, 99, 104));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> {
            logger.info("Portal Settings Dialog cancelled");
            dispose();
        });
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // 设置回车键保存
        getRootPane().setDefaultButton(saveButton);
    }
    
    /**
     * 递归应用字体到所有组件
     * Apply font recursively to all components
     */
    private void applyFontRecursive(Container container, Font font) {
        for (Component comp : container.getComponents()) {
            comp.setFont(font);
            if (comp instanceof Container) {
                applyFontRecursive((Container) comp, font);
            }
        }
    }
    
    /**
     * 加载设置
     * Load settings from AppSettings
     */
    private void loadSettings() {
        logger.debug("Loading Portal settings");
        
        AppSettings settings = AppSettings.getInstance();
        
        usernameField.setText(settings.getPortalUsername());
        passwordField.setText(settings.getPortalPassword());
        
        List<String> tenantCodes = settings.getPortalTenantCodes();
        String tenantCodesStr = TenantCICDUtils.formatTenantCodes(tenantCodes);
        tenantCodesField.setText(tenantCodesStr);
        
        logger.debug("Loaded settings: username={}, tenantCodes count={}", 
                    settings.getPortalUsername(), tenantCodes.size());
    }
    
    /**
     * 处理保存操作
     * Handle save action
     */
    private void handleSave() {
        logger.info("Saving Portal settings");
        
        // 验证输入
        if (!validateInput()) {
            return;
        }
        
        // 获取输入值
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String tenantCodesStr = tenantCodesField.getText().trim();
        
        // 解析tenant codes
        List<String> tenantCodes = TenantCICDUtils.parseTenantCodes(tenantCodesStr);
        
        // 保存到AppSettings
        AppSettings settings = AppSettings.getInstance();
        settings.setPortalUsername(username);
        settings.setPortalPassword(password);  // 会自动加密
        settings.setPortalTenantCodes(tenantCodes);
        settings.saveSettings();
        
        logger.info("Portal settings saved successfully: username={}, tenantCodes count={}", 
                   username, tenantCodes.size());
        
        JOptionPane.showMessageDialog(this,
            "Portal settings saved successfully",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
        
        dispose();
    }
    
    /**
     * 验证输入
     * Validate input
     * 
     * @return true if valid, false otherwise
     */
    private boolean validateInput() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty()) {
            logger.warn("Validation failed: username is empty");
            JOptionPane.showMessageDialog(this,
                "Username cannot be empty",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            usernameField.requestFocus();
            return false;
        }
        
        if (password.isEmpty()) {
            logger.warn("Validation failed: password is empty");
            JOptionPane.showMessageDialog(this,
                "Password cannot be empty",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            passwordField.requestFocus();
            return false;
        }
        
        logger.debug("Input validation passed");
        return true;
    }
}
