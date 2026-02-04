package com.gitviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 分组标题面板
 * Group header panel for favorite groups
 */
public class GroupHeaderPanel extends JPanel {
    private JCheckBox groupCheckbox;
    private JLabel groupNameLabel;
    private JLabel expandIcon;
    private FavoriteGroup group;
    private GroupActionListener actionListener;
    
    // 颜色定义
    private static final Color HEADER_BG_COLOR = new Color(245, 245, 245);
    private static final Color HEADER_HOVER_COLOR = new Color(235, 235, 235);
    private static final Color TEXT_COLOR = new Color(60, 64, 67);
    
    /**
     * 分组操作监听器接口
     */
    public interface GroupActionListener {
        void onGroupCheckboxChanged(FavoriteGroup group, boolean selected);
        void onGroupExpandToggled(FavoriteGroup group);
        void onGroupRename(FavoriteGroup group, String newName);
        void onGroupDelete(FavoriteGroup group);
    }
    
    /**
     * 构造函数
     * 
     * @param group 分组对象
     * @param listener 操作监听器
     */
    public GroupHeaderPanel(FavoriteGroup group, GroupActionListener listener) {
        this.group = group;
        this.actionListener = listener;
        
        initializeUI();
        setupListeners();
    }
    
    /**
     * 初始化UI
     */
    private void initializeUI() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        setBackground(HEADER_BG_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(218, 220, 224)),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // 展开/折叠图标 - 使用 [+] 和 [-] 符号
        expandIcon = new JLabel(group.isExpanded() ? "[-]" : "[+]");
        expandIcon.setFont(new Font("Monospaced", Font.BOLD, 12));
        expandIcon.setForeground(new Color(70, 130, 180));
        add(expandIcon);
        
        // 分组复选框
        groupCheckbox = new JCheckBox();
        groupCheckbox.setBackground(HEADER_BG_COLOR);
        groupCheckbox.setFocusPainted(false);
        add(groupCheckbox);
        
        // 分组名称
        groupNameLabel = new JLabel(group.getName() + " (" + group.size() + ")");
        groupNameLabel.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 12));
        groupNameLabel.setForeground(TEXT_COLOR);
        add(groupNameLabel);
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 复选框变化监听
        groupCheckbox.addActionListener(e -> {
            if (actionListener != null) {
                actionListener.onGroupCheckboxChanged(group, groupCheckbox.isSelected());
            }
        });
        
        // 鼠标悬停效果
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(HEADER_HOVER_COLOR);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(HEADER_BG_COLOR);
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                // 左键点击：切换展开/折叠
                if (e.getButton() == MouseEvent.BUTTON1) {
                    toggleExpanded();
                }
                // 右键点击：显示菜单
                else if (e.getButton() == MouseEvent.BUTTON3) {
                    showContextMenu(e);
                }
            }
        });
        
        // 标签点击切换展开/折叠
        groupNameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    toggleExpanded();
                }
                else if (e.getButton() == MouseEvent.BUTTON3) {
                    showContextMenu(e);
                }
            }
        });
        
        expandIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    toggleExpanded();
                }
                else if (e.getButton() == MouseEvent.BUTTON3) {
                    showContextMenu(e);
                }
            }
        });
    }
    
    /**
     * 显示右键菜单
     */
    private void showContextMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        
        // 重命名选项
        JMenuItem renameItem = new JMenuItem("Rename Group");
        renameItem.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        renameItem.addActionListener(evt -> handleRename());
        menu.add(renameItem);
        
        // 删除选项
        JMenuItem deleteItem = new JMenuItem("Delete Group");
        deleteItem.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
        deleteItem.addActionListener(evt -> handleDelete());
        menu.add(deleteItem);
        
        menu.show(e.getComponent(), e.getX(), e.getY());
    }
    
    /**
     * 处理重命名
     */
    private void handleRename() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        String newName = ModernInputDialog.showInputDialog(
            owner,
            "Rename Group",
            "Enter new group name:",
            group.getName());
        
        if (newName != null && !newName.trim().isEmpty() && !newName.equals(group.getName())) {
            if (actionListener != null) {
                actionListener.onGroupRename(group, newName.trim());
            }
        }
    }
    
    /**
     * 处理删除
     */
    private void handleDelete() {
        int choice = JOptionPane.showConfirmDialog(
            this,
            "Delete group '" + group.getName() + "'?\n\n" +
            "All applications in this group will be moved to Ungrouped.",
            "Confirm Delete",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (choice == JOptionPane.OK_OPTION) {
            if (actionListener != null) {
                actionListener.onGroupDelete(group);
            }
        }
    }
    
    /**
     * 切换展开/折叠状态
     */
    private void toggleExpanded() {
        group.setExpanded(!group.isExpanded());
        expandIcon.setText(group.isExpanded() ? "▼" : "▶");
        
        if (actionListener != null) {
            actionListener.onGroupExpandToggled(group);
        }
    }
    
    /**
     * 更新分组信息显示
     */
    public void updateGroupInfo() {
        groupNameLabel.setText(group.getName() + " (" + group.size() + ")");
        expandIcon.setText(group.isExpanded() ? "[-]" : "[+]");
    }
    
    /**
     * 获取分组复选框
     */
    public JCheckBox getGroupCheckbox() {
        return groupCheckbox;
    }
    
    /**
     * 获取分组对象
     */
    public FavoriteGroup getGroup() {
        return group;
    }
}
