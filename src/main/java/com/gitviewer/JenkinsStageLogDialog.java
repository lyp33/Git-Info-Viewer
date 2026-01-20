package com.gitviewer;

import javax.swing.*;
import java.awt.*;

/**
 * Jenkins Stage 日志对话框
 * 显示指定 Stage 的构建日志
 * 包含两个 Tab：Jenkins Log 和 Portal Log
 */
public class JenkinsStageLogDialog extends JDialog {

    private JenkinsApiClient apiClient;
    private String jobPath;
    private int buildNumber;
    private JenkinsStage stage;
    
    private JTabbedPane tabbedPane;
    private JTextArea jenkinsLogTextArea;
    private JTextArea portalLogTextArea;
    private JButton refreshButton;
    
    // 缓存 Stage Log 供 Portal Log 使用
    private String cachedStageLog = null;

    public JenkinsStageLogDialog(Window parent, JenkinsApiClient apiClient, 
                                  String jobPath, int buildNumber, JenkinsStage stage) {
        super(parent, "Stage Log: " + stage.getName(), Dialog.ModalityType.APPLICATION_MODAL);
        this.apiClient = apiClient;
        this.jobPath = jobPath;
        this.buildNumber = buildNumber;
        this.stage = stage;
        
        System.out.println("[StageLogDialog] Constructor called");
        System.out.println("[StageLogDialog] Stage: " + stage.getName());
        System.out.println("[StageLogDialog] Job Path: " + jobPath);
        System.out.println("[StageLogDialog] Build Number: " + buildNumber);
        
        initializeUI();
        System.out.println("[StageLogDialog] UI initialized");
        
        setLocationRelativeTo(parent);
        System.out.println("[StageLogDialog] Location set");
        
        // 延迟加载日志，让对话框先显示出来
        // 只需要加载 Jenkins Log，它会自动触发 Portal Log 的加载
        SwingUtilities.invokeLater(() -> {
            System.out.println("[StageLogDialog] Starting to load logs...");
            loadJenkinsLog();
        });
    }

