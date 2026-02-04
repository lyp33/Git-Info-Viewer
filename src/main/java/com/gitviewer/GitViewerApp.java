package com.gitviewer;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.io.File;

/**
 * Git信息查看器主应用程序
 */
public class GitViewerApp extends JFrame {

    private DirectoryTreePanel directoryTreePanel;
    private InfoPanel infoPanel;
    private FileSearchDialog searchDialog;
    
    // 现代化配色方案
    private static final Color BACKGROUND_COLOR = new Color(250, 251, 252);
    private static final Color ACCENT_COLOR = new Color(66, 133, 244);
    private static final Color DIVIDER_COLOR = new Color(218, 220, 224);

    public GitViewerApp() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Git Info Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        
        // 添加窗口关闭监听器，确保资源清理
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                System.out.println("Application closing - cleaning up resources...");
                
                // 清理所有可能的资源
                try {
                    // 停止所有Timer
                    for (java.awt.Window window : java.awt.Window.getWindows()) {
                        if (window instanceof JDialog) {
                            window.dispose();
                        }
                    }
                    
                    // 强制退出
                    System.out.println("Forcing application exit...");
                    System.exit(0);
                } catch (Exception ex) {
                    System.err.println("Error during cleanup: " + ex.getMessage());
                    ex.printStackTrace();
                    // 即使清理失败也要退出
                    System.exit(1);
                }
            }
        });
        
        // 设置窗口图标
        try {
            // 尝试从resources目录加载图标
            java.net.URL iconURL = getClass().getResource("/icon.png");
            if (iconURL != null) {
                ImageIcon icon = new ImageIcon(iconURL);
                setIconImage(icon.getImage());
            }
        } catch (Exception e) {
            // 如果加载失败，使用默认图标
            System.err.println("Failed to load application icon: " + e.getMessage());
        }
        
        // 设置窗口背景色
        getContentPane().setBackground(BACKGROUND_COLOR);

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // 创建分割面板，使用现代化样式
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(450);
        splitPane.setResizeWeight(0.35);
        splitPane.setDividerSize(5); // 恢复为5像素，方便拖动
        splitPane.setBorder(null);
        splitPane.setBackground(BACKGROUND_COLOR);
        
        // 自定义分割线颜色
        splitPane.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(DIVIDER_COLOR);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
            }
        });

        // 创建左侧目录树面板 - 扁平边框
        directoryTreePanel = new DirectoryTreePanel();
        directoryTreePanel.setBackground(Color.WHITE);
        directoryTreePanel.setBorder(null); // 移除边框，使用扁平设计

        // 创建右侧信息面板 - 扁平边框
        infoPanel = new InfoPanel();
        infoPanel.setBackground(BACKGROUND_COLOR);
        infoPanel.setBorder(null); // 移除边框，使用扁平设计
        
        // 为左右面板添加监听器
        directoryTreePanel.addDirectorySelectionListener(selectedFile -> {
            // 当用户选择目录时，更新右侧信息面板
            infoPanel.displayInfo(selectedFile);
        });

        // 设置分割面板的左右组件
        splitPane.setLeftComponent(directoryTreePanel);
        splitPane.setRightComponent(infoPanel);

        // 添加到主面板
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        // 创建状态栏
        JPanel statusBar = createStatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        
        // 添加到主窗口
        add(mainPanel, BorderLayout.CENTER);

        // 创建菜单栏
        createMenuBar();
    }

    /**
     * 创建状态栏
     */
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(Color.WHITE);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER_COLOR),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // 作者信息标签 - 右对齐
        JLabel authorLabel = new JLabel("yunpeng.li / v2.0 / 20260113");
        authorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        authorLabel.setForeground(new Color(95, 99, 104));
        
        statusBar.add(authorLabel, BorderLayout.EAST);
        
        return statusBar;
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Color.WHITE);
        menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER_COLOR));
        
        // 设置菜单栏字体
        Font menuFont = new Font("Segoe UI", Font.PLAIN, 13);

        // 文件菜单
        JMenu fileMenu = new JMenu("File");
        fileMenu.setFont(menuFont);
        styleMenu(fileMenu);

        // 选择根目录菜单项
        JMenuItem selectRootItem = new JMenuItem("Select Root Directory...");
        selectRootItem.setFont(menuFont);
        selectRootItem.addActionListener(e -> selectRootDirectory());
        fileMenu.add(selectRootItem);

        fileMenu.addSeparator();

        // 设置菜单项
        JMenuItem settingsItem = new JMenuItem("Settings...");
        settingsItem.setFont(menuFont);
        settingsItem.addActionListener(e -> showSettingsDialog());
        fileMenu.add(settingsItem);

        fileMenu.addSeparator();

        // GitLab认证设置菜单项
        JMenuItem gitLabSettingsItem = new JMenuItem("GitLab Authentication...");
        gitLabSettingsItem.setFont(menuFont);
        gitLabSettingsItem.addActionListener(e -> showGitLabSettingsDialog());
        fileMenu.add(gitLabSettingsItem);

        fileMenu.addSeparator();

        // 清除Git认证信息菜单项
        JMenuItem clearCredentialsItem = new JMenuItem("Clear Git Credentials");
        clearCredentialsItem.setFont(menuFont);
        clearCredentialsItem.addActionListener(e -> clearGitCredentials());
        fileMenu.add(clearCredentialsItem);

        fileMenu.addSeparator();

        // 退出菜单项
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setFont(menuFont);
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        // 搜索菜单
        JMenu searchMenu = new JMenu("Search");
        searchMenu.setFont(menuFont);
        styleMenu(searchMenu);
        
        JMenuItem searchFilesItem = new JMenuItem("Search Files...");
        searchFilesItem.setFont(menuFont);
        searchFilesItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        searchFilesItem.addActionListener(e -> showSearchDialog());
        searchMenu.add(searchFilesItem);
        
        menuBar.add(searchMenu);

        // Chat 菜单
        JMenu chatMenu = new JMenu("Chat");
        chatMenu.setFont(menuFont);
        styleMenu(chatMenu);
        
        JMenuItem openChatItem = new JMenuItem("Open AI Chat...");
        openChatItem.setFont(menuFont);
        openChatItem.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        openChatItem.addActionListener(e -> showChatDialog());
        chatMenu.add(openChatItem);
        
        chatMenu.addSeparator();
        
        JMenuItem aiSettingsItem = new JMenuItem("AI Settings...");
        aiSettingsItem.setFont(menuFont);
        aiSettingsItem.addActionListener(e -> showAISettingsDialog());
        chatMenu.add(aiSettingsItem);
        
        menuBar.add(chatMenu);

        // CI/CD 菜单
        JMenu cicdMenu = new JMenu("CI/CD");
        cicdMenu.setFont(menuFont);
        styleMenu(cicdMenu);
        
        JMenuItem coreSdkBuildItem = new JMenuItem("Core/SDK Build...");
        coreSdkBuildItem.setFont(menuFont);
        coreSdkBuildItem.addActionListener(e -> showJenkinsBrowserDialog());
        cicdMenu.add(coreSdkBuildItem);
        
        cicdMenu.addSeparator();
        
        JMenuItem tenantCICDItem = new JMenuItem("Tenant CI/CD...");
        tenantCICDItem.setFont(menuFont);
        tenantCICDItem.addActionListener(e -> showTenantCICDDialog());
        cicdMenu.add(tenantCICDItem);
        
        cicdMenu.addSeparator();
        
        JMenuItem jenkinsSettingsItem = new JMenuItem("Jenkins Settings...");
        jenkinsSettingsItem.setFont(menuFont);
        jenkinsSettingsItem.addActionListener(e -> showJenkinsSettingsDialog());
        cicdMenu.add(jenkinsSettingsItem);
        
        JMenuItem portalSettingsItem = new JMenuItem("Portal Settings...");
        portalSettingsItem.setFont(menuFont);
        portalSettingsItem.addActionListener(e -> showPortalSettingsDialog());
        cicdMenu.add(portalSettingsItem);
        
        menuBar.add(cicdMenu);

        // 帮助菜单
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setFont(menuFont);
        styleMenu(helpMenu);
        
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.setFont(menuFont);
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }
    
    /**
     * 美化菜单样式
     */
    private void styleMenu(JMenu menu) {
        menu.setOpaque(true);
        menu.setBackground(Color.WHITE);
        menu.setForeground(new Color(60, 64, 67));
    }

    private void clearGitCredentials() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "This will clear all saved Git credentials.\nYou will need to re-enter your credentials for the next Git operation.\n\nAre you sure?",
            "Clear Git Credentials",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            // 清除所有认证信息
            GitOperations.clearAllCredentials();
            
            JOptionPane.showMessageDialog(
                this,
                "Git credentials have been cleared successfully.\nYou will be prompted for credentials on the next Git operation.",
                "Credentials Cleared",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    private void showSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(this);
        dialog.setVisible(true);
    }

    private void showGitLabSettingsDialog() {
        GitLabSettingsDialog dialog = new GitLabSettingsDialog(this);
        dialog.setVisible(true);
    }

    private void selectRootDirectory() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setDialogTitle("Select Root Directory");

        // 设置文件选择器的字体为正常大小
        Font chooserFont = new Font("Segoe UI", Font.PLAIN, 12);
        applyFontRecursive(fileChooser, chooserFont);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedDir = fileChooser.getSelectedFile();
            directoryTreePanel.setRootDirectory(selectedDir);
            infoPanel.clearInfo();
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

    private void showAboutDialog() {
        String message = "Git Info Viewer v1.0\n\n" +
                "A tool to view git repository information\n" +
                "including remotes, branches, and commit history.";
        JOptionPane.showMessageDialog(this, message, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 显示搜索对话框
     */
    private void showSearchDialog() {
        File rootDir = directoryTreePanel.getRootDirectory();
        if (rootDir == null) {
            JOptionPane.showMessageDialog(
                this,
                "Please select a root directory first.\n(File -> Select Root Directory...)",
                "No Directory Selected",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        
        // 如果搜索对话框不存在或已关闭，创建新的
        if (searchDialog == null || !searchDialog.isVisible()) {
            searchDialog = new FileSearchDialog(this, rootDir);
            searchDialog.setFileSelectionListener(file -> {
                // 在左侧树中定位并选中文件
                directoryTreePanel.selectAndRevealFile(file);
                // 在右侧面板显示文件信息
                infoPanel.displayInfo(file);
                // 关闭搜索对话框
                searchDialog.dispose();
            });
            searchDialog.setVisible(true);
        } else {
            // 如果对话框已存在，只是将其置于前台
            searchDialog.toFront();
            searchDialog.requestFocus();
        }
    }

    /**
     * 显示 AI 聊天对话框
     */
    private void showChatDialog() {
        // 获取当前选中的目录
        File selectedDir = directoryTreePanel.getSelectedDirectory();
        AIChatDialog dialog = new AIChatDialog(this, selectedDir);
        dialog.setVisible(true);
    }

    /**
     * 显示 AI 设置对话框
     */
    private void showAISettingsDialog() {
        AISettingsDialog dialog = new AISettingsDialog(this);
        dialog.setVisible(true);
    }

    /**
     * 显示 Jenkins 浏览器对话框
     */
    private void showJenkinsBrowserDialog() {
        JenkinsBrowserDialog dialog = new JenkinsBrowserDialog(this);
        dialog.setVisible(true);
    }

    /**
     * 显示 Tenant CI/CD 对话框
     */
    private void showTenantCICDDialog() {
        TenantCICDDialog dialog = new TenantCICDDialog(this);
        dialog.setVisible(true);
    }

    /**
     * 显示 Jenkins 设置对话框
     */
    private void showJenkinsSettingsDialog() {
        JenkinsSettingsDialog dialog = new JenkinsSettingsDialog(this);
        dialog.setVisible(true);
    }

    /**
     * 显示 Portal 设置对话框
     */
    private void showPortalSettingsDialog() {
        PortalSettingsDialog dialog = new PortalSettingsDialog(this);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        // 打印版本信息
        System.out.println("========================================");
        System.out.println("Git Info Viewer - Version 2026-01-20-17:20");
        System.out.println("Build: Find LAST Portal URL Match");
        System.out.println("========================================");
        
        // 设置系统外观
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            GitViewerApp app = new GitViewerApp();
            app.setVisible(true);
        });
    }
}
