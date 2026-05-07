package com.gitviewer;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工作区文件差异对比对话框
 * 显示工作区文件 vs HEAD 的差异
 */
public class WorkingTreeDiffDialog extends JDialog {

    private static final Color HEADER_BG_COLOR = new Color(248, 249, 250);
    private static final Color BORDER_COLOR = new Color(227, 233, 239);
    private static final Color ADDED_LINE_COLOR = new Color(230, 255, 230);
    private static final Color REMOVED_LINE_COLOR = new Color(255, 230, 230);

    private JTextPane beforeTextPane;
    private JTextPane afterTextPane;
    private JScrollPane beforeScrollPane;
    private JScrollPane afterScrollPane;
    private JTextArea beforeLineNumbers;
    private JTextArea afterLineNumbers;
    private boolean isScrollSyncing = false;

    private File repoDir;
    private String filePath;
    private boolean isUntracked;
    private boolean isUnpushed;

    public WorkingTreeDiffDialog(Frame parent, File repoDir, String filePath, boolean isUntracked) {
        this(parent, repoDir, filePath, isUntracked, false);
    }

    public WorkingTreeDiffDialog(Frame parent, File repoDir, String filePath, boolean isUntracked, boolean isUnpushed) {
        super(parent, "Diff - " + filePath, true);
        this.repoDir = repoDir;
        this.filePath = filePath;
        this.isUntracked = isUntracked;
        this.isUnpushed = isUnpushed;
        initializeUI();
        loadDiff();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(1200, 700);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);

        // 标题
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

        String branch = GitOperations.getCurrentBranch(repoDir);
        JLabel infoLabel = new JLabel("<html><b>File:</b> " + filePath
                + "<br><b>Branch:</b> " + branch
                + "<br><b>Module:</b> " + repoDir.getName()
                + (isUntracked ? "<br><b>Status:</b> Untracked (new file)" : "")
                + (isUnpushed ? "<br><b>Status:</b> Committed (not pushed to remote)" : "")
                + "</html>");
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        titlePanel.add(infoLabel, BorderLayout.CENTER);

        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // 并排对比
        JPanel comparePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        comparePanel.setBackground(Color.WHITE);

        comparePanel.add(createComparePanel(isUnpushed ? "Remote (Tracking Branch)" : "HEAD (Last Commit)", true));
        comparePanel.add(createComparePanel(isUnpushed ? "HEAD (Local Commit)" : "Working Tree", false));

