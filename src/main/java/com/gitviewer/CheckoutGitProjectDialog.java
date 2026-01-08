package com.gitviewer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.EventObject;

/**
 * Checkout Git项目对话框
 * 用于从远程Git仓库克隆项目到指定目录
 */
public class CheckoutGitProjectDialog extends JDialog {
    
    private JTextField gitUrlField;
    private JButton checkButton;
    private JComboBox<String> branchComboBox;
    private JButton downloadButton;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JTextArea logTextArea;

    // 新增：Group项目列表相关组件
    private JPanel projectsListPanel;
    private JLabel projectsCountLabel;
    private JTable projectsTable;
    private DefaultTableModel projectsTableModel;
    private List<GitLabProject> gitLabProjects = new ArrayList<>();

    private File targetDirectory;
    private boolean checkoutSuccess = false;
    private CredentialsProvider credentialsProvider = null;
    private List<String> allBranches = new ArrayList<>(); // 保存所有分支
    private boolean isFilteringBranches = false; // 过滤标志

    private static final Color PRIMARY_COLOR = new Color(25, 84, 166);
    private static final Color BORDER_COLOR = new Color(227, 233, 239);
    private static final Color ODD_ROW_COLOR = new Color(255, 255, 255);
    private static final Color EVEN_ROW_COLOR = new Color(245, 248, 250);
    private java.text.SimpleDateFormat logDateFormat = new java.text.SimpleDateFormat("HH:mm:ss");

    public CheckoutGitProjectDialog(Frame parent, File targetDirectory) {
        super(parent, "Checkout New Git Project", true);
        this.targetDirectory = targetDirectory;
        initializeUI();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(650, 600);  // 增加高度以容纳项目列表
        setResizable(false);

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        // 标题
        JLabel titleLabel = new JLabel("Clone Git Repository");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(5));

