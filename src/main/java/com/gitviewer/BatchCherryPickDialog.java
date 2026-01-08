package com.gitviewer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 批量 Cherry-Pick 对话框
 * 用于跨项目或同项目批量 cherry-pick commits
 */
public class BatchCherryPickDialog extends JDialog {

    private JTextArea commitUrlsTextArea;
    private JTextField branchNameTextField;
    private JTextArea outputTextArea;
    private File currentDirectory;

    // 颜色常量
    private static final Color PRIMARY_COLOR = new Color(66, 133, 244);
    private static final Color BORDER_COLOR = new Color(227, 233, 239);
    private static final Color ODD_ROW_COLOR = new Color(255, 255, 255);
    private static final Color EVEN_ROW_COLOR = new Color(245, 248, 250);
    private static final Color SUCCESS_COLOR = new Color(40, 167, 69);
    private static final Color FAIL_COLOR = new Color(220, 53, 69);
    private static final Color PENDING_COLOR = new Color(255, 193, 7);

    // 结果列表
    private List<CherryPickResultItem> resultList;
    private JButton resultButton;

    public BatchCherryPickDialog(Frame parent, File currentDir) {
        super(parent, "Batch Cherry-Pick", true);
        this.currentDirectory = currentDir;
        this.resultList = new ArrayList<>();
        initializeUI();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(900, 700);

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. Commit URLs 输入区域
        JPanel urlsPanel = createCommitUrlsPanel();
        mainPanel.add(urlsPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // 2. Branch Name 输入区域
        JPanel branchPanel = createBranchNamePanel();
        mainPanel.add(branchPanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // 3. 操作按钮
        JPanel buttonPanel = createActionButtonPanel();
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // 4. 输出区域
        JPanel outputPanel = createOutputPanel();
        mainPanel.add(outputPanel);

        add(mainPanel, BorderLayout.CENTER);

        // 底部关闭按钮
        JPanel closeButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closeButton.addActionListener(e -> dispose());
        closeButtonPanel.add(closeButton);
        add(closeButtonPanel, BorderLayout.SOUTH);

        // 设置默认字体
        Font defaultFont = new Font("Segoe UI", Font.PLAIN, 12);
        applyFontRecursive(this, defaultFont);
    }

    private JPanel createCommitUrlsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Commit URLs (one per line)",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(95, 99, 104)
        ));

        commitUrlsTextArea = new JTextArea();
        commitUrlsTextArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        commitUrlsTextArea.setBackground(new Color(248, 249, 250));
        commitUrlsTextArea.setMargin(new Insets(10, 10, 10, 10));
        commitUrlsTextArea.setToolTipText("Enter Git commit URLs, one per line.\nExample: https://gitlab.insuremo.com/project1/-/commit/abc123");

        JScrollPane scrollPane = new JScrollPane(commitUrlsTextArea);
        scrollPane.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBranchNamePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Target Branch Name",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(95, 99, 104)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        branchNameTextField = new JTextField();
        branchNameTextField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        branchNameTextField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
        branchNameTextField.setToolTipText("Enter the target branch name for cherry-pick");

        panel.add(branchNameTextField, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createActionButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton sortByTimeButton = new JButton("Sort by Time");
        sortByTimeButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sortByTimeButton.setBackground(new Color(255, 152, 0)); // Orange color
        sortByTimeButton.setForeground(Color.WHITE);
        sortByTimeButton.setFocusPainted(false);
        sortByTimeButton.setBorderPainted(false);
        sortByTimeButton.setOpaque(true);
        sortByTimeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sortByTimeButton.addActionListener(e -> sortCommitUrlsByTime());
        panel.add(sortByTimeButton);

        JButton cherryPickButton = new JButton("Cherry Pick All");
        cherryPickButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cherryPickButton.setBackground(PRIMARY_COLOR);
        cherryPickButton.setForeground(Color.WHITE);
        cherryPickButton.setFocusPainted(false);
        cherryPickButton.setBorderPainted(false);
        cherryPickButton.setOpaque(true);
        cherryPickButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cherryPickButton.addActionListener(e -> executeCherryPick());
        panel.add(cherryPickButton);

        resultButton = new JButton("Show Results");
        resultButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        resultButton.setBackground(SUCCESS_COLOR);
        resultButton.setForeground(Color.WHITE);
        resultButton.setFocusPainted(false);
        resultButton.setBorderPainted(false);
        resultButton.setOpaque(true);
        resultButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        resultButton.setEnabled(false);
        resultButton.addActionListener(e -> showResultsDialog());
        panel.add(resultButton);

        JButton clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> {
            commitUrlsTextArea.setText("");
            branchNameTextField.setText("");
            outputTextArea.setText("");
            resultList.clear();
            resultButton.setEnabled(false);
        });
        panel.add(clearButton);

