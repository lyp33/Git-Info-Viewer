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
    private File currentDirectory;
    private String currentOwner;
    private String currentRepo;
    private String currentRemoteUrl;  // 添加 remote URL 字段
    private int lastSystemMessageStart = -1;  // 跟踪最后一条系统消息的起始位置
    private int lastSystemMessageEnd = -1;    // 跟踪最后一条系统消息的结束位置
    
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

        // 顶部提示面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        topPanel.setBackground(new Color(232, 240, 254));

        // 左侧：项目信息
        String platformName = (gitApiClient != null) ? gitApiClient.getPlatformName() : "Git";
        StringBuilder contextHtml = new StringBuilder("<html><b>").append(platformName).append(" Assistant</b><br/>");
        
        if (currentOwner != null && currentRepo != null) {
            contextHtml.append("<span style='font-size:11px; color:#1a73e8;'>")
                      .append("📁 ").append(currentOwner).append("/").append(currentRepo);
            
            if (currentRemoteUrl != null && !currentRemoteUrl.isEmpty()) {
                contextHtml.append("<br/>🔗 ").append(currentRemoteUrl);
            }
            contextHtml.append("</span>");
        } else {
            contextHtml.append("<span style='font-size:11px; color:#5f6368;'>")
                      .append("请在左侧选择一个 Git 项目")
                      .append("</span>");
        }
        contextHtml.append("</html>");
        
        JLabel tipLabel = new JLabel(contextHtml.toString());
        tipLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 13));
        tipLabel.setForeground(new Color(26, 115, 232));
        topPanel.add(tipLabel, BorderLayout.WEST);

        // 右侧：分支选择器
        if (currentOwner != null && currentRepo != null && !allBranches.isEmpty()) {
            JPanel branchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            branchPanel.setBackground(new Color(232, 240, 254));
            
            JLabel branchLabel = new JLabel("分支:");
            branchLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            branchPanel.add(branchLabel);
            
            // 可编辑的分支下拉框
            branchComboBox = new JComboBox<>();
            branchComboBox.setEditable(true);  // 设置为可编辑
            branchComboBox.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
            branchComboBox.setPreferredSize(new Dimension(200, 30));
            branchComboBox.setMaximumRowCount(10);
            
            // 添加所有分支
            for (String branch : allBranches) {
                branchComboBox.addItem(branch);
            }
            
            // 设置当前分支为默认选中
            if (currentBranch != null) {
                branchComboBox.setSelectedItem(currentBranch);
            }
            
            // 获取编辑器组件
            JTextField editor = (JTextField) branchComboBox.getEditor().getEditorComponent();
            
            // 添加文档监听器实现实时筛选
            editor.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
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
                // 如果正在筛选，忽略事件
                if (isFilteringBranches) {
                    return;
                }
                
                String selected = (String) branchComboBox.getSelectedItem();
                if (selected != null && !selected.trim().isEmpty() && !selected.equals(currentBranch)) {
                    // 检查选择的分支是否在原始列表中
                    if (allBranches.contains(selected)) {
                        currentBranch = selected;
                        System.out.println("[AI Chat] Branch changed to: " + currentBranch);
                        // 添加分支切换的提示消息
                        appendSystemMessage("✓ 已切换到分支: " + currentBranch);
                    }
                }
            });
            
            branchPanel.add(branchComboBox);
            
            topPanel.add(branchPanel, BorderLayout.EAST);
        }

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
        }
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
                        System.out.println("[AI Chat] Current context: " + currentOwner + "/" + currentRepo);
                        System.out.println("[AI Chat] Remote URL: " + currentRemoteUrl);
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
     * - git@github.com:owner/repo.git
     * - origin : https://github.com/owner/repo.git
     */
    private String[] extractOwnerRepoFromUrl(String url) {
        try {
            // 移除 "origin : " 前缀（如果有）
            String cleanUrl = url;
            if (url.contains(" : ")) {
                cleanUrl = url.split(" : ")[1].trim();
            }

            // 处理 HTTPS 格式
            if (cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) {
                // https://github.com/owner/repo.git
                String path = cleanUrl.replaceFirst("https?://[^/]+/", "");
                path = path.replaceFirst("\\.git$", "");
                String[] parts = path.split("/");
                if (parts.length >= 2) {
                    return new String[]{parts[0], parts[1]};
                }
            }
            // 处理 SSH 格式
            else if (cleanUrl.startsWith("git@")) {
                // git@github.com:owner/repo.git
                String path = cleanUrl.split(":")[1];
                path = path.replaceFirst("\\.git$", "");
                String[] parts = path.split("/");
                if (parts.length >= 2) {
                    return new String[]{parts[0], parts[1]};
                }
            }
        } catch (Exception e) {
            System.err.println("[AI Chat] Failed to parse remote URL: " + e.getMessage());
        }
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
        
        // 显示"正在处理"提示
        appendSystemMessage("💭 正在分析问题...");

        // 在后台线程处理
        new Thread(() -> {
            try {
                // **第一阶段：让 AI 决定调用哪个 API**
                String apiInstruction = askAIForApiCall(userMessage);
                
                System.out.println("[AI Chat] AI returned instruction: " + apiInstruction);
                
                // 移除"正在分析"提示
                SwingUtilities.invokeLater(() -> removeLastMessage());
                
                String githubData = null;
                
                // 检查 AI 是否返回了 API 调用指令
                if (apiInstruction != null && isValidApiInstruction(apiInstruction)) {
                    System.out.println("[AI Chat] Valid API instruction detected, proceeding to call GitHub API");
                    
                    // 显示"正在查询 GitHub"提示
                    SwingUtilities.invokeLater(() -> appendSystemMessage("🔍 正在查询 GitHub..."));
                    
                    // 解析并执行 API 调用
                    githubData = executeApiInstruction(apiInstruction);
                    
                    if (githubData != null) {
                        System.out.println("[AI Chat] GitHub data received successfully");
                    } else {
                        System.out.println("[AI Chat] WARNING: GitHub API returned null");
                    }
                    
                    // 移除"正在查询"提示
                    SwingUtilities.invokeLater(() -> removeLastMessage());
                } else {
                    System.out.println("[AI Chat] No valid API instruction, skipping GitHub API call");
                }
                
                // **第二阶段：让 AI 生成友好的回答**
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
        }).start();
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
            
            context.append("可用的 API：\n");
            context.append("1. get_repo - 获取仓库基本信息（star数、描述、语言等）\n");
            context.append("2. get_issues - 获取 issues 列表（参数：state=open/closed/all）\n");
            context.append("3. get_prs - 获取 pull requests/merge requests（参数：state=open/closed/all）\n");
            context.append("4. get_commits - 获取最近的提交记录\n");
            context.append("5. get_branches - 获取分支列表\n");
            context.append("6. get_releases - 获取发布版本\n");
            context.append("7. get_contents - 获取目录内容（参数：path，空表示根目录）\n");
            context.append("8. search_repos - 搜索仓库（参数：query）\n");
            context.append("9. search_issues - 搜索 issues（参数：query）\n");
            context.append("10. search_files - 搜索文件（参数：filename，例如：abc.java）\n");
            context.append("11. get_file_commits - 获取文件的提交历史（参数：filepath，例如：src/main/App.java）\n");
            context.append("\n");
            
            context.append("请分析用户的问题，如果需要调用 API，返回 JSON 格式：\n");
            context.append("{\"action\": \"get_repo\", \"owner\": \"facebook\", \"repo\": \"react\"}\n");
            context.append("{\"action\": \"get_issues\", \"state\": \"open\"}\n");
            context.append("{\"action\": \"get_contents\", \"path\": \"\"}\n");
            context.append("{\"action\": \"search_repos\", \"query\": \"machine learning\"}\n");
            context.append("{\"action\": \"search_files\", \"filename\": \"abc.java\"}\n");
            context.append("{\"action\": \"get_file_commits\", \"filepath\": \"src/main/App.java\"}\n");
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
     * 第二阶段：让 AI 基于 GitHub 数据生成友好的回答
     */
    private String askAIForFinalAnswer(String userQuestion, String githubData) {
        try {
            System.out.println("\n========== 第二阶段：生成友好回答 ==========");
            System.out.println("[AI Chat] User question: " + userQuestion);
            System.out.println("[AI Chat] Has GitHub data: " + (githubData != null && !githubData.isEmpty()));
            if (githubData != null && !githubData.isEmpty()) {
                System.out.println("[AI Chat] GitHub data preview: " + 
                    githubData.substring(0, Math.min(300, githubData.length())) + "...");
            }
            
            List<AIService.ChatMessage> messages = new ArrayList<>();
            
            // 系统提示
            StringBuilder systemPrompt = new StringBuilder();
            systemPrompt.append("你是一个友好的 GitHub 助手。\n");
            if (currentOwner != null && currentRepo != null) {
                systemPrompt.append("当前项目：").append(currentOwner).append("/").append(currentRepo).append("\n");
            }
            systemPrompt.append("请用中文友好地回答用户的问题。\n");
            
            messages.add(new AIService.ChatMessage("system", systemPrompt.toString()));
            messages.add(new AIService.ChatMessage("user", userQuestion));
            
            // 如果有 GitHub 数据，添加到上下文
            if (githubData != null && !githubData.isEmpty()) {
                messages.add(new AIService.ChatMessage("system", 
                    "以下是从 GitHub API 获取的数据，请基于这些数据回答用户的问题：\n\n" + githubData));
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
     * 执行 API 调用指令
     */
    private String executeApiInstruction(String instruction) {
        try {
            String platformName = (gitApiClient != null) ? gitApiClient.getPlatformName() : "Git";
            System.out.println("\n========== 执行 " + platformName + " API 调用 ==========");
            System.out.println("[AI Chat] Instruction: " + instruction);
            
            // 简单的 JSON 解析
            String action = extractJsonValue(instruction, "action");
            String owner = extractJsonValue(instruction, "owner");
            String repo = extractJsonValue(instruction, "repo");
            String state = extractJsonValue(instruction, "state");
            String query = extractJsonValue(instruction, "query");
            String path = extractJsonValue(instruction, "path");
            
            System.out.println("[AI Chat] Parsed - action: " + action);
            System.out.println("[AI Chat] Parsed - owner: " + owner);
            System.out.println("[AI Chat] Parsed - repo: " + repo);
            System.out.println("[AI Chat] Parsed - state: " + state);
            System.out.println("[AI Chat] Parsed - query: " + query);
            System.out.println("[AI Chat] Parsed - path: " + path);
            
            // 如果没有指定 owner/repo，使用当前上下文
            if ((owner == null || owner.isEmpty()) && currentOwner != null) {
                owner = currentOwner;
                System.out.println("[AI Chat] Using current owner: " + owner);
            }
            if ((repo == null || repo.isEmpty()) && currentRepo != null) {
                repo = currentRepo;
                System.out.println("[AI Chat] Using current repo: " + repo);
            }
            
            System.out.println("[AI Chat] Final - owner: " + owner + ", repo: " + repo);
            System.out.println("[AI Chat] Calling " + platformName + " API: " + action);
            
            if (gitApiClient == null) {
                System.err.println("[AI Chat] ERROR: Git API client not initialized");
                return null;
            }
            
            String result = null;
            
            // 根据 action 调用相应的 API
            switch (action) {
                case "get_repo":
                    System.out.println("[AI Chat] API Call: gitApiClient.getRepository(" + owner + ", " + repo + ")");
                    result = gitApiClient.getRepository(owner, repo);
                    break;
                case "get_issues":
                    String issueState = state != null ? state : "open";
                    System.out.println("[AI Chat] API Call: gitApiClient.getIssues(" + owner + ", " + repo + ", " + issueState + ")");
                    result = gitApiClient.getIssues(owner, repo, issueState);
                    break;
                case "get_prs":
                    String prState = state != null ? state : "open";
                    System.out.println("[AI Chat] API Call: gitApiClient.getPullRequests(" + owner + ", " + repo + ", " + prState + ")");
                    result = gitApiClient.getPullRequests(owner, repo, prState);
                    break;
                case "get_commits":
                    System.out.println("[AI Chat] API Call: gitApiClient.getCommits(" + owner + ", " + repo + ", " + currentBranch + ")");
                    result = gitApiClient.getCommits(owner, repo, currentBranch);
                    break;
                case "get_branches":
                    System.out.println("[AI Chat] API Call: gitApiClient.getBranches(" + owner + ", " + repo + ")");
                    result = gitApiClient.getBranches(owner, repo);
                    break;
                case "get_releases":
                    System.out.println("[AI Chat] API Call: gitApiClient.getReleases(" + owner + ", " + repo + ")");
                    result = gitApiClient.getReleases(owner, repo);
                    break;
                case "get_contents":
                    String contentsPath = path != null ? path : "";
                    System.out.println("[AI Chat] API Call: gitApiClient.getContents(" + owner + ", " + repo + ", \"" + contentsPath + "\")");
                    result = gitApiClient.getContents(owner, repo, contentsPath);
                    break;
                case "search_repos":
                    System.out.println("[AI Chat] API Call: gitApiClient.searchRepositories(" + query + ")");
                    result = gitApiClient.searchRepositories(query);
                    break;
                case "search_issues":
                    System.out.println("[AI Chat] API Call: gitApiClient.searchIssues(" + query + ")");
                    result = gitApiClient.searchIssues(query);
                    break;
                case "search_files":
                    String filename = extractJsonValue(instruction, "filename");
                    System.out.println("[AI Chat] API Call: gitApiClient.searchFiles(" + owner + ", " + repo + ", " + filename + ")");
                    result = gitApiClient.searchFiles(owner, repo, filename);
                    break;
                case "get_file_commits":
                    String filepath = extractJsonValue(instruction, "filepath");
                    System.out.println("[AI Chat] API Call: gitApiClient.getFileCommits(" + owner + ", " + repo + ", " + filepath + ", " + currentBranch + ")");
                    result = gitApiClient.getFileCommits(owner, repo, filepath, currentBranch);
                    break;
                default:
                    System.err.println("[AI Chat] ERROR: Unknown action: " + action);
                    System.out.println("========================================\n");
                    return null;
            }
            
            if (result != null) {
                System.out.println("[AI Chat] API Response received, length: " + result.length() + " chars");
                System.out.println("[AI Chat] Response preview: " + 
                    result.substring(0, Math.min(200, result.length())) + "...");
            } else {
                System.out.println("[AI Chat] API Response: null");
            }
            
            System.out.println("========== API 调用完成 ==========\n");
            return result;
            
        } catch (Exception e) {
            System.err.println("[AI Chat] ERROR executing API: " + e.getMessage());
            e.printStackTrace();
            System.out.println("========================================\n");
            return null;
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
}
