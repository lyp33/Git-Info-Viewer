package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

/**
 * 文本过滤对话框
 * Dialog for filtering by text (supports fuzzy matching)
 */
public class TextFilterDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(TextFilterDialog.class);
    
    private JTextField filterTextField;
    private JButton okButton;
    private JButton cancelButton;
    private JButton clearButton;
    
    private String filterText;
    private boolean confirmed;
    private String columnName;
    
    /**
     * 构造函数
     * 
     * @param parent 父窗口
     * @param columnName 列名称（用于显示）
     * @param currentFilter 当前过滤文本（可以为null）
     */
    public TextFilterDialog(Dialog parent, String columnName, String currentFilter) {
        super(parent, "Filter by " + columnName, true);
        
        this.columnName = columnName;
        this.filterText = currentFilter;
        this.confirmed = false;
        
        logger.info("Opening Text Filter Dialog for column: {}", columnName);
        
        initializeUI();
        
        setSize(550, 220);
        setLocationRelativeTo(parent);
        setResizable(true);
    }
    
    /**
     * 初始化UI
     * Initialize UI components
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);
        
        // 创建主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);
        
        // 过滤文本输入部分
        mainPanel.add(createFilterSection());
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(createHintLabel());
        
        add(mainPanel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * 创建过滤文本输入部分
     * Create filter text input section
     */
    private JPanel createFilterSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        JLabel titleLabel = new JLabel(columnName + " (Fuzzy Match)");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(60, 64, 67));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        filterTextField = new JTextField();
        filterTextField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        filterTextField.setPreferredSize(new Dimension(400, 35));
        filterTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        filterTextField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        // 设置当前过滤文本
        if (filterText != null && !filterText.isEmpty()) {
            filterTextField.setText(filterText);
        }
        
        // 添加回车键监听
        filterTextField.addActionListener(e -> handleOk());
        
        panel.add(filterTextField, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 创建提示标签
     * Create hint label
     */
    private JLabel createHintLabel() {
        JLabel hintLabel = new JLabel("Enter keyword to filter (case-insensitive, partial match)");
        hintLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        hintLabel.setForeground(Color.GRAY);
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return hintLabel;
    }
    
    /**
     * 创建按钮面板
     * Create button panel
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        panel.setBackground(Color.WHITE);
        
        // Clear 按钮
        clearButton = createStyledButton("Clear", new Color(95, 99, 104));
        clearButton.setPreferredSize(new Dimension(80, 35));
        clearButton.addActionListener(e -> handleClear());
        panel.add(clearButton);
        
        // OK 按钮
        okButton = createStyledButton("OK", new Color(70, 130, 180));
        okButton.setPreferredSize(new Dimension(80, 35));
        okButton.addActionListener(e -> handleOk());
        panel.add(okButton);
        
        // Cancel 按钮
        cancelButton = createStyledButton("Cancel", new Color(95, 99, 104));
        cancelButton.setPreferredSize(new Dimension(80, 35));
        cancelButton.addActionListener(e -> handleCancel());
        panel.add(cancelButton);
        
        return panel;
    }
    
    /**
     * 创建样式化按钮
     * Create styled button
     */
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 13));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // 添加鼠标悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(bgColor.brighter());
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    /**
     * 处理Clear按钮
     * Handle clear button click
     */
    private void handleClear() {
        logger.info("User clicked Clear button");
        filterTextField.setText("");
        filterText = null;
        confirmed = true;
        dispose();
    }
    
    /**
     * 处理OK按钮
     * Handle OK button click
     */
    private void handleOk() {
        String text = filterTextField.getText();
        if (text != null) {
            text = text.trim();
        }
        
        logger.info("User clicked OK button with filter text: {}", text);
        
        filterText = (text != null && !text.isEmpty()) ? text : null;
        confirmed = true;
        dispose();
    }
    
    /**
     * 处理Cancel按钮
     * Handle cancel button click
     */
    private void handleCancel() {
        logger.info("User clicked Cancel button");
        confirmed = false;
        dispose();
    }
    
    /**
     * 获取过滤文本
     * Get filter text
     * 
     * @return 过滤文本，如果清空则返回null
     */
    public String getFilterText() {
        return filterText;
    }
    
    /**
     * 是否确认
     * Check if user confirmed
     * 
     * @return true if confirmed, false if cancelled
     */
    public boolean isConfirmed() {
        return confirmed;
    }
}
