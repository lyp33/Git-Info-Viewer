package com.gitviewer;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 * 构建状态单元格渲染器
 * Custom cell renderer for build status with color coding
 */
public class BuildStatusCellRenderer extends DefaultTableCellRenderer {
    
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        // 只在未选中时应用颜色编码
        if (!isSelected && value != null) {
            String status = value.toString();
            
            // 根据状态设置颜色
            if (status.contains("Success")) {
                c.setForeground(new Color(0, 128, 0));  // 绿色 - 成功
            } else if (status.contains("Failed") || status.contains("Fail")) {
                c.setForeground(Color.RED);  // 红色 - 失败
            } else if (status.contains("Start") || status.contains("Running")) {
                c.setForeground(new Color(255, 140, 0));  // 橙色 - 运行中
            } else {
                c.setForeground(Color.BLACK);  // 黑色 - 其他状态
            }
        }
        
        return c;
    }
}
