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
            
            // 调试日志：打印收藏任务的详细信息
            System.out.println("[FavoritesPanel] ========== Navigation Debug ==========");
            System.out.println("[FavoritesPanel] Job Display Name: " + job.getDisplayName());
            System.out.println("[FavoritesPanel] Job Path: " + job.getJobPath());
            System.out.println("[FavoritesPanel] Job URL: " + job.getJobUrl());
            System.out.println("[FavoritesPanel] ==========================================");
            
            // 显示加载提示对话框
            Window owner = SwingUtilities.getWindowAncestor(this);
            JDialog loadingDialog = new JDialog(owner, "Loading", Dialog.ModalityType.MODELESS);  // 改为非模态
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            panel.add(new JLabel("Loading... please wait"), BorderLayout.CENTER);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            panel.add(progressBar, BorderLayout.SOUTH);
            
            // 添加取消按钮
            JButton cancelButton = new JButton("Cancel");
            panel.add(cancelButton, BorderLayout.EAST);
            
            loadingDialog.add(panel);
            loadingDialog.setSize(350, 120);
            loadingDialog.setLocationRelativeTo(this);
            loadingDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);  // 允许关闭
            
            // 在后台线程中执行导航
            class CancellableWorker extends SwingWorker<Boolean, Void> {
                private volatile boolean cancelled = false;
                
                @Override
                protected Boolean doInBackground() throws Exception {
                    try {
                        // 设置超时：最多等待30秒
                        long startTime = System.currentTimeMillis();
                        long timeout = 30000; // 30秒
                        
                        // 在新线程中执行导航
                        java.util.concurrent.FutureTask<Boolean> task = 
                            new java.util.concurrent.FutureTask<>(() -> {
                                return parentDialog.navigateToJobPath(job.getJobPath());
                            });
                        
                        Thread thread = new Thread(task);
                        thread.setDaemon(true);
                        thread.start();
                        
                        // 等待结果或超时
                        while (!task.isDone() && !cancelled) {
                            if (System.currentTimeMillis() - startTime > timeout) {
                                System.err.println("[FavoritesPanel] Navigation timeout after 30 seconds");
                                thread.interrupt();
                                return false;
                            }
                            Thread.sleep(100);
                        }
                        
                        if (cancelled) {
                            thread.interrupt();
                            return false;
                        }
                        
                        return task.get();
                    } catch (Exception e) {
                        System.err.println("[FavoritesPanel] Navigation error: " + e.getMessage());
                        e.printStackTrace();
                        return false;
                    }
                }
                
                @Override
                protected void done() {
                    loadingDialog.dispose();
                    try {
                        boolean success = get();
                        if (success) {
                            // 导航成功，延迟一下再打开详情对话框
                            // 使用invokeLater确保在EDT线程中执行，并且在Loading对话框关闭后
                            SwingUtilities.invokeLater(() -> {
                                System.out.println("[FavoritesPanel] Navigation successful, opening job details...");
                                parentDialog.openSelectedJobDetails();
                            });
                        } else if (!cancelled) {
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
                        System.err.println("[FavoritesPanel] Error in done(): " + e.getMessage());
                    }
                }
                
                public void cancelTask() {
                    cancelled = true;
                    cancel(true);
                }
            }
            
            CancellableWorker worker = new CancellableWorker();
            
            // 取消按钮事件
            cancelButton.addActionListener(e -> {
                System.out.println("[FavoritesPanel] User cancelled navigation");
                worker.cancelTask();
                loadingDialog.dispose();
            });
            
            // 对话框关闭时取消任务
            loadingDialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    System.out.println("[FavoritesPanel] Loading dialog closed, cancelling task");
                    worker.cancelTask();
                }
            });
            
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
