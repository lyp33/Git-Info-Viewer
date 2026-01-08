package com.gitviewer;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件差异对比对话框
 * 显示文件在两个提交之间的差异 - 并排对比视图
 */
public class FileDiffDialog extends JDialog {
    
    private static final Color HEADER_BG_COLOR = new Color(248, 249, 250);
    private static final Color BORDER_COLOR = new Color(227, 233, 239);
    private static final Color ADDED_LINE_COLOR = new Color(230, 255, 230);
    private static final Color REMOVED_LINE_COLOR = new Color(255, 230, 230);
    private static final Color MODIFIED_LINE_COLOR = new Color(255, 250, 205);
    
    private JTextPane beforeTextPane;
    private JTextPane afterTextPane;
    private JScrollPane beforeScrollPane;
    private JScrollPane afterScrollPane;
    private File repoDirectory;
    private String filePath;
    private String commitId;
    private boolean isScrollSyncing = false; // 防止循环触发
    private JLabel fileInfoLabel; // 用于显示文件和commit信息
    
    public FileDiffDialog(Frame parent, File repoDirectory, String filePath, String commitId) {
        super(parent, "File Diff - " + filePath, true);
        this.repoDirectory = repoDirectory;
        this.filePath = filePath;
        this.commitId = commitId;
        initializeUI();
        loadDiff();
        setLocationRelativeTo(parent);
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(1200, 700);
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);
        
        // 标题面板
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel titleLabel = new JLabel("File Information");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(new Color(95, 99, 104));
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        
        fileInfoLabel = new JLabel("<html><b>File:</b> " + filePath + "<br><b>Commit:</b> " + commitId + "<br><b>Loading commit details...</b></html>");
        fileInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fileInfoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        titlePanel.add(fileInfoLabel, BorderLayout.CENTER);
        
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // 并排对比面板
        JPanel comparePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        comparePanel.setBackground(Color.WHITE);
        
        // 左侧：修改前
        JPanel beforePanel = createComparePanel("Before (Parent)", true);
        comparePanel.add(beforePanel);
        
        // 右侧：修改后
        JPanel afterPanel = createComparePanel("After (This Commit)", false);
        comparePanel.add(afterPanel);
        
        mainPanel.add(comparePanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
        
        // 设置滚动联动
        setupScrollSync();
        
        // 底部按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createComparePanel(String title, boolean isBefore) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        
        // 标题
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(new Color(95, 99, 104));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        titleLabel.setBackground(HEADER_BG_COLOR);
        titleLabel.setOpaque(true);
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // 文本面板
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("Consolas", Font.PLAIN, 12));
        textPane.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        if (isBefore) {
            beforeTextPane = textPane;
            beforeScrollPane = scrollPane;
        } else {
            afterTextPane = textPane;
            afterScrollPane = scrollPane;
        }
        
