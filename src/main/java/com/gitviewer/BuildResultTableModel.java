package com.gitviewer;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * 构建结果表格模型
 * Custom table model for displaying build results
 */
public class BuildResultTableModel extends AbstractTableModel {
    private List<BuildResult> results;
    private String[] columnNames = {"App Name", "Image Name", "Build Status", 
                                     "Create Time", "Version", "Git Branch"};
    private int[] columnWidths = {150, 400, 120, 180, 150, 100};  // 首选列宽
    
    /**
     * 构造函数
     */
    public BuildResultTableModel() {
        this.results = new ArrayList<>();
    }
    
    /**
     * 设置结果数据
     * Set results data
     * 
     * @param results 构建结果列表
     */
    public void setResults(List<BuildResult> results) {
        this.results = results != null ? new ArrayList<>(results) : new ArrayList<>();
        fireTableDataChanged();
    }
    
    /**
     * 获取结果数据
     * Get results data
     * 
     * @return 构建结果列表
     */
    public List<BuildResult> getResults() {
        return new ArrayList<>(results);
    }
    
    /**
     * 获取列宽数组
     * Get column widths
     * 
     * @return 列宽数组
     */
    public int[] getColumnWidths() {
        return columnWidths;
    }
    
    @Override
    public int getRowCount() {
        return results.size();
    }
    
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }
    
    @Override
    public Object getValueAt(int row, int column) {
        if (row < 0 || row >= results.size()) {
            return "";
        }
        
        BuildResult result = results.get(row);
        
        switch (column) {
            case 0:
                return result.getAppName();
            case 1:
                return result.getImageName();
            case 2:
                return result.getBuildStatus();
            case 3:
                return result.getFormattedCreateTime();
            case 4:
                return result.getVersion();
            case 5:
                return result.getGitBranch();
            default:
                return "";
        }
    }
    
    @Override
    public String getColumnName(int column) {
        if (column >= 0 && column < columnNames.length) {
            return columnNames[column];
        }
        return "";
    }
    
    @Override
    public Class<?> getColumnClass(int column) {
        // 所有列都是字符串类型，用于正确的排序
        return String.class;
    }
    
    @Override
    public boolean isCellEditable(int row, int column) {
        // 所有单元格都不可编辑
        return false;
    }
}
