package com.gitviewer;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Jenkins Pipeline Stage 视图面板
 * 显示构建的各个阶段及其状态
 */
public class JenkinsStageViewPanel extends JPanel {

    private JenkinsApiClient apiClient;
    private String jobPath;
    private int buildNumber;
    private JList<JenkinsStage> stageList;
    private DefaultListModel<JenkinsStage> stageListModel;
    private JTextArea externalConsoleLogArea; // 外部的 Console Log 区域

    public JenkinsStageViewPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(null); // 移除外层边框
        
        // 创建 stage 列表
        stageListModel = new DefaultListModel<>();
        stageList = new JList<>(stageListModel);
        stageList.setFont(AppSettings.getInstance().getRightPanelFont());
        stageList.setCellRenderer(new StageListCellRenderer());
        stageList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // 添加选择监听器 - 单击加载日志到控制台
        stageList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                JenkinsStage selectedStage = stageList.getSelectedValue();
                if (selectedStage != null) {
                    loadStageLogToConsole(selectedStage);
                }
            }
        });
        
        // 添加双击监听器 - 双击打开日志对话框
        stageList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = stageList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        JenkinsStage stage = stageListModel.getElementAt(index);
                        logToConsole("=== Double-Click Event ===");
                        logToConsole("Stage: " + stage.getName());
                        logToConsole("Stage ID: " + (stage.getId() != null ? stage.getId() : "NULL"));
                        logToConsole(">>> Opening stage log dialog <<<");
                        openStageLogDialog(stage);
                        logToConsole("========================");
                    }
                }
            }
        });
        
        // 简化布局：只显示 Module List
        JScrollPane stageScrollPane = new JScrollPane(stageList);
        stageScrollPane.setBorder(null);
        add(stageScrollPane, BorderLayout.CENTER);
        
        logToConsole("Module View Panel initialized (simplified layout)");
    }

    /**
     * 设置 API 客户端和作业信息
     */
    public void setJobInfo(JenkinsApiClient apiClient, String jobPath, int buildNumber) {
        this.apiClient = apiClient;
        this.jobPath = jobPath;
        this.buildNumber = buildNumber;
        
        logToConsole("=== Job Info Set ===");
        logToConsole("API Client: " + (apiClient != null ? "SET" : "NULL"));
        logToConsole("Job Path: " + (jobPath != null ? jobPath : "NULL"));
        logToConsole("Build Number: " + buildNumber);
        logToConsole("====================");
    }

    /**
     * 显示 Module 列表
     */
    public void displayStages(List<JenkinsStage> stages) {
        logToConsole("=== Displaying Modules ===");
        logToConsole("Number of modules: " + (stages != null ? stages.size() : 0));
        
        stageListModel.clear();
        
        if (stages == null || stages.isEmpty()) {
            logToConsole("No modules to display");
        } else {
            for (int i = 0; i < stages.size(); i++) {
                JenkinsStage stage = stages.get(i);
                logToConsole("Module " + (i+1) + ": " + stage.getName() + 
                           " (ID: " + (stage.getId() != null ? stage.getId() : "NULL") + 
                           ", Status: " + stage.getStatus() + ")");
                stageListModel.addElement(stage);
            }
            
            // 自动选择第一个 module
            if (!stages.isEmpty()) {
                stageList.setSelectedIndex(0);
                logToConsole("Auto-selected first module");
            }
        }
        
        logToConsole("========================");
    }

    /**
     * 加载 Module 日志到控制台
     */
    private void loadStageLogToConsole(JenkinsStage stage) {
        if (apiClient == null || stage.getName() == null || stage.getName().isEmpty()) {
            logToConsole("Cannot load module log: missing API client or module name");
            return;
        }
        
        logToConsole("Loading log for module: " + stage.getName());
        
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                // 检查是否是合成的 stage（build 本身）
                if (stage.getId() != null && stage.getId().startsWith("build-")) {
                    logToConsole("Detected synthetic stage - loading build console log");
                    return apiClient.fetchBuildConsoleLog(jobPath, buildNumber);
                } else {
                    // 使用新的 API：传入 stageName
                    return apiClient.fetchStageLog(jobPath, buildNumber, stage.getId(), stage.getName());
                }
            }

            @Override
            protected void done() {
                try {
                    String log = get();
                    
                    // 尝试从日志中提取 Stage Build ID（仅对非合成 stage）
                    if (stage.getId() == null || !stage.getId().startsWith("build-")) {
                        Integer stageBuildId = apiClient.extractStageBuildId(log);
                        if (stageBuildId != null) {
                            stage.setStageBuildNumber(stageBuildId);
                            logToConsole("✓ Detected Stage Build ID: #" + stageBuildId);
                            
                            // 刷新 Stage 列表显示
                            stageList.repaint();
                        }
                    }
                    
                    logToConsole("=== Module Log: " + stage.getName() + " ===");
                    logToConsole(log);
                    logToConsole("=== End of Module Log ===");
                } catch (Exception e) {
                    logToConsole("ERROR: Failed to load module log: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }

    /**
     * 打开 Module 日志对话框
     */
    private void openStageLogDialog(JenkinsStage stage) {
        logToConsole("=== openStageLogDialog START ===");
        logToConsole("Stage name: " + stage.getName());
        
        String stageId = stage.getId();
        logToConsole("Stage ID value: [" + stageId + "]");
        logToConsole("Stage ID == null: " + (stageId == null));
        if (stageId != null) {
            logToConsole("Stage ID.isEmpty(): " + stageId.isEmpty());
            logToConsole("Stage ID.length(): " + stageId.length());
        }
        
        if (apiClient == null) {
            logToConsole("ERROR: API Client is null!");
            JOptionPane.showMessageDialog(this, 
                "API Client is not initialized", 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        logToConsole("✓ API Client check passed");
        
        if (stageId == null) {
            logToConsole("ERROR: Stage ID is NULL! Returning early.");
            JOptionPane.showMessageDialog(this, 
                "Stage ID is not available for: " + stage.getName(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        logToConsole("✓ Stage ID not null check passed");
        
        if (stageId.isEmpty()) {
            logToConsole("ERROR: Stage ID is EMPTY! Returning early.");
            JOptionPane.showMessageDialog(this, 
                "Stage ID is not available for: " + stage.getName(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        logToConsole("✓ Stage ID not empty check passed");
        
        // 检查是否是合成的 stage（build 本身）
        if (stageId.startsWith("build-")) {
            logToConsole("Detected synthetic stage - this is a build without sub-stages");
            logToConsole("Opening build console log dialog instead of stage log dialog");
            
            // 对于合成 stage，直接显示 build 的 console log
            // 我们可以重用 JenkinsStageLogDialog，但传入特殊标记
            try {
                Window parentWindow = SwingUtilities.getWindowAncestor(this);
                logToConsole("Creating JenkinsStageLogDialog for synthetic stage...");
                
                JenkinsStageLogDialog dialog = new JenkinsStageLogDialog(
                    parentWindow, apiClient, jobPath, buildNumber, stage);
                
                logToConsole("Dialog created successfully");
                dialog.setVisible(true);
                logToConsole("Dialog closed");
            } catch (Throwable e) {
                logToConsole("ERROR: Exception while opening dialog: " + e.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                    "Failed to open module log dialog: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
            return;
        }
        
        logToConsole("All checks passed, getting parent frame...");
        
        Window parentWindow = null;
        try {
            parentWindow = SwingUtilities.getWindowAncestor(this);
            logToConsole("Parent window retrieved: " + (parentWindow != null ? "Found" : "NULL"));
            if (parentWindow != null) {
                logToConsole("Parent window class: " + parentWindow.getClass().getName());
            }
        } catch (Exception e) {
            logToConsole("ERROR: Exception while getting parent window: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            logToConsole("Creating JenkinsStageLogDialog...");
            logToConsole("  - parentWindow: " + parentWindow);
            logToConsole("  - apiClient: " + apiClient);
            logToConsole("  - jobPath: " + jobPath);
            logToConsole("  - buildNumber: " + buildNumber);
            logToConsole("  - stage: " + stage.getName());
            logToConsole("  - stage.getId(): " + stageId);
            
            JenkinsStageLogDialog dialog = new JenkinsStageLogDialog(
                parentWindow, apiClient, jobPath, buildNumber, stage);
            
            logToConsole("Dialog created successfully");
            logToConsole("Setting dialog visible...");
            dialog.setVisible(true);
            logToConsole("Dialog closed");
        } catch (Throwable e) {
            logToConsole("ERROR: Exception while opening dialog: " + e.getClass().getName() + ": " + e.getMessage());
            logToConsole("Stack trace:");
            e.printStackTrace();
            for (StackTraceElement element : e.getStackTrace()) {
                logToConsole("  at " + element.toString());
            }
            JOptionPane.showMessageDialog(this, 
                "Failed to open module log dialog: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 设置外部的 Console Log 区域
     */
    public void setConsoleLogArea(JTextArea consoleLogArea) {
        this.externalConsoleLogArea = consoleLogArea;
    }
    
    /**
     * 记录日志到控制台
     */
    private void logToConsole(String message) {
        if (externalConsoleLogArea != null) {
            SwingUtilities.invokeLater(() -> {
                String timestamp = new java.text.SimpleDateFormat("HH:mm:ss.SSS").format(new java.util.Date());
                externalConsoleLogArea.append("[" + timestamp + "] " + message + "\n");
                externalConsoleLogArea.setCaretPosition(externalConsoleLogArea.getDocument().getLength());
            });
        }
        
        // 同时输出到系统控制台
        System.out.println("[StageView] " + message);
    }

    /**
     * 清空显示
     */
    public void clear() {
        stageListModel.clear();
        if (externalConsoleLogArea != null) {
            externalConsoleLogArea.setText("");
        }
        logToConsole("Module view cleared");
    }
    
    /**
     * Module 列表单元格渲染器
     * 类似 Build History 的样式
     */
    private class StageListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof JenkinsStage) {
                JenkinsStage stage = (JenkinsStage) value;
                
                // 构建显示文本
                String statusIcon = getStatusIcon(stage);
                StringBuilder displayText = new StringBuilder();
                displayText.append(statusIcon).append(" ");
                
                // 检查是否是合成 stage（没有子 stage 的 build）
                if (stage.getId() != null && stage.getId().startsWith("build-") && stage.getBuildInfo() != null) {
                    // 对于合成 stage，显示完整的 build 信息
                    JenkinsBuild build = stage.getBuildInfo();
                    
                    // 构建编号
                    displayText.append("#").append(build.getNumber());
                    
                    // 状态
                    displayText.append(" - ");
                    if (build.getResult() != null) {
                        displayText.append(build.getResult());
                    } else {
                        displayText.append("IN_PROGRESS");
                    }
                    
                    // 时间
                    displayText.append(" - ");
                    displayText.append(formatTimestamp(build.getTimestamp()));
                    
                    // 触发用户
                    if (build.getTriggeredBy() != null && !build.getTriggeredBy().isEmpty()) {
                        displayText.append(" - by ").append(build.getTriggeredBy());
                    }
                    
                    // 关键参数：优先显示 SERVICE_NAME, versions, VERSION, BRANCH
                    Map<String, String> params = build.getParameters();
                    if (params != null && !params.isEmpty()) {
                        StringBuilder paramText = new StringBuilder();
                        
                        if (params.containsKey("SERVICE_NAME")) {
                            String serviceName = params.get("SERVICE_NAME");
                            if (serviceName != null && !serviceName.isEmpty()) {
                                if (paramText.length() > 0) paramText.append(", ");
                                paramText.append("SERVICE_NAME: ").append(serviceName);
                            }
                        }
                        
                        if (params.containsKey("versions") || params.containsKey("VERSIONS")) {
                            String versions = params.containsKey("versions") ? params.get("versions") : params.get("VERSIONS");
                            if (versions != null && !versions.isEmpty()) {
                                if (paramText.length() > 0) paramText.append(", ");
                                // 截断过长的值
                                if (versions.length() > 50) {
                                    versions = versions.substring(0, 47) + "...";
                                }
                                paramText.append("VERSIONS: ").append(versions);
                            }
                        }
                        
                        if (params.containsKey("VERSION")) {
                            String version = params.get("VERSION");
                            if (version != null && !version.isEmpty()) {
                                if (paramText.length() > 0) paramText.append(", ");
                                paramText.append("VERSION: ").append(version);
                            }
                        }
                        
                        if (params.containsKey("BRANCH") || params.containsKey("branch")) {
                            String branch = params.containsKey("BRANCH") ? params.get("BRANCH") : params.get("branch");
                            if (branch != null && !branch.isEmpty()) {
                                if (paramText.length() > 0) paramText.append(", ");
                                paramText.append("BRANCH: ").append(branch);
                            }
                        }
                        
                        if (paramText.length() > 0) {
                            displayText.append(" - [").append(paramText).append("]");
                        }
                    }
                } else {
                    // 对于正常 stage，显示原有格式
                    displayText.append(stage.getName());
                    displayText.append(" (").append(stage.getFormattedDuration()).append(")");
                    
                    // 如果有 Stage Build ID，显示它
                    if (stage.hasStageBuildId()) {
                        displayText.append(" - Build ").append(stage.getStageBuildDisplay());
                    }
                }
                
                setText(displayText.toString());
                
                // 设置工具提示
                String tooltip = buildTooltip(stage);
                setToolTipText(tooltip);
                
                // 设置颜色
                if (!isSelected) {
                    if (stage.isSuccess()) {
                        setForeground(new Color(0, 128, 0));  // 绿色
                    } else if (stage.isFailure()) {
                        setForeground(new Color(255, 0, 0));  // 红色
                    } else if (stage.isInProgress()) {
                        setForeground(new Color(0, 0, 255));  // 蓝色
                    } else {
                        setForeground(Color.GRAY);  // 灰色
                    }
                }
                
                // 设置边距
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            }
            
            return this;
        }

        private String getStatusIcon(JenkinsStage stage) {
            if (stage.isSuccess()) {
                return "[OK]";  // 成功
            } else if (stage.isFailure()) {
                return "[FAIL]";  // 失败
            } else if (stage.isInProgress()) {
                return "[RUN]";  // 进行中
            } else {
                return "[ ]";  // 其他
            }
        }

        /**
         * 构建工具提示
         */
        private String buildTooltip(JenkinsStage stage) {
            StringBuilder tooltip = new StringBuilder("<html>");
            tooltip.append("<b>Module: ").append(stage.getName()).append("</b><br>");
            tooltip.append("<b>Status:</b> ").append(stage.getStatus() != null ? stage.getStatus() : "UNKNOWN").append("<br>");
            tooltip.append("<b>Duration:</b> ").append(stage.getFormattedDuration()).append("<br>");
            
            if (stage.hasStageBuildId()) {
                tooltip.append("<b>Build ID:</b> ").append(stage.getStageBuildDisplay()).append("<br>");
            }
            
            // 对于合成 stage（没有子 stage 的 build），显示额外信息
            if (stage.getId() != null && stage.getId().startsWith("build-")) {
                if (stage.getBuildInfo() != null) {
                    JenkinsBuild build = stage.getBuildInfo();
                    
                    tooltip.append("<br><b>--- Build Details ---</b><br>");
                    
                    if (build.getTriggeredBy() != null) {
                        tooltip.append("<b>Triggered by:</b> ").append(build.getTriggeredBy()).append("<br>");
                    }
                    
                    tooltip.append("<b>Trigger Time:</b> ").append(formatTimestamp(build.getTimestamp())).append("<br>");
                    
                    Map<String, String> params = build.getParameters();
                    if (params != null && !params.isEmpty()) {
                        if (params.containsKey("SERVICE_NAME")) {
                            tooltip.append("<b>Service Name:</b> ").append(params.get("SERVICE_NAME")).append("<br>");
                        }
                        if (params.containsKey("versions")) {
                            tooltip.append("<b>Versions:</b> ").append(params.get("versions")).append("<br>");
                        }
                        if (params.containsKey("VERSION")) {
                            tooltip.append("<b>Version:</b> ").append(params.get("VERSION")).append("<br>");
                        }
                        if (params.containsKey("BRANCH") || params.containsKey("branch")) {
                            String branch = params.containsKey("BRANCH") ? params.get("BRANCH") : params.get("branch");
                            tooltip.append("<b>Branch:</b> ").append(branch).append("<br>");
                        }
                    }
                }
            }
            
            tooltip.append("<br><i>Click to view log in console, double-click to open dialog</i>");
            tooltip.append("</html>");
            return tooltip.toString();
        }
        
        private String formatTimestamp(long timestamp) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm");
            return sdf.format(new java.util.Date(timestamp));
        }
    }
}
