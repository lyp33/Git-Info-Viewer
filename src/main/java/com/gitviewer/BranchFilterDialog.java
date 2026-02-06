package com.gitviewer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Branch过滤对话框
 * Dialog for filtering build results by Git branch
 */
public class BranchFilterDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(BranchFilterDialog.class);
    
    private JComboBox<String> branchComboBox;
    private JButton okButton;
    private JButton cancelButton;
    private JButton clearButton;
    
    private String selectedBranch;
    private boolean confirmed;
    
    // 防抖Timer，用于延迟过滤
    private javax.swing.Timer filterTimer;
    
    // 防止KeyListener递归调用的标志
    private boolean isUpdatingComboBox = false;
    
    // KeyListener引用，用于清理
    private KeyAdapter branchKeyListener;
    
    // 分支列表
    private List<String> branchList;
    
    /**
     * 构造函数
     * 
     * @param parent 父窗口
     * @param branches 分支列表
     * @param currentBranch 当前选中的分支（可以为null）
     */
    public BranchFilterDialog(Dialog parent, List<String> branches, String currentBranch) {
        super(parent, "Filter by Branch", true);
        
        this.branchList = branches;
        this.selectedBranch = currentBranch;
        this.confirmed = false;
        
        logger.info("Opening Branch Filter Dialog with {} branches", branches.size());
        
        initializeUI();
        
        setSize(500, 200);
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
        
        // Branch选择部分
        mainPanel.add(createBranchSection());
        
        add(mainPanel, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
    }
    
    /**
     * 创建分支选择部分
     * Create branch selection section
     */
    private JPanel createBranchSection() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        JLabel titleLabel = new JLabel("Git Branch");
        titleLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(60, 64, 67));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        branchComboBox = new JComboBox<>();
        branchComboBox.setEditable(true);
        branchComboBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        branchComboBox.setPreferredSize(new Dimension(350, 35));
        branchComboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        // 填充分支列表
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String branch : branchList) {
            model.addElement(branch);
        }
        branchComboBox.setModel(model);
        
        // 设置当前选中的分支
        if (selectedBranch != null && !selectedBranch.isEmpty()) {
            branchComboBox.setSelectedItem(selectedBranch);
        }
        
        // 设置过滤功能
        setupBranchFiltering();
        
        panel.add(branchComboBox, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * 设置分支过滤
     * Setup branch filtering with debounce
     */
    private void setupBranchFiltering() {
        JTextField editor = (JTextField) branchComboBox.getEditor().getEditorComponent();
        
        // 创建防抖Timer（300ms延迟）
        filterTimer = new javax.swing.Timer(300, e -> {
            if (!isUpdatingComboBox) {
                performBranchFiltering(editor.getText());
            }
        });
        filterTimer.setRepeats(false);
        
        branchKeyListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                // 防止递归调用
                if (isUpdatingComboBox) {
                    return;
                }
                
                // 重启Timer（防抖）
                filterTimer.restart();
            }
        };
        
        editor.addKeyListener(branchKeyListener);
        
        logger.debug("Branch filtering setup complete");
    }
    
    /**
     * 执行分支过滤
     * Perform branch filtering
     */
    private void performBranchFiltering(String input) {
        isUpdatingComboBox = true;
        try {
            String lowerInput = input.toLowerCase();
            
            logger.debug("Filtering branches with text: {}", input);
            
            // 过滤分支列表
            List<String> filteredBranches = branchList.stream()
                .filter(branch -> branch.toLowerCase().contains(lowerInput))
                .collect(Collectors.toList());
            
            logger.debug("Filtered {} branches from {} total", filteredBranches.size(), branchList.size());
            
            // 更新下拉框
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            for (String branch : filteredBranches) {
                model.addElement(branch);
            }
            
            branchComboBox.setModel(model);
            
            // 保持编辑器文本
            JTextField editor = (JTextField) branchComboBox.getEditor().getEditorComponent();
            editor.setText(input);
            
            // 只在有结果时显示下拉框
            if (!filteredBranches.isEmpty()) {
                branchComboBox.showPopup();
            }
        } finally {
            isUpdatingComboBox = false;
        }
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
        branchComboBox.setSelectedItem("");
        selectedBranch = null;
        confirmed = true;
        dispose();
    }
    
    /**
     * 处理OK按钮
     * Handle OK button click
     */
    private void handleOk() {
        String branch = (String) branchComboBox.getSelectedItem();
        if (branch != null) {
            branch = branch.trim();
        }
        
        logger.info("User clicked OK button with branch: {}", branch);
        
        selectedBranch = (branch != null && !branch.isEmpty()) ? branch : null;
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
     * 获取选中的分支
     * Get selected branch
     * 
     * @return 选中的分支，如果清空则返回null
     */
    public String getSelectedBranch() {
        return selectedBranch;
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
    
    /**
     * 资源清理
     * Resource cleanup
     */
    @Override
    public void dispose() {
        logger.info("=== Disposing Branch Filter Dialog ===");
        
        // 停止并清理防抖Timer
        if (filterTimer != null) {
            filterTimer.stop();
            filterTimer = null;
            logger.debug("Filter timer stopped");
        }
        
        // 移除KeyListener
        if (branchKeyListener != null && branchComboBox != null) {
            try {
                JTextField editor = (JTextField) branchComboBox.getEditor().getEditorComponent();
                editor.removeKeyListener(branchKeyListener);
                branchKeyListener = null;
                logger.debug("Branch KeyListener removed");
            } catch (Exception e) {
                logger.warn("Failed to remove branch KeyListener", e);
            }
        }
        
        super.dispose();
    }
}
