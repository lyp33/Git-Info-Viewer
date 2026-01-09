package com.gitviewer;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件搜索对话框
 */
public class FileSearchDialog extends JDialog {

    private JTextField searchField;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private File rootDirectory;
    private FileSelectionListener selectionListener;
    private List<File> allFiles;
    private volatile boolean indexing = false;
    private final Object filesLock = new Object();
    
    // 静态缓存，用于存储已索引的文件
    private static final java.util.Map<String, List<File>> fileCache = new java.util.HashMap<>();

    public FileSearchDialog(Frame parent, File rootDirectory) {
        super(parent, "Search Files", false); // 非模态对话框
        this.rootDirectory = rootDirectory;
        this.allFiles = new ArrayList<>();
        
        initializeUI();
        setLocationRelativeTo(parent);
        
        // 检查缓存，如果有则使用缓存，否则开始索引
        loadOrIndexFiles();
    }

    private void initializeUI() {
        setSize(700, 500);
        setLayout(new BorderLayout(10, 10));
        
        // 顶部搜索面板
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));
        
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchPanel.add(searchLabel, BorderLayout.WEST);
        
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        searchField.addActionListener(e -> performSearch());
        
        searchPanel.add(searchField, BorderLayout.CENTER);
        
        // 添加搜索按钮
        JButton searchButton = new JButton("Search");
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchButton.setBackground(new Color(66, 133, 244));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setBorderPainted(false);
        searchButton.setOpaque(true);
        searchButton.setPreferredSize(new Dimension(80, 32));
        searchButton.addActionListener(e -> performSearch());
        
        searchPanel.add(searchButton, BorderLayout.EAST);
        
        add(searchPanel, BorderLayout.NORTH);
        
        // 中间结果表格
        String[] columnNames = {"File Name", "Absolute Path"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resultTable.setRowHeight(28);
        resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.setShowGrid(false);
        resultTable.setIntercellSpacing(new Dimension(0, 0));
        
        // 设置列宽
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(450);
        
        // 双击打开文件
        resultTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = resultTable.getSelectedRow();
                    if (row >= 0) {
                        openSelectedFile(row);
                    }
                }
            }
        });
        
        // 添加排序器
        sorter = new TableRowSorter<>(tableModel);
        resultTable.setRowSorter(sorter);
        
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // 底部状态栏
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(new Color(248, 249, 250));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        
        JLabel statusLabel = new JLabel("Indexing files...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(95, 99, 104));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        
        add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * 加载或索引文件
     */
    private void loadOrIndexFiles() {
        if (rootDirectory == null || !rootDirectory.exists()) {
            return;
        }
        
        String cacheKey = rootDirectory.getAbsolutePath();
        
        // 检查缓存
        synchronized (fileCache) {
            if (fileCache.containsKey(cacheKey)) {
                // 使用缓存
                synchronized (filesLock) {
                    allFiles = new ArrayList<>(fileCache.get(cacheKey));
                }
                JLabel statusLabel = (JLabel) ((JPanel) getContentPane().getComponent(2)).getComponent(0);
                statusLabel.setText(allFiles.size() + " files indexed (from cache). Type to search...");
                searchField.setEnabled(true);
                searchField.requestFocus();
                return;
            }
        }
        
        // 缓存中没有，开始索引
        indexFiles();
    }

    /**
     * 索引所有文件
     */
    private void indexFiles() {
        if (rootDirectory == null || !rootDirectory.exists()) {
            return;
        }
        
        indexing = true;
        searchField.setEnabled(false);
        
        String cacheKey = rootDirectory.getAbsolutePath();
        
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                synchronized (filesLock) {
                    allFiles.clear();
                    scanDirectory(rootDirectory);
                    
                    // 保存到缓存
                    synchronized (fileCache) {
                        fileCache.put(cacheKey, new ArrayList<>(allFiles));
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                indexing = false;
                JLabel statusLabel = (JLabel) ((JPanel) getContentPane().getComponent(2)).getComponent(0);
                statusLabel.setText(allFiles.size() + " files indexed. Type to search...");
                searchField.setEnabled(true);
                searchField.requestFocus();
            }
        };
        
        worker.execute();
    }

    /**
     * 递归扫描目录
     */
    private void scanDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }
        
        for (File file : files) {
            // 跳过隐藏文件和 .git 目录
            if (file.getName().startsWith(".")) {
                continue;
            }
            
            if (file.isFile()) {
                allFiles.add(file);
            } else if (file.isDirectory()) {
                scanDirectory(file);
            }
        }
    }

    /**
     * 执行搜索
     */
    private void performSearch() {
        if (indexing) {
            JOptionPane.showMessageDialog(
                this,
                "Please wait, files are still being indexed...",
                "Indexing in Progress",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        
        String searchText = searchField.getText().trim().toLowerCase();
        
        // 清空表格
        tableModel.setRowCount(0);
        
        if (searchText.isEmpty()) {
            updateStatusLabel(0);
            return;
        }
        
        // 搜索匹配的文件
        int count = 0;
        synchronized (filesLock) {
            for (File file : allFiles) {
                String fileName = file.getName().toLowerCase();
                if (fileName.contains(searchText)) {
                    String absolutePath = file.getAbsolutePath();
                    tableModel.addRow(new Object[]{file.getName(), absolutePath});
                    count++;
                    
                    // 限制结果数量，避免性能问题
                    if (count >= 1000) {
                        break;
                    }
                }
            }
        }
        
        updateStatusLabel(count);
    }

    /**
     * 更新状态标签
     */
    private void updateStatusLabel(int count) {
        JLabel statusLabel = (JLabel) ((JPanel) getContentPane().getComponent(2)).getComponent(0);
        if (count == 0) {
            statusLabel.setText("No results found.");
        } else if (count >= 1000) {
            statusLabel.setText("Showing first 1000 results. Refine your search for better results.");
        } else {
            statusLabel.setText(count + " file(s) found.");
        }
    }

    /**
     * 打开选中的文件
     */
    private void openSelectedFile(int row) {
        String absolutePath = (String) tableModel.getValueAt(row, 1);
        File selectedFile = new File(absolutePath);
        
        if (selectedFile.exists() && selectionListener != null) {
            selectionListener.onFileSelected(selectedFile);
            // 注意：关闭对话框的操作由监听器处理（在 GitViewerApp 中）
        }
    }

    /**
     * 设置文件选择监听器
     */
    public void setFileSelectionListener(FileSelectionListener listener) {
        this.selectionListener = listener;
    }

    /**
     * 文件选择监听器接口
     */
    @FunctionalInterface
    public interface FileSelectionListener {
        void onFileSelected(File file);
    }
}
