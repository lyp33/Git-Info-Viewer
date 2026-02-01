package com.gitviewer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Jenkins 收藏任务面板
 * 显示用户收藏的 Jenkins Job 列表，支持快速定位和别名编辑
 */
public class FavoritesPanel extends JPanel {
    private JTable favoritesTable;
    private FavoritesTableModel tableModel;
    private JenkinsBrowserDialog parentDialog;
    private JPopupMenu popupMenu;
    
    public FavoritesPanel(JenkinsBrowserDialog parent) {
        this.parentDialog = parent;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        javax.swing.border.TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "Favorite Jobs",
            TitledBorder.LEFT,
            TitledBorder.TOP
        );
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        setBorder(border);
        
        // 创建表格模型和表格
        tableModel = new FavoritesTableModel();
        favoritesTable = new JTable(tableModel);
        favoritesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        favoritesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        favoritesTable.setRowHeight(32);
        favoritesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        favoritesTable.getTableHeader().setBackground(new Color(248, 249, 250));
        favoritesTable.getTableHeader().setForeground(new Color(60, 64, 67));
        favoritesTable.setShowGrid(true);
        favoritesTable.setGridColor(new Color(240, 240, 240));
        
        // 设置列宽
        favoritesTable.getColumnModel().getColumn(0).setPreferredWidth(400);  // Job Path
        favoritesTable.getColumnModel().getColumn(1).setPreferredWidth(150);  // Alias
        
        // 添加双击监听器 - 定位到任务
        favoritesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = favoritesTable.rowAtPoint(e.getPoint());
                    int col = favoritesTable.columnAtPoint(e.getPoint());
                    
