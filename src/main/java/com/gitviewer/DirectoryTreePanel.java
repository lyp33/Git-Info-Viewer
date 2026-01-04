package com.gitviewer;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;

/**
 * 左侧目录树面板
 * 显示文件系统的目录结构
 */
public class DirectoryTreePanel extends JPanel {

    private JTree tree;
    private DefaultTreeModel treeModel;
    private File rootDirectory;
    private DirectorySelectionListener selectionListener;
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
        // 创建顶部标签
        JLabel titleLabel = new JLabel("Directory Tree");
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(titleLabel, BorderLayout.NORTH);

        // 创建树组件
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Select a directory");
        treeModel = new DefaultTreeModel(root);
        tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);

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

        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(tree);
        add(scrollPane, BorderLayout.CENTER);

        // 添加底部路径输入框
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel pathLabel = new JLabel("Root Path:");
        pathLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bottomPanel.add(pathLabel, BorderLayout.WEST);

        pathTextField = new JTextField();
        pathTextField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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
     * 设置目录选择监听器
     */
    public void addDirectorySelectionListener(DirectorySelectionListener listener) {
        this.selectionListener = listener;
    }

    /**
     * 目录选择监听器接口
     */
    @FunctionalInterface
    public interface DirectorySelectionListener {
        void onDirectorySelected(File directory);
    }
}