        mainPanel.add(comparePanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

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

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(new Color(95, 99, 104));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        titleLabel.setBackground(HEADER_BG_COLOR);
        titleLabel.setOpaque(true);
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextArea lineNumberArea = new JTextArea();
        lineNumberArea.setEditable(false);
        lineNumberArea.setFocusable(false);
        lineNumberArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        lineNumberArea.setBackground(new Color(245, 245, 245));
        lineNumberArea.setForeground(new Color(102, 102, 102));
        lineNumberArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR),
                BorderFactory.createEmptyBorder(10, 5, 10, 5)
        ));
        lineNumberArea.setPreferredSize(new Dimension(60, Integer.MAX_VALUE));

        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("Consolas", Font.PLAIN, 12));
        textPane.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(null);
        scrollPane.setRowHeaderView(lineNumberArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        if (isBefore) {
            beforeTextPane = textPane;
            beforeScrollPane = scrollPane;
            beforeLineNumbers = lineNumberArea;
        } else {
            afterTextPane = textPane;
            afterScrollPane = scrollPane;
            afterLineNumbers = lineNumberArea;
        }

        return panel;
    }

    private void setupScrollSync() {
        beforeScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (!isScrollSyncing) {
                isScrollSyncing = true;
                afterScrollPane.getVerticalScrollBar().setValue(e.getValue());
                isScrollSyncing = false;
            }
        });
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

        SwingWorker<DiffData, Void> worker = new SwingWorker<DiffData, Void>() {
            @Override
            protected DiffData doInBackground() {
                String diff;
                if (isUnpushed) {
                    diff = GitOperations.getUnpushedDiff(repoDir, filePath);
                } else if (isUntracked) {
                    diff = GitOperations.getUntrackedFileDiff(repoDir, filePath);
                } else {
                    diff = GitOperations.getWorkingTreeDiff(repoDir, filePath);
                }
                return parseDiff(diff);
            }

            @Override
            protected void done() {
                try {
                    DiffData data = get();
                    displayDiff(data);
                } catch (Exception e) {
                    beforeTextPane.setText("Error loading diff: " + e.getMessage());
                    afterTextPane.setText("Error loading diff: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    // ===== Diff parsing (same logic as FileDiffDialog) =====

    private enum LineType { ADDED, REMOVED, UNCHANGED, EMPTY }

    private static class DiffData {
        List<String> beforeLines = new ArrayList<>();
        List<String> afterLines = new ArrayList<>();
        List<LineType> beforeTypes = new ArrayList<>();
        List<LineType> afterTypes = new ArrayList<>();
        List<Integer> beforeLineNumbers = new ArrayList<>();
        List<Integer> afterLineNumbers = new ArrayList<>();
    }

    private DiffData parseDiff(String diff) {
        DiffData data = new DiffData();
        if (diff == null || diff.isEmpty()) {
            return data;
        }

        String[] lines = diff.split("\n");
        int currentBeforeLine = 0;
        int currentAfterLine = 0;
        boolean inHunk = false;

        List<String> pendingRemovals = new ArrayList<>();
        List<String> pendingAdditions = new ArrayList<>();
        List<Integer> pendingRemovalLineNums = new ArrayList<>();
        List<Integer> pendingAdditionLineNums = new ArrayList<>();

        for (String line : lines) {
            if (line.startsWith("@@")) {
                HunkInfo hunk = HunkInfo.parse(line);
                if (hunk != null) {
                    currentBeforeLine = hunk.oldStart;
                    currentAfterLine = hunk.newStart;
                    inHunk = true;
                }
                continue;
            }
            if (!inHunk) continue;
            if (line.startsWith("---") || line.startsWith("+++")) continue;
            if (line.startsWith("diff --git")) continue;

            if (line.startsWith("-")) {
                pendingRemovals.add(line.substring(1));
                pendingRemovalLineNums.add(currentBeforeLine);
                currentBeforeLine++;
            } else if (line.startsWith("+")) {
                pendingAdditions.add(line.substring(1));
                pendingAdditionLineNums.add(currentAfterLine);
                currentAfterLine++;
            } else {
                processPendingChanges(data, pendingRemovals, pendingAdditions,
                        pendingRemovalLineNums, pendingAdditionLineNums);
                String content = line.startsWith(" ") ? line.substring(1) : line;
                data.beforeLines.add(content);
                data.afterLines.add(content);
                data.beforeTypes.add(LineType.UNCHANGED);
                data.afterTypes.add(LineType.UNCHANGED);
                data.beforeLineNumbers.add(currentBeforeLine);
                data.afterLineNumbers.add(currentAfterLine);
                currentBeforeLine++;
                currentAfterLine++;
            }
        }
        processPendingChanges(data, pendingRemovals, pendingAdditions,
                pendingRemovalLineNums, pendingAdditionLineNums);
        return data;
    }

    private void processPendingChanges(DiffData data,
                                        List<String> pendingRemovals, List<String> pendingAdditions,
                                        List<Integer> removalLineNums, List<Integer> additionLineNums) {
        if (pendingRemovals.isEmpty() && pendingAdditions.isEmpty()) return;
        int maxLines = Math.max(pendingRemovals.size(), pendingAdditions.size());

        for (int i = 0; i < maxLines; i++) {
            if (i < pendingRemovals.size()) {
                data.beforeLines.add(pendingRemovals.get(i));
                data.beforeTypes.add(LineType.REMOVED);
                data.beforeLineNumbers.add(removalLineNums.get(i));
            } else {
                data.beforeLines.add("");
                data.beforeTypes.add(LineType.EMPTY);
                data.beforeLineNumbers.add(null);
            }
            if (i < pendingAdditions.size()) {
                data.afterLines.add(pendingAdditions.get(i));
                data.afterTypes.add(LineType.ADDED);
                data.afterLineNumbers.add(additionLineNums.get(i));
            } else {
                data.afterLines.add("");
                data.afterTypes.add(LineType.EMPTY);
                data.afterLineNumbers.add(null);
            }
        }
        pendingRemovals.clear();
        pendingAdditions.clear();
        removalLineNums.clear();
        additionLineNums.clear();
    }

    private void displayDiff(DiffData data) {
        if (data.beforeLines.isEmpty() && data.afterLines.isEmpty()) {
            beforeTextPane.setText("No changes.");
            afterTextPane.setText("No changes.");
            return;
        }
        displayColoredText(beforeTextPane, beforeLineNumbers,
                data.beforeLines, data.beforeTypes, data.beforeLineNumbers);
        displayColoredText(afterTextPane, afterLineNumbers,
                data.afterLines, data.afterTypes, data.afterLineNumbers);
    }

    private void displayColoredText(JTextPane textPane, JTextArea lineNumberArea,
                                     List<String> lines, List<LineType> types, List<Integer> lineNumbers) {
        StyledDocument doc = textPane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            for (int i = 0; i < lines.size(); i++) {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                switch (types.get(i)) {
                    case ADDED: StyleConstants.setBackground(attrs, ADDED_LINE_COLOR); break;
                    case REMOVED: StyleConstants.setBackground(attrs, REMOVED_LINE_COLOR); break;
                    case UNCHANGED: StyleConstants.setBackground(attrs, Color.WHITE); break;
                    case EMPTY: StyleConstants.setBackground(attrs, new Color(245, 245, 245)); break;
                }
                doc.insertString(doc.getLength(), lines.get(i) + "\n", attrs);
            }
            textPane.setCaretPosition(0);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        if (lineNumberArea != null && lineNumbers != null) {
            StringBuilder sb = new StringBuilder();
            for (Integer num : lineNumbers) {
                sb.append(num != null ? String.format("%5d", num) : "     ").append("\n");
            }
            lineNumberArea.setText(sb.toString());
            lineNumberArea.setCaretPosition(0);
        }
    }

    private static class HunkInfo {
        int oldStart, oldCount, newStart, newCount;

        static HunkInfo parse(String header) {
            try {
                Pattern pattern = Pattern.compile("@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@");
                Matcher matcher = pattern.matcher(header);
                if (matcher.find()) {
                    HunkInfo info = new HunkInfo();
                    info.oldStart = Integer.parseInt(matcher.group(1));
                    info.oldCount = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 1;
                    info.newStart = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 1;
                    info.newCount = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 1;
                    return info;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
