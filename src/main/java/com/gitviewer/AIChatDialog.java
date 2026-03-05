package com.gitviewer;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * AI 聊天对话框
 */
public class AIChatDialog extends JDialog {

    private JTextPane chatPane;
    private StyledDocument chatDoc;
    private JTextField inputField;
    private JButton sendButton;
    private JComboBox<String> branchComboBox;  // 分支下拉框（可编辑，支持筛选）
    private List<String> allBranches;  // 所有分支列表
    private String currentBranch;  // 当前选择的分支
    private boolean isFilteringBranches = false;  // 标志位：是否正在筛选分支
    private List<AIService.ChatMessage> chatHistory;
    private AIService aiService;
    private String gitToken;
    private GitApiClient gitApiClient;  // 统一的 Git API 客户端
    private GitToolRegistry toolRegistry;  // Tool 注册表
    private File currentDirectory;
    private String currentOwner;
    private String currentRepo;
    private String currentRemoteUrl;  // 添加 remote URL 字段
    private int lastSystemMessageStart = -1;  // 跟踪最后一条系统消息的起始位置
    private int lastSystemMessageEnd = -1;    // 跟踪最后一条系统消息的结束位置
    
    // Agent 模式相关
    private int currentIteration = 0;  // 当前循环轮次
    private StringBuilder collectedData = new StringBuilder();  // 已收集的数据
    
    // 样式
    private Style userStyle;
    private Style assistantStyle;
    private Style systemStyle;
    private Style timestampStyle;

    public AIChatDialog(Frame parent, File selectedDirectory) {
        super(parent, "AI Chat - Git Assistant", false);
        this.currentDirectory = selectedDirectory;
        this.allBranches = new ArrayList<>();
        chatHistory = new ArrayList<>();
        extractGitInfo();
        initializeUI();
        initializeStyles();
        initializeAIService();
        setLocationRelativeTo(parent);
        
        // 添加系统提示
        addSystemMessage();
        
        // 显示欢迎消息
        showWelcomeMessage();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(800, 700);

        // 聊天显示区域 - 使用 JTextPane 支持富文本
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        chatPane.setMargin(new Insets(10, 10, 10, 10));
        chatPane.setBackground(new Color(245, 245, 245));
        chatDoc = chatPane.getStyledDocument();

        JScrollPane scrollPane = new JScrollPane(chatPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // 底部输入面板
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
        bottomPanel.setBackground(Color.WHITE);

        inputField = new JTextField();
        inputField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        inputField.setPreferredSize(new Dimension(0, 40));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 13));
        sendButton.setPreferredSize(new Dimension(90, 40));
        sendButton.setBackground(new Color(66, 133, 244));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendButton.addActionListener(e -> sendMessage());

        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        // 顶部配置面板
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        topPanel.setBackground(new Color(232, 240, 254));

