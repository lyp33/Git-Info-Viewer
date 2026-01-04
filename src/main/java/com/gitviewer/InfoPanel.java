package com.gitviewer;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.*;
import java.util.EventObject;

/**
 * 右侧信息显示面板
 * 显示选中目录的子目录信息，包括Git仓库信息
 */
public class InfoPanel extends JPanel {

    private SimpleDateFormat dateFormat;
    private SimpleDateFormat logDateFormat;
    private JPanel mainPanel;
    private JScrollPane scrollPane;
    private File currentDirectory;
    private JComboBox<String> branchComboBox;
    private JButton applyButton;
    private JButton fetchButton;
    private JTextField batchBranchTextField;
    private JButton applyAllButton;
    private JTextField messageSearchTextField;
    private JButton searchMessageButton;
    private JTextField startDateField;
    private JTextField endDateField;
    private JTable gitReposTable;
    private DefaultTableModel tableModel;
    private Map<String, java.util.List<String>> repoBranchesMap;
    private JTextArea logTextArea;
    private JPanel logPanel;

    // 现代化配色方案
    private static final Color PRIMARY_COLOR = new Color(66, 133, 244);      // Google Blue
    private static final Color SUCCESS_COLOR = new Color(144, 238, 144);      // Light Green
    private static final Color SUCCESS_BORDER_COLOR = new Color(100, 200, 100); // Light Green Border
    private static final Color SUCCESS_TEXT_COLOR = new Color(20, 100, 20);    // Dark Green Text
    private static final Color HEADER_BG_COLOR = new Color(248, 249, 250);   // Light Gray
    private static final Color ODD_ROW_COLOR = new Color(255, 255, 255);     // White
    private static final Color EVEN_ROW_COLOR = new Color(245, 248, 250);    // Light Blue Gray
    private static final Color BORDER_COLOR = new Color(227, 233, 239);      // Subtle Border
    private static final Color PANEL_BG_COLOR = new Color(255, 255, 255);    // White
    private static final Color ACCENT_COLOR = new Color(103, 58, 183);       // Purple

    // 提交搜索结果数据类
    private static class CommitSearchResult {
        String projectName;
        String branch;
        String commitId;
        String message;
        String author;
        long commitTime;
        String changedFiles;
    }

    // Non-editable table model
    private static class NonEditableTableModel extends DefaultTableModel {
        public NonEditableTableModel(Object[][] data, Object[] columnNames) {
            super(data, columnNames);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            if (column == 0) { // Select列
                // 只有Git仓库行的checkbox才可编辑
                if (row >= 0 && row < getRowCount()) {
                    String type = (String) getValueAt(row, 2); // Type列是第2列
                    return "[Git Repo]".equals(type);
                }
                return false;
            }
            return column == 3 || column == 7;  // Branch, Action列可编辑
        }
    }

    public InfoPanel() {
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        this.logDateFormat = new SimpleDateFormat("HH:mm:ss");
        this.repoBranchesMap = new HashMap<>();
        initializeUI();
        loadFontSettings();

        // 注册字体变化监听器
        AppSettings.getInstance().addFontChangeListener((leftFont, rightFont) -> {
            updateFont(rightFont);
        });
    }

    private void loadFontSettings() {
        Font font = AppSettings.getInstance().getRightPanelFont();
        updateFont(font);
    }

    private void updateFont(Font font) {
        // 更新主面板和所有组件的字体
        if (mainPanel != null) {
            mainPanel.setFont(font);
            // 递归更新所有子组件的字体
            updateComponentFonts(mainPanel, font);
        }
    }

    private void updateComponentFonts(Container container, Font font) {
        for (Component comp : container.getComponents()) {
            comp.setFont(font);
            if (comp instanceof Container) {
                updateComponentFonts((Container) comp, font);
            }
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(PANEL_BG_COLOR);

        // 创建顶部标签 - 更现代化
        JLabel titleLabel = new JLabel("Directory Information", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(51, 51, 51));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        add(titleLabel, BorderLayout.NORTH);

        // 创建主面板用于放置内容
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(PANEL_BG_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        scrollPane = new JScrollPane(mainPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(null);
        scrollPane.setBackground(PANEL_BG_COLOR);
        add(scrollPane, BorderLayout.CENTER);

        // 创建底部日志面板
        logPanel = createLogPanel();
        add(logPanel, BorderLayout.SOUTH);
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 150));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        panel.setBorder(createStyledBorder("Operation Log"));

        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logTextArea.setBackground(new Color(250, 250, 250));
        logTextArea.setForeground(new Color(60, 64, 67));

        JScrollPane logScroll = new JScrollPane(logTextArea);
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(logScroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBatchSwitchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // 左侧：标题和描述
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Batch Switch All Git Repos",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(95, 99, 104)
        ));

        // 右侧：输入框和按钮
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JLabel branchLabel = new JLabel("Target Branch:");
        branchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rightPanel.add(branchLabel);

        batchBranchTextField = new JTextField();
        batchBranchTextField.setPreferredSize(new Dimension(200, 28));
        batchBranchTextField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        batchBranchTextField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
        batchBranchTextField.setToolTipText("Enter the branch name to switch all Git repositories to");
        rightPanel.add(batchBranchTextField);

        applyAllButton = new JButton("Switch All");
        applyAllButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        applyAllButton.setBackground(ACCENT_COLOR);
        applyAllButton.setForeground(Color.WHITE);
        applyAllButton.setFocusPainted(false);
        applyAllButton.setBorderPainted(false);
        applyAllButton.setOpaque(true);
        applyAllButton.setPreferredSize(new Dimension(100, 32));
        applyAllButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyAllButton.addActionListener(this::onApplyAll);
        rightPanel.add(applyAllButton);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }
    
