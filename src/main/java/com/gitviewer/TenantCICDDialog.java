package com.gitviewer;

import javax.swing.*;
import java.awt.*;

/**
 * Tenant CI/CD 对话框（占位符）
 * 未来功能的占位符
 */
public class TenantCICDDialog extends JDialog {

    public TenantCICDDialog(Frame parent) {
        super(parent, "Tenant CI/CD", true);
        initializeUI();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(20, 20));
        setSize(400, 200);
        setResizable(false);

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 20, 30));

        // 图标
        JLabel iconLabel = new JLabel("🚧");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 48));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(iconLabel);
        mainPanel.add(Box.createVerticalStrut(20));

        // 消息
        JLabel messageLabel = new JLabel("Tenant CI/CD functionality coming soon");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(messageLabel);

        add(mainPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        okButton.addActionListener(e -> dispose());
        buttonPanel.add(okButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }
}
