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
    private List<Boolean> selectedRows;  // 复选框状态
    private String[] columnNames = {"Select", "App Name", "Image Name", "Build Status", 
                                     "Create Time", "Version", "Git Branch"};
    private int[] columnWidths = {60, 150, 400, 120, 180, 150, 100};  // 首选列宽
    
    /**
     * 构造函数
     */
    public BuildResultTableModel() {
        this.results = new ArrayList<>();
        this.selectedRows = new ArrayList<>();
    }
    
    /**
     * 设置结果数据
     * Set results data
     * 
     * @param results 构建结果列表
     */
    public void setResults(List<BuildResult> results) {
        this.results = results != null ? new ArrayList<>(results) : new ArrayList<>();
        
        // 初始化复选框状态（全部未选中）
        this.selectedRows = new ArrayList<>();
        for (int i = 0; i < this.results.size(); i++) {
            this.selectedRows.add(false);
        }
        
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
     * 获取选中的构建结果
     * Get selected build results
     * 
     * @return 选中的构建结果列表
     */
    public List<BuildResult> getSelectedResults() {
        List<BuildResult> selected = new ArrayList<>();
        for (int i = 0; i < results.size() && i < selectedRows.size(); i++) {
            if (selectedRows.get(i)) {
                selected.add(results.get(i));
            }
        }
        return selected;
    }
    
    /**
     * 获取选中行的镜像名称列表
     * Get image names of selected rows
     * 
     * @return 镜像名称列表
     */
    public List<String> getSelectedImageNames() {
        List<String> imageNames = new ArrayList<>();
        for (int i = 0; i < results.size() && i < selectedRows.size(); i++) {
            if (selectedRows.get(i)) {
                String imageName = results.get(i).getImageName();
                if (imageName != null && !imageName.trim().isEmpty()) {
                    imageNames.add(imageName);
                }
            }
        }
        return imageNames;
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
                // 复选框列
                return row < selectedRows.size() ? selectedRows.get(row) : false;
            case 1:
                return result.getAppName();
            case 2:
                return result.getImageName();
            case 3:
                return result.getBuildStatus();
            case 4:
                return result.getFormattedCreateTime();
            case 5:
                return result.getVersion();
            case 6:
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
        if (column == 0) {
            // 第一列是复选框
            return Boolean.class;
        }
        // 其他列都是字符串类型
        return String.class;
    }
    
    @Override
    public boolean isCellEditable(int row, int column) {
        // 只有复选框列可编辑
        return column == 0;
    }
    
    @Override
    public void setValueAt(Object value, int row, int column) {
        if (column == 0 && row >= 0 && row < selectedRows.size()) {
            selectedRows.set(row, (Boolean) value);
            fireTableCellUpdated(row, column);
        }
    }
}
