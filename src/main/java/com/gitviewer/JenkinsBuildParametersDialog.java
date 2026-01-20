package com.gitviewer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Jenkins 构建参数对话框
 * 用于输入构建参数并触发构建
 */
public class JenkinsBuildParametersDialog extends JDialog {

    private JenkinsApiClient apiClient;
    private String jobPath;
    private String jobName;
    private Map<String, String> prefilledParameters;
    
    private List<JenkinsBuildParameter> parameters;
    private Map<String, JComponent> parameterComponents;

    public JenkinsBuildParametersDialog(Frame parent, JenkinsApiClient apiClient, 
            String jobPath, String jobName, Map<String, String> prefilledParameters) {
        super(parent, "Build Parameters - " + jobName, true);
        this.apiClient = apiClient;
        this.jobPath = jobPath;
        this.jobName = jobName;
        this.prefilledParameters = prefilledParameters;
        this.parameterComponents = new HashMap<>();
        
        loadParametersAndInitUI();
        setLocationRelativeTo(parent);
    }

    /**
     * 加载参数定义并初始化 UI
     */
    private void loadParametersAndInitUI() {
        SwingWorker<List<JenkinsBuildParameter>, Void> worker = new SwingWorker<List<JenkinsBuildParameter>, Void>() {
            @Override
            protected List<JenkinsBuildParameter> doInBackground() throws Exception {
                return apiClient.fetchBuildParameters(jobPath);
            }

            @Override
            protected void done() {
                try {
                    parameters = get();
                    
                    // 打印参数定义信息
                    System.out.println("=== Build Parameters Definition ===");
                    System.out.println("Total parameters: " + parameters.size());
                    for (JenkinsBuildParameter param : parameters) {
                        System.out.println("Parameter: " + param.getName());
                        System.out.println("  Type: " + param.getType());
                        System.out.println("  Default Value: " + param.getDefaultValue());
                        System.out.println("  Description: " + param.getDescription());
                        if (param.isChoiceParameter()) {
                            System.out.println("  Choices: " + param.getChoices());
                        }
                    }
                    
                    // 打印预填充参数信息
                    if (prefilledParameters != null && !prefilledParameters.isEmpty()) {
                        System.out.println("=== Prefilled Parameters ===");
                        for (Map.Entry<String, String> entry : prefilledParameters.entrySet()) {
                            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
                        }
                    } else {
                        System.out.println("=== No Prefilled Parameters ===");
                    }
                    System.out.println("===================================");
                    
                    if (parameters.isEmpty()) {
                        // 没有参数，直接触发构建
                        int result = JOptionPane.showConfirmDialog(JenkinsBuildParametersDialog.this,
                            "This job has no parameters. Trigger build now?",
                            "Confirm Build",
                            JOptionPane.YES_NO_OPTION);
                        
                        if (result == JOptionPane.YES_OPTION) {
                            triggerBuild(new HashMap<>());
                        }
                        dispose();
                    } else {
                        initializeUI();
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(JenkinsBuildParametersDialog.this,
                        "Failed to load build parameters: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    dispose();
                }
            }
        };
        
        worker.execute();
    }

    /**
     * 初始化 UI
     */
    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        
        // 计算对话框大小 - 增加高度以容纳所有参数
        int height = Math.min(700, 200 + parameters.size() * 60);
        setSize(700, height);

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 10, 30));