    private JPanel createMessageSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // 左侧：标题和描述
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Search Commit Messages in Selected Repos",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(95, 99, 104)
        ));

        // 右侧：所有输入框在同一行
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        // 关键词输入框
        JLabel messageLabel = new JLabel("Keywords:");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rightPanel.add(messageLabel);

        messageSearchTextField = new JTextField();
        messageSearchTextField.setPreferredSize(new Dimension(150, 28));
        messageSearchTextField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        messageSearchTextField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
        messageSearchTextField.setToolTipText("Enter keywords to search (optional - leave empty to search all)");
        rightPanel.add(messageSearchTextField);

        // 准备当天日期字符串
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String todayDate = sdf.format(new Date());

        // 开始日期
        JLabel startLabel = new JLabel("Start Date:");
        startLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rightPanel.add(startLabel);

        startDateField = new JTextField();
        startDateField.setPreferredSize(new Dimension(120, 28));
        startDateField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        startDateField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
        startDateField.setText(todayDate); // 默认显示当天日期
        startDateField.setToolTipText("Format: yyyy-MM-dd (optional, default: today)");
        rightPanel.add(startDateField);

        // 结束日期
        JLabel endLabel = new JLabel("End Date:");
        endLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        rightPanel.add(endLabel);

        endDateField = new JTextField();
        endDateField.setPreferredSize(new Dimension(120, 28));
        endDateField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        endDateField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
        endDateField.setText(todayDate); // 默认显示当天日期
        endDateField.setToolTipText("Format: yyyy-MM-dd (optional, default: today)");
        rightPanel.add(endDateField);

        // 搜索按钮
        searchMessageButton = new JButton("Global Search");
        searchMessageButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchMessageButton.setBackground(PRIMARY_COLOR);
        searchMessageButton.setForeground(Color.WHITE);
        searchMessageButton.setFocusPainted(false);
        searchMessageButton.setBorderPainted(false);
        searchMessageButton.setOpaque(true);
        searchMessageButton.setPreferredSize(new Dimension(120, 32));
        searchMessageButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchMessageButton.addActionListener(this::onSearchMessages);
        rightPanel.add(searchMessageButton);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createBatchCherryPickPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // 左侧：标题和描述
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Batch Cherry-Pick Commits",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(95, 99, 104)
        ));

        // 右侧：按钮
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        JButton cherryPickButton = new JButton("Batch Cherry Pick");
        cherryPickButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cherryPickButton.setBackground(new Color(220, 53, 69)); // Red color for cherry-pick
        cherryPickButton.setForeground(Color.WHITE);
        cherryPickButton.setFocusPainted(false);
        cherryPickButton.setBorderPainted(false);
        cherryPickButton.setOpaque(true);
        cherryPickButton.setPreferredSize(new Dimension(140, 32));
        cherryPickButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cherryPickButton.addActionListener(this::onBatchCherryPick);
        rightPanel.add(cherryPickButton);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private void appendLog(String message) {
        if (logTextArea != null) {
            String timestamp = logDateFormat.format(new Date());
            logTextArea.append("[" + timestamp + "] " + message + "\n");
            logTextArea.setCaretPosition(logTextArea.getDocument().getLength());
        }
    }

    private void clearLog() {
        if (logTextArea != null) {
            logTextArea.setText("");
        }
    }

    public void displayInfo(File directory) {
        this.currentDirectory = directory;
        mainPanel.removeAll();

        addDirectoryTitle(directory);

        if (GitInfoExtractor.isGitRepository(directory)) {
            addGitRepositoryPanel(directory);
        } else {
            addNotGitRepoPanel();
        }

        addSubdirectoriesTable(directory);

        mainPanel.revalidate();
        mainPanel.repaint();

        // 重新应用设置的字体（因为新建的组件使用了默认字体）
        Font rightFont = AppSettings.getInstance().getRightPanelFont();
        updateFont(rightFont);
    }

    private void addDirectoryTitle(File directory) {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(PANEL_BG_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
        titlePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel pathLabel = new JLabel("<html><div style='padding: 8px 12px; background: #E8F0FE; border-radius: 6px;'>" +
                "<span style='color: #1967D2; font-weight: bold;'>Directory:</span> " +
                "<span style='color: #3C4043;'>" + directory.getAbsolutePath() + "</span></div></html>");
        pathLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        titlePanel.add(pathLabel, BorderLayout.CENTER);

        mainPanel.add(titlePanel);
    }

    private void addGitRepositoryPanel(File directory) {
        GitInfoExtractor.GitRepositoryInfo repoInfo = GitInfoExtractor.getRepositoryInfo(directory);

        if (repoInfo == null) {
            JPanel errorPanel = createStyledInfoPanel("Error", "Unable to read Git repository information",
                    new Color(255, 243, 243), new Color(211, 47, 47));
            mainPanel.add(errorPanel);
            return;
        }

        JPanel gitPanel = new JPanel();
        gitPanel.setLayout(new BoxLayout(gitPanel, BoxLayout.Y_AXIS));
        gitPanel.setBackground(PANEL_BG_COLOR);
        gitPanel.setBorder(createStyledBorder("Git Repository Information"));
        gitPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 500));

        gitPanel.add(createStyledKeyValuePanel("Remote URLs:", formatRemoteUrls(repoInfo.getRemoteUrls())));
        gitPanel.add(createStyledKeyValuePanel("Current Branch:", repoInfo.getCurrentBranch()));

        if (repoInfo.getBranches() != null && !repoInfo.getBranches().isEmpty()) {
            gitPanel.add(createBranchSelectionPanel(repoInfo.getBranches()));
        }

        if (repoInfo.getLastCommit() != null) {
            gitPanel.add(createStyledLastCommitPanel(repoInfo.getLastCommit()));
        }

        mainPanel.add(gitPanel);
        mainPanel.add(Box.createVerticalStrut(15));
    }

    private JPanel createBranchSelectionPanel(java.util.List<String> branches) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        panel.setBackground(PANEL_BG_COLOR);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));

        JLabel label = new JLabel("Switch Branch:");
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(new Color(51, 51, 51));
        panel.add(label);

        branchComboBox = new JComboBox<>();
        branchComboBox.setEditable(true); // 设置为可编辑
        branchComboBox.setPreferredSize(new Dimension(280, 32));
        branchComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        branchComboBox.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));

        for (String branch : branches) {
            String displayName = branch.replace("refs/heads/", "");
            branchComboBox.addItem(displayName);
        }
        panel.add(branchComboBox);

        applyButton = createStyledButton("Switch Branch", PRIMARY_COLOR);
        applyButton.addActionListener(this::onApplyBranch);
        panel.add(applyButton);

        fetchButton = createStyledButton("Fetch & Pull", SUCCESS_COLOR);
        fetchButton.addActionListener(this::onFetchPull);
        panel.add(fetchButton);

        return panel;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(130, 32));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Add hover effect
        button.getModel().addChangeListener(e -> {
            ButtonModel model = (ButtonModel) e.getSource();
            if (model.isRollover()) {
                button.setBackground(color.brighter());
            } else {
                button.setBackground(color);
            }
        });

        return button;
    }

    private JPanel createStyledKeyValuePanel(String key, String value) {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(PANEL_BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        JLabel keyLabel = new JLabel(key);
        keyLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        keyLabel.setForeground(new Color(95, 99, 104));
        keyLabel.setPreferredSize(new Dimension(130, 20));
        panel.add(keyLabel, BorderLayout.WEST);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        valueLabel.setForeground(new Color(60, 64, 67));
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStyledLastCommitPanel(GitInfoExtractor.GitCommitInfo lastCommit) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(250, 251, 252));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        JLabel titleLabel = new JLabel("Last Commit:");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(new Color(95, 99, 104));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(8));

        String[] labels = {"Commit Code:", "Author:", "Date:", "Message:"};
        String[] values = {
            lastCommit.getCommitId(),
            lastCommit.getAuthor() + " <" + lastCommit.getEmail() + ">",
            formatDate(lastCommit.getCommitTime()),
            truncateMessage(lastCommit.getMessage())
        };

        for (int i = 0; i < labels.length; i++) {
            JPanel line = new JPanel(new BorderLayout(12, 3));
            line.setBackground(new Color(250, 251, 252));
            line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            lbl.setForeground(new Color(95, 99, 104));
            lbl.setPreferredSize(new Dimension(90, 16));

            JLabel val = new JLabel(values[i]);
            val.setFont(new Font("Consolas", Font.PLAIN, 10));
            val.setForeground(new Color(60, 64, 67));

            line.add(lbl, BorderLayout.WEST);
            line.add(val, BorderLayout.CENTER);
            panel.add(line);
        }

        return panel;
    }

    private void addSubdirectoriesTable(File directory) {
        File[] children = directory.listFiles();
        if (children == null || children.length == 0) {
            return;
        }

        repoBranchesMap.clear();

        String[] columnNames = {"Select", "Name", "Type", "Branch", "Remote", "Last Modified", "Author", "Action"};
        tableModel = new NonEditableTableModel(new Object[][]{}, columnNames);

        for (File child : children) {
            if (child.isDirectory() && !child.getName().startsWith(".")) {
                Vector<Object> row = new Vector<>();

                boolean isGitRepo = GitInfoExtractor.isGitRepository(child);
                
                // 只有Git仓库才有checkbox，普通目录为null
                if (isGitRepo) {
                    row.add(Boolean.FALSE); // 默认不选中Git仓库
                } else {
                    row.add(null); // 普通目录不显示checkbox
                }
                
                row.add(child.getName());

                if (isGitRepo) {
                    GitInfoExtractor.GitRepositoryInfo repoInfo = GitInfoExtractor.getRepositoryInfo(child);
                    row.add("[Git Repo]");

                    if (repoInfo != null) {
                        String currentBranch = repoInfo.getCurrentBranch();
                        row.add(currentBranch);

                        java.util.List<String> allBranches = GitOperations.getRemoteBranches(child);
                        java.util.List<String> displayBranches = new ArrayList<>();
                        for (String branch : allBranches) {
                            String displayName = branch.replace("origin/", "");
                            if (!displayBranches.contains(displayName)) {
                                displayBranches.add(displayName);
                            }
                        }
                        repoBranchesMap.put(child.getAbsolutePath(), displayBranches);

                        if (repoInfo.getRemoteUrls() != null && !repoInfo.getRemoteUrls().isEmpty()) {
                            row.add(extractRemoteName(repoInfo.getRemoteUrls().get(0)));
                        } else {
                            row.add("-");
                        }
                        if (repoInfo.getLastCommit() != null) {
                            row.add(formatDate(repoInfo.getLastCommit().getCommitTime()));
                            row.add(repoInfo.getLastCommit().getAuthor());
                        } else {
                            row.add("-");
                            row.add("-");
                        }
                    } else {
                        row.add("-");
                        row.add("-");
                        row.add("-");
                        row.add("-");
                    }
                } else {
                    row.add("Directory");
                    row.add("-");
                    row.add("-");
                    row.add("-");
                    row.add("-");
                }

                row.add("Switch");
                tableModel.addRow(row);
            }
        }

        if (tableModel.getRowCount() > 0) {
            JPanel tablePanel = new JPanel(new BorderLayout());
            tablePanel.setBackground(PANEL_BG_COLOR);
            tablePanel.setBorder(createStyledBorder("Subdirectories"));

            gitReposTable = new JTable(tableModel) {
                @Override
                public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                    Component c = super.prepareRenderer(renderer, row, column);
                    // 始终使用斑马纹颜色，忽略选中状态
                    c.setBackground(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
                    // 特殊处理 Action 列的按钮
                    if (column == 7 && c instanceof JButton) {
                        // 按钮保持自己的背景色
                    }
                    return c;
                }
            };

            gitReposTable.setAutoCreateRowSorter(true);
            gitReposTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            gitReposTable.setRowHeight(32);
            gitReposTable.setIntercellSpacing(new Dimension(0, 0));
            gitReposTable.setShowGrid(false);
            // 禁用选择高亮
            gitReposTable.setSelectionForeground(new Color(60, 64, 67));

            // 表头样式
            JTableHeader header = gitReposTable.getTableHeader();
            header.setBackground(HEADER_BG_COLOR);
            header.setForeground(new Color(95, 99, 104));
            header.setFont(new Font("Segoe UI", Font.BOLD, 12));
            header.setReorderingAllowed(false);
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));

            // 设置列宽
            gitReposTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // Select
            gitReposTable.getColumnModel().getColumn(1).setPreferredWidth(180);  // Name
            gitReposTable.getColumnModel().getColumn(2).setPreferredWidth(90);   // Type
            gitReposTable.getColumnModel().getColumn(3).setPreferredWidth(200);  // Branch
            gitReposTable.getColumnModel().getColumn(4).setPreferredWidth(120);  // Remote
            gitReposTable.getColumn("Last Modified").setPreferredWidth(150);
            gitReposTable.getColumn("Author").setPreferredWidth(120);
            gitReposTable.getColumn("Action").setPreferredWidth(80);

            // 为 Select 列添加复选框渲染器和编辑器
            gitReposTable.getColumnModel().getColumn(0).setCellRenderer(new CheckBoxCellRenderer());
            gitReposTable.getColumnModel().getColumn(0).setCellEditor(new CheckBoxCellEditor());

            // Type 列渲染器 - 居中对齐，Git Repo显示红色
            DefaultTableCellRenderer typeRenderer = new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    setHorizontalAlignment(SwingConstants.CENTER);
                    
                    // 如果是Git仓库，使用红色字体
                    if ("[Git Repo]".equals(value)) {
                        setForeground(Color.RED);
                    } else {
                        // 普通目录使用默认颜色
                        setForeground(new Color(60, 64, 67));
                    }
                    
                    return c;
                }
            };
            gitReposTable.getColumnModel().getColumn(2).setCellRenderer(typeRenderer);

            // 为 Branch 列设置下拉框编辑器和渲染器
            gitReposTable.getColumnModel().getColumn(3).setCellRenderer(new BranchCellRenderer());
            gitReposTable.getColumnModel().getColumn(3).setCellEditor(new BranchCellEditor());

            // 为 Action 列设置按钮渲染器和编辑器
            gitReposTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonCellRenderer());
            gitReposTable.getColumnModel().getColumn(7).setCellEditor(new ButtonCellEditor());

            JScrollPane tableScroll = new JScrollPane(gitReposTable);
            tableScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
            tableScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, Math.min(350, tableModel.getRowCount() * 32 + 40)));

            tablePanel.add(tableScroll, BorderLayout.CENTER);
            mainPanel.add(tablePanel);

            // 检查是否有Git仓库
            boolean hasGitRepos = hasGitRepositories();
            
            if (hasGitRepos) {
                // 添加批量Switch面板
                JPanel batchSwitchPanel = createBatchSwitchPanel();
                mainPanel.add(batchSwitchPanel);
                
                // 添加Message查询面板
                JPanel messageSearchPanel = createMessageSearchPanel();
                mainPanel.add(messageSearchPanel);

                // 添加Batch Cherry-Pick面板
                JPanel batchCherryPickPanel = createBatchCherryPickPanel();
                mainPanel.add(batchCherryPickPanel);
            }

            // 添加鼠标监听器，监听双击事件
            gitReposTable.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2) { // 双击
                        int selectedRow = gitReposTable.getSelectedRow();
                        if (selectedRow != -1) {
                            String dirPath = getDirectoryPathForRow(selectedRow);
                            String type = (String) gitReposTable.getValueAt(selectedRow, 2);  // Type列现在是第2列
                            if (dirPath != null && "[Git Repo]".equals(type)) {
                                showRepoDetailsInLog(dirPath);
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * 在对话框中显示 Git 项目详细信息
     */
    private void showRepoDetailsInLog(String dirPath) {
        File repoDir = new File(dirPath);

        // 在 log 中显示简单提示
        appendLog("Loading repository details for: " + repoDir.getName());
        appendLog("Please wait...");

        // 使用对话框显示详细信息
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // 在后台线程中准备数据
                GitInfoExtractor.getRepositoryInfo(repoDir);
                return null;
            }

            @Override
            protected void done() {
                // 显示对话框
                RepoDetailsDialog dialog = new RepoDetailsDialog(null);
                dialog.displayRepoDetails(repoDir);
                appendLog("✓ Repository details loaded.");
            }
        };

        worker.execute();
    }

    /**
     * 检查当前表格中是否有Git仓库
     */
    private boolean hasGitRepositories() {
        if (gitReposTable == null || gitReposTable.getModel() == null) {
            return false;
        }
        
        DefaultTableModel model = (DefaultTableModel) gitReposTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String type = (String) model.getValueAt(i, 2); // Type列是第2列
            if ("[Git Repo]".equals(type)) {
                return true;
            }
        }
        return false;
    }

    private class BranchCellRenderer extends JComboBox<String> implements TableCellRenderer {
        public BranchCellRenderer() {
            setBorder(BorderFactory.createEmptyBorder());
            setFont(new Font("Segoe UI", Font.PLAIN, 11));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            removeAllItems();

            String dirPath = getDirectoryPathForRow(row);
            if (dirPath != null) {
                java.util.List<String> branches = repoBranchesMap.get(dirPath);
                if (branches != null) {
                    for (String branch : branches) {
                        addItem(branch);
                    }
                }
            }

            if (value != null) {
                setSelectedItem(value);
            }

            // 始终使用斑马纹颜色，忽略选中状态
            setBackground(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
            setForeground(new Color(60, 64, 67));

            return this;
        }
    }

    private class BranchCellEditor extends AbstractCellEditor implements TableCellEditor {
        private JComboBox<String> comboBox;
        private java.util.List<String> allBranches;
        private boolean isFiltering = false;
        private javax.swing.event.DocumentListener documentListener;

        public BranchCellEditor() {
            comboBox = new JComboBox<>();
            comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            comboBox.setEditable(true); // 设置为可编辑
            
            // 添加ActionListener，但要避免在过滤时触发
            comboBox.addActionListener(e -> {
                if (!isFiltering) {
                    fireEditingStopped();
                }
            });
            
            // 创建DocumentListener
            documentListener = new javax.swing.event.DocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) { 
                    if (!isFiltering) {
                        SwingUtilities.invokeLater(() -> filterBranches()); 
                    }
                }
                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) { 
                    if (!isFiltering) {
                        SwingUtilities.invokeLater(() -> filterBranches()); 
                    }
                }
                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) { 
                    if (!isFiltering) {
                        SwingUtilities.invokeLater(() -> filterBranches()); 
                    }
                }
            };
        }
        
        private void filterBranches() {
            if (allBranches == null || allBranches.isEmpty() || isFiltering) return;
            
            isFiltering = true; // 设置过滤标志
            
            try {
                JTextField textField = (JTextField) comboBox.getEditor().getEditorComponent();
                String filterText = textField.getText().toLowerCase();
                
                // 清空并重新填充
                comboBox.removeAllItems();
                
                boolean hasMatches = false;
                for (String branch : allBranches) {
                    if (filterText.isEmpty() || branch.toLowerCase().contains(filterText)) {
                        comboBox.addItem(branch);
                        hasMatches = true;
                    }
                }
                
                // 如果没有匹配项，显示所有分支
                if (!hasMatches && !filterText.isEmpty()) {
                    for (String branch : allBranches) {
                        comboBox.addItem(branch);
                    }
                }
                
                // 显示下拉列表
                if (comboBox.getItemCount() > 0 && !filterText.isEmpty()) {
                    comboBox.showPopup();
                }
                
            } finally {
                isFiltering = false; // 清除过滤标志
            }
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            
            String dirPath = getDirectoryPathForRow(row);
            if (dirPath != null) {
                allBranches = repoBranchesMap.get(dirPath);
                
                isFiltering = true;
                try {
                    comboBox.removeAllItems();
                    
                    if (allBranches != null) {
                        for (String branch : allBranches) {
                            comboBox.addItem(branch);
                        }
                    }
                } finally {
                    isFiltering = false;
                }
            }

            // 添加DocumentListener
            JTextField textField = (JTextField) comboBox.getEditor().getEditorComponent();
            textField.getDocument().addDocumentListener(documentListener);

            if (value != null) {
                comboBox.setSelectedItem(value);
                // 设置编辑器文本
                textField.setText(value.toString());
            }

            return comboBox;
        }

        @Override
        public Object getCellEditorValue() {
            // 移除DocumentListener
            JTextField textField = (JTextField) comboBox.getEditor().getEditorComponent();
            textField.getDocument().removeDocumentListener(documentListener);
            
            // 返回编辑器中的文本，而不是选中的项目
            return textField.getText();
        }
    }

    /**
     * 凸起按钮样式的渲染器
     */
    private class ButtonCellRenderer extends JButton implements TableCellRenderer {
        public ButtonCellRenderer() {
            setText("Switch");
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setFocusPainted(false);
            setBorderPainted(true);
            setOpaque(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            String type = (String) table.getValueAt(row, 2);  // Type is in column 2
            boolean isGitRepo = "[Git Repo]".equals(type);

            if (isGitRepo) {
                setBackground(SUCCESS_COLOR);
                setForeground(SUCCESS_TEXT_COLOR);
                setEnabled(true);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(SUCCESS_BORDER_COLOR, 1),
                        BorderFactory.createEmptyBorder(6, 16, 6, 16)
                ));
            } else {
                setBackground(new Color(240, 240, 240));
                setForeground(new Color(150, 150, 150));
                setEnabled(false);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                        BorderFactory.createEmptyBorder(6, 16, 6, 16)
                ));
            }

            return this;
        }
    }

    /**
     * 扁平化复选框单元格渲染器
     */
    private class CheckBoxCellRenderer extends JCheckBox implements TableCellRenderer {
        public CheckBoxCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(true);
            // 扁平化样式
            setBackground(Color.WHITE);
            setForeground(PRIMARY_COLOR);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            // 检查是否是Git仓库
            String type = (String) table.getValueAt(row, 2);
            boolean isGitRepo = "[Git Repo]".equals(type);
            
            if (isGitRepo) {
                setVisible(true);
                setEnabled(true);
                boolean checked = value != null && (Boolean) value;
                setSelected(checked);
                setBackground(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
                
                // 扁平化样式设置 - 根据实际状态设置图标
                setIcon(createCheckIcon(checked));
                setSelectedIcon(createCheckIcon(checked));
                setText("");
            } else {
                // 非Git仓库不显示checkbox，完全禁用
                setVisible(false);
                setEnabled(false);
                setSelected(false);
                setBackground(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
                setIcon(null);
                setSelectedIcon(null);
                setText("");
            }
            
            return this;
        }
        
        /**
         * 创建扁平化的勾选图标
         */
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
                public int getIconWidth() { return 16; }
                
                @Override
                public int getIconHeight() { return 16; }
            };
        }
    }
    
    /**
     * 扁平化复选框单元格编辑器
     */
    private class CheckBoxCellEditor extends AbstractCellEditor implements TableCellEditor {
        private JCheckBox checkBox;
        
        public CheckBoxCellEditor() {
            checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setBorderPainted(false);
            checkBox.setFocusPainted(false);
            checkBox.setOpaque(true);
            
            // 添加状态变化监听器，动态更新图标
            checkBox.addActionListener(e -> {
                updateCheckBoxIcon();
                // 立即停止编辑以确保值被保存
                SwingUtilities.invokeLater(() -> fireEditingStopped());
            });
        }
        
        @Override
        public boolean isCellEditable(EventObject e) {
            // 额外的检查：确保只有Git仓库行可以编辑
            if (e instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) e;
                JTable table = (JTable) me.getSource();
                int row = table.rowAtPoint(me.getPoint());
                if (row >= 0 && row < table.getRowCount()) {
                    String type = (String) table.getValueAt(row, 2);
                    return "[Git Repo]".equals(type);
                }
            }
            return super.isCellEditable(e);
        }
        
        /**
         * 更新checkbox的图标
         */
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
            
            // 检查是否是Git仓库
            String type = (String) table.getValueAt(row, 2);
            boolean isGitRepo = "[Git Repo]".equals(type);
            
            if (isGitRepo) {
                checkBox.setVisible(true);
                checkBox.setEnabled(true);
                checkBox.setSelected(value != null && (Boolean) value);
                checkBox.setBackground(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
                
                // 设置扁平化图标
                updateCheckBoxIcon();
                checkBox.setText("");
            } else {
                // 非Git仓库，返回一个不可编辑的组件
                checkBox.setVisible(false);
                checkBox.setEnabled(false);
                checkBox.setSelected(false);
            }
            
            return checkBox;
        }
        
        @Override
        public Object getCellEditorValue() {
            return checkBox.isSelected();
        }
        
        /**
         * 创建扁平化的勾选图标（复用CheckBoxCellRenderer的方法）
         */
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
                public int getIconWidth() { return 16; }
                
                @Override
                public int getIconHeight() { return 16; }
            };
        }
    }

    private class ButtonCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private JButton button;
        private int currentRow;

        public ButtonCellEditor() {
            button = new JButton("Switch");
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.setFocusPainted(false);
            button.setBorderPainted(true);
            button.setOpaque(true);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.addActionListener(this);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            String type = (String) table.getValueAt(row, 2);  // Type is in column 2
            boolean isGitRepo = "[Git Repo]".equals(type);

            if (isGitRepo) {
                button.setBackground(SUCCESS_COLOR);
                button.setForeground(SUCCESS_TEXT_COLOR);
                button.setEnabled(true);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(SUCCESS_BORDER_COLOR, 1),
                        BorderFactory.createEmptyBorder(6, 16, 6, 16)
                ));
            } else {
                button.setBackground(new Color(240, 240, 240));
                button.setForeground(new Color(150, 150, 150));
                button.setEnabled(false);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
                        BorderFactory.createEmptyBorder(6, 16, 6, 16)
                ));
            }

            currentRow = row;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return "Switch";
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // Check if this is a Git repository before processing
            String type = (String) gitReposTable.getValueAt(currentRow, 2);  // Type is in column 2
            boolean isGitRepo = "[Git Repo]".equals(type);
            
            if (!isGitRepo) {
                // For non-git projects, do nothing
                fireEditingStopped();
                return;
            }
            
            String dirPath = getDirectoryPathForRow(currentRow);
            if (dirPath != null) {
                Object selectedBranchObj = gitReposTable.getValueAt(currentRow, 3);  // Branch列现在是第3列
                if (selectedBranchObj != null) {
                    String selectedBranch = selectedBranchObj.toString();
                    switchBranchForRepo(dirPath, selectedBranch);
                }
            }
            fireEditingStopped();
        }
    }

    private String getDirectoryPathForRow(int row) {
        int modelRow = gitReposTable.convertRowIndexToModel(row);
        String dirName = (String) tableModel.getValueAt(modelRow, 1);  // Name列现在是第1列

        if (currentDirectory != null && dirName != null) {
            File[] children = currentDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.getName().equals(dirName)) {
                        return child.getAbsolutePath();
                    }
                }
            }
        }
        return null;
    }

    private void switchBranchForRepo(String dirPath, String branch) {
        File repoDir = new File(dirPath);
        clearLog(); // 清空之前的日志

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Switching branch for: " + repoDir.getName());
                publish("New branch: " + branch);
                boolean success = GitOperations.switchBranch(repoDir, branch);
                if (success) {
                    publish("✓ Branch switched successfully!");
                    publish("Pulling latest changes...");
                    boolean pullSuccess = GitOperations.pull(repoDir);
                    if (pullSuccess) {
                        publish("✓ Pull completed!");
                    } else {
                        publish("⚠ Pull completed with warnings.");
                    }
                } else {
                    publish("✗ Failed to switch branch.");
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                // 实时输出日志到 UI
                for (String message : chunks) {
                    appendLog(message);
                }
            }

            @Override
            protected void done() {
                if (currentDirectory != null) {
                    // 保存当前checkbox状态
                    Map<String, Boolean> checkboxStates = saveCheckboxStates();
                    // 重新显示信息
                    displayInfo(currentDirectory);
                    // 恢复checkbox状态
                    restoreCheckboxStates(checkboxStates);
                }
            }
        };

        worker.execute();
    }

    /**
     * 保存当前表格中所有checkbox的状态
     */
    private Map<String, Boolean> saveCheckboxStates() {
        Map<String, Boolean> states = new HashMap<>();
        if (gitReposTable != null && tableModel != null) {
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                String dirName = (String) tableModel.getValueAt(i, 1); // Name列
                Boolean selected = (Boolean) tableModel.getValueAt(i, 0); // Select列
                // 保存所有Git仓库的状态（包括false和true）
                String type = (String) tableModel.getValueAt(i, 2); // Type列
                if ("[Git Repo]".equals(type)) {
                    states.put(dirName, selected != null ? selected : Boolean.FALSE);
                }
            }
        }
        return states;
    }

    /**
     * 恢复表格中checkbox的状态
     */
    private void restoreCheckboxStates(Map<String, Boolean> states) {
        if (gitReposTable != null && tableModel != null && states != null) {
            int rowCount = tableModel.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                String dirName = (String) tableModel.getValueAt(i, 1); // Name列
                String type = (String) tableModel.getValueAt(i, 2); // Type列
                
                // 只恢复Git仓库的checkbox状态
                if ("[Git Repo]".equals(type)) {
                    Boolean savedState = states.get(dirName);
                    if (savedState != null) {
                        tableModel.setValueAt(savedState, i, 0); // Select列
                    }
                    // 如果没有保存的状态，保持默认的Boolean.FALSE
                }
            }
            // 刷新表格显示
            gitReposTable.repaint();
        }
    }

    private void addNotGitRepoPanel() {
        JPanel panel = createStyledInfoPanel("Status", "Not a Git repository",
                new Color(248, 249, 250), new Color(95, 99, 104));
        mainPanel.add(panel);
        mainPanel.add(Box.createVerticalStrut(15));
    }

    private JPanel createStyledInfoPanel(String title, String message, Color bgColor, Color textColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        JLabel label = new JLabel("<html><span style='font-weight:bold; color:" +
                colorToHex(textColor) + ";'>" + title + ":</span> " +
                "<span style='color:" + colorToHex(new Color(60, 64, 67)) + ";'>" + message + "</span></html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        panel.add(label, BorderLayout.CENTER);

        return panel;
    }

    private Border createStyledBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                title,
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13),
                new Color(95, 99, 104)
        );
    }

    private String colorToHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void onApplyBranch(ActionEvent e) {
        if (branchComboBox == null || currentDirectory == null) {
            return;
        }

        String selectedBranch = (String) branchComboBox.getSelectedItem();
        if (selectedBranch == null) {
            return;
        }

        // 使用和"Apply"按钮相同的逻辑
        switchBranchForRepo(currentDirectory.getAbsolutePath(), selectedBranch);
    }

    private void onFetchPull(ActionEvent e) {
        if (currentDirectory == null) {
            return;
        }

        clearLog(); // 清空之前的日志

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Pulling latest changes...");
                boolean success = GitOperations.pull(currentDirectory);
                if (success) {
                    publish("✓ Pull completed successfully!");
                } else {
                    publish("⚠ Pull failed or had conflicts.");
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    appendLog(message);
                }
            }

            @Override
            protected void done() {
                // 保存当前checkbox状态 (onFetchPull)
                Map<String, Boolean> checkboxStates = saveCheckboxStates();
                // 重新显示信息
                displayInfo(currentDirectory);
                // 恢复checkbox状态
                restoreCheckboxStates(checkboxStates);
            }
        };

        worker.execute();
    }
    
    private void onSearchMessages(ActionEvent e) {
        if (currentDirectory == null) {
            return;
        }

        final String searchKeywords = messageSearchTextField.getText().trim();
        String startDateStr = startDateField.getText().trim();
        String endDateStr = endDateField.getText().trim();

        // 解析日期范围
        final Long startTimestamp;
        final Long endTimestamp;

        // 解析开始日期
        if (!startDateStr.isEmpty()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date startDate = dateFormat.parse(startDateStr);
                // 设置为当天 00:00:00
                Calendar cal = Calendar.getInstance();
                cal.setTime(startDate);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                startTimestamp = cal.getTimeInMillis();
            } catch (Exception ex) {
                appendLog("⚠ Invalid start date format. Use yyyy-MM-dd.");
                return;
            }
        } else {
            startTimestamp = null;
        }

        // 解析结束日期
        if (!endDateStr.isEmpty()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date endDate = dateFormat.parse(endDateStr);
                // 设置为当天 00:00:00
                Calendar cal = Calendar.getInstance();
                cal.setTime(endDate);
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                endTimestamp = cal.getTimeInMillis();
            } catch (Exception ex) {
                appendLog("⚠ Invalid end date format. Use yyyy-MM-dd.");
                return;
            }
        } else {
            endTimestamp = null;
        }

        clearLog(); // 清空之前的日志

        SwingWorker<java.util.List<CommitSearchResult>, String> worker = new SwingWorker<java.util.List<CommitSearchResult>, String>() {
            @Override
            protected java.util.List<CommitSearchResult> doInBackground() throws Exception {
                // 判断是否为默认模式（无关键词无日期）
                boolean isDefaultMode = searchKeywords.isEmpty() && startTimestamp == null && endTimestamp == null;
                int limit = isDefaultMode ? 100 : 500;

                if (isDefaultMode) {
                    publish("Retrieving first " + limit + " commits from each repository...");
                } else {
                    publish("Starting commit message search");
                    if (!searchKeywords.isEmpty()) {
                        publish("Keywords: " + searchKeywords);
                    }
                    if (startTimestamp != null) {
                        publish("Start Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(startTimestamp)));
                    }
                    if (endTimestamp != null) {
                        publish("End Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(endTimestamp)));
                    }
                }
                publish("========================================");

                java.util.List<CommitSearchResult> results = new ArrayList<>();

                // 遍历表格中的所有行，只处理勾选的Git仓库
                int rowCount = tableModel.getRowCount();
                int selectedCount = 0;
                int processedCount = 0;

                for (int i = 0; i < rowCount; i++) {
                    // 检查是否勾选
                    Boolean selected = (Boolean) tableModel.getValueAt(i, 0);
                    if (selected == null || !selected) {
                        continue;  // 跳过未勾选的行
                    }

                    String type = (String) tableModel.getValueAt(i, 2);

                    // 只处理Git仓库
                    if ("[Git Repo]".equals(type)) {
                        selectedCount++;
                        String dirPath = getDirectoryPathForRow(i);
                        if (dirPath != null) {
                            File repoDir = new File(dirPath);
                            String repoName = repoDir.getName();
                            String currentBranch = (String) tableModel.getValueAt(i, 3);

                            publish("");
                            publish("[" + selectedCount + "] Searching in: " + repoName);

                            try {
                                // 获取提交记录
                                java.util.List<GitInfoExtractor.GitCommitInfo> commits =
                                    GitInfoExtractor.getRecentCommits(repoDir, limit);

                                int foundCount = 0;
                                for (GitInfoExtractor.GitCommitInfo commit : commits) {
                                    // 检查时间范围
                                    if (startTimestamp != null && commit.getCommitTime() < startTimestamp) {
                                        continue;
                                    }
                                    if (endTimestamp != null && commit.getCommitTime() > endTimestamp) {
                                        continue;
                                    }

                                    // 检查关键词（如果不为空）
                                    if (!searchKeywords.isEmpty() &&
                                        !commit.getMessage().toLowerCase().contains(searchKeywords.toLowerCase())) {
                                        continue;
                                    }

                                    // 获取修改的文件列表
                                    java.util.List<String> changedFiles =
                                        GitInfoExtractor.getCommitFiles(repoDir, commit.getCommitId());

                                    CommitSearchResult result = new CommitSearchResult();
                                    result.projectName = repoName;
                                    result.branch = currentBranch;
                                    result.commitId = commit.getCommitId();
                                    result.message = commit.getMessage();
                                    result.author = commit.getAuthor();
                                    result.commitTime = commit.getCommitTime();
                                    result.changedFiles = String.join(", ", changedFiles);

                                    results.add(result);
                                    foundCount++;
                                }

                                publish("  ✓ Found " + foundCount + " commits");
                                processedCount++;

                            } catch (Exception ex) {
                                publish("  ✗ Error searching in " + repoName + ": " + ex.getMessage());
                            }
                        }
                    }
                }

                publish("");
                publish("========================================");
                if (selectedCount == 0) {
                    publish("⚠ No repositories selected.");
                } else {
                    publish("Search completed!");
                    publish("Processed: " + processedCount + " repositories, Total commits: " + results.size());
                }

                return results;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    appendLog(message);
                }
            }

            @Override
            protected void done() {
                try {
                    java.util.List<CommitSearchResult> results = get();
                    if (!results.isEmpty()) {
                        // 显示搜索结果对话框
                        CommitSearchResultDialog dialog = new CommitSearchResultDialog(null, searchKeywords, results, currentDirectory);
                        dialog.setVisible(true);
                    } else {
                        appendLog("No commits found.");
                    }
                } catch (Exception ex) {
                    appendLog("Error during search: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        };

        worker.execute();
    }

    private void onApplyAll(ActionEvent e) {
        if (currentDirectory == null) {
            return;
        }

        String targetBranch = batchBranchTextField.getText().trim();
        if (targetBranch.isEmpty()) {
            appendLog("⚠ Please enter a branch name.");
            return;
        }

        clearLog(); // 清空之前的日志

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("Starting batch switch to branch: " + targetBranch);
                publish("========================================");

                // 遍历表格中的所有行，只处理勾选的Git仓库
                int rowCount = tableModel.getRowCount();
                int successCount = 0;
                int failCount = 0;
                int selectedCount = 0;

                for (int i = 0; i < rowCount; i++) {
                    // 检查是否勾选
                    Boolean selected = (Boolean) tableModel.getValueAt(i, 0);
                    if (selected == null || !selected) {
                        continue;  // 跳过未勾选的行
                    }

                    String type = (String) tableModel.getValueAt(i, 2);

                    // 只处理Git仓库
                    if ("[Git Repo]".equals(type)) {
                        selectedCount++;
                        String dirPath = getDirectoryPathForRow(i);
                        if (dirPath != null) {
                            File repoDir = new File(dirPath);
                            publish("");
                            publish("[" + selectedCount + "] Processing: " + repoDir.getName());

                            // 切换分支
                            boolean switchSuccess = GitOperations.switchBranch(repoDir, targetBranch);
                            if (switchSuccess) {
                                publish("  ✓ Branch switched to " + targetBranch);

                                // Pull最新代码
                                publish("  Pulling latest changes...");
                                boolean pullSuccess = GitOperations.pull(repoDir);
                                if (pullSuccess) {
                                    publish("  ✓ Pull completed successfully!");
                                    successCount++;
                                } else {
                                    publish("  ⚠ Pull failed or had conflicts.");
                                    successCount++; // switch成功也算
                                }
                            } else {
                                publish("  ✗ Failed to switch branch.");
                                failCount++;
                            }
                        }
                    }
                }

                publish("");
                publish("========================================");
                if (selectedCount == 0) {
                    publish("⚠ No repositories selected.");
                } else {
                    publish("Batch operation completed!");
                    publish("Processed: " + selectedCount + ", Success: " + successCount + ", Failed: " + failCount);
                }

                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    appendLog(message);
                }
            }

            @Override
            protected void done() {
                // 保存当前checkbox状态 (onApplyAll)
                Map<String, Boolean> checkboxStates = saveCheckboxStates();
                // 重新显示信息
                displayInfo(currentDirectory);
                // 恢复checkbox状态
                restoreCheckboxStates(checkboxStates);
            }
        };

        worker.execute();
    }

    private void onBatchCherryPick(ActionEvent e) {
        if (currentDirectory == null) {
            JOptionPane.showMessageDialog(this,
                    "Please select a directory first.",
                    "No Directory Selected",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 打开批量 Cherry-Pick 对话框
        BatchCherryPickDialog dialog = new BatchCherryPickDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                currentDirectory
        );
        dialog.setVisible(true);
    }

    public void clearInfo() {
        mainPanel.removeAll();
        JLabel label = new JLabel("Select a directory from the tree to view its information.",
                SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        label.setForeground(new Color(154, 160, 166));
        mainPanel.add(label);
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    private String formatDate(long timestamp) {
        return dateFormat.format(new Date(timestamp));
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        message = message.replaceAll("\\r\\n|\\r|\\n", " ");
        if (message.length() > 60) {
            return message.substring(0, 57) + "...";
        }
        return message;
    }

    private String formatRemoteUrls(java.util.List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return "No remotes configured";
        }
        return String.join(", ", urls);
    }

    private String extractRemoteName(String remote) {
        if (remote == null) {
            return "-";
        }
        int colonIndex = remote.indexOf(" : ");
        if (colonIndex > 0) {
            return remote.substring(0, colonIndex);
        }
        return remote;
    }
    
    /**
     * 提交搜索结果对话框
     */
    private static class CommitSearchResultDialog extends JDialog {
        private static final Color HEADER_BG_COLOR = new Color(248, 249, 250);
        private static final Color BORDER_COLOR = new Color(227, 233, 239);
        private static final Color ODD_ROW_COLOR = new Color(255, 255, 255);
        private static final Color EVEN_ROW_COLOR = new Color(245, 248, 250);

        private JTable resultsTable;
        private DefaultTableModel tableModel;
        private SimpleDateFormat dateFormat;
        private java.util.List<CommitSearchResult> originalResults;
        private JTextField projectFilterField;
        private JTextField keywordFilterField;
        private JTextField authorFilterField;
        private JLabel resultCountLabel;
        private JTextArea commitDetailsTextArea;
        private String currentCommitUrl;
        private File currentDirectory; // 保存当前目录的引用

        public CommitSearchResultDialog(Frame parent, String searchKeywords, java.util.List<CommitSearchResult> results, File currentDir) {
            super(parent, "Commit Search Results", true);
            this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            this.originalResults = new ArrayList<>(results); // 保存原始结果
            this.currentDirectory = currentDir; // 保存当前目录
            initializeUI(results);
            setLocationRelativeTo(parent);
        }

        private void initializeUI(java.util.List<CommitSearchResult> results) {
            setLayout(new BorderLayout(10, 10));
            setSize(1400, 800);

            // 主面板
            JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

            // 过滤面板
            JPanel filterPanel = createFilterPanel();
            mainPanel.add(filterPanel, BorderLayout.NORTH);

            // 结果统计标签
            resultCountLabel = new JLabel("Found " + results.size() + " commits");
            resultCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            resultCountLabel.setForeground(new Color(51, 51, 51));
            JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            statsPanel.add(resultCountLabel);
            mainPanel.add(statsPanel, BorderLayout.CENTER);

            // 创建表格
            String[] columnNames = {"Project Name", "Branch", "Commit Code", "Date", "Author", "Message", "Changed Files"};
            tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            // 填充数据
            populateTable(results);

            resultsTable = new JTable(tableModel) {
                @Override
                public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                    Component c = super.prepareRenderer(renderer, row, column);
                    if (!isRowSelected(row)) {
                        c.setBackground(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
                    }
                    return c;
                }
            };

            resultsTable.setAutoCreateRowSorter(true);
            resultsTable.setRowHeight(28);
            resultsTable.setIntercellSpacing(new Dimension(0, 0));
            resultsTable.setShowGrid(false);
            resultsTable.setSelectionBackground(new Color(187, 222, 251));
            resultsTable.setSelectionForeground(new Color(0, 0, 0));
            resultsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            // 表头样式
            JTableHeader header = resultsTable.getTableHeader();
            header.setBackground(HEADER_BG_COLOR);
            header.setForeground(new Color(95, 99, 104));
            header.setFont(new Font("Segoe UI", Font.BOLD, 12));
            header.setReorderingAllowed(false);
            header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 35));

            // 设置列宽
            resultsTable.getColumnModel().getColumn(0).setPreferredWidth(150);  // Project Name
            resultsTable.getColumnModel().getColumn(1).setPreferredWidth(100);  // Branch
            resultsTable.getColumnModel().getColumn(2).setPreferredWidth(90);   // Commit Code
            resultsTable.getColumnModel().getColumn(3).setPreferredWidth(150);  // Date
            resultsTable.getColumnModel().getColumn(4).setPreferredWidth(120);  // Author
            resultsTable.getColumnModel().getColumn(5).setPreferredWidth(350);  // Message
            resultsTable.getColumnModel().getColumn(6).setPreferredWidth(300);  // Changed Files

            // 为Changed Files列设置自定义渲染器，支持工具提示
            resultsTable.getColumnModel().getColumn(6).setCellRenderer(new ChangedFilesRenderer(originalResults));

            JScrollPane tableScroll = new JScrollPane(resultsTable);
            tableScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
            tableScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 300));

            JPanel tableWrapperPanel = new JPanel(new BorderLayout());
            tableWrapperPanel.add(tableScroll, BorderLayout.CENTER);
            mainPanel.add(tableWrapperPanel, BorderLayout.CENTER);

            // 提交详情显示区域（类似 RepoDetailsDialog）
            JPanel detailsPanel = new JPanel(new BorderLayout());
            detailsPanel.setBorder(BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1),
                    "Commit Details",
                    javax.swing.border.TitledBorder.LEFT,
                    javax.swing.border.TitledBorder.TOP,
                    new Font("Segoe UI", Font.BOLD, 12),
                    new Color(95, 99, 104)
            ));

            commitDetailsTextArea = new JTextArea();
            commitDetailsTextArea.setEditable(false);
            commitDetailsTextArea.setFont(new Font("Consolas", Font.PLAIN, 11));
            commitDetailsTextArea.setBackground(new Color(248, 249, 250));
            commitDetailsTextArea.setMargin(new Insets(10, 10, 10, 10));
            commitDetailsTextArea.setText("Select a commit to see detailed information...");

            // 添加鼠标监听器来处理链接点击
            commitDetailsTextArea.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2 && currentCommitUrl != null) {
                        // 双击时打开commit链接
                        openUrlInBrowser(currentCommitUrl);
                    }
                }
            });

            // 添加工具提示
            commitDetailsTextArea.setToolTipText("Double-click to open commit link in browser");

            JScrollPane detailsScroll = new JScrollPane(commitDetailsTextArea);
            detailsScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
            detailsScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));
            detailsPanel.add(detailsScroll, BorderLayout.CENTER);

            mainPanel.add(detailsPanel, BorderLayout.SOUTH);

            add(mainPanel, BorderLayout.CENTER);

            // 底部按钮
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

            JButton exportButton = new JButton("Export to CSV");
            exportButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            exportButton.addActionListener(e -> exportToCSV(getFilteredResults()));
            buttonPanel.add(exportButton);

            JButton closeButton = new JButton("Close");
            closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            closeButton.addActionListener(e -> dispose());
            buttonPanel.add(closeButton);

            add(buttonPanel, BorderLayout.SOUTH);

            // 添加表格选择监听器（类似 RepoDetailsDialog）
            resultsTable.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = resultsTable.getSelectedRow();
                    if (selectedRow >= 0) {
                        // 获取选中的commit信息
                        int modelRow = resultsTable.convertRowIndexToModel(selectedRow);
                        String projectName = (String) tableModel.getValueAt(modelRow, 0);
                        String branch = (String) tableModel.getValueAt(modelRow, 1);
                        String commitId = (String) tableModel.getValueAt(modelRow, 2);
                        displayCommitDetails(projectName, branch, commitId);
                    }
                }
            });
        }

        /**
         * 创建过滤面板
         */
        private JPanel createFilterPanel() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Filter Results",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(95, 99, 104)
            ));

            JPanel filtersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

            // 项目名称过滤
            JLabel projectLabel = new JLabel("Project:");
            projectLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            filtersPanel.add(projectLabel);

            projectFilterField = new JTextField();
            projectFilterField.setPreferredSize(new Dimension(150, 28));
            projectFilterField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            projectFilterField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
            projectFilterField.setToolTipText("Filter by project name");
            filtersPanel.add(projectFilterField);

            // 消息关键词过滤
            JLabel keywordLabel = new JLabel("Message Keywords:");
            keywordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            filtersPanel.add(keywordLabel);

            keywordFilterField = new JTextField();
            keywordFilterField.setPreferredSize(new Dimension(150, 28));
            keywordFilterField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            keywordFilterField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
            keywordFilterField.setToolTipText("Filter by message content");
            filtersPanel.add(keywordFilterField);

            // 提交人过滤
            JLabel authorLabel = new JLabel("Author:");
            authorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            filtersPanel.add(authorLabel);

            authorFilterField = new JTextField();
            authorFilterField.setPreferredSize(new Dimension(150, 28));
            authorFilterField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            authorFilterField.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true));
            authorFilterField.setToolTipText("Filter by author name");
            filtersPanel.add(authorFilterField);

            // 应用过滤按钮
            JButton applyFilterButton = new JButton("Apply Filters");
            applyFilterButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
            applyFilterButton.setBackground(PRIMARY_COLOR);
            applyFilterButton.setForeground(Color.WHITE);
            applyFilterButton.setFocusPainted(false);
            applyFilterButton.setBorderPainted(false);
            applyFilterButton.setOpaque(true);
            applyFilterButton.setPreferredSize(new Dimension(120, 32));
            applyFilterButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            applyFilterButton.addActionListener(e -> applyFilters());
            filtersPanel.add(applyFilterButton);

            // 清除过滤按钮
            JButton clearFilterButton = new JButton("Clear");
            clearFilterButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            clearFilterButton.setPreferredSize(new Dimension(80, 32));
            clearFilterButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            clearFilterButton.addActionListener(e -> clearFilters());
            filtersPanel.add(clearFilterButton);

            panel.add(filtersPanel, BorderLayout.CENTER);

            return panel;
        }

        /**
         * 应用过滤条件
         */
        private void applyFilters() {
            String projectFilter = projectFilterField.getText().trim().toLowerCase();
            String keywordFilter = keywordFilterField.getText().trim().toLowerCase();
            String authorFilter = authorFilterField.getText().trim().toLowerCase();

            java.util.List<CommitSearchResult> filteredResults = new ArrayList<>();

            for (CommitSearchResult result : originalResults) {
                // 检查项目名称
                if (!projectFilter.isEmpty() && !result.projectName.toLowerCase().contains(projectFilter)) {
                    continue;
                }

                // 检查消息关键词
                if (!keywordFilter.isEmpty() && !result.message.toLowerCase().contains(keywordFilter)) {
                    continue;
                }

                // 检查提交人
                if (!authorFilter.isEmpty() && !result.author.toLowerCase().contains(authorFilter)) {
                    continue;
                }

                filteredResults.add(result);
            }

            // 清空表格并重新填充
            tableModel.setRowCount(0);
            populateTable(filteredResults);

            // 更新结果计数
            resultCountLabel.setText("Found " + filteredResults.size() + " commits" +
                (filteredResults.size() < originalResults.size() ?
                    " (filtered from " + originalResults.size() + ")" : ""));
        }

        /**
         * 清除过滤条件
         */
        private void clearFilters() {
            projectFilterField.setText("");
            keywordFilterField.setText("");
            authorFilterField.setText("");

            // 清空表格并重新填充所有结果
            tableModel.setRowCount(0);
            populateTable(originalResults);

            // 更新结果计数
            resultCountLabel.setText("Found " + originalResults.size() + " commits");
        }

        /**
         * 填充表格数据
         */
        private void populateTable(java.util.List<CommitSearchResult> results) {
            for (CommitSearchResult result : results) {
                Object[] row = {
                    result.projectName,
                    result.branch,
                    result.commitId,
                    dateFormat.format(new Date(result.commitTime)),
                    result.author,
                    truncateMessage(result.message, 100),
                    truncateMessage(result.changedFiles, 80)
                };
                tableModel.addRow(row);
            }
        }

        /**
         * 获取当前过滤后的结果
         */
        private java.util.List<CommitSearchResult> getFilteredResults() {
            java.util.List<CommitSearchResult> filteredResults = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String projectName = (String) tableModel.getValueAt(i, 0);
                String commitId = (String) tableModel.getValueAt(i, 2);

                // 从原始结果中查找匹配的记录
                for (CommitSearchResult result : originalResults) {
                    if (result.projectName.equals(projectName) && result.commitId.equals(commitId)) {
                        filteredResults.add(result);
                        break;
                    }
                }
            }
            return filteredResults;
        }

        /**
         * 显示选中的commit详细信息
         */
        private void displayCommitDetails(String projectName, String branch, String commitId) {
            // 从原始结果中查找对应的commit
            CommitSearchResult selectedCommit = null;
            for (CommitSearchResult result : originalResults) {
                if (result.projectName.equals(projectName) && result.commitId.equals(commitId)) {
                    selectedCommit = result;
                    break;
                }
            }

            if (selectedCommit == null) {
                commitDetailsTextArea.setText("Error: Cannot find commit details");
                return;
            }

            // 构建详细信息字符串（类似 RepoDetailsDialog 的格式）
            StringBuilder sb = new StringBuilder();

            // 获取仓库的远程URL
            String remoteUrl = getRemoteUrlForProject(projectName);
            String commitUrl = buildCommitUrl(remoteUrl, commitId);
            currentCommitUrl = commitUrl;

            sb.append("Project: ").append(selectedCommit.projectName).append("\n");
            sb.append("Branch: ").append(selectedCommit.branch).append("\n");
            sb.append("Commit Code: ").append(selectedCommit.commitId).append("\n");
            sb.append("Author: ").append(selectedCommit.author).append("\n");
            sb.append("Date: ").append(dateFormat.format(new Date(selectedCommit.commitTime))).append("\n");
            sb.append("Repository URL: ").append(remoteUrl).append("\n");

            if (commitUrl != null) {
                sb.append("Commit Link: ").append(commitUrl).append("\n");
                sb.append("(Double-click anywhere in this area to open the commit link)\n");
            }

            sb.append("\nMessage:\n");
            sb.append("========================================\n");
            sb.append(selectedCommit.message).append("\n\n");

            // 显示修改的文件
            if (selectedCommit.changedFiles != null && !selectedCommit.changedFiles.trim().isEmpty()) {
                String[] files = selectedCommit.changedFiles.split(",\\s*");
                sb.append("Changed Files (").append(files.length).append("):\n");
                sb.append("========================================\n");
                for (String file : files) {
                    sb.append(file).append("\n");
                }
            } else {
                sb.append("Changed Files: No files changed or this is the initial commit.\n");
            }

            commitDetailsTextArea.setText(sb.toString());
        }

        /**
         * 获取指定项目的远程URL
         */
        private String getRemoteUrlForProject(String projectName) {
            // 尝试从当前目录查找项目
            if (currentDirectory != null) {
                File[] children = currentDirectory.listFiles();
                if (children != null) {
                    for (File child : children) {
                        if (child.getName().equals(projectName) && GitInfoExtractor.isGitRepository(child)) {
                            try {
                                org.eclipse.jgit.storage.file.FileRepositoryBuilder builder = new org.eclipse.jgit.storage.file.FileRepositoryBuilder();
                                org.eclipse.jgit.lib.Repository repository = builder
                                        .setGitDir(new java.io.File(child, ".git"))
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
                    }
                }
            }
            return "Unknown repository";
        }

        /**
         * 构建commit的超链接URL（类似 RepoDetailsDialog）
         */
        private String buildCommitUrl(String remoteUrl, String commitId) {
            if (remoteUrl == null || remoteUrl.equals("No remote URL configured") ||
                remoteUrl.equals("Unknown repository") || remoteUrl.startsWith("Error")) {
                return null;
            }

            try {
                // 移除.git后缀
                String baseUrl = remoteUrl;
                if (baseUrl.endsWith(".git")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 4);
                }

                // 使用GitLab格式
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
        
        private String truncateMessage(String message, int maxLength) {
            if (message == null) {
                return "";
            }
            message = message.replaceAll("\\r\\n|\\r|\\n", " ");
            if (message.length() > maxLength) {
                return message.substring(0, maxLength - 3) + "...";
            }
            return message;
        }
        
        /**
         * Changed Files列的自定义渲染器，支持工具提示
         */
        private class ChangedFilesRenderer extends DefaultTableCellRenderer {
            private java.util.List<CommitSearchResult> results;
            
            public ChangedFilesRenderer(java.util.List<CommitSearchResult> results) {
                this.results = results;
            }
            
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                // 设置工具提示
                if (row < results.size()) {
                    CommitSearchResult result = results.get(row);
                    String changedFiles = result.changedFiles;
                    
                    if (changedFiles != null && !changedFiles.trim().isEmpty()) {
                        // 将逗号分隔的文件名转换为HTML格式的换行显示
                        String[] files = changedFiles.split(",\\s*");
                        StringBuilder tooltip = new StringBuilder("<html>");
                        tooltip.append("<b>Changed Files (").append(files.length).append("):</b><br>");
                        
                        for (int i = 0; i < files.length; i++) {
                            String file = files[i].trim();
                            if (!file.isEmpty()) {
                                // 根据文件操作类型设置不同的颜色
                                if (file.startsWith("[ADD]")) {
                                    tooltip.append("<span style='color: #28a745;'>").append(file).append("</span>");
                                } else if (file.startsWith("[DELETE]")) {
                                    tooltip.append("<span style='color: #dc3545;'>").append(file).append("</span>");
                                } else if (file.startsWith("[MODIFY]")) {
                                    tooltip.append("<span style='color: #007bff;'>").append(file).append("</span>");
                                } else if (file.startsWith("[RENAME]")) {
                                    tooltip.append("<span style='color: #ffc107;'>").append(file).append("</span>");
                                } else {
                                    tooltip.append(file);
                                }
                                
                                if (i < files.length - 1) {
                                    tooltip.append("<br>");
                                }
                            }
                        }
                        tooltip.append("</html>");
                        setToolTipText(tooltip.toString());
                    } else {
                        setToolTipText("No files changed");
                    }
                } else {
                    setToolTipText(null);
                }
                
                // 设置斑马纹背景色
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
                }
                
                return c;
            }
        }
        
        private void exportToCSV(java.util.List<CommitSearchResult> results) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Search Results");
            fileChooser.setSelectedFile(new java.io.File("commit_search_results.csv"));
            
            // 设置文件选择器的字体为12号
            Font chooserFont = new Font("Segoe UI", Font.PLAIN, 12);
            applyFontToFileChooser(fileChooser, chooserFont);
            
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                try (java.io.PrintWriter writer = new java.io.PrintWriter(fileChooser.getSelectedFile())) {
                    // CSV头部
                    writer.println("Project Name,Branch,Commit Code,Date,Author,Message,Changed Files");
                    
                    // 数据行
                    for (CommitSearchResult searchResult : results) {
                        writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                            searchResult.projectName,
                            searchResult.branch,
                            searchResult.commitId,
                            dateFormat.format(new Date(searchResult.commitTime)),
                            searchResult.author,
                            searchResult.message.replace("\"", "\"\""),
                            searchResult.changedFiles.replace("\"", "\"\"")
                        );
                    }
                    
                    JOptionPane.showMessageDialog(this, 
                        "Results exported successfully to: " + fileChooser.getSelectedFile().getAbsolutePath(),
                        "Export Complete", 
                        JOptionPane.INFORMATION_MESSAGE);
                        
                } catch (java.io.IOException ex) {
                    JOptionPane.showMessageDialog(this, 
                        "Error exporting results: " + ex.getMessage(),
                        "Export Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        
        /**
         * 递归设置JFileChooser及其所有子组件的字体
         */
        private void applyFontToFileChooser(Container container, Font font) {
            for (Component comp : container.getComponents()) {
                comp.setFont(font);
                if (comp instanceof Container) {
                    applyFontToFileChooser((Container) comp, font);
                }
            }
        }
    }
}
