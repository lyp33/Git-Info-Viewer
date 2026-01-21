package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 部署Pod列表对话框
 * Deployment Pod List Dialog
 */
public class DeploymentPodListDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(DeploymentPodListDialog.class);
    
    // UI Components
    private JTable podTable;
    private PodTableModel tableModel;
    private JButton refreshButton;
    private JButton closeButton;
    
    // Data
    private PortalApiClient apiClient;
    private String workspace;
    private String environment;
    private String workspaceToken;
    
    // SwingWorker reference
    private SwingWorker<?, ?> currentWorker;
    
    /**
     * 构造函数
     * 
     * @param parent 父窗口
     * @param apiClient Portal API客户端
     * @param workspace 工作空间
     * @param environment 环境
     * @param workspaceToken 工作空间Token
     */
    public DeploymentPodListDialog(Frame parent, PortalApiClient apiClient, String workspace, 
                                   String environment, String workspaceToken) {
        super(parent, "Deployment Pods - " + workspace + " / " + environment, true);
        logger.info("Opening Deployment Pod List Dialog");
        
        this.apiClient = apiClient;
        this.workspace = workspace;
        this.environment = environment;
        this.workspaceToken = workspaceToken;
        this.tableModel = new PodTableModel();
        
        initializeUI();
        loadPods();
        
        setSize(1000, 600);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(parent);
    }
    
    /**
     * 初始化UI
     * Initialize UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);
        
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);
        
        // 标题
        JLabel titleLabel = new JLabel("Deployment Pods");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(60, 64, 67));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 表格
        mainPanel.add(createTablePanel(), BorderLayout.CENTER);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // 按钮面板
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * 创建表格面板
     * Create table panel
     */
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // 创建表格
        podTable = new JTable(tableModel);
        podTable.setRowHeight(32);
        podTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        podTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        podTable.getTableHeader().setBackground(new Color(248, 249, 250));
        podTable.getTableHeader().setForeground(new Color(60, 64, 67));
        podTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        podTable.setShowGrid(true);
        podTable.setGridColor(new Color(240, 240, 240));
        podTable.setSelectionBackground(new Color(66, 133, 244, 50));
        podTable.setSelectionForeground(Color.BLACK);
        
        // 设置列宽
        int[] widths = {300, 150, 200, 150, 150, 400};  // 添加Image列宽度400
        for (int i = 0; i < widths.length && i < podTable.getColumnCount(); i++) {
            TableColumn column = podTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(widths[i]);
        }
        
        // 为Image列（第5列）设置自定义渲染器，支持换行显示
        TableColumn imageColumn = podTable.getColumnModel().getColumn(5);
        imageColumn.setCellRenderer(new MultiLineTableCellRenderer());
        
        // 双击查看日志
        podTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = podTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        handleViewLogs(row);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(podTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(218, 220, 224), 1));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建按钮面板
     * Create button panel
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(Color.WHITE);
        
        // Refresh 按钮
        refreshButton = new JButton("<html><font color='white'><b>Refresh</b></font></html>");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        refreshButton.setPreferredSize(new Dimension(100, 35));
        refreshButton.setBackground(new Color(66, 133, 244));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setOpaque(true);
        refreshButton.setContentAreaFilled(true);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> loadPods());
        panel.add(refreshButton);
        
        // Close 按钮
        closeButton = new JButton("<html><font color='white'><b>Close</b></font></html>");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        closeButton.setPreferredSize(new Dimension(100, 35));
        closeButton.setBackground(new Color(95, 99, 104));
        closeButton.setForeground(Color.WHITE);
        closeButton.setOpaque(true);
        closeButton.setContentAreaFilled(true);
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);
        
        return panel;
    }
    
    /**
     * 加载Pod列表
     * Load pod list
     */
    private void loadPods() {
        logger.info("Loading pods for workspace: {}, environment: {}", workspace, environment);
        
        // 禁用按钮
        refreshButton.setEnabled(false);
        refreshButton.setText("<html><font color='white'><b>Loading...</b></font></html>");
        
        // 异步加载
        SwingWorker<List<DeploymentPod>, Void> worker = new SwingWorker<List<DeploymentPod>, Void>() {
            @Override
            protected List<DeploymentPod> doInBackground() throws Exception {
                return apiClient.getDeploymentPods(workspace, environment, workspaceToken, null);
            }
            
            @Override
            protected void done() {
                // 重新启用按钮
                refreshButton.setEnabled(true);
                refreshButton.setText("<html><font color='white'><b>Refresh</b></font></html>");
                
                try {
                    List<DeploymentPod> pods = get();
                    tableModel.setPods(pods);
                    logger.info("Loaded {} pods", pods.size());
                    
                    if (pods.isEmpty()) {
                        JOptionPane.showMessageDialog(DeploymentPodListDialog.this,
                            "No pods found for this workspace and environment.",
                            "No Pods",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    logger.error("Failed to load pods", e);
                    JOptionPane.showMessageDialog(DeploymentPodListDialog.this,
                        "Failed to load pods:\n" + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        currentWorker = worker;
        worker.execute();
    }
    
    /**
     * 处理查看日志
     * Handle view logs
     * 
     * @param row 表格行索引
     */
    private void handleViewLogs(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }
        
        DeploymentPod pod = tableModel.getPodAt(row);
        logger.info("Opening logs for pod: {}", pod.getName());
        
        try {
            DeploymentPodLogDialog logDialog = new DeploymentPodLogDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                apiClient,
                workspace,
                environment,
                workspaceToken,
                pod
            );
            logDialog.setVisible(true);
        } catch (Exception e) {
            logger.error("Failed to open log dialog", e);
            JOptionPane.showMessageDialog(this,
                "Failed to open log dialog:\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 资源清理
     * Resource cleanup
     */
    @Override
    public void dispose() {
        logger.info("Disposing Deployment Pod List Dialog");
        
        // 取消当前运行的worker
        if (currentWorker != null && !currentWorker.isDone()) {
            logger.info("Cancelling current worker");
            currentWorker.cancel(true);
        }
        
        super.dispose();
    }
    
    /**
     * Pod表格模型
     * Pod table model
     */
    private static class PodTableModel extends AbstractTableModel {
        private List<DeploymentPod> pods;
        private String[] columnNames = {"Name", "Namespace", "Creation Time", "App", "Status", "Image"};
        
        public PodTableModel() {
            this.pods = new ArrayList<>();
        }
        
        public void setPods(List<DeploymentPod> pods) {
            this.pods = pods != null ? new ArrayList<>(pods) : new ArrayList<>();
            fireTableDataChanged();
        }
        
        public DeploymentPod getPodAt(int row) {
            if (row >= 0 && row < pods.size()) {
                return pods.get(row);
            }
            return null;
        }
        
        @Override
        public int getRowCount() {
            return pods.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            if (row < 0 || row >= pods.size()) {
                return "";
            }
            
            DeploymentPod pod = pods.get(row);
            
            switch (column) {
                case 0:
                    return pod.getName();
                case 1:
                    return pod.getNamespace();
                case 2:
                    return pod.getCreationTimestamp();
                case 3:
                    return pod.getApp();
                case 4:
                    return pod.getRealStatus();
                case 5:
                    return pod.getImage();
                default:
                    return "";
            }
        }
    }
    
    /**
     * 多行文本单元格渲染器
     * Multi-line table cell renderer for wrapping long text
     */
    private static class MultiLineTableCellRenderer extends JTextArea implements javax.swing.table.TableCellRenderer {
        
        public MultiLineTableCellRenderer() {
            setLineWrap(true);
            setWrapStyleWord(true);
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.PLAIN, 11));
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                      boolean isSelected, boolean hasFocus,
                                                      int row, int column) {
            setText(value != null ? value.toString() : "");
            
            if (isSelected) {
                setBackground(new Color(66, 133, 244, 50));
                setForeground(Color.BLACK);
            } else {
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);
            }
            
            // 设置边框
            if (hasFocus) {
                setBorder(BorderFactory.createLineBorder(new Color(66, 133, 244), 1));
            } else {
                setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
            }
            
            // 根据内容自动调整行高
            setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
            if (table.getRowHeight(row) < getPreferredSize().height) {
                table.setRowHeight(row, getPreferredSize().height);
            }
            
            return this;
        }
    }
}
