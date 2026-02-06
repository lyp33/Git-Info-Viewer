package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * Tenant CI/CD主对话框
 * Main UI dialog for Tenant CI/CD feature
 */
public class TenantCICDDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(TenantCICDDialog.class);
    
    // Connection Panel组件
    private JComboBox<String> tenantComboBox;
    private JLabel statusLabel;
    
    // Query Panel组件
    private JTextField planNameField;
    private JComboBox<String> appNameComboBox;
    private JTextField creatorField;
    private JTextField pageSizeField;
    private JButton searchButton;
    private JLabel cancelSearchLink;  // 取消搜索链接
    private JButton cancelSearchButton;  // 新增：取消搜索按钮
    
    // Results Panel组件
    private JTable resultsTable;
    private BuildResultTableModel tableModel;
    private JScrollPane tableScrollPane;
    
    // Action Buttons
    private JButton downloadCsvButton;
    private JButton copyImageNamesButton;
    private JButton buildButton;
    private JButton deployButton;
    
    // Loading Indicator
    private JLabel loadingLabel;
    private JProgressBar loadingProgressBar;
    
    // API Client和数据
    private PortalApiClient apiClient;
    private String currentToken;
    private String currentTenant;
    private List<String> allAppNames;  // 缓存用于过滤
    private List<String> filteredAppNames;  // 过滤后的应用名称
    private List<BuildResult> allResults;  // 存储所有查询结果
    private String currentBranchFilter;  // 当前的分支过滤器
    
    // SwingWorker引用，用于取消操作
    private SwingWorker<?, ?> currentWorker;
    
    // 防止KeyListener递归调用的标志
    private boolean isUpdatingComboBox = false;
    
    // KeyListener引用，用于清理
    private KeyAdapter appNameKeyListener;
    
    // 防抖Timer，用于延迟过滤
    private javax.swing.Timer filterTimer;
    
    // Hover tooltip相关
    private javax.swing.Timer hoverTimer;
    private int lastHoverRow = -1;
    
    // Auto Refresh相关
    private JCheckBox autoRefreshCheckBox;
    private JTextField refreshIntervalField;
    private javax.swing.Timer autoRefreshTimer;
    private boolean isSearching = false;  // 标记是否正在搜索中
    
    /**
     * 构造函数
     * 
     * @param parent 父窗口
     */
    public TenantCICDDialog(Frame parent) {
        super(parent, "Tenant CI/CD", true);
        logger.info("Opening Tenant CI/CD Dialog");
        
        this.apiClient = new PortalApiClient();
        this.allAppNames = new ArrayList<>();
        this.filteredAppNames = new ArrayList<>();
        this.allResults = new ArrayList<>();
        this.currentBranchFilter = null;
        this.tableModel = new BuildResultTableModel();
        
        initializeUI();
        loadPortalSettings();
        
        setSize(1200, 700);
        setLocationRelativeTo(parent);
    }
    
    /**
     * 初始化UI
     * Initialize UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);
        
        // 设置对话框默认字体为 Segoe UI 11
        Font defaultFont = new Font("Segoe UI", Font.PLAIN, 11);
        
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setFont(defaultFont);
        mainPanel.setBackground(Color.WHITE);
        
        // 创建顶部面板（Connection + Query）
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(createConnectionPanel(), BorderLayout.NORTH);
        topPanel.add(createQueryPanel(), BorderLayout.CENTER);
        
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(createResultsPanel(), BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // 应用字体到所有组件
        applyFontRecursive(this, defaultFont);
        
        // 初始状态：禁用查询和操作按钮
        updateUIState(false);
    }
    
    /**
     * 递归应用字体到所有组件
     * Apply font recursively to all components
     */
    private void applyFontRecursive(Container container, Font font) {
        for (Component comp : container.getComponents()) {
            // 跳过按钮，因为按钮有自己的字体设置
            if (!(comp instanceof JButton)) {
                comp.setFont(font);
            }
            if (comp instanceof Container) {
                applyFontRecursive((Container) comp, font);
            }
        }
    }
    
    /**
     * 创建连接面板
     * Create connection panel
     */
    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        panel.setBackground(Color.WHITE);
        
        // 使用简单的底部边框线代替 TitledBorder
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(218, 220, 224)),
            BorderFactory.createEmptyBorder(5, 10, 10, 10)
        ));
        
        JLabel tenantLabel = new JLabel("Tenant:");
        tenantLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tenantLabel.setForeground(new Color(60, 64, 67));
        panel.add(tenantLabel);
        
        tenantComboBox = new JComboBox<>();
        tenantComboBox.setPreferredSize(new Dimension(200, 32));
        tenantComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        // 添加租户选择监听器，自动连接
        tenantComboBox.addActionListener(e -> {
            String selected = (String) tenantComboBox.getSelectedItem();
            // 只有当选择的不是 "Please select" 时才自动连接
            if (selected != null && !selected.equals("Please select") && !selected.trim().isEmpty()) {
                handleConnect();
            }
        });
        panel.add(tenantComboBox);
        
        statusLabel = new JLabel("Please select a tenant");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(95, 99, 104));
        panel.add(statusLabel);
        
        // Build Image 按钮 - 紫色
        buildButton = new JButton("<html><font color='white'><b>Build Image</b></font></html>");
        buildButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        buildButton.setPreferredSize(new Dimension(120, 32));
        buildButton.setBackground(new Color(142, 68, 173));
        buildButton.setForeground(Color.WHITE);
        buildButton.setOpaque(true);
        buildButton.setContentAreaFilled(true);
        buildButton.setFocusPainted(false);
        buildButton.setBorderPainted(false);
        buildButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buildButton.addActionListener(e -> handleBuild());
        panel.add(buildButton);
        
        // Deployment 按钮 - 深绿色
        deployButton = new JButton("<html><font color='white'><b>Deployment</b></font></html>");
        deployButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        deployButton.setPreferredSize(new Dimension(120, 32));
        deployButton.setBackground(new Color(34, 139, 34));
        deployButton.setForeground(Color.WHITE);
        deployButton.setOpaque(true);
        deployButton.setContentAreaFilled(true);
        deployButton.setFocusPainted(false);
        deployButton.setBorderPainted(false);
        deployButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deployButton.addActionListener(e -> handleDeployment());
        panel.add(deployButton);
        
        // Close 按钮 - 灰色
        JButton closeButton = new JButton("<html><font color='white'><b>Close</b></font></html>");
        closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        closeButton.setPreferredSize(new Dimension(100, 32));
        closeButton.setBackground(new Color(95, 99, 104));
        closeButton.setForeground(Color.WHITE);
        closeButton.setOpaque(true);
        closeButton.setContentAreaFilled(true);
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);
        
        // Loading indicator
        loadingLabel = new JLabel("Loading...");
        loadingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        loadingLabel.setForeground(new Color(66, 133, 244));
        loadingLabel.setVisible(false);
        panel.add(loadingLabel);
        
        loadingProgressBar = new JProgressBar();
        loadingProgressBar.setIndeterminate(true);
        loadingProgressBar.setPreferredSize(new Dimension(100, 20));
        loadingProgressBar.setVisible(false);
        panel.add(loadingProgressBar);
        
        return panel;
    }
    
    /**
     * 创建查询面板
     * Create query panel
     */
    private JPanel createQueryPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        
        // 使用简单的底部边框线代替 TitledBorder
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(218, 220, 224)),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Plan Name
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Version Name:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        planNameField = new JTextField(20);
        planNameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        planNameField.setPreferredSize(new Dimension(200, 28));
        planNameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        panel.add(planNameField, gbc);
        
        // App Name
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("App Name:"), gbc);
        
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        appNameComboBox = new JComboBox<>();
        appNameComboBox.setEditable(true);
        appNameComboBox.setPreferredSize(new Dimension(200, 25));
        setupAppNameFiltering();
        panel.add(appNameComboBox, gbc);
        
        // Creator
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Creator:"), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        creatorField = new JTextField(20);
        creatorField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        creatorField.setPreferredSize(new Dimension(200, 28));
        creatorField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        panel.add(creatorField, gbc);
        
        // Page Size
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        panel.add(new JLabel("Page Size:"), gbc);
        
        gbc.gridx = 3;
        gbc.weightx = 0.3;
        pageSizeField = new JTextField("10", 10);
        pageSizeField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pageSizeField.setPreferredSize(new Dimension(80, 28));
        pageSizeField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        panel.add(pageSizeField, gbc);
        
        // Search Button
        gbc.gridx = 4;
        gbc.weightx = 0.0;
        searchButton = new JButton("<html><font color='white'><b>Search</b></font></html>");
        searchButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchButton.setPreferredSize(new Dimension(90, 28));
        searchButton.setBackground(new Color(66, 133, 244));
        searchButton.setForeground(Color.WHITE);
        searchButton.setOpaque(true);
        searchButton.setContentAreaFilled(true);
        searchButton.setFocusPainted(false);
        searchButton.setBorderPainted(false);
        searchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        searchButton.addActionListener(e -> handleSearch());
        panel.add(searchButton, gbc);
        
        // Cancel Search Link (initially hidden)
        gbc.gridx = 5;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(5, 10, 5, 5);
        cancelSearchLink = new JLabel("<html><u>Cancel</u></html>");
        cancelSearchLink.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelSearchLink.setForeground(new Color(220, 53, 69));  // 红色
        cancelSearchLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelSearchLink.setVisible(false);  // 默认隐藏
        cancelSearchLink.setToolTipText("Cancel current search");
        cancelSearchLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                handleCancelSearch();
            }
            
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                cancelSearchLink.setForeground(new Color(200, 35, 51));  // 深红色
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                cancelSearchLink.setForeground(new Color(220, 53, 69));  // 恢复原色
            }
        });
        panel.add(cancelSearchLink, gbc);
        
        return panel;
    }
    
    /**
     * 创建结果面板
     * Create results panel
     */
    private JPanel createResultsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        
        // 使用简单的边距，不使用 TitledBorder
        panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
        // 创建表格
        resultsTable = new JTable(tableModel);
        resultsTable.setAutoCreateRowSorter(true);
        resultsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultsTable.setRowHeight(28);
        resultsTable.getTableHeader().setReorderingAllowed(false);
        resultsTable.setShowGrid(true);
        resultsTable.setGridColor(new Color(240, 240, 240));
        resultsTable.setBackground(Color.WHITE);
        resultsTable.setSelectionBackground(new Color(173, 216, 230));  // 更明显的淡蓝色
        resultsTable.setSelectionForeground(Color.BLACK);
        
        // 启用单元格选择模式，支持单个单元格选择和拖动多选
        resultsTable.setCellSelectionEnabled(true);
        resultsTable.setRowSelectionAllowed(true);  // 必须为true才能选择单元格
        resultsTable.setColumnSelectionAllowed(true);  // 必须为true才能选择单元格
        
        // 设置表格和表头字体
        resultsTable.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        resultsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        resultsTable.getTableHeader().setBackground(new Color(248, 249, 250));
        resultsTable.getTableHeader().setForeground(new Color(60, 64, 67));
        
        // 设置列宽
        int[] widths = tableModel.getColumnWidths();
        for (int i = 0; i < widths.length && i < resultsTable.getColumnCount(); i++) {
            TableColumn column = resultsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(widths[i]);
        }
        
        // 设置Build Status列的渲染器（颜色编码）
        // 注意：由于添加了复选框列，Build Status现在是第4列（索引3）
        resultsTable.getColumnModel().getColumn(3).setCellRenderer(new BuildStatusCellRenderer());
        
        // 为Git Branch列添加过滤图标
        addBranchFilterIcon();
        
        // 添加 Ctrl+C 复制功能
        resultsTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C) {
                    copySelectedCellsToClipboard();
                }
            }
        });
        
        // 添加双击功能 - 查看构建输出
        resultsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = resultsTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        handleViewBuildOutput(row);
                    }
                }
            }
            
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
        });
        
        // 添加鼠标移动监听器 - 显示悬停提示
        setupHoverTooltip();
        
        tableScrollPane = new JScrollPane(resultsTable);
        panel.add(tableScrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建按钮面板
     * Create button panel
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(Color.WHITE);
        
        // Auto Refresh 复选框
        autoRefreshCheckBox = new JCheckBox("Auto Refresh");
        autoRefreshCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        autoRefreshCheckBox.setBackground(Color.WHITE);
        autoRefreshCheckBox.setFocusPainted(false);
        autoRefreshCheckBox.addActionListener(e -> handleAutoRefreshToggle());
        panel.add(autoRefreshCheckBox);
        
        // 刷新间隔输入框
        refreshIntervalField = new JTextField("10", 4);
        refreshIntervalField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        refreshIntervalField.setPreferredSize(new Dimension(50, 28));
        refreshIntervalField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        refreshIntervalField.setToolTipText("Refresh interval in seconds");
        panel.add(refreshIntervalField);
        
        JLabel secondsLabel = new JLabel("S");
        secondsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        secondsLabel.setForeground(new Color(95, 99, 104));
        panel.add(secondsLabel);
        
        // 添加一些间距
        panel.add(Box.createHorizontalStrut(20));
        
        // Download CSV 按钮 - 绿色
        downloadCsvButton = new JButton("<html><font color='white'><b>Download CSV</b></font></html>");
        downloadCsvButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        downloadCsvButton.setPreferredSize(new Dimension(140, 35));
        downloadCsvButton.setBackground(new Color(52, 168, 83));
        downloadCsvButton.setForeground(Color.WHITE);
        downloadCsvButton.setOpaque(true);
        downloadCsvButton.setContentAreaFilled(true);
        downloadCsvButton.setFocusPainted(false);
        downloadCsvButton.setBorderPainted(false);
        downloadCsvButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        downloadCsvButton.addActionListener(e -> handleDownloadCsv());
        panel.add(downloadCsvButton);
        
        // Copy Image Names 按钮 - 橙色
        copyImageNamesButton = new JButton("<html><font color='white'><b>Copy Image Names</b></font></html>");
        copyImageNamesButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        copyImageNamesButton.setPreferredSize(new Dimension(160, 35));
        copyImageNamesButton.setBackground(new Color(251, 140, 0));
        copyImageNamesButton.setForeground(Color.WHITE);
        copyImageNamesButton.setOpaque(true);
        copyImageNamesButton.setContentAreaFilled(true);
        copyImageNamesButton.setFocusPainted(false);
        copyImageNamesButton.setBorderPainted(false);
        copyImageNamesButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyImageNamesButton.addActionListener(e -> handleCopyImageNames());
        panel.add(copyImageNamesButton);
        
        return panel;
    }
    
    /**
     * 设置悬停提示功能
     * Setup hover tooltip for table rows
     */
    private void setupHoverTooltip() {
        // 创建1秒延迟的Timer
        hoverTimer = new javax.swing.Timer(1000, e -> {
            if (lastHoverRow >= 0) {
                showRowTooltip(lastHoverRow);
            }
        });
        hoverTimer.setRepeats(false);
        
        // 添加鼠标移动监听器
        resultsTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                
                if (row != lastHoverRow) {
                    // 鼠标移动到不同的行，重置Timer
                    lastHoverRow = row;
                    hoverTimer.restart();
                    
                    // 清除现有的tooltip
                    resultsTable.setToolTipText(null);
                }
            }
        });
        
        // 添加鼠标退出监听器
        resultsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                // 鼠标离开表格，停止Timer并清除tooltip
                hoverTimer.stop();
                lastHoverRow = -1;
                resultsTable.setToolTipText(null);
            }
        });
    }
    
    /**
     * 显示行的详细信息提示
     * Show detailed tooltip for a table row
     */
    private void showRowTooltip(int viewRow) {
        if (viewRow < 0) {
            return;
        }
        
        System.out.println("========================================");
        System.out.println("=== TOOLTIP: showRowTooltip called ===");
        System.out.println("  viewRow: " + viewRow);
        
        try {
            // 转换为模型行索引
            int modelRow = resultsTable.convertRowIndexToModel(viewRow);
            System.out.println("  modelRow: " + modelRow);
            
            // 获取构建结果
            List<BuildResult> results = tableModel.getResults();
            System.out.println("  Total results: " + results.size());
            
            if (modelRow < 0 || modelRow >= results.size()) {
                System.out.println("  ERROR: modelRow out of bounds");
                return;
            }
            
            BuildResult result = results.get(modelRow);
            System.out.println("  BuildResult retrieved");
            
            // 从原始JSON中提取字段
            // Extract fields directly from raw JSON to ensure accuracy
            String id = "";
            String queueId = "";
            String appName = "";
            String creator = "";
            String packageTitle = "";
            String createTime = "";
            String modifyTime = "";
            String imageName = "";
            
            try {
                String rawJson = result.getRawJsonData();
                System.out.println("  Raw JSON length: " + (rawJson != null ? rawJson.length() : 0));
                
                if (rawJson != null && !rawJson.isEmpty()) {
                    System.out.println("========================================");
                    System.out.println("=== TOOLTIP: RAW JSON FROM BuildResult ===");
                    System.out.println(rawJson);
                    System.out.println("========================================");
                    
                    JSONObject json = new JSONObject(rawJson);
                    System.out.println("  JSON parsed successfully");
                    
                    id = json.optString("id", "");
                    System.out.println("  Extracted id: [" + id + "]");
                    
                    // Queue ID
                    long queueIdLong = json.optLong("queue_id", 0);
                    if (queueIdLong > 0) {
                        queueId = String.valueOf(queueIdLong);
                    }
                    System.out.println("  Extracted queue_id: [" + queueId + "]");
                    
                    appName = json.optString("app_name", "");
                    System.out.println("  Extracted app_name: [" + appName + "]");
                    
                    creator = json.optString("creator", "");
                    System.out.println("  Extracted creator: [" + creator + "]");
                    
                    packageTitle = json.optString("package_title", "");
                    System.out.println("  Extracted package_title: [" + packageTitle + "]");
                    
                    imageName = json.optString("image_name", "");
                    System.out.println("  Extracted image_name: [" + imageName + "]");
                    
                    // 格式化时间
                    String rawCreateTime = json.optString("create_time", "");
                    System.out.println("  Raw create_time: [" + rawCreateTime + "]");
                    createTime = formatTime(rawCreateTime);
                    System.out.println("  Formatted create_time: [" + createTime + "]");
                    
                    String rawModifyTime = json.optString("modify_time", "");
                    System.out.println("  Raw modify_time: [" + rawModifyTime + "]");
                    modifyTime = formatTime(rawModifyTime);
                    System.out.println("  Formatted modify_time: [" + modifyTime + "]");
                    
                    // 输出调试信息
                    String logMsg = "Tooltip data from raw JSON: id=" + id + 
                                   ", queueId=" + queueId + 
                                   ", creator=" + creator + 
                                   ", packageTitle=" + packageTitle;
                    System.out.println(logMsg);
                    logger.debug(logMsg);
                } else {
                    // 如果没有原始JSON，使用BuildResult对象的字段
                    System.out.println("  WARNING: No raw JSON data, using BuildResult fields");
                    logger.warn("No raw JSON data, using BuildResult fields");
                    id = result.getId();
                    queueId = result.getQueueId();
                    appName = result.getAppName();
                    creator = result.getCreator();
                    packageTitle = result.getPackageTitle();
                    createTime = result.getFormattedCreateTime();
                    modifyTime = result.getFormattedModifyTime();
                    imageName = result.getImageName();
                    
                    System.out.println("  From BuildResult - creator: [" + creator + "]");
                    System.out.println("  From BuildResult - packageTitle: [" + packageTitle + "]");
                }
            } catch (Exception e) {
                System.out.println("  ERROR: Failed to parse raw JSON");
                e.printStackTrace();
                logger.error("Failed to parse raw JSON, using BuildResult fields", e);
                // 回退到使用BuildResult对象的字段
                id = result.getId();
                queueId = result.getQueueId();
                appName = result.getAppName();
                creator = result.getCreator();
                packageTitle = result.getPackageTitle();
                createTime = result.getFormattedCreateTime();
                modifyTime = result.getFormattedModifyTime();
                imageName = result.getImageName();
            }
            
            System.out.println("========================================");
            System.out.println("=== TOOLTIP: FINAL VALUES ===");
            System.out.println("  ID: " + id);
            System.out.println("  Queue ID: " + queueId);
            System.out.println("  App Name: " + appName);
            System.out.println("  Creator: " + creator);
            System.out.println("  Package Title: " + packageTitle);
            System.out.println("  Create Time: " + createTime);
            System.out.println("  Modify Time: " + modifyTime);
            System.out.println("  Image Name: " + imageName);
            System.out.println("========================================");
            
            // 构建HTML格式的tooltip
            StringBuilder tooltip = new StringBuilder("<html><body style='width: 500px; padding: 8px;'>");
            tooltip.append("<table cellpadding='2' cellspacing='0' style='font-family: Microsoft YaHei UI; font-size: 9px;'>");
            
            // 添加详细信息
            addTooltipRow(tooltip, "ID", id);
            addTooltipRow(tooltip, "Queue ID", queueId);
            addTooltipRow(tooltip, "App Name", appName);
            addTooltipRow(tooltip, "Creator", creator);
            addTooltipRow(tooltip, "Package Title", packageTitle);
            addTooltipRow(tooltip, "Create Time", createTime);
            addTooltipRow(tooltip, "Modify Time", modifyTime);
            addTooltipRow(tooltip, "Image Name", imageName);
            
            tooltip.append("</table>");
            tooltip.append("</body></html>");
            
            // 设置tooltip
            resultsTable.setToolTipText(tooltip.toString());
            
            logger.debug("Showing tooltip for row {}: id={}", viewRow, id);
        } catch (Exception e) {
            System.out.println("  FATAL ERROR in showRowTooltip");
            e.printStackTrace();
            logger.error("Failed to show tooltip", e);
        }
    }
    
    /**
     * 格式化时间
     * Format time from ISO 8601 to readable format
     */
    private String formatTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty() || isoTime.equals("0001-01-01T00:00:00Z")) {
            return "";
        }
        
        try {
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(isoTime);
            
            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return displayFormat.format(date);
        } catch (Exception e) {
            // 尝试不带毫秒的格式
            try {
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = isoFormat.parse(isoTime);
                
                SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                return displayFormat.format(date);
            } catch (Exception ex) {
                logger.warn("Failed to parse time: {}", isoTime);
                return isoTime;
            }
        }
    }
    
    /**
     * 添加tooltip行
     * Add a row to the tooltip table
     */
    private void addTooltipRow(StringBuilder tooltip, String label, String value) {
        if (value == null || value.isEmpty()) {
            value = "-";
        }
        
        tooltip.append("<tr>");
        tooltip.append("<td style='font-weight: bold; color: #5f6368; padding-right: 10px; white-space: nowrap;'>")
               .append(label)
               .append(":</td>");
        tooltip.append("<td style='color: #202124; word-wrap: break-word;'>")
               .append(escapeHtml(value))
               .append("</td>");
        tooltip.append("</tr>");
    }
    
    /**
     * HTML转义
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * 设置App Name下拉框的实时过滤
     * Setup real-time filtering for app name dropdown
     */
    private void setupAppNameFiltering() {
        JTextField editor = (JTextField) appNameComboBox.getEditor().getEditorComponent();
        
        // 创建防抖Timer（300ms延迟）
        filterTimer = new javax.swing.Timer(300, e -> {
            if (!isUpdatingComboBox) {
                performAppNameFiltering(editor.getText());
            }
        });
        filterTimer.setRepeats(false);
        
        appNameKeyListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                // 防止递归调用
                if (isUpdatingComboBox) {
                    return;
                }
                
                // 重启Timer（防抖）
                filterTimer.restart();
            }
        };
        
        editor.addKeyListener(appNameKeyListener);
    }
    
    /**
     * 执行应用名称过滤
     * Perform app name filtering
     */
    private void performAppNameFiltering(String input) {
        isUpdatingComboBox = true;
        try {
            String lowerInput = input.toLowerCase();
            
            // 过滤应用名称
            filteredAppNames = allAppNames.stream()
                .filter(name -> name.toLowerCase().contains(lowerInput))
                .collect(Collectors.toList());
            
            logger.debug("Filtered {} apps from {} total", filteredAppNames.size(), allAppNames.size());
            
            // 更新下拉框
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (String name : filteredAppNames) {
                model.addElement(name);
            }
            
            appNameComboBox.setModel(model);
            
            // 保持编辑器文本
            JTextField editor = (JTextField) appNameComboBox.getEditor().getEditorComponent();
            editor.setText(input);
            
            // 只在有结果时显示下拉框
            if (!filteredAppNames.isEmpty()) {
                appNameComboBox.showPopup();
            }
        } finally {
            isUpdatingComboBox = false;
        }
    }
    
    /**
     * 加载Portal设置
     * Load Portal settings
     */
    private void loadPortalSettings() {
        logger.debug("Loading Portal settings");
        
        AppSettings settings = AppSettings.getInstance();
        String tenantCodesStr = settings.getPortalTenantCodesString();
        
        // 解析租户代码，提取主租户名称（不包含子租户）
        java.util.Map<String, List<String>> tenantMap = TenantCICDUtils.parseTenantCodesWithSubTenants(tenantCodesStr);
        
        // 填充tenant下拉框，首先添加 "Please select" 作为默认选项
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Please select");  // 添加默认提示选项
        for (String mainTenant : tenantMap.keySet()) {
            model.addElement(mainTenant);  // 只添加主租户名称，例如 "stbd"
        }
        tenantComboBox.setModel(model);
        
        // 设置creator默认值为Portal username
        creatorField.setText(settings.getPortalUsername());
        
        logger.info("Loaded {} tenant codes", tenantMap.size());
        
        if (tenantMap.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No tenant codes configured. Please configure Portal settings first.",
                "Configuration Required",
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    /**
     * 处理连接操作
     * Handle connect action
     */
    private void handleConnect() {
        logger.info("=== User Action: Tenant Selected - Auto Connect ===");
        
        String selectedTenant = (String) tenantComboBox.getSelectedItem();
        if (selectedTenant == null || selectedTenant.trim().isEmpty() || selectedTenant.equals("Please select")) {
            logger.warn("No valid tenant selected");
            return;
        }
        
        currentTenant = selectedTenant.trim();
        logger.info("Selected tenant: {}", currentTenant);
        
        // 异步执行连接
        showLoading("Connecting...");
        
        // 取消之前的操作
        cancelCurrentWorker();
        
        SwingWorker<TokenResponse, Void> worker = new SwingWorker<>() {
            @Override
            protected TokenResponse doInBackground() throws Exception {
                AppSettings settings = AppSettings.getInstance();
                String username = settings.getPortalUsername();
                String password = settings.getPortalPassword();
                
                logger.info("Attempting to get token for user: {}, tenant: {}", username, currentTenant);
                return apiClient.getToken(username, password, currentTenant);
            }
            
            @Override
            protected void done() {
                hideLoading();
                try {
                    TokenResponse response = get();
                    
                    if (response.isSuccess()) {
                        currentToken = response.getAccessToken();
                        statusLabel.setText("Connected successfully to " + currentTenant);
                        statusLabel.setForeground(new Color(0, 128, 0));
                        logger.info("Connection successful, token expires in {} seconds", response.getExpireIn());
                        
                        // 更新窗口标题，显示当前租户
                        setTitle("Tenant CI/CD - " + currentTenant);
                        
                        // 更新UI状态
                        updateUIState(true);
                        
                        // 加载应用列表
                        loadApplicationList();
                    } else {
                        handleConnectionFailure("Authentication failed: " + response.getMessage());
                    }
                } catch (Exception e) {
                    logger.error("Connection failed", e);
                    handleConnectionFailure(e.getMessage());
                }
            }
        };
        
        currentWorker = worker;
        worker.execute();
    }
    
    /**
     * 处理连接失败
     * Handle connection failure
     */
    private void handleConnectionFailure(String errorMessage) {
        currentToken = null;
        statusLabel.setText("Connection failed");
        statusLabel.setForeground(Color.RED);
        updateUIState(false);
        
        logger.error("Connection failed: {}", errorMessage);
        
        JOptionPane.showMessageDialog(this,
            "Failed to connect: " + errorMessage + "\n\nPlease check your credentials and try again.",
            "Connection Error",
            JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * 加载应用列表
     * Load application list
     */
    private void loadApplicationList() {
        logger.info("=== Loading Application List ===");
        
        showLoading("Loading applications...");
        
        // 取消之前的操作
        cancelCurrentWorker();
        
        SwingWorker<List<Application>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Application> doInBackground() throws Exception {
                return apiClient.getApplicationList(currentTenant, currentToken);
            }
            
            @Override
            protected void done() {
                hideLoading();
                try {
                    List<Application> applications = get();
                    
                    // 提取应用名称
                    allAppNames.clear();
                    for (Application app : applications) {
                        if (!app.getAppName().isEmpty()) {
                            allAppNames.add(app.getAppName());
                        }
                    }
                    
                    // 填充下拉框
                    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                    model.addElement("");  // 空选项
                    for (String name : allAppNames) {
                        model.addElement(name);
                    }
                    appNameComboBox.setModel(model);
                    
                    logger.info("Loaded {} applications", allAppNames.size());
                } catch (Exception e) {
                    logger.error("Failed to load application list", e);
                    JOptionPane.showMessageDialog(TenantCICDDialog.this,
                        "Failed to load applications: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        currentWorker = worker;
        worker.execute();
    }
    
    /**
     * 处理查询操作
     * Handle search action
     */
    private void handleSearch() {
        logger.info("=== User Action: Search Button Clicked ===");
        
        // 检查是否正在搜索中
        if (isSearching) {
            logger.info("Search already in progress, skipping...");
            return;
        }
        
        if (currentToken == null || currentToken.isEmpty()) {
            logger.warn("Search attempted without connection");
            JOptionPane.showMessageDialog(this,
                "Please connect to a tenant first",
                "Not Connected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 标记开始搜索，显示取消链接
        isSearching = true;
        cancelSearchLink.setVisible(true);
        
        // 获取查询参数
        String planName = planNameField.getText().trim();
        String appName = (String) appNameComboBox.getSelectedItem();
        appName = (appName != null) ? appName.trim() : "";
        String creator = creatorField.getText().trim();
        
        // 解析page size（pageNumber固定为0，始终查询第一页）
        int pageSize = TenantCICDUtils.parseNumericInput(pageSizeField.getText(), 10, "Page Size");
        int pageNumber = 0;  // 固定为0，始终查询第一页
        
        logger.info("Query parameters: planName='{}', appName='{}', creator='{}', pageSize={}", 
                   planName, appName, creator, pageSize);
        
        // 查询优先级逻辑
        if (!planName.isEmpty()) {
            // Plan查询优先
            executeQueryByPlan(planName);
        } else if (appName != null && !appName.isEmpty()) {
            // App查询（带app name）
            executeQueryByApp(appName, creator, pageSize, pageNumber);
        } else {
            // App查询（不带app name，查询所有）
            executeQueryByApp(null, creator, pageSize, pageNumber);
        }
    }
    
    /**
     * 处理取消搜索
     * Handle cancel search action
     */
    private void handleCancelSearch() {
        logger.info("=== User Action: Cancel Search ===");
        
        if (currentWorker != null && !currentWorker.isDone()) {
            logger.info("Cancelling current search operation");
            currentWorker.cancel(true);
            currentWorker = null;
            
            // 重置状态
            isSearching = false;
            cancelSearchLink.setVisible(false);
            hideLoading();
            
            statusLabel.setText("Search cancelled");
            statusLabel.setForeground(new Color(255, 140, 0));  // 橙色
            
            logger.info("Search cancelled successfully");
        }
    }
    
    /**
     * 执行Plan查询
     * Execute query by plan
     */
    private void executeQueryByPlan(String planName) {
        logger.info("=== Executing Plan Query ===");
        logger.info("Plan name: {}", planName);
        
        showLoading("Searching by plan...");
        
        // 取消之前的操作
        cancelCurrentWorker();
        
        SwingWorker<List<BuildResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<BuildResult> doInBackground() throws Exception {
                // 1. 获取所有plan名称
                List<String> planNames = apiClient.getPlanNames(currentTenant, currentToken);
                logger.info("Retrieved {} plan names", planNames.size());
                
                // 2. 过滤匹配的plan
                String matchedPlan = TenantCICDUtils.filterPlanName(planNames, planName);
                
                if (matchedPlan == null) {
                    logger.warn("No matching plan found for: {}", planName);
                    return new ArrayList<>();
                }
                
                logger.info("Matched plan: {}", matchedPlan);
                
                // 3. 获取plan的构建结果
                PlanBuildResult result = apiClient.getBuildResultByPlan(currentTenant, currentToken, matchedPlan);
                return result.getAppBuildHistories();
            }
            
            @Override
            protected void done() {
                hideLoading();
                isSearching = false;  // 重置搜索标志
                cancelSearchLink.setVisible(false);  // 隐藏取消链接
                try {
                    List<BuildResult> results = get();
                    
                    if (results.isEmpty()) {
                        JOptionPane.showMessageDialog(TenantCICDDialog.this,
                            "No plan found matching the entered name: " + planName,
                            "No Results",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                    
                    displayResults(results);
                } catch (Exception e) {
                    logger.error("Plan query failed", e);
                    statusLabel.setText("Search failed");
                    statusLabel.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(TenantCICDDialog.this,
                        "Search failed: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        currentWorker = worker;
        worker.execute();
    }
    
    /**
     * 执行App查询
     * Execute query by app
     */
    private void executeQueryByApp(String appName, String creator, int pageSize, int pageNumber) {
        logger.info("=== Executing App Query ===");
        logger.info("App name: {}, Creator: {}, PageSize: {}", 
                   appName, creator, pageSize);
        
        showLoading("Searching by app...");
        
        // 取消之前的操作
        cancelCurrentWorker();
        
        SwingWorker<List<BuildResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<BuildResult> doInBackground() throws Exception {
                AppBuildResult result = apiClient.getBuildResultByApp(
                    currentTenant, currentToken, appName, creator, pageNumber, pageSize);
                
                logger.info("Query returned {} results (total: {})", 
                           result.getData().size(), result.getTotal());
                
                return result.getData();
            }
            
            @Override
            protected void done() {
                hideLoading();
                isSearching = false;  // 重置搜索标志
                cancelSearchLink.setVisible(false);  // 隐藏取消链接
                try {
                    List<BuildResult> results = get();
                    displayResults(results);
                } catch (Exception e) {
                    logger.error("App query failed", e);
                    statusLabel.setText("Search failed");
                    statusLabel.setForeground(Color.RED);
                    JOptionPane.showMessageDialog(TenantCICDDialog.this,
                        "Search failed: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        currentWorker = worker;
        worker.execute();
    }

    /**
     * 添加分支过滤图标到表头
     * Add branch filter icon to table header
     */
    private void addBranchFilterIcon() {
        // 获取Git Branch列的表头（第6列，索引为6）
        TableColumn branchColumn = resultsTable.getColumnModel().getColumn(6);
        
        // 创建带图标的表头渲染器
        branchColumn.setHeaderRenderer((table, value, isSelected, hasFocus, row, column) -> {
            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            headerPanel.setBackground(new Color(248, 249, 250));
            headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            
            JLabel textLabel = new JLabel(value.toString());
            textLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            textLabel.setForeground(new Color(60, 64, 67));
            headerPanel.add(textLabel);
            
            // 创建过滤图标标签（使用文本而不是emoji）
            JLabel filterIcon = new JLabel("[F]");
            filterIcon.setFont(new Font("Segoe UI", Font.BOLD, 10));
            filterIcon.setToolTipText("Filter by branch");
            
            // 如果当前有过滤器，显示不同的颜色
            if (currentBranchFilter != null) {
                filterIcon.setForeground(new Color(70, 130, 180));  // 蓝色
            } else {
                filterIcon.setForeground(new Color(95, 99, 104));   // 灰色
            }
            
            headerPanel.add(filterIcon);
            
            return headerPanel;
        });
        
        // 添加鼠标监听器到表头，处理点击事件
        resultsTable.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int column = resultsTable.getTableHeader().columnAtPoint(e.getPoint());
                if (column == 6) {  // Git Branch列（第6列，索引为6）
                    // 检查点击位置是否在图标区域
                    Rectangle headerRect = resultsTable.getTableHeader().getHeaderRect(column);
                    int iconX = headerRect.x + headerRect.width - 30;  // 图标大约在右侧30像素内
                    
                    if (e.getX() >= iconX) {
                        handleBranchFilter();
                    }
                }
            }
        });
    }
    
    /**
     * 处理分支过滤
     * Handle branch filter action
     */
    private void handleBranchFilter() {
        logger.info("=== User Action: Branch Filter Icon Clicked ===");
        
        // 从所有结果中提取唯一的分支列表
        List<String> branches = allResults.stream()
            .map(BuildResult::getGitBranch)
            .filter(branch -> branch != null && !branch.isEmpty())
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        
        if (branches.isEmpty()) {
            logger.warn("No branches available for filtering");
            JOptionPane.showMessageDialog(this,
                "No branches available for filtering.\nPlease perform a search first.",
                "No Data",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        logger.info("Opening branch filter dialog with {} unique branches", branches.size());
        
        // 显示过滤对话框
        BranchFilterDialog filterDialog = new BranchFilterDialog(this, branches, currentBranchFilter);
        filterDialog.setVisible(true);
        
        if (filterDialog.isConfirmed()) {
            String selectedBranch = filterDialog.getSelectedBranch();
            logger.info("User selected branch filter: {}", selectedBranch);
            
            currentBranchFilter = selectedBranch;
            applyBranchFilter();
            
            // 刷新表头以更新图标颜色
            resultsTable.getTableHeader().repaint();
        } else {
            logger.info("User cancelled branch filter");
        }
    }
    
    /**
     * 应用分支过滤
     * Apply branch filter to results
     */
    private void applyBranchFilter() {
        logger.info("=== Applying Branch Filter ===");
        logger.info("Filter: {}", currentBranchFilter);
        logger.info("Total results: {}", allResults.size());
        
        List<BuildResult> filteredResults;
        
        if (currentBranchFilter == null || currentBranchFilter.isEmpty()) {
            // 没有过滤器，显示所有结果
            filteredResults = new ArrayList<>(allResults);
            logger.info("No filter applied, showing all {} results", filteredResults.size());
        } else {
            // 应用过滤器
            filteredResults = allResults.stream()
                .filter(result -> currentBranchFilter.equals(result.getGitBranch()))
                .collect(Collectors.toList());
            logger.info("Filter applied, showing {} of {} results", filteredResults.size(), allResults.size());
        }
        
        // 更新表格
        tableModel.setResults(filteredResults);
        
        // 更新按钮状态
        boolean hasResults = !filteredResults.isEmpty();
        downloadCsvButton.setEnabled(hasResults);
        copyImageNamesButton.setEnabled(hasResults);
        
        // 更新状态标签
        if (hasResults) {
            String statusText = filteredResults.size() + " results displayed";
            if (currentBranchFilter != null) {
                statusText += " (filtered by branch: " + currentBranchFilter + ")";
            }
            statusLabel.setText(statusText);
            statusLabel.setForeground(new Color(0, 128, 0));
        } else {
            statusLabel.setText("No results match the filter");
            statusLabel.setForeground(Color.GRAY);
        }
    }
    
    /**
     * 显示查询结果
     * Display query results
     */
    private void displayResults(List<BuildResult> results) {
        logger.info("=== Displaying Results ===");
        logger.info("Result count: {}", results.size());
        
        // 存储原始结果用于过滤
        allResults = new ArrayList<>(results);
        
        // 检查大结果集
        if (results.size() > 100) {
            int choice = JOptionPane.showConfirmDialog(this,
                "Found " + results.size() + " results. Displaying large result sets may be slow.\n" +
                "Consider using pagination (page size) to limit results.\n\n" +
                "Do you want to continue?",
                "Large Result Set",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            
            if (choice != JOptionPane.YES_OPTION) {
                logger.info("User cancelled large result set display");
                return;
            }
        }
        
        // 更新表格
        tableModel.setResults(results);
        
        // 更新按钮状态
        boolean hasResults = !results.isEmpty();
        downloadCsvButton.setEnabled(hasResults);
        copyImageNamesButton.setEnabled(hasResults);
        
        // 更新状态标签
        if (hasResults) {
            statusLabel.setText(results.size() + " results displayed");
            statusLabel.setForeground(new Color(0, 128, 0));
        } else {
            statusLabel.setText("No results found");
            statusLabel.setForeground(Color.GRAY);
            
            JOptionPane.showMessageDialog(this,
                "No build results found for the specified criteria",
                "No Results",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * 处理CSV导出
     * Handle CSV export
     */
    private void handleDownloadCsv() {
        logger.info("=== User Action: Download CSV Button Clicked ===");
        
        List<BuildResult> results = tableModel.getResults();
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No results to export",
                "Export CSV",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        fileChooser.setSelectedFile(new File("tenant-cicd-results-" + timestamp + ".csv"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
                // 写入表头
                writer.println("App Name,Image Name,Build Status,Create Time,Version,Git Branch");
                
                // 写入数据行
                for (BuildResult buildResult : results) {
                    writer.printf("%s,%s,%s,%s,%s,%s%n",
                        escapeCsv(buildResult.getAppName()),
                        escapeCsv(buildResult.getImageName()),
                        escapeCsv(buildResult.getBuildStatus()),
                        escapeCsv(buildResult.getCreateTime()),
                        escapeCsv(buildResult.getVersion()),
                        escapeCsv(buildResult.getGitBranch()));
                }
                
                logger.info("CSV exported successfully to: {}", file.getAbsolutePath());
                
                JOptionPane.showMessageDialog(this,
                    "CSV exported successfully to:\n" + file.getAbsolutePath(),
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                logger.error("Failed to export CSV", e);
                JOptionPane.showMessageDialog(this,
                    "Failed to export CSV: " + e.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * CSV转义
     * Escape CSV value
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
    
    /**
     * 处理复制镜像名称
     * Handle copy image names
     */
    private void handleCopyImageNames() {
        logger.info("=== User Action: Copy Image Names Button Clicked ===");
        
        List<BuildResult> results = tableModel.getResults();
        if (results.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No results to copy",
                "Copy Image Names",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // 提取镜像名称并用换行符连接
        String imageNames = results.stream()
            .map(BuildResult::getImageName)
            .filter(name -> name != null && !name.isEmpty())
            .collect(Collectors.joining("\n"));
        
        // 复制到剪贴板
        StringSelection selection = new StringSelection(imageNames);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        
        logger.info("Copied {} image names to clipboard", results.size());
        
        JOptionPane.showMessageDialog(this,
            "Copied " + results.size() + " image names to clipboard",
            "Copy Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    /**
     * 处理Build按钮
     * Handle build button
     */
    private void handleBuild() {
        logger.info("=== User Action: Build Button Clicked ===");
        
        // 检查连接状态
        if (currentToken == null || currentToken.isEmpty()) {
            logger.warn("Build attempted without connection");
            JOptionPane.showMessageDialog(this,
                "Please connect to a tenant first",
                "Not Connected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 打开Build Package对话框
        try {
            BuildPackageDialog dialog = new BuildPackageDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                apiClient,
                currentToken,
                currentTenant,
                allAppNames.stream()
                    .map(name -> {
                        Application app = new Application();
                        app.setAppName(name);
                        return app;
                    })
                    .collect(java.util.stream.Collectors.toList())
            );
            dialog.setVisible(true);
        } catch (Exception e) {
            logger.error("Failed to open Build Package dialog", e);
            JOptionPane.showMessageDialog(this,
                "Failed to open Build Package dialog:\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 处理部署按钮
     * Handle deployment button
     */
    private void handleDeployment() {
        logger.info("=== User Action: Deployment Button Clicked ===");
        
        // 检查连接状态
        if (currentToken == null || currentToken.isEmpty()) {
            logger.warn("Deployment attempted without connection");
            JOptionPane.showMessageDialog(this,
                "Please connect to a tenant first",
                "Not Connected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 获取选中的镜像
        List<String> selectedImages = getSelectedImagesFromTable();
        
        logger.info("Opening Deployment dialog with {} selected images", selectedImages.size());
        
        // 打开Deployment对话框
        try {
            DeploymentDialog dialog = new DeploymentDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                apiClient,
                currentToken,
                currentTenant,
                selectedImages
            );
            dialog.setVisible(true);
        } catch (Exception e) {
            logger.error("Failed to open Deployment dialog", e);
            JOptionPane.showMessageDialog(this,
                "Failed to open Deployment dialog:\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 从表格中获取选中的镜像名称
     * Get selected image names from results table (based on checkbox column)
     * 
     * @return 镜像名称列表
     */
    private List<String> getSelectedImagesFromTable() {
        // 使用表格模型的 getSelectedImageNames 方法获取选中的镜像
        List<String> images = tableModel.getSelectedImageNames();
        
        logger.info("Extracted {} images from checkbox selection", images.size());
        
        if (images.isEmpty()) {
            logger.debug("No images selected via checkbox, checking for row selection");
            
            // 如果没有通过复选框选择，则回退到行选择模式
            int[] selectedRows = resultsTable.getSelectedRows();
            logger.debug("Getting images from {} selected rows", selectedRows.length);
            
            for (int viewRow : selectedRows) {
                // 转换为模型行索引
                int modelRow = resultsTable.convertRowIndexToModel(viewRow);
                
                if (modelRow >= 0 && modelRow < tableModel.getResults().size()) {
                    BuildResult result = tableModel.getResults().get(modelRow);
                    String imageName = result.getImageName();
                    
                    if (imageName != null && !imageName.isEmpty()) {
                        images.add(imageName);
                        logger.debug("Added image from row {}: {}", modelRow, imageName);
                    }
                }
            }
        }
        
        logger.info("Total {} images selected for deployment", images.size());
        return images;
    }
    
    /**
     * 显示加载指示器
     * Show loading indicator
     */
    private void showLoading(String message) {
        loadingLabel.setText(message);
        loadingLabel.setVisible(true);
        loadingProgressBar.setVisible(true);
        searchButton.setEnabled(false);
    }
    
    /**
     * 隐藏加载指示器
     * Hide loading indicator
     */
    private void hideLoading() {
        loadingLabel.setVisible(false);
        loadingProgressBar.setVisible(false);
        searchButton.setEnabled(currentToken != null);
    }
    
    /**
     * 更新UI状态
     * Update UI state based on connection status
     */
    private void updateUIState(boolean connected) {
        searchButton.setEnabled(connected);
        buildButton.setEnabled(connected);
        deployButton.setEnabled(connected);
        downloadCsvButton.setEnabled(false);
        copyImageNamesButton.setEnabled(false);
        
        // 强制保持按钮文字为白色（即使禁用状态）
        downloadCsvButton.setText("<html><font color='white'><b>Download CSV</b></font></html>");
        copyImageNamesButton.setText("<html><font color='white'><b>Copy Image Names</b></font></html>");
        buildButton.setText("<html><font color='white'><b>Build Image</b></font></html>");
        deployButton.setText("<html><font color='white'><b>Deployment</b></font></html>");
        searchButton.setText("<html><font color='white'><b>Search</b></font></html>");
        
        if (!connected) {
            // 清空应用列表
            appNameComboBox.setModel(new DefaultComboBoxModel<>());
            allAppNames.clear();
            filteredAppNames.clear();
        }
    }
    
    /**
     * 取消当前正在执行的SwingWorker
     * Cancel current running SwingWorker
     */
    private void cancelCurrentWorker() {
        if (currentWorker != null && !currentWorker.isDone()) {
            logger.info("Cancelling previous worker operation");
            currentWorker.cancel(true);
            currentWorker = null;
        }
    }
    
    /**
     * 资源清理
     * Resource cleanup
     */
    @Override
    public void dispose() {
        logger.info("Disposing Tenant CI/CD Dialog");
        
        // 取消正在进行的异步操作
        cancelCurrentWorker();
        
        // 停止并清理防抖Timer
        if (filterTimer != null) {
            if (filterTimer.isRunning()) {
                filterTimer.stop();
            }
            filterTimer = null;
            logger.debug("Filter timer stopped");
        }
        
        // 停止并清理hover Timer
        if (hoverTimer != null) {
            if (hoverTimer.isRunning()) {
                hoverTimer.stop();
            }
            hoverTimer = null;
            logger.debug("Hover timer stopped");
        }
        
        // 停止并清理auto refresh Timer
        if (autoRefreshTimer != null) {
            if (autoRefreshTimer.isRunning()) {
                autoRefreshTimer.stop();
            }
            autoRefreshTimer = null;
            logger.debug("Auto refresh timer stopped");
        }
        
        // 移除KeyListener防止内存泄漏
        if (appNameKeyListener != null && appNameComboBox != null) {
            try {
                JTextField editor = (JTextField) appNameComboBox.getEditor().getEditorComponent();
                editor.removeKeyListener(appNameKeyListener);
                appNameKeyListener = null;
                logger.debug("KeyListener removed successfully");
            } catch (Exception e) {
                logger.warn("Failed to remove KeyListener: {}", e.getMessage());
            }
        }
        
        // 移除表格的所有监听器
        if (resultsTable != null) {
            try {
                // 移除鼠标监听器
                java.awt.event.MouseListener[] mouseListeners = resultsTable.getMouseListeners();
                for (java.awt.event.MouseListener listener : mouseListeners) {
                    resultsTable.removeMouseListener(listener);
                }
                
                // 移除鼠标移动监听器
                java.awt.event.MouseMotionListener[] motionListeners = resultsTable.getMouseMotionListeners();
                for (java.awt.event.MouseMotionListener listener : motionListeners) {
                    resultsTable.removeMouseMotionListener(listener);
                }
                
                logger.debug("Table listeners removed successfully");
            } catch (Exception e) {
                logger.warn("Failed to remove table listeners: {}", e.getMessage());
            }
        }
        
        // 清除敏感数据
        currentToken = null;
        
        // 清除缓存数据
        if (allAppNames != null) {
            allAppNames.clear();
            allAppNames = null;
        }
        if (filteredAppNames != null) {
            filteredAppNames.clear();
            filteredAppNames = null;
        }
        if (allResults != null) {
            allResults.clear();
            allResults = null;
        }
        
        // 清空表格数据
        if (tableModel != null) {
            tableModel.setResults(new ArrayList<>());
        }
        
        // 清空API客户端引用
        apiClient = null;
        
        logger.info("Tenant CI/CD Dialog disposed successfully");
        
        super.dispose();
    }
    
    /**
     * 复制选中的单元格到剪贴板
     * Copy selected cells to clipboard
     */
    private void copySelectedCellsToClipboard() {
        int[] selectedRows = resultsTable.getSelectedRows();
        int[] selectedCols = resultsTable.getSelectedColumns();
        
        if (selectedRows.length == 0 || selectedCols.length == 0) {
            logger.debug("No cells selected");
            return;
        }
        
        // 如果只选中一个单元格，直接复制该单元格的内容
        if (selectedRows.length == 1 && selectedCols.length == 1) {
            int modelRow = resultsTable.convertRowIndexToModel(selectedRows[0]);
            int modelCol = resultsTable.convertColumnIndexToModel(selectedCols[0]);
            Object value = tableModel.getValueAt(modelRow, modelCol);
            String cellValue = value != null ? value.toString() : "";
            
            StringSelection selection = new StringSelection(cellValue);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            
            logger.info("Copied single cell value to clipboard: {}", cellValue);
            return;
        }
        
        // 如果选中多个单元格，复制为表格格式（用制表符和换行符分隔）
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < selectedRows.length; i++) {
            for (int j = 0; j < selectedCols.length; j++) {
                int modelRow = resultsTable.convertRowIndexToModel(selectedRows[i]);
                int modelCol = resultsTable.convertColumnIndexToModel(selectedCols[j]);
                Object value = tableModel.getValueAt(modelRow, modelCol);
                sb.append(value != null ? value.toString() : "");
                
                if (j < selectedCols.length - 1) {
                    sb.append("\t");  // 列之间用制表符分隔
                }
            }
            if (i < selectedRows.length - 1) {
                sb.append("\n");  // 行之间用换行符分隔
            }
        }
        
        // 复制到剪贴板
        StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
        
        logger.info("Copied {} rows x {} columns to clipboard", selectedRows.length, selectedCols.length);
    }
    
    /**
     * 处理查看构建输出
     * Handle view build output (double-click on table row)
     */
    private void handleViewBuildOutput(int viewRow) {
        logger.info("=== User Action: Double-click on row {} ===", viewRow);
        
        // 转换为模型行索引
        int modelRow = resultsTable.convertRowIndexToModel(viewRow);
        
        // 获取构建结果
        List<BuildResult> results = tableModel.getResults();
        if (modelRow < 0 || modelRow >= results.size()) {
            logger.warn("Invalid row index: {}", modelRow);
            return;
        }
        
        BuildResult buildResult = results.get(modelRow);
        String buildId = buildResult.getId();
        String appName = buildResult.getAppName();
        String buildStatus = buildResult.getBuildStatus();
        
        logger.info("Opening build output for: buildId={}, app={}, status={}", buildId, appName, buildStatus);
        
        // 检查是否有有效的ID
        if (buildId == null || buildId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Build ID is not available for this record.",
                "Cannot View Output",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 检查连接状态
        if (currentToken == null || currentToken.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Not connected. Please connect to a tenant first.",
                "Not Connected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 检查构建状态，如果是 "Build Start"，使用 check_status API
        boolean useBuildStart = "Build Start".equalsIgnoreCase(buildStatus);
        
        // 打开构建输出对话框
        try {
            BuildOutputDialog dialog = new BuildOutputDialog(
                this, apiClient, currentTenant, currentToken, buildId, appName, useBuildStart);
            dialog.setVisible(true);
        } catch (Exception e) {
            logger.error("Failed to open build output dialog", e);
            JOptionPane.showMessageDialog(this,
                "Failed to open build output dialog:\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * 处理Auto Refresh开关切换
     * Handle auto refresh toggle
     */
    private void handleAutoRefreshToggle() {
        boolean enabled = autoRefreshCheckBox.isSelected();
        logger.info("Auto refresh toggled: {}", enabled);
        
        if (enabled) {
            // 启动自动刷新
            startAutoRefresh();
        } else {
            // 停止自动刷新
            stopAutoRefresh();
        }
    }
    
    /**
     * 启动自动刷新
     * Start auto refresh
     */
    private void startAutoRefresh() {
        // 验证间隔时间
        String intervalText = refreshIntervalField.getText().trim();
        int intervalSeconds;
        
        try {
            intervalSeconds = Integer.parseInt(intervalText);
            if (intervalSeconds < 1) {
                throw new NumberFormatException("Interval must be at least 1 second");
            }
        } catch (NumberFormatException e) {
            logger.warn("Invalid refresh interval: {}", intervalText);
            JOptionPane.showMessageDialog(this,
                "Please enter a valid refresh interval (minimum 1 second)",
                "Invalid Interval",
                JOptionPane.WARNING_MESSAGE);
            autoRefreshCheckBox.setSelected(false);
            return;
        }
        
        // 检查是否已连接
        if (currentToken == null || currentToken.isEmpty()) {
            logger.warn("Auto refresh attempted without connection");
            JOptionPane.showMessageDialog(this,
                "Please connect to a tenant first",
                "Not Connected",
                JOptionPane.WARNING_MESSAGE);
            autoRefreshCheckBox.setSelected(false);
            return;
        }
        
        // 停止现有的Timer（如果有）
        stopAutoRefresh();
        
        // 创建新的Timer
        int intervalMillis = intervalSeconds * 1000;
        autoRefreshTimer = new javax.swing.Timer(intervalMillis, e -> {
            // 只有当上一次搜索完成后才触发新的搜索
            if (!isSearching) {
                logger.info("Auto refresh triggered");
                handleSearch();
            } else {
                logger.info("Auto refresh skipped - previous search still in progress");
            }
        });
        
        autoRefreshTimer.setRepeats(true);
        autoRefreshTimer.start();
        
        logger.info("Auto refresh started with interval: {} seconds", intervalSeconds);
        
        // 禁用间隔输入框
        refreshIntervalField.setEnabled(false);
    }
    
    /**
     * 停止自动刷新
     * Stop auto refresh
     */
    private void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
            autoRefreshTimer = null;
            logger.info("Auto refresh stopped");
        }
        
        // 启用间隔输入框
        refreshIntervalField.setEnabled(true);
    }
    
    /**
     * 显示右键菜单
     * Show context menu on right-click
     */
    private void showContextMenu(java.awt.event.MouseEvent e) {
        int row = resultsTable.rowAtPoint(e.getPoint());
        if (row < 0) {
            return;
        }
        
        // 选中右键点击的行
        resultsTable.setRowSelectionInterval(row, row);
        
        // 获取构建结果
        int modelRow = resultsTable.convertRowIndexToModel(row);
        List<BuildResult> results = tableModel.getResults();
        if (modelRow < 0 || modelRow >= results.size()) {
            return;
        }
        
        BuildResult buildResult = results.get(modelRow);
        String buildStatus = buildResult.getBuildStatus();
        
        // Build Fail和Build Success状态都可以Rebuild
        if ("Build Fail".equalsIgnoreCase(buildStatus) || "Build Success".equalsIgnoreCase(buildStatus)) {
            JPopupMenu popupMenu = new JPopupMenu();
            
            JMenuItem rebuildItem = new JMenuItem("Rebuild");
            rebuildItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            rebuildItem.addActionListener(event -> handleRebuild(buildResult));
            
            popupMenu.add(rebuildItem);
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    /**
     * 处理Rebuild操作
     * Handle rebuild action for failed builds
     */
    private void handleRebuild(BuildResult buildResult) {
        logger.info("=== User Action: Rebuild ===");
        logger.info("App: {}, Version: {}, Branch: {}", 
                   buildResult.getAppName(), buildResult.getVersion(), buildResult.getGitBranch());
        
        // 确认对话框
        int confirm = JOptionPane.showConfirmDialog(this,
            "Rebuild the following build?\n\n" +
            "App Name: " + buildResult.getAppName() + "\n" +
            "Version: " + buildResult.getVersion() + "\n" +
            "Git Branch: " + buildResult.getGitBranch() + "\n",
            "Confirm Rebuild",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            logger.info("Rebuild cancelled by user");
            return;
        }
        
        // 检查连接状态
        if (currentToken == null || currentToken.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Not connected. Please connect to a tenant first.",
                "Not Connected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 构建请求体
        org.json.JSONObject requestBody = new org.json.JSONObject();
        requestBody.put("app_name", buildResult.getAppName());
        requestBody.put("build_args", "");
        requestBody.put("build_type", "build_only");
        requestBody.put("change_log", "");
        requestBody.put("git_branch", buildResult.getGitBranch());
        requestBody.put("issues", new org.json.JSONArray());
        requestBody.put("plan_id", "");
        requestBody.put("popconVisible", false);
        requestBody.put("user_name", currentTenant);
        requestBody.put("version", buildResult.getVersion());
        
        logger.info("Rebuild request body: {}", requestBody.toString());
        
        // 显示loading
        showLoading("Submitting rebuild request...");
        
        // 异步提交rebuild请求
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                // 调用build API
                return apiClient.submitSingleBuild(currentTenant, currentToken, requestBody.toString());
            }
            
            @Override
            protected void done() {
                hideLoading();
                try {
                    String response = get();
                    logger.info("Rebuild submitted successfully");
                    
                    JOptionPane.showMessageDialog(TenantCICDDialog.this,
                        "Rebuild request submitted successfully!\n\n" +
                        "App: " + buildResult.getAppName() + "\n" +
                        "Version: " + buildResult.getVersion(),
                        "Rebuild Submitted",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    // 自动刷新搜索结果
                    if (!isSearching) {
                        handleSearch();
                    }
                } catch (Exception e) {
                    logger.error("Rebuild failed", e);
                    JOptionPane.showMessageDialog(TenantCICDDialog.this,
                        "Failed to submit rebuild request:\n" + e.getMessage(),
                        "Rebuild Failed",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }
}