    /**
     * 初始化 UI
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(900, 600);

        // 顶部面板 - Stage 信息和按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel stageLabel = new JLabel(String.format("Stage: %s | Build #%d | Status: %s | Duration: %s",
            stage.getName(), buildNumber, stage.getStatus(), stage.getFormattedDuration()));
        stageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        topPanel.add(stageLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            // 清除缓存并重新加载 Jenkins Log
            cachedStageLog = null;
            portalLogTextArea.setText("Click to load Portal log...");
            loadJenkinsLog();
        });
        buttonPanel.add(refreshButton);
        
        topPanel.add(buttonPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 中间面板 - Tab 页
        tabbedPane = new JTabbedPane();
        
        // Tab 1: Jenkins Log
        jenkinsLogTextArea = new JTextArea();
        jenkinsLogTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        jenkinsLogTextArea.setEditable(false);
        jenkinsLogTextArea.setLineWrap(false);
        jenkinsLogTextArea.setWrapStyleWord(false);
        jenkinsLogTextArea.setBackground(Color.BLACK);
        jenkinsLogTextArea.setForeground(Color.WHITE);
        jenkinsLogTextArea.setCaretColor(Color.WHITE);
        jenkinsLogTextArea.setTabSize(4);
        
        JScrollPane jenkinsScrollPane = new JScrollPane(jenkinsLogTextArea);
        jenkinsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        jenkinsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        tabbedPane.addTab("Jenkins Log", jenkinsScrollPane);
        
        // Tab 2: Portal Log
        portalLogTextArea = new JTextArea();
        portalLogTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        portalLogTextArea.setEditable(false);
        portalLogTextArea.setLineWrap(false);
        portalLogTextArea.setWrapStyleWord(false);
        portalLogTextArea.setBackground(Color.BLACK);
        portalLogTextArea.setForeground(Color.WHITE);
        portalLogTextArea.setCaretColor(Color.WHITE);
        portalLogTextArea.setTabSize(4);
        
        JScrollPane portalScrollPane = new JScrollPane(portalLogTextArea);
        portalScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        portalScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        tabbedPane.addTab("Portal Log", portalScrollPane);
        
        // 添加 Tab 切换监听器 - 只在切换到 Portal Log 时加载
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            System.out.println("[StageLogDialog] Tab changed to index: " + selectedIndex);
            
            // 如果切换到 Portal Log (index 1) 且内容为空或显示初始消息，则加载
            if (selectedIndex == 1) {
                String currentText = portalLogTextArea.getText();
                if (currentText.isEmpty() || currentText.equals("Click to load Portal log...")) {
                    System.out.println("[StageLogDialog] Loading Portal Log on tab switch");
                    loadPortalLogOnDemand();
                }
            }
        });
        
        // 设置 Portal Log 初始提示
        portalLogTextArea.setText("Click to load Portal log...");
        
        add(tabbedPane, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        bottomPanel.add(closeButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 加载 Jenkins Log
     */
    private void loadJenkinsLog() {
        System.out.println("[StageLogDialog] loadJenkinsLog() called");
        jenkinsLogTextArea.setText("Loading Jenkins log...");
        refreshButton.setEnabled(false);
        
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                System.out.println("[StageLogDialog] Jenkins Worker started");
                try {
                    // 首先获取 Stage Log
                    System.out.println("[StageLogDialog] Fetching stage log...");
                    String stageLog = apiClient.fetchStageLog(jobPath, buildNumber, stage.getId(), stage.getName());
                    System.out.println("[StageLogDialog] Stage log fetched, length: " + (stageLog != null ? stageLog.length() : 0));
                    
                    // 尝试从 Stage Log 中提取子作业路径和构建 ID，并获取子作业的完整日志
                    System.out.println("[StageLogDialog] Fetching sub-job console log...");
                    String subJobLog = apiClient.fetchSubJobConsoleLog(stageLog);
                    System.out.println("[StageLogDialog] Sub-job log fetched, length: " + (subJobLog != null ? subJobLog.length() : 0));
                    
                    // 缓存子作业日志供 Portal Log 使用（因为 curl 命令在子作业日志中）
                    // 如果成功获取子作业日志，缓存它；否则缓存原始 Stage Log
                    cachedStageLog = (subJobLog != null && !subJobLog.isEmpty()) ? subJobLog : stageLog;
                    System.out.println("[StageLogDialog] Cached log for Portal Log, length: " + (cachedStageLog != null ? cachedStageLog.length() : 0));
                    
                    // 返回子作业日志用于显示
                    return subJobLog;
                } catch (Exception e) {
                    System.err.println("[StageLogDialog] ERROR in doInBackground: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }

            @Override
            protected void done() {
                System.out.println("[StageLogDialog] Jenkins Worker done() called");
                try {
                    String log = get();
                    System.out.println("[StageLogDialog] Got log, length: " + (log != null ? log.length() : 0));
                    
                    // 调试：检查日志内容
                    if (log != null && !log.isEmpty()) {
                        System.out.println("[StageLogDialog] Log preview (first 200 chars): " + log.substring(0, Math.min(200, log.length())));
                        System.out.println("[StageLogDialog] Contains newlines: " + log.contains("\n"));
                        System.out.println("[StageLogDialog] Newline count: " + log.split("\n").length);
                        
                        // 检查是否包含中文字符
                        boolean hasChinese = log.chars().anyMatch(c -> Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
                        System.out.println("[StageLogDialog] Contains Chinese characters: " + hasChinese);
                    }
                    
                    // 在日志前添加 "printing..." 提示
                    String displayLog = "printing...\n\n" + (log != null ? log : "");
                    
                    jenkinsLogTextArea.setText(displayLog);
                    jenkinsLogTextArea.setCaretPosition(0);  // 滚动到顶部
                    System.out.println("[StageLogDialog] Jenkins log displayed successfully");
                    
                    // 不再自动加载 Portal Log - 等待用户切换到 Portal Log tab
                    System.out.println("[StageLogDialog] Stage log cached for Portal Log (not loading automatically)");
                } catch (Exception e) {
                    System.err.println("[StageLogDialog] ERROR in done(): " + e.getMessage());
                    e.printStackTrace();
                    String errorMsg = "Failed to load Jenkins log:\n" + e.getMessage();
                    if (e.getCause() != null) {
                        errorMsg += "\nCause: " + e.getCause().getMessage();
                    }
                    jenkinsLogTextArea.setText(errorMsg);
                    JOptionPane.showMessageDialog(JenkinsStageLogDialog.this,
                        errorMsg,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                } finally {
                    refreshButton.setEnabled(true);
                    System.out.println("[StageLogDialog] Jenkins Worker completed");
                }
            }
        };
        
        worker.execute();
        System.out.println("[StageLogDialog] Jenkins Worker executed");
    }
    
    /**
     * 按需加载 Portal Log（当用户切换到 Portal Log tab 时）
     */
    private void loadPortalLogOnDemand() {
        System.out.println("[StageLogDialog] loadPortalLogOnDemand() called");
        System.out.println("[StageLogDialog] cachedStageLog>>>" + cachedStageLog );
        
        if (cachedStageLog == null) {
            portalLogTextArea.setText("Error: Stage log not available. Please load Jenkins Log first.");
            return;
        }
        
        // 第一步：清空 log
        portalLogTextArea.setText("");
        
        SwingWorker<PortalLogInfo, String> worker = new SwingWorker<PortalLogInfo, String>() {
            @Override
            protected PortalLogInfo doInBackground() throws Exception {
                System.out.println("[StageLogDialog] Portal Worker started");
                
                // 提取 Portal API URL
                String portalUrl = apiClient.extractPortalUrlPublic(cachedStageLog);
                if (portalUrl == null) {
                    throw new Exception("Could not find Portal API URL in stage log");
                }
                
                // 提取 headers
                java.util.Map<String, String> headers = apiClient.extractCurlHeadersPublic(cachedStageLog);
                
                // 第二步：发布 API 信息
                StringBuilder apiInfo = new StringBuilder();
                apiInfo.append("=== Portal API Request Info ===\n\n");
                apiInfo.append("URL:\n").append(portalUrl).append("\n\n");
                apiInfo.append("Headers:\n");
                if (headers.isEmpty()) {
                    apiInfo.append("  (no headers)\n");
                } else {
                    for (java.util.Map.Entry<String, String> entry : headers.entrySet()) {
                        apiInfo.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                    }
                }
                apiInfo.append("\n");
                publish(apiInfo.toString());
                
                // 第三步：发布 "Loading..." 消息
                publish("Loading...\n\n");
                
                // 第四步：调用 API 获取日志
                try {
                    String portalLog = apiClient.fetchPortalBuildOutputWithInfo(cachedStageLog, portalUrl, headers);
                    System.out.println("[StageLogDialog] Portal log fetched, length: " + (portalLog != null ? portalLog.length() : 0));
                    
                    PortalLogInfo info = new PortalLogInfo();
                    info.apiInfo = apiInfo.toString();
                    info.logContent = portalLog;
                    return info;
                } catch (Exception e) {
                    System.err.println("[StageLogDialog] ERROR fetching portal log: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                // 逐步追加信息到文本区域
                for (String chunk : chunks) {
                    portalLogTextArea.append(chunk);
                }
            }

            @Override
            protected void done() {
                System.out.println("[StageLogDialog] Portal Worker done() called");
                try {
                    PortalLogInfo info = get();
                    System.out.println("[StageLogDialog] Got portal log, length: " + (info.logContent != null ? info.logContent.length() : 0));
                    
                    // 追加最终的日志内容
                    portalLogTextArea.append("=== Portal API Response ===\n\n");
                    
                    // 如果内容太大，截断并提示
                    String logContent = info.logContent != null ? info.logContent : "(empty response)";
                    final int MAX_DISPLAY_LENGTH = 500000; // 最多显示 500KB
                    
                    if (logContent.length() > MAX_DISPLAY_LENGTH) {
                        System.out.println("[StageLogDialog] Log content too large (" + logContent.length() + " chars), truncating to " + MAX_DISPLAY_LENGTH);
                        
                        // 禁用文本区域更新以提高性能
                        portalLogTextArea.setEditable(false);
                        
                        // 显示截断的内容
                        portalLogTextArea.append("⚠ Warning: Log content is very large (" + logContent.length() + " characters).\n");
                        portalLogTextArea.append("Displaying first " + MAX_DISPLAY_LENGTH + " characters only.\n\n");
                        portalLogTextArea.append(logContent.substring(0, MAX_DISPLAY_LENGTH));
                        portalLogTextArea.append("\n\n... (truncated)");
                    } else {
                        // 禁用文本区域更新以提高性能
                        portalLogTextArea.setEditable(false);
                        portalLogTextArea.append(logContent);
                    }
                    
                    portalLogTextArea.setCaretPosition(0);  // 滚动到顶部
                    System.out.println("[StageLogDialog] Portal log displayed successfully");
                } catch (Exception e) {
                    System.err.println("[StageLogDialog] ERROR in portal done(): " + e.getMessage());
                    e.printStackTrace();
                    String errorMsg = "\n\nFailed to load Portal log:\n" + e.getMessage();
                    if (e.getCause() != null) {
                        errorMsg += "\nCause: " + e.getCause().getMessage();
                    }
                    portalLogTextArea.append(errorMsg);
                } finally {
                    System.out.println("[StageLogDialog] Portal Worker completed");
                }
            }
        };
        
        worker.execute();
        System.out.println("[StageLogDialog] Portal Worker executed");
    }
    
    /**
     * Portal Log 信息容器
     */
    private static class PortalLogInfo {
        String apiInfo;
        String logContent;
    }
    
    /**
     * 加载 Portal Log（使用 Stage Log）
     */
    private void loadPortalLogWithStageLog(String stageLog) {
        System.out.println("[StageLogDialog] loadPortalLogWithStageLog() called");
        portalLogTextArea.setText("Loading Portal log...");
        
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                System.out.println("[StageLogDialog] Portal Worker started");
                try {
                    // 从 Stage Log 中提取 Portal API 信息并调用
                    String portalLog = apiClient.fetchPortalBuildOutput(stageLog);
                    System.out.println("[StageLogDialog] Portal log fetched, length: " + (portalLog != null ? portalLog.length() : 0));
                    return portalLog;
                } catch (Exception e) {
                    System.err.println("[StageLogDialog] ERROR fetching portal log: " + e.getMessage());
                    e.printStackTrace();
                    throw e;
                }
            }

            @Override
            protected void done() {
                System.out.println("[StageLogDialog] Portal Worker done() called");
                try {
                    String log = get();
                    System.out.println("[StageLogDialog] Got portal log, length: " + (log != null ? log.length() : 0));
                    
                    // 在日志前添加 "printing..." 提示
                    String displayLog = "printing...\n\n" + (log != null ? log : "");
                    
                    portalLogTextArea.setText(displayLog);
                    portalLogTextArea.setCaretPosition(0);  // 滚动到顶部
                    System.out.println("[StageLogDialog] Portal log displayed successfully");
                } catch (Exception e) {
                    System.err.println("[StageLogDialog] ERROR in portal done(): " + e.getMessage());
                    e.printStackTrace();
                    String errorMsg = "Failed to load Portal log:\n" + e.getMessage();
                    if (e.getCause() != null) {
                        errorMsg += "\nCause: " + e.getCause().getMessage();
                    }
                    portalLogTextArea.setText(errorMsg);
                } finally {
                    System.out.println("[StageLogDialog] Portal Worker completed");
                }
            }
        };
        
        worker.execute();
        System.out.println("[StageLogDialog] Portal Worker executed");
    }
    
    /**
     * 加载 Portal Log（旧方法，已废弃）
     */
    @Deprecated
    private void loadPortalLog() {
        // 这个方法已经不再使用，因为需要先获取 Stage Log
        System.out.println("[StageLogDialog] loadPortalLog() called (deprecated)");
    }
}
