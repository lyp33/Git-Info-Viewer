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
    private JCheckBox selectAllUnfavoritedCheckbox;
    private JCheckBox selectAllFavoritedCheckbox;
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
    private List<String> favoriteAppNames;  // 收藏的应用名称
    
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
        
        initializeUI();
        loadFavoriteApps();
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
        
        JLabel titleLabel = new JLabel("Version Code/Plan Code");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(60, 64, 67));
        panel.add(titleLabel, BorderLayout.NORTH);
        
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
     * Generate version code with format: {branch}_yyyyMMddHHmmss
     */
    private String generateVersionCode(String branch) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String timestamp = sdf.format(new Date());
        return branch + "_" + timestamp;
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
     * Load and filter application list by tenant code
     */
    private void loadAndFilterApplications() {
        logger.info("=== Loading and Filtering Applications ===");
        logger.info("Total applications: {}", allApplications.size());
        
        // 过滤：只显示以当前租户代码开头的应用
        filteredApplications = allApplications.stream()
            .filter(app -> app.getAppName().startsWith(currentTenant))
            .sorted(Comparator.comparing(Application::getAppName))
            .collect(Collectors.toList());
        
        logger.info("Filtered to {} applications starting with '{}'", 
                   filteredApplications.size(), currentTenant);
        
        // 填充应用列表
        populateApplicationList();
    }
    
    /**
     * 加载收藏的应用列表
     * Load favorite applications from settings
     */
    private void loadFavoriteApps() {
        AppSettings settings = AppSettings.getInstance();
        favoriteAppNames = settings.getPortalFavoriteApps(currentTenant);
        logger.info("Loaded {} favorite apps for tenant {}", favoriteAppNames.size(), currentTenant);
    }
    
    /**
     * 保存收藏的应用列表
     * Save favorite applications to settings
     * Note: setPortalFavoriteApps() already saves to file, no need to call saveSettings()
     */
    private void saveFavoriteApps() {
        AppSettings settings = AppSettings.getInstance();
        settings.setPortalFavoriteApps(currentTenant, favoriteAppNames);
        logger.info("Saved {} favorite apps for tenant {}", favoriteAppNames.size(), currentTenant);
    }
    
    /**
     * 填充应用列表（分为未收藏和已收藏两列）
     * Populate application list with checkboxes (split into unfavorited and favorited)
     */
    private void populateApplicationList() {
        unfavoritedAppListPanel.removeAll();
        favoritedAppListPanel.removeAll();
        unfavoritedAppCheckboxes.clear();
        favoritedAppCheckboxes.clear();
        
        // 分离未收藏和已收藏的应用
        List<Application> unfavoritedApps = new ArrayList<>();
        List<Application> favoritedApps = new ArrayList<>();
        
        for (Application app : filteredApplications) {
            if (favoriteAppNames.contains(app.getAppName())) {
                favoritedApps.add(app);
            } else {
                unfavoritedApps.add(app);
            }
        }
        
        // 填充未收藏列表
        for (Application app : unfavoritedApps) {
            JCheckBox checkbox = new JCheckBox(app.getAppName());
            checkbox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
            checkbox.setBackground(Color.WHITE);
            unfavoritedAppCheckboxes.add(checkbox);
            unfavoritedAppListPanel.add(checkbox);
            unfavoritedAppListPanel.add(Box.createVerticalStrut(3));
        }
        
        // 填充已收藏列表（按favoriteAppNames的顺序）
        for (String favAppName : favoriteAppNames) {
            // 查找对应的Application对象
            Application app = filteredApplications.stream()
                .filter(a -> a.getAppName().equals(favAppName))
                .findFirst()
                .orElse(null);
            
            if (app != null) {
                JCheckBox checkbox = new JCheckBox(app.getAppName());
                checkbox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
                checkbox.setBackground(Color.WHITE);
                
                // 添加拖拽支持
                setupDragAndDrop(checkbox);
                
                favoritedAppCheckboxes.add(checkbox);
                favoritedAppListPanel.add(checkbox);
                favoritedAppListPanel.add(Box.createVerticalStrut(3));
            }
        }
        
        unfavoritedAppListPanel.revalidate();
        unfavoritedAppListPanel.repaint();
        favoritedAppListPanel.revalidate();
        favoritedAppListPanel.repaint();
        
        logger.info("Populated {} unfavorited and {} favorited application checkboxes", 
                   unfavoritedAppCheckboxes.size(), favoritedAppCheckboxes.size());
    }
    
    /**
     * 设置拖拽功能
     * Setup drag-and-drop for favorited checkbox
     */
    private void setupDragAndDrop(JCheckBox checkbox) {
        // 设置为可拖拽
        checkbox.setTransferHandler(new CheckboxTransferHandler());
        
        // 添加鼠标监听器以启动拖拽
        checkbox.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    draggedCheckbox = checkbox;
                    draggedIndex = favoritedAppCheckboxes.indexOf(checkbox);
                    logger.debug("Drag started: {} at index {}", checkbox.getText(), draggedIndex);
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
                    if (draggedCheckbox != null && draggedIndex >= 0) {
                        int targetIndex = favoritedAppCheckboxes.indexOf(checkbox);
                        
                        if (targetIndex >= 0 && targetIndex != draggedIndex) {
                            logger.info("Reordering: moving from index {} to {}", draggedIndex, targetIndex);
                            
                            // 重新排序favoriteAppNames列表
                            String movedAppName = favoriteAppNames.remove(draggedIndex);
                            favoriteAppNames.add(targetIndex, movedAppName);
                            
                            // 保存新顺序
                            saveFavoriteApps();
                            
                            // 刷新UI
                            populateApplicationList();
                            
                            logger.info("Reordering complete");
                        }
                        
                        draggedCheckbox = null;
                        draggedIndex = -1;
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
     * 处理未收藏列表全选/取消全选
     * Handle select all / deselect all for unfavorited list
     */
    private void handleSelectAllUnfavorited() {
        boolean selected = selectAllUnfavoritedCheckbox.isSelected();
        logger.info("Select All Unfavorited: {}", selected);
        
        for (JCheckBox checkbox : unfavoritedAppCheckboxes) {
            checkbox.setSelected(selected);
        }
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
        
        // 添加到收藏列表
        for (String appName : selectedApps) {
            if (!favoriteAppNames.contains(appName)) {
                favoriteAppNames.add(appName);
            }
        }
        
        // 保存并刷新UI
        saveFavoriteApps();
        populateApplicationList();
        
        logger.info("Added {} apps to favorites", selectedApps.size());
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
        
        // 从收藏列表移除
        favoriteAppNames.removeAll(selectedApps);
        
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
