package com.gitviewer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commit & Push 对话框
 * 支持查看未暂存/未推送文件，勾选后一键 commit & push
 */
public class CommitPushDialog extends JDialog {

    private static final Color BORDER_COLOR = new Color(218, 220, 224);
    private static final Color ACCENT_COLOR = new Color(66, 133, 244);
    private static final Color COMMIT_HEADER_BG = new Color(255, 243, 224);
    private static final Color COMMIT_HEADER_BORDER = new Color(230, 160, 60);
    private static final Color COMMIT_LIST_BG = new Color(255, 252, 245);
    private static final Color PUSH_HEADER_BG = new Color(224, 242, 254);
    private static final Color PUSH_HEADER_BORDER = new Color(66, 133, 244);
    private static final Color PUSH_LIST_BG = new Color(245, 250, 255);

    // 数据
    private List<File> gitRepos;
    private List<GitOperations.FileChangeInfo> allUnstagedFiles = new ArrayList<>();
    private List<GitOperations.FileChangeInfo> allUnpushedFiles = new ArrayList<>();
    private List<GitOperations.FileChangeInfo> filteredUnstagedFiles = new ArrayList<>();
    private List<GitOperations.FileChangeInfo> filteredUnpushedFiles = new ArrayList<>();

    // 左侧（待 Commit）
    private JTextField unstagedFilterField;
    private JCheckBox unstagedSelectAllCheckbox;
    private JPanel unstagedListPanel;
    private JLabel unstagedCountLabel;
    private List<JCheckBox> unstagedCheckboxes = new ArrayList<>();

    // 右侧（待 Push）
    private JTextField unpushedFilterField;
    private JCheckBox unpushedSelectAllCheckbox;
    private JPanel unpushedListPanel;
    private JLabel unpushedCountLabel;
    private List<JCheckBox> unpushedCheckboxes = new ArrayList<>();

    // 底部
    private JTextArea commitMessageArea;
    private JButton pushButton;
    private JButton gitignoreButton;
    private JButton refreshButton;

    public CommitPushDialog(Frame parent, File selectedDir) {
        super(parent, "Commit & Push", true);
        gitRepos = GitOperations.findGitRepositories(selectedDir);
        initializeUI();
        loadData();
        setSize(1400, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);

        add(createHeader(), BorderLayout.NORTH);

        // 主内容：左右分栏
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(700);
        splitPane.setResizeWeight(0.5);
        splitPane.setDividerSize(5);
        splitPane.setBorder(null);
        splitPane.setBackground(Color.WHITE);

        splitPane.setLeftComponent(createFileListPanel(true));
        splitPane.setRightComponent(createFileListPanel(false));

        add(splitPane, BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(10, 15, 5, 15)
        ));

