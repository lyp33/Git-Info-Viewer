package com.gitviewer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    
    private File targetDirectory;
    private boolean checkoutSuccess = false;
    private CredentialsProvider credentialsProvider = null;
    
    private static final Color PRIMARY_COLOR = new Color(25, 84, 166);
    private java.text.SimpleDateFormat logDateFormat = new java.text.SimpleDateFormat("HH:mm:ss");

    public CheckoutGitProjectDialog(Frame parent, File targetDirectory) {
        super(parent, "Checkout New Git Project", true);
        this.targetDirectory = targetDirectory;
        initializeUI();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(650, 450);
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

        // 分支选择区域
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
        branchComboBox.setPreferredSize(new Dimension(250, 28));
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
     * 查询远程仓库，获取分支列表
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
        appendLog("Checking repository: " + gitUrl);

        // 在后台线程执行查询
        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            private String errorMessage = null;

            @Override
            protected List<String> doInBackground() {
                return queryRemoteBranches(gitUrl);
            }

            @Override
            protected void done() {
                setQueryingState(false);
                try {
                    List<String> branches = get();
                    if (branches != null && !branches.isEmpty()) {
                        branchComboBox.removeAllItems();
                        for (String branch : branches) {
                            branchComboBox.addItem(branch);
                        }
                        // 默认选择 master 或 main
                        selectDefaultBranch();
                        branchComboBox.setEnabled(true);
                        downloadButton.setEnabled(true);
                        showStatus("Found " + branches.size() + " branches. Select a branch and click Download.", PRIMARY_COLOR);
                        appendLog("Repository check successful. Found " + branches.size() + " branches.");
                    } else {
                        showStatus("No branches found or repository does not exist.", Color.RED);
                        appendLog("ERROR: No branches found or repository does not exist.");
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
     */
    private CredentialsProvider getCredentialsProvider(String repositoryUrl) {
        // 如果有保存的认证信息，使用它
        if (GitCredentialsDialog.hasSavedCredentials()) {
            return new UsernamePasswordCredentialsProvider(
                GitCredentialsDialog.getSavedUsername(),
                GitCredentialsDialog.getSavedPassword()
            );
        }

        // 显示认证对话框
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
     * 执行 checkout（克隆）操作
     */
    private void checkoutRepository() {
        String gitUrl = gitUrlField.getText().trim();
        String branch = (String) branchComboBox.getSelectedItem();
        
        if (gitUrl.isEmpty() || branch == null) {
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
    }

    private void setCheckoutState(boolean checking) {
        downloadButton.setEnabled(!checking);
        checkButton.setEnabled(!checking);
        gitUrlField.setEnabled(!checking);
        branchComboBox.setEnabled(!checking);
        progressBar.setIndeterminate(checking);
        progressBar.setVisible(checking);
    }

    private void showStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    public boolean isCheckoutSuccess() {
        return checkoutSuccess;
    }
}