        return panel;
    }
    
    /**
     * 设置滚动联动
     */
    private void setupScrollSync() {
        // 左侧滚动时，同步右侧
        beforeScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (!isScrollSyncing) {
                isScrollSyncing = true;
                afterScrollPane.getVerticalScrollBar().setValue(e.getValue());
                isScrollSyncing = false;
            }
        });
        
        // 右侧滚动时，同步左侧
        afterScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (!isScrollSyncing) {
                isScrollSyncing = true;
                beforeScrollPane.getVerticalScrollBar().setValue(e.getValue());
                isScrollSyncing = false;
            }
        });
    }
    
    private void loadDiff() {
        beforeTextPane.setText("Loading...");
        afterTextPane.setText("Loading...");
        
        SwingWorker<DiffResult, Void> worker = new SwingWorker<DiffResult, Void>() {
            @Override
            protected DiffResult doInBackground() throws Exception {
                DiffResult result = new DiffResult();
                
                // 加载diff
                String diff = GitInfoExtractor.getFileDiff(repoDirectory, null, commitId, filePath);
                result.diffData = parseDiff(diff);
                
                // 加载commit信息
                try {
                    List<GitInfoExtractor.GitCommitInfo> commits = GitInfoExtractor.getRecentCommits(repoDirectory, 1000);
                    for (GitInfoExtractor.GitCommitInfo commit : commits) {
                        if (commit.getCommitId().equals(commitId)) {
                            result.author = commit.getAuthor();
                            result.commitTime = new java.util.Date(commit.getCommitTime());
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                return result;
            }
            
            @Override
            protected void done() {
                try {
                    DiffResult result = get();
                    
                    // 更新文件信息标签
                    StringBuilder info = new StringBuilder("<html>");
                    info.append("<b>File:</b> ").append(filePath).append("<br>");
                    info.append("<b>Commit:</b> ").append(commitId).append("<br>");
                    if (result.author != null) {
                        info.append("<b>Author:</b> ").append(result.author).append("<br>");
                    }
                    if (result.commitTime != null) {
                        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        info.append("<b>Time:</b> ").append(dateFormat.format(result.commitTime));
                    }
                    info.append("</html>");
                    fileInfoLabel.setText(info.toString());
                    
                    // 显示diff
                    displayDiff(result.diffData);
                } catch (Exception e) {
                    beforeTextPane.setText("Error loading diff: " + e.getMessage());
                    afterTextPane.setText("Error loading diff: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        
        worker.execute();
    }
    
    private static class DiffResult {
        DiffData diffData;
        String author;
        java.util.Date commitTime;
    }
    
    private DiffData parseDiff(String diff) {
        DiffData data = new DiffData();
        if (diff == null || diff.isEmpty()) {
            return data;
        }
        
        String[] lines = diff.split("\n");
        List<String> beforeLines = new ArrayList<>();
        List<String> afterLines = new ArrayList<>();
        List<LineType> beforeTypes = new ArrayList<>();
        List<LineType> afterTypes = new ArrayList<>();
        
        // 临时存储连续的删除和添加行
        List<String> pendingRemovals = new ArrayList<>();
        List<String> pendingAdditions = new ArrayList<>();
        
        for (String line : lines) {
            if (line.startsWith("@@")) {
                // 跳过hunk头
                continue;
            } else if (line.startsWith("---") || line.startsWith("+++")) {
                // 跳过文件头
                continue;
            } else if (line.startsWith("diff --git")) {
                // 跳过diff头
                continue;
            } else if (line.startsWith("-")) {
                // 删除的行 - 暂存
                pendingRemovals.add(line.substring(1));
            } else if (line.startsWith("+")) {
                // 添加的行 - 暂存
                pendingAdditions.add(line.substring(1));
            } else {
                // 未改变的行 - 先处理之前暂存的删除和添加
                processPendingChanges(beforeLines, afterLines, beforeTypes, afterTypes, 
                                     pendingRemovals, pendingAdditions);
                
                // 添加未改变的行
                String content = line.startsWith(" ") ? line.substring(1) : line;
                beforeLines.add(content);
                afterLines.add(content);
                beforeTypes.add(LineType.UNCHANGED);
                afterTypes.add(LineType.UNCHANGED);
            }
        }
        
        // 处理最后剩余的删除和添加
        processPendingChanges(beforeLines, afterLines, beforeTypes, afterTypes, 
                             pendingRemovals, pendingAdditions);
        
        data.beforeLines = beforeLines;
        data.afterLines = afterLines;
        data.beforeTypes = beforeTypes;
        data.afterTypes = afterTypes;
        
        return data;
    }
    
    /**
     * 处理暂存的删除和添加行，确保左右对齐
     */
    private void processPendingChanges(List<String> beforeLines, List<String> afterLines,
                                      List<LineType> beforeTypes, List<LineType> afterTypes,
                                      List<String> pendingRemovals, List<String> pendingAdditions) {
        if (pendingRemovals.isEmpty() && pendingAdditions.isEmpty()) {
            return;
        }
        
        int maxLines = Math.max(pendingRemovals.size(), pendingAdditions.size());
        
        for (int i = 0; i < maxLines; i++) {
            // 左侧（删除的行）
            if (i < pendingRemovals.size()) {
                beforeLines.add(pendingRemovals.get(i));
                beforeTypes.add(LineType.REMOVED);
            } else {
                // 右侧有新增但左侧没有对应删除，添加空行
                beforeLines.add("");
                beforeTypes.add(LineType.EMPTY);
            }
            
            // 右侧（添加的行）
            if (i < pendingAdditions.size()) {
                afterLines.add(pendingAdditions.get(i));
                afterTypes.add(LineType.ADDED);
            } else {
                // 左侧有删除但右侧没有对应添加，添加空行
                afterLines.add("");
                afterTypes.add(LineType.EMPTY);
            }
        }
        
        // 清空暂存列表
        pendingRemovals.clear();
        pendingAdditions.clear();
    }
    
    private void displayDiff(DiffData data) {
        if (data.beforeLines.isEmpty() && data.afterLines.isEmpty()) {
            beforeTextPane.setText("No changes or file was not modified in this commit.");
            afterTextPane.setText("No changes or file was not modified in this commit.");
            return;
        }
        
        // 显示前后对比
        displayColoredText(beforeTextPane, data.beforeLines, data.beforeTypes);
        displayColoredText(afterTextPane, data.afterLines, data.afterTypes);
    }
    
    private void displayColoredText(JTextPane textPane, List<String> lines, List<LineType> types) {
        StyledDocument doc = textPane.getStyledDocument();
        
        try {
            doc.remove(0, doc.getLength());
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                LineType type = types.get(i);
                
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                
                switch (type) {
                    case ADDED:
                        StyleConstants.setBackground(attrs, ADDED_LINE_COLOR);
                        break;
                    case REMOVED:
                        StyleConstants.setBackground(attrs, REMOVED_LINE_COLOR);
                        break;
                    case UNCHANGED:
                        StyleConstants.setBackground(attrs, Color.WHITE);
                        break;
                    case EMPTY:
                        // 空行使用浅灰色背景
                        StyleConstants.setBackground(attrs, new Color(245, 245, 245));
                        break;
                }
                
                doc.insertString(doc.getLength(), line + "\n", attrs);
            }
            
            textPane.setCaretPosition(0);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private enum LineType {
        ADDED, REMOVED, UNCHANGED, EMPTY
    }
    
    private static class DiffData {
        List<String> beforeLines = new ArrayList<>();
        List<String> afterLines = new ArrayList<>();
        List<LineType> beforeTypes = new ArrayList<>();
        List<LineType> afterTypes = new ArrayList<>();
    }
}