        JLabel titleLabel = new JLabel("Commit & Push");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(60, 64, 67));
        panel.add(titleLabel);

        // 变更类型图例
        JLabel legendLabel = new JLabel("[M] Modified   [A] Added   [D] Deleted   [UT] Untracked   [RE] Renamed");
        legendLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        legendLabel.setForeground(new Color(140, 140, 140));
        panel.add(legendLabel);

        if (gitRepos.size() > 1) {
            StringBuilder repoInfo = new StringBuilder("Repositories: ");
            for (int i = 0; i < gitRepos.size(); i++) {
                if (i > 0) repoInfo.append(", ");
                repoInfo.append(gitRepos.get(i).getName());
            }
            JLabel repoLabel = new JLabel(repoInfo.toString());
            repoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            repoLabel.setForeground(new Color(95, 99, 104));
            panel.add(Box.createVerticalStrut(3));
            panel.add(repoLabel);
        }

        return panel;
    }

    private JPanel createFileListPanel(boolean isUnstaged) {
        Color headerBg = isUnstaged ? COMMIT_HEADER_BG : PUSH_HEADER_BG;
        Color headerBorder = isUnstaged ? COMMIT_HEADER_BORDER : PUSH_HEADER_BORDER;
        Color listBg = isUnstaged ? COMMIT_LIST_BG : PUSH_LIST_BG;

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(listBg);
        panel.setBorder(BorderFactory.createLineBorder(headerBorder, 2));

        // 标题栏
        String stepNum = isUnstaged ? "STEP 1" : "STEP 2";
        String action = isUnstaged ? "COMMIT" : "PUSH";
        String desc = isUnstaged
                ? "Modified / untracked files not yet committed"
                : "Committed files not yet pushed to remote";

        JPanel titlePanel = new JPanel(new BorderLayout(5, 3));
        titlePanel.setBackground(headerBg);
        titlePanel.setBorder(new EmptyBorder(10, 12, 10, 12));

        // 左侧：步骤 + 动作名
        JPanel leftTitle = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftTitle.setBackground(headerBg);

        JLabel stepLabel = new JLabel(stepNum + "  ");
        stepLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        stepLabel.setForeground(new Color(150, 150, 150));
        leftTitle.add(stepLabel);

        JLabel actionLabel = new JLabel(action);
        actionLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        actionLabel.setForeground(headerBorder);
        leftTitle.add(actionLabel);

        titlePanel.add(leftTitle, BorderLayout.WEST);

        // 右侧：说明
        JLabel descLabel = new JLabel(desc);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLabel.setForeground(new Color(120, 120, 120));
        titlePanel.add(descLabel, BorderLayout.EAST);

        panel.add(titlePanel, BorderLayout.NORTH);

        // 工具栏：过滤框 + 全选
        JPanel toolbarPanel = new JPanel(new BorderLayout(5, 5));
        toolbarPanel.setBackground(listBg);
        toolbarPanel.setBorder(new EmptyBorder(5, 5, 3, 5));

        JTextField filterField = new JTextField();
        filterField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        filterField.setPreferredSize(new Dimension(200, 28));
        filterField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        toolbarPanel.add(filterField, BorderLayout.CENTER);

        JCheckBox selectAll = new JCheckBox("Select All");
        selectAll.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        selectAll.setBackground(listBg);
        toolbarPanel.add(selectAll, BorderLayout.WEST);

        panel.add(toolbarPanel, BorderLayout.BEFORE_FIRST_LINE);

        // 列表标题 + 滚动面板 的容器
        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setBackground(listBg);

        // 列表标题
        String listTitle = isUnstaged ? "to be committed" : "to be pushed";
        JLabel listTitleLabel = new JLabel(listTitle + " (0)");
        listTitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        listTitleLabel.setForeground(new Color(140, 140, 140));
        listTitleLabel.setBackground(listBg);
        listTitleLabel.setBorder(new EmptyBorder(3, 5, 3, 5));
        centerPanel.add(listTitleLabel, BorderLayout.NORTH);

        if (isUnstaged) {
            unstagedCountLabel = listTitleLabel;
        } else {
            unpushedCountLabel = listTitleLabel;
        }

        // 文件列表面板
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(listBg);

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(listBg);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        // 绑定引用
        if (isUnstaged) {
            unstagedFilterField = filterField;
            unstagedSelectAllCheckbox = selectAll;
            unstagedListPanel = listPanel;
        } else {
            unpushedFilterField = filterField;
            unpushedSelectAllCheckbox = selectAll;
            unpushedListPanel = listPanel;
        }

        setupFilterListener(filterField, isUnstaged);
        setupSelectAllListener(selectAll, isUnstaged);

        return panel;
    }

    private void setupFilterListener(JTextField filterField, boolean isUnstaged) {
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilter(isUnstaged); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilter(isUnstaged); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilter(isUnstaged); }
        });
    }

    private void applyFilter(boolean isUnstaged) {
        if (isUnstaged) {
            String keyword = unstagedFilterField.getText().trim().toLowerCase();
            filteredUnstagedFiles = keyword.isEmpty()
                    ? new ArrayList<>(allUnstagedFiles)
                    : allUnstagedFiles.stream()
                        .filter(f -> f.toString().toLowerCase().contains(keyword)
                                || f.getFilePath().toLowerCase().contains(keyword)
                                || f.getModuleName().toLowerCase().contains(keyword))
                        .collect(Collectors.toList());
            refreshUnstagedList();
        } else {
            String keyword = unpushedFilterField.getText().trim().toLowerCase();
            filteredUnpushedFiles = keyword.isEmpty()
                    ? new ArrayList<>(allUnpushedFiles)
                    : allUnpushedFiles.stream()
                        .filter(f -> f.toString().toLowerCase().contains(keyword)
                                || f.getFilePath().toLowerCase().contains(keyword)
                                || f.getModuleName().toLowerCase().contains(keyword))
                        .collect(Collectors.toList());
            refreshUnpushedList();
        }
    }

    private void setupSelectAllListener(JCheckBox selectAll, boolean isUnstaged) {
        selectAll.addActionListener(e -> {
            boolean selected = selectAll.isSelected();
            List<JCheckBox> checkboxes = isUnstaged ? unstagedCheckboxes : unpushedCheckboxes;
            for (JCheckBox cb : checkboxes) {
                cb.setSelected(selected);
            }
        });
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(10, 15, 10, 15)
        ));

        // Commit message
        JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
        messagePanel.setBackground(Color.WHITE);

        JLabel msgLabel = new JLabel("Commit Message");
        msgLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        msgLabel.setForeground(new Color(60, 64, 67));
        messagePanel.add(msgLabel, BorderLayout.NORTH);

        commitMessageArea = new JTextArea(3, 40);
        commitMessageArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        commitMessageArea.setLineWrap(true);
        commitMessageArea.setWrapStyleWord(true);
        commitMessageArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(8, 10, 8, 10)
        ));
        JScrollPane msgScroll = new JScrollPane(commitMessageArea);
        msgScroll.setBorder(null);
        messagePanel.add(msgScroll, BorderLayout.CENTER);

        panel.add(messagePanel, BorderLayout.CENTER);

        // 按钮栏
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setBackground(Color.WHITE);

        refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshButton.addActionListener(e -> loadData());
        buttonPanel.add(refreshButton);

        gitignoreButton = new JButton("Edit .gitignore");
        gitignoreButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gitignoreButton.addActionListener(e -> openGitignoreEditor());
        buttonPanel.add(gitignoreButton);

        pushButton = new JButton("Commit & Push");
        pushButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pushButton.setForeground(Color.WHITE);
        pushButton.setBackground(ACCENT_COLOR);
        pushButton.setOpaque(true);
        pushButton.setBorderPainted(false);
        pushButton.setFocusPainted(false);
        pushButton.setPreferredSize(new Dimension(140, 32));
        pushButton.addActionListener(e -> performCommitAndPush());
        buttonPanel.add(pushButton);

        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    // ===== Data Loading =====

    private void loadData() {
        allUnstagedFiles.clear();
        allUnpushedFiles.clear();

        pushButton.setEnabled(false);
        refreshButton.setEnabled(false);

        // 显示 loading
        showLoading(true);

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                for (File repo : gitRepos) {
                    try {
                        allUnstagedFiles.addAll(GitOperations.getUnstagedFiles(repo));
                        allUnpushedFiles.addAll(GitOperations.getUnpushedFiles(repo));
                    } catch (Exception e) {
                        System.err.println("Error loading repo " + repo.getName() + ": " + e.getMessage());
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                showLoading(false);
                filteredUnstagedFiles = new ArrayList<>(allUnstagedFiles);
                filteredUnpushedFiles = new ArrayList<>(allUnpushedFiles);
                refreshUnstagedList();
                refreshUnpushedList();
                pushButton.setEnabled(true);
                refreshButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    private void showLoading(boolean loading) {
        unstagedListPanel.removeAll();
        unpushedListPanel.removeAll();
        unstagedCheckboxes.clear();
        unpushedCheckboxes.clear();

        if (loading) {
            JLabel loadingLabel = new JLabel("Loading...");
            loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            loadingLabel.setForeground(new Color(128, 128, 128));
            loadingLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
            loadingLabel.setBackground(COMMIT_LIST_BG);
            loadingLabel.setOpaque(true);
            unstagedListPanel.add(loadingLabel);

            JLabel loadingLabel2 = new JLabel("Loading...");
            loadingLabel2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            loadingLabel2.setForeground(new Color(128, 128, 128));
            loadingLabel2.setBorder(new EmptyBorder(20, 20, 20, 20));
            loadingLabel2.setBackground(PUSH_LIST_BG);
            loadingLabel2.setOpaque(true);
            unpushedListPanel.add(loadingLabel2);
        }

        unstagedListPanel.revalidate();
        unstagedListPanel.repaint();
        unpushedListPanel.revalidate();
        unpushedListPanel.repaint();
    }

    private void refreshUnstagedList() {
        java.util.Set<String> selectedPaths = getSelectedFilePaths(true);

        unstagedListPanel.removeAll();
        unstagedCheckboxes.clear();

        unstagedCountLabel.setText("to be committed (" + filteredUnstagedFiles.size() + ")");

        if (filteredUnstagedFiles.isEmpty()) {
            JLabel emptyLabel = new JLabel("No changes to commit");
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            emptyLabel.setForeground(new Color(128, 128, 128));
            emptyLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
            emptyLabel.setBackground(COMMIT_LIST_BG);
            emptyLabel.setOpaque(true);
            unstagedListPanel.add(emptyLabel);
        } else {
            for (GitOperations.FileChangeInfo file : filteredUnstagedFiles) {
                JPanel item = createFileItem(file, true);
                JCheckBox cb = (JCheckBox) ((BorderLayout) item.getLayout()).getLayoutComponent(BorderLayout.WEST);
                if (cb != null && selectedPaths.contains(file.getRepoDir().getName() + ":" + file.getFilePath())) {
                    cb.setSelected(true);
                }
                unstagedListPanel.add(item);
            }
        }

        unstagedListPanel.revalidate();
        unstagedListPanel.repaint();
        updateSelectAllState(true);
    }

    private void refreshUnpushedList() {
        java.util.Set<String> selectedPaths = getSelectedFilePaths(false);

        unpushedListPanel.removeAll();
        unpushedCheckboxes.clear();

        unpushedCountLabel.setText("to be pushed (" + filteredUnpushedFiles.size() + ")");

        if (filteredUnpushedFiles.isEmpty()) {
            JLabel emptyLabel = new JLabel("No changes to push");
            emptyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            emptyLabel.setForeground(new Color(128, 128, 128));
            emptyLabel.setBorder(new EmptyBorder(20, 20, 20, 20));
            emptyLabel.setBackground(PUSH_LIST_BG);
            emptyLabel.setOpaque(true);
            unpushedListPanel.add(emptyLabel);
        } else {
            for (GitOperations.FileChangeInfo file : filteredUnpushedFiles) {
                JPanel item = createFileItem(file, false);
                JCheckBox cb = (JCheckBox) ((BorderLayout) item.getLayout()).getLayoutComponent(BorderLayout.WEST);
                if (cb != null && selectedPaths.contains(file.getRepoDir().getName() + ":" + file.getFilePath())) {
                    cb.setSelected(true);
                }
                unpushedListPanel.add(item);
            }
        }

        unpushedListPanel.revalidate();
        unpushedListPanel.repaint();
        updateSelectAllState(false);
    }

    private java.util.Set<String> getSelectedFilePaths(boolean isUnstaged) {
        java.util.Set<String> selected = new java.util.HashSet<>();
        List<JCheckBox> checkboxes = isUnstaged ? unstagedCheckboxes : unpushedCheckboxes;
        List<GitOperations.FileChangeInfo> files = isUnstaged ? filteredUnstagedFiles : filteredUnpushedFiles;
        for (int i = 0; i < checkboxes.size() && i < files.size(); i++) {
            if (checkboxes.get(i).isSelected()) {
                GitOperations.FileChangeInfo f = files.get(i);
                selected.add(f.getRepoDir().getName() + ":" + f.getFilePath());
            }
        }
        return selected;
    }

    private void updateSelectAllState(boolean isUnstaged) {
        List<JCheckBox> checkboxes = isUnstaged ? unstagedCheckboxes : unpushedCheckboxes;
        JCheckBox selectAll = isUnstaged ? unstagedSelectAllCheckbox : unpushedSelectAllCheckbox;
        if (checkboxes.isEmpty()) {
            selectAll.setSelected(false);
            return;
        }
        boolean allSelected = true;
        for (JCheckBox cb : checkboxes) {
            if (!cb.isSelected()) {
                allSelected = false;
                break;
            }
        }
        selectAll.setSelected(allSelected);
    }

    private JPanel createFileItem(GitOperations.FileChangeInfo file, boolean isUnstaged) {
        Color listBg = isUnstaged ? COMMIT_LIST_BG : PUSH_LIST_BG;

        JPanel itemPanel = new JPanel(new BorderLayout(8, 0));
        itemPanel.setBackground(listBg);
        itemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                new EmptyBorder(5, 5, 5, 5)
        ));
        itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JCheckBox checkbox = new JCheckBox();
        checkbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        checkbox.setBackground(listBg);

        // 文件信息
        String changeTypeShort = getShortChangeType(file.getChangeType());
        String changeTypeColor = getChangeTypeColor(file.getChangeType());
        String timeStr = "";
        if (file.getLastModified() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
            timeStr = sdf.format(new Date(file.getLastModified()));
        }

        JLabel infoLabel = new JLabel("<html>"
                + "<b>" + escapeHtml(file.getModuleName()) + "</b> "
                + "<span style='color:#5f6368'>[" + escapeHtml(file.getBranch()) + "]</span> "
                + "<span style='color:" + changeTypeColor + "'>[" + changeTypeShort + "]</span> "
                + escapeHtml(file.getFilePath())
                + (timeStr.isEmpty() ? "" : " <span style='color:#999;font-size:11px'>" + timeStr + "</span>")
                + "<br><span style='color:#aaa;font-size:10px'>" + escapeHtml(file.getRepoPath()) + "</span>"
                + "</html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // 右侧：ignore 按钮
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setBackground(listBg);

        JButton ignoreBtn = new JButton("ignore");
        ignoreBtn.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        ignoreBtn.setForeground(new Color(120, 120, 120));
        ignoreBtn.setBorderPainted(false);
        ignoreBtn.setContentAreaFilled(false);
        ignoreBtn.setFocusPainted(false);
        ignoreBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ignoreBtn.setToolTipText("Add to " + file.getRepoPath() + "/.gitignore");
        ignoreBtn.addActionListener(e -> {
            GitOperations.addToGitignore(file.getRepoDir(), List.of(file.getFilePath()));
            // 刷新列表，被 .gitignore 的文件会自动从列表消失
            loadData();
        });

        rightPanel.add(ignoreBtn);

        itemPanel.add(checkbox, BorderLayout.WEST);
        itemPanel.add(infoLabel, BorderLayout.CENTER);
        itemPanel.add(rightPanel, BorderLayout.EAST);

        if (isUnstaged) {
            unstagedCheckboxes.add(checkbox);
        } else {
            unpushedCheckboxes.add(checkbox);
        }

        // 双击打开 diff
        MouseAdapter dblClickListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (isUnstaged) {
                        // 工作区 vs HEAD
                        boolean untracked = "Untracked".equals(file.getChangeType());
                        WorkingTreeDiffDialog diffDialog = new WorkingTreeDiffDialog(
                                (Frame) getOwner(), file.getRepoDir(), file.getFilePath(), untracked);
                        diffDialog.setVisible(true);
                    } else {
                        // 远程跟踪分支 vs HEAD（已 commit 未 push 的差异）
                        WorkingTreeDiffDialog diffDialog = new WorkingTreeDiffDialog(
                                (Frame) getOwner(), file.getRepoDir(), file.getFilePath(), false, true);
                        diffDialog.setVisible(true);
                    }
                }
            }
        };
        infoLabel.addMouseListener(dblClickListener);
        itemPanel.addMouseListener(dblClickListener);

        // 右键菜单
        MouseAdapter popupListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showFilePopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showFilePopup(e);
            }
            private void showFilePopup(MouseEvent e) {
                JPopupMenu popup = new JPopupMenu();

                JMenuItem openFolderItem = new JMenuItem("Open Folder");
                openFolderItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                openFolderItem.addActionListener(ev -> {
                    File targetFile = new File(file.getRepoDir(), file.getFilePath());
                    File parentDir = targetFile.getParentFile();
                    if (parentDir != null && parentDir.exists()) {
                        try {
                            java.awt.Desktop.getDesktop().open(parentDir);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(CommitPushDialog.this,
                                    "Cannot open folder: " + ex.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
                popup.add(openFolderItem);

                popup.addSeparator();

                // Rollback 菜单
                String rollbackDesc = isUnstaged ? "Rollback to HEAD version" : "Rollback to remote version";
                JMenuItem rollbackItem = new JMenuItem(rollbackDesc);
                rollbackItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                rollbackItem.setForeground(new Color(200, 50, 50));
                rollbackItem.addActionListener(ev -> {
                    int confirm = JOptionPane.showConfirmDialog(CommitPushDialog.this,
                            "<html><b>Rollback file?</b><br><br>"
                                    + escapeHtml(file.getFilePath()) + "<br><br>"
                                    + (isUnstaged
                                    ? "This will discard all local changes and restore the HEAD version."
                                    : "This will revert the file to the remote tracking branch version and create a new commit.")
                                    + "<br>This action cannot be undone.</html>",
                            "Confirm Rollback", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm != JOptionPane.YES_OPTION) return;

                    boolean ok;
                    if (isUnstaged) {
                        ok = GitOperations.rollbackToHead(file.getRepoDir(), file.getFilePath(),
                                "Untracked".equals(file.getChangeType()));
                    } else {
                        ok = GitOperations.rollbackToRemote(file.getRepoDir(), file.getFilePath());
                    }

                    if (ok) {
                        JOptionPane.showMessageDialog(CommitPushDialog.this,
                                "File rolled back successfully.", "Rollback", JOptionPane.INFORMATION_MESSAGE);
                        loadData();
                    } else {
                        JOptionPane.showMessageDialog(CommitPushDialog.this,
                                "Rollback failed.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
                popup.add(rollbackItem);

                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        };
        infoLabel.addMouseListener(popupListener);
        itemPanel.addMouseListener(popupListener);

        return itemPanel;
    }

    private String getShortChangeType(String changeType) {
        if (changeType == null) return "?";
        switch (changeType) {
            case "Modified": return "M";
            case "Added": return "A";
            case "Deleted": return "D";
            case "Untracked": return "UT";
            default: return changeType.substring(0, Math.min(2, changeType.length())).toUpperCase();
        }
    }

    private String getChangeTypeColor(String changeType) {
        if (changeType == null) return "#666";
        switch (changeType) {
            case "Modified": return "#e6a817";
            case "Added": return "#1a991a";
            case "Deleted": return "#cc3333";
            case "Untracked": return "#6666cc";
            default: return "#666";
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ===== .gitignore Editor =====

    private void openGitignoreEditor() {
        if (gitRepos.isEmpty()) return;

        final File targetRepo;
        if (gitRepos.size() == 1) {
            targetRepo = gitRepos.get(0);
        } else {
            // 显示完整路径方便区分
            String[] repoOptions = gitRepos.stream()
                    .map(r -> r.getName() + "  (" + r.getAbsolutePath() + ")")
                    .toArray(String[]::new);
            String selected = (String) JOptionPane.showInputDialog(this,
                    "Select repository to edit .gitignore:",
                    "Edit .gitignore",
                    JOptionPane.PLAIN_MESSAGE, null, repoOptions, repoOptions[0]);
            if (selected == null) return;
            File chosen = gitRepos.get(0);
            for (File r : gitRepos) {
                if (selected.startsWith(r.getName())) {
                    chosen = r;
                    break;
                }
            }
            targetRepo = chosen;
        }

        // 弹出编辑对话框
        JDialog editorDialog = new JDialog(this,
                ".gitignore - " + targetRepo.getName() + " [" + targetRepo.getAbsolutePath() + "]", true);
        editorDialog.setLayout(new BorderLayout(10, 10));
        editorDialog.setSize(600, 500);
        editorDialog.setLocationRelativeTo(this);
        editorDialog.getContentPane().setBackground(Color.WHITE);

        // 顶部：当前选中文件可快速添加
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBackground(Color.WHITE);
        topPanel.setBorder(new EmptyBorder(10, 10, 5, 10));

        JLabel topLabel = new JLabel("Select files below to add, or edit .gitignore directly");
        topLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        topLabel.setForeground(new Color(95, 99, 104));
        topPanel.add(topLabel, BorderLayout.NORTH);

        JButton addSelectedBtn = new JButton("Add Selected Files");
        addSelectedBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        topPanel.add(addSelectedBtn, BorderLayout.EAST);

        editorDialog.add(topPanel, BorderLayout.NORTH);

        // 文本编辑区
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        textArea.setLineWrap(false);
        textArea.setTabSize(4);
        textArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 加载 .gitignore 内容
        File gitignoreFile = new File(targetRepo, ".gitignore");
        String originalContent = "";
        if (gitignoreFile.exists()) {
            try {
                originalContent = new String(Files.readAllBytes(gitignoreFile.toPath()), StandardCharsets.UTF_8);
            } catch (Exception e) {
                originalContent = "";
            }
        }
        textArea.setText(originalContent);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        editorDialog.add(scrollPane, BorderLayout.CENTER);

        // "Add Selected Files" 按钮逻辑
        addSelectedBtn.addActionListener(ev -> {
            StringBuilder toAdd = new StringBuilder();
            for (int i = 0; i < unstagedCheckboxes.size(); i++) {
                if (unstagedCheckboxes.get(i).isSelected()) {
                    GitOperations.FileChangeInfo f = filteredUnstagedFiles.get(i);
                    if (f.getRepoDir().equals(targetRepo)) {
                        String path = f.getFilePath();
                        // 检查是否已存在
                        if (!textArea.getText().contains(path) && !textArea.getText().contains("/" + path)) {
                            toAdd.append(path).append("\n");
                        }
                    }
                }
            }
            if (toAdd.length() > 0) {
                String current = textArea.getText();
                if (!current.isEmpty() && !current.endsWith("\n")) {
                    textArea.append("\n");
                }
                textArea.append(toAdd.toString());
            } else {
                JOptionPane.showMessageDialog(editorDialog,
                        "No new files to add (either none selected or already in .gitignore)",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // 底部按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(new EmptyBorder(5, 10, 10, 10));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelBtn.addActionListener(ev -> editorDialog.dispose());
        btnPanel.add(cancelBtn);

        JButton saveBtn = new JButton("Save");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBackground(ACCENT_COLOR);
        saveBtn.setOpaque(true);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.addActionListener(ev -> {
            try {
                String content = textArea.getText();
                Files.write(gitignoreFile.toPath(), content.getBytes(StandardCharsets.UTF_8));
                JOptionPane.showMessageDialog(editorDialog,
                        ".gitignore saved successfully",
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
                editorDialog.dispose();
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(editorDialog,
                        "Error saving .gitignore: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        btnPanel.add(saveBtn);

        editorDialog.add(btnPanel, BorderLayout.SOUTH);
        editorDialog.setVisible(true);
    }

    // ===== Actions =====

    private boolean confirmCommitAndPush(java.util.Map<File, List<String>> repoFiles,
                                          List<File> reposToPush, String commitMessage) {
        StringBuilder msg = new StringBuilder("<html><div style='width:450px'>");
        msg.append("<b>Commit & Push Confirmation</b><br><br>");

        // Commit 部分
        if (!repoFiles.isEmpty()) {
            int totalCommitFiles = repoFiles.values().stream().mapToInt(List::size).sum();
            msg.append("<b>STEP 1 - Commit (").append(totalCommitFiles).append(" file").append(totalCommitFiles > 1 ? "s" : "").append(")</b><br>");

            for (java.util.Map.Entry<File, List<String>> entry : repoFiles.entrySet()) {
                String branch = GitOperations.getCurrentBranch(entry.getKey());
                msg.append("&nbsp;&nbsp;<b>").append(escapeHtml(entry.getKey().getName()))
                   .append("</b> [").append(escapeHtml(branch != null ? branch : "?")).append("]: ");

                List<String> files = entry.getValue();
                if (files.size() <= 30) {
                    msg.append(String.join(", ", files));
                } else {
                    for (int i = 0; i < 30; i++) {
                        msg.append(files.get(i)).append(", ");
                    }
                    msg.append("... and ").append(files.size() - 30).append(" more");
                }
                msg.append("<br>");
            }
            msg.append("<br>");
        }

        // Push 部分
        if (!reposToPush.isEmpty()) {
            msg.append("<b>STEP 2 - Push</b><br>");
            for (File repo : reposToPush) {
                String branch = GitOperations.getCurrentBranch(repo);
                msg.append("&nbsp;&nbsp;<b>").append(escapeHtml(repo.getName()))
                   .append("</b> → ").append(escapeHtml(branch != null ? branch : "?")).append("<br>");
            }
            msg.append("<br>");
        }

        if (!commitMessage.isEmpty()) {
            msg.append("<b>Message:</b> ").append(escapeHtml(commitMessage)).append("<br>");
        }

        msg.append("<br>Proceed?");
        msg.append("</div></html>");

        JLabel msgLabel = new JLabel(msg.toString());
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(msgLabel);
        scrollPane.setPreferredSize(new Dimension(520, 350));
        scrollPane.setBorder(null);

        String[] options = {"Yes", "No"};
        JOptionPane pane = new JOptionPane(scrollPane, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_OPTION, null, options, options[0]);
        JDialog confirmDialog = pane.createDialog(this, "Confirm Commit & Push");
        confirmDialog.setResizable(true);
        confirmDialog.setVisible(true);

        Object selectedValue = pane.getValue();
        return selectedValue != null && selectedValue.equals("Yes");
    }

    private void performCommitAndPush() {
        // 收集选中的文件，按 repo 分组
        java.util.Map<File, List<String>> repoFiles = new java.util.LinkedHashMap<>();

        for (int i = 0; i < unstagedCheckboxes.size(); i++) {
            if (unstagedCheckboxes.get(i).isSelected()) {
                GitOperations.FileChangeInfo file = filteredUnstagedFiles.get(i);
                repoFiles.computeIfAbsent(file.getRepoDir(), k -> new ArrayList<>())
                        .add(file.getFilePath());
            }
        }

        // 检查是否有未推送的选中文件（这些文件已经 commit 过，只需 push）
        java.util.Set<File> reposToPushSet = new java.util.LinkedHashSet<>();
        java.util.Map<File, Integer> pushFileCountPerRepo = new java.util.LinkedHashMap<>();
        int selectedPushFileCount = 0;
        for (int i = 0; i < unpushedCheckboxes.size(); i++) {
            if (unpushedCheckboxes.get(i).isSelected()) {
                File repo = filteredUnpushedFiles.get(i).getRepoDir();
                reposToPushSet.add(repo);
                pushFileCountPerRepo.merge(repo, 1, Integer::sum);
                selectedPushFileCount++;
            }
        }
        final int finalPushFileCount = selectedPushFileCount;
        // 也要 push 包含 commit 文件的 repo
        reposToPushSet.addAll(repoFiles.keySet());
        List<File> reposToPush = new ArrayList<>(reposToPushSet);

        if (repoFiles.isEmpty() && reposToPush.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select files to commit or push",
                    "No Files Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String commitMessage = commitMessageArea.getText().trim();
        if (!repoFiles.isEmpty() && commitMessage.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a commit message",
                    "Commit Message Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 构建确认信息
        if (!confirmCommitAndPush(repoFiles, reposToPush, commitMessage)) {
            return;
        }

        pushButton.setEnabled(false);
        pushButton.setText("Pushing...");

        // 进度对话框
        int totalSteps = repoFiles.size() + (int) reposToPush.stream().filter(r -> !repoFiles.containsKey(r)).count();
        JLabel statusLabel = new JLabel("Preparing...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JTextArea logArea = new JTextArea(8, 40);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setEditable(false);
        logArea.setBackground(new Color(248, 249, 250));

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JProgressBar progressBar = new JProgressBar(0, totalSteps);
        progressBar.setPreferredSize(new Dimension(400, 20));
        progressBar.setStringPainted(true);
        panel.add(statusLabel, BorderLayout.NORTH);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        JOptionPane optionPane = new JOptionPane(panel,
                JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, new Object[]{}, null);
        JDialog progressDialog = optionPane.createDialog(this, "Commit & Push Progress");
        progressDialog.setResizable(true);
        progressDialog.setModal(false);
        progressDialog.setVisible(true);

        SwingWorker<String[], Void> worker = new SwingWorker<String[], Void>() {
            @Override
            protected String[] doInBackground() {
                StringBuilder result = new StringBuilder();
                int step = 0;
                final int[] successRef = {0};
                final int[] failRef = {0};

                // 1. 对有选中 unstaged 文件的 repo：add + commit + push
                for (java.util.Map.Entry<File, List<String>> entry : repoFiles.entrySet()) {
                    File repo = entry.getKey();
                    List<String> files = entry.getValue();
                    step++;
                    final int s = step;
                    final String repoName = repo.getName();
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Committing & Pushing " + s + "/" + totalSteps + ": " + repoName);
                        progressBar.setValue(s - 1);
                        logArea.append("[" + s + "/" + totalSteps + "] " + repoName + " - committing " + files.size() + " files...\n");
                    });

                    final boolean[] okRef = {false};
                    String detail = null;
                    try {
                        okRef[0] = GitOperations.commitAndPush(repo, files, commitMessage);
                    } catch (Exception ex) {
                        detail = ex.getMessage();
                    }
                    if (okRef[0]) successRef[0] += files.size(); else failRef[0] += files.size();
                    String line = repoName + ": " + (okRef[0] ? "Success" : "Failed") + "\n";
                    if (!okRef[0] && detail != null) {
                        line = repoName + ": Failed - " + GitOperations.translateError(detail) + "\n";
                    }
                    result.append(line);
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(s);
                        logArea.append("  → " + (okRef[0] ? "OK" : "FAILED") + "\n");
                    });
                }

                // 2. 对只选了 unpushed 但没有 unstaged 的 repo：只 push
                for (File repo : reposToPush) {
                    if (!repoFiles.containsKey(repo)) {
                        step++;
                        final int s = step;
                        final String repoName = repo.getName();
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Pushing " + s + "/" + totalSteps + ": " + repoName);
                            progressBar.setValue(s - 1);
                            logArea.append("[" + s + "/" + totalSteps + "] " + repoName + " - pushing...\n");
                        });

                        final boolean[] ok2Ref = {false};
                        String detail2 = null;
                        try {
                            ok2Ref[0] = GitOperations.commitAndPush(repo, new ArrayList<>(), commitMessage.isEmpty() ? "push" : commitMessage);
                        } catch (Exception ex) {
                            detail2 = ex.getMessage();
                        }
                        int pushFileCount = pushFileCountPerRepo.getOrDefault(repo, 1);
                        if (ok2Ref[0]) successRef[0] += pushFileCount; else failRef[0] += pushFileCount;
                        String line = repoName + ": " + (ok2Ref[0] ? "Push Success" : "Push Failed") + "\n";
                        if (!ok2Ref[0] && detail2 != null) {
                            line = repoName + ": Failed - " + GitOperations.translateError(detail2) + "\n";
                        }
                        result.append(line);
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(s);
                            logArea.append("  → " + (ok2Ref[0] ? "OK" : "FAILED") + "\n");
                        });
                    }
                }

                // 统计文件数
                int totalCommitFiles = repoFiles.values().stream().mapToInt(List::size).sum();
                return new String[]{result.toString(), String.valueOf(successRef[0]), String.valueOf(failRef[0]),
                        String.valueOf(totalCommitFiles), String.valueOf(finalPushFileCount)};
            }

            @Override
            protected void done() {
                pushButton.setEnabled(true);
                pushButton.setText("Commit & Push");
                progressDialog.dispose();
                try {
                    String[] data = get();
                    int success = Integer.parseInt(data[1]);
                    int failed = Integer.parseInt(data[2]);
                    int totalCommitFiles = Integer.parseInt(data[3]);
                    int totalPushFiles = Integer.parseInt(data[4]);

                    StringBuilder msg = new StringBuilder("<html><div style='width:400px'>");
                    msg.append("<b>Commit & Push Result</b><br><br>");
                    msg.append("<b>Total files:</b> ").append(totalCommitFiles + totalPushFiles);
                    if (totalCommitFiles > 0) {
                        msg.append(" (").append(totalCommitFiles).append(" to commit");
                    }
                    if (totalPushFiles > 0) {
                        if (totalCommitFiles > 0) msg.append(", ");
                        else msg.append(" (");
                        msg.append(totalPushFiles).append(" to push");
                    }
                    if (totalCommitFiles > 0 || totalPushFiles > 0) msg.append(")");
                    msg.append("<br><br>");
                    msg.append("<span style='color:#2e7d32;font-size:14px'><b>").append(success)
                       .append(" files succeeded</b></span>");
                    if (failed > 0) {
                        msg.append("<br><span style='color:#c62828;font-size:14px'><b>")
                           .append(failed).append(" files failed</b></span>");
                    }
                    msg.append("<br><br>");
                    msg.append("<br><br>");
                    // 逐行显示，失败用红色
                    String[] lines = data[0].split("\n");
                    for (String line : lines) {
                        if (line.contains("Failed")) {
                            msg.append("<span style='color:#c62828'>").append(escapeHtml(line)).append("</span><br>");
                        } else {
                            msg.append("<span style='color:#2e7d32'>").append(escapeHtml(line)).append("</span><br>");
                        }
                    }
                    msg.append("</div></html>");

                    JLabel resultLabel = new JLabel(msg.toString());
                    resultLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    JScrollPane resultScroll = new JScrollPane(resultLabel);
                    resultScroll.setPreferredSize(new Dimension(480, 250));
                    resultScroll.setBorder(null);

                    int msgType = failed > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE;
                    JOptionPane.showMessageDialog(CommitPushDialog.this,
                            resultScroll, "Commit & Push Result", msgType);
                    loadData();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(CommitPushDialog.this,
                            GitOperations.translateError(e.getMessage()), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
