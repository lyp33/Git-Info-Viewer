package com.gitviewer;

import java.util.ArrayList;
import java.util.List;

/**
 * Jenkins 构建参数定义
 */
public class JenkinsBuildParameter {
    private String name;
    private String type;  // StringParameterDefinition, ChoiceParameterDefinition, BooleanParameterDefinition, TextParameterDefinition
    private String description;
    private Object defaultValue;
    private List<String> choices;  // 用于 ChoiceParameterDefinition

    public JenkinsBuildParameter() {
        this.choices = new ArrayList<>();
    }

    public JenkinsBuildParameter(String name, String type, String description, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.defaultValue = defaultValue;
        this.choices = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public List<String> getChoices() {
        return choices;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }

    /**
     * 判断是否为字符串参数
     */
    public boolean isStringParameter() {
        return type != null && type.contains("StringParameterDefinition");
    }

    /**
     * 判断是否为选择参数
     */
    public boolean isChoiceParameter() {
        return type != null && type.contains("ChoiceParameterDefinition");
    }

    /**
     * 判断是否为布尔参数
     */
    public boolean isBooleanParameter() {
        return type != null && type.contains("BooleanParameterDefinition");
    }

    /**
     * 判断是否为文本参数
     */
    public boolean isTextParameter() {
        return type != null && type.contains("TextParameterDefinition");
    }
}