        // 创建输入区域面板（使用 GridBagLayout 实现灵活布局）
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(new Color(232, 240, 254));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 第一行：Git Path 输入
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel pathLabel = new JLabel("Git Path:");
        pathLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        inputPanel.add(pathLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField gitPathField = new JTextField();
        gitPathField.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        gitPathField.setPreferredSize(new Dimension(0, 30));
        
        // 设置初始值 - 显示完整的远程 URL
        if (currentRemoteUrl != null && !currentRemoteUrl.isEmpty()) {
            // 移除 "origin : " 前缀（如果有）
            String displayUrl = currentRemoteUrl;
            if (displayUrl.contains(" : ")) {
                displayUrl = displayUrl.split(" : ")[1].trim();
            }
            gitPathField.setText(displayUrl);
        } else if (currentOwner != null && currentRepo != null) {
            gitPathField.setText(currentOwner + "/" + currentRepo);
        }
        
        gitPathField.setToolTipText("输入完整的 Git 远程 URL，例如：https://gitlab.com/group/project.git 或 git@gitlab.com:group/project.git");
        inputPanel.add(gitPathField, gbc);

        // 第二行：Branch 选择
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        JLabel branchLabel = new JLabel("Branch:");
        branchLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        inputPanel.add(branchLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        
        // 可编辑的分支下拉框
        branchComboBox = new JComboBox<>();
        branchComboBox.setEditable(true);
        branchComboBox.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        branchComboBox.setPreferredSize(new Dimension(0, 30));
        branchComboBox.setMaximumRowCount(10);
        
        // 添加所有分支
        if (!allBranches.isEmpty()) {
            for (String branch : allBranches) {
                branchComboBox.addItem(branch);
            }
            
            // 设置当前分支为默认选中
            if (currentBranch != null) {
                branchComboBox.setSelectedItem(currentBranch);
            }
        } else {
            // 如果没有分支列表，添加常用分支
            branchComboBox.addItem("main");
            branchComboBox.addItem("master");
            branchComboBox.addItem("dev");
            branchComboBox.addItem("develop");
            if (currentBranch != null && !currentBranch.isEmpty()) {
                branchComboBox.setSelectedItem(currentBranch);
            }
        }
        
        branchComboBox.setToolTipText("选择或输入分支名称");
        inputPanel.add(branchComboBox, gbc);

        // 获取编辑器组件用于实时筛选
        JTextField branchEditor = (JTextField) branchComboBox.getEditor().getEditorComponent();
        
        // 添加文档监听器实现实时筛选
        branchEditor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (!isFilteringBranches) {
                    SwingUtilities.invokeLater(() -> filterBranches());
                }
            }
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (!isFilteringBranches) {
                    SwingUtilities.invokeLater(() -> filterBranches());
                }
            }
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (!isFilteringBranches) {
                    SwingUtilities.invokeLater(() -> filterBranches());
                }
            }
        });
        
        // 添加选择监听器
        branchComboBox.addActionListener(e -> {
            if (isFilteringBranches) {
                return;
            }
            
            String selected = (String) branchComboBox.getSelectedItem();
            if (selected != null && !selected.trim().isEmpty() && !selected.equals(currentBranch)) {
                if (allBranches.isEmpty() || allBranches.contains(selected)) {
                    currentBranch = selected;
                    System.out.println("[AI Chat] Branch changed to: " + currentBranch);
                    appendSystemMessage("✓ 已切换到分支: " + currentBranch);
                }
            }
        });

        // 第三行：Apply 按钮和提示信息
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        
        JPanel actionPanel = new JPanel(new BorderLayout(10, 0));
        actionPanel.setBackground(new Color(232, 240, 254));
        
        JButton applyButton = new JButton("Apply");
        applyButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        applyButton.setPreferredSize(new Dimension(80, 28));
        applyButton.setBackground(new Color(66, 133, 244));
        applyButton.setForeground(Color.WHITE);
        applyButton.setFocusPainted(false);
        applyButton.setBorderPainted(false);
        applyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyButton.setToolTipText("应用新的 Git 路径和分支设置");
        
        applyButton.addActionListener(e -> {
            String newGitPath = gitPathField.getText().trim();
            String newBranch = (String) branchComboBox.getSelectedItem();
            
            if (newGitPath.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "请输入 Git 远程 URL 或项目路径", 
                    "输入错误", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // 判断是完整 URL 还是简化路径
            if (newGitPath.startsWith("https://") || newGitPath.startsWith("http://") || newGitPath.startsWith("git@")) {
                // 完整的 Git URL
                currentRemoteUrl = newGitPath;
                
                // 从 URL 提取 owner/repo
                String[] parts = extractOwnerRepoFromUrl(newGitPath);
                if (parts != null) {
                    currentOwner = parts[0];
                    currentRepo = parts[1];
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "无法解析 Git URL\n\n请确保 URL 格式正确", 
                        "解析错误", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
            } else {
                // 简化的路径格式（owner/repo 或 group/subgroup/project）
                String[] parts = newGitPath.split("/");
                if (parts.length < 2) {
                    JOptionPane.showMessageDialog(this, 
                        "Git 路径格式错误\n\n正确格式：\n" +
                        "- 完整 URL: https://gitlab.com/group/project.git\n" +
                        "- 简化路径: owner/repo 或 group/subgroup/project", 
                        "输入错误", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // 对于简化路径，构建 owner/repo
                if (parts.length == 2) {
                    currentOwner = parts[0];
                    currentRepo = parts[1];
                } else {
                    // 对于多级路径（如 group/subgroup/project），将前面的部分作为 owner
                    currentOwner = String.join("/", java.util.Arrays.copyOfRange(parts, 0, parts.length - 1));
                    currentRepo = parts[parts.length - 1];
                }
                
                // 简化路径不更新 currentRemoteUrl（保持原有的）
            }
            
            if (newBranch != null && !newBranch.trim().isEmpty()) {
                currentBranch = newBranch.trim();
            }
            
            System.out.println("[AI Chat] Manual context updated:");
            System.out.println("[AI Chat]   Remote URL: " + currentRemoteUrl);
            System.out.println("[AI Chat]   Owner: " + currentOwner);
            System.out.println("[AI Chat]   Repo: " + currentRepo);
            System.out.println("[AI Chat]   Branch: " + currentBranch);
            
            // 重新初始化 Git API 客户端
            if (currentRemoteUrl != null && !currentRemoteUrl.isEmpty()) {
                gitApiClient = new GitApiClient(currentRemoteUrl, gitToken);
                initializeToolRegistry();  // 重新初始化 Tool Registry
            }
            
            // 显示成功消息
            String displayPath = (currentRemoteUrl != null && !currentRemoteUrl.isEmpty()) 
                ? currentRemoteUrl 
                : currentOwner + "/" + currentRepo;
            appendSystemMessage("✓ 已更新项目上下文: " + displayPath + " (分支: " + currentBranch + ")");
            
            // 更新系统消息
            updateSystemMessage();
        });
        
        actionPanel.add(applyButton, BorderLayout.WEST);
        
        // 添加提示信息
        String platformName = (gitApiClient != null) ? gitApiClient.getPlatformName() : "Git";
        JLabel hintLabel = new JLabel("<html><span style='font-size:10px; color:#5f6368;'>💡 提示：可以手动输入任意 " + platformName + " 项目路径</span></html>");
        hintLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
        actionPanel.add(hintLabel, BorderLayout.CENTER);
        
        inputPanel.add(actionPanel, gbc);

        topPanel.add(inputPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
    }

    private void initializeStyles() {
        // 用户消息样式 - 右对齐，淡蓝色背景
        userStyle = chatPane.addStyle("User", null);
        StyleConstants.setForeground(userStyle, new Color(33, 33, 33));  // 深灰色文字
        StyleConstants.setBackground(userStyle, new Color(225, 239, 255));  // 淡蓝色背景
        StyleConstants.setFontFamily(userStyle, "Microsoft YaHei");
        StyleConstants.setFontSize(userStyle, 13);
        StyleConstants.setAlignment(userStyle, StyleConstants.ALIGN_RIGHT);
        StyleConstants.setBold(userStyle, false);

        // AI 消息样式 - 左对齐，白色背景
        assistantStyle = chatPane.addStyle("Assistant", null);
        StyleConstants.setForeground(assistantStyle, new Color(33, 33, 33));
        StyleConstants.setBackground(assistantStyle, Color.WHITE);
        StyleConstants.setFontFamily(assistantStyle, "Microsoft YaHei");
        StyleConstants.setFontSize(assistantStyle, 13);
        StyleConstants.setAlignment(assistantStyle, StyleConstants.ALIGN_LEFT);

        // 系统消息样式 - 居中，灰色
        systemStyle = chatPane.addStyle("System", null);
        StyleConstants.setForeground(systemStyle, new Color(117, 117, 117));
        StyleConstants.setFontFamily(systemStyle, "Microsoft YaHei");
        StyleConstants.setFontSize(systemStyle, 12);
        StyleConstants.setAlignment(systemStyle, StyleConstants.ALIGN_CENTER);
        StyleConstants.setItalic(systemStyle, true);

        // 时间戳样式
        timestampStyle = chatPane.addStyle("Timestamp", null);
        StyleConstants.setForeground(timestampStyle, new Color(158, 158, 158));
        StyleConstants.setFontFamily(timestampStyle, "Microsoft YaHei");
        StyleConstants.setFontSize(timestampStyle, 11);
    }

    private void showWelcomeMessage() {
        String platformName = (gitApiClient != null) ? gitApiClient.getPlatformName() : "Git";
        String welcome = currentOwner != null && currentRepo != null
            ? String.format("你好！我是 %s 助手。当前项目：%s/%s\n\n你可以问我关于这个项目的任何问题，比如：\n• 这个项目有多少 star？\n• 最近有什么更新？\n• 有多少个开放的 issue？", 
                platformName, currentOwner, currentRepo)
            : String.format("你好！我是 %s 助手。\n\n请先在左侧选择一个 Git 项目，然后你可以问我关于项目的任何问题！", platformName);
        
        appendStyledMessage("Assistant", welcome, assistantStyle);
    }

    private void initializeAIService() {
        AppSettings settings = AppSettings.getInstance();
        String apiUrl = settings.getAiApiUrl();
        String apiKey = settings.getAiApiKey();
        String model = settings.getAiModel();
        gitToken = settings.getGithubToken();

        if (apiUrl.isEmpty() || apiKey.isEmpty()) {
            appendSystemMessage("⚠️ AI API 未配置。请前往 Chat -> AI Settings 进行配置。");
            sendButton.setEnabled(false);
            inputField.setEnabled(false);
            return;
        }

        aiService = new AIService(apiUrl, apiKey, model);
        
        // 初始化 Git API 客户端
        if (currentRemoteUrl != null && !currentRemoteUrl.isEmpty()) {
            gitApiClient = new GitApiClient(currentRemoteUrl, gitToken);
            initializeToolRegistry();  // 初始化 Tool Registry
        }
    }
    
    /**
     * 初始化 Tool Registry
     * 注册所有可用的 Git Tools
     */
    private void initializeToolRegistry() {
        toolRegistry = new GitToolRegistry();
        
        // 注册所有 Tools
        toolRegistry.register(new com.gitviewer.tools.GetRepoTool(gitApiClient));
        toolRegistry.register(new com.gitviewer.tools.GetIssuesTool(gitApiClient));
        toolRegistry.register(new com.gitviewer.tools.GetPullRequestsTool(gitApiClient));
        toolRegistry.register(new com.gitviewer.tools.GetCommitsTool(gitApiClient, currentBranch));
        toolRegistry.register(new com.gitviewer.tools.GetBranchesTool(gitApiClient));
        toolRegistry.register(new com.gitviewer.tools.GetReleasesTool(gitApiClient));
        toolRegistry.register(new com.gitviewer.tools.GetContentsTool(gitApiClient));
        toolRegistry.register(new com.gitviewer.tools.SearchRepositoriesTool(gitApiClient));
        toolRegistry.register(new com.gitviewer.tools.SearchIssuesTool(gitApiClient));
        toolRegistry.register(new com.gitviewer.tools.SearchFilesTool(gitApiClient));
        toolRegistry.register(new com.gitviewer.tools.GetFileCommitsTool(gitApiClient, currentBranch));
        toolRegistry.register(new com.gitviewer.tools.GetFileContentTool(gitApiClient, currentBranch));
        toolRegistry.register(new com.gitviewer.tools.GetSingleCommitTool(gitApiClient));
        
        System.out.println("[AI Chat] Tool Registry initialized with " + toolRegistry.size() + " tools");
    }

    private void addSystemMessage() {
        // 简化的系统提示，不再需要复杂的 API 指令说明
        String contextInfo = "";
        if (currentOwner != null && currentRepo != null) {
            contextInfo = "当前项目：" + currentOwner + "/" + currentRepo + "。";
        }
        
        String systemPrompt = contextInfo + "你是一个友好的 GitHub 助手，请用中文回答用户的问题。";
        chatHistory.add(new AIService.ChatMessage("system", systemPrompt));
    }

    /**
     * 更新系统消息（当用户手动更改项目上下文时）
     */
    private void updateSystemMessage() {
        // 更新聊天历史中的第一条系统消息
        if (!chatHistory.isEmpty() && chatHistory.get(0).role.equals("system")) {
            String contextInfo = "";
            if (currentOwner != null && currentRepo != null) {
                contextInfo = "当前项目：" + currentOwner + "/" + currentRepo + "。";
            }
            
            String systemPrompt = contextInfo + "你是一个友好的 GitHub 助手，请用中文回答用户的问题。";
            chatHistory.set(0, new AIService.ChatMessage("system", systemPrompt));
            
            System.out.println("[AI Chat] System message updated with new context");
        }
    }

    /**
     * 从当前目录提取 Git 仓库信息
     */
    private void extractGitInfo() {
        if (currentDirectory == null) {
            return;
        }

        try {
            // 查找 Git 仓库根目录
            File gitRepo = findGitRepository(currentDirectory);
            if (gitRepo == null) {
                return;
            }

            // 获取 remote URL 和分支信息
            GitInfoExtractor.GitRepositoryInfo repoInfo = GitInfoExtractor.getRepositoryInfo(gitRepo);
            if (repoInfo != null) {
                // 获取 remote URL
                if (!repoInfo.getRemoteUrls().isEmpty()) {
                    String remoteUrl = repoInfo.getRemoteUrls().get(0);
                    
                    // 保存完整的 remote URL
                    currentRemoteUrl = remoteUrl;
                    
                    // 从 remote URL 提取 owner/repo
                    String[] parts = extractOwnerRepoFromUrl(remoteUrl);
                    if (parts != null) {
                        currentOwner = parts[0];
                        currentRepo = parts[1];
                        System.out.println("[AI Chat] extractGitInfo - Remote URL: " + remoteUrl);
                        System.out.println("[AI Chat] extractGitInfo - Extracted owner: " + currentOwner);
                        System.out.println("[AI Chat] extractGitInfo - Extracted repo: " + currentRepo);
                        System.out.println("[AI Chat] extractGitInfo - Full context: " + currentOwner + "/" + currentRepo);
                    } else {
                        System.err.println("[AI Chat] extractGitInfo - Failed to extract owner/repo from URL: " + remoteUrl);
                    }
                }
                
                // 获取分支信息
                currentBranch = repoInfo.getCurrentBranch();
                allBranches = new ArrayList<>(repoInfo.getBranches());
                System.out.println("[AI Chat] Current branch: " + currentBranch);
                System.out.println("[AI Chat] Total branches: " + allBranches.size());
            }
        } catch (Exception e) {
            System.err.println("[AI Chat] Failed to extract git info: " + e.getMessage());
        }
    }

    /**
     * 查找 Git 仓库根目录
     */
    private File findGitRepository(File dir) {
        File current = dir;
        while (current != null) {
            if (GitInfoExtractor.isGitRepository(current)) {
                return current;
            }
            current = current.getParentFile();
        }
        return null;
    }

    /**
     * 从 Git remote URL 提取 owner/repo
     * 支持格式：
     * - https://github.com/owner/repo.git
     * - https://gitlab.com/group/subgroup/project.git (多级路径)
     * - git@github.com:owner/repo.git
     * - git@gitlab.com:group/subgroup/project.git (多级路径)
     * - origin : https://github.com/owner/repo.git
     */
    private String[] extractOwnerRepoFromUrl(String url) {
        try {
            System.out.println("[AI Chat] extractOwnerRepoFromUrl - Input URL: " + url);
            
            // 移除 "origin : " 前缀（如果有）
            String cleanUrl = url;
            if (url.contains(" : ")) {
                cleanUrl = url.split(" : ")[1].trim();
                System.out.println("[AI Chat] extractOwnerRepoFromUrl - Cleaned URL: " + cleanUrl);
            }

            String path = null;
            
            // 处理 HTTPS 格式
            if (cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) {
                // https://gitlab.com/group/subgroup/project.git
                path = cleanUrl.replaceFirst("https?://[^/]+/", "");
                System.out.println("[AI Chat] extractOwnerRepoFromUrl - HTTPS path extracted: " + path);
            }
            // 处理 SSH 格式
            else if (cleanUrl.startsWith("git@")) {
                // git@gitlab.com:group/subgroup/project.git
                path = cleanUrl.split(":")[1];
                System.out.println("[AI Chat] extractOwnerRepoFromUrl - SSH path extracted: " + path);
            }
            
            if (path != null) {
                // 移除 .git 后缀
                path = path.replaceFirst("\\.git$", "");
                System.out.println("[AI Chat] extractOwnerRepoFromUrl - Path after removing .git: " + path);
                
                // 分割路径
                String[] parts = path.split("/");
                System.out.println("[AI Chat] extractOwnerRepoFromUrl - Split into " + parts.length + " parts");
                for (int i = 0; i < parts.length; i++) {
                    System.out.println("[AI Chat] extractOwnerRepoFromUrl -   parts[" + i + "]: " + parts[i]);
                }
                
                if (parts.length >= 2) {
                    // 对于多级路径（如 group/subgroup/project）
                    // owner = group/subgroup, repo = project
                    String owner = String.join("/", java.util.Arrays.copyOfRange(parts, 0, parts.length - 1));
                    String repo = parts[parts.length - 1];
                    System.out.println("[AI Chat] extractOwnerRepoFromUrl - Final owner: " + owner);
                    System.out.println("[AI Chat] extractOwnerRepoFromUrl - Final repo: " + repo);
                    return new String[]{owner, repo};
                }
            }
        } catch (Exception e) {
            System.err.println("[AI Chat] extractOwnerRepoFromUrl - ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        System.err.println("[AI Chat] extractOwnerRepoFromUrl - Returning null");
        return null;
    }

    private void sendMessage() {
        String userMessage = inputField.getText().trim();
        if (userMessage.isEmpty()) {
            return;
        }

        System.out.println("\n");
        System.out.println("==================================================");
        System.out.println("========== 新的对话开始 ==========");
        System.out.println("==================================================");
        System.out.println("[AI Chat] User message: " + userMessage);
        System.out.println("[AI Chat] Current project: " + 
            (currentOwner != null ? currentOwner + "/" + currentRepo : "None"));
        System.out.println("[AI Chat] Remote URL: " + 
            (currentRemoteUrl != null ? currentRemoteUrl : "None"));
        
        // 显示用户消息
        appendStyledMessage("You", userMessage, userStyle);
        inputField.setText("");

        // 禁用输入
        sendButton.setEnabled(false);
        inputField.setEnabled(false);

        // 在后台线程处理
        Thread thread = new Thread(() -> {
            try {
                // 判断模式
                AppSettings settings = AppSettings.getInstance();
                String chatMode = settings.getAiChatMode();
                
                System.out.println("[AI Chat] Chat Mode: " + chatMode);
                
                if ("agent".equals(chatMode)) {
                    // ===== Agent Mode =====
                    processAgentMode(userMessage);
                } else {
                    // ===== Simple Mode =====
                    processSimpleMode(userMessage);
                }
                
                System.out.println("==================================================");
                System.out.println("========== 对话完成 ==========");
                System.out.println("==================================================\n");
                
            } catch (Exception e) {
                System.err.println("[AI Chat] FATAL ERROR in sendMessage: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    removeLastMessage();
                    appendSystemMessage("❌ 错误: " + e.getMessage());
                    sendButton.setEnabled(true);
                    inputField.setEnabled(true);
                });
            }
        });
        thread.setDaemon(true);  // 设置为守护线程，防止阻止应用退出
        thread.start();
    }

    /**
     * Simple Mode（简单模式）- 单次 API 调用
     */
    private void processSimpleMode(String userMessage) {
        System.out.println("========== 第一阶段：询问 AI 需要调用哪个 API ==========");
        
        // 显示"正在处理"提示
        SwingUtilities.invokeLater(() -> appendSystemMessage("💭 正在分析问题..."));

        // **第一阶段：让 AI 决定调用哪个 API**
        String apiInstruction = askAIForApiCall(userMessage);
        
        System.out.println("[AI Chat] AI returned instruction: " + apiInstruction);
        
        // 移除"正在分析"提示
        SwingUtilities.invokeLater(() -> removeLastMessage());
        
        String githubData = null;
        
        // 检查 AI 是否返回了 API 调用指令
        if (apiInstruction != null && isValidApiInstruction(apiInstruction)) {
            System.out.println("[AI Chat] Valid API instruction detected, proceeding to call Git API");
            
            // 显示"正在查询 Git"提示
            SwingUtilities.invokeLater(() -> appendSystemMessage("🔍 正在查询 Git..."));
            
            // 解析并执行 API 调用
            githubData = executeApiInstruction(apiInstruction);
            
            if (githubData != null) {
                System.out.println("[AI Chat] Git data received successfully");
            } else {
                System.out.println("[AI Chat] WARNING: Git API returned null");
            }
            
            // 移除"正在查询"提示
            SwingUtilities.invokeLater(() -> removeLastMessage());
        } else {
            System.out.println("[AI Chat] No valid API instruction, skipping Git API call");
        }
        
        // **第二阶段：让 AI 生成友好的回答**
        System.out.println("========== 第二阶段：生成友好回答 ==========");
        SwingUtilities.invokeLater(() -> appendSystemMessage("🤖 AI 正在生成回答..."));
        
        String finalAnswer = askAIForFinalAnswer(userMessage, githubData);
        
        // 移除"正在生成"提示
        SwingUtilities.invokeLater(() -> removeLastMessage());
        
        System.out.println("[AI Chat] Final answer generated");
        
        // 更新历史记录
        chatHistory.add(new AIService.ChatMessage("user", userMessage));
        chatHistory.add(new AIService.ChatMessage("assistant", finalAnswer));
        
        // 限制历史记录长度
        if (chatHistory.size() > 20) {
            // 保留第一条 system message 和最近的对话
            AIService.ChatMessage systemMsg = chatHistory.get(0);
            chatHistory = new ArrayList<>(chatHistory.subList(chatHistory.size() - 19, chatHistory.size()));
            chatHistory.add(0, systemMsg);
            System.out.println("[AI Chat] Chat history trimmed to 20 messages");
        }
        
        // 显示响应
        String finalResponse = finalAnswer;
        SwingUtilities.invokeLater(() -> {
            appendStyledMessage("Assistant", finalResponse, assistantStyle);
            sendButton.setEnabled(true);
            inputField.setEnabled(true);
            inputField.requestFocus();
        });
        
        System.out.println("========== Simple Mode 完成 ==========\n");
    }

    /**
     * Agent Mode（智能模式）- 多轮推理循环
     */
    private void processAgentMode(String userMessage) {
        AppSettings settings = AppSettings.getInstance();
        int maxIterations = settings.getAiMaxIterations();
        
        System.out.println("========== Agent Mode ==========");
        System.out.println("[AI Chat] Max Iterations: " + maxIterations);
        
        // 重置状态
        currentIteration = 0;
        collectedData = new StringBuilder();
        int executedIterations = 0;  // 记录实际执行的轮数
        
        // Agent 循环
        for (currentIteration = 1; currentIteration <= maxIterations; currentIteration++) {
            executedIterations = currentIteration;  // 记录当前轮次
            System.out.println("\n========== Agent循环 第" + currentIteration + "轮 ==========");
            
            // 显示进度
            final int iteration = currentIteration;
            final int progress = (iteration * 100) / maxIterations;
            SwingUtilities.invokeLater(() -> 
                appendSystemMessage("🔄 Agent 循环 第" + iteration + "/" + maxIterations + "轮 (" + progress + "%)..."));
            
            // 询问 AI 下一步做什么
            String nextAction = askAIForNextAction(userMessage, collectedData.toString(), 
                                                   currentIteration, maxIterations);
            
            SwingUtilities.invokeLater(() -> removeLastMessage());
            
            System.out.println("[AI Chat] Agent decision: " + nextAction);
            
            // 解析 AI 决策
            if (nextAction != null && nextAction.contains("\"action\": \"FINISH\"")) {
                System.out.println("[AI Chat] Agent decided to FINISH");
                break;  // AI 认为信息足够，结束循环
            }
            
            // 检查是否是有效的 API 调用
            if (nextAction == null || !isValidApiInstruction(nextAction)) {
                System.out.println("[AI Chat] Invalid API instruction, ending loop");
                break;
            }
            
            // 执行 API 调用
            SwingUtilities.invokeLater(() -> 
                appendSystemMessage("🔍 正在调用 Git API..."));
            
            String apiData = executeApiInstruction(nextAction);
            
            SwingUtilities.invokeLater(() -> removeLastMessage());
            
            // 收集数据（带来源标注）
            if (apiData != null && !apiData.isEmpty()) {
                String apiName = extractJsonValue(nextAction, "action");
                collectedData.append("\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                collectedData.append("📊 数据来源: ").append(apiName)
                            .append(" (第").append(currentIteration).append("轮)\n");
                collectedData.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                collectedData.append(apiData);
                
                System.out.println("[AI Chat] 第" + currentIteration + "轮数据收集成功");
                System.out.println("[AI Chat] 已收集数据总长度: " + collectedData.length() + " chars");
            } else {
                System.out.println("[AI Chat] 第" + currentIteration + "轮 API 返回空数据");
                // 记录失败但继续
                collectedData.append("\n\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
                collectedData.append("⚠️ API 调用失败 (第").append(currentIteration).append("轮)\n");
                collectedData.append("API: ").append(extractJsonValue(nextAction, "action")).append("\n");
                collectedData.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            }
            
            // 检查上下文大小，防止过大
            if (collectedData.length() > 100000) {
                System.out.println("[AI Chat] 上下文过大，截断早期数据");
                String data = collectedData.toString();
                int keepFrom = data.length() - 80000;
                collectedData = new StringBuilder();
                collectedData.append("...[早期数据已省略，保留最近80KB]\n\n");
                collectedData.append(data.substring(keepFrom));
            }
        }
        
        System.out.println("\n[AI Chat] Agent模式完成，共执行" + executedIterations + "轮");
        
        // 生成最终回答
        System.out.println("========== 生成最终回答 ==========");
        SwingUtilities.invokeLater(() -> appendSystemMessage("🤖 AI 正在生成最终回答..."));
        
        String finalAnswer = askAIForFinalAnswer(userMessage, collectedData.toString());
        
        SwingUtilities.invokeLater(() -> removeLastMessage());
        
        System.out.println("[AI Chat] Final answer generated");
        
        // 更新历史记录
        chatHistory.add(new AIService.ChatMessage("user", userMessage));
        chatHistory.add(new AIService.ChatMessage("assistant", finalAnswer));
        
        // 限制历史记录长度
        if (chatHistory.size() > 20) {
            AIService.ChatMessage systemMsg = chatHistory.get(0);
            chatHistory = new ArrayList<>(chatHistory.subList(chatHistory.size() - 19, chatHistory.size()));
            chatHistory.add(0, systemMsg);
            System.out.println("[AI Chat] Chat history trimmed to 20 messages");
        }
        
        // 显示响应
        String finalResponse = finalAnswer;
        SwingUtilities.invokeLater(() -> {
            appendStyledMessage("Assistant", finalResponse, assistantStyle);
            sendButton.setEnabled(true);
            inputField.setEnabled(true);
            inputField.requestFocus();
        });
        
        System.out.println("========== Agent Mode 完成 ==========\n");
    }

    /**
     * 第一阶段：询问 AI 需要调用哪个 Git API
     */
    private String askAIForApiCall(String userQuestion) {
        try {
            System.out.println("\n========== 第一阶段：询问 AI 需要调用哪个 API ==========");
            System.out.println("[AI Chat] User question: " + userQuestion);
            
            List<AIService.ChatMessage> messages = new ArrayList<>();
            
            String platformName = (gitApiClient != null) ? gitApiClient.getPlatformName() : "Git";
            
            // 构建详细的上下文
            StringBuilder context = new StringBuilder();
            context.append("你是一个 ").append(platformName).append(" API 助手。\n\n");
            context.append("当前项目信息：\n");
            if (currentOwner != null && currentRepo != null) {
                context.append("- Owner: ").append(currentOwner).append("\n");
                context.append("- Repo: ").append(currentRepo).append("\n");
                context.append("- Remote URL: ").append(currentRemoteUrl).append("\n");
                context.append("- Platform: ").append(platformName).append("\n");
                System.out.println("[AI Chat] Context - Owner: " + currentOwner + ", Repo: " + currentRepo);
                System.out.println("[AI Chat] Context - Platform: " + platformName);
            } else {
                context.append("- 未选择项目\n");
                System.out.println("[AI Chat] Context - No project selected");
            }
            context.append("\n");
            
            // 使用 Tool Registry 自动生成 API 列表
            if (toolRegistry != null) {
                context.append(toolRegistry.generateToolsDescription());
            } else {
                context.append("【可用的 Tools】\n");
                context.append("(Tool Registry 未初始化)\n");
            }
            context.append("\n");
            
            context.append("请分析用户的问题，如果需要调用 API，返回 JSON 格式：\n");
            context.append("{\"action\": \"get_repo\", \"owner\": \"facebook\", \"repo\": \"react\"}\n");
            context.append("{\"action\": \"get_issues\", \"state\": \"open\"}\n");
            context.append("{\"action\": \"get_contents\", \"path\": \"\"}\n");
            context.append("{\"action\": \"search_repos\", \"query\": \"machine learning\"}\n");
            context.append("{\"action\": \"search_files\", \"filename\": \"abc.java\"}\n");
            context.append("{\"action\": \"get_file_commits\", \"filepath\": \"src/main/App.java\"}\n");
            context.append("{\"action\": \"get_file_content\", \"filepath\": \"envs/common/.basic\"}\n");
            context.append("{\"action\": \"get_file_content\", \"filepath\": \"src/App.java\", \"branch\": \"dev\"}\n");
            context.append("\n");
            context.append("特别说明：\n");
            context.append("- 当用户询问某个文件的内容、文件是做什么的、文件里有什么代码时，使用 get_file_content API\n");
            context.append("- get_file_content 会返回文件的完整源代码，你可以直接分析并回答用户的问题\n");
            context.append("- 不要建议用户使用 curl 命令，直接使用 API 获取内容\n");
            context.append("\n");
            context.append("注意：\n");
            context.append("- 如果用户没有指定项目，使用当前项目的 owner 和 repo\n");
            context.append("- 只返回 JSON，不要有其他文字\n");
            context.append("- 如果不需要调用 API，返回：{\"action\": \"none\"}\n");
            
            messages.add(new AIService.ChatMessage("system", context.toString()));
            messages.add(new AIService.ChatMessage("user", userQuestion));
            
            System.out.println("[AI Chat] Sending request to AI API...");
            System.out.println("[AI Chat] System prompt length: " + context.length() + " chars");
            
            String response = aiService.chat(messages);
            
            System.out.println("[AI Chat] AI Response (raw): " + response);
            System.out.println("[AI Chat] Response length: " + response.length() + " chars");
            System.out.println("========== 第一阶段完成 ==========\n");
            
            return response.trim();
            
        } catch (Exception e) {
            System.err.println("[AI Chat] ERROR in askAIForApiCall: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 第二阶段：让 AI 基于 Git API 数据生成友好的回答
     */
    private String askAIForFinalAnswer(String userQuestion, String githubData) {
        try {
            System.out.println("\n========== 第二阶段：生成友好回答 ==========");
            System.out.println("[AI Chat] User question: " + userQuestion);
            System.out.println("[AI Chat] Has Git data: " + (githubData != null && !githubData.isEmpty()));
            if (githubData != null && !githubData.isEmpty()) {
                System.out.println("[AI Chat] Git data preview: " + 
                    githubData.substring(0, Math.min(300, githubData.length())) + "...");
            }
            
            List<AIService.ChatMessage> messages = new ArrayList<>();
            
            String platformName = (gitApiClient != null) ? gitApiClient.getPlatformName() : "Git";
            
            // 系统提示
            StringBuilder systemPrompt = new StringBuilder();
            systemPrompt.append("你是一个友好的 ").append(platformName).append(" 助手。\n");
            if (currentOwner != null && currentRepo != null) {
                systemPrompt.append("当前项目：").append(currentOwner).append("/").append(currentRepo).append("\n");
            }
            systemPrompt.append("请用中文友好地回答用户的问题。\n");
            
            // 如果有 Git 数据，强调 AI 应该直接使用这些数据
            if (githubData != null && !githubData.isEmpty()) {
                systemPrompt.append("\n【重要】你的能力和任务：\n");
                systemPrompt.append("1. 你可以直接读取和分析文件内容\n");
                systemPrompt.append("2. 我已经为你获取了完整的文件内容\n");
                systemPrompt.append("3. 你必须直接展示和分析这些内容\n");
                systemPrompt.append("\n【严格禁止】：\n");
                systemPrompt.append("- 禁止说\"无法访问\"、\"无法读取\"、\"不能直接查看\"\n");
                systemPrompt.append("- 禁止建议用户使用 git 命令、curl 命令\n");
                systemPrompt.append("- 禁止说需要其他工具或权限\n");
                systemPrompt.append("\n【你应该做的】：\n");
                systemPrompt.append("- 直接展示文件内容或 commit 修改内容\n");
                systemPrompt.append("- 分析文件的作用和配置项\n");
                systemPrompt.append("- 回答用户关于文件的任何问题\n");
                systemPrompt.append("- 如果是 commit 信息，直接展示 commit message、author、date 和具体的代码修改（diff）\n");
                systemPrompt.append("- 可以提供相关的 URL 链接（如 commit URL、文件 URL）方便用户查看\n");
            }
            
            messages.add(new AIService.ChatMessage("system", systemPrompt.toString()));
            messages.add(new AIService.ChatMessage("user", userQuestion));
            
            // 如果有 Git 数据，添加到上下文
            if (githubData != null && !githubData.isEmpty()) {
                messages.add(new AIService.ChatMessage("system", 
                    "【文件内容】以下是我从 " + platformName + " 获取的完整文件内容，你现在拥有这些数据，请直接展示和分析：\n\n" + githubData));
            }
            
            System.out.println("[AI Chat] Sending request to AI API for final answer...");
            
            String response = aiService.chat(messages);
            
            System.out.println("[AI Chat] Final answer (raw): " + response);
            System.out.println("[AI Chat] Answer length: " + response.length() + " chars");
            System.out.println("========== 第二阶段完成 ==========\n");
            
            return response;
            
        } catch (Exception e) {
            System.err.println("[AI Chat] ERROR in askAIForFinalAnswer: " + e.getMessage());
            e.printStackTrace();
            return "抱歉，生成回答时出错：" + e.getMessage();
        }
    }

    /**
     * 检查是否是有效的 API 指令
     */
    private boolean isValidApiInstruction(String instruction) {
        System.out.println("\n========== 检查 API 指令有效性 ==========");
        System.out.println("[AI Chat] Instruction to validate: " + instruction);
        
        if (instruction == null || instruction.isEmpty()) {
            System.out.println("[AI Chat] Validation result: INVALID (null or empty)");
            System.out.println("========================================\n");
            return false;
        }
        
        String trimmed = instruction.trim();
        boolean startsWithBrace = trimmed.startsWith("{");
        boolean containsAction = trimmed.contains("\"action\"");
        boolean isNoneAction = trimmed.contains("\"action\": \"none\"");
        
        System.out.println("[AI Chat] Starts with '{': " + startsWithBrace);
        System.out.println("[AI Chat] Contains 'action': " + containsAction);
        System.out.println("[AI Chat] Is 'none' action: " + isNoneAction);
        
        boolean isValid = startsWithBrace && containsAction && !isNoneAction;
        System.out.println("[AI Chat] Validation result: " + (isValid ? "VALID" : "INVALID"));
        System.out.println("========================================\n");
        
        return isValid;
    }

    /**
     * 执行 API 调用指令（Tool-Based 架构）
     */
    private String executeApiInstruction(String instruction) {
        try {
            String platformName = (gitApiClient != null) ? gitApiClient.getPlatformName() : "Git";
            System.out.println("\n========== 执行 " + platformName + " Tool 调用 ==========");
            System.out.println("[AI Chat] Instruction: " + instruction);
            
            // 解析 action（Tool 名称）
            String toolName = extractJsonValue(instruction, "action");
            System.out.println("[AI Chat] Tool name: " + toolName);
            
            // 检查 Tool Registry 是否初始化
            if (toolRegistry == null) {
                System.err.println("[AI Chat] ERROR: Tool Registry not initialized");
                return "ERROR: Tool Registry not initialized. Please check Git API configuration.";
            }
            
            // 检查 Tool 是否存在
            if (!toolRegistry.hasTool(toolName)) {
                System.err.println("[AI Chat] ERROR: Unknown tool: " + toolName);
                String availableTools = toolRegistry.getToolNames();
                return "ERROR: Unknown tool '" + toolName + "'. Available tools: " + availableTools;
            }
            
            // 获取 Tool
            GitTool tool = toolRegistry.getTool(toolName);
            System.out.println("[AI Chat] Tool found: " + tool.getDescription());
            
            // 解析参数
            Map<String, String> params = new java.util.HashMap<>();
            for (String paramName : tool.getParameters().keySet()) {
                String value = extractJsonValue(instruction, paramName);
                if (value != null && !value.isEmpty()) {
                    params.put(paramName, value);
                }
            }
            
            // 自动填充 owner/repo（如果未指定）
            if (!params.containsKey("owner") || params.get("owner").isEmpty()) {
                if (currentOwner != null && !currentOwner.isEmpty()) {
                    params.put("owner", currentOwner);
                    System.out.println("[AI Chat] Auto-filled owner: " + currentOwner);
                }
            }
            if (!params.containsKey("repo") || params.get("repo").isEmpty()) {
                if (currentRepo != null && !currentRepo.isEmpty()) {
                    params.put("repo", currentRepo);
                    System.out.println("[AI Chat] Auto-filled repo: " + currentRepo);
                }
            }
            
            System.out.println("[AI Chat] Parameters: " + params);
            
            // 执行 Tool
            System.out.println("[AI Chat] Executing tool: " + toolName);
            String result = tool.execute(params);
            
            // 统一的数据大小限制（除了 get_file_content 已经在 Tool 内部处理）
            if (result != null && !toolName.equals("get_file_content") && result.length() > 20000) {
                System.out.println("[AI Chat] Response too large (" + result.length() + " chars), truncating to 20000");
                result = result.substring(0, 20000) + "\n\n...[数据过多，已截断到20000字符。建议使用更具体的搜索条件]";
            }
            
            if (result != null) {
                System.out.println("[AI Chat] Tool execution completed, result length: " + result.length() + " chars");
                if (!result.startsWith("ERROR:")) {
                    System.out.println("[AI Chat] Result preview: " + 
                        result.substring(0, Math.min(200, result.length())) + "...");
                } else {
                    System.err.println("[AI Chat] Tool returned error: " + result);
                }
            } else {
                System.out.println("[AI Chat] Tool returned null");
            }
            
            System.out.println("========== Tool 调用完成 ==========\n");
            return result;
            
        } catch (Exception e) {
            System.err.println("[AI Chat] ERROR executing tool: " + e.getMessage());
            e.printStackTrace();
            System.out.println("========================================\n");
            return "ERROR: " + e.getMessage();
        }
    }

    /**
     * 从 JSON 字符串中提取值
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            System.err.println("[AI Chat] Error extracting JSON value for key: " + key);
        }
        return null;
    }

    /**
     * 添加带样式的消息
     */
    private void appendStyledMessage(String sender, String message, Style style) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            String timestamp = sdf.format(new Date());
            
            // 添加间距
            chatDoc.insertString(chatDoc.getLength(), "\n", null);
            
            // 记录消息开始位置
            int messageStart = chatDoc.getLength();
            
            // 添加发送者和时间戳
            String header = sender + "  " + timestamp + "\n";
            chatDoc.insertString(chatDoc.getLength(), header, timestampStyle);
            
            // 添加消息内容（带背景色和圆角效果）
            chatDoc.insertString(chatDoc.getLength(), message + "\n", style);
            
            // 设置段落对齐方式
            int messageEnd = chatDoc.getLength();
            chatDoc.setParagraphAttributes(messageStart, messageEnd - messageStart, style, false);
            
            // 添加间距
            chatDoc.insertString(chatDoc.getLength(), "\n", null);
            
            // 滚动到底部
            chatPane.setCaretPosition(chatDoc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加系统消息
     */
    private void appendSystemMessage(String message) {
        try {
            lastSystemMessageStart = chatDoc.getLength();
            chatDoc.insertString(chatDoc.getLength(), "\n" + message + "\n\n", systemStyle);
            lastSystemMessageEnd = chatDoc.getLength();
            chatPane.setCaretPosition(chatDoc.getLength());
            
            System.out.println("[AI Chat] System message added at position: " + lastSystemMessageStart + " to " + lastSystemMessageEnd);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 移除最后一条系统消息（用于移除"正在思考"提示）
     */
    private void removeLastMessage() {
        try {
            if (lastSystemMessageStart >= 0 && lastSystemMessageEnd >= 0) {
                System.out.println("[AI Chat] Removing system message from position: " + lastSystemMessageStart + " to " + lastSystemMessageEnd);
                System.out.println("[AI Chat] Document length before removal: " + chatDoc.getLength());
                
                // 只移除系统消息
                int length = lastSystemMessageEnd - lastSystemMessageStart;
                if (length > 0 && lastSystemMessageStart < chatDoc.getLength()) {
                    chatDoc.remove(lastSystemMessageStart, length);
                    System.out.println("[AI Chat] Document length after removal: " + chatDoc.getLength());
                }
                
                // 重置标记
                lastSystemMessageStart = -1;
                lastSystemMessageEnd = -1;
            } else {
                System.out.println("[AI Chat] No system message to remove");
            }
        } catch (BadLocationException e) {
            System.err.println("[AI Chat] Error removing system message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查响应是否是 API 调用指令
     */
    private boolean isApiInstruction(String response) {
        String trimmed = response.trim();
        return trimmed.startsWith("{") && trimmed.contains("\"action\"");
    }

    /**
     * 筛选分支列表（从可编辑的 ComboBox 编辑器获取筛选文本）
     */
    private void filterBranches() {
        if (isFilteringBranches || allBranches == null || allBranches.isEmpty()) {
            return;
        }
        
        isFilteringBranches = true;  // 设置标志位，防止递归
        
        try {
            // 从 ComboBox 的编辑器组件获取筛选文本
            JTextField editor = (JTextField) branchComboBox.getEditor().getEditorComponent();
            String filterText = editor.getText();
            String filterTextLower = filterText.toLowerCase().trim();
            
            // 保存当前光标位置
            int caretPosition = editor.getCaretPosition();
            
            // 移除所有项
            branchComboBox.removeAllItems();
            
            if (filterTextLower.isEmpty()) {
                // 没有筛选条件，显示所有分支
                for (String branch : allBranches) {
                    branchComboBox.addItem(branch);
                }
            } else {
                // 根据筛选条件过滤分支
                boolean hasMatches = false;
                for (String branch : allBranches) {
                    if (branch.toLowerCase().contains(filterTextLower)) {
                        branchComboBox.addItem(branch);
                        hasMatches = true;
                    }
                }
                
                // 如果没有匹配项，显示所有分支
                if (!hasMatches) {
                    for (String branch : allBranches) {
                        branchComboBox.addItem(branch);
                    }
                }
            }
            
            // 恢复编辑器中的文本和光标位置
            editor.setText(filterText);
            try {
                editor.setCaretPosition(Math.min(caretPosition, filterText.length()));
            } catch (IllegalArgumentException e) {
                // 忽略光标位置错误
            }
            
            // 显示下拉列表（只在有输入时，且组件可见且可显示时）
            if (branchComboBox.getItemCount() > 0 && !filterTextLower.isEmpty() && 
                branchComboBox.isShowing() && branchComboBox.isDisplayable()) {
                try {
                    branchComboBox.showPopup();
                } catch (IllegalComponentStateException | IllegalArgumentException e) {
                    // 忽略组件未完全显示时的异常
                }
            }
        } catch (Exception e) {
            // 捕获所有可能的异常，确保不影响主流程
        } finally {
            isFilteringBranches = false;  // 重置标志位
        }
    }

    /**
     * Agent Mode: 询问 AI 下一步应该做什么
     */
    private String askAIForNextAction(String userQuestion, String collectedData, 
                                      int currentIteration, int maxIterations) {
        try {
            System.out.println("\n========== 询问 AI 下一步行动 ==========");
            System.out.println("[AI Chat] Current iteration: " + currentIteration + "/" + maxIterations);
            System.out.println("[AI Chat] Collected data length: " + collectedData.length() + " chars");
            
            List<AIService.ChatMessage> messages = new ArrayList<>();
            
            String platformName = (gitApiClient != null) ? gitApiClient.getPlatformName() : "Git";
            
            // 构建完整上下文
            StringBuilder context = new StringBuilder();
            context.append("你是一个智能 ").append(platformName).append(" Agent。\n\n");
            
            // 当前状态
            context.append("【当前状态】\n");
            context.append("- 用户问题：").append(userQuestion).append("\n");
            context.append("- 当前轮次：").append(currentIteration).append("/").append(maxIterations).append("\n");
            if (currentOwner != null && currentRepo != null) {
                context.append("- 当前项目：").append(currentOwner).append("/").append(currentRepo).append("\n");
                if (currentBranch != null && !currentBranch.isEmpty()) {
                    context.append("- 当前分支：").append(currentBranch).append("\n");
                }
            }
            context.append("\n");
            
            // 已收集的数据
            if (collectedData != null && !collectedData.isEmpty()) {
                context.append("【已收集的数据】\n");
                context.append(collectedData);
                context.append("\n\n");
            } else {
                context.append("【已收集的数据】\n");
                context.append("（暂无数据）\n\n");
            }
            
            // 使用 Tool Registry 自动生成 API 列表（每轮都包含）
            if (toolRegistry != null) {
                context.append(toolRegistry.generateToolsDescription());
            } else {
                context.append("【可用的 Tools】\n");
                context.append("(Tool Registry 未初始化)\n");
            }
            context.append("\n");
            
            // 决策指南
            context.append("【分步思考】\n");
            context.append("请按照以下步骤分析用户问题：\n");
            context.append("1. 理解用户问题的核心需求\n");
            context.append("2. 分解问题：需要哪些信息才能回答？\n");
            context.append("3. 检查已收集的数据：缺少哪些关键信息？\n");
            context.append("4. 确定下一步：需要调用哪个 API 来获取缺失的信息？\n");
            context.append("\n");
            
            context.append("【常见问题的分步策略】\n");
            context.append("• 询问\"某个文件的XXX\"：\n");
            context.append("  步骤1：如果不知道文件路径 → search_files 查找文件（返回路径和代码片段）\n");
            context.append("  步骤2：找到路径后 → 根据需求调用 get_file_commits 或 get_file_content\n");
            context.append("  注意：search_files 只返回代码片段，不是完整文件！\n");
            context.append("\n");
            context.append("• 询问\"文件最后一次修改了什么\"或\"最近的修改内容\"：\n");
            context.append("  步骤1：如果不知道文件路径 → search_files 查找文件\n");
            context.append("  步骤2：get_file_commits 获取提交历史（返回 commit 列表，包含 SHA）\n");
            context.append("  步骤3：**关键**从返回的 commit 列表中提取第一个（最新）commit 的 SHA\n");
            context.append("  步骤4：调用 get_single_commit 获取该 commit 的详细信息（包括 diff）\n");
            context.append("  步骤5：FINISH（已有完整的修改内容）\n");
            context.append("  错误示例：重复调用 get_file_commits ❌\n");
            context.append("  正确示例：get_file_commits → get_single_commit(sha=最新commit的SHA) → FINISH ✓\n");
            context.append("\n");
            context.append("• 询问\"最近修改了XXX的commit\"：\n");
            context.append("  步骤1：如果不知道文件路径 → search_files 查找文件\n");
            context.append("  步骤2：get_file_commits 获取提交历史\n");
            context.append("  步骤3：如果需要查看具体修改内容 → get_single_commit 获取 diff\n");
            context.append("\n");
            context.append("• 询问\"包含某个业务术语的文件\"（如 'interest settlement'）：\n");
            context.append("  **关键**：业务术语在代码中可能是分开的（如 InterestSettlement、interest_settlement）\n");
            context.append("  **策略**：提取最核心的关键词进行搜索\n");
            context.append("  示例：'interest settlement' → 先搜索 'interest'（最核心的词）\n");
            context.append("  方法1（推荐）：用最核心的关键词搜索（如 filename=\"interest\"）\n");
            context.append("  方法2（如果结果太多）：搜索组合词（如 filename=\"InterestSettlement\" 或 \"interest_settlement\"）\n");
            context.append("  方法3（精确筛选）：先搜索核心词，然后逐个查看文件内容，筛选包含所有关键词的文件\n");
            context.append("  注意：search_files 返回的是代码片段，如需完整文件内容，必须再调用 get_file_content\n");
            context.append("\n");
            context.append("• 询问\"查看完整文件内容\"：\n");
            context.append("  步骤1：search_files 找到文件路径（如果不知道路径）\n");
            context.append("  步骤2：**必须**调用 get_file_content 获取完整内容（search_files 的结果不完整！）\n");
            context.append("  错误示例：只调用 search_files 就返回 FINISH ❌\n");
            context.append("  正确示例：search_files → get_file_content → FINISH ✓\n");
            context.append("\n");
            context.append("• 询问\"对比两个分支\"：\n");
            context.append("  步骤1：get_commits (branch: master)\n");
            context.append("  步骤2：get_commits (branch: develop)\n");
            context.append("  步骤3：FINISH（已有足够数据对比）\n");
            context.append("\n");
            
            context.append("【决策规则】\n");
            context.append("1. 如果用户提到文件名但没有完整路径，优先使用 search_files 确认文件是否存在\n");
            context.append("2. 如果已有文件路径，可以直接调用 get_file_commits 或 get_file_content\n");
            context.append("3. **重要**：search_files 只返回代码片段！如果用户问的是文件内容、文件做什么、最后修改等，必须再调用 get_file_content 或 get_file_commits\n");
            context.append("4. **重要**：get_file_commits 只返回 commit 列表（SHA、message、author）！如果用户问具体修改了什么，必须再调用 get_single_commit 获取 diff\n");
            context.append("5. **禁止重复调用同一个 API**：如果上一轮已经调用过某个 API 并获得了数据，不要再次调用相同的 API\n");
            context.append("6. 只有当已收集的数据能完整回答用户问题时，才返回 FINISH\n");
            context.append("7. 每次只执行一个最关键的步骤，不要跳步\n");
            context.append("8. **重要**：调用 API 时，如果有当前分支信息，必须传递 ref 参数指定分支\n");
            context.append("\n");
            
            context.append("【返回格式】\n");
            context.append("如果数据足够：\n");
            context.append("{\"action\": \"FINISH\", \"reason\": \"已收集足够信息\"}\n");
            context.append("\n");
            context.append("如果需要更多数据（必须包含 reason 说明为什么需要这个 API）：\n");
            
            // 根据是否有当前分支，提供不同的示例
            if (currentBranch != null && !currentBranch.isEmpty()) {
                context.append("{\"action\": \"search_files\", \"filename\": \"pom.xml\", \"ref\": \"")
                       .append(currentBranch).append("\", \"reason\": \"需要在当前分支查找文件\"}\n");
                context.append("{\"action\": \"get_file_commits\", \"filepath\": \"pom.xml\", \"branch\": \"")
                       .append(currentBranch).append("\", \"reason\": \"需要查看当前分支的提交历史\"}\n");
                context.append("{\"action\": \"get_single_commit\", \"sha\": \"abc123\", \"reason\": \"需要查看这个 commit 的具体修改内容（diff）\"}\n");
                context.append("{\"action\": \"get_file_content\", \"filepath\": \"src/App.java\", \"ref\": \"")
                       .append(currentBranch).append("\", \"reason\": \"需要查看当前分支的文件内容\"}\n");
            } else {
                context.append("{\"action\": \"search_files\", \"filename\": \"pom.xml\", \"reason\": \"需要先确认 pom 文件的完整路径\"}\n");
                context.append("{\"action\": \"get_file_commits\", \"filepath\": \"pom.xml\", \"reason\": \"需要查看文件的提交历史\"}\n");
                context.append("{\"action\": \"get_file_content\", \"filepath\": \"src/App.java\", \"reason\": \"需要查看文件内容\"}\n");
            }
            context.append("\n");
            context.append("【重要提醒】\n");
            context.append("- 只返回 JSON，不要有其他文字\n");
            context.append("- 必须包含 reason 字段解释你的决策\n");
            context.append("- 如果当前轮次是最后一轮（").append(currentIteration).append("/").append(maxIterations)
                      .append("），建议返回 FINISH\n");
            
            messages.add(new AIService.ChatMessage("system", context.toString()));
            messages.add(new AIService.ChatMessage("user", "下一步应该做什么？"));
            
            System.out.println("[AI Chat] Sending request to AI API...");
            System.out.println("[AI Chat] Context length: " + context.length() + " chars");
            
            String response = aiService.chat(messages);
            
            System.out.println("[AI Chat] AI Response: " + response);
            System.out.println("========== 询问完成 ==========\n");
            
            return response.trim();
            
        } catch (Exception e) {
            System.err.println("[AI Chat] ERROR in askAIForNextAction: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
