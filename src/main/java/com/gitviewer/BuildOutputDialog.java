package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Build Output对话框
 * 显示构建输出日志，支持Ctrl+F搜索功能
 */
public class BuildOutputDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(BuildOutputDialog.class);
    
    private JTextArea outputTextArea;
    private JButton refreshButton;
    
    // API相关
    private PortalApiClient apiClient;
    private String tenantCode;
    private String token;
    private String buildId;
    private String appName;
    
    // 搜索相关
    private JPanel searchPanel;
    private JTextField searchField;
    private JLabel searchResultLabel;
    private int currentSearchIndex = -1;
    private List<Integer> searchPositions = new ArrayList<>();
    
    /**
     * 构造函数
     * 
     * @param parent 父窗口
     * @param apiClient API客户端
     * @param tenantCode 租户代码
     * @param token 认证Token
     * @param buildId 构建记录ID
     * @param appName 应用名称（用于标题显示）
     */
    public BuildOutputDialog(Window parent, PortalApiClient apiClient, 
                            String tenantCode, String token, String buildId, String appName) {
        super(parent, "Build Output: " + appName, Dialog.ModalityType.MODELESS);
        
        this.apiClient = apiClient;
        this.tenantCode = tenantCode;
        this.token = token;
        this.buildId = buildId;
        this.appName = appName;
        
        logger.info("Opening Build Output Dialog for buildId: {}, app: {}", buildId, appName);
        
        initializeUI();
        setLocationRelativeTo(parent);
        
        // 延迟加载日志，让对话框先显示出来
        SwingUtilities.invokeLater(this::loadBuildOutput);
    }
    
    /**
     * 初始化UI
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(900, 600);
        
        // 顶部面板 - 信息和按钮
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        
        JLabel infoLabel = new JLabel(String.format("App: %s | Build ID: %s", appName, buildId));
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        topPanel.add(infoLabel, BorderLayout.WEST);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("Refresh");
        refreshButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        refreshButton.addActionListener(e -> loadBuildOutput());
        buttonPanel.add(refreshButton);
        
        topPanel.add(buttonPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);
        
        // 搜索面板（初始隐藏）
        searchPanel = createSearchPanel();
        searchPanel.setVisible(false);
        add(searchPanel, BorderLayout.SOUTH);
        
        // 中间面板 - 文本区域
        outputTextArea = new JTextArea();
        outputTextArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, 11));
        outputTextArea.setEditable(false);
        outputTextArea.setLineWrap(false);
        outputTextArea.setWrapStyleWord(false);
        outputTextArea.setBackground(Color.BLACK);
        outputTextArea.setForeground(Color.WHITE);
        outputTextArea.setCaretColor(Color.WHITE);
        outputTextArea.setTabSize(4);
        outputTextArea.setText("Loading build output...");
        
        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, BorderLayout.CENTER);
        
        // 注册 Ctrl+F 快捷键
        registerSearchShortcut();
    }
    
    /**
     * 创建搜索面板
     */
    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        panel.setBackground(new Color(240, 240, 240));
        
        // 左侧：搜索输入框和按钮
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftPanel.setOpaque(false);
        
        JLabel searchLabel = new JLabel("Find:");
        searchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        leftPanel.add(searchLabel);
        
        searchField = new JTextField(30);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        searchField.addActionListener(e -> performSearch());
        leftPanel.add(searchField);
        
        JButton findButton = new JButton("Find");
        findButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        findButton.addActionListener(e -> performSearch());
        leftPanel.add(findButton);
        
        JButton nextButton = new JButton("Next");
        nextButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        nextButton.addActionListener(e -> findNext());
        leftPanel.add(nextButton);
        
        JButton prevButton = new JButton("Previous");
        prevButton.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        prevButton.addActionListener(e -> findPrevious());
        leftPanel.add(prevButton);
        
        searchResultLabel = new JLabel("");
        searchResultLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        searchResultLabel.setForeground(new Color(100, 100, 100));
        leftPanel.add(searchResultLabel);
        
        panel.add(leftPanel, BorderLayout.WEST);
        
        // 右侧：关闭按钮
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        
        JButton closeButton = new JButton("✕");
        closeButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeButton.setFocusPainted(false);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.addActionListener(e -> hideSearchPanel());
        rightPanel.add(closeButton);
        
        panel.add(rightPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * 注册 Ctrl+F 快捷键
     */
    private void registerSearchShortcut() {
        // Ctrl+F 显示搜索面板
        KeyStroke ctrlF = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK);
        getRootPane().registerKeyboardAction(
            e -> showSearchPanel(),
            ctrlF,
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        // ESC 隐藏搜索面板
        KeyStroke esc = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().registerKeyboardAction(
            e -> hideSearchPanel(),
            esc,
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        // F3 查找下一个
        KeyStroke f3 = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0);
        getRootPane().registerKeyboardAction(
            e -> findNext(),
            f3,
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        
        // Shift+F3 查找上一个
        KeyStroke shiftF3 = KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.SHIFT_DOWN_MASK);
        getRootPane().registerKeyboardAction(
            e -> findPrevious(),
            shiftF3,
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }
    
    /**
     * 显示搜索面板
     */
    private void showSearchPanel() {
        searchPanel.setVisible(true);
        searchField.requestFocus();
        searchField.selectAll();
    }
    
    /**
     * 隐藏搜索面板
     */
    private void hideSearchPanel() {
        searchPanel.setVisible(false);
        clearHighlights();
        searchPositions.clear();
        currentSearchIndex = -1;
        searchResultLabel.setText("");
    }
    
    /**
     * 执行搜索
     */
    private void performSearch() {
        String searchText = searchField.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            searchResultLabel.setText("Please enter search text");
            return;
        }
        
        String content = outputTextArea.getText();
        if (content == null || content.isEmpty()) {
            searchResultLabel.setText("No content to search");
            return;
        }
        
        // 清除之前的高亮
        clearHighlights();
        searchPositions.clear();
        currentSearchIndex = -1;
        
        // 查找所有匹配位置（不区分大小写）
        String lowerContent = content.toLowerCase();
        String lowerSearch = searchText.toLowerCase();
        int index = 0;
        
        while ((index = lowerContent.indexOf(lowerSearch, index)) != -1) {
            searchPositions.add(index);
            index += searchText.length();
        }
        
        if (searchPositions.isEmpty()) {
            searchResultLabel.setText("No matches found");
            return;
        }
        
        // 高亮所有匹配项
        highlightAllMatches(searchText);
        
        // 跳转到第一个匹配项
        currentSearchIndex = 0;
        highlightCurrentMatch(searchText);
        
        searchResultLabel.setText(String.format("%d of %d", currentSearchIndex + 1, searchPositions.size()));
    }
    
    /**
     * 查找下一个
     */
    private void findNext() {
        if (searchPositions.isEmpty()) {
            performSearch();
            return;
        }
        
        currentSearchIndex = (currentSearchIndex + 1) % searchPositions.size();
        highlightCurrentMatch(searchField.getText());
        searchResultLabel.setText(String.format("%d of %d", currentSearchIndex + 1, searchPositions.size()));
    }
    
    /**
     * 查找上一个
     */
    private void findPrevious() {
        if (searchPositions.isEmpty()) {
            performSearch();
            return;
        }
        
        currentSearchIndex = (currentSearchIndex - 1 + searchPositions.size()) % searchPositions.size();
        highlightCurrentMatch(searchField.getText());
        searchResultLabel.setText(String.format("%d of %d", currentSearchIndex + 1, searchPositions.size()));
    }
    
    /**
     * 高亮所有匹配项
     */
    private void highlightAllMatches(String searchText) {
        Highlighter highlighter = outputTextArea.getHighlighter();
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(
            new Color(255, 255, 0, 100));
        
        for (int pos : searchPositions) {
            try {
                highlighter.addHighlight(pos, pos + searchText.length(), painter);
            } catch (BadLocationException e) {
                // 忽略
            }
        }
    }
    
    /**
     * 高亮当前匹配项
     */
    private void highlightCurrentMatch(String searchText) {
        if (currentSearchIndex < 0 || currentSearchIndex >= searchPositions.size()) {
            return;
        }
        
        int pos = searchPositions.get(currentSearchIndex);
        
        // 滚动到当前匹配项
        try {
            Rectangle rect = outputTextArea.modelToView(pos);
            if (rect != null) {
                outputTextArea.scrollRectToVisible(rect);
            }
            outputTextArea.setCaretPosition(pos);
            outputTextArea.moveCaretPosition(pos + searchText.length());
        } catch (BadLocationException e) {
            // 忽略
        }
    }
    
    /**
     * 清除所有高亮
     */
    private void clearHighlights() {
        outputTextArea.getHighlighter().removeAllHighlights();
    }
    
    /**
     * 加载构建输出
     */
    private void loadBuildOutput() {
        logger.info("=== Loading Build Output ===");
        logger.info("BuildId: {}, TenantCode: {}", buildId, tenantCode);
        
        outputTextArea.setText("Loading build output...");
        refreshButton.setEnabled(false);
        
        SwingWorker<BuildOutputInfo, String> worker = new SwingWorker<BuildOutputInfo, String>() {
            @Override
            protected BuildOutputInfo doInBackground() throws Exception {
                logger.info("[BuildOutputDialog] Worker started");
                
                BuildOutputInfo info = new BuildOutputInfo();
                
                // 构建 API URL（注意：使用 portal-gw.insuremo.com）
                String url = "https://portal-gw.insuremo.com/eBao/1.0/ops/build/query_one?id=" + 
                             java.net.URLEncoder.encode(buildId, java.nio.charset.StandardCharsets.UTF_8);
                info.apiUrl = url;
                
                logger.info("[BuildOutputDialog] API URL: {}", url);
                logger.info("[BuildOutputDialog] Tenant: {}", tenantCode);
                logger.info("[BuildOutputDialog] Token: {}...", token != null && token.length() > 8 ? 
                           token.substring(0, 4) + "..." + token.substring(token.length() - 4) : "[INVALID]");
                
                // 发布 API 信息到 UI
                StringBuilder apiInfo = new StringBuilder();
                apiInfo.append("=== API Request Info ===\n\n");
                apiInfo.append("URL:\n").append(url).append("\n\n");
                apiInfo.append("Headers:\n");
                apiInfo.append("  x-mo-target-tenant: ").append(tenantCode).append("\n");
                apiInfo.append("  authorization: Bearer ").append(
                    token != null && token.length() > 8 ? 
                    token.substring(0, 4) + "..." + token.substring(token.length() - 4) : "[INVALID]"
                ).append("\n\n");
                
                publish(apiInfo.toString());
                publish("Loading...\n\n");
                
                // 调用 API
                try {
                    String output = apiClient.getBuildOutputById(tenantCode, token, buildId);
                    logger.info("[BuildOutputDialog] Build output received, length: {}", 
                               output != null ? output.length() : 0);
                    info.buildOutput = output;
                    return info;
                } catch (Exception e) {
                    logger.error("[BuildOutputDialog] Failed to get build output", e);
                    info.error = e.getMessage();
                    if (e.getCause() != null) {
                        info.error += "\nCause: " + e.getCause().getMessage();
                    }
                    throw e;
                }
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                // 逐步追加信息到文本区域
                for (String chunk : chunks) {
                    outputTextArea.append(chunk);
                }
            }
            
            @Override
            protected void done() {
                logger.info("[BuildOutputDialog] Worker done");
                try {
                    BuildOutputInfo info = get();
                    
                    outputTextArea.append("=== API Response ===\n\n");
                    
                    if (info.buildOutput == null || info.buildOutput.isEmpty()) {
                        outputTextArea.append("(No build output available)");
                    } else {
                        outputTextArea.append(info.buildOutput);
                    }
                    
                    outputTextArea.setCaretPosition(0);  // 滚动到顶部
                    logger.info("[BuildOutputDialog] Build output displayed successfully");
                } catch (Exception e) {
                    logger.error("[BuildOutputDialog] Error in done()", e);
                    String errorMsg = "\n\nFailed to load build output:\n" + e.getMessage();
                    if (e.getCause() != null) {
                        errorMsg += "\nCause: " + e.getCause().getMessage();
                    }
                    outputTextArea.append(errorMsg);
                    
                    JOptionPane.showMessageDialog(BuildOutputDialog.this,
                        errorMsg,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                } finally {
                    refreshButton.setEnabled(true);
                    logger.info("[BuildOutputDialog] Worker completed");
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Build Output 信息容器
     */
    private static class BuildOutputInfo {
        String apiUrl;
        String buildOutput;
        String error;
    }
}
