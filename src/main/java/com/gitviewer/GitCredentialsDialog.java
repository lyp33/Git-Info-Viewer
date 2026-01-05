package com.gitviewer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

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
    private JLabel statusLabel;
    private JButton testButton;
    private JButton okButton;
    private boolean confirmed = false;
    private boolean credentialsTested = false;
    private String username;
    private String password;
    private String repositoryUrl;
    
    // 静态变量保存认证信息（会话期间有效）
    private static String savedUsername = null;
    private static String savedPassword = null;
    private static boolean rememberCredentials = false;

    public GitCredentialsDialog(Frame parent, String repositoryUrl) {
        super(parent, "Git Authentication Required", true);
        this.repositoryUrl = repositoryUrl;
        initializeUI(repositoryUrl);
        setLocationRelativeTo(parent);
    }

    private void initializeUI(String repositoryUrl) {
        setLayout(new BorderLayout(10, 10));
        setSize(500, 380);

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
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(usernameLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        if (savedUsername != null) {
            usernameField.setText(savedUsername);
        }
        formPanel.add(usernameField, gbc);

        // 密码/Token
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JLabel passwordLabel = new JLabel("Password/Token:");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(passwordLabel, gbc);
        
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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

        // Test 按钮
        gbc.gridx = 1; gbc.gridy = 3; gbc.anchor = GridBagConstraints.WEST;
        testButton = new JButton("Test Connection");
        testButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        testButton.setBackground(new Color(25, 84, 166));
        testButton.setForeground(Color.WHITE);
        testButton.setFocusPainted(false);
        testButton.setBorderPainted(false);
        testButton.setOpaque(true);
        testButton.addActionListener(e -> testCredentials());
        formPanel.add(testButton, gbc);

        // 状态标签
        gbc.gridx = 1; gbc.gridy = 4; gbc.anchor = GridBagConstraints.WEST;
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        formPanel.add(statusLabel, gbc);

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
        
        okButton = new JButton("OK");
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        okButton.setBackground(new Color(25, 84, 166));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setBorderPainted(false);
        okButton.setOpaque(true);
        okButton.setEnabled(false); // 默认禁用，需要先测试
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

        // 焦点设置
        if (usernameField.getText().isEmpty()) {
            usernameField.requestFocus();
        } else {
            passwordField.requestFocus();
        }
        
        // 输入变化时重置测试状态
        usernameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { resetTestStatus(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { resetTestStatus(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { resetTestStatus(); }
        });
        passwordField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { resetTestStatus(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { resetTestStatus(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { resetTestStatus(); }
        });
    }
    
    private void resetTestStatus() {
        credentialsTested = false;
        okButton.setEnabled(false);
        statusLabel.setText(" ");
    }
    
    private void testCredentials() {
        String testUsername = usernameField.getText().trim();
        String testPassword = new String(passwordField.getPassword());
        
        if (testUsername.isEmpty() || testPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter both username and password/token.", 
                "Missing Information", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        testButton.setEnabled(false);
        statusLabel.setText("Testing...");
        statusLabel.setForeground(new Color(95, 99, 104));
        
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;
            
            @Override
            protected Boolean doInBackground() {
                try {
                    CredentialsProvider cp = new UsernamePasswordCredentialsProvider(testUsername, testPassword);
                    Git.lsRemoteRepository()
                        .setRemote(repositoryUrl)
                        .setCredentialsProvider(cp)
                        .setHeads(true)
                        .call();
                    return true;
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    return false;
                }
            }
            
            @Override
            protected void done() {
                testButton.setEnabled(true);
                try {
                    if (get()) {
                        credentialsTested = true;
                        okButton.setEnabled(true);
                        statusLabel.setText("✓ Connection successful!");
                        statusLabel.setForeground(new Color(76, 175, 80));
                    } else {
                        credentialsTested = false;
                        okButton.setEnabled(false);
                        statusLabel.setText("✗ Authentication failed");
                        statusLabel.setForeground(Color.RED);
                    }
                } catch (Exception e) {
                    statusLabel.setText("✗ Test failed");
                    statusLabel.setForeground(Color.RED);
                }
            }
        };
        worker.execute();
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