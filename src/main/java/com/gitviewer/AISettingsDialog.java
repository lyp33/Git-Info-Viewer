package com.gitviewer;

import javax.swing.*;
import java.awt.*;

/**
 * AI Chat 设置对话框
 */
public class AISettingsDialog extends JDialog {

    private JTextField githubTokenField;
    private JTextField aiApiUrlField;
    private JTextField aiApiKeyField;
    private JTextField aiModelField;
    private boolean confirmed = false;

    public AISettingsDialog(Frame parent) {
        super(parent, "AI Chat Settings", true);
        initializeUI();
        loadSettings();
        setLocationRelativeTo(parent);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(600, 400);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // GitHub Token
        mainPanel.add(createSectionLabel("GitHub Configuration"));
        mainPanel.add(Box.createVerticalStrut(10));
        
        JPanel githubPanel = new JPanel(new BorderLayout(10, 5));
        githubPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JLabel githubLabel = new JLabel("GitHub Token:");
        githubLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        githubTokenField = new JTextField();
        githubTokenField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        githubPanel.add(githubLabel, BorderLayout.NORTH);
        githubPanel.add(githubTokenField, BorderLayout.CENTER);
        mainPanel.add(githubPanel);

        mainPanel.add(Box.createVerticalStrut(20));

        // AI API Configuration
        mainPanel.add(createSectionLabel("AI API Configuration"));
        mainPanel.add(Box.createVerticalStrut(10));

        // API URL
        JPanel urlPanel = new JPanel(new BorderLayout(10, 5));
        urlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JLabel urlLabel = new JLabel("API URL:");
        urlLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        aiApiUrlField = new JTextField();
        aiApiUrlField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        urlPanel.add(urlLabel, BorderLayout.NORTH);
        urlPanel.add(aiApiUrlField, BorderLayout.CENTER);
        mainPanel.add(urlPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // API Key
        JPanel keyPanel = new JPanel(new BorderLayout(10, 5));
        keyPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JLabel keyLabel = new JLabel("API Key:");
        keyLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        aiApiKeyField = new JPasswordField();
        aiApiKeyField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        keyPanel.add(keyLabel, BorderLayout.NORTH);
        keyPanel.add(aiApiKeyField, BorderLayout.CENTER);
        mainPanel.add(keyPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // Model
        JPanel modelPanel = new JPanel(new BorderLayout(10, 5));
        modelPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JLabel modelLabel = new JLabel("Model:");
        modelLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        aiModelField = new JTextField();
        aiModelField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        modelPanel.add(modelLabel, BorderLayout.NORTH);
        modelPanel.add(aiModelField, BorderLayout.CENTER);
        mainPanel.add(modelPanel);

        add(mainPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        okButton.addActionListener(e -> {
            saveSettings();
            confirmed = true;
            dispose();
        });

        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(new Color(32, 33, 36));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void loadSettings() {
        AppSettings settings = AppSettings.getInstance();
        githubTokenField.setText(settings.getGithubToken());
        aiApiUrlField.setText(settings.getAiApiUrl());
        aiApiKeyField.setText(settings.getAiApiKey());
        aiModelField.setText(settings.getAiModel());
    }

    private void saveSettings() {
        AppSettings settings = AppSettings.getInstance();
        settings.setGithubToken(githubTokenField.getText().trim());
        settings.setAiApiUrl(aiApiUrlField.getText().trim());
        settings.setAiApiKey(aiApiKeyField.getText().trim());
        settings.setAiModel(aiModelField.getText().trim());
        settings.saveSettings();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
