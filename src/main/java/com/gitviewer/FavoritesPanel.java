package com.gitviewer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Jenkins 收藏任务面板
 * 显示用户收藏的 Jenkins Job 列表，支持快速定位
 */
public class FavoritesPanel extends JPanel {
    private JList<FavoriteJob> favoritesList;
    private DefaultListModel<FavoriteJob> listModel;
    private JenkinsBrowserDialog parentDialog;
    private JPopupMenu popupMenu;
    
    public FavoritesPanel(JenkinsBrowserDialog parent) {
        this.parentDialog = parent;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(),
            "\u2B50 Favorite Jobs",
            TitledBorder.LEFT,
            TitledBorder.TOP
        ));
        
        // 创建列表模型和列表
        listModel = new DefaultListModel<>();
        favoritesList = new JList<>(listModel);
        favoritesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        favoritesList.setCellRenderer(new FavoriteJobRenderer());
        
        // 添加双击监听器 - 定位到任务
        favoritesList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.out.println("=== Mouse Clicked ===");
                System.out.println("Click count: " + e.getClickCount());
                System.out.println("Button: " + e.getButton());
                
                if (e.getClickCount() == 2) {
                    System.out.println("Double-click detected!");
                    int index = favoritesList.locationToIndex(e.getPoint());
                    System.out.println("Index: " + index);
                    
                    if (index >= 0) {
                        FavoriteJob job = listModel.getElementAt(index);
                        System.out.println("Job retrieved: " + (job != null ? job.getDisplayName() : "NULL"));
                        
                        if (job != null) {
                            navigateToJob(job);
                        } else {
                            System.out.println("ERROR: Job is null!");
                        }
                    } else {
                        System.out.println("ERROR: Invalid index!");
                    }
                } else {
                    System.out.println("Single click, ignoring");
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                System.out.println("Mouse pressed: button=" + e.getButton() + ", isPopupTrigger=" + e.isPopupTrigger());
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                System.out.println("Mouse released: button=" + e.getButton() + ", isPopupTrigger=" + e.isPopupTrigger());
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
        });
        
        // 创建右键菜单
        createPopupMenu();
        
        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(favoritesList);
        scrollPane.setPreferredSize(new Dimension(0, 150));
        add(scrollPane, BorderLayout.CENTER);
        
        // If list is empty, show hint
        if (listModel.isEmpty()) {
            JLabel emptyLabel = new JLabel("No favorite jobs", SwingConstants.CENTER);
            emptyLabel.setForeground(Color.GRAY);
            add(emptyLabel, BorderLayout.NORTH);
        }
    }
    
    /**
     * Create context menu
     */
    private void createPopupMenu() {
        popupMenu = new JPopupMenu();
        
        JMenuItem removeItem = new JMenuItem("Remove from Favorites");
        removeItem.addActionListener(e -> {
            int index = favoritesList.getSelectedIndex();
            if (index >= 0) {
                FavoriteJob job = listModel.getElementAt(index);
                removeFavorite(job);
            }
        });
        
        JMenuItem moveUpItem = new JMenuItem("Move Up");
        moveUpItem.addActionListener(e -> {
            int index = favoritesList.getSelectedIndex();
            if (index > 0) {
                moveFavoriteUp(index);
            }
        });
        
        JMenuItem moveDownItem = new JMenuItem("Move Down");
        moveDownItem.addActionListener(e -> {
            int index = favoritesList.getSelectedIndex();
            if (index >= 0 && index < listModel.getSize() - 1) {
                moveFavoriteDown(index);
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
        int index = favoritesList.locationToIndex(e.getPoint());
        if (index >= 0) {
            favoritesList.setSelectedIndex(index);
            popupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }
    
    /**
     * 添加收藏任务
     */
    public void addFavorite(FavoriteJob job) {
        if (job == null) return;
        
        // 检查是否已存在
        for (int i = 0; i < listModel.getSize(); i++) {
            if (listModel.getElementAt(i).getJobPath().equals(job.getJobPath())) {
                return; // 已存在
            }
        }
        
        listModel.addElement(job);
        AppSettings.getInstance().addJenkinsFavorite(job);
        
        // 移除空提示
        removeEmptyLabel();
    }
    
    /**
     * 移除收藏任务
     */
    public void removeFavorite(FavoriteJob job) {
        if (job == null) return;
        
        listModel.removeElement(job);
        AppSettings.getInstance().removeJenkinsFavorite(job.getJobPath());
        
        // 如果列表为空，显示提示
        if (listModel.isEmpty()) {
            showEmptyLabel();
        }
        
        // 通知父对话框更新树节点显示
        if (parentDialog != null) {
            parentDialog.refreshTreeFavoriteMarks();
        }
    }
    
    /**
     * 上移收藏任务
     */
    public void moveFavoriteUp(int index) {
        if (index > 0 && index < listModel.getSize()) {
            FavoriteJob job = listModel.remove(index);
            listModel.add(index - 1, job);
            favoritesList.setSelectedIndex(index - 1);
            AppSettings.getInstance().moveFavoriteUp(index);
        }
    }
    
    /**
     * 下移收藏任务
     */
    public void moveFavoriteDown(int index) {
        if (index >= 0 && index < listModel.getSize() - 1) {
            FavoriteJob job = listModel.remove(index);
            listModel.add(index + 1, job);
            favoritesList.setSelectedIndex(index + 1);
            AppSettings.getInstance().moveFavoriteDown(index);
        }
    }
    
    /**
     * 加载收藏列表
     */
    public void loadFavorites(List<FavoriteJob> favorites) {
        listModel.clear();
        if (favorites != null) {
            for (FavoriteJob job : favorites) {
                listModel.addElement(job);
            }
        }
        
        if (listModel.isEmpty()) {
            showEmptyLabel();
        } else {
            removeEmptyLabel();
        }
    }
    
    /**
     * Navigate to favorited job
     */
    private void navigateToJob(FavoriteJob job) {
        System.out.println("=== navigateToJob called ===");
        System.out.println("VERSION CHECK: FavoritesPanel compiled at: " + java.time.LocalDateTime.now());
        System.out.println("Job: " + job.getDisplayName());
        System.out.println("Job Path: " + job.getJobPath());
        System.out.println("Parent Dialog: " + (parentDialog != null ? "SET" : "NULL"));
        
        if (parentDialog != null) {
            // 检查 Jenkins 是否正在加载
            if (parentDialog.isLoading()) {
                System.out.println("Jenkins is still loading, showing message...");
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
            
            System.out.println("Loading dialog created");
            
            // 在后台线程中执行导航
            SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    System.out.println("SwingWorker: doInBackground started");
                    boolean result = parentDialog.navigateToJobPath(job.getJobPath());
                    System.out.println("SwingWorker: navigateToJobPath returned: " + result);
                    return result;
                }
                
                @Override
                protected void done() {
                    System.out.println("SwingWorker: done() called");
                    loadingDialog.dispose();
                    try {
                        boolean success = get();
                        System.out.println("SwingWorker: success = " + success);
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
                        System.out.println("SwingWorker: Exception in done(): " + e.getMessage());
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(
                            FavoritesPanel.this,
                            "Error navigating to job: " + e.getMessage(),
                            "Navigation Error",
                            JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            };
            
            System.out.println("Starting SwingWorker...");
            worker.execute();
            System.out.println("Showing loading dialog...");
            loadingDialog.setVisible(true);
            System.out.println("Loading dialog closed");
        } else {
            System.out.println("ERROR: parentDialog is null!");
        }
    }
    
    /**
     * Show empty list hint
     */
    private void showEmptyLabel() {
        // Remove existing empty hint
        removeEmptyLabel();
        
        JLabel emptyLabel = new JLabel("No favorite jobs", SwingConstants.CENTER);
        emptyLabel.setForeground(Color.GRAY);
        emptyLabel.setName("emptyLabel");
        add(emptyLabel, BorderLayout.NORTH);
        revalidate();
        repaint();
    }
    
    /**
     * 移除空列表提示
     */
    private void removeEmptyLabel() {
        Component[] components = getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel && "emptyLabel".equals(comp.getName())) {
                remove(comp);
                revalidate();
                repaint();
                break;
            }
        }
    }
    
    /**
     * Custom list renderer
     * Display job name and path hint, handle long text truncation
     */
    private static class FavoriteJobRenderer extends DefaultListCellRenderer {
        
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof FavoriteJob) {
                FavoriteJob job = (FavoriteJob) value;
                
                // Display full job path instead of just name
                String jobPath = job.getJobPath();
                
                // Use Unicode star character to avoid encoding issues
                String displayText = "\u2B50 " + jobPath;
                
                setText(displayText);
                
                // Set tooltip to show full path
                String tooltip = "<html>" +
                        "<b>Job Name:</b> " + job.getDisplayName() + "<br>" +
                        "<b>Full Path:</b> " + jobPath +
                        "</html>";
                setToolTipText(tooltip);
                
                // Set icon and style
                setIcon(null); // Use star in text instead of icon
                
                // Set font
                Font font = getFont();
                if (font != null) {
                    setFont(font.deriveFont(Font.PLAIN));
                }
                
                // Set margins
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
            }
            
            return this;
        }
    }
}
