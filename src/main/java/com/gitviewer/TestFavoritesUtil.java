package com.gitviewer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 测试工具：用于在本地测试环境中添加测试收藏数据
 * 不需要连接到真实的 Jenkins 服务器
 */
public class TestFavoritesUtil {
    
    /**
     * 创建测试收藏数据文件
     */
    public static void createTestFavorites() {
        List<FavoriteJob> testFavorites = new ArrayList<>();
        
        // 添加测试数据
        testFavorites.add(new FavoriteJob(
            "gemini/job/Manual-Build/job/all-in-one-auto-CI",
            "all-in-one-auto-CI",
            "https://ci.jenkins.io/job/gemini/job/Manual-Build/job/all-in-one-auto-CI",
            0
        ));
        
        testFavorites.add(new FavoriteJob(
            "gemini/job/Test-Job/job/backend-service",
            "backend-service",
            "https://ci.jenkins.io/job/gemini/job/Test-Job/job/backend-service",
            1
        ));
        
        testFavorites.add(new FavoriteJob(
            "gemini/job/Deploy/job/production-deploy",
            "production-deploy",
            "https://ci.jenkins.io/job/gemini/job/Deploy/job/production-deploy",
            2
        ));
        
        // 保存到文件
        File file = new File(System.getProperty("user.home"), "gitviewer-jenkins-favorites.dat");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(testFavorites);
            System.out.println("✓ 测试收藏数据已创建: " + file.getAbsolutePath());
            System.out.println("✓ 添加了 " + testFavorites.size() + " 个测试收藏");
            for (FavoriteJob job : testFavorites) {
                System.out.println("  - " + job.getDisplayName() + " (" + job.getJobPath() + ")");
            }
        } catch (IOException e) {
            System.err.println("✗ 创建测试数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 显示当前收藏数据
     */
    @SuppressWarnings("unchecked")
    public static void showCurrentFavorites() {
        File file = new File(System.getProperty("user.home"), "gitviewer-jenkins-favorites.dat");
        if (!file.exists()) {
            System.out.println("收藏数据文件不存在: " + file.getAbsolutePath());
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<FavoriteJob> favorites = (List<FavoriteJob>) ois.readObject();
            System.out.println("当前收藏数量: " + favorites.size());
            for (FavoriteJob job : favorites) {
                System.out.println("  - " + job.getDisplayName() + " (" + job.getJobPath() + ")");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("读取收藏数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 删除收藏数据文件
     */
    public static void deleteFavoritesFile() {
        File file = new File(System.getProperty("user.home"), "gitviewer-jenkins-favorites.dat");
        if (file.exists()) {
            if (file.delete()) {
                System.out.println("✓ 收藏数据文件已删除: " + file.getAbsolutePath());
            } else {
                System.out.println("✗ 删除收藏数据文件失败");
            }
        } else {
            System.out.println("收藏数据文件不存在");
        }
    }
    
    /**
     * 显示测试加载对话框
     */
    public static void showTestLoadingDialog() {
        JFrame frame = new JFrame("Test Loading Dialog - 测试加载对话框");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 300);
        frame.setLocationRelativeTo(null);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 说明文字
        JTextArea instructions = new JTextArea(
            "点击下面的按钮测试 Loading 对话框\n\n" +
            "测试场景：\n" +
            "1. 快速测试（0.5秒） - 可能看不到对话框\n" +
            "2. 正常测试（2秒） - 应该能看到对话框\n" +
            "3. 慢速测试（5秒） - 清楚看到对话框\n\n" +
            "对话框特性：\n" +
            "- 模态对话框（阻止其他操作）\n" +
            "- 不可关闭（用户无法点击X）\n" +
            "- 进度条动画\n" +
            "- 自动关闭"
        );
        instructions.setEditable(false);
        instructions.setBackground(mainPanel.getBackground());
        instructions.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        mainPanel.add(instructions, BorderLayout.NORTH);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        
        JButton fastButton = new JButton("快速测试 (0.5秒)");
        JButton normalButton = new JButton("正常测试 (2秒)");
        JButton slowButton = new JButton("慢速测试 (5秒)");
        
        fastButton.addActionListener(e -> testLoadingDialog(frame, 500));
        normalButton.addActionListener(e -> testLoadingDialog(frame, 2000));
        slowButton.addActionListener(e -> testLoadingDialog(frame, 5000));
        
        buttonPanel.add(fastButton);
        buttonPanel.add(normalButton);
        buttonPanel.add(slowButton);
        
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        
        frame.add(mainPanel);
        frame.setVisible(true);
    }
    
    /**
     * 测试加载对话框
     */
    private static void testLoadingDialog(JFrame parent, int delayMs) {
        System.out.println("=== 开始测试 Loading 对话框 ===");
        System.out.println("延迟时间: " + delayMs + "ms");
        
        // 创建模态加载对话框
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog loadingDialog = new JDialog(owner, "Loading", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel label = new JLabel("Loading... please wait");
        label.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        panel.add(label, BorderLayout.CENTER);
        
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        panel.add(progressBar, BorderLayout.SOUTH);
        
        loadingDialog.add(panel);
        loadingDialog.setSize(300, 120);
        loadingDialog.setLocationRelativeTo(parent);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        System.out.println("Loading 对话框已创建");
        
        // 模拟后台任务
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                System.out.println("后台任务开始...");
                Thread.sleep(delayMs);
                System.out.println("后台任务完成");
                return null;
            }
            
            @Override
            protected void done() {
                System.out.println("关闭 Loading 对话框");
                loadingDialog.dispose();
                JOptionPane.showMessageDialog(parent, 
                    "Loading 完成！\n延迟时间: " + delayMs + "ms", 
                    "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        };
        
        System.out.println("启动后台任务...");
        worker.execute();
        System.out.println("显示 Loading 对话框...");
        loadingDialog.setVisible(true);
        System.out.println("Loading 对话框已关闭");
    }
    
    /**
     * 主函数 - 提供命令行界面
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        String command = args[0].toLowerCase();
        switch (command) {
            case "create":
                createTestFavorites();
                break;
            case "show":
                showCurrentFavorites();
                break;
            case "delete":
                deleteFavoritesFile();
                break;
            case "test-dialog":
                SwingUtilities.invokeLater(TestFavoritesUtil::showTestLoadingDialog);
                break;
            default:
                System.out.println("未知命令: " + command);
                printUsage();
        }
    }
    
    private static void printUsage() {
        System.out.println("测试收藏工具 - 用法:");
        System.out.println("  java -cp target/git-info-viewer-1.0.0-jar-with-dependencies.jar com.gitviewer.TestFavoritesUtil <command>");
        System.out.println();
        System.out.println("命令:");
        System.out.println("  create       - 创建测试收藏数据");
        System.out.println("  show         - 显示当前收藏数据");
        System.out.println("  delete       - 删除收藏数据文件");
        System.out.println("  test-dialog  - 测试加载对话框");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -cp target/git-info-viewer-1.0.0-jar-with-dependencies.jar com.gitviewer.TestFavoritesUtil create");
    }
}