        // 标题
        JLabel titleLabel = new JLabel("Build Parameters");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));

        // 说明文本
        JTextArea descriptionArea = new JTextArea("Configure build parameters and click Build to start the build process.");
        descriptionArea.setEditable(false);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setBackground(new Color(240, 248, 255));
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        descriptionArea.setMaximumSize(new Dimension(600, 35));
        descriptionArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(descriptionArea);
        mainPanel.add(Box.createVerticalStrut(15));

        // 创建参数表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        for (JenkinsBuildParameter param : parameters) {
            // 参数标签
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.anchor = GridBagConstraints.NORTHEAST;
            gbc.weightx = 0;
            
            JLabel label = new JLabel(param.getName() + ":");
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            if (param.getDescription() != null && !param.getDescription().isEmpty()) {
                label.setToolTipText(param.getDescription());
            }
            formPanel.add(label, gbc);

            // 参数输入组件
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1.0;
            
            JComponent inputComponent = createInputComponent(param);
            parameterComponents.put(param.getName(), inputComponent);
            formPanel.add(inputComponent, gbc);

            row++;
        }

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setBorder(null);
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(scrollPane);

        add(mainPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton buildButton = new JButton("Build");
        buildButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        buildButton.addActionListener(e -> validateAndTriggerBuild());
        buttonPanel.add(buildButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * 根据参数类型创建输入组件
     */
    private JComponent createInputComponent(JenkinsBuildParameter param) {
        // 特殊处理：versions 参数始终使用文本框（即使定义为 Choice）
        if ("versions".equals(param.getName())) {
            JTextField textField = new JTextField();
            textField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            String value = getParameterValue(param);
            if (value != null) {
                textField.setText(value);
            }
            
            textField.setPreferredSize(new Dimension(450, 28));
            return textField;
        }
        
        if (param.isChoiceParameter()) {
            // 选择参数 - 使用下拉框
            JComboBox<String> comboBox = new JComboBox<>();
            for (String choice : param.getChoices()) {
                comboBox.addItem(choice);
            }
            
            // 设置默认值或预填充值
            String value = getParameterValue(param);
            if (value != null) {
                comboBox.setSelectedItem(value);
            }
            
            comboBox.setPreferredSize(new Dimension(450, 28));
            return comboBox;
            
        } else if (param.isBooleanParameter()) {
            // 布尔参数 - 使用复选框
            JCheckBox checkBox = new JCheckBox();
            
            String value = getParameterValue(param);
            if (value != null) {
                checkBox.setSelected(Boolean.parseBoolean(value));
            }
            
            return checkBox;
            
        } else if (param.isTextParameter()) {
            // 文本参数 - 使用文本区域
            JTextArea textArea = new JTextArea(3, 40);
            textArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            
            String value = getParameterValue(param);
            if (value != null) {
                textArea.setText(value);
            }
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(450, 70));
            return scrollPane;
            
        } else {
            // 字符串参数 - 使用文本框
            JTextField textField = new JTextField();
            textField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            String value = getParameterValue(param);
            if (value != null) {
                textField.setText(value);
            }
            
            textField.setPreferredSize(new Dimension(450, 28));
            return textField;
        }
    }

    /**
     * 获取参数值（预填充值或默认值）
     */
    private String getParameterValue(JenkinsBuildParameter param) {
        String value = null;
        
        // 优先使用预填充值
        if (prefilledParameters != null && prefilledParameters.containsKey(param.getName())) {
            value = prefilledParameters.get(param.getName());
            System.out.println("Using prefilled value for " + param.getName() + ": " + value);
            return value;
        }
        
        // 使用默认值
        if (param.getDefaultValue() != null) {
            value = param.getDefaultValue().toString();
            System.out.println("Using default value for " + param.getName() + ": " + value);
            return value;
        }
        
        System.out.println("No value found for " + param.getName());
        return null;
    }

    /**
     * 验证并触发构建
     */
    private void validateAndTriggerBuild() {
        Map<String, String> parameterValues = new HashMap<>();
        
        // 收集参数值
        for (JenkinsBuildParameter param : parameters) {
            JComponent component = parameterComponents.get(param.getName());
            String value = extractValue(component);
            
            // 验证必填参数
            if ((value == null || value.trim().isEmpty()) && param.getDefaultValue() == null) {
                JOptionPane.showMessageDialog(this,
                    "Please fill in all required parameters: " + param.getName(),
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (value != null && !value.trim().isEmpty()) {
                parameterValues.put(param.getName(), value);
            }
        }
        
        // 触发构建
        triggerBuild(parameterValues);
    }

    /**
     * 从组件中提取值
     */
    private String extractValue(JComponent component) {
        if (component instanceof JTextField) {
            return ((JTextField) component).getText();
        } else if (component instanceof JComboBox) {
            Object selected = ((JComboBox<?>) component).getSelectedItem();
            return selected != null ? selected.toString() : null;
        } else if (component instanceof JCheckBox) {
            return String.valueOf(((JCheckBox) component).isSelected());
        } else if (component instanceof JScrollPane) {
            JViewport viewport = ((JScrollPane) component).getViewport();
            Component view = viewport.getView();
            if (view instanceof JTextArea) {
                return ((JTextArea) view).getText();
            }
        }
        return null;
    }

    /**
     * 触发构建
     */
    private void triggerBuild(Map<String, String> parameterValues) {
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return apiClient.triggerBuild(jobPath, parameterValues);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    JOptionPane.showMessageDialog(JenkinsBuildParametersDialog.this,
                        result,
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(JenkinsBuildParametersDialog.this,
                        "Failed to trigger build: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        
        worker.execute();
    }
}
