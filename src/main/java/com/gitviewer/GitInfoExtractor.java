package com.gitviewer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.ByteArrayOutputStream;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Git信息提取工具类
 * 用于从Git仓库中提取remote、branch、最后修改时间和修改人等信息
 */
public class GitInfoExtractor {

    /**
     * 检查指定目录是否是Git仓库
     */
    public static boolean isGitRepository(File directory) {
        if (directory == null || !directory.isDirectory()) {
            return false;
        }
        File gitDir = new File(directory, ".git");
        return gitDir.exists();
    }

    /**
     * 获取Git仓库信息
     */
    public static GitRepositoryInfo getRepositoryInfo(File directory) {
        if (!isGitRepository(directory)) {
            return null;
        }

        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder
                    .setGitDir(new File(directory, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            GitRepositoryInfo info = new GitRepositoryInfo();
            info.setPath(directory.getAbsolutePath());

            // 获取remote信息
            List<String> remotes = getRemoteUrls(repository);
            info.setRemoteUrls(remotes);

            // 获取当前分支
            String currentBranch = getCurrentBranch(repository);
            info.setCurrentBranch(currentBranch);

            // 获取所有分支
            List<String> branches = getAllBranches(repository);
            info.setBranches(branches);

            // 获取最后提交信息
            GitCommitInfo lastCommit = getLastCommitInfo(repository);
            info.setLastCommit(lastCommit);

            repository.close();
            return info;

        } catch (IOException e) {
            System.err.println("Error reading git repository: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取所有remote URL
     */
    private static List<String> getRemoteUrls(Repository repository) throws IOException {
        List<String> urls = new ArrayList<>();

        // 从config读取所有remote配置
        org.eclipse.jgit.lib.Config config = repository.getConfig();
        java.util.Set<String> remoteNames = config.getSubsections("remote");

        for (String remoteName : remoteNames) {
            String remoteUrl = config.getString("remote", remoteName, "url");
            if (remoteUrl != null && !remoteUrl.isEmpty()) {
                urls.add(remoteName + " : " + remoteUrl);
            }
        }

        return urls;
    }

    /**
     * 获取当前分支
     */
    private static String getCurrentBranch(Repository repository) throws IOException {
        String head = repository.getBranch();
        return head != null ? head : "Unknown";
    }

    /**
     * 获取所有分支
     */
    private static List<String> getAllBranches(Repository repository) throws IOException {
        List<String> branches = new ArrayList<>();
        Map<String, Ref> refs = repository.getRefDatabase().getRefs(Constants.R_HEADS);

        for (Map.Entry<String, Ref> entry : refs.entrySet()) {
            String branchName = entry.getKey();
            if (!branchName.endsWith("HEAD")) {
                branches.add(branchName);
            }
        }

        return branches;
    }

    /**
     * 获取最后一次提交信息
     */
    private static GitCommitInfo getLastCommitInfo(Repository repository) throws IOException {
        RevWalk revWalk = new RevWalk(repository);
        try {
            ObjectId head = repository.resolve(Constants.HEAD);
            if (head == null) {
                return null;
            }

            RevCommit commit = revWalk.parseCommit(head);

            GitCommitInfo info = new GitCommitInfo();
            info.setMessage(commit.getFullMessage());
            info.setAuthor(commit.getAuthorIdent().getName());
            info.setEmail(commit.getAuthorIdent().getEmailAddress());
            info.setCommitTime(commit.getCommitTime() * 1000L); // 转换为毫秒
            info.setCommitId(commit.getName());

            return info;
        } finally {
            revWalk.close();
        }
    }

    /**
     * 获取指定提交修改的文件列表
     */
    public static List<String> getCommitFiles(File directory, String commitId) {
        if (!isGitRepository(directory)) {
            return new ArrayList<>();
        }

        List<String> files = new ArrayList<>();

        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder
                    .setGitDir(new File(directory, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            RevWalk revWalk = new RevWalk(repository);
            try {
                ObjectId commitIdObj = ObjectId.fromString(commitId);
                RevCommit commit = revWalk.parseCommit(commitIdObj);

                // 获取该提交的父提交
                if (commit.getParentCount() > 0) {
                    RevCommit parent = revWalk.parseCommit(commit.getParent(0));

                    // 使用DiffFormatter获取文件变更
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    DiffFormatter formatter = new DiffFormatter(out);
                    formatter.setRepository(repository);
                    formatter.setDiffComparator(RawTextComparator.DEFAULT);
                    formatter.setDetectRenames(true);

                    List<DiffEntry> diffs = formatter.scan(parent.getTree(), commit.getTree());

                    for (DiffEntry diff : diffs) {
                        String path;
                        if (diff.getChangeType() == DiffEntry.ChangeType.ADD) {
                            path = "[ADD] " + diff.getNewPath();
                        } else if (diff.getChangeType() == DiffEntry.ChangeType.DELETE) {
                            path = "[DELETE] " + diff.getOldPath();
                        } else if (diff.getChangeType() == DiffEntry.ChangeType.RENAME) {
                            path = "[RENAME] " + diff.getOldPath() + " -> " + diff.getNewPath();
                        } else if (diff.getChangeType() == DiffEntry.ChangeType.MODIFY) {
                            path = "[MODIFY] " + diff.getNewPath();
                        } else if (diff.getChangeType() == DiffEntry.ChangeType.COPY) {
                            path = "[COPY] " + diff.getOldPath() + " -> " + diff.getNewPath();
                        } else {
                            path = "[CHANGE] " + diff.getNewPath();
                        }
                        files.add(path);
                    }

                    formatter.close();
                } else {
                    // 初始提交，列出所有文件
                    TreeWalk treeWalk = new TreeWalk(repository);
                    treeWalk.addTree(commit.getTree());
                    treeWalk.setRecursive(true);
                    while (treeWalk.next()) {
                        files.add("[ADD] " + treeWalk.getPathString());
                    }
                    treeWalk.close();
                }
            } finally {
                revWalk.close();
            }

            repository.close();
            return files;

        } catch (IOException e) {
            System.err.println("Error reading commit files: " + e.getMessage());
            e.printStackTrace();
            return files;
        }
    }

    /**
     * 获取最近N次提交记录
     */
    public static List<GitCommitInfo> getRecentCommits(File directory, int count) {
        if (!isGitRepository(directory)) {
            return new ArrayList<>();
        }

        List<GitCommitInfo> commits = new ArrayList<>();

        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder
                    .setGitDir(new File(directory, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            RevWalk revWalk = new RevWalk(repository);
            try {
                ObjectId head = repository.resolve(Constants.HEAD);
                if (head == null) {
                    return commits;
                }

                revWalk.markStart(revWalk.parseCommit(head));

                int fetched = 0;
                for (RevCommit commit : revWalk) {
                    if (fetched >= count) {
                        break;
                    }

                    GitCommitInfo info = new GitCommitInfo();
                    info.setMessage(commit.getFullMessage());
                    info.setAuthor(commit.getAuthorIdent().getName());
                    info.setEmail(commit.getAuthorIdent().getEmailAddress());
                    info.setCommitTime(commit.getCommitTime() * 1000L);
                    info.setCommitId(commit.getName());

                    commits.add(info);
                    fetched++;
                }
            } finally {
                revWalk.close();
            }

            repository.close();
            return commits;

        } catch (IOException e) {
            System.err.println("Error reading git commits: " + e.getMessage());
            return commits;
        }
    }

    /**
     * 获取指定文件的提交历史
     * @param repoDirectory Git仓库目录
     * @param filePath 文件相对于仓库根目录的路径
     * @param count 获取的提交数量
     * @return 提交历史列表
     */
    public static List<GitCommitInfo> getFileCommitHistory(File repoDirectory, String filePath, int count) {
        List<GitCommitInfo> commits = new ArrayList<>();

        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder
                    .setGitDir(new File(repoDirectory, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            Git git = new Git(repository);
            
            // 使用 log 命令获取文件的提交历史
            Iterable<RevCommit> logs = git.log()
                    .addPath(filePath)
                    .setMaxCount(count)
                    .call();

            for (RevCommit commit : logs) {
                GitCommitInfo info = new GitCommitInfo();
                info.setMessage(commit.getFullMessage());
                info.setAuthor(commit.getAuthorIdent().getName());
                info.setEmail(commit.getAuthorIdent().getEmailAddress());
                info.setCommitTime(commit.getCommitTime() * 1000L);
                info.setCommitId(commit.getName());
                commits.add(info);
            }

            git.close();
            repository.close();
            return commits;

        } catch (Exception e) {
            System.err.println("Error reading file commit history: " + e.getMessage());
            e.printStackTrace();
            return commits;
        }
    }

    /**
     * 获取文件在指定提交中的内容
     * @param repoDirectory Git仓库目录
     * @param commitId 提交ID
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String getFileContentAtCommit(File repoDirectory, String commitId, String filePath) {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder
                    .setGitDir(new File(repoDirectory, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            ObjectId commitObjectId = ObjectId.fromString(commitId);
            RevWalk revWalk = new RevWalk(repository);
            RevCommit commit = revWalk.parseCommit(commitObjectId);
            
            TreeWalk treeWalk = TreeWalk.forPath(repository, filePath, commit.getTree());
            if (treeWalk != null) {
                ObjectId blobId = treeWalk.getObjectId(0);
                org.eclipse.jgit.lib.ObjectLoader loader = repository.open(blobId);
                byte[] bytes = loader.getBytes();
                
                treeWalk.close();
                revWalk.close();
                repository.close();
                
                return new String(bytes, "UTF-8");
            }
            
            revWalk.close();
            repository.close();
            return "";

        } catch (Exception e) {
            System.err.println("Error reading file content: " + e.getMessage());
            return "";
        }
    }

    /**
     * 获取文件在两个提交之间的差异
     * @param repoDirectory Git仓库目录
     * @param oldCommitId 旧提交ID（可以为null，表示父提交）
     * @param newCommitId 新提交ID
     * @param filePath 文件路径
     * @return 差异文本
     */
    public static String getFileDiff(File repoDirectory, String oldCommitId, String newCommitId, String filePath) {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder
                    .setGitDir(new File(repoDirectory, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            RevWalk revWalk = new RevWalk(repository);
            RevCommit newCommit = revWalk.parseCommit(ObjectId.fromString(newCommitId));
            
            RevCommit oldCommit = null;
            if (oldCommitId != null && !oldCommitId.isEmpty()) {
                oldCommit = revWalk.parseCommit(ObjectId.fromString(oldCommitId));
            } else if (newCommit.getParentCount() > 0) {
                // 如果没有指定旧提交，使用父提交
                oldCommit = revWalk.parseCommit(newCommit.getParent(0));
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DiffFormatter formatter = new DiffFormatter(out);
            formatter.setRepository(repository);
            formatter.setDiffComparator(RawTextComparator.DEFAULT);
            formatter.setDetectRenames(true);

            if (oldCommit != null) {
                List<DiffEntry> diffs = formatter.scan(oldCommit.getTree(), newCommit.getTree());
                
                for (DiffEntry diff : diffs) {
                    if (diff.getNewPath().equals(filePath) || diff.getOldPath().equals(filePath)) {
                        formatter.format(diff);
                    }
                }
            } else {
                // 初始提交，显示整个文件
                TreeWalk treeWalk = new TreeWalk(repository);
                treeWalk.addTree(newCommit.getTree());
                treeWalk.setRecursive(true);
                treeWalk.setFilter(org.eclipse.jgit.treewalk.filter.PathFilter.create(filePath));
                
                if (treeWalk.next()) {
                    out.write(("New file: " + filePath + "\n").getBytes());
                    String content = getFileContentAtCommit(repoDirectory, newCommitId, filePath);
                    String[] lines = content.split("\n");
                    for (int i = 0; i < lines.length; i++) {
                        out.write(("+" + lines[i] + "\n").getBytes());
                    }
                }
                treeWalk.close();
            }

            formatter.close();
            revWalk.close();
            repository.close();

            return out.toString("UTF-8");

        } catch (Exception e) {
            System.err.println("Error getting file diff: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Git仓库信息封装类
     */
    public static class GitRepositoryInfo {
        private String path;
        private List<String> remoteUrls;
        private String currentBranch;
        private List<String> branches;
        private GitCommitInfo lastCommit;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public List<String> getRemoteUrls() {
            return remoteUrls;
        }

        public void setRemoteUrls(List<String> remoteUrls) {
            this.remoteUrls = remoteUrls;
        }

        public String getCurrentBranch() {
            return currentBranch;
        }

        public void setCurrentBranch(String currentBranch) {
            this.currentBranch = currentBranch;
        }

        public List<String> getBranches() {
            return branches;
        }

        public void setBranches(List<String> branches) {
            this.branches = branches;
        }

        public GitCommitInfo getLastCommit() {
            return lastCommit;
        }

        public void setLastCommit(GitCommitInfo lastCommit) {
            this.lastCommit = lastCommit;
        }

        public boolean isGitRepo() {
            return true;
        }
    }

    /**
     * Git提交信息封装类
     */
    public static class GitCommitInfo {
        private String commitId;
        private String message;
        private String author;
        private String email;
        private long commitTime;

        public String getCommitId() {
            return commitId;
        }

        public void setCommitId(String commitId) {
            this.commitId = commitId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public long getCommitTime() {
            return commitTime;
        }

        public void setCommitTime(long commitTime) {
            this.commitTime = commitTime;
        }
    }
}
