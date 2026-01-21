package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * 部署Pod日志对话框
 * Deployment Pod Log Dialog
 */
public class DeploymentPodLogDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(DeploymentPodLogDialog.class);
    
    // UI Components
    private JTextArea logTextArea;
    private JButton refreshButton;
    private JButton closeButton;
    
    // Data
    private PortalApiClient apiClient;
    private String workspace;
    private String environment;
    private String workspaceToken;
    private DeploymentPod pod;
    
    // SwingWorker reference
    private SwingWorker<?, ?> currentWorker;
    
    /**
     * 构造函数
     * 
     * @param parent 父窗口
     * @param apiClient Portal API客户端
     * @param workspace 工作空间
     * @param environment 环境
     * @param workspaceToken 工作空间Token
     * @param pod Pod信息
     */
    public DeploymentPodLogDialog(Frame parent, PortalApiClient apiClient, String workspace, 
                                  String environment, String workspaceToken, DeploymentPod pod) {
        super(parent, "Pod Logs - " + pod.getName(), true);
        logger.info("Opening Deployment Pod Log Dialog for pod: {}", pod.getName());
        
        this.apiClient = apiClient;
        this.workspace = workspace;
        this.environment = environment;
        this.workspaceToken = workspaceToken;
        this.pod = pod;
        
        initializeUI();
        loadLogs();
        
        setSize(1000, 700);
        setMinimumSize(new Dimension(800, 600));
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
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);
        
        // 顶部信息面板
        mainPanel.add(createInfoPanel(), BorderLayout.NORTH);
        
        // 日志面板
        mainPanel.add(createLogPanel(), BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // 按钮面板
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * 创建信息面板
     * Create info panel
     */
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 5, 3, 15);
        gbc.gridy = 0;
        
        // Pod Name
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        JLabel nameLabel = new JLabel("Pod:");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLabel.setForeground(new Color(60, 64, 67));
        panel.add(nameLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JLabel nameValue = new JLabel(pod.getName());
        nameValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(nameValue, gbc);
        
        // App
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        JLabel appLabel = new JLabel("App:");
        appLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        appLabel.setForeground(new Color(60, 64, 67));
        panel.add(appLabel, gbc);
        
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        JLabel appValue = new JLabel(pod.getApp());
        appValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(appValue, gbc);
        
        // Status
        gbc.gridx = 4;
        gbc.weightx = 0.0;
        JLabel statusLabel = new JLabel("Status:");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(new Color(60, 64, 67));
        panel.add(statusLabel, gbc);
        
        gbc.gridx = 5;
        gbc.weightx = 0.5;
        JLabel statusValue = new JLabel(pod.getRealStatus());
        statusValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(statusValue, gbc);
        
        return panel;
    }
    
    /**
     * 创建日志面板
     * Create log panel
     */
    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        
        // 标题
        JLabel titleLabel = new JLabel("Console Log:");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(60, 64, 67));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // 黑色背景 + 白色字体的终端风格
        logTextArea = new JTextArea();
        logTextArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logTextArea.setBackground(Color.BLACK);
        logTextArea.setForeground(Color.WHITE);
        logTextArea.setCaretColor(Color.WHITE);
        logTextArea.setEditable(false);
        logTextArea.setLineWrap(true);
        logTextArea.setWrapStyleWord(false);
        logTextArea.setTabSize(4);
        
        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
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
        
        // Refresh 按钮
        refreshButton = new JButton("<html><font color='white'><b>Refresh</b></font></html>");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        refreshButton.setPreferredSize(new Dimension(100, 35));
        refreshButton.setBackground(new Color(66, 133, 244));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setOpaque(true);
        refreshButton.setContentAreaFilled(true);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> loadLogs());
        panel.add(refreshButton);
        
        // Close 按钮
        closeButton = new JButton("<html><font color='white'><b>Close</b></font></html>");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        closeButton.setPreferredSize(new Dimension(100, 35));
        closeButton.setBackground(new Color(95, 99, 104));
        closeButton.setForeground(Color.WHITE);
        closeButton.setOpaque(true);
        closeButton.setContentAreaFilled(true);
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);
        
        return panel;
    }
    
    /**
     * 加载日志
     * Load logs
     */
    private void loadLogs() {
        logger.info("Loading logs for pod: {}", pod.getName());
        
        // 清空现有日志
        logTextArea.setText("Loading logs...\n");
        
        // 禁用按钮
        refreshButton.setEnabled(false);
        refreshButton.setText("<html><font color='white'><b>Loading...</b></font></html>");
        
        // 异步加载
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return apiClient.getDeploymentPodLogs(workspace, environment, workspaceToken, 
                                                      pod.getName(), pod.getApp());
            }
            
            @Override
            protected void done() {
                // 重新启用按钮
                refreshButton.setEnabled(true);
                refreshButton.setText("<html><font color='white'><b>Refresh</b></font></html>");
                
                try {
                    String logs = get();
                    
                    // 处理换行符
                    if (logs != null) {
                        // 替换 \n 为实际换行
                        logs = logs.replace("\\n", "\n");
                        // 替换 \t 为制表符
                        logs = logs.replace("\\t", "\t");
                        logTextArea.setText(logs);
                        logTextArea.setCaretPosition(0);
                        logger.info("Loaded {} characters of logs", logs.length());
                    } else {
                        logTextArea.setText("No logs available.");
                    }
                } catch (Exception e) {
                    logger.error("Failed to load logs", e);
                    logTextArea.setText("Failed to load logs:\n" + e.getMessage());
                    
                    JOptionPane.showMessageDialog(DeploymentPodLogDialog.this,
                        "Failed to load logs:\n" + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        currentWorker = worker;
        worker.execute();
    }
    
    /**
     * 资源清理
     * Resource cleanup
     */
    @Override
    public void dispose() {
        logger.info("Disposing Deployment Pod Log Dialog");
        
        // 取消当前运行的worker
        if (currentWorker != null && !currentWorker.isDone()) {
            logger.info("Cancelling current worker");
            currentWorker.cancel(true);
        }
        
        super.dispose();
    }
}
