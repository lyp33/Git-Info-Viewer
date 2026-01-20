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
        setLayout(new BorderLayout(5, 5));
        setSize(1400, 900);  // 增大尺寸：从 1000x700 改为 1400x900

        // 顶部面板 - 作业信息和按钮
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        // 左侧：作业名称
        JLabel jobLabel = new JLabel("Job: " + jobName);
        jobLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        topPanel.add(jobLabel, BorderLayout.WEST);
        
        // 右侧：按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buildButton = new JButton("Build with Parameters");
        buildButton.addActionListener(e -> openBuildParametersDialog());
        buttonPanel.add(buildButton);
        
        refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> loadBuildHistory());
        buttonPanel.add(refreshButton);
        
        topPanel.add(buttonPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 主内容区域 - 左右分割
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(400);  // 左侧 Build History 占 40%
        mainSplitPane.setResizeWeight(0.4);     // 左侧占 40%，右侧占 60%
        mainSplitPane.setBorder(null);

        // 左侧：构建历史列表
        JPanel buildHistoryPanel = new JPanel(new BorderLayout());
        buildHistoryPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 10, 0, 5),
            BorderFactory.createTitledBorder("Build History")
        ));
        
        buildListModel = new DefaultListModel<>();
        buildList = new JList<>(buildListModel);
        buildList.setFont(AppSettings.getInstance().getRightPanelFont());
        buildList.setCellRenderer(new BuildListCellRenderer());
        buildList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
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
        buildHistoryPanel.add(buildScrollPane, BorderLayout.CENTER);
        
        mainSplitPane.setLeftComponent(buildHistoryPanel);

        // 右侧：Stage View（上）+ Console Log（下）
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setDividerLocation(300);  // Stage View 和 Console Log 各占 50%
        rightSplitPane.setResizeWeight(0.5);     // 上下各占 50%
        rightSplitPane.setBorder(null);
        
        // Stage 视图面板
        stageViewPanel = new JenkinsStageViewPanel();
        stageViewPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(0, 5, 0, 10),
            BorderFactory.createTitledBorder("Stage View")
        ));
        stageViewPanel.setJobInfo(apiClient, jobPath, 0);
        
        rightSplitPane.setTopComponent(stageViewPanel);
        
        // Console Log 面板
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 0, 10),
            BorderFactory.createTitledBorder("Console Log")
        ));
        
        consoleLogArea = new JTextArea();
        consoleLogArea.setEditable(false);
        consoleLogArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        consoleLogArea.setBackground(new Color(30, 30, 30));
        consoleLogArea.setForeground(new Color(200, 200, 200));
        consoleLogArea.setCaretColor(Color.WHITE);
        JScrollPane consoleScrollPane = new JScrollPane(consoleLogArea);
        consoleScrollPane.setBorder(null);
        consolePanel.add(consoleScrollPane, BorderLayout.CENTER);
        
        // 将 Console Log 区域传递给 StageViewPanel
        stageViewPanel.setConsoleLogArea(consoleLogArea);
        
        rightSplitPane.setBottomComponent(consolePanel);
        
        mainSplitPane.setRightComponent(rightSplitPane);
        
        add(mainSplitPane, BorderLayout.CENTER);

        // 底部按钮面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
        JButton closeButton = new JButton("Close");
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
                    
                    for (JenkinsStage stage : stages) {
                        logToConsole("  Module: " + stage.getName() + 
                                   ", ID: " + (stage.getId() != null ? stage.getId() : "NULL") +
                                   ", Status: " + stage.getStatus());
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
                return "●";
            } else if (build.isFailure()) {
                return "●";
            } else if (build.isInProgress()) {
                return "●";
            } else {
                return "○";
            }
        }

        private String buildTooltip(JenkinsBuild build) {
            StringBuilder tooltip = new StringBuilder("<html>");
            tooltip.append("<b>Build #").append(build.getNumber()).append("</b><br>");
            tooltip.append("<b>Status:</b> ").append(build.getResult() != null ? build.getResult() : "IN_PROGRESS").append("<br>");
            tooltip.append("<b>Triggered by:</b> ").append(build.getTriggeredBy() != null ? build.getTriggeredBy() : "Unknown").append("<br>");
            
            if (build.getParameters() != null && !build.getParameters().isEmpty()) {
                tooltip.append("<b>Parameters:</b><br>");
                for (Map.Entry<String, String> entry : build.getParameters().entrySet()) {
                    tooltip.append("&nbsp;&nbsp;").append(entry.getKey()).append(": ").append(entry.getValue()).append("<br>");
                }
            }
            
            tooltip.append("</html>");
            return tooltip.toString();
        }
    }
}
