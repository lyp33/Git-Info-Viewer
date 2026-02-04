package com.gitviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 现代化输入对话框
 * Modern styled input dialog
 */
public class ModernInputDialog extends JDialog {
    private JTextField inputField;
    private String result;
    private boolean confirmed = false;
    
    /**
     * 构造函数
     * 
     * @param parent 父窗口
     * @param title 对话框标题
     * @param message 提示信息
     * @param initialValue 初始值
     */
    public ModernInputDialog(Window parent, String title, String message, String initialValue) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
        
        initializeUI(message, initialValue);
        
        setSize(420, 180);
        setLocationRelativeTo(parent);
        setResizable(false);
    }
    
    /**
     * 初始化UI
     */
    private void initializeUI(String message, String initialValue) {
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(Color.WHITE);
        
        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 20, 30));
        
        // 提示信息
        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        messageLabel.setForeground(new Color(60, 64, 67));
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainPanel.add(messageLabel);
        
        mainPanel.add(Box.createVerticalStrut(12));
        
        // 输入框
        inputField = new JTextField(initialValue != null ? initialValue : "");
        inputField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 13));
        inputField.setPreferredSize(new Dimension(340, 35));
        inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        inputField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // 输入框获得焦点时的边框效果
        inputField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                inputField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(70, 130, 180), 2),
                    BorderFactory.createEmptyBorder(4, 9, 4, 9)
                ));
            }
            
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                inputField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
                ));
            }
        });
        
        // 回车键确认
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleOK();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    handleCancel();
                }
            }
        });
        
        mainPanel.add(inputField);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)));
        
        // Cancel 按钮
        JButton cancelButton = createStyledButton("Cancel", new Color(95, 99, 104));
        cancelButton.setPreferredSize(new Dimension(90, 35));
        cancelButton.addActionListener(e -> handleCancel());
        buttonPanel.add(cancelButton);
        
        // OK 按钮
        JButton okButton = createStyledButton("OK", new Color(70, 130, 180));
        okButton.setPreferredSize(new Dimension(90, 35));
        okButton.addActionListener(e -> handleOK());
        buttonPanel.add(okButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // 设置默认按钮
        getRootPane().setDefaultButton(okButton);
    }
    
    /**
     * 创建样式化按钮
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
        
        // 鼠标悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            Color originalColor = bgColor;
            Color hoverColor = new Color(
                Math.min(255, bgColor.getRed() + 20),
                Math.min(255, bgColor.getGreen() + 20),
                Math.min(255, bgColor.getBlue() + 20)
            );
            
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(hoverColor);
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(originalColor);
            }
        });
        
        return button;
    }
    
    /**
     * 处理OK按钮
     */
    private void handleOK() {
        result = inputField.getText().trim();
        confirmed = true;
        dispose();
    }
    
    /**
     * 处理Cancel按钮
     */
    private void handleCancel() {
        result = null;
        confirmed = false;
        dispose();
    }
    
    /**
     * 显示对话框并返回输入结果
     * 
     * @param parent 父窗口
     * @param title 对话框标题
     * @param message 提示信息
     * @return 用户输入的文本，如果取消则返回null
     */
    public static String showInputDialog(Window parent, String title, String message) {
        return showInputDialog(parent, title, message, "");
    }
    
    /**
     * 显示对话框并返回输入结果
     * 
     * @param parent 父窗口
     * @param title 对话框标题
     * @param message 提示信息
     * @param initialValue 初始值
     * @return 用户输入的文本，如果取消则返回null
     */
    public static String showInputDialog(Window parent, String title, String message, String initialValue) {
        ModernInputDialog dialog = new ModernInputDialog(parent, title, message, initialValue);
        dialog.setVisible(true);
        return dialog.confirmed ? dialog.result : null;
    }
}