        JButton copyCommandsButton = new JButton("Copy Commands");
        copyCommandsButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        copyCommandsButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        copyCommandsButton.addActionListener(e -> copyCommandsToClipboard());
        panel.add(copyCommandsButton);

        return panel;
    }

    private JPanel createOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Output Log / Generated Commands",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(95, 99, 104)
        ));

        outputTextArea = new JTextArea();
        outputTextArea.setEditable(false);
        outputTextArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        outputTextArea.setBackground(Color.WHITE);
        outputTextArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 执行批量 cherry-pick
     */
    private void executeCherryPick() {
        String urlsText = commitUrlsTextArea.getText().trim();
        String targetBranch = branchNameTextField.getText().trim();

        if (urlsText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter at least one commit URL.",
                    "Input Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (targetBranch.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter the target branch name.",
                    "Input Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 清空结果列表
        resultList.clear();

        outputTextArea.setText("");
        appendLog("=== Batch Cherry-Pick Started ===\n");
        appendLog("Target Branch: " + targetBranch);
        appendLog("Number of URLs: " + urlsText.split("\n").length);
        appendLog("Current Working Directory: " + (currentDirectory != null ? currentDirectory.getAbsolutePath() : "N/A"));
        appendLog("========================================\n\n");

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            private StringBuilder logBuilder = new StringBuilder();

            @Override
            protected Void doInBackground() throws Exception {
                String[] urls = urlsText.split("\n");
                List<String> crossProjectCommands = new ArrayList<>();
                int sameProjectCount = 0;
                int crossProjectCount = 0;

                // 初始化日志
                logBuilder.append("=== Batch Cherry-Pick Started ===\n");
                logBuilder.append("Timestamp: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
                logBuilder.append("Target Branch: ").append(targetBranch).append("\n");
                logBuilder.append("Number of URLs: ").append(urls.length).append("\n");
                logBuilder.append("Current Working Directory: ").append(currentDirectory != null ? currentDirectory.getAbsolutePath() : "N/A").append("\n");
                logBuilder.append("========================================\n\n");

                for (int i = 0; i < urls.length; i++) {
                    String url = urls[i].trim();
                    if (url.isEmpty()) {
                        publish("Skipping empty line " + (i + 1));
                        logBuilder.append("Skipping empty line ").append(i + 1).append("\n");
                        continue;
                    }

                    publish("\n" + "=".repeat(60));
                    publish("Processing URL " + (i + 1) + " of " + urls.length);
                    publish("Original URL: " + url);
                    publish("=".repeat(60));

                    logBuilder.append("\n").append("=".repeat(60)).append("\n");
                    logBuilder.append("Processing URL ").append(i + 1).append(" of ").append(urls.length).append("\n");
                    logBuilder.append("Original URL: ").append(url).append("\n");
                    logBuilder.append("=".repeat(60)).append("\n");

                    // 创建结果项并初始化日志列表
                    CherryPickResultItem resultItem = new CherryPickResultItem();
                    resultItem.index = i + 1;
                    resultItem.url = url;
                    resultItem.targetBranch = targetBranch;
                    resultItem.logs = new ArrayList<>();

                    try {
                        // 1. 解析 commit URL
                        publish("\n[Step 1] Parsing commit URL...");
                        logBuilder.append("\n[Step 1] Parsing commit URL...\n");
                        resultItem.logs.add("[Step 1] Parsing commit URL...");

                        CommitInfo commitInfo = parseCommitUrl(url);
                        if (commitInfo == null) {
                            publish("  ✗ Failed: Invalid URL format");
                            publish("  Expected format: https://gitlab.insuremo.com/project/path/-/commit/abc123");
                            logBuilder.append("  ✗ Failed: Invalid URL format\n");
                            resultItem.logs.add("  ✗ Failed: Invalid URL format");
                            resultItem.logs.add("  Expected formats:");
                            resultItem.logs.add("    - GitLab: https://gitlab.insuremo.com/project/path/-/commit/abc123");
                            resultItem.logs.add("    - GitHub: https://github.com/user/project/commit/abc123");

                            resultItem.status = CherryPickStatus.FAIL;
                            resultItem.errorMessage = "Invalid URL format";
                            resultList.add(resultItem);
                            continue;
                        }

                        publish("  ✓ URL parsing successful");
                        publish("  - Extracted Project Code: " + commitInfo.projectCode);
                        publish("  - Extracted Commit ID: " + commitInfo.commitId);
                        publish("  - Extracted Base URL: " + commitInfo.baseUrl);
                        publish("  - Full URL: " + commitInfo.fullUrl);
                        logBuilder.append("  ✓ URL parsing successful\n");
                        logBuilder.append("  - Project Code: ").append(commitInfo.projectCode).append("\n");
                        logBuilder.append("  - Commit ID: ").append(commitInfo.commitId).append("\n");
                        resultItem.logs.add("  ✓ URL parsing successful");
                        resultItem.logs.add("  - Project Code: " + commitInfo.projectCode);
                        resultItem.logs.add("  - Commit ID: " + commitInfo.commitId);
                        resultItem.logs.add("  - Base URL: " + commitInfo.baseUrl);

                        resultItem.projectCode = commitInfo.projectCode;
                        resultItem.commitId = commitInfo.commitId;

                        // 2. 查找匹配的项目目录
                        publish("\n[Step 2] Searching for project directory...");
                        publish("  Looking for directory: " + commitInfo.projectCode);
                        publish("  In parent directory: " + currentDirectory.getAbsolutePath());
                        logBuilder.append("\n[Step 2] Searching for project directory...\n");
                        logBuilder.append("  Looking for: ").append(commitInfo.projectCode).append("\n");
                        resultItem.logs.add("[Step 2] Searching for project directory...");
                        resultItem.logs.add("  Looking for: " + commitInfo.projectCode);

                        File projectDir = findProjectDirectory(commitInfo.projectCode);
                        if (projectDir == null) {
                            publish("  ✗ Project directory NOT found");
                            publish("  Available directories in current path:");
                            logBuilder.append("  ✗ Project directory NOT found\n");
                            resultItem.logs.add("  ✗ Project directory NOT found");

                            File[] children = currentDirectory.listFiles();
                            if (children != null) {
                                for (File child : children) {
                                    if (child.isDirectory()) {
                                        publish("    - " + child.getName());
                                        logBuilder.append("    - ").append(child.getName()).append("\n");
                                        resultItem.logs.add("    - " + child.getName());
                                    }
                                }
                            }

                            resultItem.status = CherryPickStatus.FAIL;
                            resultItem.errorMessage = "Project directory not found";
                            resultList.add(resultItem);
                            continue;
                        }

                        publish("  ✓ Project directory found");
                        publish("  - Absolute Path: " + projectDir.getAbsolutePath());
                        publish("  - Is Git Repository: " + GitInfoExtractor.isGitRepository(projectDir));
                        logBuilder.append("  ✓ Project directory found\n");
                        logBuilder.append("  - Path: ").append(projectDir.getAbsolutePath()).append("\n");
                        resultItem.logs.add("  ✓ Project directory found");
                        resultItem.logs.add("  - Path: " + projectDir.getAbsolutePath());

                        resultItem.projectPath = projectDir.getAbsolutePath();

                        // 3. 获取当前项目的 git remote URL
                        publish("\n[Step 3] Retrieving current project Git configuration...");
                        logBuilder.append("\n[Step 3] Retrieving current project Git configuration...\n");
                        resultItem.logs.add("[Step 3] Retrieving current project Git configuration...");

                        String currentRemoteUrl = getRemoteUrl(projectDir);
                        publish("  Current project remote URL: " + currentRemoteUrl);
                        logBuilder.append("  Current remote URL: ").append(currentRemoteUrl).append("\n");
                        resultItem.logs.add("  Current remote URL: " + currentRemoteUrl);

                        // 4. 判断是否是同一项目
                        publish("\n[Step 4] Comparing projects...");
                        publish("  Commit URL Base: " + commitInfo.baseUrl);
                        publish("  Local Project URL: " + currentRemoteUrl);
                        logBuilder.append("\n[Step 4] Comparing projects...\n");
                        logBuilder.append("  Commit URL Base: ").append(commitInfo.baseUrl).append("\n");
                        logBuilder.append("  Local Project URL: ").append(currentRemoteUrl).append("\n");
                        resultItem.logs.add("[Step 4] Comparing projects...");
                        resultItem.logs.add("  Commit URL Base: " + commitInfo.baseUrl);
                        resultItem.logs.add("  Local Project URL: " + currentRemoteUrl);

                        boolean isSameProject = isSameProject(commitInfo.baseUrl, currentRemoteUrl);
                        publish("  Comparison result: " + (isSameProject ? "SAME PROJECT" : "DIFFERENT PROJECT"));
                        logBuilder.append("  Result: ").append(isSameProject ? "SAME PROJECT" : "DIFFERENT PROJECT").append("\n");
                        resultItem.logs.add("  Result: " + (isSameProject ? "SAME PROJECT" : "DIFFERENT PROJECT"));

                        resultItem.isSameProject = isSameProject;

                        if (isSameProject) {
                            // 同项目 cherry-pick
                            publish("\n[Step 5] Executing SAME-PROJECT cherry-pick...");
                            publish("  Strategy: Direct cherry-pick within same repository");
                            logBuilder.append("\n[Step 5] Executing SAME-PROJECT cherry-pick...\n");
                            resultItem.logs.add("[Step 5] Executing SAME-PROJECT cherry-pick...");
                            resultItem.logs.add("  Strategy: Direct cherry-pick within same repository");
                            sameProjectCount++;

                            CherryPickResult cpResult = executeSameProjectCherryPick(projectDir, commitInfo.commitId, targetBranch);

                            // 输出详细日志
                            publish("\n  Cherry-Pick Execution Log:");
                            logBuilder.append("  Cherry-Pick Execution Log:\n");
                            resultItem.logs.add("  Cherry-Pick Execution Log:");
                            for (String log : cpResult.logs) {
                                publish("  " + log);
                                logBuilder.append("  ").append(log).append("\n");
                                resultItem.logs.add("  " + log);
                            }

                            if (cpResult.success) {
                                if (cpResult.hasConflicts) {
                                    publish("\n  [SUCCESS] Cherry-pick completed with conflicts");
                                    logBuilder.append("\n  [SUCCESS] Cherry-pick completed with conflicts\n");
                                    resultItem.logs.add("  [SUCCESS] Cherry-pick completed with conflicts");

                                    resultItem.status = CherryPickStatus.SUCCESS;
                                    resultItem.errorMessage = "Warning: conflicted files exist";
                                } else {
                                    publish("\n  [SUCCESS] Cherry-pick completed successfully");
                                    logBuilder.append("\n  [SUCCESS] Cherry-pick completed successfully\n");
                                    resultItem.logs.add("  [SUCCESS] Cherry-pick completed successfully");

                                    resultItem.status = CherryPickStatus.SUCCESS;
                                }
                            } else {
                                publish("\n  [FAILED] Cherry-pick execution failed");
                                logBuilder.append("\n  [FAILED] Cherry-pick execution failed\n");
                                resultItem.logs.add("  [FAILED] Cherry-pick execution failed");

                                resultItem.status = CherryPickStatus.FAIL;
                                resultItem.errorMessage = "Cherry-pick execution failed";
                            }
                        } else {
                            // 跨项目 cherry-pick
                            publish("\n[Step 5] Executing CROSS-PROJECT cherry-pick...");
                            publish("  Strategy: Generate CMD commands for cross-repository cherry-pick");
                            publish("  This requires adding the source repository as 'upstream' remote");
                            logBuilder.append("\n[Step 5] Executing CROSS-PROJECT cherry-pick...\n");
                            logBuilder.append("  Strategy: Generate CMD commands\n");
                            resultItem.logs.add("[Step 5] Executing CROSS-PROJECT cherry-pick...");
                            resultItem.logs.add("  Strategy: Generate CMD commands");
                            crossProjectCount++;

                            List<String> commands = generateCrossProjectCommands(
                                    projectDir.getAbsolutePath(),
                                    commitInfo.baseUrl,
                                    commitInfo.commitId,
                                    targetBranch
                            );

                            publish("\n  Generated " + commands.size() + " CMD commands:");
                            logBuilder.append("  Generated ").append(commands.size()).append(" CMD commands:\n");
                            resultItem.logs.add("  Generated " + commands.size() + " CMD commands:");
                            for (int cmdIdx = 0; cmdIdx < commands.size(); cmdIdx++) {
                                String cmd = commands.get(cmdIdx);
                                if (cmd.isEmpty()) {
                                    publish("    [empty line]");
                                    logBuilder.append("    [empty line]\n");
                                    resultItem.logs.add("    [empty line]");
                                } else {
                                    publish("    " + (cmdIdx + 1) + ". " + cmd);
                                    logBuilder.append("    ").append(cmdIdx + 1).append(". ").append(cmd).append("\n");
                                    resultItem.logs.add("    " + (cmdIdx + 1) + ". " + cmd);
                                }
                            }

                            crossProjectCommands.addAll(commands);
                            publish("\n  [OK] Commands generated and added to batch execution list");
                            logBuilder.append("\n  [OK] Commands generated and added to batch execution list\n");
                            resultItem.logs.add("  [OK] Commands generated and added to batch execution list");

                            resultItem.status = CherryPickStatus.TO_RUN_CMD;
                            resultItem.generatedCommands = new ArrayList<>(commands);
                        }

                        publish("\n" + "-".repeat(60));
                        logBuilder.append("\n").append("-".repeat(60)).append("\n");

                        resultList.add(resultItem);

                    } catch (Exception e) {
                        publish("\n  ✗✗✗ ERROR processing URL ✗✗✗");
                        publish("  Error message: " + e.getMessage());
                        publish("  Error type: " + e.getClass().getSimpleName());
                        logBuilder.append("\n  ✗✗✗ ERROR processing URL ✗✗✗\n");
                        logBuilder.append("  Error: ").append(e.getMessage()).append("\n");
                        logBuilder.append("  Type: ").append(e.getClass().getSimpleName()).append("\n");
                        resultItem.logs.add("  ✗✗✗ ERROR processing URL ✗✗✗");
                        resultItem.logs.add("  Error: " + e.getMessage());
                        resultItem.logs.add("  Type: " + e.getClass().getSimpleName());

                        java.io.StringWriter sw = new java.io.StringWriter();
                        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                        e.printStackTrace(pw);
                        publish("  Stack trace: " + sw.toString());
                        logBuilder.append("  Stack trace: ").append(sw.toString()).append("\n");
                        resultItem.logs.add("  Stack trace: " + sw.toString());

                        resultItem.status = CherryPickStatus.FAIL;
                        resultItem.errorMessage = e.getMessage();
                        resultList.add(resultItem);
                    }
                }

                // 输出跨项目命令汇总
                if (!crossProjectCommands.isEmpty()) {
                    publish("\n\n" + "=".repeat(60));
                    publish("CROSS-PROJECT COMMANDS SUMMARY");
                    publish("=".repeat(60));
                    publish("Total cross-project commands: " + crossProjectCommands.size());
                    publish("\nFull command list:");
                    logBuilder.append("\n\n").append("=".repeat(60)).append("\n");
                    logBuilder.append("CROSS-PROJECT COMMANDS SUMMARY\n");
                    logBuilder.append("=".repeat(60)).append("\n");
                    logBuilder.append("Total cross-project commands: ").append(crossProjectCommands.size()).append("\n");
                    logBuilder.append("\nFull command list:\n");

                    int cmdNum = 1;
                    for (String cmd : crossProjectCommands) {
                        if (cmd.startsWith("::")) {
                            publish("\n" + cmdNum + ". " + cmd);
                            logBuilder.append("\n").append(cmdNum).append(". ").append(cmd).append("\n");
                            cmdNum++;
                        } else {
                            publish("   " + cmd);
                            logBuilder.append("   ").append(cmd).append("\n");
                        }
                    }
                    publish("=".repeat(60));
                    logBuilder.append("=".repeat(60)).append("\n");
                }

                // 输出总结
                publish("\n\n" + "=".repeat(60));
                publish("BATCH CHERRY-PICK SUMMARY");
                publish("=".repeat(60));
                publish("Total URLs provided: " + urls.length);
                publish("Same-project cherry-picks executed: " + sameProjectCount);
                publish("Cross-project commands generated: " + crossProjectCount);
                publish("Total operations: " + (sameProjectCount + crossProjectCount));
                publish("=".repeat(60));
                publish("\nProcess completed at: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));

                logBuilder.append("\n\n").append("=".repeat(60)).append("\n");
                logBuilder.append("BATCH CHERRY-PICK SUMMARY\n");
                logBuilder.append("=".repeat(60)).append("\n");
                logBuilder.append("Total URLs provided: ").append(urls.length).append("\n");
                logBuilder.append("Same-project cherry-picks executed: ").append(sameProjectCount).append("\n");
                logBuilder.append("Cross-project commands generated: ").append(crossProjectCount).append("\n");
                logBuilder.append("Total operations: ").append(sameProjectCount + crossProjectCount).append("\n");
                logBuilder.append("=".repeat(60)).append("\n");
                logBuilder.append("\nProcess completed at: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");

                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    appendLog(message + "\n");
                }
            }

            @Override
            protected void done() {
                appendLog("\n=== Batch Cherry-Pick Completed ===\n");

                // 保存日志文件
                saveLogToFile(logBuilder.toString());

                // 启用结果按钮
                if (!resultList.isEmpty()) {
                    resultButton.setEnabled(true);

                    // 自动显示结果对话框
                    SwingUtilities.invokeLater(() -> {
                        showResultsDialog();
                    });
                }
            }
        };

        worker.execute();
    }

    /**
     * 按时间排序 commit URLs
     */
    private void sortCommitUrlsByTime() {
        String urlsText = commitUrlsTextArea.getText().trim();
        
        if (urlsText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter commit URLs first.",
                    "No URLs to Sort",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        outputTextArea.setText("");
        appendLog("=== Sorting Commit URLs by Time ===\n");
        appendLog("Processing URLs and extracting commit information...\n\n");

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            private List<CommitTimeInfo> commitTimeInfos = new ArrayList<>();

            @Override
            protected Void doInBackground() throws Exception {
                String[] urls = urlsText.split("\n");
                
                publish("Found " + urls.length + " URLs to process");
                publish("=".repeat(50));

                for (int i = 0; i < urls.length; i++) {
                    String url = urls[i].trim();
                    if (url.isEmpty()) {
                        publish("Skipping empty line " + (i + 1));
                        continue;
                    }

                    publish("\nProcessing URL " + (i + 1) + ": " + url);

                    try {
                        // 解析 commit URL
                        CommitInfo commitInfo = parseCommitUrl(url);
                        if (commitInfo == null) {
                            publish("  ✗ Invalid URL format, skipping");
                            continue;
                        }

                        publish("  ✓ Parsed - Project: " + commitInfo.projectCode + ", Commit: " + commitInfo.commitId);

                        // 查找项目目录
                        File projectDir = findProjectDirectory(commitInfo.projectCode);
                        if (projectDir == null) {
                            publish("  ✗ Project directory not found, skipping");
                            continue;
                        }

                        publish("  ✓ Found project directory: " + projectDir.getName());

                        // 获取 commit 信息
                        CommitTimeInfo timeInfo = getCommitTimeInfo(projectDir, commitInfo.commitId, url);
                        if (timeInfo != null) {
                            commitTimeInfos.add(timeInfo);
                            publish("  ✓ Commit time: " + timeInfo.timeString + ", Author: " + timeInfo.author);
                        } else {
                            publish("  ✗ Failed to get commit information");
                        }

                    } catch (Exception e) {
                        publish("  ✗ Error processing URL: " + e.getMessage());
                    }
                }

                // 按时间排序 (正序 - 从早到晚)
                commitTimeInfos.sort(Comparator.comparing(info -> info.timestamp));

                publish("\n" + "=".repeat(50));
                publish("SORTING RESULTS");
                publish("=".repeat(50));
                publish("Successfully processed " + commitTimeInfos.size() + " commits");
                publish("Sorted by commit time (ascending order):");
                publish("");

                StringBuilder sortedUrls = new StringBuilder();
                for (int i = 0; i < commitTimeInfos.size(); i++) {
                    CommitTimeInfo info = commitTimeInfos.get(i);
                    publish((i + 1) + ". " + info.timeString + " | " + info.author + " | " + info.commitId.substring(0, 8));
                    publish("   " + info.url);
                    
                    sortedUrls.append(info.url);
                    if (i < commitTimeInfos.size() - 1) {
                        sortedUrls.append("\n");
                    }
                }

                // 更新文本区域中的 URLs
                SwingUtilities.invokeLater(() -> {
                    commitUrlsTextArea.setText(sortedUrls.toString());
                });

                publish("\n" + "=".repeat(50));
                publish("URLs have been reordered in the input area above");
                publish("Sort completed at: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    appendLog(message + "\n");
                }
            }

            @Override
            protected void done() {
                appendLog("\n=== Sort by Time Completed ===\n");
            }
        };

        worker.execute();
    }

    /**
     * 获取 commit 的时间信息
     */
    private CommitTimeInfo getCommitTimeInfo(File projectDir, String commitId, String url) {
        try {
            if (!GitInfoExtractor.isGitRepository(projectDir)) {
                return null;
            }

            org.eclipse.jgit.storage.file.FileRepositoryBuilder builder =
                new org.eclipse.jgit.storage.file.FileRepositoryBuilder();
            org.eclipse.jgit.lib.Repository repository = builder
                    .setGitDir(new java.io.File(projectDir, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            try (org.eclipse.jgit.revwalk.RevWalk revWalk = new org.eclipse.jgit.revwalk.RevWalk(repository)) {
                org.eclipse.jgit.lib.ObjectId objectId = repository.resolve(commitId);
                if (objectId == null) {
                    repository.close();
                    return null;
                }

                org.eclipse.jgit.revwalk.RevCommit commit = revWalk.parseCommit(objectId);
                
                CommitTimeInfo timeInfo = new CommitTimeInfo();
                timeInfo.url = url;
                timeInfo.commitId = commitId;
                timeInfo.timestamp = commit.getCommitTime() * 1000L; // Convert to milliseconds
                timeInfo.timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timeInfo.timestamp));
                timeInfo.author = commit.getAuthorIdent().getName();
                
                repository.close();
                return timeInfo;
                
            } catch (Exception e) {
                repository.close();
                return null;
            }

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析 commit URL
     * 支持两种格式：
     * 1. GitLab格式: https://gitlab.insuremo.com/project/path/-/commit/abc123
     * 2. GitHub格式: https://github.com/user/project/commit/abc123
     */
    private CommitInfo parseCommitUrl(String url) {
        try {
            // 移除末尾的 .git 如果存在
            String originalUrl = url;
            if (url.endsWith(".git")) {
                url = url.substring(0, url.length() - 4);
            }

            // 尝试匹配 GitLab 格式: /-/commit/
            Pattern gitlabPattern = Pattern.compile("(https?://[^/]+)/(.+)/-/commit/([a-fA-F0-9]+)");
            Matcher gitlabMatcher = gitlabPattern.matcher(url);

            // 尝试匹配 GitHub 格式: /commit/ (没有 /-/)
            Pattern githubPattern = Pattern.compile("(https?://[^/]+)/(.+)/commit/([a-fA-F0-9]+)");
            Matcher githubMatcher = githubPattern.matcher(url);

            Matcher matcher = null;
            String basePath = "";
            String commitId = null;
            String fullPath = null;

            if (gitlabMatcher.matches()) {
                // GitLab 格式
                matcher = gitlabMatcher;
                basePath = gitlabMatcher.group(2);
                commitId = gitlabMatcher.group(3);
                fullPath = basePath;
            } else if (githubMatcher.matches()) {
                // GitHub 格式
                matcher = githubMatcher;
                basePath = githubMatcher.group(2);
                commitId = githubMatcher.group(3);
                fullPath = basePath;
            } else {
                // 无法匹配任何格式
                return null;
            }

            String baseUrl = matcher.group(1) + "/" + basePath;

            // 提取项目代码：路径的最后一部分
            String[] pathParts = fullPath.split("/");
            String projectCode = null;

            for (int i = pathParts.length - 1; i >= 0; i--) {
                String part = pathParts[i];
                if (!part.equals("-")) {
                    projectCode = part;
                    break;
                }
            }

            if (projectCode == null) {
                return null;
            }

            CommitInfo info = new CommitInfo();
            info.projectCode = projectCode;
            info.commitId = commitId;
            info.baseUrl = baseUrl;
            info.fullUrl = originalUrl;

            return info;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 查找项目目录
     */
    private File findProjectDirectory(String projectCode) {
        if (currentDirectory == null || !currentDirectory.exists()) {
            return null;
        }

        File[] children = currentDirectory.listFiles();
        if (children == null) {
            return null;
        }

        for (File child : children) {
            if (child.isDirectory() && child.getName().equals(projectCode)) {
                return child;
            }
        }

        return null;
    }

    /**
     * 获取项目的 git remote URL
     */
    private String getRemoteUrl(File projectDir) {
        try {
            if (!GitInfoExtractor.isGitRepository(projectDir)) {
                return "Not a Git repository";
            }

            org.eclipse.jgit.storage.file.FileRepositoryBuilder builder =
                new org.eclipse.jgit.storage.file.FileRepositoryBuilder();
            org.eclipse.jgit.lib.Repository repository = builder
                    .setGitDir(new java.io.File(projectDir, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            String url = repository.getConfig().getString("remote", "origin", "url");
            repository.close();

            if (url != null && url.endsWith(".git")) {
                url = url.substring(0, url.length() - 4);
            }

            return url != null ? url : "No remote URL configured";

        } catch (Exception e) {
            return "Error reading remote URL: " + e.getMessage();
        }
    }

    /**
     * 判断是否是同一项目
     */
    private boolean isSameProject(String url1, String url2) {
        if (url1 == null || url2 == null) {
            return false;
        }

        // 移除末尾的 .git
        if (url1.endsWith(".git")) {
            url1 = url1.substring(0, url1.length() - 4);
        }
        if (url2.endsWith(".git")) {
            url2 = url2.substring(0, url2.length() - 4);
        }

        // 比较基础 URL
        return url1.equalsIgnoreCase(url2);
    }

    /**
     * 执行同项目 cherry-pick
     * 返回操作结果字符串列表用于日志输出
     */
    private CherryPickResult executeSameProjectCherryPick(File projectDir, String commitId, String targetBranch) {
        CherryPickResult result = new CherryPickResult();
        result.logs = new ArrayList<>();
        result.hasConflicts = false;
        result.conflictedFiles = new ArrayList<>();

        try {
            result.logs.add("[Branch Check] Checking current branch...");

            // 获取当前分支
            String currentBranch = GitOperations.getCurrentBranch(projectDir);
            result.logs.add("  Current branch: " + currentBranch);
            result.logs.add("  Target branch: " + targetBranch);

            // 如果当前分支不是目标分支，先切换
            if (!currentBranch.equals(targetBranch)) {
                result.logs.add("[Branch Switch] Branches differ, initiating switch...");
                result.logs.add("  From: " + currentBranch);
                result.logs.add("  To: " + targetBranch);

                boolean switched = GitOperations.switchBranch(projectDir, targetBranch);
                if (!switched) {
                    result.logs.add("  [FAILED] Branch switch failed");
                    result.success = false;
                    return result;
                }
                result.logs.add("  [OK] Branch switch completed");
            } else {
                result.logs.add("[Branch Check] Already on target branch, no switch needed");
            }

            // 执行 cherry-pick
            result.logs.add("[Cherry-Pick] Executing git cherry-pick...");
            result.logs.add("  Commit ID: " + commitId);
            result.logs.add("  Repository: " + projectDir.getAbsolutePath());

            boolean success = GitOperations.cherryPick(projectDir, commitId);

            if (success) {
                result.logs.add("  [OK] Cherry-pick command executed successfully");

                // 检查是否有冲突
                if (GitOperations.hasUncommittedChanges(projectDir)) {
                    result.logs.add("  [WARNING] Uncommitted changes detected");

                    // 获取冲突文件列表
                    List<String> conflictedFiles = GitOperations.getConflictedFiles(projectDir);
                    if (!conflictedFiles.isEmpty()) {
                        result.hasConflicts = true;
                        result.conflictedFiles = conflictedFiles;
                        result.logs.add("  [CONFLICT] Conflicted files:");
                        for (String file : conflictedFiles) {
                            result.logs.add("    - " + file);
                        }
                        result.logs.add("  Please resolve conflicts manually");
                        result.logs.add("  Use 'git status' to see details");
                    } else {
                        result.logs.add("  This may indicate cherry-pick conflicts");
                        result.logs.add("  Please check: git status");
                    }
                } else {
                    result.logs.add("  [OK] Clean cherry-pick (no conflicts)");
                }
                result.success = true;
            } else {
                result.logs.add("  [FAILED] Cherry-pick command failed");
                result.logs.add("  Possible causes:");
                result.logs.add("    - Commit ID not found");
                result.logs.add("    - Merge conflicts");
                result.logs.add("    - Repository errors");
                result.success = false;
            }

        } catch (Exception e) {
            result.logs.add("  [ERROR] Exception during cherry-pick");
            result.logs.add("  Error: " + e.getMessage());
            result.logs.add("  Type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            result.success = false;
        }

        return result;
    }

    /**
     * 生成跨项目 cherry-pick 命令
     */
    private List<String> generateCrossProjectCommands(String projectPath, String upstreamUrl, String commitId, String targetBranch) {
        List<String> commands = new ArrayList<>();

        commands.add(":: Cherry-pick commit " + commitId + " from upstream to " + targetBranch);
        commands.add("cd " + projectPath);
        commands.add("git switch " + targetBranch);
        commands.add("git remote remove upstream");
        commands.add("git remote add upstream " + upstreamUrl + ".git");
        commands.add("git fetch upstream");
        commands.add("git cherry-pick " + commitId);
        commands.add("");

        return commands;
    }

    /**
     * 复制命令到剪贴板
     */
    private void copyCommandsToClipboard() {
        String output = outputTextArea.getText();
        if (output.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No commands to copy.",
                    "Copy Commands",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try {
            java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(output);
            java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);

            JOptionPane.showMessageDialog(this,
                    "Commands copied to clipboard!",
                    "Copy Success",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to copy: " + e.getMessage(),
                    "Copy Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void appendLog(String message) {
        outputTextArea.append(message);
        outputTextArea.setCaretPosition(outputTextArea.getDocument().getLength());
    }

    /**
     * 递归设置容器及其所有子组件的字体
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
     * 显示结果对话框
     */
    private void showResultsDialog() {
        BatchCherryPickResultDialog dialog = new BatchCherryPickResultDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                resultList
        );
        dialog.setVisible(true);
    }

    /**
     * 保存日志到文件
     */
    private void saveLogToFile(String logContent) {
        try {
            // 创建logs目录
            String userHome = System.getProperty("user.home");
            File logDir = new File(userHome, ".git-viewer" + File.separator + "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            // 生成文件名：batch-cherry-pick-yyyyMMdd-HHmmss.log
            String timestamp = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            String fileName = "batch-cherry-pick-" + timestamp + ".log";
            File logFile = new File(logDir, fileName);

            // 写入日志
            try (FileWriter writer = new FileWriter(logFile)) {
                writer.write(logContent);
            }

            appendLog("\n✓ Log saved to: " + logFile.getAbsolutePath());

        } catch (Exception e) {
            appendLog("\n✗ Failed to save log: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cherry-Pick 状态枚举
     */
    public enum CherryPickStatus {
        SUCCESS("Success", SUCCESS_COLOR),
        FAIL("Fail", FAIL_COLOR),
        TO_RUN_CMD("To Run CMD", PENDING_COLOR);

        private final String displayName;
        private final Color color;

        CherryPickStatus(String displayName, Color color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Color getColor() {
            return color;
        }
    }

    /**
     * Cherry-Pick 结果项
     */
    public static class CherryPickResultItem {
        public int index;
        public String url;
        public String projectCode;
        public String commitId;
        public String projectPath;
        public String targetBranch;
        public boolean isSameProject;
        public CherryPickStatus status;
        public String errorMessage;
        public List<String> generatedCommands;
        public List<String> logs; // 该URL的详细日志
    }

    /**
     * Cherry-Pick 执行结果
     */
    private static class CherryPickResult {
        boolean success;
        List<String> logs;
        boolean hasConflicts; // 新增：是否有冲突
        List<String> conflictedFiles; // 新增：冲突文件列表
    }

    /**
     * Commit 信息封装类
     */
    private static class CommitInfo {
        String projectCode;
        String commitId;
        String baseUrl;
        String fullUrl;
    }

    /**
     * Commit 时间信息封装类
     */
    private static class CommitTimeInfo {
        String url;
        String commitId;
        long timestamp;
        String timeString;
        String author;
    }
}
