package com.gitviewer;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Git 仓库详细信息对话框
 */
public class RepoDetailsDialog extends JDialog {

    private static final Color HEADER_BG_COLOR = new Color(248, 249, 250);
    private static final Color BORDER_COLOR = new Color(227, 233, 239);
    private static final Color ODD_ROW_COLOR = new Color(255, 255, 255);
    private static final Color EVEN_ROW_COLOR = new Color(245, 248, 250);

    private JTable commitsTable;
    private DefaultTableModel tableModel;
    private SimpleDateFormat dateFormat;
    private JLabel remoteLabel;
    private JTextArea filesTextArea;
    private File currentRepoDir;
    private java.util.Map<String, String> shortIdToFullIdMap;
    private JComboBox<Integer> displaySizeComboBox;
    private JTextField messageSearchField;
    private JComboBox<String> authorFilterComboBox;
    private int currentDisplaySize = 20;
    private List<GitInfoExtractor.GitCommitInfo> allCommits; // 存储所有提交记录
    private List<GitInfoExtractor.GitCommitInfo> filteredCommits; // 存储过滤后的提交记录
    private String currentCommitUrl; // 当前显示的commit URL

    public RepoDetailsDialog(Frame parent) {
        super(parent, "Git Repository Details", true);
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.shortIdToFullIdMap = new java.util.HashMap<>();
        this.allCommits = new java.util.ArrayList<>();
        this.filteredCommits = new java.util.ArrayList<>();
        initializeUI();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(900, 750);

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Remote Path 面板
        JPanel remotePanel = new JPanel(new BorderLayout(10, 10));
        remotePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Remote Repository",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12)
        ));
        remotePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        remoteLabel = new JLabel("No repository selected");
        remoteLabel.setForeground(new Color(60, 64, 67));
        remotePanel.add(remoteLabel, BorderLayout.CENTER);

        mainPanel.add(remotePanel);
        mainPanel.add(Box.createVerticalStrut(10));

        // 提交记录表格
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Recent Commits",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12)
        ));

        // 工具栏 - 包含Display Size、Message搜索和Author过滤
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        // Display Size下拉框 - 扩展到2000，按100增量
        JLabel displaySizeLabel = new JLabel("Display Size:");
        displaySizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        // 创建100的增量数组到2000
        java.util.List<Integer> sizeList = new java.util.ArrayList<>();
        sizeList.add(20);
        sizeList.add(50);
        for (int i = 100; i <= 2000; i += 100) {
            sizeList.add(i);
        }
        Integer[] sizes = sizeList.toArray(new Integer[0]);
        
        displaySizeComboBox = new JComboBox<>(sizes);
        displaySizeComboBox.setSelectedItem(20);
        displaySizeComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        displaySizeComboBox.addActionListener(e -> {
            Integer selectedSize = (Integer) displaySizeComboBox.getSelectedItem();
            if (selectedSize != null && selectedSize != currentDisplaySize) {
                currentDisplaySize = selectedSize;
                reloadCommits();
            }
        });
        
        // Message搜索框
        JLabel messageSearchLabel = new JLabel("Message Filter:");
        messageSearchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        messageSearchField = new JTextField();
        messageSearchField.setPreferredSize(new Dimension(200, 25));
        messageSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageSearchField.setToolTipText("Enter keywords to filter commit messages");
        
        // 添加实时搜索监听器
        messageSearchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });
        
        // Author过滤下拉框
        JLabel authorFilterLabel = new JLabel("Author:");
        authorFilterLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        authorFilterComboBox = new JComboBox<>();
        authorFilterComboBox.setPreferredSize(new Dimension(150, 25));
        authorFilterComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        authorFilterComboBox.addActionListener(e -> applyFilters());
        
        // 清除过滤器按钮
        JButton clearFiltersButton = new JButton("Clear Filters");
        clearFiltersButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        clearFiltersButton.addActionListener(e -> clearFilters());
        
        toolbarPanel.add(displaySizeLabel);
        toolbarPanel.add(displaySizeComboBox);
        toolbarPanel.add(Box.createHorizontalStrut(15));
        toolbarPanel.add(messageSearchLabel);
        toolbarPanel.add(messageSearchField);
        toolbarPanel.add(Box.createHorizontalStrut(15));
        toolbarPanel.add(authorFilterLabel);
        toolbarPanel.add(authorFilterComboBox);
        toolbarPanel.add(Box.createHorizontalStrut(10));
        toolbarPanel.add(clearFiltersButton);
        
        tablePanel.add(toolbarPanel, BorderLayout.NORTH);

        // 创建表格
        String[] columnNames = {"Commit Code", "Date", "Author", "Message"};
        tableModel = new NonEditableTableModel(new Object[][]{}, columnNames);
        commitsTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
                }
                return c;
            }
        };

        commitsTable.setAutoCreateRowSorter(true);
        commitsTable.setRowHeight(28);
        commitsTable.setIntercellSpacing(new Dimension(0, 0));
        commitsTable.setShowGrid(false);
        commitsTable.setSelectionBackground(new Color(187, 222, 251));
        commitsTable.setSelectionForeground(new Color(0, 0, 0));
        commitsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // 表头样式
        JTableHeader header = commitsTable.getTableHeader();
        header.setBackground(HEADER_BG_COLOR);
        header.setForeground(new Color(95, 99, 104));
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 35));

        // 设置列宽
        commitsTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // Commit Code
        commitsTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Date
        commitsTable.getColumnModel().getColumn(2).setPreferredWidth(120); // Author
        commitsTable.getColumnModel().getColumn(3).setPreferredWidth(450); // Message

        JScrollPane tableScroll = new JScrollPane(commitsTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        tableScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 250));
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        mainPanel.add(tablePanel);

        // 文件变更显示区域
        JPanel filesPanel = new JPanel(new BorderLayout());
        filesPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Changed Files",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12)
        ));

        filesTextArea = new JTextArea();
        filesTextArea.setEditable(false);
        filesTextArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        filesTextArea.setBackground(new Color(248, 249, 250));
        filesTextArea.setMargin(new Insets(10, 10, 10, 10));

        // 添加鼠标监听器来处理链接点击
        filesTextArea.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && currentCommitUrl != null) {
                    // 双击时打开commit链接
                    openUrlInBrowser(currentCommitUrl);
                }
            }
        });

        // 添加工具提示
        filesTextArea.setToolTipText("Double-click to open commit link in browser");

        JScrollPane filesScroll = new JScrollPane(filesTextArea);
        filesScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        filesScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));
        filesPanel.add(filesScroll, BorderLayout.CENTER);

        mainPanel.add(filesPanel);

        add(mainPanel, BorderLayout.CENTER);

        // 底部按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // 添加表格选择监听器
        commitsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = commitsTable.getSelectedRow();
                if (selectedRow >= 0) {
                    // 获取提交ID（考虑排序后的行索引）
                    int modelRow = commitsTable.convertRowIndexToModel(selectedRow);
                    String shortCommitId = (String) tableModel.getValueAt(modelRow, 0);
                    loadCommitFiles(shortCommitId);
                }
            }
        });

        // 在所有组件创建完成后，统一设置12号字体
        Font defaultFont = new Font("Segoe UI", Font.PLAIN, 12);
        applyFontRecursive(this, defaultFont);
    }

    /**
     * 应用过滤器
     */
    private void applyFilters() {
        if (allCommits.isEmpty()) {
            return;
        }
        
        String messageFilter = messageSearchField.getText().toLowerCase().trim();
        String authorFilter = (String) authorFilterComboBox.getSelectedItem();
        
        filteredCommits.clear();
        
        for (GitInfoExtractor.GitCommitInfo commit : allCommits) {
            boolean matchMessage = messageFilter.isEmpty() || 
                    commit.getMessage().toLowerCase().contains(messageFilter);
            
            boolean matchAuthor = authorFilter == null || 
                    "All Authors".equals(authorFilter) || 
                    commit.getAuthor().equals(authorFilter);
            
            if (matchMessage && matchAuthor) {
                filteredCommits.add(commit);
            }
        }
        
        updateCommitsTable();
    }
    
    /**
     * 清除所有过滤器
     */
    private void clearFilters() {
        messageSearchField.setText("");
        authorFilterComboBox.setSelectedItem("All Authors");
        filteredCommits.clear();
        filteredCommits.addAll(allCommits);
        updateCommitsTable();
    }
    
    /**
     * 更新提交表格显示
     */
    private void updateCommitsTable() {
        tableModel.setRowCount(0);
        shortIdToFullIdMap.clear();
        
        for (GitInfoExtractor.GitCommitInfo commit : filteredCommits) {
            String fullId = commit.getCommitId();
            shortIdToFullIdMap.put(fullId, fullId); // 使用完整ID作为key和value

            Object[] row = {
                fullId,  // 显示完整的commit ID作为commit code
                dateFormat.format(new Date(commit.getCommitTime())),
                commit.getAuthor(),
                truncateMessage(commit.getMessage())
            };
            tableModel.addRow(row);
        }
        
        filesTextArea.setText("Showing " + filteredCommits.size() + " commits. Select a commit to see changed files.");
    }
    
    /**
     * 更新Author过滤下拉框
     */
    private void updateAuthorFilter() {
        authorFilterComboBox.removeAllItems();
        authorFilterComboBox.addItem("All Authors");
        
        java.util.Set<String> authors = new java.util.TreeSet<>();
        for (GitInfoExtractor.GitCommitInfo commit : allCommits) {
            authors.add(commit.getAuthor());
        }
        
        for (String author : authors) {
            authorFilterComboBox.addItem(author);
        }
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
     * 显示仓库详细信息
     */
    public void displayRepoDetails(File repoDir) {
        this.currentRepoDir = repoDir;
        remoteLabel.setText("Loading...");
        tableModel.setRowCount(0);
        filesTextArea.setText("Select a commit to see changed files...");
        shortIdToFullIdMap.clear();
        allCommits.clear();
        filteredCommits.clear();

        SwingWorker<RepoData, String> worker = new SwingWorker<RepoData, String>() {
            @Override
            protected RepoData doInBackground() throws Exception {
                RepoData data = new RepoData();
                data.repoInfo = GitInfoExtractor.getRepositoryInfo(repoDir);
                data.commits = GitInfoExtractor.getRecentCommits(repoDir, currentDisplaySize);
                return data;
            }

            @Override
            protected void done() {
                try {
                    RepoData data = get();

                    if (data.repoInfo != null) {
                        // 显示 Remote Path
                        List<String> remotes = data.repoInfo.getRemoteUrls();
                        StringBuilder remoteText = new StringBuilder("<html>");
                        if (remotes != null && !remotes.isEmpty()) {
                            for (String remote : remotes) {
                                remoteText.append(remote).append("<br>");
                            }
                        } else {
                            remoteText.append("No remotes configured");
                        }
                        remoteText.append("</html>");
                        remoteLabel.setText(remoteText.toString());
                    }

                    // 存储所有提交记录
                    if (data.commits != null) {
                        allCommits.addAll(data.commits);
                        filteredCommits.addAll(data.commits);
                        
                        // 更新Author过滤器
                        updateAuthorFilter();
                        
                        // 更新表格显示
                        updateCommitsTable();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    remoteLabel.setText("Error loading repository information");
                }
            }
        };

        worker.execute();

        setVisible(true);
    }

    /**
     * 加载并显示指定提交的文件变更
     */
    private void loadCommitFiles(String commitId) {
        if (currentRepoDir == null) {
            return;
        }

        // 现在直接使用完整的commit ID
        String fullCommitId = shortIdToFullIdMap.get(commitId);
        if (fullCommitId == null) {
            filesTextArea.setText("Error: Cannot find commit code " + commitId);
            return;
        }

        // 显示完整的commit ID
        filesTextArea.setText("Loading files for commit code " + commitId + "...");

        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() throws Exception {
                return GitInfoExtractor.getCommitFiles(currentRepoDir, fullCommitId);
            }

            @Override
            protected void done() {
                try {
                    List<String> files = get();
                    if (files.isEmpty()) {
                        filesTextArea.setText("No files changed or this is the initial commit.");
                    } else {
                        StringBuilder sb = new StringBuilder();
                        
                        // 获取远程URL
                        String remoteUrl = getRemoteUrl();
                        
                        // 构建commit超链接
                        String commitUrl = buildCommitUrl(remoteUrl, commitId);
                        currentCommitUrl = commitUrl; // 保存当前URL
                        
                        sb.append("Commit Code: ").append(commitId).append("\n");
                        sb.append("Repository URL: ").append(remoteUrl).append("\n");
                        if (commitUrl != null) {
                            sb.append("Commit Link: ").append(commitUrl).append("\n");
                            sb.append("(Double-click anywhere in this area to open the commit link)\n");
                        }
                        sb.append("Changed files (").append(files.size()).append("):\n");
                        sb.append("========================================\n\n");
                        for (String file : files) {
                            sb.append(file).append("\n");
                        }
                        filesTextArea.setText(sb.toString());
                    }
                } catch (Exception e) {
                    filesTextArea.setText("Error loading commit files: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    /**
     * 重新加载提交记录
     */
    private void reloadCommits() {
        if (currentRepoDir == null) {
            return;
        }

        tableModel.setRowCount(0);
        filesTextArea.setText("Loading commits...");
        shortIdToFullIdMap.clear();
        allCommits.clear();
        filteredCommits.clear();

        SwingWorker<List<GitInfoExtractor.GitCommitInfo>, Void> worker = new SwingWorker<List<GitInfoExtractor.GitCommitInfo>, Void>() {
            @Override
            protected List<GitInfoExtractor.GitCommitInfo> doInBackground() throws Exception {
                return GitInfoExtractor.getRecentCommits(currentRepoDir, currentDisplaySize);
            }

            @Override
            protected void done() {
                try {
                    List<GitInfoExtractor.GitCommitInfo> commits = get();
                    if (commits != null) {
                        allCommits.addAll(commits);
                        filteredCommits.addAll(commits);
                        
                        // 更新Author过滤器
                        updateAuthorFilter();
                        
                        // 清除当前过滤器
                        clearFilters();
                    }
                } catch (Exception e) {
                    filesTextArea.setText("Error loading commits: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        message = message.replaceAll("\\r\\n|\\r|\\n", " ");
        if (message.length() > 80) {
            return message.substring(0, 77) + "...";
        }
        return message;
    }

    private static class NonEditableTableModel extends DefaultTableModel {
        public NonEditableTableModel(Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    /**
     * 构建commit的超链接URL
     */
    private String buildCommitUrl(String remoteUrl, String commitId) {
        if (remoteUrl == null || remoteUrl.equals("No remote URL configured") || remoteUrl.startsWith("Error")) {
            return null;
        }
        
        try {
            // 移除.git后缀
            String baseUrl = remoteUrl;
            if (baseUrl.endsWith(".git")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 4);
            }
            
            // 构建commit URL
            // 对于GitHub: /commit/
            // 对于GitLab: /-/commit/
            // 先尝试GitLab格式，因为题目要求使用 /-/commit/
            return baseUrl + "/-/commit/" + commitId;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 打开URL在默认浏览器中
     */
    private void openUrlInBrowser(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(url));
                }
            }
        } catch (Exception e) {
            // 如果无法打开浏览器，显示错误信息
            JOptionPane.showMessageDialog(this, 
                "Cannot open browser. Please copy the URL manually:\n" + url, 
                "Open URL", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * 获取Git仓库的远程URL
     */
    private String getRemoteUrl() {
        if (currentRepoDir == null) {
            return "Unknown";
        }
        
        try {
            org.eclipse.jgit.storage.file.FileRepositoryBuilder builder = new org.eclipse.jgit.storage.file.FileRepositoryBuilder();
            org.eclipse.jgit.lib.Repository repository = builder
                    .setGitDir(new java.io.File(currentRepoDir, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();
            
            String url = repository.getConfig().getString("remote", "origin", "url");
            repository.close();
            
            return url != null ? url : "No remote URL configured";
        } catch (Exception e) {
            return "Error reading remote URL: " + e.getMessage();
        }
    }

    private static class RepoData {
        GitInfoExtractor.GitRepositoryInfo repoInfo;
        List<GitInfoExtractor.GitCommitInfo> commits;
    }
}
