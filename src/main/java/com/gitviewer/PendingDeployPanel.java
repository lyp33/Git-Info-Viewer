package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PendingDeployPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(PendingDeployPanel.class);
    private static final Color BG_COLOR = new Color(248, 249, 250);
    private static final Color BORDER_COLOR = new Color(218, 220, 224);
    private static final Color STATUS_PENDING_COLOR = new Color(120, 120, 120);
    private static final Color STATUS_BUILDING_COLOR = new Color(70, 130, 180);
    private static final Color STATUS_SUCCESS_COLOR = new Color(52, 168, 83);
    private static final Color STATUS_FAILED_COLOR = new Color(234, 67, 53);
    private static final Color STATUS_CANCELLED_COLOR = new Color(160, 160, 160);

    private JCheckBox autoPollingCheckbox;
    private JTextField pollingIntervalField;
    private JLabel refreshLink;
    private JLabel errorBanner;
    private JPanel listPanel;
    private JLabel emptyLabel;

    private BuildQueue queue;
    private final List<EntryRow> entryRows = new ArrayList<>();
    private javax.swing.Timer animationTimer;
    private int animationDotCount = 0;

    public PendingDeployPanel(BuildQueue queue) {
        this.queue = queue;
        setLayout(new BorderLayout(0, 4));
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(10, 10, 10, 10)));
        setPreferredSize(new Dimension(420, 0));
        add(buildTopBar(), BorderLayout.NORTH);
        add(buildCenterArea(), BorderLayout.CENTER);
        startAnimationTimer();
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBackground(BG_COLOR);

        JLabel title = new JLabel("Pending Deploy Queue");
        title.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        title.setForeground(new Color(60, 64, 67));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.add(title);
        bar.add(Box.createVerticalStrut(8));

        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controlRow.setBackground(BG_COLOR);
        controlRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        autoPollingCheckbox = new JCheckBox("Auto Polling");
        autoPollingCheckbox.setSelected(true);
        autoPollingCheckbox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        autoPollingCheckbox.setBackground(BG_COLOR);
        autoPollingCheckbox.addActionListener(e -> handleAutoPollingToggle());
        controlRow.add(autoPollingCheckbox);

        pollingIntervalField = new JTextField("20", 4);
        pollingIntervalField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        pollingIntervalField.setToolTipText("Polling interval (seconds, min 5)");
        pollingIntervalField.addActionListener(e -> handlePollingIntervalChange());
        pollingIntervalField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { handlePollingIntervalChange(); }
        });
        controlRow.add(new JLabel("s"));
        controlRow.add(pollingIntervalField);

        refreshLink = new JLabel("<html><u>Refresh</u></html>");
        refreshLink.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        refreshLink.setForeground(new Color(70, 130, 180));
        refreshLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { handleManualRefresh(); }
        });
        controlRow.add(refreshLink);
        bar.add(controlRow);

        errorBanner = new JLabel();
        errorBanner.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        errorBanner.setForeground(new Color(234, 67, 53));
        errorBanner.setOpaque(true);
        errorBanner.setBackground(new Color(255, 235, 238));
        errorBanner.setBorder(new EmptyBorder(4, 8, 4, 8));
        errorBanner.setVisible(false);
        errorBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.add(Box.createVerticalStrut(4));
        bar.add(errorBanner);
        return bar;
    }

    private JScrollPane buildCenterArea() {
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        emptyLabel = new JLabel("No pending builds");
        emptyLabel.setFont(new Font("Microsoft YaHei UI", Font.ITALIC, 12));
        emptyLabel.setForeground(new Color(150, 150, 150));
        emptyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyLabel.setBorder(new EmptyBorder(20, 0, 0, 0));
        listPanel.add(emptyLabel);

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    public void loadEntries(List<QueueEntry> entries) {
        clearEntries();
        for (QueueEntry entry : entries) addEntryRow(entry);
        updateEmptyState();
        revalidate(); repaint();
    }

    public void refreshEntry(QueueEntry entry) {
        for (EntryRow row : entryRows) {
            if (row.entry == entry || row.entry.getGroupName().equals(entry.getGroupName())) {
                // 同步运行时状态（appBuildStatuses 不持久化，需从回调传入的对象复制）
                if (row.entry != entry) {
                    row.entry.setStatus(entry.getStatus());
                    row.entry.setTriggeredAt(entry.getTriggeredAt());
                    row.entry.setAppBuildStatuses(new java.util.HashMap<>(entry.getAppBuildStatuses()));
                }
                row.update();
                break;
            }
        }
    }

    public void clearEntries() {
        entryRows.clear();
        listPanel.removeAll();
        listPanel.add(emptyLabel);
        updateEmptyState();
        revalidate(); repaint();
    }

    public void setQueue(BuildQueue queue) { this.queue = queue; }

    public void showError(String message) {
        errorBanner.setText("<html><b>Error:</b> " + message + "</html>");
        errorBanner.setVisible(true);
        revalidate(); repaint();
    }

    public void hideError() { errorBanner.setVisible(false); revalidate(); repaint(); }

    public int getPollingIntervalSeconds() {
        try { return Math.max(5, Integer.parseInt(pollingIntervalField.getText().trim())); }
        catch (NumberFormatException e) { return QueuePersistence.DEFAULT_POLLING_INTERVAL; }
    }

    public void setPollingIntervalSeconds(int seconds) {
        pollingIntervalField.setText(String.valueOf(Math.max(5, seconds)));
    }

    private void addEntryRow(QueueEntry entry) {
        emptyLabel.setVisible(false);
        EntryRow row = new EntryRow(entry);
        entryRows.add(row);
        listPanel.add(row.panel);
        listPanel.add(Box.createVerticalStrut(4));
    }

    private void updateEmptyState() {
        if (entryRows.isEmpty()) {
            listPanel.removeAll();
            listPanel.add(emptyLabel);
            emptyLabel.setVisible(true);
        } else {
            emptyLabel.setVisible(false);
        }
    }

    private void handleAutoPollingToggle() {
        if (queue == null) return;
        if (autoPollingCheckbox.isSelected()) queue.resumeAutoPolling();
        else queue.pausePolling();
    }

    private void handlePollingIntervalChange() {
        if (queue == null) return;
        int seconds = getPollingIntervalSeconds();
        pollingIntervalField.setText(String.valueOf(seconds));
        queue.setPollingInterval(seconds);
        QueuePersistence.save(queue.getEntries(), seconds);
    }

    private void handleManualRefresh() {
        hideError();
        if (queue != null) queue.manualRefresh();
    }

    private void startAnimationTimer() {
        animationTimer = new javax.swing.Timer(500, e -> {
            animationDotCount = (animationDotCount + 1) % 4;
            for (EntryRow row : entryRows) {
                if (row.entry.getStatus() == QueueEntry.QueueStatus.BUILDING) row.updateStatusLabel();
            }
        });
        animationTimer.setRepeats(true);
        animationTimer.start();
    }

    private class EntryRow {
        final QueueEntry entry;
        final JPanel panel;
        private JLabel statusLabel;
        private JPanel infoPanel;

        EntryRow(QueueEntry entry) {
            this.entry = entry;
            this.panel = buildPanel();
        }

        private JPanel buildPanel() {
            JPanel p = new JPanel(new BorderLayout(6, 2));
            p.setBackground(Color.WHITE);
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COLOR, 1),
                    new EmptyBorder(6, 8, 6, 8)));
            p.setAlignmentX(Component.LEFT_ALIGNMENT);

            infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setBackground(Color.WHITE);

            JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            nameRow.setBackground(Color.WHITE);

            JLabel nameLabel = new JLabel(entry.getGroupName());
            nameLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
            nameLabel.setForeground(new Color(60, 64, 67));
            nameRow.add(nameLabel);

            JLabel countLabel = new JLabel("(" + entry.getAppNames().size() + " apps)");
            countLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
            countLabel.setForeground(new Color(120, 120, 120));
            nameRow.add(countLabel);

            statusLabel = new JLabel();
            statusLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 11));
            nameRow.add(statusLabel);
            updateStatusLabel();
            infoPanel.add(nameRow);

            JLabel detailLabel = new JLabel("branch: " + entry.getBranch() + "  ver: " + entry.getVersion());
            detailLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 10));
            detailLabel.setForeground(new Color(150, 150, 150));
            infoPanel.add(detailLabel);

            // 初始渲染 app 子行
            refreshAppStatusRows();

            p.add(infoPanel, BorderLayout.CENTER);

            JButton deleteBtn = new JButton("x");
            deleteBtn.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
            deleteBtn.setForeground(new Color(150, 150, 150));
            deleteBtn.setBackground(Color.WHITE);
            deleteBtn.setBorderPainted(false);
            deleteBtn.setContentAreaFilled(false);
            deleteBtn.setFocusPainted(false);
            deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            deleteBtn.setToolTipText("Remove this entry");
            deleteBtn.addActionListener(e -> handleDelete());
            p.add(deleteBtn, BorderLayout.EAST);
            return p;
        }

        void updateStatusLabel() {
            QueueEntry.QueueStatus status = entry.getStatus();
            String text; Color color;
            switch (status) {
                case PENDING:   text = "[Pending]";   color = STATUS_PENDING_COLOR;   break;
                case BUILDING:
                    String dots = ".".repeat(animationDotCount);
                    text = "[Building" + dots + "]"; color = STATUS_BUILDING_COLOR;  break;
                case SUCCESS:   text = "[Success]";   color = STATUS_SUCCESS_COLOR;   break;
                case FAILED:    text = "[Failed]";    color = STATUS_FAILED_COLOR;    break;
                case CANCELLED: text = "[Cancelled]"; color = STATUS_CANCELLED_COLOR; break;
                default:        text = "[" + status.name() + "]"; color = STATUS_PENDING_COLOR;
            }
            statusLabel.setText(text);
            statusLabel.setForeground(color);
        }

        private void refreshAppStatusRows() {
            // 移除旧的 app 子行（保留前两行：nameRow 和 detailLabel）
            while (infoPanel.getComponentCount() > 2) infoPanel.remove(2);
            Map<String, String> appStatuses = entry.getAppBuildStatuses();
            QueueEntry.QueueStatus status = entry.getStatus();
            for (String appName : entry.getAppNames()) {
                String buildStatus = appStatuses.get(appName);
                String displayText;
                Color color;
                if (buildStatus != null && !buildStatus.isEmpty()) {
                    displayText = "  • " + appName + ": " + buildStatus;
                    color = statusColorFor(buildStatus);
                } else {
                    // 还没有轮询结果，根据 group 状态推断
                    switch (status) {
                        case BUILDING:
                            displayText = "  • " + appName + ": waiting...";
                            color = STATUS_BUILDING_COLOR;
                            break;
                        case SUCCESS:
                            displayText = "  • " + appName + ": SUCCESS";
                            color = STATUS_SUCCESS_COLOR;
                            break;
                        case FAILED:
                        case CANCELLED:
                            displayText = "  • " + appName + ": -";
                            color = STATUS_CANCELLED_COLOR;
                            break;
                        default: // PENDING
                            displayText = "  • " + appName;
                            color = STATUS_PENDING_COLOR;
                    }
                }
                JLabel appLabel = new JLabel(displayText);
                appLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
                appLabel.setForeground(color);
                infoPanel.add(appLabel);
            }
        }

        private Color statusColorFor(String buildStatus) {
            if (buildStatus == null) return STATUS_BUILDING_COLOR;
            String t = buildStatus.trim().toLowerCase();
            if (t.equals("success") || t.equals("build success")) return STATUS_SUCCESS_COLOR;
            if (t.equals("failure") || t.equals("failed") || t.equals("build fail")
                    || t.equals("build failed") || t.equals("aborted")) return STATUS_FAILED_COLOR;
            return STATUS_BUILDING_COLOR;
        }

        void update() {
            updateStatusLabel();
            refreshAppStatusRows();
            panel.revalidate();
            panel.repaint();
            listPanel.revalidate();
            listPanel.repaint();
        }

        private void handleDelete() {
            entryRows.remove(this);
            listPanel.remove(panel);
            updateEmptyState();
            listPanel.revalidate(); listPanel.repaint();
            List<QueueEntry> remaining = new ArrayList<>();
            for (EntryRow r : entryRows) remaining.add(r.entry);
            QueuePersistence.save(remaining, getPollingIntervalSeconds());
            logger.info("Entry removed from panel: {}", entry.getGroupName());
        }
    }
}
