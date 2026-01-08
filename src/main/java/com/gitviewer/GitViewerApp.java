package com.gitviewer;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Git信息查看器主应用程序
 */
public class GitViewerApp extends JFrame {

    private DirectoryTreePanel directoryTreePanel;
    private InfoPanel infoPanel;

    public GitViewerApp() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Git Info Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // 创建主面板，使用分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setResizeWeight(0.4);

        // 创建左侧目录树面板
        directoryTreePanel = new DirectoryTreePanel();
        directoryTreePanel.addDirectorySelectionListener(selectedFile -> {
            // 当用户选择目录时，更新右侧信息面板
            infoPanel.displayInfo(selectedFile);
        });

        // 创建右侧信息面板
        infoPanel = new InfoPanel();

        // 设置分割面板的左右组件
        splitPane.setLeftComponent(directoryTreePanel);
        splitPane.setRightComponent(infoPanel);

        // 添加到主窗口
        add(splitPane, BorderLayout.CENTER);

        // 创建菜单栏
        createMenuBar();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // 文件菜单
        JMenu fileMenu = new JMenu("File");

        // 选择根目录菜单项
        JMenuItem selectRootItem = new JMenuItem("Select Root Directory...");
        selectRootItem.addActionListener(e -> selectRootDirectory());
        fileMenu.add(selectRootItem);

        fileMenu.addSeparator();

        // 设置菜单项
        JMenuItem settingsItem = new JMenuItem("Settings...");
        settingsItem.addActionListener(e -> showSettingsDialog());
        fileMenu.add(settingsItem);

        fileMenu.addSeparator();

        // GitLab认证设置菜单项
        JMenuItem gitLabSettingsItem = new JMenuItem("GitLab Authentication...");
        gitLabSettingsItem.addActionListener(e -> showGitLabSettingsDialog());
        fileMenu.add(gitLabSettingsItem);

        fileMenu.addSeparator();

        // 清除Git认证信息菜单项
        JMenuItem clearCredentialsItem = new JMenuItem("Clear Git Credentials");
        clearCredentialsItem.addActionListener(e -> clearGitCredentials());
        fileMenu.add(clearCredentialsItem);

        fileMenu.addSeparator();

        // 退出菜单项
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        // 帮助菜单
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
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

    public static void main(String[] args) {
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
