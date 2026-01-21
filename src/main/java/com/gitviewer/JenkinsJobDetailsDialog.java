package com.gitviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Jenkins 作业详情对话框
 * 显示构建历史、Stage 视图和构建参数
 */
public class JenkinsJobDetailsDialog extends JDialog {

    private JenkinsApiClient apiClient;
    private String jobPath;
    private String jobName;
    
    private JList<JenkinsBuild> buildList;
    private DefaultListModel<JenkinsBuild> buildListModel;
    private JenkinsStageViewPanel stageViewPanel;
    private JButton buildButton;
    private JButton refreshButton;
    private JTextArea consoleLogArea;

    public JenkinsJobDetailsDialog(Frame parent, JenkinsApiClient apiClient, String jobPath, String jobName) {
        super(parent, "Job: " + jobPath, true);
        this.apiClient = apiClient;
        this.jobPath = jobPath;
        this.jobName = jobName;
        
        initializeUI();
        loadBuildHistory();
        setLocationRelativeTo(parent);
    }

    /**
     * 初始化 UI
     */
    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(Color.WHITE);
        setSize(1400, 900);  // 增大尺寸：从 1000x700 改为 1400x900

        // 顶部面板 - 作业信息和按钮 - 现代化样式
        JPanel topPanel = new JPanel(new BorderLayout(15, 0));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(218, 220, 224)),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        
        // 左侧：作业名称
        JLabel jobLabel = new JLabel("Job: " + jobName);
        jobLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        jobLabel.setForeground(new Color(60, 64, 67));
        topPanel.add(jobLabel, BorderLayout.WEST);
        
        // 右侧：按钮 - 现代化样式
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        buildButton = new JButton("<html><font color='white'><b>Build with Parameters</b></font></html>");
        buildButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        buildButton.setPreferredSize(new Dimension(180, 35));
        buildButton.setBackground(new Color(52, 168, 83));  // 绿色
        buildButton.setForeground(Color.WHITE);
        buildButton.setOpaque(true);
        buildButton.setContentAreaFilled(true);
        buildButton.setFocusPainted(false);
        buildButton.setBorderPainted(false);
        buildButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buildButton.addActionListener(e -> openBuildParametersDialog());
        buttonPanel.add(buildButton);
        
        refreshButton = new JButton("<html><font color='white'><b>Refresh</b></font></html>");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        refreshButton.setPreferredSize(new Dimension(100, 35));
        refreshButton.setBackground(new Color(66, 133, 244));  // 蓝色
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setOpaque(true);
        refreshButton.setContentAreaFilled(true);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> loadBuildHistory());
        buttonPanel.add(refreshButton);
        
        topPanel.add(buttonPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 主内容区域 - 左右分割
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(400);  // 左侧 Build History 占 40%
        mainSplitPane.setResizeWeight(0.4);     // 左侧占 40%，右侧占 60%
        mainSplitPane.setBorder(null);
        mainSplitPane.setBackground(Color.WHITE);

        // 左侧：构建历史列表 - 现代化样式
        JPanel buildHistoryPanel = new JPanel(new BorderLayout());
        buildHistoryPanel.setBackground(Color.WHITE);
        buildHistoryPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));
        
        // 添加标题标签
        JLabel buildHistoryLabel = new JLabel("Build History");
        buildHistoryLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        buildHistoryLabel.setForeground(new Color(60, 64, 67));
        buildHistoryLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        buildHistoryPanel.add(buildHistoryLabel, BorderLayout.NORTH);
        
        buildListModel = new DefaultListModel<>();
        buildList = new JList<>(buildListModel);
        buildList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        buildList.setCellRenderer(new BuildListCellRenderer());
        buildList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        buildList.setBackground(Color.WHITE);
        buildList.setSelectionBackground(new Color(66, 133, 244, 50));
        buildList.setSelectionForeground(Color.BLACK);
        
        // 添加选择监听器
        buildList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                JenkinsBuild selectedBuild = buildList.getSelectedValue();
                if (selectedBuild != null) {
                    loadStageView(selectedBuild);
                }
            }
        });
        
        // 添加右键菜单
        buildList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int index = buildList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        buildList.setSelectedIndex(index);
                        showBuildContextMenu(e.getX(), e.getY());
                    }
                }
            }
        });
        
        JScrollPane buildScrollPane = new JScrollPane(buildList);
        buildScrollPane.setBorder(BorderFactory.createLineBorder(new Color(218, 220, 224), 1));
        buildScrollPane.setBackground(Color.WHITE);
        buildHistoryPanel.add(buildScrollPane, BorderLayout.CENTER);
        
        mainSplitPane.setLeftComponent(buildHistoryPanel);

        // 右侧：Stage View（上）+ Console Log（下）
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setDividerLocation(300);  // Stage View 和 Console Log 各占 50%
        rightSplitPane.setResizeWeight(0.5);     // 上下各占 50%
        rightSplitPane.setBorder(null);
        rightSplitPane.setBackground(Color.WHITE);
        
        // Stage 视图面板 - 现代化样式
        JPanel stagePanel = new JPanel(new BorderLayout());
        stagePanel.setBackground(Color.WHITE);
        stagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 20));
        
        JLabel stageLabel = new JLabel("Stage View");
        stageLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        stageLabel.setForeground(new Color(60, 64, 67));
        stageLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        stagePanel.add(stageLabel, BorderLayout.NORTH);
        
        stageViewPanel = new JenkinsStageViewPanel();
        stageViewPanel.setBorder(BorderFactory.createLineBorder(new Color(218, 220, 224), 1));
        stageViewPanel.setBackground(Color.WHITE);
        stageViewPanel.setJobInfo(apiClient, jobPath, 0);
        stagePanel.add(stageViewPanel, BorderLayout.CENTER);
        
        rightSplitPane.setTopComponent(stagePanel);
        
        // Console Log 面板 - 现代化样式
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBackground(Color.WHITE);
        consolePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 20));
        
        JLabel consoleLabel = new JLabel("Console Log");
        consoleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        consoleLabel.setForeground(new Color(60, 64, 67));
        consoleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        consolePanel.add(consoleLabel, BorderLayout.NORTH);
        
        consoleLogArea = new JTextArea();
        consoleLogArea.setEditable(false);
        consoleLogArea.setFont(new Font("Monospaced", Font.PLAIN, 13));  // 增大字体：从 12 改为 13
        consoleLogArea.setBackground(new Color(30, 30, 30));
        consoleLogArea.setForeground(new Color(220, 220, 220));
        consoleLogArea.setCaretColor(new Color(220, 220, 220));
        consoleLogArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane consoleScrollPane = new JScrollPane(consoleLogArea);
        consoleScrollPane.setBorder(BorderFactory.createLineBorder(new Color(218, 220, 224), 1));
        consolePanel.add(consoleScrollPane, BorderLayout.CENTER);
        
        // 将 Console Log 区域传递给 StageViewPanel
        stageViewPanel.setConsoleLogArea(consoleLogArea);
        
        rightSplitPane.setBottomComponent(consolePanel);
        
        mainSplitPane.setRightComponent(rightSplitPane);
        
        add(mainSplitPane, BorderLayout.CENTER);

        // 底部按钮面板 - 现代化样式
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 20));
        
        JButton closeButton = new JButton("<html><font color='white'><b>Close</b></font></html>");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        closeButton.setPreferredSize(new Dimension(100, 35));
        closeButton.setBackground(new Color(95, 99, 104));  // 灰色
        closeButton.setForeground(Color.WHITE);
        closeButton.setOpaque(true);
        closeButton.setContentAreaFilled(true);
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        bottomPanel.add(closeButton);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // 记录初始化日志
        logToConsole("Job Details Dialog initialized for: " + jobPath);
    }

    /**
     * 加载构建历史
     */
    private void loadBuildHistory() {
        logToConsole("Loading build history for job: " + jobPath);
        
        SwingWorker<List<JenkinsBuild>, Void> worker = new SwingWorker<List<JenkinsBuild>, Void>() {
            @Override
            protected List<JenkinsBuild> doInBackground() throws Exception {
                return apiClient.fetchBuildHistory(jobPath, 20);
            }

            @Override
            protected void done() {
                try {
                    List<JenkinsBuild> builds = get();
                    logToConsole("Successfully loaded " + builds.size() + " builds");
                    buildListModel.clear();
                    for (JenkinsBuild build : builds) {
                        buildListModel.addElement(build);
                    }
                    
                    if (!builds.isEmpty()) {
                        buildList.setSelectedIndex(0);
                        logToConsole("Auto-selected build #" + builds.get(0).getNumber());
                    }
                } catch (Exception e) {
                    logToConsole("ERROR: Failed to load build history: " + e.getMessage());
                    e.printStackTrace();
                    logToConsole("Please check the console log for details");
                }
            }
        };
        
        worker.execute();
    }


    /**
     * 加载 Stage 视图
     */
    private void loadStageView(JenkinsBuild build) {
        logToConsole("Loading module view for build #" + build.getNumber());
        
        SwingWorker<List<JenkinsStage>, Void> worker = new SwingWorker<List<JenkinsStage>, Void>() {
            @Override
            protected List<JenkinsStage> doInBackground() throws Exception {
                return apiClient.fetchBuildStages(jobPath, build.getNumber());
            }

            @Override
            protected void done() {
                try {
                    List<JenkinsStage> stages = get();
                    logToConsole("Successfully loaded " + stages.size() + " modules");
                    
                    // 特殊情况：如果没有 stages，说明这个 build 本身就是一个 stage
                    // 创建一个合成的 stage 来代表这个 build
                    if (stages.isEmpty()) {
                        logToConsole("No modules found - this build IS itself a stage");
                        logToConsole("Creating synthetic stage for build #" + build.getNumber());
                        
                        JenkinsStage syntheticStage = new JenkinsStage();
                        syntheticStage.setName("Build #" + build.getNumber());
                        syntheticStage.setId("build-" + build.getNumber());  // 使用特殊的 ID 格式
                        
                        // 设置状态（从 build 的 result 映射到 stage 的 status）
                        if (build.getResult() != null) {
                            syntheticStage.setStatus(build.getResult());
                        } else {
                            syntheticStage.setStatus("IN_PROGRESS");
                        }
                        
                        // 持续时间设置为 0（因为我们没有这个信息）
                        syntheticStage.setDurationMillis(0);
                        
                        // 附加 build 信息，用于在 Stage View 中显示详细信息
                        syntheticStage.setBuildInfo(build);
                        
                        stages = new java.util.ArrayList<>();
                        stages.add(syntheticStage);
                        
                        logToConsole("Created synthetic stage: " + syntheticStage.getName() + 
                                   ", Status: " + syntheticStage.getStatus());
                    } else {
                        for (JenkinsStage stage : stages) {
                            logToConsole("  Module: " + stage.getName() + 
                                       ", ID: " + (stage.getId() != null ? stage.getId() : "NULL") +
                                       ", Status: " + stage.getStatus());
                        }
                    }
                    
                    stageViewPanel.setJobInfo(apiClient, jobPath, build.getNumber());
                    stageViewPanel.displayStages(stages);
                } catch (Exception e) {
                    logToConsole("ERROR: Failed to load module view: " + e.getMessage());
                    e.printStackTrace();
                    stageViewPanel.clear();
                    logToConsole("Please check the console log for details");
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * 记录日志到控制台
     */
    private void logToConsole(String message) {
        if (consoleLogArea != null) {
            SwingUtilities.invokeLater(() -> {
                String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                consoleLogArea.append("[" + timestamp + "] " + message + "\n");
                consoleLogArea.setCaretPosition(consoleLogArea.getDocument().getLength());
            });
        }
    }


    /**
     * 打开构建参数对话框
     */   
    private void openBuildParametersDialog() {
        logToConsole("Opening build parameters dialog...");
        
        SwingWorker<Map<String, String>, Void> worker = new SwingWorker<Map<String, String>, Void>() {
            @Override
            protected Map<String, String> doInBackground() throws Exception {
                List<JenkinsBuild> builds = apiClient.fetchBuildHistory(jobPath, 1);
                if (builds.isEmpty()) {
                    logToConsole("No previous builds found");
                    return new java.util.HashMap<>();
                }
                
                JenkinsBuild latestBuild = builds.get(0);
                logToConsole("Found latest build #" + latestBuild.getNumber());
                
                Map<String, String> params = apiClient.fetchBuildParametersForRebuild(jobPath, latestBuild.getNumber());
                logToConsole("Fetched " + params.size() + " parameters from latest build");
                
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    logToConsole("  Param: " + entry.getKey() + " = " + entry.getValue());
                }
                
                if (params.containsKey("versions")) {
                    String versions = params.get("versions");
                    String incrementedVersions = incrementVersionNumbers(versions);
                    params.put("versions", incrementedVersions);
                    logToConsole("Incremented versions: " + versions + " -> " + incrementedVersions);
                }
                
                return params;
            }

            @Override
            protected void done() {
                try {
                    Map<String, String> parameters = get();
                    
                    JenkinsBuildParametersDialog dialog = new JenkinsBuildParametersDialog(
                        (Frame) getOwner(), apiClient, jobPath, jobName, parameters);
                    dialog.setVisible(true);
                    
                    logToConsole("Build dialog closed. Click Refresh to update build history.");
                } catch (Exception e) {
                    logToConsole("ERROR: Failed to fetch latest build parameters: " + e.getMessage());
                    
                    JenkinsBuildParametersDialog dialog = new JenkinsBuildParametersDialog(
                        (Frame) getOwner(), apiClient, jobPath, jobName, null);
                    dialog.setVisible(true);
                    
                    logToConsole("Build dialog closed. Click Refresh to update build history.");
                }
            }
        };
        
        worker.execute();
    }
    
    private String incrementVersionNumbers(String versions) {
        if (versions == null || versions.trim().isEmpty()) {
            return versions;
        }
        
        String[] versionParts = versions.split("[,;\n]+");
        StringBuilder result = new StringBuilder();
        
        for (String version : versionParts) {
            version = version.trim();
            if (version.isEmpty()) {
                continue;
            }
            
            if (result.length() > 0) {
                result.append(",");
            }
            
            result.append(incrementSingleVersion(version));
        }
        
        return result.toString();
    }
    
    private String incrementSingleVersion(String version) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
        java.util.regex.Matcher matcher = pattern.matcher(version);
        
        java.util.List<int[]> numberPositions = new java.util.ArrayList<>();
        while (matcher.find()) {
            numberPositions.add(new int[]{matcher.start(), matcher.end()});
        }
        
        if (numberPositions.isEmpty()) {
            return version;
        }
        
        int[] lastNumberPos = numberPositions.get(numberPositions.size() - 1);
        String numberStr = version.substring(lastNumberPos[0], lastNumberPos[1]);
        
        try {
            int number = Integer.parseInt(numberStr);
            int incrementedNumber = number + 1;
            
            String before = version.substring(0, lastNumberPos[0]);
            String after = version.substring(lastNumberPos[1]);
            
            return before + incrementedNumber + after;
        } catch (NumberFormatException e) {
            return version;
        }
    }

    private void showBuildContextMenu(int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem rebuildItem = new JMenuItem("Rebuild");
        rebuildItem.addActionListener(e -> rebuildSelectedBuild());
        menu.add(rebuildItem);
        
        menu.show(buildList, x, y);
    }


    private void rebuildSelectedBuild() {
        JenkinsBuild selectedBuild = buildList.getSelectedValue();
        if (selectedBuild == null) {
            return;
        }
        
        SwingWorker<Map<String, String>, Void> worker = new SwingWorker<Map<String, String>, Void>() {
            @Override
            protected Map<String, String> doInBackground() throws Exception {
                return apiClient.fetchBuildParametersForRebuild(jobPath, selectedBuild.getNumber());
            }

            @Override
            protected void done() {
                try {
                    Map<String, String> parameters = get();
                    
                    JenkinsBuildParametersDialog dialog = new JenkinsBuildParametersDialog(
                        (Frame) getOwner(), apiClient, jobPath, jobName, parameters);
                    dialog.setVisible(true);
                    
                    logToConsole("Build dialog closed. Click Refresh to update build history.");
                } catch (Exception e) {
                    logToConsole("ERROR: Failed to fetch build parameters: " + e.getMessage());
                }
            }
        };
        
        worker.execute();
    }

    private class BuildListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof JenkinsBuild) {
                JenkinsBuild build = (JenkinsBuild) value;
                
                String statusIcon = getStatusIcon(build);
                String displayText = statusIcon + " " + build.getFormattedDisplay();
                
                setText(displayText);
                setToolTipText(buildTooltip(build));
                
                if (!isSelected) {
                    if (build.isSuccess()) {
                        setForeground(new Color(0, 128, 0));
                    } else if (build.isFailure()) {
                        setForeground(new Color(255, 0, 0));
                    } else if (build.isInProgress()) {
                        setForeground(new Color(0, 0, 255));
                    }
                }
                
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            }
            
            return this;
        }

        private String getStatusIcon(JenkinsBuild build) {
            if (build.isSuccess()) {
                return "[OK]";  // 成功
            } else if (build.isFailure()) {
                return "[FAIL]";  // 失败
            } else if (build.isInProgress()) {
                return "[RUN]";  // 进行中
            } else {
                return "[ ]";  // 其他
            }
        }

        private String buildTooltip(JenkinsBuild build) {
            StringBuilder tooltip = new StringBuilder("<html>");
            tooltip.append("<b>Build #").append(build.getNumber()).append("</b><br>");
            tooltip.append("<b>Status:</b> ").append(build.getResult() != null ? build.getResult() : "IN_PROGRESS").append("<br>");
            tooltip.append("<b>Triggered by:</b> ").append(build.getTriggeredBy() != null ? build.getTriggeredBy() : "Unknown").append("<br>");
            tooltip.append("<b>Trigger Time:</b> ").append(formatTimestamp(build.getTimestamp())).append("<br>");
            
            // 显示关键参数
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
            
            tooltip.append("</html>");
            return tooltip.toString();
        }
        
        private String formatTimestamp(long timestamp) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
            return sdf.format(new Date(timestamp));
        }
    }
}
