package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Date;

/**
 * 版本代码模式配置对话框
 * Dialog for configuring version code pattern
 */
public class VersionPatternDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(VersionPatternDialog.class);
    
    private JTextField patternField;
    private JLabel previewPatternValue;
    private JLabel previewResultValue;
    private String currentBranch;
    private boolean confirmed = false;
    
    /**
     * 构造函数
     * 
     * @param parent 父窗口
     * @param currentPattern 当前配置的模式
     * @param currentBranch 当前分支名称（用于预览）
     */
    public VersionPatternDialog(Frame parent, String currentPattern, String currentBranch) {
        super(parent, "Configure Version Code Pattern", true);
        
        this.currentBranch = currentBranch != null ? currentBranch : "master";
        
        logger.info("Opening Version Pattern Dialog with pattern: {}, branch: {}", 
                   currentPattern, this.currentBranch);
        
        initializeUI(currentPattern);
        
        setSize(600, 650);
        setLocationRelativeTo(parent);
        setResizable(false);
    }
    
    /**
     * 初始化UI
     * Initialize UI components
     */
    private void initializeUI(String currentPattern) {
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(Color.WHITE);
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);
        
        // Pattern input section
        mainPanel.add(createPatternInputSection(currentPattern));
        mainPanel.add(Box.createVerticalStrut(15));
        
        // Live preview section
        mainPanel.add(createLivePreviewSection());
        mainPanel.add(Box.createVerticalStrut(15));
        
        // Help text section
        mainPanel.add(createHelpTextSection());
        
        add(mainPanel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
        
        // 初始化预览
        updateLivePreview();
    }
    
    /**
     * 创建模式输入部分
     * Create pattern input section
     */
    private JPanel createPatternInputSection(String currentPattern) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        
        JLabel titleLabel = new JLabel("Pattern Template");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 13));
        titleLabel.setForeground(new Color(60, 64, 67));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        patternField = new JTextField(currentPattern != null ? currentPattern : "");
        patternField.setFont(new Font("Consolas", Font.PLAIN, 14));
        patternField.setPreferredSize(new Dimension(550, 35));
        patternField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // 添加文档监听器以实时更新预览
        patternField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateLivePreview();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                updateLivePreview();
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateLivePreview();
            }
        });
        
        panel.add(patternField, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建实时预览部分
     * Create live preview section
     */
    private JPanel createLivePreviewSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 248, 250));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        // Title
        JLabel titleLabel = new JLabel("Live Preview");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        titleLabel.setForeground(new Color(60, 64, 67));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));
        
        // Pattern line
        JPanel patternLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        patternLine.setBackground(new Color(245, 248, 250));
        patternLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel previewPatternLabel = new JLabel("Pattern: ");
        previewPatternLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        previewPatternLabel.setForeground(new Color(95, 99, 104));
        patternLine.add(previewPatternLabel);
        
        previewPatternValue = new JLabel();
        previewPatternValue.setFont(new Font("Consolas", Font.BOLD, 11));
        previewPatternValue.setForeground(new Color(60, 64, 67));
        patternLine.add(previewPatternValue);
        
        panel.add(patternLine);
        panel.add(Box.createVerticalStrut(5));
        
        // Result line
        JPanel resultLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        resultLine.setBackground(new Color(245, 248, 250));
        resultLine.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel previewResultLabel = new JLabel("Result:  ");
        previewResultLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        previewResultLabel.setForeground(new Color(95, 99, 104));
        resultLine.add(previewResultLabel);
        
        previewResultValue = new JLabel();
        previewResultValue.setFont(new Font("Consolas", Font.BOLD, 12));
        previewResultValue.setForeground(new Color(26, 115, 232));
        resultLine.add(previewResultValue);
        
        panel.add(resultLine);
        panel.add(Box.createVerticalStrut(10));
        
        // Note
        JLabel noteLabel = new JLabel("(Updates automatically as you type)");
        noteLabel.setFont(new Font("Microsoft YaHei UI", Font.ITALIC, 10));
        noteLabel.setForeground(new Color(95, 99, 104));
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(noteLabel);
        
        return panel;
    }
    
    /**
     * 创建帮助文本部分
     * Create help text section
     */
    private JPanel createHelpTextSection() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        
        JLabel titleLabel = new JLabel("Available Placeholders");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        titleLabel.setForeground(new Color(60, 64, 67));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        JTextArea helpText = new JTextArea();
        helpText.setEditable(false);
        helpText.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 10));
        helpText.setForeground(new Color(60, 64, 67));
        helpText.setBackground(Color.WHITE);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        
        String helpContent = 
            "Branch Information:\n" +
            "  {branch}  - Current Git branch name\n" +
            "              Example: master, develop, feature/login\n" +
            "\n" +
            "Date Components:\n" +
            "  {YYYY}    - 4-digit year        (e.g., 2026)\n" +
            "  {MM}      - 2-digit month       (e.g., 02 for February)\n" +
            "  {DD}      - 2-digit day         (e.g., 06)\n" +
            "  {YYYYMMDD} - Combined date      (e.g., 20260206)\n" +
            "\n" +
            "Time Components:\n" +
            "  {HH}      - 2-digit hour (24h)  (e.g., 17 for 5 PM)\n" +
            "  {MI}      - 2-digit minute      (e.g., 59)\n" +
            "  {SS}      - 2-digit second      (e.g., 50)\n" +
            "  {HHMMSS}  - Combined time       (e.g., 175950)\n" +
            "\n" +
            "Combined:\n" +
            "  {YYYYMMDDHHMMSS} - Full datetime (e.g., 20260206175950)\n" +
            "\n" +
            "Pattern Examples:\n" +
            "  {branch}_{YYYYMMDD}_{HHMMSS}\n" +
            "    → master_20260206_175950\n" +
            "\n" +
            "  v{YYYY}.{MM}.{DD}_{branch}\n" +
            "    → v2026.02.06_master\n" +
            "\n" +
            "  release_{YYYYMMDD}\n" +
            "    → release_20260206\n" +
            "\n" +
            "  {branch}_build_{HH}{MI}\n" +
            "    → master_build_1759\n" +
            "\n" +
            "You can combine placeholders with any literal text (letters, numbers,\n" +
            "underscores, hyphens, dots, etc.) to create your custom format.";
        
        helpText.setText(helpContent);
        helpText.setCaretPosition(0);
        
        JScrollPane scrollPane = new JScrollPane(helpText);
        scrollPane.setPreferredSize(new Dimension(550, 280));
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建按钮面板
     * Create button panel
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        panel.setBackground(Color.WHITE);
        
        // Save button
        JButton saveButton = new JButton("Save");
        saveButton.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 13));
        saveButton.setPreferredSize(new Dimension(100, 35));
        saveButton.setBackground(new Color(70, 130, 180));
        saveButton.setForeground(Color.WHITE);
        saveButton.setOpaque(true);
        saveButton.setContentAreaFilled(true);
        saveButton.setFocusPainted(false);
        saveButton.setBorderPainted(false);
        saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveButton.addActionListener(e -> validateAndSave());
        
        // Hover effect
        saveButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                saveButton.setBackground(new Color(90, 150, 200));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                saveButton.setBackground(new Color(70, 130, 180));
            }
        });
        
        panel.add(saveButton);
        
        // Cancel button
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 13));
        cancelButton.setPreferredSize(new Dimension(100, 35));
        cancelButton.setBackground(new Color(95, 99, 104));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setOpaque(true);
        cancelButton.setContentAreaFilled(true);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorderPainted(false);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
        
        // Hover effect
        cancelButton.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                cancelButton.setBackground(new Color(115, 119, 124));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                cancelButton.setBackground(new Color(95, 99, 104));
            }
        });
        
        panel.add(cancelButton);
        
        return panel;
    }
    
    /**
     * 更新实时预览
     * Update live preview
     */
    private void updateLivePreview() {
        String pattern = patternField.getText();
        
        // 显示pattern
        if (pattern == null || pattern.trim().isEmpty()) {
            previewPatternValue.setText("{branch}_{YYYYMMDDHHMMSS} (default)");
        } else {
            previewPatternValue.setText(pattern);
        }
        
        // 生成并显示结果
        try {
            String result = VersionPatternGenerator.generateVersionCode(
                pattern, currentBranch, new Date());
            previewResultValue.setText(result);
            previewResultValue.setForeground(new Color(26, 115, 232));  // Blue
        } catch (Exception e) {
            previewResultValue.setText("Error generating preview");
            previewResultValue.setForeground(Color.RED);
            logger.error("Failed to generate preview", e);
        }
    }
    
    /**
     * 验证并保存
     * Validate pattern and save
     */
    private void validateAndSave() {
        String pattern = patternField.getText().trim();
        
        // 验证模式
        if (!VersionPatternGenerator.validatePattern(pattern)) {
            String errorMessage = VersionPatternGenerator.getValidationErrorMessage(pattern);
            showValidationError(errorMessage);
            return;
        }
        
        logger.info("Pattern validated successfully: {}", pattern);
        confirmed = true;
        dispose();
    }
    
    /**
     * 显示验证错误
     * Show validation error dialog
     */
    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this,
            message,
            "Invalid Pattern",
            JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * 获取配置的模式
     * Get configured pattern
     * 
     * @return 模式字符串
     */
    public String getPattern() {
        return patternField.getText().trim();
    }
    
    /**
     * 是否已确认
     * Check if user confirmed
     * 
     * @return true if user clicked Save
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}