                    // 如果双击的不是别名列，则导航到任务
                    if (row >= 0 && col != 1) {
                        FavoriteJob job = tableModel.getJobAt(row);
                        if (job != null) {
                            navigateToJob(job);
                        }
                    }
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
        });
        
        // 创建右键菜单
        createPopupMenu();
        
        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(favoritesTable);
        scrollPane.setPreferredSize(new Dimension(0, 150));
        add(scrollPane, BorderLayout.CENTER);
    }
    
    /**
     * Create context menu
     */
    private void createPopupMenu() {
        popupMenu = new JPopupMenu();
        
        JMenuItem removeItem = new JMenuItem("Remove from Favorites");
        removeItem.addActionListener(e -> {
            int row = favoritesTable.getSelectedRow();
            if (row >= 0) {
                FavoriteJob job = tableModel.getJobAt(row);
                removeFavorite(job);
            }
        });
        
        JMenuItem moveUpItem = new JMenuItem("Move Up");
        moveUpItem.addActionListener(e -> {
            int row = favoritesTable.getSelectedRow();
            if (row > 0) {
                moveFavoriteUp(row);
            }
        });
        
        JMenuItem moveDownItem = new JMenuItem("Move Down");
        moveDownItem.addActionListener(e -> {
            int row = favoritesTable.getSelectedRow();
            if (row >= 0 && row < tableModel.getRowCount() - 1) {
                moveFavoriteDown(row);
            }
        });
        
        popupMenu.add(removeItem);
        popupMenu.addSeparator();
        popupMenu.add(moveUpItem);
        popupMenu.add(moveDownItem);
    }
    
    /**
     * 显示右键菜单
     */
    private void showPopupMenu(MouseEvent e) {
        int row = favoritesTable.rowAtPoint(e.getPoint());
        if (row >= 0) {
            favoritesTable.setRowSelectionInterval(row, row);
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    /**
     * 添加收藏任务
     */
    public void addFavorite(FavoriteJob job) {
        if (job == null) return;
        
        // 检查是否已存在
        if (tableModel.containsJob(job.getJobPath())) {
            return; // 已存在
        }
        
        tableModel.addJob(job);
        AppSettings.getInstance().addJenkinsFavorite(job);
    }
    
    /**
     * 移除收藏任务
     */
    public void removeFavorite(FavoriteJob job) {
        if (job == null) return;
        
        tableModel.removeJob(job);
        AppSettings.getInstance().removeJenkinsFavorite(job.getJobPath());
        
        // 通知父对话框更新树节点显示
        if (parentDialog != null) {
            parentDialog.refreshTreeFavoriteMarks();
        }
    }
    
    /**
     * 上移收藏任务
     */
    public void moveFavoriteUp(int row) {
        if (row > 0 && row < tableModel.getRowCount()) {
            tableModel.moveJobUp(row);
            favoritesTable.setRowSelectionInterval(row - 1, row - 1);
            AppSettings.getInstance().moveFavoriteUp(row);
        }
    }
    
    /**
     * 下移收藏任务
     */
    public void moveFavoriteDown(int row) {
        if (row >= 0 && row < tableModel.getRowCount() - 1) {
            tableModel.moveJobDown(row);
            favoritesTable.setRowSelectionInterval(row + 1, row + 1);
            AppSettings.getInstance().moveFavoriteDown(row);
        }
    }
    
    /**
     * 加载收藏列表
     */
    public void loadFavorites(List<FavoriteJob> favorites) {
        tableModel.setJobs(favorites);
    }
    
    /**
     * Navigate to favorited job
     */
    private void navigateToJob(FavoriteJob job) {
        if (parentDialog != null) {
            // 检查 Jenkins 是否正在加载
            if (parentDialog.isLoading()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Jenkins is loading now, please wait...",
                    "Loading",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }
            
            // 显示加载提示对话框
            Window owner = SwingUtilities.getWindowAncestor(this);
            JDialog loadingDialog = new JDialog(owner, "Loading", Dialog.ModalityType.APPLICATION_MODAL);
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            panel.add(new JLabel("Loading... please wait"), BorderLayout.CENTER);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            panel.add(progressBar, BorderLayout.SOUTH);
            loadingDialog.add(panel);
            loadingDialog.setSize(300, 120);
            loadingDialog.setLocationRelativeTo(this);
            loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            
            // 在后台线程中执行导航
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return parentDialog.navigateToJobPath(job.getJobPath());
                }
                
                @Override
                protected void done() {
                    loadingDialog.dispose();
                    try {
                        boolean success = get();
                        if (!success) {
                            // Navigation failed, ask if should remove
                            int result = JOptionPane.showConfirmDialog(
                                FavoritesPanel.this,
                                "Cannot find job: " + job.getDisplayName() + "\nRemove from favorites?",
                                "Job Not Found",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE
                            );
                            
                            if (result == JOptionPane.YES_OPTION) {
                                removeFavorite(job);
                            }
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                            FavoritesPanel.this,
                            "Error navigating to job: " + e.getMessage(),
                            "Navigation Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            };
            
            worker.execute();
            loadingDialog.setVisible(true);
        }
    }
    
    /**
     * 收藏任务表格模型
     */
    private class FavoritesTableModel extends AbstractTableModel {
        private List<FavoriteJob> jobs;
        private String[] columnNames = {"Job Path", "Alias"};
        
        public FavoritesTableModel() {
            this.jobs = new ArrayList<>();
        }
        
        public void setJobs(List<FavoriteJob> jobs) {
            this.jobs = jobs != null ? new ArrayList<>(jobs) : new ArrayList<>();
            fireTableDataChanged();
        }
        
        public void addJob(FavoriteJob job) {
            jobs.add(job);
            fireTableRowsInserted(jobs.size() - 1, jobs.size() - 1);
        }
        
        public void removeJob(FavoriteJob job) {
            int index = jobs.indexOf(job);
            if (index >= 0) {
                jobs.remove(index);
                fireTableRowsDeleted(index, index);
            }
        }
        
        public void moveJobUp(int row) {
            if (row > 0 && row < jobs.size()) {
                FavoriteJob job = jobs.remove(row);
                jobs.add(row - 1, job);
                fireTableRowsUpdated(row - 1, row);
            }
        }
        
        public void moveJobDown(int row) {
            if (row >= 0 && row < jobs.size() - 1) {
                FavoriteJob job = jobs.remove(row);
                jobs.add(row + 1, job);
                fireTableRowsUpdated(row, row + 1);
            }
        }
        
        public FavoriteJob getJobAt(int row) {
            if (row >= 0 && row < jobs.size()) {
                return jobs.get(row);
            }
            return null;
        }
        
        public boolean containsJob(String jobPath) {
            for (FavoriteJob job : jobs) {
                if (job.getJobPath().equals(jobPath)) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public int getRowCount() {
            return jobs.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            if (row < 0 || row >= jobs.size()) {
                return "";
            }
            
            FavoriteJob job = jobs.get(row);
            
            switch (column) {
                case 0:
                    return "[*] " + job.getJobPath();
                case 1:
                    return job.getAlias() != null ? job.getAlias() : "";
                default:
                    return "";
            }
        }
        
        @Override
        public boolean isCellEditable(int row, int column) {
            // 只有别名列可编辑
            return column == 1;
        }
        
        @Override
        public void setValueAt(Object value, int row, int column) {
            if (row < 0 || row >= jobs.size()) {
                return;
            }
            
            if (column == 1) {
                FavoriteJob job = jobs.get(row);
                String newAlias = value != null ? value.toString().trim() : "";
                job.setAlias(newAlias.isEmpty() ? null : newAlias);
                
                // 保存到设置
                AppSettings.getInstance().updateJenkinsFavoriteAlias(job.getJobPath(), job.getAlias());
                
                fireTableCellUpdated(row, column);
            }
        }
    }
}
