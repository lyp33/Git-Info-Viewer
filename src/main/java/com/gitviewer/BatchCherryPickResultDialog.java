package com.gitviewer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 批量 Cherry-Pick 结果对话框
 * 显示每个 commit URL 的处理结果
 */
public class BatchCherryPickResultDialog extends JDialog {

    private static final Color BORDER_COLOR = new Color(227, 233, 239);
    private static final Color HEADER_BG_COLOR = new Color(248, 249, 250);
    private static final Color ODD_ROW_COLOR = new Color(255, 255, 255);
    private static final Color EVEN_ROW_COLOR = new Color(245, 248, 250);

    private JTable resultTable;
    private DefaultTableModel tableModel;
    private List<BatchCherryPickDialog.CherryPickResultItem> resultList;
    private JTextArea logTextArea;
    private JLabel logLabel;

    public BatchCherryPickResultDialog(Frame parent, List<BatchCherryPickDialog.CherryPickResultItem> results) {
        super(parent, "Batch Cherry-Pick Results", true);
        this.resultList = results;
        initializeUI();
        loadResults();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(1200, 700);

        // 顶部：统计和表格
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        // 统计信息面板
        JPanel summaryPanel = createSummaryPanel();
        topPanel.add(summaryPanel);
        topPanel.add(Box.createVerticalStrut(10));

        // 结果表格面板
        JPanel tablePanel = createTablePanel();
        topPanel.add(tablePanel);

        add(topPanel, BorderLayout.CENTER);

        // 底部：日志显示区域
        JPanel logPanel = createLogPanel();
        add(logPanel, BorderLayout.SOUTH);

        // 设置默认字体
        Font defaultFont = new Font("Segoe UI", Font.PLAIN, 12);
        applyFontRecursive(this, defaultFont);
    }

