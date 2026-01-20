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
import java.util.stream.Collectors;

/**
 * Tenant CI/CD主对话框
 * Main UI dialog for Tenant CI/CD feature
 */
public class TenantCICDDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(TenantCICDDialog.class);
    
    // Connection Panel组件
    private JComboBox<String> tenantComboBox;
    private JButton connectButton;
    private JLabel statusLabel;
    
    // Query Panel组件
    private JTextField planNameField;
    private JComboBox<String> appNameComboBox;
    private JTextField creatorField;
    private JTextField pageSizeField;
    private JButton searchButton;
    
    // Results Panel组件
    private JTable resultsTable;
    private BuildResultTableModel tableModel;
    private JScrollPane tableScrollPane;
    
    // Action Buttons
    private JButton downloadCsvButton;
    private JButton copyImageNamesButton;
    private JButton buildButton;
    
    // Loading Indicator
    private JLabel loadingLabel;
    private JProgressBar loadingProgressBar;
    
    // API Client和数据
    private PortalApiClient apiClient;
    private String currentToken;
    private String currentTenant;
    private List<String> allAppNames;  // 缓存用于过滤
    private List<String> filteredAppNames;  // 过滤后的应用名称
    
    // SwingWorker引用，用于取消操作
    private SwingWorker<?, ?> currentWorker;
    
    // 防止KeyListener递归调用的标志
    private boolean isUpdatingComboBox = false;
    
    // KeyListener引用，用于清理
    private KeyAdapter appNameKeyListener;
    
    // 防抖Timer，用于延迟过滤
    private javax.swing.Timer filterTimer;
    
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
        panel.add(tenantComboBox);
        
        // Connect 按钮 - 蓝色主题
        connectButton = new JButton("<html><font color='white'><b>Connect</b></font></html>");
        connectButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        connectButton.setPreferredSize(new Dimension(100, 32));
        connectButton.setBackground(new Color(66, 133, 244));
        connectButton.setForeground(Color.WHITE);
        connectButton.setOpaque(true);
        connectButton.setContentAreaFilled(true);
        connectButton.setFocusPainted(false);
        connectButton.setBorderPainted(false);
        connectButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        connectButton.addActionListener(e -> handleConnect());
        panel.add(connectButton);
        
        statusLabel = new JLabel("Not connected");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(95, 99, 104));
        panel.add(statusLabel);
        
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
        panel.add(new JLabel("Plan Name:"), gbc);
        
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
        resultsTable.getColumnModel().getColumn(2).setCellRenderer(new BuildStatusCellRenderer());
        
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
        });
        
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
        
        // Build 按钮 - 紫色
        buildButton = new JButton("<html><font color='white'><b>Build</b></font></html>");
        buildButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        buildButton.setPreferredSize(new Dimension(100, 35));
        buildButton.setBackground(new Color(142, 68, 173));
        buildButton.setForeground(Color.WHITE);
        buildButton.setOpaque(true);
        buildButton.setContentAreaFilled(true);
        buildButton.setFocusPainted(false);
        buildButton.setBorderPainted(false);
        buildButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        buildButton.addActionListener(e -> handleBuild());
        panel.add(buildButton);
        
        // Close 按钮 - 灰色
        JButton closeButton = new JButton("<html><font color='white'><b>Close</b></font></html>");
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
        List<String> tenantCodes = settings.getPortalTenantCodes();
        
        // 填充tenant下拉框
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String code : tenantCodes) {
            model.addElement(code);
        }
        tenantComboBox.setModel(model);
        
        // 设置creator默认值为Portal username
        creatorField.setText(settings.getPortalUsername());
        
        logger.info("Loaded {} tenant codes", tenantCodes.size());
        
        if (tenantCodes.isEmpty()) {
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
        logger.info("=== User Action: Connect Button Clicked ===");
        
        String selectedTenant = (String) tenantComboBox.getSelectedItem();
        if (selectedTenant == null || selectedTenant.trim().isEmpty()) {
            logger.warn("No tenant selected");
            JOptionPane.showMessageDialog(this,
                "Please select a tenant",
                "Validation Error",
                JOptionPane.ERROR_MESSAGE);
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
        
        if (currentToken == null || currentToken.isEmpty()) {
            logger.warn("Search attempted without connection");
            JOptionPane.showMessageDialog(this,
                "Please connect to a tenant first",
                "Not Connected",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
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
     * 显示查询结果
     * Display query results
     */
    private void displayResults(List<BuildResult> results) {
        logger.info("=== Displaying Results ===");
        logger.info("Result count: {}", results.size());
        
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
        
        JOptionPane.showMessageDialog(this,
            "Build functionality is not yet implemented.\n\nThis feature will be added in a future update.",
            "Not Implemented",
            JOptionPane.INFORMATION_MESSAGE);
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
        connectButton.setEnabled(false);
    }
    
    /**
     * 隐藏加载指示器
     * Hide loading indicator
     */
    private void hideLoading() {
        loadingLabel.setVisible(false);
        loadingProgressBar.setVisible(false);
        searchButton.setEnabled(currentToken != null);
        connectButton.setEnabled(true);
    }
    
    /**
     * 更新UI状态
     * Update UI state based on connection status
     */
    private void updateUIState(boolean connected) {
        searchButton.setEnabled(connected);
        buildButton.setEnabled(connected);
        downloadCsvButton.setEnabled(false);
        copyImageNamesButton.setEnabled(false);
        
        // 强制保持按钮文字为白色（即使禁用状态）
        downloadCsvButton.setText("<html><font color='white'><b>Download CSV</b></font></html>");
        copyImageNamesButton.setText("<html><font color='white'><b>Copy Image Names</b></font></html>");
        buildButton.setText("<html><font color='white'><b>Build</b></font></html>");
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
            filterTimer.stop();
            filterTimer = null;
            logger.debug("Filter timer stopped");
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
        
        // 清除敏感数据
        currentToken = null;
        
        // 清除缓存数据
        if (allAppNames != null) {
            allAppNames.clear();
        }
        if (filteredAppNames != null) {
            filteredAppNames.clear();
        }
        
        // 清空表格数据
        tableModel.setResults(new ArrayList<>());
        
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
        
        logger.info("Opening build output for: buildId={}, app={}", buildId, appName);
        
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
        
        // 打开构建输出对话框
        try {
            BuildOutputDialog dialog = new BuildOutputDialog(
                this, apiClient, currentTenant, currentToken, buildId, appName);
            dialog.setVisible(true);
        } catch (Exception e) {
            logger.error("Failed to open build output dialog", e);
            JOptionPane.showMessageDialog(this,
                "Failed to open build output dialog:\n" + e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}
