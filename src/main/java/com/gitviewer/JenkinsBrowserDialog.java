package com.gitviewer;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Jenkins 作业浏览器对话框
 * 显示 Jenkins 作业层次结构树
 */
public class JenkinsBrowserDialog extends JDialog {

    private JTree tree;
    private DefaultTreeModel treeModel;
    private JenkinsApiClient apiClient;
    private String baseJobPath;
    private JTextArea consoleArea;
    private FavoritesPanel favoritesPanel;
    private volatile boolean isLoading = false;

    public JenkinsBrowserDialog(Frame parent) {
        super(parent, "Jenkins Job Browser - Build: " + new java.util.Date().toString(), true);
        initializeApiClient();
        initializeUI();
        loadJobHierarchy();
        setLocationRelativeTo(parent);
    }

    /**
     * 初始化 API 客户端
     */
    private void initializeApiClient() {
        AppSettings settings = AppSettings.getInstance();
        String url = settings.getJenkinsUrl();
        String username = settings.getJenkinsUsername();
        String apiToken = settings.getJenkinsApiToken();
        baseJobPath = settings.getJenkinsDefaultJobPath();

        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Jenkins URL is not configured. Please configure Jenkins settings first.",
                "Configuration Error",
                JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        apiClient = new JenkinsApiClient(url, username, apiToken);
    }

