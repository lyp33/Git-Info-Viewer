package com.gitviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Git认证对话框
 * 用于输入用户名和密码/Token
 */
public class GitCredentialsDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox rememberCheckBox;
    private boolean confirmed = false;
    private String username;
    private String password;
    
    // 静态变量保存认证信息（会话期间有效）
    private static String savedUsername = null;
    private static String savedPassword = null;
    private static boolean rememberCredentials = false;

    public GitCredentialsDialog(Frame parent, String repositoryUrl) {
        super(parent, "Git Authentication Required", true);
        initializeUI(repositoryUrl);
        setLocationRelativeTo(parent);
    }

    private void initializeUI(String repositoryUrl) {
        setLayout(new BorderLayout(10, 10));
        setSize(450, 280);

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 标题和说明
        JLabel titleLabel = new JLabel("Authentication Required");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel urlLabel = new JLabel("Repository: " + repositoryUrl);
        urlLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        urlLabel.setForeground(new Color(95, 99, 104));
        urlLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel infoLabel = new JLabel("<html><div style='text-align: center;'>" +
                "Please enter your Git credentials.<br>" +
                "For GitLab/GitHub, you can use Personal Access Token as password." +
                "</div></html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoLabel.setForeground(new Color(95, 99, 104));
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(urlLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(infoLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // 输入表单
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // 用户名
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
        formPanel.add(new JLabel("Username:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        if (savedUsername != null) {
            usernameField.setText(savedUsername);
        }
        formPanel.add(usernameField, gbc);

        // 密码/Token
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("Password/Token:"), gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        if (savedPassword != null && rememberCredentials) {
            passwordField.setText(savedPassword);
        }
        formPanel.add(passwordField, gbc);

        // 记住密码选项
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        rememberCheckBox = new JCheckBox("Remember credentials for this session");
        rememberCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rememberCheckBox.setSelected(rememberCredentials);
        formPanel.add(rememberCheckBox, gbc);

        mainPanel.add(formPanel);

        add(mainPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
        
        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        okButton.setBackground(new Color(66, 133, 244));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.addActionListener(e -> {
            username = usernameField.getText().trim();
            password = new String(passwordField.getPassword());
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter both username and password/token.", 
                    "Missing Information", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // 保存认证信息（如果选择记住）
            if (rememberCheckBox.isSelected()) {
                savedUsername = username;
                savedPassword = password;
                rememberCredentials = true;
            }
            
            confirmed = true;
            dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 设置默认按钮和回车键处理
        getRootPane().setDefaultButton(okButton);
        
        // 回车键确认
        KeyStroke enterStroke = KeyStroke.getKeyStroke("ENTER");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(enterStroke, "ENTER");
        getRootPane().getActionMap().put("ENTER", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okButton.doClick();
            }
        });

        // 焦点设置
        if (usernameField.getText().isEmpty()) {
            usernameField.requestFocus();
        } else {
            passwordField.requestFocus();
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    /**
     * 清除保存的认证信息
     */
    public static void clearSavedCredentials() {
        savedUsername = null;
        savedPassword = null;
        rememberCredentials = false;
    }

    /**
     * 检查是否有保存的认证信息
     */
    public static boolean hasSavedCredentials() {
        return rememberCredentials && savedUsername != null && savedPassword != null;
    }

    /**
     * 获取保存的用户名
     */
    public static String getSavedUsername() {
        return savedUsername;
    }

    /**
     * 获取保存的密码
     */
    public static String getSavedPassword() {
        return savedPassword;
    }
}