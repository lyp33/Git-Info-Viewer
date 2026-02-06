package com.gitviewer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Build Package对话框
 * Dialog for configuring and triggering multi-application builds
 */
public class BuildPackageDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(BuildPackageDialog.class);
    
    // UI组件
    private JComboBox<String> branchComboBox;
    private JTextField versionCodeField;
    private JLabel versionPatternLink;  // 版本模式配置超链接
    private JCheckBox selectAllUnfavoritedCheckbox;
    private JCheckBox selectAllFavoritedCheckbox;
    private JTextField unfavoritedFilterField;  // 过滤文本框
    private JPanel unfavoritedAppListPanel;
    private JPanel favoritedAppListPanel;
    private List<JCheckBox> unfavoritedAppCheckboxes;
    private List<JCheckBox> favoritedAppCheckboxes;
    private JButton addToFavoritesButton;
    private JButton removeFromFavoritesButton;
    private JButton buildPackageButton;
    private JButton closeButton;
    
    // 数据
    private PortalApiClient apiClient;
    private String currentToken;
    private String currentTenant;
    private List<String> branchList;
    private List<Application> allApplications;
    private List<Application> filteredApplications;
    private List<String> favoriteAppNames;  // 收藏的应用名称（兼容旧版本）
    private String currentVersionPattern;  // 当前租户的版本模式
    
    // 分组数据
    private List<FavoriteGroup> favoriteGroups;  // 分组列表
    private List<String> ungroupedFavorites;     // 未分组的收藏应用
    
    // SwingWorker引用，用于取消操作
    private SwingWorker<?, ?> currentWorker;
    
    // 防抖Timer，用于延迟过滤
    private javax.swing.Timer filterTimer;
    
    // 防止KeyListener递归调用的标志
    private boolean isUpdatingComboBox = false;
    
    // KeyListener引用，用于清理
    private KeyAdapter branchKeyListener;
    
    // 拖拽相关
    private JCheckBox draggedCheckbox;
    private int draggedIndex = -1;
    private FavoriteGroup draggedSourceGroup;  // 拖拽源分组（null表示ungrouped）
    
    /**
     * 构造函数
     * 
     * @param parent 父窗口
     * @param apiClient Portal API客户端
     * @param token 认证Token
     * @param tenant 租户代码
     * @param applications 应用列表（从父对话框传入）
     */
    public BuildPackageDialog(Frame parent, PortalApiClient apiClient, String token, 
                              String tenant, List<Application> applications) {
        super(parent, "Build Package", true);
        
        logger.info("=== Opening Build Package Dialog ===");
        logger.info("Tenant: {}", tenant);
        
        this.apiClient = apiClient;
        this.currentToken = token;
        this.currentTenant = tenant;
        this.allApplications = applications != null ? applications : new ArrayList<>();
        this.branchList = new ArrayList<>();
        this.filteredApplications = new ArrayList<>();
        this.unfavoritedAppCheckboxes = new ArrayList<>();
        this.favoritedAppCheckboxes = new ArrayList<>();
        this.favoriteAppNames = new ArrayList<>();
        this.favoriteGroups = new ArrayList<>();
        this.ungroupedFavorites = new ArrayList<>();
        
        initializeUI();
        loadFavoriteApps();
        loadVersionPattern();  // 加载版本模式
        loadTenantConfiguration();
        loadAndFilterApplications();
        
        setSize(900, 750);
        setLocationRelativeTo(parent);
    }
    
    /**
     * 初始化UI
     * Initialize UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);
        
        // 创建主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);
        
        // 添加各个部分
        mainPanel.add(createBranchSection());
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(createVersionCodeSection());
        mainPanel.add(Box.createVerticalStrut(20));
        mainPanel.add(createApplicationSection());
        
        add(mainPanel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * 创建分支选择部分
     * Create branch selection section
     */
    private JPanel createBranchSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        JLabel titleLabel = new JLabel("Branch");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(60, 64, 67));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        branchComboBox = new JComboBox<>();
        branchComboBox.setEditable(true);
        branchComboBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        branchComboBox.setPreferredSize(new Dimension(400, 35));
        branchComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        setupBranchFiltering();
        setupBranchChangeListener();
        panel.add(branchComboBox, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建版本代码部分
     * Create version code section
     */
    private JPanel createVersionCodeSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        // Title with pattern link
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        titlePanel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel("Version Code/Plan Code");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(60, 64, 67));
        titlePanel.add(titleLabel);
        
        // Pattern configuration link
        versionPatternLink = new JLabel("<html><u>-</u></html>");
        versionPatternLink.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        versionPatternLink.setForeground(new Color(70, 130, 180));  // Steel blue
        versionPatternLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        versionPatternLink.setToolTipText("Click to configure version code pattern");
        versionPatternLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                handlePatternLinkClick();
            }
            
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                versionPatternLink.setForeground(new Color(50, 100, 150));  // Darker blue
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                versionPatternLink.setForeground(new Color(70, 130, 180));  // Original blue
            }
        });
        titlePanel.add(versionPatternLink);
        
        panel.add(titlePanel, BorderLayout.NORTH);
        
        versionCodeField = new JTextField();
        versionCodeField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        versionCodeField.setPreferredSize(new Dimension(400, 35));
        versionCodeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        versionCodeField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        panel.add(versionCodeField, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建应用选择部分（两列布局：未收藏 | 已收藏）
     * Create application selection section (two-column layout: unfavorited | favorited)
     */
    private JPanel createApplicationSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel("Applications");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(60, 64, 67));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // 创建两列布局 - 使用 GridBagLayout 实现更好的控制
        JPanel twoColumnPanel = new JPanel(new GridBagLayout());
        twoColumnPanel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridy = 0;
        
        // 左列：未收藏的应用 (权重 1)
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        JPanel leftColumn = createAppListColumn("Unfavorited Applications", true);
        twoColumnPanel.add(leftColumn, gbc);
        
        // 中间：操作按钮 (固定宽度)
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 10, 0, 10);
        JPanel middleColumn = createFavoriteButtonsPanel();
        middleColumn.setPreferredSize(new Dimension(80, 300));
        twoColumnPanel.add(middleColumn, gbc);
        
        // 右列：已收藏的应用 (权重 1)
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        JPanel rightColumn = createAppListColumn("Favorited Applications", false);
        twoColumnPanel.add(rightColumn, gbc);
        
        panel.add(twoColumnPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建应用列表列
     * Create application list column
     */
    private JPanel createAppListColumn(String title, boolean isUnfavorited) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        
        // 标题和Select All checkbox
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setBackground(Color.WHITE);
        
        JLabel columnTitle = new JLabel(title);
        columnTitle.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        columnTitle.setForeground(new Color(60, 64, 67));
        headerPanel.add(columnTitle);
        
        JCheckBox selectAllCheckbox = new JCheckBox("Select All");
        selectAllCheckbox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        selectAllCheckbox.setBackground(Color.WHITE);
        
        if (isUnfavorited) {
            selectAllUnfavoritedCheckbox = selectAllCheckbox;
            selectAllCheckbox.addActionListener(e -> handleSelectAllUnfavorited());
            
            // 添加过滤文本框（仅左侧列表）
            unfavoritedFilterField = new JTextField();
            unfavoritedFilterField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
            unfavoritedFilterField.setPreferredSize(new Dimension(120, 25));
            unfavoritedFilterField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)
            ));
            unfavoritedFilterField.setToolTipText("Filter applications");
            
            // 添加过滤监听器
            unfavoritedFilterField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    filterUnfavoritedApps();
                }
                
                @Override
                public void removeUpdate(DocumentEvent e) {
                    filterUnfavoritedApps();
                }
                
                @Override
                public void changedUpdate(DocumentEvent e) {
                    filterUnfavoritedApps();
                }
            });
            
            headerPanel.add(unfavoritedFilterField);
        } else {
            selectAllFavoritedCheckbox = selectAllCheckbox;
            selectAllCheckbox.addActionListener(e -> handleSelectAllFavorited());
        }
        
        headerPanel.add(selectAllCheckbox);
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // 应用列表面板（可滚动）
        JPanel appListPanel = new JPanel();
        appListPanel.setLayout(new BoxLayout(appListPanel, BoxLayout.Y_AXIS));
        appListPanel.setBackground(Color.WHITE);
        
        if (isUnfavorited) {
            unfavoritedAppListPanel = appListPanel;
        } else {
            favoritedAppListPanel = appListPanel;
        }
        
        JScrollPane scrollPane = new JScrollPane(appListPanel);
        scrollPane.setPreferredSize(new Dimension(280, 300));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(218, 220, 224), 1));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建收藏操作按钮面板
     * Create favorite operation buttons panel
     */
    private JPanel createFavoriteButtonsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(5, 0, 5, 0);
        
        // Add to Favorites 按钮
        addToFavoritesButton = new JButton("→");
        addToFavoritesButton.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 18));
        addToFavoritesButton.setPreferredSize(new Dimension(60, 40));
        addToFavoritesButton.setToolTipText("Add to Favorites");
        addToFavoritesButton.setBackground(new Color(70, 130, 180));
        addToFavoritesButton.setForeground(Color.WHITE);
        addToFavoritesButton.setOpaque(true);
        addToFavoritesButton.setContentAreaFilled(true);
        addToFavoritesButton.setFocusPainted(false);
        addToFavoritesButton.setBorderPainted(false);
        addToFavoritesButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addToFavoritesButton.addActionListener(e -> handleAddToFavorites());
        
        gbc.gridy = 0;
        panel.add(addToFavoritesButton, gbc);
        
        // Remove from Favorites 按钮
        removeFromFavoritesButton = new JButton("←");
        removeFromFavoritesButton.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 18));
        removeFromFavoritesButton.setPreferredSize(new Dimension(60, 40));
        removeFromFavoritesButton.setToolTipText("Remove from Favorites");
        removeFromFavoritesButton.setBackground(new Color(95, 99, 104));
        removeFromFavoritesButton.setForeground(Color.WHITE);
        removeFromFavoritesButton.setOpaque(true);
        removeFromFavoritesButton.setContentAreaFilled(true);
        removeFromFavoritesButton.setFocusPainted(false);
        removeFromFavoritesButton.setBorderPainted(false);
        removeFromFavoritesButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        removeFromFavoritesButton.addActionListener(e -> handleRemoveFromFavorites());
        
        gbc.gridy = 1;
        panel.add(removeFromFavoritesButton, gbc);
        
        return panel;
    }
    
    /**
     * 创建按钮面板
     * Create button panel
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        panel.setBackground(Color.WHITE);
        
        // Build Package 按钮
        buildPackageButton = createStyledButton("Build Package", new Color(70, 130, 180));
        buildPackageButton.setPreferredSize(new Dimension(150, 40));
        buildPackageButton.addActionListener(e -> handleBuildPackage());
        panel.add(buildPackageButton);
        
        // Close 按钮
        closeButton = createStyledButton("Close", new Color(95, 99, 104));
        closeButton.setPreferredSize(new Dimension(100, 40));
        closeButton.addActionListener(e -> dispose());
        panel.add(closeButton);
        
        return panel;
    }
    
    /**
     * 创建样式化按钮
     * Create styled button
     */
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // 添加鼠标悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    /**
     * 设置分支过滤
     * Setup branch filtering with debounce (similar to TenantCICDDialog app name filtering)
     */
    private void setupBranchFiltering() {
        JTextField editor = (JTextField) branchComboBox.getEditor().getEditorComponent();
        
        // 创建防抖Timer（300ms延迟）
        filterTimer = new javax.swing.Timer(300, e -> {
            if (!isUpdatingComboBox) {
                performBranchFiltering(editor.getText());
            }
        });
        filterTimer.setRepeats(false);
        
        branchKeyListener = new KeyAdapter() {
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
        
        editor.addKeyListener(branchKeyListener);
        
        logger.debug("Branch filtering setup complete");
    }
    
    /**
     * 执行分支过滤
     * Perform branch filtering
     */
    private void performBranchFiltering(String input) {
        isUpdatingComboBox = true;
        try {
            String lowerInput = input.toLowerCase();
            
            logger.debug("Filtering branches with text: {}", input);
            
            // 过滤分支列表
            List<String> filteredBranches = branchList.stream()
                .filter(branch -> branch.toLowerCase().contains(lowerInput))
                .collect(Collectors.toList());
            
            logger.debug("Filtered {} branches from {} total", filteredBranches.size(), branchList.size());
            
            // 更新下拉框
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (String branch : filteredBranches) {
                model.addElement(branch);
            }
            
            branchComboBox.setModel(model);
            
            // 保持编辑器文本
            JTextField editor = (JTextField) branchComboBox.getEditor().getEditorComponent();
            editor.setText(input);
            
            // 只在有结果时显示下拉框
            if (!filteredBranches.isEmpty()) {
                branchComboBox.showPopup();
            }
        } finally {
            isUpdatingComboBox = false;
        }
    }
    
    /**
     * 设置分支变更监听器
     * Setup branch change listener to regenerate version code
     */
    private void setupBranchChangeListener() {
        branchComboBox.addActionListener(e -> {
            String selectedBranch = (String) branchComboBox.getSelectedItem();
            if (selectedBranch != null && !selectedBranch.trim().isEmpty()) {
                String versionCode = generateVersionCode(selectedBranch);
                versionCodeField.setText(versionCode);
                logger.info("Branch changed to '{}', generated version code: {}", 
                           selectedBranch, versionCode);
            }
        });
    }
    
    /**
     * 生成版本代码
     * Generate version code using configured pattern or default format
     */
    private String generateVersionCode(String branch) {
        try {
            String versionCode = VersionPatternGenerator.generateVersionCode(
                currentVersionPattern, branch, new Date());
            logger.debug("Generated version code: {} (pattern: {}, branch: {})", 
                        versionCode, 
                        currentVersionPattern != null ? currentVersionPattern : "default", 
                        branch);
            return versionCode;
        } catch (Exception e) {
            logger.error("Failed to generate version code with pattern '{}' for branch '{}', falling back to default", 
                        currentVersionPattern, branch, e);
            // Fall back to default format on error
            String defaultPattern = "{branch}_{YYYYMMDDHHMMSS}";
            return VersionPatternGenerator.generateVersionCode(defaultPattern, branch, new Date());
        }
    }
    
    /**
     * 加载版本模式
     * Load version pattern from settings
     */
    private void loadVersionPattern() {
        try {
            AppSettings settings = AppSettings.getInstance();
            currentVersionPattern = settings.getPortalVersionPattern(currentTenant);
            
            // 更新pattern link显示
            if (currentVersionPattern == null || currentVersionPattern.trim().isEmpty()) {
                versionPatternLink.setText("<html><u>-</u></html>");
                logger.debug("No version pattern configured for tenant: {}", currentTenant);
            } else {
                versionPatternLink.setText("<html><u>" + currentVersionPattern + "</u></html>");
                logger.info("Loaded version pattern for tenant {}: {}", currentTenant, currentVersionPattern);
            }
        } catch (Exception e) {
            logger.error("Failed to load version pattern for tenant: {}", currentTenant, e);
            currentVersionPattern = null;
            versionPatternLink.setText("<html><u>-</u></html>");
        }
    }
    
    /**
     * 保存版本模式
     * Save version pattern to settings
     */
    private void saveVersionPattern(String pattern) {
        AppSettings settings = AppSettings.getInstance();
        settings.setPortalVersionPattern(currentTenant, pattern);
        currentVersionPattern = pattern;
        
        // 更新pattern link显示
        if (pattern == null || pattern.trim().isEmpty()) {
            versionPatternLink.setText("<html><u>-</u></html>");
        } else {
            versionPatternLink.setText("<html><u>" + pattern + "</u></html>");
        }
        
        logger.info("Saved version pattern for tenant {}: {}", currentTenant, pattern);
    }
    
    /**
     * 处理pattern link点击
     * Handle pattern link click
     */
    private void handlePatternLinkClick() {
        logger.info("=== User Action: Pattern Link Clicked ===");
        
        // 获取当前分支
        String currentBranch = (String) branchComboBox.getSelectedItem();
        if (currentBranch == null || currentBranch.trim().isEmpty()) {
            currentBranch = "master";  // 默认分支
        }
        
        // 打开配置对话框
        VersionPatternDialog dialog = new VersionPatternDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            currentVersionPattern,
            currentBranch
        );
        dialog.setVisible(true);
        
        // 如果用户确认，保存pattern并重新生成version code
        if (dialog.isConfirmed()) {
            String newPattern = dialog.getPattern();
            saveVersionPattern(newPattern);
            
            // 重新生成version code
            if (currentBranch != null && !currentBranch.trim().isEmpty()) {
                String versionCode = generateVersionCode(currentBranch);
                versionCodeField.setText(versionCode);
                logger.info("Regenerated version code with new pattern: {}", versionCode);
            }
        }
    }
    
    /**
     * 加载租户配置
     * Load tenant configuration including branch list
     */
    private void loadTenantConfiguration() {
        logger.info("=== Loading Tenant Configuration ===");
        
        buildPackageButton.setEnabled(false);
        buildPackageButton.setText("Loading...");
        
        cancelCurrentWorker();
        
        SwingWorker<TenantConfig, Void> worker = new SwingWorker<>() {
            @Override
            protected TenantConfig doInBackground() throws Exception {
                return apiClient.getTenantConfiguration(currentTenant, currentToken);
            }
            
            @Override
            protected void done() {
                buildPackageButton.setEnabled(true);
                buildPackageButton.setText("Build Package");
                
                try {
                    TenantConfig config = get();
                    branchList = config.getBranchList();
                    
                    logger.info("Loaded {} branches", branchList.size());
                    
                    // 填充分支下拉框
                    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
                    for (String branch : branchList) {
                        model.addElement(branch);
                    }
                    branchComboBox.setModel(model);
                    
                    // 生成默认版本代码（使用第一个分支）
                    if (!branchList.isEmpty()) {
                        String firstBranch = branchList.get(0);
                        branchComboBox.setSelectedItem(firstBranch);
                        String versionCode = generateVersionCode(firstBranch);
                        versionCodeField.setText(versionCode);
                        logger.info("Generated default version code: {}", versionCode);
                    }
                } catch (Exception e) {
                    logger.error("Failed to load tenant configuration", e);
                    JOptionPane.showMessageDialog(BuildPackageDialog.this,
                        "Failed to load branch list: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        currentWorker = worker;
        worker.execute();
    }
    
    /**
     * 加载并过滤应用列表
     * Load and filter application list
     */
    private void loadAndFilterApplications() {
        logger.info("=== Loading and Filtering Applications ===");
        logger.info("Total applications: {}", allApplications.size());
        
        // 显示所有应用，按名称正序排序
        filteredApplications = allApplications.stream()
            .sorted(Comparator.comparing(Application::getAppName))
            .collect(Collectors.toList());
        
        logger.info("Displaying all {} applications sorted by name", filteredApplications.size());
        
        // 填充应用列表
        populateApplicationList();
    }
    
    /**
     * 加载收藏的应用列表（包括分组数据）
     * Load favorite applications from settings (including groups)
     */
    private void loadFavoriteApps() {
        AppSettings settings = AppSettings.getInstance();
        
        // 加载分组数据
        favoriteGroups = settings.getPortalFavoriteGroups(currentTenant);
        ungroupedFavorites = settings.getPortalUngroupedFavorites(currentTenant);
        
        // 如果没有分组数据，尝试从旧格式迁移
        if (favoriteGroups.isEmpty() && ungroupedFavorites.isEmpty()) {
            List<String> oldFavorites = settings.getPortalFavoriteApps(currentTenant);
            if (!oldFavorites.isEmpty()) {
                logger.info("Migrating {} old favorites to new format", oldFavorites.size());
                ungroupedFavorites = new ArrayList<>(oldFavorites);
                saveFavoriteApps();  // 保存为新格式
            }
        }
        
        // 构建完整的收藏列表（用于向后兼容）
        favoriteAppNames = new ArrayList<>();
        for (FavoriteGroup group : favoriteGroups) {
            favoriteAppNames.addAll(group.getAppNames());
        }
        favoriteAppNames.addAll(ungroupedFavorites);
        
        logger.info("Loaded {} groups and {} ungrouped favorites for tenant {}", 
                   favoriteGroups.size(), ungroupedFavorites.size(), currentTenant);
    }
    
    /**
     * 保存收藏的应用列表（包括分组数据）
     * Save favorite applications to settings (including groups)
     */
    private void saveFavoriteApps() {
        AppSettings settings = AppSettings.getInstance();
        
        // 保存分组数据
        settings.setPortalFavoriteGroups(currentTenant, favoriteGroups);
        settings.setPortalUngroupedFavorites(currentTenant, ungroupedFavorites);
        
        // 更新完整的收藏列表（用于向后兼容）
        favoriteAppNames = new ArrayList<>();
        for (FavoriteGroup group : favoriteGroups) {
            favoriteAppNames.addAll(group.getAppNames());
        }
        favoriteAppNames.addAll(ungroupedFavorites);
        
        logger.info("Saved {} groups and {} ungrouped favorites for tenant {}", 
                   favoriteGroups.size(), ungroupedFavorites.size(), currentTenant);
    }
    
    /**
     * 填充应用列表（分为未收藏和已收藏两列，已收藏支持分组）
     * Populate application list with checkboxes (split into unfavorited and favorited with groups)
     */
    private void populateApplicationList() {
        unfavoritedAppListPanel.removeAll();
        favoritedAppListPanel.removeAll();
        unfavoritedAppCheckboxes.clear();
        favoritedAppCheckboxes.clear();
        
        // 分离未收藏和已收藏的应用
        List<Application> unfavoritedApps = new ArrayList<>();
        
        for (Application app : filteredApplications) {
            if (!favoriteAppNames.contains(app.getAppName())) {
                unfavoritedApps.add(app);
            }
        }
        
        // 填充未收藏列表
        for (Application app : unfavoritedApps) {
            JCheckBox checkbox = new JCheckBox(app.getAppName());
            checkbox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
            checkbox.setBackground(Color.WHITE);
            checkbox.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));  // 使用边距代替固定间距
            checkbox.setIconTextGap(2);  // 减少checkbox图标与文字之间的间距
            
            // 添加拖拽支持（从unfavorited拖到favorited）
            setupUnfavoritedDragSupport(checkbox);
            
            unfavoritedAppCheckboxes.add(checkbox);
            unfavoritedAppListPanel.add(checkbox);
        }
        
        // 设置unfavorited面板为drop target，支持从favorited拖回来取消收藏
        setupUnfavoritedPanelDropTarget();
        
        // 填充已收藏列表（支持分组）
        populateFavoritedListWithGroups();
        
        // 设置favorited面板为drop target，支持拖到空白处默认放入Ungrouped
        setupFavoritedPanelDropTarget();
        
        // 添加右键菜单到收藏区域
        setupFavoritesContextMenu();
        
        unfavoritedAppListPanel.revalidate();
        unfavoritedAppListPanel.repaint();
        favoritedAppListPanel.revalidate();
        favoritedAppListPanel.repaint();
        
        logger.info("Populated {} unfavorited and {} favorited application checkboxes", 
                   unfavoritedAppCheckboxes.size(), favoritedAppCheckboxes.size());
    }
    
    /**
     * 填充已收藏列表（支持分组）
     * Populate favorited list with group support
     */
    private void populateFavoritedListWithGroups() {
        // 渲染每个分组
        for (FavoriteGroup group : favoriteGroups) {
            // 添加分组标题
            GroupHeaderPanel groupHeader = new GroupHeaderPanel(group, new GroupHeaderPanel.GroupActionListener() {
                @Override
                public void onGroupCheckboxChanged(FavoriteGroup g, boolean selected) {
                    handleGroupCheckboxChanged(g, selected);
                }
                
                @Override
                public void onGroupExpandToggled(FavoriteGroup g) {
                    handleGroupExpandToggled(g);
                }
                
                @Override
                public void onGroupRename(FavoriteGroup g, String newName) {
                    handleGroupRename(g, newName);
                }
                
                @Override
                public void onGroupDelete(FavoriteGroup g) {
                    handleGroupDelete(g);
                }
            });
            
            // 设置group header为drop target，支持拖拽item到group
            setupGroupHeaderDropTarget(groupHeader, group);
            
            favoritedAppListPanel.add(groupHeader);
            favoritedAppListPanel.add(Box.createVerticalStrut(2));
            
            // 添加分组内容（如果展开）
            if (group.isExpanded()) {
                JPanel groupContent = createGroupContentPanel(group);
                favoritedAppListPanel.add(groupContent);
                favoritedAppListPanel.add(Box.createVerticalStrut(5));
            }
        }
        
        // 添加未分组应用（始终显示，即使为空）
        // 创建Ungrouped header（样式与group header一致）
        JPanel ungroupedHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        ungroupedHeader.setBackground(new Color(245, 245, 245));
        ungroupedHeader.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(218, 220, 224)),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        ungroupedHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        JLabel ungroupedLabel = new JLabel("Ungrouped (" + ungroupedFavorites.size() + ")");
        ungroupedLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        ungroupedLabel.setForeground(new Color(60, 64, 67));
        ungroupedHeader.add(ungroupedLabel);
        
        // 设置ungrouped header为drop target
        setupUngroupedHeaderDropTarget(ungroupedHeader);
        
        favoritedAppListPanel.add(ungroupedHeader);
        favoritedAppListPanel.add(Box.createVerticalStrut(2));
        
        // 添加未分组应用内容
        if (!ungroupedFavorites.isEmpty()) {
            JPanel ungroupedContent = new JPanel();
            ungroupedContent.setLayout(new BoxLayout(ungroupedContent, BoxLayout.Y_AXIS));
            ungroupedContent.setBackground(Color.WHITE);
            ungroupedContent.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));  // 完全不缩进，与ungrouped header对齐
            
            for (String appName : ungroupedFavorites) {
                Application app = filteredApplications.stream()
                    .filter(a -> a.getAppName().equals(appName))
                    .findFirst()
                    .orElse(null);
                
                if (app != null) {
                    JCheckBox checkbox = new JCheckBox(app.getAppName());
                    checkbox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
                    checkbox.setBackground(Color.WHITE);
                    checkbox.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));  // 减少checkbox内边距
                    checkbox.setIconTextGap(2);  // 减少checkbox图标与文字之间的间距
                    
                    // 添加拖拽支持（ungrouped区域内排序）
                    setupDragAndDropInGroup(checkbox, null);
                    
                    // 添加右键菜单
                    setupAppContextMenu(checkbox, null);
                    
                    favoritedAppCheckboxes.add(checkbox);
                    ungroupedContent.add(checkbox);
                    ungroupedContent.add(Box.createVerticalStrut(3));
                }
            }
            
            favoritedAppListPanel.add(ungroupedContent);
            favoritedAppListPanel.add(Box.createVerticalStrut(5));
        }
    }
    
    /**
     * 创建分组内容面板
     * Create group content panel
     */
    private JPanel createGroupContentPanel(FavoriteGroup group) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));  // 完全不缩进，与group header对齐
        
        for (String appName : group.getAppNames()) {
            Application app = filteredApplications.stream()
                .filter(a -> a.getAppName().equals(appName))
                .findFirst()
                .orElse(null);
            
            if (app != null) {
                JCheckBox checkbox = new JCheckBox(app.getAppName());
                checkbox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
                checkbox.setBackground(Color.WHITE);
                checkbox.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));  // 减少checkbox内边距
                checkbox.setIconTextGap(2);  // 减少checkbox图标与文字之间的间距
                
                // 添加拖拽支持（仅限同组内）
                setupDragAndDropInGroup(checkbox, group);
                
                // 添加右键菜单
                setupAppContextMenu(checkbox, group);
                
                favoritedAppCheckboxes.add(checkbox);
                panel.add(checkbox);
                panel.add(Box.createVerticalStrut(3));
            }
        }
        
        return panel;
    }
    
    /**
     * 处理分组复选框变化
     * Handle group checkbox change
     */
    private void handleGroupCheckboxChanged(FavoriteGroup group, boolean selected) {
        logger.info("Group '{}' checkbox changed to: {}", group.getName(), selected);
        
        // 勾选/取消勾选该组下所有应用
        for (JCheckBox checkbox : favoritedAppCheckboxes) {
            if (group.containsApp(checkbox.getText())) {
                checkbox.setSelected(selected);
            }
        }
    }
    
    /**
     * 处理分组展开/折叠
     * Handle group expand/collapse toggle
     */
    private void handleGroupExpandToggled(FavoriteGroup group) {
        logger.info("Group '{}' expanded: {}", group.getName(), group.isExpanded());
        
        // 保存状态并刷新UI
        saveFavoriteApps();
        populateApplicationList();
    }
    
    /**
     * 处理分组重命名
     * Handle group rename
     */
    private void handleGroupRename(FavoriteGroup group, String newName) {
        logger.info("Renaming group '{}' to '{}'", group.getName(), newName);
        
        // 验证名称不重复
        for (FavoriteGroup g : favoriteGroups) {
            if (g != group && g.getName().equals(newName)) {
                JOptionPane.showMessageDialog(this,
                    "Group name already exists: " + newName,
                    "Duplicate Name",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        
        // 更新名称
        group.setName(newName);
        
        // 保存并刷新UI
        saveFavoriteApps();
        populateApplicationList();
        
        logger.info("Group renamed successfully");
    }
    
    /**
     * 处理分组删除
     * Handle group delete
     */
    private void handleGroupDelete(FavoriteGroup group) {
        logger.info("Deleting group '{}'", group.getName());
        
        // 将分组中的应用移动到未分组列表
        ungroupedFavorites.addAll(group.getAppNames());
        
        // 删除分组
        favoriteGroups.remove(group);
        
        // 保存并刷新UI
        saveFavoriteApps();
        populateApplicationList();
        
        logger.info("Group deleted, {} apps moved to ungrouped", group.getAppNames().size());
    }
    
    /**
     * 设置收藏区域右键菜单
     * Setup context menu for favorites area
     */
    private void setupFavoritesContextMenu() {
        favoritedAppListPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showFavoritesContextMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showFavoritesContextMenu(e);
                }
            }
        });
    }
    
    /**
     * 显示收藏区域右键菜单
     * Show context menu for favorites area
     */
    private void showFavoritesContextMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenuItem addGroupItem = new JMenuItem("Add Group");
        addGroupItem.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        addGroupItem.addActionListener(evt -> handleAddGroup());
        menu.add(addGroupItem);
        
        menu.show(e.getComponent(), e.getX(), e.getY());
    }
    
    /**
     * 处理添加分组
     * Handle add group
     */
    private void handleAddGroup() {
        String groupName = ModernInputDialog.showInputDialog(
            this, 
            "Add Group", 
            "Enter group name:");
        
        if (groupName == null || groupName.trim().isEmpty()) {
            return;
        }
        
        groupName = groupName.trim();
        
        // 验证名称不重复
        for (FavoriteGroup group : favoriteGroups) {
            if (group.getName().equals(groupName)) {
                JOptionPane.showMessageDialog(this,
                    "Group name already exists: " + groupName,
                    "Duplicate Name",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        
        // 创建新分组
        FavoriteGroup newGroup = new FavoriteGroup(groupName);
        favoriteGroups.add(newGroup);
        
        logger.info("Created new group: {}", groupName);
        
        // 保存并刷新UI
        saveFavoriteApps();
        populateApplicationList();
    }
    
    /**
     * 设置应用右键菜单
     * Setup context menu for application checkbox
     */
    private void setupAppContextMenu(JCheckBox checkbox, FavoriteGroup currentGroup) {
        checkbox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showAppContextMenu(e, checkbox, currentGroup);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showAppContextMenu(e, checkbox, currentGroup);
                }
            }
        });
    }
    
    /**
     * 显示应用右键菜单
     * Show context menu for application
     */
    private void showAppContextMenu(MouseEvent e, JCheckBox checkbox, FavoriteGroup currentGroup) {
        JPopupMenu menu = new JPopupMenu();
        
        JMenu moveToMenu = new JMenu("Move to Group");
        moveToMenu.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        
        // 添加"Ungrouped"选项（如果当前不在未分组中）
        if (currentGroup != null) {
            JMenuItem ungroupedItem = new JMenuItem("Ungrouped");
            ungroupedItem.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
            ungroupedItem.addActionListener(evt -> 
                moveAppToGroup(checkbox.getText(), currentGroup, null));
            moveToMenu.add(ungroupedItem);
            moveToMenu.addSeparator();
        }
        
        // 添加所有分组（排除当前分组）
        for (FavoriteGroup group : favoriteGroups) {
            if (group != currentGroup) {
                JMenuItem groupItem = new JMenuItem(group.getName());
                groupItem.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
                groupItem.addActionListener(evt -> 
                    moveAppToGroup(checkbox.getText(), currentGroup, group));
                moveToMenu.add(groupItem);
            }
        }
        
        menu.add(moveToMenu);
        menu.show(e.getComponent(), e.getX(), e.getY());
    }
    
    /**
     * 移动应用到指定分组
     * Move application to specified group
     * 
     * @param appName 应用名称
     * @param fromGroup 源分组（null表示未分组）
     * @param toGroup 目标分组（null表示移动到未分组）
     */
    private void moveAppToGroup(String appName, FavoriteGroup fromGroup, FavoriteGroup toGroup) {
        logger.info("Moving app '{}' from {} to {}", 
                   appName, 
                   fromGroup != null ? fromGroup.getName() : "Ungrouped",
                   toGroup != null ? toGroup.getName() : "Ungrouped");
        
        // 从源位置移除
        if (fromGroup != null) {
            fromGroup.removeApp(appName);
            // 如果分组为空，自动删除
            if (fromGroup.isEmpty()) {
                favoriteGroups.remove(fromGroup);
                logger.info("Removed empty group: {}", fromGroup.getName());
            }
        } else {
            ungroupedFavorites.remove(appName);
        }
        
        // 添加到目标位置
        if (toGroup != null) {
            toGroup.addApp(appName);
        } else {
            ungroupedFavorites.add(appName);
        }
        
        // 保存并刷新UI
        saveFavoriteApps();
        populateApplicationList();
    }
    
    /**
     * 设置unfavorited checkbox的拖拽支持
     * Setup drag support for unfavorited checkboxes
     */
    private void setupUnfavoritedDragSupport(JCheckBox checkbox) {
        // 设置为可拖拽
        checkbox.setTransferHandler(new CheckboxTransferHandler());
        
        // 添加鼠标监听器以启动拖拽
        checkbox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    draggedCheckbox = checkbox;
                    draggedSourceGroup = null;  // 标记为来自unfavorited
                    draggedIndex = -2;  // 使用-2标记来自unfavorited（区别于favorited的-1）
                    logger.debug("Drag started from unfavorited: {}", checkbox.getText());
                }
            }
        });
        
        checkbox.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedCheckbox == checkbox) {
                    JComponent comp = (JComponent) e.getSource();
                    TransferHandler handler = comp.getTransferHandler();
                    handler.exportAsDrag(comp, e, TransferHandler.MOVE);
                }
            }
        });
    }
    
    /**
     * 设置unfavorited面板为drop target
     * Setup unfavorited panel as drop target to unfavorite items
     */
    private void setupUnfavoritedPanelDropTarget() {
        unfavoritedAppListPanel.setDropTarget(new DropTarget(unfavoritedAppListPanel, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    // 只处理来自favorited的拖拽（draggedIndex >= 0）
                    if (draggedCheckbox != null && draggedIndex >= 0) {
                        String draggedAppName = draggedCheckbox.getText();
                        
                        logger.info("Dropping '{}' onto unfavorited panel from {}", 
                                   draggedAppName,
                                   draggedSourceGroup != null ? draggedSourceGroup.getName() : "Ungrouped");
                        
                        // 从favorites移除
                        favoriteAppNames.remove(draggedAppName);
                        
                        // 从源位置移除
                        if (draggedSourceGroup != null) {
                            draggedSourceGroup.removeApp(draggedAppName);
                            if (draggedSourceGroup.isEmpty()) {
                                favoriteGroups.remove(draggedSourceGroup);
                                logger.info("Removed empty group: {}", draggedSourceGroup.getName());
                            }
                        } else {
                            ungroupedFavorites.remove(draggedAppName);
                        }
                        
                        // 保存并刷新
                        saveFavoriteApps();
                        populateApplicationList();
                        
                        logger.info("Unfavorite complete");
                        
                        draggedCheckbox = null;
                        draggedIndex = -1;
                        draggedSourceGroup = null;
                    }
                    
                    dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    logger.error("Error during drop onto unfavorited panel", e);
                    dtde.rejectDrop();
                }
            }
            
            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                // 只接受来自favorited的拖拽
                if (draggedCheckbox != null && draggedIndex >= 0) {
                    dtde.acceptDrag(DnDConstants.ACTION_MOVE);
                } else {
                    dtde.rejectDrag();
                }
            }
            
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                // 只有来自favorited的拖拽才显示视觉反馈
                if (draggedCheckbox != null && draggedIndex >= 0) {
                    unfavoritedAppListPanel.setBackground(new Color(255, 240, 240));  // 淡红色表示取消收藏
                }
            }
            
            @Override
            public void dragExit(DropTargetEvent dte) {
                // 恢复原始背景色
                unfavoritedAppListPanel.setBackground(Color.WHITE);
            }
        }));
    }
    
    /**
     * 设置group header为drop target
     * Setup group header as drop target for dragging items into group
     */
    private void setupGroupHeaderDropTarget(GroupHeaderPanel groupHeader, FavoriteGroup targetGroup) {
        groupHeader.setDropTarget(new DropTarget(groupHeader, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    if (draggedCheckbox != null) {
                        String draggedAppName = draggedCheckbox.getText();
                        
                        // 来自unfavorited的拖拽（draggedIndex == -2）
                        if (draggedIndex == -2) {
                            logger.info("Dropping '{}' from unfavorited onto group header '{}'", 
                                       draggedAppName, targetGroup.getName());
                            
                            // 添加到favorites和目标group
                            favoriteAppNames.add(draggedAppName);
                            targetGroup.addApp(draggedAppName);
                            
                            // 保存并刷新
                            saveFavoriteApps();
                            populateApplicationList();
                            
                            logger.info("Drop from unfavorited to group complete");
                        }
                        // 来自favorited的拖拽（draggedIndex >= 0）
                        else if (draggedIndex >= 0 && draggedSourceGroup != targetGroup) {
                            logger.info("Dropping '{}' onto group header '{}' from {}", 
                                       draggedAppName,
                                       targetGroup.getName(),
                                       draggedSourceGroup != null ? draggedSourceGroup.getName() : "Ungrouped");
                            
                            // 从源位置移除
                            if (draggedSourceGroup != null) {
                                draggedSourceGroup.removeApp(draggedAppName);
                                if (draggedSourceGroup.isEmpty()) {
                                    favoriteGroups.remove(draggedSourceGroup);
                                    logger.info("Removed empty group: {}", draggedSourceGroup.getName());
                                }
                            } else {
                                ungroupedFavorites.remove(draggedAppName);
                            }
                            
                            // 添加到目标group的末尾
                            targetGroup.addApp(draggedAppName);
                            
                            // 保存并刷新
                            saveFavoriteApps();
                            populateApplicationList();
                            
                            logger.info("Drop onto group header complete");
                        }
                        
                        draggedCheckbox = null;
                        draggedIndex = -1;
                        draggedSourceGroup = null;
                    }
                    
                    dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    logger.error("Error during drop onto group header", e);
                    dtde.rejectDrop();
                }
            }
            
            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                dtde.acceptDrag(DnDConstants.ACTION_MOVE);
            }
            
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                // 可以添加视觉反馈，比如改变背景色
                groupHeader.setBackground(new Color(220, 235, 255));
            }
            
            @Override
            public void dragExit(DropTargetEvent dte) {
                // 恢复原始背景色
                groupHeader.setBackground(new Color(245, 245, 245));
            }
        }));
    }
    
    /**
     * 设置ungrouped header为drop target
     * Setup ungrouped header as drop target for dragging items to ungrouped
     */
    private void setupUngroupedHeaderDropTarget(JPanel ungroupedHeader) {
        ungroupedHeader.setDropTarget(new DropTarget(ungroupedHeader, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    if (draggedCheckbox != null) {
                        String draggedAppName = draggedCheckbox.getText();
                        
                        // 来自unfavorited的拖拽（draggedIndex == -2）
                        if (draggedIndex == -2) {
                            logger.info("Dropping '{}' from unfavorited onto Ungrouped", draggedAppName);
                            
                            // 添加到favorites和ungrouped
                            favoriteAppNames.add(draggedAppName);
                            ungroupedFavorites.add(draggedAppName);
                            
                            // 保存并刷新
                            saveFavoriteApps();
                            populateApplicationList();
                            
                            logger.info("Drop from unfavorited to ungrouped complete");
                        }
                        // 来自favorited group的拖拽（draggedIndex >= 0）
                        else if (draggedIndex >= 0 && draggedSourceGroup != null) {
                            logger.info("Dropping '{}' onto Ungrouped from group '{}'", 
                                       draggedAppName,
                                       draggedSourceGroup.getName());
                            
                            // 从源分组移除
                            draggedSourceGroup.removeApp(draggedAppName);
                            if (draggedSourceGroup.isEmpty()) {
                                favoriteGroups.remove(draggedSourceGroup);
                                logger.info("Removed empty group: {}", draggedSourceGroup.getName());
                            }
                            
                            // 添加到ungrouped
                            ungroupedFavorites.add(draggedAppName);
                            
                            // 保存并刷新
                            saveFavoriteApps();
                            populateApplicationList();
                            
                            logger.info("Drop onto Ungrouped complete");
                        }
                        
                        draggedCheckbox = null;
                        draggedIndex = -1;
                        draggedSourceGroup = null;
                    }
                    
                    dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    logger.error("Error during drop onto ungrouped label", e);
                    dtde.rejectDrop();
                }
            }
            
            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                dtde.acceptDrag(DnDConstants.ACTION_MOVE);
            }
            
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                // 添加视觉反馈
                ungroupedHeader.setBackground(new Color(220, 235, 255));
            }
            
            @Override
            public void dragExit(DropTargetEvent dte) {
                // 恢复原始背景色
                ungroupedHeader.setBackground(new Color(245, 245, 245));
            }
        }));
    }
    
    /**
     * 设置favorited面板为drop target
     * Setup favorited panel as drop target to add items to ungrouped when dropped on empty space
     */
    private void setupFavoritedPanelDropTarget() {
        favoritedAppListPanel.setDropTarget(new DropTarget(favoritedAppListPanel, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    // 只处理来自unfavorited的拖拽（draggedIndex == -2）
                    if (draggedCheckbox != null && draggedIndex == -2) {
                        String draggedAppName = draggedCheckbox.getText();
                        
                        logger.info("Dropping '{}' onto favorited panel empty space, adding to Ungrouped", 
                                   draggedAppName);
                        
                        // 添加到favorites和ungrouped
                        favoriteAppNames.add(draggedAppName);
                        ungroupedFavorites.add(draggedAppName);
                        
                        // 保存并刷新
                        saveFavoriteApps();
                        populateApplicationList();
                        
                        logger.info("Drop onto empty space complete, added to Ungrouped");
                        
                        draggedCheckbox = null;
                        draggedIndex = -1;
                        draggedSourceGroup = null;
                    }
                    
                    dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    logger.error("Error during drop onto favorited panel", e);
                    dtde.rejectDrop();
                }
            }
            
            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                // 只接受来自unfavorited的拖拽
                if (draggedCheckbox != null && draggedIndex == -2) {
                    dtde.acceptDrag(DnDConstants.ACTION_MOVE);
                } else {
                    dtde.rejectDrag();
                }
            }
        }));
    }
    
    /**
     * 设置分组内拖拽功能（支持跨组拖拽）
     * Setup drag-and-drop with cross-group support
     */
    private void setupDragAndDropInGroup(JCheckBox checkbox, FavoriteGroup group) {
        // 设置为可拖拽
        checkbox.setTransferHandler(new CheckboxTransferHandler());
        
        // 添加鼠标监听器以启动拖拽
        checkbox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    draggedCheckbox = checkbox;
                    draggedSourceGroup = group;  // 记录源分组
                    
                    if (group != null) {
                        draggedIndex = group.getAppNames().indexOf(checkbox.getText());
                        logger.debug("Drag started in group '{}': {} at index {}", 
                                   group.getName(), checkbox.getText(), draggedIndex);
                    } else {
                        draggedIndex = ungroupedFavorites.indexOf(checkbox.getText());
                        logger.debug("Drag started in ungrouped: {} at index {}", 
                                   checkbox.getText(), draggedIndex);
                    }
                }
            }
        });
        
        checkbox.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggedCheckbox == checkbox) {
                    JComponent comp = (JComponent) e.getSource();
                    TransferHandler handler = comp.getTransferHandler();
                    handler.exportAsDrag(comp, e, TransferHandler.MOVE);
                }
            }
        });
        
        // 设置为可接收拖放
        checkbox.setDropTarget(new DropTarget(checkbox, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    if (draggedCheckbox != null) {
                        String draggedAppName = draggedCheckbox.getText();
                        String targetAppName = checkbox.getText();
                        
                        // 来自unfavorited的拖拽（draggedIndex == -2）
                        if (draggedIndex == -2) {
                            logger.info("Dropping '{}' from unfavorited onto item '{}' in {}", 
                                       draggedAppName, targetAppName,
                                       group != null ? group.getName() : "Ungrouped");
                            
                            // 添加到favorites
                            favoriteAppNames.add(draggedAppName);
                            
                            // 添加到目标位置（在目标item之前）
                            if (group != null) {
                                int targetIndex = group.getAppNames().indexOf(targetAppName);
                                group.getAppNames().add(targetIndex, draggedAppName);
                            } else {
                                int targetIndex = ungroupedFavorites.indexOf(targetAppName);
                                ungroupedFavorites.add(targetIndex, draggedAppName);
                            }
                            
                            // 保存并刷新
                            saveFavoriteApps();
                            populateApplicationList();
                            
                            logger.info("Drop from unfavorited complete");
                        }
                        // 来自favorited的跨组拖拽（draggedIndex >= 0）
                        else if (draggedIndex >= 0 && draggedSourceGroup != group) {
                            logger.info("Cross-group drag: moving '{}' from {} to {}", 
                                       draggedAppName,
                                       draggedSourceGroup != null ? draggedSourceGroup.getName() : "Ungrouped",
                                       group != null ? group.getName() : "Ungrouped");
                            
                            // 从源位置移除
                            if (draggedSourceGroup != null) {
                                draggedSourceGroup.removeApp(draggedAppName);
                                if (draggedSourceGroup.isEmpty()) {
                                    favoriteGroups.remove(draggedSourceGroup);
                                    logger.info("Removed empty group: {}", draggedSourceGroup.getName());
                                }
                            } else {
                                ungroupedFavorites.remove(draggedAppName);
                            }
                            
                            // 添加到目标位置（在目标item之前）
                            if (group != null) {
                                int targetIndex = group.getAppNames().indexOf(targetAppName);
                                group.getAppNames().add(targetIndex, draggedAppName);
                            } else {
                                int targetIndex = ungroupedFavorites.indexOf(targetAppName);
                                ungroupedFavorites.add(targetIndex, draggedAppName);
                            }
                            
                            // 保存并刷新
                            saveFavoriteApps();
                            populateApplicationList();
                            
                            logger.info("Cross-group drag complete");
                        }
                        // 同组内拖拽：重新排序
                        else if (draggedIndex >= 0 && group != null) {
                            int targetIndex = group.getAppNames().indexOf(targetAppName);
                            
                            if (targetIndex >= 0 && targetIndex != draggedIndex) {
                                logger.info("Reordering in group '{}': moving from index {} to {}", 
                                           group.getName(), draggedIndex, targetIndex);
                                
                                // 重新排序
                                String movedAppName = group.getAppNames().remove(draggedIndex);
                                group.getAppNames().add(targetIndex, movedAppName);
                                
                                // 保存新顺序
                                saveFavoriteApps();
                                
                                // 刷新UI
                                populateApplicationList();
                                
                                logger.info("Reordering complete");
                            }
                        } else if (draggedIndex >= 0 && group == null) {
                            // Ungrouped区域拖拽
                            int targetIndex = ungroupedFavorites.indexOf(targetAppName);
                            
                            if (targetIndex >= 0 && targetIndex != draggedIndex) {
                                logger.info("Reordering in ungrouped: moving from index {} to {}", 
                                           draggedIndex, targetIndex);
                                
                                // 重新排序
                                String movedAppName = ungroupedFavorites.remove(draggedIndex);
                                ungroupedFavorites.add(targetIndex, movedAppName);
                                
                                // 保存新顺序
                                saveFavoriteApps();
                                
                                // 刷新UI
                                populateApplicationList();
                                
                                logger.info("Reordering complete");
                            }
                        }
                        
                        draggedCheckbox = null;
                        draggedIndex = -1;
                        draggedSourceGroup = null;
                    }
                    
                    dtde.acceptDrop(DnDConstants.ACTION_MOVE);
                    dtde.dropComplete(true);
                } catch (Exception e) {
                    logger.error("Error during drop", e);
                    dtde.rejectDrop();
                }
            }
            
            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                dtde.acceptDrag(DnDConstants.ACTION_MOVE);
            }
        }));
    }
    
    /**
     * 处理未收藏列表全选/取消全选
     * Handle select all / deselect all for unfavorited list
     */
    private void handleSelectAllUnfavorited() {
        boolean selected = selectAllUnfavoritedCheckbox.isSelected();
        logger.info("Select All Unfavorited: {}", selected);
        
        for (JCheckBox checkbox : unfavoritedAppCheckboxes) {
            if (checkbox.isVisible()) {  // 只选择可见的（未被过滤的）
                checkbox.setSelected(selected);
            }
        }
    }
    
    /**
     * 过滤未收藏应用列表
     * Filter unfavorited applications list
     */
    private void filterUnfavoritedApps() {
        String filterText = unfavoritedFilterField.getText().toLowerCase().trim();
        
        logger.debug("Filtering unfavorited apps with text: {}", filterText);
        
        for (JCheckBox checkbox : unfavoritedAppCheckboxes) {
            String appName = checkbox.getText().toLowerCase();
            boolean matches = filterText.isEmpty() || appName.contains(filterText);
            checkbox.setVisible(matches);
        }
        
        unfavoritedAppListPanel.revalidate();
        unfavoritedAppListPanel.repaint();
    }
    
    /**
     * 处理已收藏列表全选/取消全选
     * Handle select all / deselect all for favorited list
     */
    private void handleSelectAllFavorited() {
        boolean selected = selectAllFavoritedCheckbox.isSelected();
        logger.info("Select All Favorited: {}", selected);
        
        for (JCheckBox checkbox : favoritedAppCheckboxes) {
            checkbox.setSelected(selected);
        }
    }
    
    /**
     * 处理添加到收藏
     * Handle add to favorites
     */
    private void handleAddToFavorites() {
        logger.info("=== User Action: Add to Favorites Button Clicked ===");
        
        // 获取选中的未收藏应用
        List<String> selectedApps = unfavoritedAppCheckboxes.stream()
            .filter(JCheckBox::isSelected)
            .map(JCheckBox::getText)
            .collect(Collectors.toList());
        
        if (selectedApps.isEmpty()) {
            logger.warn("No unfavorited apps selected");
            JOptionPane.showMessageDialog(this,
                "Please select at least one application to add to favorites",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 添加到未分组收藏列表
        for (String appName : selectedApps) {
            if (!favoriteAppNames.contains(appName)) {
                ungroupedFavorites.add(appName);
            }
        }
        
        // 保存并刷新UI
        saveFavoriteApps();
        populateApplicationList();
        
        logger.info("Added {} apps to favorites (ungrouped)", selectedApps.size());
    }
    
    /**
     * 处理从收藏移除
     * Handle remove from favorites
     */
    private void handleRemoveFromFavorites() {
        logger.info("=== User Action: Remove from Favorites Button Clicked ===");
        
        // 获取选中的已收藏应用
        List<String> selectedApps = favoritedAppCheckboxes.stream()
            .filter(JCheckBox::isSelected)
            .map(JCheckBox::getText)
            .collect(Collectors.toList());
        
        if (selectedApps.isEmpty()) {
            logger.warn("No favorited apps selected");
            JOptionPane.showMessageDialog(this,
                "Please select at least one application to remove from favorites",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 从所有分组和未分组列表中移除
        for (String appName : selectedApps) {
            // 从分组中移除
            for (FavoriteGroup group : new ArrayList<>(favoriteGroups)) {
                group.removeApp(appName);
                // 如果分组为空，自动删除
                if (group.isEmpty()) {
                    favoriteGroups.remove(group);
                    logger.info("Removed empty group: {}", group.getName());
                }
            }
            // 从未分组列表中移除
            ungroupedFavorites.remove(appName);
        }
        
        // 保存并刷新UI
        saveFavoriteApps();
        populateApplicationList();
        
        logger.info("Removed {} apps from favorites", selectedApps.size());
    }
    
    /**
     * 处理Build Package按钮
     * Handle build package button click
     */
    private void handleBuildPackage() {
        logger.info("=== User Action: Build Package Button Clicked ===");
        
        // 验证配置
        if (!validateBuildConfiguration()) {
            return;
        }
        
        // 显示确认对话框
        showConfirmationDialog();
    }
    
    /**
     * 验证构建配置
     * Validate build configuration
     */
    private boolean validateBuildConfiguration() {
        // 检查分支选择
        String branch = (String) branchComboBox.getSelectedItem();
        if (branch == null || branch.trim().isEmpty()) {
            logger.warn("Validation failed: No branch selected");
            JOptionPane.showMessageDialog(this,
                "Please select a branch",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // 验证输入的分支是否存在于分支列表中
        branch = branch.trim();
        if (!branchList.contains(branch)) {
            logger.warn("Validation failed: Branch '{}' does not exist in branch list", branch);
            JOptionPane.showMessageDialog(this,
                "Invalid branch: '" + branch + "'\n\n" +
                "The branch you entered does not exist in the available branch list.\n" +
                "Please select a valid branch from the dropdown.",
                "Invalid Branch",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // 检查版本代码
        String versionCode = versionCodeField.getText().trim();
        if (versionCode.isEmpty()) {
            logger.warn("Validation failed: Version code is empty");
            JOptionPane.showMessageDialog(this,
                "Please enter a version code",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        // 检查应用选择
        List<String> selectedApps = getSelectedApplications();
        if (selectedApps.isEmpty()) {
            logger.warn("Validation failed: No applications selected");
            JOptionPane.showMessageDialog(this,
                "Please select at least one application",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return false;
        }
        
        logger.info("Validation passed: branch={}, version={}, apps={}", 
                   branch, versionCode, selectedApps.size());
        return true;
    }
    
    /**
     * 获取选中的应用列表（按顺序：先收藏的，后非收藏的）
     * Get list of selected applications (ordered: favorited first, then unfavorited)
     */
    private List<String> getSelectedApplications() {
        List<String> selectedApps = new ArrayList<>();
        
        // 1. 先按照favoriteAppNames的顺序添加选中的收藏应用
        for (String favAppName : favoriteAppNames) {
            // 检查这个收藏应用是否被选中
            for (JCheckBox checkbox : favoritedAppCheckboxes) {
                if (checkbox.getText().equals(favAppName) && checkbox.isSelected()) {
                    selectedApps.add(favAppName);
                    break;
                }
            }
        }
        
        // 2. 添加选中的非收藏应用（排在最后）
        selectedApps.addAll(unfavoritedAppCheckboxes.stream()
            .filter(JCheckBox::isSelected)
            .map(JCheckBox::getText)
            .collect(Collectors.toList()));
        
        logger.debug("Selected apps ordered: {} favorited, {} unfavorited", 
                    selectedApps.size() - unfavoritedAppCheckboxes.stream().filter(JCheckBox::isSelected).count(),
                    unfavoritedAppCheckboxes.stream().filter(JCheckBox::isSelected).count());
        
        return selectedApps;
    }
    
    /**
     * 显示确认对话框
     * Show confirmation dialog with build details
     */
    private void showConfirmationDialog() {
        String branch = (String) branchComboBox.getSelectedItem();
        String versionCode = versionCodeField.getText().trim();
        List<String> selectedApps = getSelectedApplications();
        
        logger.info("=== Showing Confirmation Dialog ===");
        logger.info("Branch: {}, Version: {}, Apps: {}", branch, versionCode, selectedApps.size());
        
        StringBuilder message = new StringBuilder();
        message.append("You are about to build the following package:\n\n");
        message.append("Branch:       ").append(branch).append("\n");
        message.append("Version Code: ").append(versionCode).append("\n\n");
        message.append("Applications (").append(selectedApps.size()).append(" selected):\n");
        for (String app : selectedApps) {
            message.append("  • ").append(app).append("\n");
        }
        
        int choice = JOptionPane.showConfirmDialog(this,
            message.toString(),
            "Confirm Build Package",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (choice == JOptionPane.OK_OPTION) {
            logger.info("User confirmed build");
            submitBuildRequest();
        } else {
            logger.info("User cancelled build");
        }
    }
    
    /**
     * 提交构建请求
     * Submit build request to Portal API
     */
    private void submitBuildRequest() {
        String branch = (String) branchComboBox.getSelectedItem();
        String versionCode = versionCodeField.getText().trim();
        List<String> selectedApps = getSelectedApplications();
        
        logger.info("=== Submitting Build Request ===");
        logger.info("Branch: {}, Version: {}, Apps: {}", branch, versionCode, selectedApps);
        
        // 禁用按钮并显示进度
        buildPackageButton.setEnabled(false);
        buildPackageButton.setText("Building...");
        
        cancelCurrentWorker();
        
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                // 构建请求体
                JSONObject requestBody = new JSONObject();
                JSONArray appsArray = new JSONArray();
                
                for (String appName : selectedApps) {
                    JSONObject appObj = new JSONObject();
                    appObj.put("app_name", appName);
                    appObj.put("build_type", "build_only");
                    appObj.put("git_branch", branch);
                    appObj.put("issues", new JSONArray());
                    appObj.put("popconVisible", false);
                    appObj.put("user_name", currentTenant);
                    appObj.put("version", versionCode);
                    appsArray.put(appObj);
                }
                
                requestBody.put("apps", appsArray);
                requestBody.put("description", "");
                requestBody.put("need_release_plan", false);
                requestBody.put("plan_id", "");
                requestBody.put("title", versionCode);
                
                logger.info("Build request body constructed:");
                logger.info(requestBody.toString(2));
                
                // 调用API
                apiClient.submitMultiBuild(currentTenant, currentToken, requestBody.toString());
                
                return null;
            }
            
            @Override
            protected void done() {
                buildPackageButton.setEnabled(true);
                buildPackageButton.setText("Build Package");
                
                try {
                    get();
                    logger.info("Build request submitted successfully");
                    
                    JOptionPane.showMessageDialog(BuildPackageDialog.this,
                        "Build package submitted successfully!\n\n" +
                        "Branch: " + branch + "\n" +
                        "Version: " + versionCode + "\n" +
                        "Applications: " + selectedApps.size(),
                        "Build Submitted",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    dispose();
                } catch (Exception e) {
                    logger.error("Build request failed", e);
                    
                    JOptionPane.showMessageDialog(BuildPackageDialog.this,
                        "Build request failed:\n" + e.getMessage(),
                        "Build Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        currentWorker = worker;
        worker.execute();
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
     * TransferHandler for checkbox drag-and-drop
     */
    private class CheckboxTransferHandler extends TransferHandler {
        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }
        
        @Override
        protected Transferable createTransferable(JComponent c) {
            return new Transferable() {
                @Override
                public DataFlavor[] getTransferDataFlavors() {
                    return new DataFlavor[]{DataFlavor.stringFlavor};
                }
                
                @Override
                public boolean isDataFlavorSupported(DataFlavor flavor) {
                    return DataFlavor.stringFlavor.equals(flavor);
                }
                
                @Override
                public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                    if (!isDataFlavorSupported(flavor)) {
                        throw new UnsupportedFlavorException(flavor);
                    }
                    return ((JCheckBox) c).getText();
                }
            };
        }
    }
    
    /**
     * 资源清理
     * Resource cleanup
     */
    @Override
    public void dispose() {
        logger.info("=== Disposing Build Package Dialog ===");
        
        // 取消正在进行的异步操作
        cancelCurrentWorker();
        
        // 停止并清理防抖Timer
        if (filterTimer != null) {
            filterTimer.stop();
            filterTimer = null;
            logger.debug("Filter timer stopped");
        }
        
        // 移除KeyListener
        if (branchKeyListener != null && branchComboBox != null) {
            try {
                JTextField editor = (JTextField) branchComboBox.getEditor().getEditorComponent();
                editor.removeKeyListener(branchKeyListener);
                branchKeyListener = null;
                logger.debug("Branch KeyListener removed");
            } catch (Exception e) {
                logger.warn("Failed to remove branch KeyListener", e);
            }
        }
        
        // 清除敏感数据
        currentToken = null;
        
        // 清除缓存数据
        if (branchList != null) {
            branchList.clear();
        }
        if (allApplications != null) {
            allApplications.clear();
        }
        if (filteredApplications != null) {
            filteredApplications.clear();
        }
        if (unfavoritedAppCheckboxes != null) {
            unfavoritedAppCheckboxes.clear();
        }
        if (favoritedAppCheckboxes != null) {
            favoritedAppCheckboxes.clear();
        }
        if (favoriteAppNames != null) {
            favoriteAppNames.clear();
        }
        
        super.dispose();
    }
}