    /**
     * 初始化 UI
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);
        setSize(1200, 900);  // 增大尺寸：从 800x700 改为 1200x900

        // 创建收藏面板
        favoritesPanel = new FavoritesPanel(this);
        
        // 加载收藏列表
        List<FavoriteJob> favorites = AppSettings.getInstance().getJenkinsFavorites();
        favoritesPanel.loadFavorites(favorites);

        // 创建主分割面板（上下分割）
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.7);  // 上部占70%
        mainSplitPane.setBorder(null);  // 移除边框，更现代

        // 上部：树组件
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Loading...");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setFont(new Font("Segoe UI", Font.PLAIN, 12));  // 使用现代字体
        tree.setRowHeight(32);  // 增加行高，更舒适
        tree.setLargeModel(false); // 禁用大模型优化，确保完整渲染
        tree.setBackground(Color.WHITE);

        // 设置自定义渲染器
        tree.setCellRenderer(new FavoriteTreeCellRenderer());

        // 添加树展开监听器（懒加载）
        tree.addTreeExpansionListener(new javax.swing.event.TreeExpansionListener() {
            @Override
            public void treeExpanded(javax.swing.event.TreeExpansionEvent event) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                loadChildrenIfNeeded(node);
            }

            @Override
            public void treeCollapsed(javax.swing.event.TreeExpansionEvent event) {
                // 不需要处理折叠事件
            }
        });

        // 添加鼠标监听器（双击和右键菜单）
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                    if (node != null && node.getUserObject() instanceof JenkinsItem) {
                        JenkinsItem item = (JenkinsItem) node.getUserObject();
                        if (!item.isFolder()) {
                            openJobDetails(item);
                        }
                    }
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showTreePopupMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showTreePopupMenu(e);
                }
            }
        });

        // 添加滚动面板 - 现代化样式
        JScrollPane treeScrollPane = new JScrollPane(tree);
        treeScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 15, 5, 15),
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1)
        ));
        treeScrollPane.setBackground(Color.WHITE);
        
        // 添加标题标签
        JLabel treeLabel = new JLabel("Jenkins Job Browser");
        treeLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        treeLabel.setForeground(new Color(60, 64, 67));
        treeLabel.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));
        
        JPanel treePanel = new JPanel(new BorderLayout());
        treePanel.setBackground(Color.WHITE);
        treePanel.add(treeLabel, BorderLayout.NORTH);
        treePanel.add(treeScrollPane, BorderLayout.CENTER);
        
        mainSplitPane.setTopComponent(treePanel);

        // 下部：控制台日志 - 现代化样式
        consoleArea = new JTextArea();
        consoleArea.setEditable(false);
        consoleArea.setFont(new Font("Monospaced", Font.PLAIN, 13));  // 增大字体：从 12 改为 13
        consoleArea.setBackground(new Color(30, 30, 30));  // 深灰色背景，更现代
        consoleArea.setForeground(new Color(220, 220, 220));  // 浅灰色字体
        consoleArea.setCaretColor(new Color(220, 220, 220));  // 浅灰色光标
        consoleArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane consoleScrollPane = new JScrollPane(consoleArea);
        consoleScrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 15, 10, 15),
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1)
        ));
        
        // 添加控制台标题标签
        JLabel consoleLabel = new JLabel("Console Log");
        consoleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        consoleLabel.setForeground(new Color(60, 64, 67));
        consoleLabel.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBackground(Color.WHITE);
        consolePanel.add(consoleLabel, BorderLayout.NORTH);
        consolePanel.add(consoleScrollPane, BorderLayout.CENTER);
        
        mainSplitPane.setBottomComponent(consolePanel);

        // 创建中心面板，包含收藏面板和主分割面板
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.add(favoritesPanel, BorderLayout.NORTH);
        centerPanel.add(mainSplitPane, BorderLayout.CENTER);
        
        add(centerPanel, BorderLayout.CENTER);

        // 按钮面板 - 现代化样式
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        
        // Refresh 按钮 - 蓝色
        JButton refreshButton = new JButton("<html><font color='white'><b>Refresh</b></font></html>");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        refreshButton.setPreferredSize(new Dimension(100, 35));
        refreshButton.setBackground(new Color(66, 133, 244));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setOpaque(true);
        refreshButton.setContentAreaFilled(true);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorderPainted(false);
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshButton.addActionListener(e -> loadJobHierarchy());
        buttonPanel.add(refreshButton);

        // Clear Log 按钮 - 橙色
        JButton clearLogButton = new JButton("<html><font color='white'><b>Clear Log</b></font></html>");
        clearLogButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clearLogButton.setPreferredSize(new Dimension(100, 35));
        clearLogButton.setBackground(new Color(251, 140, 0));
        clearLogButton.setForeground(Color.WHITE);
        clearLogButton.setOpaque(true);
        clearLogButton.setContentAreaFilled(true);
        clearLogButton.setFocusPainted(false);
        clearLogButton.setBorderPainted(false);
        clearLogButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearLogButton.addActionListener(e -> consoleArea.setText(""));
        buttonPanel.add(clearLogButton);

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
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 加载作业层次结构
     */
    private void loadJobHierarchy() {
        if (apiClient == null) {
            return;
        }

        logToConsole("Loading job hierarchy for: " + baseJobPath);
        isLoading = true;  // 设置加载状态

        // 在后台线程中加载
        SwingWorker<List<JenkinsItem>, Void> worker = new SwingWorker<List<JenkinsItem>, Void>() {
            @Override
            protected List<JenkinsItem> doInBackground() throws Exception {
                return apiClient.fetchJobHierarchy(baseJobPath);
            }

            @Override
            protected void done() {
                try {
                    List<JenkinsItem> items = get();
                    logToConsole("Successfully loaded " + items.size() + " items");
                    
                    DefaultMutableTreeNode root = new DefaultMutableTreeNode(baseJobPath);
                    
                    for (JenkinsItem item : items) {
                        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(item);
                        root.add(childNode);
                        
                        // 如果是文件夹，添加一个占位符节点以显示展开图标
                        if (item.isFolder()) {
                            childNode.add(new DefaultMutableTreeNode("Loading..."));
                        }
                    }
                    
                    treeModel.setRoot(root);
                    tree.expandRow(0);
                } catch (Exception e) {
                    logToConsole("ERROR: Failed to load Jenkins jobs: " + e.getMessage());
                    e.printStackTrace();
                    // 不弹出对话框，避免循环，只在控制台显示错误
                } finally {
                    isLoading = false;  // 加载完成，重置状态
                }
            }
        };
        
        worker.execute();
    }