    private JPanel createSummaryPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 0));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Summary",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(95, 99, 104)
        ));

        int total = resultList.size();
        int success = 0;
        int fail = 0;
        int toRunCmd = 0;

        for (BatchCherryPickDialog.CherryPickResultItem item : resultList) {
            switch (item.status) {
                case SUCCESS:
                    success++;
                    break;
                case FAIL:
                    fail++;
                    break;
                case TO_RUN_CMD:
                    toRunCmd++;
                    break;
            }
        }

        // Total
        JPanel totalPanel = createStatPanel("Total", String.valueOf(total), new Color(66, 133, 244));
        panel.add(totalPanel);

        // Success
        JPanel successPanel = createStatPanel("Success", String.valueOf(success), new Color(40, 167, 69));
        panel.add(successPanel);

        // Fail
        JPanel failPanel = createStatPanel("Fail / To Run CMD", String.valueOf(fail + toRunCmd), new Color(220, 53, 69));
        panel.add(failPanel);

        return panel;
    }

    private JPanel createStatPanel(String title, String value, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(color, 2));
        panel.setBackground(Color.WHITE);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        titleLabel.setForeground(new Color(95, 99, 104));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 0, 10));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(color);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Cherry-Pick Results",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(95, 99, 104)
        ));

        // 创建表格
        String[] columnNames = {
            "#", "Project Code", "Commit ID", "Status", "Type", "Target Branch", "Project Path", "Error Message"
        };

        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? EVEN_ROW_COLOR : ODD_ROW_COLOR);
                }
                return c;
            }

            @Override
            public String getToolTipText(MouseEvent e) {
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());

                if (row >= 0 && col >= 0) {
                    if (col == 7) { // Error Message column
                        Object value = getValueAt(row, col);
                        if (value != null && !value.toString().isEmpty()) {
                            return "<html>" + value.toString() + "</html>";
                        }
                    }
                    // Show full URL as tooltip
                    if (row >= 0 && row < resultList.size()) {
                        BatchCherryPickDialog.CherryPickResultItem item = resultList.get(row);
                        return "<html><b>URL:</b> " + item.url + "</html>";
                    }
                }
                return super.getToolTipText(e);
            }
        };

        resultTable.setAutoCreateRowSorter(true);
        resultTable.setRowHeight(28);
        resultTable.setIntercellSpacing(new Dimension(0, 0));
        resultTable.setShowGrid(false);
        resultTable.setSelectionBackground(new Color(187, 222, 251));
        resultTable.setSelectionForeground(new Color(0, 0, 0));
        resultTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));

        // 表头样式
        JTableHeader header = resultTable.getTableHeader();
        header.setBackground(HEADER_BG_COLOR);
        header.setForeground(new Color(95, 99, 104));
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 35));

        // 设置列宽
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(40);   // #
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(120);  // Project Code
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(80);   // Commit ID
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(100);  // Status
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(100);  // Type
        resultTable.getColumnModel().getColumn(5).setPreferredWidth(100);  // Target Branch
        resultTable.getColumnModel().getColumn(6).setPreferredWidth(250);  // Project Path
        resultTable.getColumnModel().getColumn(7).setPreferredWidth(200);  // Error Message

        // 设置Status列的渲染器
        resultTable.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());

        // 添加表格选择监听器
        resultTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = resultTable.getSelectedRow();
                if (selectedRow >= 0) {
                    // 转换排序后的行索引到模型索引
                    int modelRow = resultTable.convertRowIndexToModel(selectedRow);
                    showLogsForItem(modelRow);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                "Detailed Logs",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(95, 99, 104)
        ));
        panel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));

        logLabel = new JLabel("Select a row to view logs");
        logLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        logLabel.setForeground(new Color(95, 99, 104));
        logLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        logTextArea = new JTextArea();
        logTextArea.setEditable(false);
        logTextArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logTextArea.setBackground(new Color(248, 249, 250));
        logTextArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(logTextArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));

        panel.add(logLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void showLogsForItem(int modelRow) {
        if (modelRow < 0 || modelRow >= resultList.size()) {
            return;
        }

        BatchCherryPickDialog.CherryPickResultItem item = resultList.get(modelRow);

        // 更新标签
        logLabel.setText(String.format("Logs for #%d - %s", item.index, item.projectCode));

        // 显示日志
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("========================================\n");
        logBuilder.append("Processing URL: ").append(item.index).append("\n");
        logBuilder.append("========================================\n\n");
        logBuilder.append("URL: ").append(item.url).append("\n");
        logBuilder.append("Project Code: ").append(item.projectCode != null ? item.projectCode : "N/A").append("\n");
        logBuilder.append("Commit ID: ").append(item.commitId != null ? item.commitId : "N/A").append("\n");
        logBuilder.append("Target Branch: ").append(item.targetBranch).append("\n");
        logBuilder.append("Status: ").append(item.status.getDisplayName()).append("\n");
        logBuilder.append("\n========================================\n");
        logBuilder.append("Detailed Logs:\n");
        logBuilder.append("========================================\n\n");

        if (item.logs != null && !item.logs.isEmpty()) {
            for (String log : item.logs) {
                logBuilder.append(log).append("\n");
            }
        } else {
            logBuilder.append("(No logs available)\n");
        }

        // 如果有生成的命令，高亮显示
        if (item.generatedCommands != null && !item.generatedCommands.isEmpty()) {
            logBuilder.append("\n========================================\n");
            logBuilder.append("[GENERATED CMD COMMANDS - RUN MANUALLY]\n");
            logBuilder.append("========================================\n\n");
            for (String cmd : item.generatedCommands) {
                if (cmd.startsWith("::")) {
                    logBuilder.append(">>> ").append(cmd).append(" <<<\n");
                } else {
                    logBuilder.append("    ").append(cmd).append("\n");
                }
            }
            logBuilder.append("\n========================================\n");
        }

        // 如果有错误信息，高亮显示
        if (item.errorMessage != null) {
            logBuilder.append("\n========================================\n");
            logBuilder.append("[ERROR MESSAGE]\n");
            logBuilder.append("========================================\n");
            logBuilder.append(item.errorMessage).append("\n");
            logBuilder.append("========================================\n");
        }

        logTextArea.setText(logBuilder.toString());
        logTextArea.setCaretPosition(0);
    }

    private void loadResults() {
        tableModel.setRowCount(0);

        for (BatchCherryPickDialog.CherryPickResultItem item : resultList) {
            Object[] row = {
                item.index,
                item.projectCode != null ? item.projectCode : "N/A",
                item.commitId != null ? item.commitId.substring(0, Math.min(8, item.commitId.length())) : "N/A",
                item.status,
                item.isSameProject ? "Same Project" : "Cross-Project",
                item.targetBranch,
                item.projectPath != null ? item.projectPath : "N/A",
                item.errorMessage != null ? item.errorMessage : ""
            };
            tableModel.addRow(row);
        }
    }

    private void exportToCsv() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setSelectedFile(new java.io.File("batch-cherry-pick-results.csv"));

            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();

                try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                    // 写入CSV头部
                    writer.println("Index,URL,Project Code,Commit ID,Status,Type,Target Branch,Project Path,Error Message");

                    // 写入数据
                    for (BatchCherryPickDialog.CherryPickResultItem item : resultList) {
                        writer.println(
                            item.index + "," +
                            escapeCsv(item.url) + "," +
                            escapeCsv(item.projectCode != null ? item.projectCode : "N/A") + "," +
                            escapeCsv(item.commitId != null ? item.commitId : "N/A") + "," +
                            escapeCsv(item.status.getDisplayName()) + "," +
                            escapeCsv(item.isSameProject ? "Same Project" : "Cross-Project") + "," +
                            escapeCsv(item.targetBranch) + "," +
                            escapeCsv(item.projectPath != null ? item.projectPath : "N/A") + "," +
                            escapeCsv(item.errorMessage != null ? item.errorMessage : "")
                        );
                    }
                }

                JOptionPane.showMessageDialog(this,
                        "Results exported to: " + file.getAbsolutePath(),
                        "Export Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Failed to export: " + e.getMessage(),
                    "Export Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 状态列的自定义渲染器
     */
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof BatchCherryPickDialog.CherryPickStatus) {
                BatchCherryPickDialog.CherryPickStatus status = (BatchCherryPickDialog.CherryPickStatus) value;
                setText(status.getDisplayName());
                setForeground(status.getColor());
                setFont(getFont().deriveFont(Font.BOLD));
            }

            return c;
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
}
