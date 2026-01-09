package com.gitviewer;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 左侧目录树面板
 * 显示文件系统的目录结构
 */
public class DirectoryTreePanel extends JPanel {

    private JTree tree;
    private DefaultTreeModel treeModel;
    private File rootDirectory;
    private DirectorySelectionListener selectionListener;
    private TreeRefreshListener refreshListener;
    JTextField pathTextField;

    public DirectoryTreePanel() {
        setLayout(new BorderLayout());
        initializeComponents();
        loadFontSettings();

        // 注册字体变化监听器
        AppSettings.getInstance().addFontChangeListener((leftFont, rightFont) -> {
            updateFont(leftFont);
        });
    }

    private void loadFontSettings() {
        Font font = AppSettings.getInstance().getLeftPanelFont();
        updateFont(font);
    }

    private void updateFont(Font font) {
        if (tree != null) {
            tree.setFont(font);
        }
        if (pathTextField != null) {
            pathTextField.setFont(font);
        }
        // 更新标题标签使用相同的字体
        Component[] components = getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel) {
                comp.setFont(font);
            }
        }
    }

    private void initializeComponents() {
        setBackground(new Color(255, 255, 255));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建顶部标签 - 现代化样式
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(255, 255, 255));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        JLabel titleLabel = new JLabel("Directory Tree");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(32, 33, 36));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        add(headerPanel, BorderLayout.NORTH);

        // 创建树组件
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Select a directory");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true); // 显示展开/折叠图标
        tree.setBackground(new Color(255, 255, 255));
        tree.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tree.setRowHeight(28);
        tree.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 设置自定义渲染器，只显示文件名/目录名
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                    boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                
                // 调用父类方法获取默认组件
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                
                // 设置字体
                setFont(new Font("Segoe UI", Font.PLAIN, 13));
                
                // 设置颜色 - 确保文字可见
                if (selected) {
                    setBackgroundSelectionColor(new Color(232, 240, 254));
                    setTextSelectionColor(new Color(26, 115, 232));
                    setBorderSelectionColor(null);
                    setBackground(new Color(232, 240, 254));
                    setForeground(new Color(26, 115, 232));
                } else {
                    setBackgroundNonSelectionColor(Color.WHITE);
                    setTextNonSelectionColor(new Color(60, 64, 67));
                    setBackground(Color.WHITE);
                    setForeground(new Color(60, 64, 67));
                }
                
                // 设置为不透明，确保背景色显示
                setOpaque(true);
                
                // 如果节点包含 File 对象，只显示文件名
                if (value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                    Object userObject = node.getUserObject();
                    
                    if (userObject instanceof File) {
                        File file = (File) userObject;
                        setText(file.getName());
                        
                        // 设置图标
                        if (file.isDirectory()) {
                            if (expanded) {
                                setIcon(UIManager.getIcon("Tree.openIcon"));
                            } else {
                                setIcon(UIManager.getIcon("Tree.closedIcon"));
                            }
                        } else {
                            setIcon(UIManager.getIcon("Tree.leafIcon"));
                        }
                    } else {
                        // 对于非 File 对象（如 "Loading..." 或根节点），使用默认显示
                        setText(userObject.toString());
                    }
                }
                
                return this;
            }
        });

        // Note: SINGLE_TREE_SELECTION is the default mode for JTree
        // tree.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        // 添加选择监听器
        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if (node == null) return;

            Object userObject = node.getUserObject();
            if (userObject instanceof File) {
                File selectedFile = (File) userObject;
                if (selectionListener != null) {
                    selectionListener.onDirectorySelected(selectedFile);
                }
            }
        });

        // 添加树展开监听器，懒加载子节点
        tree.addTreeWillExpandListener(new javax.swing.event.TreeWillExpandListener() {
            @Override
            public void treeWillExpand(javax.swing.event.TreeExpansionEvent event) throws javax.swing.tree.ExpandVetoException {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                Object userObject = node.getUserObject();
                
                if (userObject instanceof File) {
                    File directory = (File) userObject;
                    
                    // 检查是否有 "Loading..." 占位节点
                    if (node.getChildCount() == 1) {
                        DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) node.getChildAt(0);
                        if ("Loading...".equals(firstChild.getUserObject())) {
                            // 移除占位节点
                            treeModel.removeNodeFromParent(firstChild);
                            // 加载实际的子节点
                            loadChildren(node, directory);
                            // 刷新节点
                            treeModel.nodeStructureChanged(node);
                        }
                    }
                }
            }

            @Override
            public void treeWillCollapse(javax.swing.event.TreeExpansionEvent event) throws javax.swing.tree.ExpandVetoException {
                // 不需要处理折叠事件
            }
        });

        // 添加右键菜单
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
        });

        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1)); // 使用白色边框
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // 添加底部路径输入框 - 现代化样式
        JPanel bottomPanel = new JPanel(new BorderLayout(8, 0));
        bottomPanel.setBackground(new Color(255, 255, 255));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10)); // 左右各10像素边距

        // 移除了文件夹图标

        pathTextField = new JTextField();
        pathTextField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pathTextField.setBackground(new Color(248, 249, 250));
        pathTextField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.WHITE, 1), // 白色边框
            BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));
        pathTextField.setPreferredSize(new Dimension(0, 28)); // 缩短高度到28像素
        pathTextField.setToolTipText("Enter directory path and press Enter to navigate");
        // 添加回车键监听
        pathTextField.addActionListener(e -> navigateToPath());
        bottomPanel.add(pathTextField, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 导航到用户输入的路径
     */
    private void navigateToPath() {
        String pathText = pathTextField.getText().trim();
        if (pathText.isEmpty()) {
            return;
        }

        File targetDir = new File(pathText);
        if (!targetDir.exists()) {
            JOptionPane.showMessageDialog(this,
                "Directory does not exist: " + pathText,
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!targetDir.isDirectory()) {
            JOptionPane.showMessageDialog(this,
                "Path is not a directory: " + pathText,
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 设置为新的根目录
        setRootDirectory(targetDir);

        // 触发选择事件，更新右侧面板
        if (selectionListener != null) {
            selectionListener.onDirectorySelected(targetDir);
        }
    }

    /**
     * 获取根目录
     */
    public File getRootDirectory() {
        return rootDirectory;
    }

    /**
     * 设置根目录
     */
    public void setRootDirectory(File directory) {
        this.rootDirectory = directory;

        // 更新路径文本框
        if (pathTextField != null) {
            pathTextField.setText(directory.getAbsolutePath());
        }

        // 清空并重新构建树
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(directory);
        treeModel.setRoot(root);
        buildTree(root, directory, 0);

        // 展开第一层节点
        tree.expandRow(0);
    }

    /**
     * 递归构建目录树
     */
    private void buildTree(DefaultMutableTreeNode parentNode, File parentFile, int depth) {
        // 限制递归深度，避免性能问题
        if (depth > 5) {
            return;
        }

        File[] children = parentFile.listFiles();
        if (children == null) {
            return;
        }

        // 排序：目录在前，文件在后
        java.util.Arrays.sort(children, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1;
            } else if (!f1.isDirectory() && f2.isDirectory()) {
                return 1;
            } else {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });

        for (File child : children) {
            // 跳过隐藏文件和.git目录
            if (child.getName().startsWith(".")) {
                continue;
            }

            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
            treeModel.insertNodeInto(childNode, parentNode, parentNode.getChildCount());

            // 如果是目录，递归添加子节点
            if (child.isDirectory()) {
                // 只展开第一层
                if (depth == 0) {
                    buildTree(childNode, child, depth + 1);
                } else {
                    // 添加一个占位节点，表示该目录有子节点
                    treeModel.insertNodeInto(new DefaultMutableTreeNode("Loading..."), childNode, 0);
                }
            }
        }
    }

    /**
     * 懒加载子节点（当用户展开节点时调用）
     */
    private void loadChildren(DefaultMutableTreeNode parentNode, File parentFile) {
        File[] children = parentFile.listFiles();
        if (children == null) {
            return;
        }

        // 排序：目录在前，文件在后
        java.util.Arrays.sort(children, (f1, f2) -> {
            if (f1.isDirectory() && !f2.isDirectory()) {
                return -1;
            } else if (!f1.isDirectory() && f2.isDirectory()) {
                return 1;
            } else {
                return f1.getName().compareToIgnoreCase(f2.getName());
            }
        });

        for (File child : children) {
            // 跳过隐藏文件和.git目录
            if (child.getName().startsWith(".")) {
                continue;
            }

            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
            treeModel.insertNodeInto(childNode, parentNode, parentNode.getChildCount());

            // 如果是目录，添加占位节点
            if (child.isDirectory()) {
                treeModel.insertNodeInto(new DefaultMutableTreeNode("Loading..."), childNode, 0);
            }
        }
    }

    /**
     * 设置目录选择监听器
     */
    public void addDirectorySelectionListener(DirectorySelectionListener listener) {
        this.selectionListener = listener;
    }

    /**
     * 设置树刷新监听器
     */
    public void addTreeRefreshListener(TreeRefreshListener listener) {
        this.refreshListener = listener;
    }

    /**
     * 显示右键菜单
     */
    private void showPopupMenu(MouseEvent e) {
        int row = tree.getRowForLocation(e.getX(), e.getY());
        if (row < 0) return;

        // 选中右键点击的节点
        tree.setSelectionRow(row);

        TreePath path = tree.getPathForRow(row);
        if (path == null) return;

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        Object userObject = node.getUserObject();

        if (!(userObject instanceof File)) return;

        File selectedFile = (File) userObject;

        // 创建右键菜单
        JPopupMenu popupMenu = new JPopupMenu();

        if (selectedFile.isFile()) {
            // 文件的右键菜单
            
            // Open 菜单项 - 用默认程序打开文件
            JMenuItem openItem = new JMenuItem("Open");
            openItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            openItem.addActionListener(event -> {
                openFile(selectedFile);
            });
            popupMenu.add(openItem);

            // Open Folder 菜单项 - 打开文件所在的目录
            JMenuItem openFolderItem = new JMenuItem("Open Folder");
            openFolderItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            openFolderItem.addActionListener(event -> {
                openFolder(selectedFile.getParentFile());
            });
            popupMenu.add(openFolderItem);

        } else if (selectedFile.isDirectory()) {
            // 目录的右键菜单
            
            // Open Folder 菜单项 - 打开目录
            JMenuItem openFolderItem = new JMenuItem("Open Folder");
            openFolderItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            openFolderItem.addActionListener(event -> {
                openFolder(selectedFile);
            });
            popupMenu.add(openFolderItem);

            popupMenu.addSeparator();

            // New Folder 菜单项
            JMenuItem newFolderItem = new JMenuItem("New Folder");
            newFolderItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            newFolderItem.addActionListener(event -> {
                showNewFolderDialog(node, selectedFile);
            });
            popupMenu.add(newFolderItem);

            // Checkout New Git Project 菜单项
            JMenuItem checkoutItem = new JMenuItem("Checkout New Git Project");
            checkoutItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            checkoutItem.addActionListener(event -> {
                showCheckoutDialog(selectedFile);
            });
            popupMenu.add(checkoutItem);

            popupMenu.addSeparator();

            // 刷新菜单项
            JMenuItem refreshItem = new JMenuItem("Refresh");
            refreshItem.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            refreshItem.addActionListener(event -> {
                refreshNode(node, selectedFile);
            });
            popupMenu.add(refreshItem);
        }

        popupMenu.show(tree, e.getX(), e.getY());
    }

    /**
     * 用默认程序打开文件
     */
    private void openFile(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(file);
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Desktop OPEN action is not supported on this platform.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Desktop is not supported on this platform.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Failed to open file: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * 在文件资源管理器中打开文件夹
     */
    private void openFolder(File folder) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(folder);
                } else {
                    JOptionPane.showMessageDialog(
                        this,
                        "Desktop OPEN action is not supported on this platform.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Desktop is not supported on this platform.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                this,
                "Failed to open folder: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * 显示新建文件夹对话框
     */
    private void showNewFolderDialog(DefaultMutableTreeNode node, File parentDirectory) {
        // 创建自定义对话框
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "New Folder", true);
        dialog.setSize(400, 150);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        JLabel label = new JLabel("Enter folder name:");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        mainPanel.add(label, BorderLayout.NORTH);

        JTextField folderNameField = new JTextField();
        folderNameField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        folderNameField.setPreferredSize(new Dimension(350, 30));
        mainPanel.add(folderNameField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton submitBtn = new JButton("Submit");
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        submitBtn.setBackground(new Color(25, 84, 166));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);
        submitBtn.setBorderPainted(false);
        submitBtn.setOpaque(true);
        submitBtn.addActionListener(e -> {
            String folderName = folderNameField.getText().trim();
            if (createNewFolder(node, parentDirectory, folderName)) {
                dialog.dispose();
            }
        });

        // 回车键提交
        folderNameField.addActionListener(e -> submitBtn.doClick());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(submitBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    /**
     * 创建新文件夹
     */
    private boolean createNewFolder(DefaultMutableTreeNode node, File parentDirectory, String folderName) {
        if (folderName == null || folderName.isEmpty()) {
            JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(this),
                "Please enter a folder name.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        // 检查文件名是否合法
        if (folderName.contains("/") || folderName.contains("\\") || 
            folderName.contains(":") || folderName.contains("*") ||
            folderName.contains("?") || folderName.contains("\"") ||
            folderName.contains("<") || folderName.contains(">") ||
            folderName.contains("|")) {
            JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(this),
                "Invalid folder name. Cannot contain: / \\ : * ? \" < > |",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        File newFolder = new File(parentDirectory, folderName);
        
        if (newFolder.exists()) {
            JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(this),
                "Folder '" + folderName + "' already exists.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }

        if (newFolder.mkdir()) {
            // 刷新节点
            refreshNode(node, parentDirectory);
            JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(this),
                "Folder '" + folderName + "' created successfully.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
            );
            return true;
        } else {
            JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(this),
                "Failed to create folder.",
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
    }

    /**
     * 显示 Checkout Git 项目对话框
     */
    private void showCheckoutDialog(File targetDirectory) {
        Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
        CheckoutGitProjectDialog dialog = new CheckoutGitProjectDialog(parentFrame, targetDirectory);
        dialog.setVisible(true);

        // 如果克隆成功，刷新目录树
        if (dialog.isCheckoutSuccess()) {
            // 刷新当前目录
            if (rootDirectory != null) {
                setRootDirectory(rootDirectory);
            }
            // 通知刷新监听器
            if (refreshListener != null) {
                refreshListener.onTreeRefreshed();
            }
        }
    }

    /**
     * 刷新指定节点
     */
    private void refreshNode(DefaultMutableTreeNode node, File directory) {
        // 移除所有子节点
        node.removeAllChildren();
        
        // 重新构建子树
        buildTree(node, directory, 0);
        
        // 通知模型更新
        treeModel.reload(node);
        
        // 通知刷新监听器
        if (refreshListener != null) {
            refreshListener.onTreeRefreshed();
        }
    }

    /**
     * 目录选择监听器接口
     */
    @FunctionalInterface
    public interface DirectorySelectionListener {
        void onDirectorySelected(File directory);
    }

    /**
     * 树刷新监听器接口
     */
    @FunctionalInterface
    public interface TreeRefreshListener {
        void onTreeRefreshed();
    }

    /**
     * 在树中选中并展开到指定文件
     */
    public void selectAndRevealFile(File targetFile) {
        if (targetFile == null || !targetFile.exists()) {
            return;
        }

        // 构建从根目录到目标文件的路径
        List<File> pathToFile = new ArrayList<>();
        File current = targetFile;
        
        while (current != null && !current.equals(rootDirectory)) {
            pathToFile.add(0, current);
            current = current.getParentFile();
        }
        
        if (current == null) {
            // 目标文件不在根目录下
            return;
        }
        
        // 从根节点开始，逐级展开并查找
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) treeModel.getRoot();
        TreePath currentPath = new TreePath(currentNode);
        
        for (File fileInPath : pathToFile) {
            // 确保当前节点已加载子节点
            if (currentNode.getChildCount() == 1) {
                DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) currentNode.getChildAt(0);
                if ("Loading...".equals(firstChild.getUserObject())) {
                    // 移除占位节点并加载实际子节点
                    treeModel.removeNodeFromParent(firstChild);
                    Object userObject = currentNode.getUserObject();
                    if (userObject instanceof File) {
                        loadChildren(currentNode, (File) userObject);
                    }
                }
            }
            
            // 在子节点中查找匹配的文件
            boolean found = false;
            for (int i = 0; i < currentNode.getChildCount(); i++) {
                DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) currentNode.getChildAt(i);
                Object userObject = childNode.getUserObject();
                
                if (userObject instanceof File) {
                    File childFile = (File) userObject;
                    if (childFile.equals(fileInPath)) {
                        currentNode = childNode;
                        currentPath = currentPath.pathByAddingChild(childNode);
                        found = true;
                        break;
                    }
                }
            }
            
            if (!found) {
                // 无法找到路径中的某个节点
                return;
            }
        }
        
        // 展开并选中目标节点
        tree.expandPath(currentPath);
        tree.setSelectionPath(currentPath);
        tree.scrollPathToVisible(currentPath);
        
        // 触发选择事件
        if (selectionListener != null) {
            selectionListener.onDirectorySelected(targetFile);
        }
    }
}
