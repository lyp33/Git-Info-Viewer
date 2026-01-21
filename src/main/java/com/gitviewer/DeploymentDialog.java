package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 部署对话框
 * Deployment dialog for deploying images to workspaces and environments
 */
public class DeploymentDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(DeploymentDialog.class);
    
    // UI Components
    private JTextArea imageListTextArea;
    private JComboBox<String> workspaceComboBox;
    private JComboBox<String> environmentComboBox;
    private JTextArea consoleLogArea;
    private JButton deployButton;
    private JButton closeButton;
    
    // Data
    private PortalApiClient apiClient;
    private String mainTenantToken;
    private String mainTenantCode;
    private String workspaceToken;
    private Map<String, List<String>> tenantSubTenantMap;
    
    // SwingWorker reference for cancellation
    private SwingWorker<?, ?> currentWorker;
    
    /**
     * 构造函数
     * 
     * @param parent 父窗口
     * @param apiClient Portal API客户端
     * @param mainToken 主租户Token
     * @param mainTenant 主租户代码
     * @param selectedImages 预选的镜像列表
     */
    public DeploymentDialog(Frame parent, PortalApiClient apiClient, String mainToken, 
                           String mainTenant, List<String> selectedImages) {
        super(parent, "Deployment", true);
        logger.info("Opening Deployment Dialog for tenant: {}", mainTenant);
        
        this.apiClient = apiClient;
        this.mainTenantToken = mainToken;
        this.mainTenantCode = mainTenant;
        this.workspaceToken = null;
        
        initializeUI();
        loadWorkspaceList();
        
        // 预填充镜像列表
        if (selectedImages != null && !selectedImages.isEmpty()) {
            String imageText = String.join("\n", selectedImages);
            imageListTextArea.setText(imageText);
            logger.info("Pre-filled {} images", selectedImages.size());
        }
        
        setSize(700, 800);
        setMinimumSize(new Dimension(600, 700));
        setLocationRelativeTo(parent);
    }
    
    /**
     * 初始化UI
     * Initialize UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);
        
        // 顶部：镜像列表
        mainPanel.add(createImageListPanel(), BorderLayout.NORTH);
        
        // 中间：配置面板（workspace + environment）
        mainPanel.add(createConfigurationPanel(), BorderLayout.CENTER);
        
        // 底部：控制台日志
        mainPanel.add(createConsoleLogPanel(), BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // 按钮面板
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * 创建镜像列表面板
     * Create image list panel
     */
    private JPanel createImageListPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel("Image List:");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        imageListTextArea = new JTextArea(8, 50);
        imageListTextArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        imageListTextArea.setLineWrap(false);
        imageListTextArea.setWrapStyleWord(false);
        imageListTextArea.setToolTipText("Enter image names (one per line)");
        
        JScrollPane scrollPane = new JScrollPane(imageListTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建配置面板
     * Create configuration panel (workspace + environment)
     */
    private JPanel createConfigurationPanel() {
        JPanel outerPanel = new JPanel(new BorderLayout(5, 5));
        outerPanel.setBackground(Color.WHITE);
        
        // 标题标签（加粗）
        JLabel titleLabel = new JLabel("Deployment Configuration");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
        outerPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 配置内容面板
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridy = 0;
        
        // Workspace 标签
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        JLabel workspaceLabel = new JLabel("Workspace:");
        workspaceLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        panel.add(workspaceLabel, gbc);
        
        // Workspace 下拉框
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        workspaceComboBox = new JComboBox<>();
        workspaceComboBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        workspaceComboBox.setPreferredSize(new Dimension(250, 30));
        workspaceComboBox.addActionListener(e -> handleWorkspaceSelection());
        panel.add(workspaceComboBox, gbc);
        
        // Environment 标签
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(5, 20, 5, 5);
        JLabel environmentLabel = new JLabel("Environment:");
        environmentLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        panel.add(environmentLabel, gbc);
        
        // Environment 下拉框
        gbc.gridx = 3;
        gbc.weightx = 0.5;
        gbc.insets = new Insets(5, 5, 5, 5);
        environmentComboBox = new JComboBox<>();
        environmentComboBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        environmentComboBox.setPreferredSize(new Dimension(250, 30));
        environmentComboBox.setEnabled(false);
        panel.add(environmentComboBox, gbc);
        
        outerPanel.add(panel, BorderLayout.CENTER);
        
        return outerPanel;
    }
    
    /**
     * 创建控制台日志面板
     * Create console log panel
     */
    private JPanel createConsoleLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel("Console Log:");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // 黑色背景 + 白色字体的终端风格
        consoleLogArea = new JTextArea(10, 50);
        consoleLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        consoleLogArea.setBackground(Color.BLACK);
        consoleLogArea.setForeground(Color.WHITE);
        consoleLogArea.setCaretColor(Color.WHITE);
        consoleLogArea.setEditable(false);
        consoleLogArea.setLineWrap(true);
        consoleLogArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(consoleLogArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new Dimension(600, 250));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 2));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建按钮面板
     * Create button panel
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(Color.WHITE);
        
        // Deployment Log 按钮 - 蓝色
        JButton deploymentLogButton = new JButton("<html><font color='white'><b>Deployment Log</b></font></html>");
        deploymentLogButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        deploymentLogButton.setPreferredSize(new Dimension(150, 35));
        deploymentLogButton.setBackground(new Color(66, 133, 244));
        deploymentLogButton.setForeground(Color.WHITE);
        deploymentLogButton.setOpaque(true);
        deploymentLogButton.setContentAreaFilled(true);
        deploymentLogButton.setFocusPainted(false);
        deploymentLogButton.setBorderPainted(false);
        deploymentLogButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deploymentLogButton.addActionListener(e -> handleDeploymentLog());
        panel.add(deploymentLogButton);
        
        // Deploy 按钮
        deployButton = new JButton("<html><font color='white'><b>Deploy</b></font></html>");
        deployButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        deployButton.setPreferredSize(new Dimension(120, 35));
        deployButton.setBackground(new Color(52, 168, 83));
        deployButton.setForeground(Color.WHITE);
        deployButton.setOpaque(true);
        deployButton.setContentAreaFilled(true);
        deployButton.setFocusPainted(false);
        deployButton.setBorderPainted(false);
        deployButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deployButton.addActionListener(e -> handleDeploy());
        panel.add(deployButton);
        
        // Close 按钮
        closeButton = new JButton("<html><font color='white'><b>Close</b></font></html>");
        closeButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        closeButton.setPreferredSize(new Dimension(100, 35));
        closeButton.setBackground(new Color(95, 99, 104));
        closeButton.setForeground(Color.WHITE);
        deployButton.setOpaque(true);
        closeButton.setContentAreaFilled(true);
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);
        
        return panel;
    }
    
    /**
     * 加载工作空间列表
     * Load workspace list from Portal Settings
     */
    private void loadWorkspaceList() {
        logger.info("Loading workspace list for tenant: {}", mainTenantCode);
        
        // 从Portal Settings获取租户代码配置
        AppSettings settings = AppSettings.getInstance();
        String tenantCodesStr = settings.getPortalTenantCodesString();
        
        // 解析租户代码和子租户代码
        tenantSubTenantMap = TenantCICDUtils.parseTenantCodesWithSubTenants(tenantCodesStr);
        
        // 获取当前主租户的子租户代码列表
        List<String> subTenants = tenantSubTenantMap.get(mainTenantCode);
        
        if (subTenants == null || subTenants.isEmpty()) {
            logger.warn("No workspaces configured for tenant: {}", mainTenantCode);
            logToConsole("⚠ No workspaces configured for tenant: " + mainTenantCode);
            logToConsole("Please configure sub-tenant codes in Portal Settings.");
            JOptionPane.showMessageDialog(this,
                "No workspaces configured for tenant: " + mainTenantCode + "\n" +
                "Please configure sub-tenant codes in Portal Settings.",
                "No Workspaces",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 填充workspace下拉框
        workspaceComboBox.removeAllItems();
        for (String subTenant : subTenants) {
            workspaceComboBox.addItem(subTenant);
        }
        
        logger.info("Loaded {} workspaces", subTenants.size());
        logToConsole("✓ Loaded " + subTenants.size() + " workspaces");
    }
    
    /**
     * 处理工作空间选择
     * Handle workspace selection
     */
    private void handleWorkspaceSelection() {
        String selectedWorkspace = (String) workspaceComboBox.getSelectedItem();
        
        if (selectedWorkspace == null || selectedWorkspace.trim().isEmpty()) {
            logger.debug("No workspace selected");
            return;
        }
        
        logger.info("Workspace selected: {}", selectedWorkspace);
        logToConsole("Retrieving token for workspace: " + selectedWorkspace);
        
        // 禁用environment下拉框，直到token获取成功
        environmentComboBox.setEnabled(false);
        environmentComboBox.removeAllItems();
        
        // 异步获取workspace token
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // 获取Portal Settings的用户名和密码
                AppSettings settings = AppSettings.getInstance();
                String username = settings.getPortalUsername();
                String password = settings.getPortalPassword();
                
                logger.info("Getting token for workspace: {}", selectedWorkspace);
                
                // 使用workspace作为tenant code获取token
                TokenResponse response = apiClient.getToken(username, password, selectedWorkspace);
                
                if (response.isAuthResult() && response.getAccessToken() != null && !response.getAccessToken().isEmpty()) {
                    return response.getAccessToken();
                } else {
                    throw new Exception("Authentication failed: " + response.getMessage());
                }
            }
            
            @Override
            protected void done() {
                try {
                    workspaceToken = get();
                    logger.info("Workspace token retrieved successfully");
                    logToConsole("✓ Workspace token retrieved successfully");
                    
                    // 加载environment列表
                    loadEnvironmentList();
                    
                } catch (Exception e) {
                    logger.error("Failed to get workspace token", e);
                    logToConsole("✗ Failed to get workspace token: " + e.getMessage());
                    
                    JOptionPane.showMessageDialog(DeploymentDialog.this,
                        "Failed to get workspace token:\n" + e.getMessage(),
                        "Authentication Error",
                        JOptionPane.ERROR_MESSAGE);
                    
                    workspaceToken = null;
                    environmentComboBox.setEnabled(false);
                }
            }
        };
        
        currentWorker = worker;
        worker.execute();
    }
    
    /**
     * 处理部署操作
     * Handle deploy action
     */
    private void handleDeploy() {
        logger.info("Deploy button clicked");
        
        // 验证配置
        if (!validateDeploymentConfiguration()) {
            return;
        }
        
        // 显示确认对话框
        showDeploymentConfirmation();
    }
    
    /**
     * 处理查看部署日志
     * Handle deployment log
     */
    private void handleDeploymentLog() {
        logger.info("Deployment Log button clicked");
        
        // 检查workspace
        String workspace = (String) workspaceComboBox.getSelectedItem();
        if (workspace == null || workspace.trim().isEmpty()) {
            logger.warn("Workspace not selected");
            JOptionPane.showMessageDialog(this,
                "Please select a workspace first",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 检查environment
        String environment = (String) environmentComboBox.getSelectedItem();
        if (environment == null || environment.trim().isEmpty()) {
            logger.warn("Environment not selected");
            JOptionPane.showMessageDialog(this,
                "Please select an environment first",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 检查workspace token
        if (workspaceToken == null || workspaceToken.trim().isEmpty()) {
            logger.warn("Workspace token not available");
            JOptionPane.showMessageDialog(this,
                "Workspace token not available. Please reselect the workspace.",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        logger.info("Opening Deployment Pod List Dialog");
        
        try {
            DeploymentPodListDialog podListDialog = new DeploymentPodListDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                apiClient,
                workspace,
                environment,
                workspaceToken
            );
            podListDialog.setVisible(true);
        } catch (Exception e) {
            logger.error("Failed to open Deployment Pod List Dialog", e);
            JOptionPane.showMessageDialog(this,
                "Failed to open Deployment Pod List Dialog:\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 验证部署配置
     * Validate deployment configuration
     * 
     * @return true if valid, false otherwise
     */
    private boolean validateDeploymentConfiguration() {
        // 检查镜像列表
        String imageText = imageListTextArea.getText().trim();
        if (imageText.isEmpty()) {
            logger.warn("Validation failed: image list is empty");
            JOptionPane.showMessageDialog(this,
                "Please enter at least one image name",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            imageListTextArea.requestFocus();
            return false;
        }
        
        // 检查workspace
        String workspace = (String) workspaceComboBox.getSelectedItem();
        if (workspace == null || workspace.trim().isEmpty()) {
            logger.warn("Validation failed: workspace not selected");
            JOptionPane.showMessageDialog(this,
                "Please select a workspace",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            workspaceComboBox.requestFocus();
            return false;
        }
        
        // 检查environment
        String environment = (String) environmentComboBox.getSelectedItem();
        if (environment == null || environment.trim().isEmpty()) {
            logger.warn("Validation failed: environment not selected");
            JOptionPane.showMessageDialog(this,
                "Please select an environment",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            environmentComboBox.requestFocus();
            return false;
        }
        
        // 检查workspace token
        if (workspaceToken == null || workspaceToken.trim().isEmpty()) {
            logger.warn("Validation failed: workspace token not available");
            JOptionPane.showMessageDialog(this,
                "Workspace token not available. Please reselect the workspace.",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        logger.debug("Deployment configuration validation passed");
        return true;
    }
    
    /**
     * 显示部署确认对话框
     * Show deployment confirmation dialog
     */
    private void showDeploymentConfirmation() {
        String workspace = (String) workspaceComboBox.getSelectedItem();
        String environment = (String) environmentComboBox.getSelectedItem();
        String imageText = imageListTextArea.getText().trim();
        
        // 解析镜像列表
        String[] imageLines = imageText.split("\n");
        List<String> images = new ArrayList<>();
        for (String line : imageLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                images.add(trimmed);
            }
        }
        
        // 提取应用名称
        List<String> appNames = new ArrayList<>();
        for (String image : images) {
            String appName = TenantCICDUtils.extractAppNameFromImage(image);
            if (appName != null) {
                appNames.add(appName);
            } else {
                appNames.add("[Unable to parse]");
            }
        }
        
        // 构建确认消息
        StringBuilder message = new StringBuilder();
        message.append("You are about to deploy the following images:\n\n");
        message.append("Workspace: ").append(workspace).append("\n");
        message.append("Environment: ").append(environment).append("\n");
        message.append("Image Count: ").append(images.size()).append("\n\n");
        message.append("Images:\n");
        
        for (int i = 0; i < images.size(); i++) {
            message.append(String.format("%d. %s\n", i + 1, images.get(i)));
            message.append(String.format("   App: %s\n", appNames.get(i)));
        }
        
        message.append("\nDeployment will proceed sequentially and stop on first failure.\n");
        message.append("Do you want to continue?");
        
        logger.info("Showing deployment confirmation for {} images", images.size());
        
        int result = JOptionPane.showConfirmDialog(this,
            message.toString(),
            "Confirm Deployment",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            logger.info("Deployment confirmed by user");
            executeDeployment(images);
        } else {
            logger.info("Deployment cancelled by user");
            logToConsole("Deployment cancelled by user");
        }
    }
    
    /**
     * 执行部署
     * Execute deployment
     * 
     * @param images 镜像列表
     */
    private void executeDeployment(List<String> images) {
        final String workspace = (String) workspaceComboBox.getSelectedItem();
        final String environment = (String) environmentComboBox.getSelectedItem();
        
        logger.info("Starting deployment: workspace={}, environment={}, imageCount={}", 
                   workspace, environment, images.size());
        
        logToConsole("========================================");
        logToConsole("Starting Deployment");
        logToConsole("Workspace: " + workspace);
        logToConsole("Environment: " + environment);
        logToConsole("Total Images: " + images.size());
        logToConsole("========================================");
        
        // 禁用部署按钮
        deployButton.setEnabled(false);
        deployButton.setText("<html><font color='white'><b>Deploying...</b></font></html>");
        
        // 创建异步worker
        SwingWorker<Integer, String> worker = new SwingWorker<Integer, String>() {
            private int successCount = 0;
            private int failureCount = 0;
            
            @Override
            protected Integer doInBackground() throws Exception {
                for (int i = 0; i < images.size(); i++) {
                    String image = images.get(i);
                    int imageNum = i + 1;
                    
                    // 提取应用名称
                    String appName = TenantCICDUtils.extractAppNameFromImage(image);
                    if (appName == null) {
                        String errorMsg = String.format("✗ [%d/%d] Failed to parse app name from image: %s", 
                                                       imageNum, images.size(), image);
                        publish(errorMsg);
                        logger.warn(errorMsg);
                        failureCount++;
                        break;  // 停止部署
                    }
                    
                    // 发布进度消息
                    publish(String.format("→ [%d/%d] Deploying: %s", imageNum, images.size(), image));
                    publish(String.format("   App: %s", appName));
                    publish(String.format("   Workspace: %s", workspace));
                    publish(String.format("   Environment: %s", environment));
                    
                    try {
                        // 调用部署API
                        String result = apiClient.deployImage(workspace, environment, workspaceToken, image, appName);
                        
                        String successMsg = String.format("✓ [%d/%d] Deployment successful: %s", 
                                                         imageNum, images.size(), appName);
                        publish(successMsg);
                        publish(String.format("   Result: %s", result));
                        logger.info(successMsg);
                        successCount++;
                        
                    } catch (Exception e) {
                        String errorMsg = String.format("✗ [%d/%d] Deployment failed: %s", 
                                                       imageNum, images.size(), appName);
                        publish(errorMsg);
                        publish(String.format("   Error: %s", e.getMessage()));
                        logger.error(errorMsg, e);
                        failureCount++;
                        break;  // 停止部署
                    }
                    
                    publish("----------------------------------------");
                }
                
                return successCount;
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    logToConsole(message);
                }
            }
            
            @Override
            protected void done() {
                // 重新启用部署按钮
                deployButton.setEnabled(true);
                deployButton.setText("<html><font color='white'><b>Deploy</b></font></html>");
                
                try {
                    int deployed = get();
                    
                    logToConsole("========================================");
                    logToConsole("Deployment Complete");
                    logToConsole(String.format("Success: %d, Failed: %d, Total: %d", 
                                              successCount, failureCount, images.size()));
                    logToConsole("========================================");
                    
                    if (failureCount == 0) {
                        logger.info("All deployments completed successfully");
                        JOptionPane.showMessageDialog(DeploymentDialog.this,
                            String.format("All %d images deployed successfully!", deployed),
                            "Deployment Complete",
                            JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        logger.warn("Deployment completed with failures: success={}, failed={}", 
                                   successCount, failureCount);
                        JOptionPane.showMessageDialog(DeploymentDialog.this,
                            String.format("Deployment stopped due to failure.\nSuccess: %d, Failed: %d", 
                                        successCount, failureCount),
                            "Deployment Failed",
                            JOptionPane.ERROR_MESSAGE);
                    }
                    
                } catch (Exception e) {
                    logger.error("Deployment execution failed", e);
                    logToConsole("✗ Deployment execution failed: " + e.getMessage());
                    
                    JOptionPane.showMessageDialog(DeploymentDialog.this,
                        "Deployment execution failed:\n" + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        currentWorker = worker;
        worker.execute();
    }
    
    /**
     * 加载环境列表
     * Load environment list using new API
     */
    private void loadEnvironmentList() {
        String selectedWorkspace = (String) workspaceComboBox.getSelectedItem();
        
        if (selectedWorkspace == null || workspaceToken == null) {
            logger.warn("Cannot load environment list: workspace or token is null");
            return;
        }
        
        logger.info("Loading environment list for workspace: {}", selectedWorkspace);
        logToConsole("Loading environments for workspace: " + selectedWorkspace);
        
        // 异步加载环境列表
        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                // 调用新的getEnvironments API
                return apiClient.getEnvironments(workspaceToken);
            }
            
            @Override
            protected void done() {
                try {
                    List<String> environments = get();
                    
                    if (environments.isEmpty()) {
                        logger.warn("No environments found for workspace: {}", selectedWorkspace);
                        logToConsole("⚠ No environments found for workspace: " + selectedWorkspace);
                        
                        JOptionPane.showMessageDialog(DeploymentDialog.this,
                            "No environments found for workspace: " + selectedWorkspace,
                            "No Environments",
                            JOptionPane.WARNING_MESSAGE);
                        
                        environmentComboBox.setEnabled(false);
                        return;
                    }
                    
                    // 填充environment下拉框
                    environmentComboBox.removeAllItems();
                    for (String env : environments) {
                        environmentComboBox.addItem(env);
                    }
                    environmentComboBox.setEnabled(true);
                    
                    logger.info("Loaded {} environments", environments.size());
                    logToConsole("✓ Loaded " + environments.size() + " environments");
                    
                } catch (Exception e) {
                    logger.error("Failed to load environment list", e);
                    logToConsole("✗ Failed to load environments: " + e.getMessage());
                    
                    JOptionPane.showMessageDialog(DeploymentDialog.this,
                        "Failed to load environments:\n" + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    
                    environmentComboBox.setEnabled(false);
                }
            }
        };
        
        currentWorker = worker;
        worker.execute();
    }
    
    /**
     * 记录到控制台
     * Log message to console
     * 
     * @param message 日志消息
     */
    private void logToConsole(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            consoleLogArea.append("[" + timestamp + "] " + message + "\n");
            consoleLogArea.setCaretPosition(consoleLogArea.getDocument().getLength());
        });
        logger.info(message);
    }
    
    /**
     * 资源清理
     * Resource cleanup
     */
    @Override
    public void dispose() {
        logger.info("Disposing Deployment Dialog");
        
        // 取消当前运行的worker
        if (currentWorker != null && !currentWorker.isDone()) {
            logger.info("Cancelling current worker");
            currentWorker.cancel(true);
        }
        
        // 清除敏感数据
        workspaceToken = null;
        mainTenantToken = null;
        
        // 清除缓存数据
        if (tenantSubTenantMap != null) {
            tenantSubTenantMap.clear();
        }
        
        super.dispose();
    }
}