        // 目标目录显示
        JLabel targetLabel = new JLabel("Target Directory: " + targetDirectory.getAbsolutePath());
        targetLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        targetLabel.setForeground(new Color(95, 99, 104));
        targetLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(targetLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // Git URL 输入区域
        JPanel urlPanel = new JPanel(new BorderLayout(10, 0));
        urlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        urlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        JLabel urlLabel = new JLabel("Git URL:");
        urlLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        urlLabel.setPreferredSize(new Dimension(70, 28));
        urlPanel.add(urlLabel, BorderLayout.WEST);
        
        gitUrlField = new JTextField();
        gitUrlField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gitUrlField.setToolTipText("Enter the Git repository URL (e.g., https://github.com/user/repo.git)");
        urlPanel.add(gitUrlField, BorderLayout.CENTER);
        
        checkButton = new JButton("Check");
        checkButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        checkButton.setBackground(PRIMARY_COLOR);
        checkButton.setForeground(Color.WHITE);
        checkButton.setFocusPainted(false);
        checkButton.setBorderPainted(false);
        checkButton.setOpaque(true);
        checkButton.setPreferredSize(new Dimension(80, 28));
        checkButton.addActionListener(e -> queryRepository());
        urlPanel.add(checkButton, BorderLayout.EAST);

        mainPanel.add(urlPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // 分支选择区域（先添加，避免下拉框被遮挡）
        JPanel branchPanel = new JPanel(new BorderLayout(10, 0));
        branchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        branchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        JLabel branchLabel = new JLabel("Branch:");
        branchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        branchLabel.setPreferredSize(new Dimension(70, 28));
        branchPanel.add(branchLabel, BorderLayout.WEST);
        
        // 中间面板：分支下拉框和下载按钮
        JPanel branchCenterPanel = new JPanel(new BorderLayout(10, 0));
        
        branchComboBox = new JComboBox<>();
        branchComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        branchComboBox.setEnabled(false);
        branchComboBox.setEditable(true); // 设置为可编辑
        branchComboBox.setPreferredSize(new Dimension(250, 28));
        
        // 添加文本变化监听器，实现自动过滤
        JTextField branchTextField = (JTextField) branchComboBox.getEditor().getEditorComponent();
        branchTextField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (!isFilteringBranches) {
                    SwingUtilities.invokeLater(() -> filterBranches());
                }
            }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (!isFilteringBranches) {
                    SwingUtilities.invokeLater(() -> filterBranches());
                }
            }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (!isFilteringBranches) {
                    SwingUtilities.invokeLater(() -> filterBranches());
                }
            }
        });
        
        branchCenterPanel.add(branchComboBox, BorderLayout.WEST);
        
        downloadButton = new JButton("Download");
        downloadButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        downloadButton.setBackground(PRIMARY_COLOR);
        downloadButton.setForeground(Color.WHITE);
        downloadButton.setFocusPainted(false);
        downloadButton.setBorderPainted(false);
        downloadButton.setOpaque(true);
        downloadButton.setPreferredSize(new Dimension(100, 28));
        downloadButton.setEnabled(false);
        downloadButton.addActionListener(e -> checkoutRepository());
        branchCenterPanel.add(downloadButton, BorderLayout.EAST);
        
        branchPanel.add(branchCenterPanel, BorderLayout.CENTER);

        mainPanel.add(branchPanel);
        mainPanel.add(Box.createVerticalStrut(15));

        // ====== 新增：项目列表面板 ======
        projectsListPanel = new JPanel(new BorderLayout());
        projectsListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        projectsListPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Group Projects",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12)
        ));
        projectsListPanel.setVisible(false);  // 默认隐藏

        // 项目数量标签
        projectsCountLabel = new JLabel(" ");
        projectsCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        projectsCountLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // 创建表格
        String[] columnNames = {"Download", "Project Name", "Path"};
        projectsTableModel = new DefaultTableModel(new Object[][]{}, columnNames) {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) return Boolean.class;
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;  // 只有Download列可编辑
            }
        };

        projectsTable = new JTable(projectsTableModel);
        projectsTable.setRowHeight(28);
        projectsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        projectsTable.setAutoCreateRowSorter(true);

        // 设置列宽
        projectsTable.getColumnModel().getColumn(0).setPreferredWidth(60);   // Download
        projectsTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Project Name
        projectsTable.getColumnModel().getColumn(2).setPreferredWidth(300);  // Path

        // 设置表格样式
        setupTableStyle(projectsTable);

        // 复选框列设置
        projectsTable.getColumnModel().getColumn(0)
                .setCellRenderer(new CheckBoxCellRenderer());
        projectsTable.getColumnModel().getColumn(0)
                .setCellEditor(new CheckBoxCellEditor());

        JScrollPane projectsScroll = new JScrollPane(projectsTable);
        projectsScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 150));
        projectsScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        projectsListPanel.add(projectsCountLabel, BorderLayout.NORTH);
        projectsListPanel.add(projectsScroll, BorderLayout.CENTER);

        mainPanel.add(projectsListPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        // ====== 新增结束 ======

        // 状态显示
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(statusLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        // 进度条
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        mainPanel.add(progressBar);
        mainPanel.add(Box.createVerticalStrut(10));

        // 日志输出区域
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        logPanel.setBorder(BorderFactory.createTitledBorder("Log"));
        
        logTextArea = new JTextArea(8, 50);
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logTextArea.setBackground(new Color(250, 250, 250));
        JScrollPane logScroll = new JScrollPane(logTextArea);
        logPanel.add(logScroll, BorderLayout.CENTER);
        
        mainPanel.add(logPanel);

        add(mainPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 回车键触发查询
        gitUrlField.addActionListener(e -> queryRepository());
    }
    
    /**
     * 添加日志
     */
    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = logDateFormat.format(new java.util.Date());
            logTextArea.append("[" + timestamp + "] " + message + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        });
    }

    /**
     * 查询远程仓库，获取分支列表或Group项目列表
     */
    private void queryRepository() {
        String gitUrl = gitUrlField.getText().trim();
        if (gitUrl.isEmpty()) {
            showStatus("Please enter a Git URL", Color.RED);
            return;
        }

        // 禁用按钮，显示进度
        setQueryingState(true);
        showStatus("Checking repository...", new Color(95, 99, 104));
        appendLog("Checking URL: " + gitUrl);

        // 在后台线程执行查询
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception exception = null;

            @Override
            protected Void doInBackground() throws Exception {
                if (GitLabApiClient.isGroupUrl(gitUrl)) {
                    // Group URL模式
                    appendLog("Detected GitLab Group URL");
                    handleGroupUrl(gitUrl);
                } else {
                    // 单仓库模式（原有逻辑）
                    appendLog("Detected single repository URL");
                    handleSingleRepoUrl(gitUrl);
                }
                return null;
            }

            @Override
            protected void done() {
                setQueryingState(false);
                try {
                    get();
                } catch (Exception e) {
                    exception = e;
                    showStatus("Error: " + e.getMessage(), Color.RED);
                    appendLog("ERROR: " + e.getMessage());
                    handleApiError(e);
                }
            }
        };
        worker.execute();
    }

    /**
     * 查询远程仓库的分支列表
     */
    private List<String> queryRemoteBranches(String gitUrl) {
        List<String> branches = new ArrayList<>();
        
        try {
            appendLog("Connecting to remote repository...");
            LsRemoteCommand lsRemote = Git.lsRemoteRepository().setRemote(gitUrl);
            
            // 首先尝试不使用认证
            Collection<Ref> refs;
            try {
                refs = lsRemote.setHeads(true).call();
            } catch (Exception e) {
                // 如果失败，可能需要认证
                if (isAuthenticationError(e)) {
                    appendLog("Authentication required, requesting credentials...");
                    // 在EDT线程中显示认证对话框
                    final CredentialsProvider[] cp = new CredentialsProvider[1];
                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            cp[0] = getCredentialsProvider(gitUrl);
                        });
                    } catch (Exception ex) {
                        appendLog("ERROR: Failed to get credentials.");
                        return branches;
                    }
                    
                    if (cp[0] == null) {
                        appendLog("User cancelled authentication.");
                        return branches; // 用户取消了认证
                    }
                    
                    credentialsProvider = cp[0];
                    appendLog("Retrying with credentials...");
                    lsRemote = Git.lsRemoteRepository()
                            .setRemote(gitUrl)
                            .setCredentialsProvider(credentialsProvider);
                    refs = lsRemote.setHeads(true).call();
                } else {
                    throw e;
                }
            }
            
            appendLog("Fetching branch list...");
            for (Ref ref : refs) {
                String branchName = ref.getName();
                if (branchName.startsWith("refs/heads/")) {
                    String name = branchName.replace("refs/heads/", "");
                    branches.add(name);
                    appendLog("  Found branch: " + name);
                }
            }
        } catch (Exception e) {
            appendLog("ERROR: " + e.getMessage());
            System.err.println("Error querying remote branches: " + e.getMessage());
        }
        
        return branches;
    }

    /**
     * 检查是否是认证错误
     */
    private boolean isAuthenticationError(Exception e) {
        String message = e.getMessage();
        return message != null && (
            message.contains("Authentication") ||
            message.contains("not authorized") ||
            message.contains("authentication failed") ||
            message.contains("401") ||
            message.contains("403")
        );
    }

    /**
     * 获取认证提供者
     * 优先级：
     * 1. AppSettings中配置的gitLabUsername/gitLabPassword
     * 2. 会话期间保存的认证信息
     * 3. 弹出对话框让用户输入
     */
    private CredentialsProvider getCredentialsProvider(String repositoryUrl) {
        // 1. 首先检查AppSettings中是否配置了username/password
        AppSettings settings = AppSettings.getInstance();
        String configuredUsername = settings.getGitLabUsername();
        String configuredPassword = settings.getGitLabPassword();

        if (!configuredUsername.isEmpty() && !configuredPassword.isEmpty()) {
            appendLog("Using configured username/password from settings");
            return new UsernamePasswordCredentialsProvider(configuredUsername, configuredPassword);
        }

        // 2. 如果有保存的认证信息（会话期间），使用它
        if (GitCredentialsDialog.hasSavedCredentials()) {
            appendLog("Using saved session credentials");
            return new UsernamePasswordCredentialsProvider(
                GitCredentialsDialog.getSavedUsername(),
                GitCredentialsDialog.getSavedPassword()
            );
        }

        // 3. 显示认证对话框让用户输入
        appendLog("Requesting user credentials...");
        GitCredentialsDialog dialog = new GitCredentialsDialog(
            (Frame) SwingUtilities.getWindowAncestor(this), repositoryUrl);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            return new UsernamePasswordCredentialsProvider(
                dialog.getUsername(),
                dialog.getPassword()
            );
        }

        return null;
    }

    /**
     * 选择默认分支（优先 main，其次 master）
     */
    private void selectDefaultBranch() {
        for (int i = 0; i < branchComboBox.getItemCount(); i++) {
            String branch = branchComboBox.getItemAt(i);
            if ("main".equals(branch) || "master".equals(branch)) {
                branchComboBox.setSelectedIndex(i);
                return;
            }
        }
    }

    /**
     * 过滤分支列表
     */
    private void filterBranches() {
        if (allBranches == null || allBranches.isEmpty() || isFilteringBranches) {
            return;
        }
        
        isFilteringBranches = true;
        
        try {
            JTextField textField = (JTextField) branchComboBox.getEditor().getEditorComponent();
            String filterText = textField.getText();
            String filterTextLower = filterText.toLowerCase();
            
            // 保存当前光标位置
            int caretPosition = textField.getCaretPosition();
            
            // 清空并重新填充
            branchComboBox.removeAllItems();
            
            if (filterText.isEmpty()) {
                // 如果文本为空，显示所有分支
                for (String branch : allBranches) {
                    branchComboBox.addItem(branch);
                }
            } else {
                // 根据过滤文本筛选分支
                boolean hasMatches = false;
                for (String branch : allBranches) {
                    if (branch.toLowerCase().contains(filterTextLower)) {
                        branchComboBox.addItem(branch);
                        hasMatches = true;
                    }
                }
                
                // 如果没有匹配项，显示所有分支
                if (!hasMatches) {
                    for (String branch : allBranches) {
                        branchComboBox.addItem(branch);
                    }
                }
                
                // 显示下拉列表（只在有输入时，且组件可见且可显示时）
                if (branchComboBox.getItemCount() > 0 && branchComboBox.isShowing() && branchComboBox.isDisplayable()) {
                    try {
                        branchComboBox.showPopup();
                    } catch (IllegalComponentStateException | IllegalArgumentException e) {
                        // 忽略组件未完全显示时的异常
                    }
                }
            }
            
            // 恢复文本和光标位置（重要：保持用户输入）
            textField.setText(filterText);
            textField.setCaretPosition(Math.min(caretPosition, filterText.length()));
            
        } catch (Exception e) {
            // 捕获所有可能的异常，确保不影响主流程
        } finally {
            isFilteringBranches = false;
        }
    }

    /**
     * 执行 checkout（克隆）操作
     * 支持单仓库和Group批量下载
     */
    private void checkoutRepository() {
        String gitUrl = gitUrlField.getText().trim();

        // 判断是单仓库还是Group批量下载
        if (GitLabApiClient.isGroupUrl(gitUrl)) {
            checkoutMultipleRepositories(gitUrl);
        } else {
            checkoutSingleRepository(gitUrl);
        }
    }

    /**
     * 从 Git URL 提取项目名称
     */
    private String extractProjectName(String gitUrl) {
        String name = gitUrl;
        // 移除 .git 后缀
        if (name.endsWith(".git")) {
            name = name.substring(0, name.length() - 4);
        }
        // 获取最后一个 / 后的部分
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        // 处理 : 的情况（SSH URL）
        int lastColon = name.lastIndexOf(':');
        if (lastColon >= 0) {
            name = name.substring(lastColon + 1);
        }
        return name.isEmpty() ? "project" : name;
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }

    private void setQueryingState(boolean querying) {
        checkButton.setEnabled(!querying);
        gitUrlField.setEnabled(!querying);
        progressBar.setIndeterminate(querying);
        progressBar.setVisible(querying);

        // Group模式下，禁用表格
        if (projectsTable != null) {
            projectsTable.setEnabled(!querying);
        }
    }

    private void setCheckoutState(boolean checking) {
        downloadButton.setEnabled(!checking);
        checkButton.setEnabled(!checking);
        gitUrlField.setEnabled(!checking);
        branchComboBox.setEnabled(!checking);
        progressBar.setIndeterminate(checking);
        progressBar.setVisible(checking);

        // 禁用项目表格
        if (projectsTable != null) {
            projectsTable.setEnabled(!checking);
        }
    }

    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    // ====== 新增方法：Group URL处理和批量下载 ======

    /**
     * 处理Group URL
     */
    private void handleGroupUrl(String groupUrl) throws Exception {
        appendLog("========================================");
        appendLog("Starting Group URL processing...");
        appendLog("Input URL: " + groupUrl);

        // 2. 调用GitLab API获取项目列表（使用新的智能认证方法）
        appendLog("Fetching group projects from GitLab API...");

        // 打印URL分解信息
        String baseUrl = GitLabApiClient.extractBaseUrl(groupUrl);
        String groupPath = GitLabApiClient.extractGroupPath(groupUrl);
        String apiUrl = GitLabApiClient.buildApiUrl(baseUrl, groupPath);

        appendLog("URL breakdown:");
        appendLog("  Base URL: " + baseUrl);
        appendLog("  Group path: " + groupPath);
        appendLog("  API URL: " + apiUrl);

        appendLog("Calling GitLabApiClient.fetchGroupProjectsWithAuth()...");

        List<GitLabProject> projects;
        try {
            // 使用新的智能认证方法
            Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
            projects = GitLabApiClient.fetchGroupProjectsWithAuth(groupUrl, parentFrame);
            appendLog("GitLabApiClient returned " + projects.size() + " projects");

            // 如果使用了配置的认证信息成功，保存为credentialsProvider用于后续克隆操作
            AppSettings settings = AppSettings.getInstance();
            String token = settings.getGitLabPrivateToken();
            String username = settings.getGitLabUsername();
            String password = settings.getGitLabPassword();

            if (!token.isEmpty()) {
                // Token方式：创建一个特殊的CredentialsProvider
                // 注意：JGit的clone操作不支持直接使用token，需要转换为username/password形式
                // GitLab的token可以作为密码，用户名可以是任意值
                credentialsProvider = new UsernamePasswordCredentialsProvider("gitlab-token", token);
                appendLog("Using Private Token for subsequent Git operations");
            } else if (!username.isEmpty() && !password.isEmpty()) {
                credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
                appendLog("Using configured username/password for subsequent Git operations - Username: " + username);
            }

        } catch (Exception e) {
            appendLog("ERROR: Failed to fetch projects: " + e.getMessage());
            appendLog("Exception type: " + e.getClass().getName());
            e.printStackTrace();
            throw e;
        }

        if (projects.isEmpty()) {
            appendLog("WARNING: No projects found in this group!");
            throw new Exception("No projects found in this group");
        }

        appendLog("Successfully found " + projects.size() + " projects in group");
        appendLog("========================================");

        // 3. 填充项目列表UI
        SwingUtilities.invokeLater(() -> {
            gitLabProjects.clear();
            gitLabProjects.addAll(projects);
            populateProjectList(projects);

            // 4. 查询项目分支（优先查找"nb-bff"项目）
            if (!projects.isEmpty()) {
                // 优先查找名为"nb-bff"的项目
                GitLabProject targetProject = null;
                for (GitLabProject project : projects) {
                    if ("nb-bff".equals(project.name)) {
                        targetProject = project;
                        appendLog("Found 'nb-bff' project, will use its branches");
                        break;
                    }
                }

                // 如果没找到"nb-bff"，使用第一个项目
                if (targetProject == null) {
                    targetProject = projects.get(0);
                    appendLog("'nb-bff' project not found, using first project: " + targetProject.name);
                }

                final String projectName = targetProject.name;
                appendLog("Querying branches for project: " + projectName);

                // 构建项目Git URL
                final String projectUrl;
                String tempUrl = groupUrl;
                if (tempUrl.endsWith("/")) {
                    tempUrl = tempUrl.substring(0, tempUrl.length() - 1);
                }
                projectUrl = tempUrl + "/" + targetProject.path + ".git";

                SwingWorker<List<String>, Void> branchWorker = new SwingWorker<List<String>, Void>() {
                    @Override
                    protected List<String> doInBackground() {
                        return queryRemoteBranches(projectUrl);
                    }

                    @Override
                    protected void done() {
                        try {
                            List<String> branches = get();
                            if (branches != null && !branches.isEmpty()) {
                                allBranches.clear();
                                allBranches.addAll(branches);

                                isFilteringBranches = true;
                                branchComboBox.removeAllItems();
                                for (String branch : branches) {
                                    branchComboBox.addItem(branch);
                                }
                                isFilteringBranches = false;

                                selectDefaultBranch();
                                branchComboBox.setEnabled(true);
                                downloadButton.setEnabled(true);

                                appendLog("Found " + branches.size() + " branches for project: " + projectName);
                            }
                        } catch (Exception e) {
                            appendLog("ERROR fetching branches: " + e.getMessage());
                        }
                    }
                };
                branchWorker.execute();
            }
        });
    }

    /**
     * 处理单仓库URL（原有逻辑）
     */
    private void handleSingleRepoUrl(String gitUrl) throws Exception {
        List<String> branches = queryRemoteBranches(gitUrl);

        if (branches == null || branches.isEmpty()) {
            throw new Exception("No branches found or repository does not exist.");
        }

        // 保存所有分支到成员变量
        allBranches.clear();
        allBranches.addAll(branches);

        // 填充下拉框
        SwingUtilities.invokeLater(() -> {
            // 隐藏项目列表面板（单仓库模式下不需要显示）
            projectsListPanel.setVisible(false);

            isFilteringBranches = true;
            branchComboBox.removeAllItems();
            for (String branch : branches) {
                branchComboBox.addItem(branch);
            }
            isFilteringBranches = false;

            // 默认选择 master 或 main
            selectDefaultBranch();
            branchComboBox.setEnabled(true);
            downloadButton.setEnabled(true);

            showStatus("Found " + branches.size() + " branches. Select a branch and click Download.", PRIMARY_COLOR);
            appendLog("Repository check successful. Found " + branches.size() + " branches.");
        });
    }

    /**
     * 填充项目列表表格
     */
    private void populateProjectList(List<GitLabProject> projects) {
        projectsTableModel.setRowCount(0);

        for (GitLabProject project : projects) {
            Object[] row = {
                    false,  // 默认不选中
                    project.name,
                    project.path
            };
            projectsTableModel.addRow(row);
        }

        // 显示项目列表面板
        projectsListPanel.setVisible(true);
        projectsCountLabel.setText("Found " + projects.size() + " projects");
        projectsCountLabel.setForeground(PRIMARY_COLOR);

        showStatus("Found " + projects.size() + " projects. Select projects to download.", PRIMARY_COLOR);
    }

    /**
     * 批量克隆多个仓库
     */
    private void checkoutMultipleRepositories(String groupUrl) {
        JTextField branchTextField = (JTextField) branchComboBox.getEditor().getEditorComponent();
        String branch = branchTextField.getText().trim();

        if (branch.isEmpty()) {
            showStatus("Please select a branch first.", Color.RED);
            return;
        }

        // 获取选中的项目
        List<GitLabProject> selectedProjects = getSelectedProjects();

        if (selectedProjects.isEmpty()) {
            showStatus("Please select at least one project to download.", Color.RED);
            return;
        }

        // 确认下载
        int result = JOptionPane.showConfirmDialog(this,
                "Download " + selectedProjects.size() + " selected projects?\n\n" +
                        "Branch: " + branch + "\n" +
                        "Target: " + targetDirectory.getAbsolutePath(),
                "Confirm Batch Download",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        // 禁用UI，显示进度
        setCheckoutState(true);
        showStatus("Downloading " + selectedProjects.size() + " projects...", new Color(95, 99, 104));
        appendLog("Starting batch download of " + selectedProjects.size() + " projects");

        SwingWorker<BatchDownloadResult, Void> worker = new SwingWorker<>() {
            @Override
            protected BatchDownloadResult doInBackground() {
                BatchDownloadResult result = new BatchDownloadResult();
                result.totalCount = selectedProjects.size();

                for (int i = 0; i < selectedProjects.size(); i++) {
                    GitLabProject project = selectedProjects.get(i);
                    appendLog("[" + (i + 1) + "/" + selectedProjects.size() + "] Downloading: " + project.name);

                    try {
                        // 构建项目Git URL
                        String projectGitUrl = groupUrl;
                        if (projectGitUrl.endsWith("/")) {
                            projectGitUrl = projectGitUrl.substring(0, projectGitUrl.length() - 1);
                        }
                        projectGitUrl = projectGitUrl + "/" + project.path + ".git";

                        File projectDir = new File(targetDirectory, project.name);

                        if (projectDir.exists()) {
                            appendLog("  - Deleting existing directory...");
                            deleteDirectory(projectDir);
                        }

                        org.eclipse.jgit.api.CloneCommand cloneCommand = Git.cloneRepository()
                                .setURI(projectGitUrl)
                                .setDirectory(projectDir)
                                .setBranch(branch);

                        if (credentialsProvider != null) {
                            cloneCommand.setCredentialsProvider(credentialsProvider);
                        }

                        try (Git git = cloneCommand.call()) {
                            result.successCount++;
                            result.successfulProjects.add(project.name);
                            appendLog("  ✓ Downloaded successfully");
                        }
                    } catch (Exception e) {
                        result.failedCount++;
                        result.failedProjects.add(project.name + ": " + e.getMessage());
                        appendLog("  ✗ Failed: " + e.getMessage());
                    }
                }

                return result;
            }

            @Override
            protected void done() {
                setCheckoutState(false);
                try {
                    BatchDownloadResult result = get();

                    StringBuilder message = new StringBuilder();
                    message.append("Batch download completed!\n\n");
                    message.append("Successfully downloaded: ").append(result.successCount).append("\n");
                    message.append("Failed: ").append(result.failedCount).append("\n\n");

                    if (result.failedCount > 0) {
                        message.append("Failed projects:\n");
                        for (String failed : result.failedProjects) {
                            message.append("  - ").append(failed).append("\n");
                        }
                    }

                    showStatus("Batch download completed", PRIMARY_COLOR);
                    appendLog("Batch download completed: " + result.successCount + " succeeded, " +
                            result.failedCount + " failed");

                    JOptionPane.showMessageDialog(CheckoutGitProjectDialog.this,
                            message.toString(),
                            "Batch Download Result",
                            result.failedCount > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);

                    if (result.failedCount == 0) {
                        checkoutSuccess = true;
                        dispose();
                    }
                } catch (Exception e) {
                    showStatus("Error: " + e.getMessage(), Color.RED);
                    appendLog("ERROR: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * 单个仓库克隆（原有逻辑）
     */
    private void checkoutSingleRepository(String gitUrl) {
        JTextField textField = (JTextField) branchComboBox.getEditor().getEditorComponent();
        String branch = textField.getText().trim();

        if (gitUrl.isEmpty() || branch == null || branch.isEmpty()) {
            showStatus("Please check the repository first.", Color.RED);
            return;
        }

        // 确定项目目录名称
        String projectName = extractProjectName(gitUrl);
        File projectDir = new File(targetDirectory, projectName);

        if (projectDir.exists()) {
            int result = JOptionPane.showConfirmDialog(this,
                    "Directory '" + projectName + "' already exists.\nDo you want to overwrite it?",
                    "Directory Exists",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) {
                return;
            }
            // 删除已存在的目录
            appendLog("Deleting existing directory: " + projectDir.getAbsolutePath());
            deleteDirectory(projectDir);
        }

        // 禁用按钮，显示进度
        setCheckoutState(true);
        showStatus("Downloading repository to " + projectDir.getName() + "...", new Color(95, 99, 104));
        appendLog("Starting download...");
        appendLog("Target directory: " + projectDir.getAbsolutePath());
        appendLog("Branch: " + branch);

        // 在后台线程执行克隆
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;

            @Override
            protected Boolean doInBackground() {
                try {
                    appendLog("Cloning repository...");
                    org.eclipse.jgit.api.CloneCommand cloneCommand = Git.cloneRepository()
                            .setURI(gitUrl)
                            .setDirectory(projectDir)
                            .setBranch(branch);

                    if (credentialsProvider != null) {
                        cloneCommand.setCredentialsProvider(credentialsProvider);
                    }

                    try (Git git = cloneCommand.call()) {
                        appendLog("Clone completed successfully.");
                        return true;
                    }
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    appendLog("ERROR: " + e.getMessage());
                    System.err.println("Error cloning repository: " + e.getMessage());
                    return false;
                }
            }

            @Override
            protected void done() {
                setCheckoutState(false);
                try {
                    if (get()) {
                        checkoutSuccess = true;
                        showStatus("Successfully downloaded to: " + projectDir.getAbsolutePath(), PRIMARY_COLOR);
                        appendLog("Download completed successfully!");
                        JOptionPane.showMessageDialog(CheckoutGitProjectDialog.this,
                                "Repository downloaded successfully!\n\nLocation: " + projectDir.getAbsolutePath(),
                                "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        showStatus("Download failed: " + (errorMessage != null ? errorMessage : "Unknown error"), Color.RED);
                    }
                } catch (Exception e) {
                    showStatus("Error: " + e.getMessage(), Color.RED);
                    appendLog("ERROR: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * 获取表格中选中的项目
     */
    private List<GitLabProject> getSelectedProjects() {
        List<GitLabProject> selected = new ArrayList<>();

        for (int i = 0; i < projectsTableModel.getRowCount(); i++) {
            Boolean isSelected = (Boolean) projectsTableModel.getValueAt(i, 0);
            if (isSelected != null && isSelected) {
                String name = (String) projectsTableModel.getValueAt(i, 1);
                String path = (String) projectsTableModel.getValueAt(i, 2);

                // 从gitLabProjects中查找对应的项目
                for (GitLabProject project : gitLabProjects) {
                    if (project.name.equals(name) && project.path.equals(path)) {
                        selected.add(project);
                        break;
                    }
                }
            }
        }

        return selected;
    }

    /**
     * 设置表格样式
     */
    private void setupTableStyle(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setForeground(new Color(60, 64, 67));
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);

        // 设置行高和斑马纹
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? ODD_ROW_COLOR : EVEN_ROW_COLOR);
                }
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return c;
            }
        });
    }

    /**
     * 统一错误处理
     */
    private void handleApiError(Exception e) {
        String message = e.getMessage();

        if (message != null) {
            if (message.contains("401") || message.contains("Authentication failed")) {
                SwingUtilities.invokeLater(() -> {
                    int retry = JOptionPane.showConfirmDialog(this,
                            "Authentication failed.\n\n" +
                                    "Please check your username and password.\n" +
                                    "Do you want to try again?",
                            "Authentication Error",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.ERROR_MESSAGE);

                    if (retry == JOptionPane.YES_OPTION) {
                        // 清除认证信息，让用户重新输入
                        credentialsProvider = null;
                        GitCredentialsDialog.clearSavedCredentials();
                        queryRepository();
                    }
                });
            } else if (message.contains("403") || message.contains("Access denied")) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Access denied.\n\n" +
                                        "You may not have permission to access this group or its projects.\n" +
                                        "Please contact your GitLab administrator.",
                                "Access Denied",
                                JOptionPane.ERROR_MESSAGE)
                );
            } else if (message.contains("404") || message.contains("not found")) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                                "Group not found.\n\n" +
                                        "Please check the group URL and try again.",
                                "Group Not Found",
                                JOptionPane.ERROR_MESSAGE)
                );
            }
        }
    }

    /**
     * 批量下载结果数据类
     */
    private static class BatchDownloadResult {
        int totalCount = 0;
        int successCount = 0;
        int failedCount = 0;
        List<String> successfulProjects = new ArrayList<>();
        List<String> failedProjects = new ArrayList<>();
    }

    // ====== 新增内部类：CheckBox渲染器和编辑器 ======

    /**
     * CheckBox单元格渲染器
     */
    private class CheckBoxCellRenderer extends JCheckBox implements TableCellRenderer {
        public CheckBoxCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setSelected(value != null && (Boolean) value);
            setBackground(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
            setIcon(createCheckIcon(isSelected()));
            return this;
        }

        private Icon createCheckIcon(boolean checked) {
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int size = 16;

                    // 绘制外框
                    g2.setColor(new Color(200, 200, 200));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x + 2, y + 2, size - 4, size - 4, 3, 3);

                    if (checked) {
                        // 填充背景
                        g2.setColor(PRIMARY_COLOR);
                        g2.fillRoundRect(x + 2, y + 2, size - 4, size - 4, 3, 3);

                        // 绘制勾选标记
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int[] checkX = {x + 5, x + 8, x + 13};
                        int[] checkY = {y + 8, y + 11, y + 6};
                        for (int i = 0; i < checkX.length - 1; i++) {
                            g2.drawLine(checkX[i], checkY[i], checkX[i + 1], checkY[i + 1]);
                        }
                    }

                    g2.dispose();
                }

                @Override
                public int getIconWidth() {
                    return 16;
                }

                @Override
                public int getIconHeight() {
                    return 16;
                }
            };
        }
    }

    /**
     * CheckBox单元格编辑器
     */
    private class CheckBoxCellEditor extends AbstractCellEditor implements TableCellEditor {
        private JCheckBox checkBox;

        public CheckBoxCellEditor() {
            checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setBorderPainted(false);
            checkBox.setFocusPainted(false);
            checkBox.setOpaque(true);
            checkBox.addActionListener(e -> {
                updateCheckBoxIcon();
                fireEditingStopped();
            });
        }

        private void updateCheckBoxIcon() {
            if (checkBox.isSelected()) {
                checkBox.setIcon(createCheckIcon(true));
                checkBox.setSelectedIcon(createCheckIcon(true));
            } else {
                checkBox.setIcon(createCheckIcon(false));
                checkBox.setSelectedIcon(createCheckIcon(false));
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            checkBox.setSelected(value != null && (Boolean) value);
            checkBox.setBackground(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
            updateCheckBoxIcon();
            return checkBox;
        }

        @Override
        public Object getCellEditorValue() {
            return checkBox.isSelected();
        }

        private Icon createCheckIcon(boolean checked) {
            return new Icon() {
                @Override
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    int size = 16;

                    // 绘制外框
                    g2.setColor(new Color(200, 200, 200));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x + 2, y + 2, size - 4, size - 4, 3, 3);

                    if (checked) {
                        // 填充背景
                        g2.setColor(PRIMARY_COLOR);
                        g2.fillRoundRect(x + 2, y + 2, size - 4, size - 4, 3, 3);

                        // 绘制勾选标记
                        g2.setColor(Color.WHITE);
                        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int[] checkX = {x + 5, x + 8, x + 13};
                        int[] checkY = {y + 8, y + 11, y + 6};
                        for (int i = 0; i < checkX.length - 1; i++) {
                            g2.drawLine(checkX[i], checkY[i], checkX[i + 1], checkY[i + 1]);
                        }
                    }

                    g2.dispose();
                }

                @Override
                public int getIconWidth() {
                    return 16;
                }

                @Override
                public int getIconHeight() {
                    return 16;
                }
            };
        }
    }


    public boolean isCheckoutSuccess() {
        return checkoutSuccess;
    }
}