    /**
     * 懒加载子节点
     */
    private void loadChildrenIfNeeded(DefaultMutableTreeNode node) {
        if (node.getChildCount() == 1) {
            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) node.getChildAt(0);
            if ("Loading...".equals(firstChild.getUserObject())) {
                // 需要加载子节点
                Object userObject = node.getUserObject();
                if (userObject instanceof JenkinsItem) {
                    JenkinsItem item = (JenkinsItem) userObject;
                    if (item.isFolder()) {
                        loadChildren(node, item);
                    }
                }
            }
        }
    }

    /**
     * 加载子节点
     */
    private void loadChildren(DefaultMutableTreeNode parentNode, JenkinsItem parentItem) {
        logToConsole("Loading children for: " + parentItem.getName());
        logToConsole("Parent URL: " + parentItem.getUrl());
        
        SwingWorker<List<JenkinsItem>, Void> worker = new SwingWorker<List<JenkinsItem>, Void>() {
            @Override
            protected List<JenkinsItem> doInBackground() throws Exception {
                // 从 URL 中提取作业路径
                String url = parentItem.getUrl();
                String jobPath = extractJobPath(url);
                
                // 构建完整的API URL
                String baseUrl = AppSettings.getInstance().getJenkinsUrl();
                String fullApiUrl = baseUrl + "/" + jobPath + "/api/json?tree=jobs[name,url,_class,jobs]";
                logToConsole("Full API URL: " + fullApiUrl);
                
                return apiClient.fetchJobHierarchy(jobPath);
            }

            @Override
            protected void done() {
                try {
                    List<JenkinsItem> items = get();
                    logToConsole("Successfully loaded " + items.size() + " child items");
                    
                    // 移除 "Loading..." 占位符
                    parentNode.removeAllChildren();
                    
                    // 添加实际的子节点
                    for (JenkinsItem item : items) {
                        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(item);
                        parentNode.add(childNode);
                        
                        // 如果是文件夹，添加占位符
                        if (item.isFolder()) {
                            childNode.add(new DefaultMutableTreeNode("Loading..."));
                        }
                    }
                    
                    treeModel.reload(parentNode);
                } catch (Exception e) {
                    logToConsole("ERROR: Failed to load children: " + e.getMessage());
                    e.printStackTrace();
                    // 不弹出对话框，避免循环，只在控制台显示错误
                }
            }
        };
        
        worker.execute();
    }

    /**
     * 从 URL 中提取作业路径
     * Jenkins URL 格式: http://server/job/folder1/job/folder2/
     * 需要保持 job/ 前缀: job/folder1/job/folder2
     */
    private String extractJobPath(String url) {
        try {
            String baseUrl = AppSettings.getInstance().getJenkinsUrl();
            // 确保 baseUrl 没有末尾斜杠
            baseUrl = baseUrl.replaceAll("/+$", "");
            
            logToConsole("Extracting path from URL: " + url);
            logToConsole("Base URL: " + baseUrl);
            
            // 如果 URL 以 http:// 或 https:// 开头，说明是完整 URL
            if (url.startsWith("http://") || url.startsWith("https://")) {
                // 尝试从完整 URL 中提取路径部分
                // 例如: http://172.25.32.166/job/gemini/job/Manual-Build/ -> job/gemini/job/Manual-Build
                
                // 先尝试直接匹配 baseUrl
                if (url.startsWith(baseUrl)) {
                    String path = url.substring(baseUrl.length());
                    path = path.replaceAll("^/+", "");  // 移除开头的斜杠
                    path = path.replaceAll("/+$", "");  // 移除末尾的斜杠
                    
                    logToConsole("Extracted job path (direct match): " + path);
                    return path;
                }
                
                // 如果不匹配，可能是端口号不同，尝试提取主机名后的路径
                // 找到第三个斜杠的位置（http://host/ 之后）
                int protocolEnd = url.indexOf("://");
                if (protocolEnd != -1) {
                    int pathStart = url.indexOf("/", protocolEnd + 3);
                    if (pathStart != -1) {
                        String path = url.substring(pathStart + 1);  // 跳过第一个斜杠
                        path = path.replaceAll("/+$", "");  // 移除末尾的斜杠
                        
                        logToConsole("Extracted job path (from host): " + path);
                        return path;
                    }
                }
            }
            
            // 如果不是完整 URL，可能已经是相对路径
            String path = url.replaceAll("^/+", "").replaceAll("/+$", "");
            logToConsole("Using as relative path: " + path);
            return path;
            
        } catch (Exception e) {
            logToConsole("ERROR: Failed to extract job path from URL: " + url);
            e.printStackTrace();
        }
        // 如果出错，返回清理后的 URL
        return url.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    /**
     * 记录日志到控制台
     */
    private void logToConsole(String message) {
        SwingUtilities.invokeLater(() -> {
            consoleArea.append(message + "\n");
            consoleArea.setCaretPosition(consoleArea.getDocument().getLength());
        });
    }

    /**
     * 检查是否正在加载
     * @return true 如果正在加载，false 如果加载完成
     */
    public boolean isLoading() {
        return isLoading;
    }

    /**
     * 打开作业详情对话框
     */
    private void openJobDetails(JenkinsItem item) {
        String jobPath = extractJobPath(item.getUrl());
        JenkinsJobDetailsDialog detailsDialog = new JenkinsJobDetailsDialog(
            (Frame) getOwner(), apiClient, jobPath, item.getName());
        detailsDialog.setVisible(true);
    }
    
    /**
     * 显示树节点右键菜单
     */
    private void showTreePopupMenu(MouseEvent e) {
        // 获取点击位置的节点
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }
        
        // 选中该节点
        tree.setSelectionPath(path);
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (node == null || !(node.getUserObject() instanceof JenkinsItem)) {
            return;
        }
        
        JenkinsItem item = (JenkinsItem) node.getUserObject();
        
        // 只为叶子节点（非文件夹）显示收藏菜单
        if (item.isFolder()) {
            return;
        }
        
        // 创建右键菜单
        JPopupMenu popupMenu = new JPopupMenu();
        
        String jobPath = extractJobPath(item.getUrl());
        boolean isFavorite = isFavoriteJob(jobPath);
        
        if (isFavorite) {
            // Already favorited, show "Remove from Favorites"
            JMenuItem removeItem = new JMenuItem("Remove from Favorites");
            removeItem.addActionListener(evt -> removeFavoriteJob(item));
            popupMenu.add(removeItem);
        } else {
            // Not favorited, show "Add to Favorites"
            JMenuItem addItem = new JMenuItem("Add to Favorites");
            addItem.addActionListener(evt -> addFavoriteJob(item));
            popupMenu.add(addItem);
        }
        
        // 显示菜单
        popupMenu.show(e.getComponent(), e.getX(), e.getY());
    }
    
    /**
     * 检查任务是否已收藏
     */
    private boolean isFavoriteJob(String jobPath) {
        List<FavoriteJob> favorites = AppSettings.getInstance().getJenkinsFavorites();
        for (FavoriteJob job : favorites) {
            if (job.getJobPath().equals(jobPath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 添加任务到收藏
     */
    private void addFavoriteJob(JenkinsItem item) {
        String jobPath = extractJobPath(item.getUrl());
        
        // 创建收藏对象
        FavoriteJob favoriteJob = new FavoriteJob(
            jobPath,
            item.getName(),
            item.getUrl(),
            AppSettings.getInstance().getJenkinsFavorites().size()
        );
        
        // 添加到收藏面板
        if (favoritesPanel != null) {
            favoritesPanel.addFavorite(favoriteJob);
        }
        
        // 刷新树显示
        refreshTreeFavoriteMarks();
        
        logToConsole("Added to favorites: " + item.getName());
    }
    
    /**
     * 从收藏中移除任务
     */
    private void removeFavoriteJob(JenkinsItem item) {
        String jobPath = extractJobPath(item.getUrl());
        
        // 从收藏列表中查找并移除
        List<FavoriteJob> favorites = AppSettings.getInstance().getJenkinsFavorites();
        FavoriteJob toRemove = null;
        for (FavoriteJob job : favorites) {
            if (job.getJobPath().equals(jobPath)) {
                toRemove = job;
                break;
            }
        }
        
        if (toRemove != null && favoritesPanel != null) {
            favoritesPanel.removeFavorite(toRemove);
        }
        
        // 刷新树显示
        refreshTreeFavoriteMarks();
        
        logToConsole("Removed from favorites: " + item.getName());
    }

    /**
     * 刷新树节点的收藏标记
     * 当收藏列表变化时调用
     */
    public void refreshTreeFavoriteMarks() {
        // 获取最新的收藏列表
        List<FavoriteJob> favorites = AppSettings.getInstance().getJenkinsFavorites();
        
        // 更新渲染器的收藏集合
        if (tree.getCellRenderer() instanceof FavoriteTreeCellRenderer) {
            FavoriteTreeCellRenderer renderer = (FavoriteTreeCellRenderer) tree.getCellRenderer();
            renderer.updateFavorites(favorites);
        }
        
        tree.repaint();
    }

    /**
     * 根据任务路径定位到树节点
     * @param jobPath 任务路径
     * @return 是否成功定位
     */
    public boolean navigateToJobPath(String jobPath) {
        System.out.println("[JenkinsBrowserDialog] ========== navigateToJobPath Debug ==========");
        System.out.println("[JenkinsBrowserDialog] Input jobPath: [" + jobPath + "]");
        System.out.println("[JenkinsBrowserDialog] Base job path: [" + baseJobPath + "]");
        
        logToConsole("Navigating to job: " + jobPath);
        logToConsole("Base job path: " + baseJobPath);
        
        // 清理路径：移除可能的 [*] 前缀
        String cleanedPath = jobPath;
        if (cleanedPath.startsWith("[*] ")) {
            cleanedPath = cleanedPath.substring(4);
            System.out.println("[JenkinsBrowserDialog] Removed [*] prefix, cleaned path: [" + cleanedPath + "]");
            logToConsole("Removed [*] prefix from path");
        }
        
        // 从根节点开始查找
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        if (root == null) {
            System.out.println("[JenkinsBrowserDialog] ERROR: Root node is null");
            return false;
        }
        
        // 分割路径 - 使用 /job/ 作为分隔符
        // 例如: job/gemini1/job/Manual-Build/job/tools_lock/job/update-bs-bff-version
        // 分割后: ["job", "gemini1", "job", "Manual-Build", "job", "tools_lock", "job", "update-bs-bff-version"]
        String[] allParts = cleanedPath.split("/");
        
        System.out.println("[JenkinsBrowserDialog] Split path into " + allParts.length + " parts");
        for (int i = 0; i < allParts.length; i++) {
            System.out.println("[JenkinsBrowserDialog]   Part[" + i + "]: [" + allParts[i] + "]");
        }
        
        // 提取实际的job名称（跳过 "job" 关键字）
        java.util.List<String> jobNames = new java.util.ArrayList<>();
        for (int i = 0; i < allParts.length; i++) {
            if ("job".equals(allParts[i]) && i + 1 < allParts.length) {
                jobNames.add(allParts[i + 1]);
                i++; // 跳过下一个元素（job名称）
            }
        }
        
        System.out.println("[JenkinsBrowserDialog] Extracted " + jobNames.size() + " job names: " + String.join(" -> ", jobNames));
        logToConsole("Extracted job names: " + String.join(" -> ", jobNames));
        
        // 同样处理 baseJobPath
        String[] baseAllParts = baseJobPath.split("/");
        java.util.List<String> baseJobNames = new java.util.ArrayList<>();
        for (int i = 0; i < baseAllParts.length; i++) {
            if ("job".equals(baseAllParts[i]) && i + 1 < baseAllParts.length) {
                baseJobNames.add(baseAllParts[i + 1]);
                i++;
            }
        }
        
        System.out.println("[JenkinsBrowserDialog] Base job names: " + String.join(" -> ", baseJobNames));
        logToConsole("Base job names: " + String.join(" -> ", baseJobNames));
        
        // 跳过与 baseJobPath 匹配的部分
        int startIndex = 0;
        if (jobNames.size() > baseJobNames.size()) {
            boolean baseMatches = true;
            for (int i = 0; i < baseJobNames.size() && i < jobNames.size(); i++) {
                if (!jobNames.get(i).equals(baseJobNames.get(i))) {
                    baseMatches = false;
                    break;
                }
            }
            if (baseMatches) {
                startIndex = baseJobNames.size();
                System.out.println("[JenkinsBrowserDialog] Base path matches, starting from index: " + startIndex);
                logToConsole("Skipping base path, starting from index: " + startIndex);
            }
        }
        
        // 如果整个路径就是 baseJobPath，直接选中根节点
        if (startIndex >= jobNames.size()) {
            TreePath treePath = new TreePath(treeModel.getPathToRoot(root));
            tree.setSelectionPath(treePath);
            tree.scrollPathToVisible(treePath);
            System.out.println("[JenkinsBrowserDialog] Selected root node (matches base path)");
            logToConsole("Selected root node (matches base path)");
            return true;
        }
        
        // 递归查找并展开节点
        System.out.println("[JenkinsBrowserDialog] Starting recursive search from index " + startIndex);
        DefaultMutableTreeNode targetNode = findNodeByJobNames(root, jobNames, startIndex);
        
        if (targetNode != null) {
            // 构建树路径
            TreePath treePath = new TreePath(treeModel.getPathToRoot(targetNode));
            
            // 展开并选中节点
            tree.setSelectionPath(treePath);
            tree.scrollPathToVisible(treePath);
            
            // 不在这里打开详情对话框！
            // 因为详情对话框是模态的，会阻塞当前线程，导致navigateToJobPath无法返回
            // 应该在FavoritesPanel中，等待导航完成后再打开
            System.out.println("[JenkinsBrowserDialog] Successfully navigated to: " + jobPath);
            System.out.println("[JenkinsBrowserDialog] Note: Not opening details dialog here to avoid blocking");
            System.out.println("[JenkinsBrowserDialog] ==========================================");
            logToConsole("Successfully navigated to: " + jobPath);
            return true;
        }
        
        System.out.println("[JenkinsBrowserDialog] Failed to find job: " + jobPath);
        System.out.println("[JenkinsBrowserDialog] ==========================================");
        logToConsole("Failed to find job: " + jobPath);
        return false;
    }
    
    /**
     * 递归查找树节点（使用job名称列表）
     * 自动加载所有需要的子节点以确保能找到目标
     * @param node 当前节点
     * @param jobNames job名称列表
     * @param index 当前索引
     * @return 找到的节点，如果未找到返回 null
     */
    private DefaultMutableTreeNode findNodeByJobNames(DefaultMutableTreeNode node, java.util.List<String> jobNames, int index) {
        // 如果已经到达路径末尾，返回当前节点
        if (index >= jobNames.size()) {
            System.out.println("[JenkinsBrowserDialog] Reached end of path, returning current node");
            return node;
        }
        
        // 获取当前要查找的名称
        String targetName = jobNames.get(index);
        
        System.out.println("[JenkinsBrowserDialog] findNodeByJobNames: Looking for [" + targetName + "] at index " + index);
        logToConsole("Looking for: " + targetName + " at index " + index);
        
        // 确保子节点已加载 - 这是关键！必须先加载才能查找
        ensureChildrenLoaded(node);
        
        // 在子节点中查找匹配的节点
        System.out.println("[JenkinsBrowserDialog] Searching among " + node.getChildCount() + " children");
        logToConsole("Searching among " + node.getChildCount() + " children");
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            Object userObject = child.getUserObject();
            
            if (userObject instanceof JenkinsItem) {
                JenkinsItem item = (JenkinsItem) userObject;
                System.out.println("[JenkinsBrowserDialog]   Child[" + i + "]: [" + item.getName() + "]");
                logToConsole("  Checking child: " + item.getName());
                if (item.getName().equals(targetName)) {
                    System.out.println("[JenkinsBrowserDialog]   ✓ MATCH FOUND! Continuing to next level...");
                    logToConsole("  Found match! Continuing to next level...");
                    
                    // 关键修复：在递归到下一级之前，确保匹配的子节点的子节点也已加载
                    // 这样才能递归加载所有层级，而不是只加载第一层
                    if (index + 1 < jobNames.size()) {
                        // 还有更深的层级需要查找，确保当前匹配节点的子节点已加载
                        System.out.println("[JenkinsBrowserDialog]   Loading children of matched node before recursing...");
                        logToConsole("  Loading children of matched node before recursing...");
                        ensureChildrenLoaded(child);
                    }
                    
                    // 找到匹配的节点，继续查找下一级
                    return findNodeByJobNames(child, jobNames, index + 1);
                }
            }
        }
        
        // 未找到匹配的节点
        System.out.println("[JenkinsBrowserDialog] ✗ Could not find: [" + targetName + "]");
        logToConsole("Could not find: " + targetName);
        return null;
    }
    
    /**
     * 确保节点的子节点已经加载
     * 如果子节点是占位符"Loading..."，则同步加载实际的子节点
     */
    private void ensureChildrenLoaded(DefaultMutableTreeNode node) {
        System.out.println("[JenkinsBrowserDialog] ensureChildrenLoaded: node has " + node.getChildCount() + " children");
        if (node.getChildCount() == 1) {
            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) node.getChildAt(0);
            if ("Loading...".equals(firstChild.getUserObject())) {
                // 需要加载子节点
                Object userObject = node.getUserObject();
                if (userObject instanceof JenkinsItem) {
                    JenkinsItem item = (JenkinsItem) userObject;
                    if (item.isFolder()) {
                        System.out.println("[JenkinsBrowserDialog] Loading children for folder: " + item.getName());
                        logToConsole("Loading children for folder: " + item.getName());
                        // 同步加载子节点
                        loadChildrenSync(node, item);
                    }
                } else if (userObject instanceof String) {
                    // 根节点，需要确保已加载
                    System.out.println("[JenkinsBrowserDialog] Root node detected, children should already be loaded");
                    logToConsole("Root node detected, children should already be loaded");
                }
            } else {
                System.out.println("[JenkinsBrowserDialog] First child is not 'Loading...', it's: " + firstChild.getUserObject());
            }
        } else {
            System.out.println("[JenkinsBrowserDialog] Node has " + node.getChildCount() + " children, assuming already loaded");
        }
    }
    
    /**
     * 同步加载子节点（用于导航）
     */
    private void loadChildrenSync(DefaultMutableTreeNode parentNode, JenkinsItem parentItem) {
        try {
            String url = parentItem.getUrl();
            String jobPath = extractJobPath(url);
            
            List<JenkinsItem> items = apiClient.fetchJobHierarchy(jobPath);
            
            // 移除 "Loading..." 占位符
            parentNode.removeAllChildren();
            
            // 添加实际的子节点
            for (JenkinsItem item : items) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(item);
                parentNode.add(childNode);
                
                // 如果是文件夹，添加占位符
                if (item.isFolder()) {
                    childNode.add(new DefaultMutableTreeNode("Loading..."));
                }
            }
            
            treeModel.reload(parentNode);
            
        } catch (Exception e) {
            logToConsole("ERROR: Failed to load children for navigation: " + e.getMessage());
        }
    }
    
    /**
     * 打开当前选中节点的详情对话框
     * 如果选中的是job（非文件夹），则打开Build History
     */
    public void openSelectedJobDetails() {
        TreePath selectedPath = tree.getSelectionPath();
        if (selectedPath != null) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (node != null && node.getUserObject() instanceof JenkinsItem) {
                JenkinsItem item = (JenkinsItem) node.getUserObject();
                if (!item.isFolder()) {
                    System.out.println("[JenkinsBrowserDialog] Opening details for selected job: " + item.getName());
                    logToConsole("Opening Build History for: " + item.getName());
                    openJobDetails(item);
                }
            }
        }
    }
    
    /**
     * 自定义树渲染器，支持显示收藏标记
     */
    private class FavoriteTreeCellRenderer extends DefaultTreeCellRenderer {
        private java.util.Set<String> favoriteJobPaths;
        
        public FavoriteTreeCellRenderer() {
            super();
            favoriteJobPaths = new java.util.HashSet<>();
            
            // 初始化收藏列表
            updateFavorites(AppSettings.getInstance().getJenkinsFavorites());
        }
        
        /**
         * 更新收藏列表
         */
        public void updateFavorites(List<FavoriteJob> favorites) {
            favoriteJobPaths.clear();
            if (favorites != null) {
                for (FavoriteJob job : favorites) {
                    favoriteJobPaths.add(job.getJobPath());
                }
            }
        }
        
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                
                if (userObject instanceof JenkinsItem) {
                    JenkinsItem item = (JenkinsItem) userObject;
                    String jobPath = extractJobPath(item.getUrl());
                    
                    // 检查是否被收藏
                    boolean isFavorite = favoriteJobPaths.contains(jobPath);
                    
                    // Set text, add star if favorited
                    String displayName = item.getName();
                    if (isFavorite && !item.isFolder()) {
                        setText("[*] " + displayName);
                    } else {
                        setText(displayName);
                    }
                    
                    // 设置工具提示显示完整名称和路径
                    if (isFavorite && !item.isFolder()) {
                        setToolTipText("<html><b>[*] Favorited:</b> " + displayName + "<br><i>" + jobPath + "</i></html>");
                    } else {
                        setToolTipText("<html>" + displayName + "<br><i>" + jobPath + "</i></html>");
                    }
                    
                    // 设置图标
                    if (item.isFolder()) {
                        if (expanded) {
                            setIcon(UIManager.getIcon("Tree.openIcon"));
                        } else {
                            setIcon(UIManager.getIcon("Tree.closedIcon"));
                        }
                    } else {
                        // 不使用系统图标，避免显示乱码方块
                        setIcon(null);
                    }
                    
                    // 如果被收藏，使用金色前景色
                    if (isFavorite && !item.isFolder()) {
                        if (!selected) {
                            setForeground(new Color(218, 165, 32)); // 金色
                        }
                    }
                    
                    // 设置首选大小，确保文本不被截断
                    setPreferredSize(null); // 重置首选大小，让组件自动计算
                }
            }
            
            return this;
        }
    }
}
