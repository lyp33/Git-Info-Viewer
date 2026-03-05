package com.gitviewer;

/**
 * Git Tool 参数定义
 */
public class GitToolParameter {
    private String type;        // 参数类型：string, number, boolean
    private String description; // 参数描述
    private boolean required;   // 是否必需
    private String defaultValue; // 默认值（可选）

    public GitToolParameter(String type, String description, boolean required) {
        this.type = type;
        this.description = description;
        this.required = required;
        this.defaultValue = null;
    }

    public GitToolParameter(String type, String description, boolean required, String defaultValue) {
        this.type = type;
        this.description = description;
        this.required = required;
        this.defaultValue = defaultValue;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(type);
        if (required) {
            sb.append(" (必需)");
        } else {
            sb.append(" (可选)");
        }
        if (defaultValue != null) {
            sb.append(" [默认: ").append(defaultValue).append("]");
        }
        sb.append(" - ").append(description);
        return sb.toString();
    }
}
