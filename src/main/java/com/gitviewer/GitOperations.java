package com.gitviewer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Git操作工具类
 * 用于执行分支切换、拉取等Git操作
 */
public class GitOperations {

    /**
     * 将技术性错误信息转换为用户友好的提示
     */
    public static String translateError(String error) {
        if (error == null) return "Unknown error";
        String lower = error.toLowerCase();

        if (error.contains("REJECTED_NONFASTFORWARD")) return "Remote has newer commits, please Pull first";
        if (error.contains("REJECTED_REMOTE_CHANGED")) return "Remote branch was updated, please Pull first";
        if (error.contains("REJECTED_NODELETE")) return "Cannot delete remote branch (protected)";
        if (error.contains("REJECTED_OTHER_REASON")) return "Push rejected by remote server";

        if (lower.contains("authentication") || lower.contains("not authorized") || lower.contains("credentials"))
            return "Authentication failed, please check username and password";
        if (lower.contains("connection") || lower.contains("timeout") || lower.contains("refused")
                || lower.contains("unknownhost") || lower.contains("no route to host") || lower.contains("connect error"))
            return "Cannot connect to remote server, please check network";
        if (lower.contains("ssl") || lower.contains("certificate"))
            return "SSL/TLS certificate error, please check network security settings";
        if (lower.contains("merge conflict") || lower.contains("conflicting"))
            return "There are merge conflicts, please resolve manually";
        if (lower.contains("dirty tree") || lower.contains("local changes"))
            return "Cannot complete operation: there are uncommitted local changes";
        if (lower.contains("missing blob") || lower.contains("corrupt"))
            return "Local repository data is incomplete, please re-clone the project";
        if (lower.contains("not a git repository") || lower.contains("not found"))
            return "Not a valid Git repository";
        if (lower.contains("detached head"))
            return "Repository is in detached HEAD state";
        if (lower.contains("lock") || lower.contains(".git/index.lock"))
            return "Repository is locked by another operation, please try again later";
        if (lower.contains("permission denied"))
            return "Permission denied, please check file/folder permissions";
        if (lower.contains("out of memory"))
            return "Out of memory, the file is too large";

        return error;
    }

    // 静态变量保存全局认证信息
    private static CredentialsProvider globalCredentialsProvider = null;
    private static boolean authenticationTested = false;

    /**
     * 获取认证提供者
     * @param repositoryUrl 仓库URL，用于显示在认证对话框中
     * @return CredentialsProvider 或 null（如果用户取消）
     */
    private static CredentialsProvider getCredentialsProvider(String repositoryUrl) {
        // 如果已经有全局认证信息，直接使用
        if (globalCredentialsProvider != null) {
            return globalCredentialsProvider;
        }

        // 1. 首先检查AppSettings中是否配置了username/password
        AppSettings settings = AppSettings.getInstance();
        String configuredUsername = settings.getGitLabUsername();
        String configuredPassword = settings.getGitLabPassword();

        if (!configuredUsername.isEmpty() && !configuredPassword.isEmpty()) {
            globalCredentialsProvider = new UsernamePasswordCredentialsProvider(
                configuredUsername, 
                configuredPassword
            );
            return globalCredentialsProvider;
        }

        // 2. 如果有保存的认证信息（会话期间），使用它
        if (GitCredentialsDialog.hasSavedCredentials()) {
            globalCredentialsProvider = new UsernamePasswordCredentialsProvider(
                GitCredentialsDialog.getSavedUsername(),
                GitCredentialsDialog.getSavedPassword()
            );
            return globalCredentialsProvider;
        }

        // 3. 显示认证对话框让用户输入
        GitCredentialsDialog dialog = new GitCredentialsDialog(null, repositoryUrl);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            globalCredentialsProvider = new UsernamePasswordCredentialsProvider(
                dialog.getUsername(),
                dialog.getPassword()
            );
            return globalCredentialsProvider;
        }

        return null; // 用户取消了认证
    }

    /**
     * 清除全局认证信息（当认证失败时调用）
     */
    private static void clearGlobalCredentials() {
        globalCredentialsProvider = null;
        authenticationTested = false;
        GitCredentialsDialog.clearSavedCredentials();
    }

    /**
     * 公共方法：清除所有认证信息
     */
    public static void clearAllCredentials() {
        clearGlobalCredentials();
    }

    /**
     * 执行需要认证的Git操作
     * @param operation Git操作函数
     * @param repositoryUrl 仓库URL
     * @return 操作结果
     */
    private static boolean executeWithAuth(GitOperation operation, String repositoryUrl) {
        try {
            // 首先尝试不使用认证
            if (!authenticationTested) {
                try {
                    return operation.execute(null);
                } catch (Exception e) {
                    // 如果不是认证错误，直接抛出
                    if (!isAuthenticationError(e)) {
                        throw e;
                    }
                    // 标记需要认证
                    authenticationTested = true;
                }
            }

            // 尝试使用认证
            CredentialsProvider credentialsProvider = getCredentialsProvider(repositoryUrl);
            if (credentialsProvider == null) {
                return false; // 用户取消了认证
            }

            try {
                return operation.execute(credentialsProvider);
            } catch (Exception e) {
                // 如果认证失败，清除认证信息并重试一次
                if (isAuthenticationError(e)) {
                    clearGlobalCredentials();
                    
                    // 重新获取认证信息
                    credentialsProvider = getCredentialsProvider(repositoryUrl);
                    if (credentialsProvider == null) {
                        return false;
                    }
                    
                    return operation.execute(credentialsProvider);
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            System.err.println("Git operation failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否是认证错误
     */
    private static boolean isAuthenticationError(Exception e) {
        String message = e.getMessage();
        return message != null && (
            message.contains("Authentication") ||
            message.contains("not authorized") ||
            message.contains("authentication failed") ||
            message.contains("401") ||
            message.contains("403")
        );
    }

    /**
     * Git操作接口
     */
    @FunctionalInterface
    private interface GitOperation {
        boolean execute(CredentialsProvider credentialsProvider) throws Exception;
    }

    /**
     * 切换到指定分支
     * @param directory Git仓库目录
     * @param branchName 分支名称
     * @return 是否成功
     */
    public static boolean switchBranch(File directory, String branchName) {
        try (Git git = Git.open(directory)) {
            // 检查分支是否存在
            boolean branchExists = checkBranchExists(git, branchName);

            if (branchExists) {
                // 分支存在，直接checkout
                git.checkout()
                        .setName(branchName)
                        .call();
                return true;
            } else {
                // 分支不存在，尝试从远程创建并切换
                String remoteUrl = getRemoteUrl(git);
                
                return executeWithAuth((credentialsProvider) -> {
                    // Fetch远程分支
                    if (credentialsProvider != null) {
                        git.fetch().setCredentialsProvider(credentialsProvider).call();
                    } else {
                        git.fetch().call();
                    }

                    // 尝试checkout远程分支
                    String remoteBranch = "refs/remotes/origin/" + branchName;
                    git.checkout()
                            .setName(branchName)
                            .setCreateBranch(true)
                            .setStartPoint(remoteBranch)
                            .call();
                    return true;
                }, remoteUrl);
            }
        } catch (GitAPIException | IOException e) {
            System.err.println("Error switching branch: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 拉取最新代码
     * @param directory Git仓库目录
     * @return 是否成功
     */
    public static boolean pull(File directory) {
        try (Git git = Git.open(directory)) {
            String remoteUrl = getRemoteUrl(git);
            
            return executeWithAuth((credentialsProvider) -> {
                PullResult result;
                if (credentialsProvider != null) {
                    result = git.pull()
                            .setRebase(false)
                            .setCredentialsProvider(credentialsProvider)
                            .call();
                } else {
                    result = git.pull()
                            .setRebase(false)
                            .call();
                }

                if (result.isSuccessful()) {
                    return true;
                } else {
                    System.err.println("Pull failed: " + result.toString());
                    return false;
                }
            }, remoteUrl);
        } catch (Exception e) {
            System.err.println("Error pulling: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取远程仓库URL
     * @param git Git对象
     * @return 远程URL
     */
    private static String getRemoteUrl(Git git) {
        try {
            return git.getRepository().getConfig().getString("remote", "origin", "url");
        } catch (Exception e) {
            return "Unknown Repository";
        }
    }

    /**
     * 获取所有远程分支列表
     * @param directory Git仓库目录
     * @return 远程分支名称列表
     */
    public static java.util.List<String> getRemoteBranches(File directory) {
        try (Git git = Git.open(directory)) {
            java.util.List<String> branches = new java.util.ArrayList<>();

            for (Ref ref : git.branchList()
                    .setListMode(org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL)
                    .call()) {
                String branchName = ref.getName();
                if (branchName.startsWith("refs/remotes/")) {
                    // 移除 refs/remotes/ 前缀
                    String displayName = branchName.replace("refs/remotes/", "");
                    if (!displayName.endsWith("HEAD")) {
                        branches.add(displayName);
                    }
                }
            }

            return branches;
        } catch (GitAPIException | IOException e) {
            System.err.println("Error getting remote branches: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 获取所有本地分支列表
     * @param directory Git仓库目录
     * @return 本地分支名称列表
     */
    public static java.util.List<String> getLocalBranches(File directory) {
        try (Git git = Git.open(directory)) {
            java.util.List<String> branches = new java.util.ArrayList<>();

            for (Ref ref : git.branchList().call()) {
                String branchName = ref.getName();
                if (branchName.startsWith("refs/heads/")) {
                    // 移除 refs/heads/ 前缀
                    String displayName = branchName.replace("refs/heads/", "");
                    branches.add(displayName);
                }
            }

            return branches;
        } catch (GitAPIException | IOException e) {
            System.err.println("Error getting local branches: " + e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

    /**
     * 检查分支是否存在
     * @param git Git对象
     * @param branchName 分支名称
     * @return 是否存在
     */
    private static boolean checkBranchExists(Git git, String branchName) {
        try {
            for (Ref ref : git.branchList().call()) {
                String name = ref.getName();
                if (name.equals("refs/heads/" + branchName) ||
                    name.equals("refs/remotes/origin/" + branchName)) {
                    return true;
                }
            }
            return false;
        } catch (GitAPIException e) {
            return false;
        }
    }

    /**
     * 获取当前分支名称
     * @param directory Git仓库目录
     * @return 当前分支名称
     */
    public static String getCurrentBranch(File directory) {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            try (Repository repository = builder
                    .setGitDir(new File(directory, ".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build()) {
                String branch = repository.getBranch();
                return branch != null ? branch : "Unknown";
            }
        } catch (IOException e) {
            System.err.println("Error getting current branch: " + e.getMessage());
            return "Unknown";
        }
    }

    /**
     * 获取仓库状态（是否有未提交的更改）
     * @param directory Git仓库目录
     * @return 是否有未提交的更改
     */
    public static boolean hasUncommittedChanges(File directory) {
        try (Git git = Git.open(directory)) {
            return git.status().call().hasUncommittedChanges();
        } catch (GitAPIException | IOException e) {
            System.err.println("Error checking status: " + e.getMessage());
            return false;
        }
    }

    /**
     * 执行fetch操作
     * @param directory Git仓库目录
     * @return 是否成功
     */
    public static boolean fetch(File directory) {
        try (Git git = Git.open(directory)) {
            String remoteUrl = getRemoteUrl(git);

            return executeWithAuth((credentialsProvider) -> {
                if (credentialsProvider != null) {
                    git.fetch().setCredentialsProvider(credentialsProvider).call();
                } else {
                    git.fetch().call();
                }
                return true;
            }, remoteUrl);
        } catch (Exception e) {
            System.err.println("Error fetching: " + e.getMessage());
            return false;
        }
    }

    /**
     * 执行cherry-pick操作
     * @param directory Git仓库目录
     * @param commitId 要cherry-pick的commit ID
     * @return 是否成功
     */
    public static boolean cherryPick(File directory, String commitId) {
        try (Git git = Git.open(directory)) {
            // 尝试 cherry-pick
            git.cherryPick()
                    .include(org.eclipse.jgit.lib.ObjectId.fromString(commitId))
                    .call();

            // 检查是否有冲突
            if (hasUncommittedChanges(directory)) {
                System.out.println("Warning: Cherry-pick completed with possible conflicts");
                return true; // 仍然返回 true，让用户知道需要解决冲突
            }

            return true;

        } catch (org.eclipse.jgit.api.errors.JGitInternalException e) {
            // Cherry-pick 可能会有冲突，但这不算完全失败
            if (e.getMessage() != null && e.getMessage().contains("cherry-pick")) {
                System.out.println("Cherry-pick result: " + e.getMessage());
                return true; // 冲突情况也返回 true，让用户知道需要处理
            }
            System.err.println("Error during cherry-pick: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error cherry-picking: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取冲突文件列表
     * @param directory Git仓库目录
     * @return 冲突文件路径列表
     */
    public static List<String> getConflictedFiles(File directory) {
        List<String> conflictedFiles = new ArrayList<>();
        try (Git git = Git.open(directory)) {
            // 使用 status 命令获取冲突文件
            Status status = git.status().call();

            // 获取冲突文件集合
            Set<String> conflicting = status.getConflicting();
            for (String file : conflicting) {
                conflictedFiles.add(file);
            }
        } catch (Exception e) {
            System.err.println("Error getting conflicted files: " + e.getMessage());
            e.printStackTrace();
        }
        return conflictedFiles;
    }

    // ===== 文件变更信息 =====

    public static class FileChangeInfo {
        private String filePath;
        private String changeType;
        private String moduleName;
        private String repoPath;
        private String branch;
        private File repoDir;
        private long lastModified;

        public FileChangeInfo(String filePath, String changeType, String moduleName, String repoPath, String branch, File repoDir, long lastModified) {
            this.filePath = filePath;
            this.changeType = changeType;
            this.moduleName = moduleName;
            this.repoPath = repoPath;
            this.branch = branch;
            this.repoDir = repoDir;
            this.lastModified = lastModified;
        }

        public String getFilePath() { return filePath; }
        public String getChangeType() { return changeType; }
        public String getModuleName() { return moduleName; }
        public String getRepoPath() { return repoPath; }
        public String getBranch() { return branch; }
        public File getRepoDir() { return repoDir; }
        public long getLastModified() { return lastModified; }

        @Override
        public String toString() {
            return "[" + (moduleName != null ? moduleName : "") + "] ["
                    + (branch != null ? branch : "?") + "] ["
                    + (changeType != null ? changeType : "") + "] "
                    + (filePath != null ? filePath : "");
        }
    }

    /**
     * 扫描目录下的所有 git 子仓库
     */
    public static List<File> findGitRepositories(File parentDir) {
        List<File> repos = new ArrayList<>();
        if (parentDir == null || !parentDir.isDirectory()) return repos;

        if (GitInfoExtractor.isGitRepository(parentDir)) {
            repos.add(parentDir);
            return repos;
        }

        File[] children = parentDir.listFiles();
        if (children == null) return repos;

        for (File child : children) {
            if (!child.isDirectory()) continue;
            if (GitInfoExtractor.isGitRepository(child)) {
                repos.add(child);
            } else {
                File[] grandchildren = child.listFiles();
                if (grandchildren != null) {
                    for (File grandchild : grandchildren) {
                        if (grandchild.isDirectory() && GitInfoExtractor.isGitRepository(grandchild)) {
                            repos.add(grandchild);
                        }
                    }
                }
            }
        }
        return repos;
    }

    /**
     * 获取未暂存的文件列表
     */
    public static List<FileChangeInfo> getUnstagedFiles(File repoDir) {
        List<FileChangeInfo> result = new ArrayList<>();
        try (Git git = Git.open(repoDir)) {
            Status status = git.status().call();
            String branch = getCurrentBranch(repoDir);
            String moduleName = repoDir.getName();
            String repoPath = repoDir.getAbsolutePath();

            List<String> gitignorePatterns = readGitignorePatterns(repoDir);

            addFileChanges(result, status.getModified(), "Modified", moduleName, repoPath, branch, repoDir, gitignorePatterns);
            addFileChanges(result, status.getUntracked(), "Untracked", moduleName, repoPath, branch, repoDir, gitignorePatterns);
            addFileChanges(result, status.getMissing(), "Deleted", moduleName, repoPath, branch, repoDir, gitignorePatterns);
            addFileChanges(result, status.getRemoved(), "Deleted", moduleName, repoPath, branch, repoDir, gitignorePatterns);
            addFileChanges(result, status.getAdded(), "Added", moduleName, repoPath, branch, repoDir, gitignorePatterns);
            addFileChanges(result, status.getChanged(), "Modified", moduleName, repoPath, branch, repoDir, gitignorePatterns);
        } catch (Exception e) {
            System.err.println("Error getting unstaged files: " + e.getMessage());
        }
        return result;
    }

    private static void addFileChanges(List<FileChangeInfo> result, Set<String> files,
                                        String changeType, String moduleName, String repoPath,
                                        String branch, File repoDir, List<String> gitignorePatterns) {
        for (String file : files) {
            // 跳过 .gitignore 自身
            if (file.equals(".gitignore")) {
                continue;
            }
            // 跳过匹配 .gitignore 规则的文件（处理已 tracked 但在 .gitignore 中的情况）
            if (!gitignorePatterns.isEmpty() && matchesGitignore(file, gitignorePatterns)) {
                continue;
            }
            File f = new File(repoDir, file);
            long lastMod = f.exists() ? f.lastModified() : 0;
            result.add(new FileChangeInfo(file, changeType, moduleName, repoPath, branch, repoDir, lastMod));
        }
    }

    /**
     * 获取已提交但未推送的文件列表
     */
    public static List<FileChangeInfo> getUnpushedFiles(File repoDir) {
        List<FileChangeInfo> result = new ArrayList<>();
        try (Git git = Git.open(repoDir)) {
            String branch = getCurrentBranch(repoDir);
            String moduleName = repoDir.getName();
            String repoPath = repoDir.getAbsolutePath();

            String trackingBranch = "refs/remotes/origin/" + branch;
            Ref trackingRef = git.getRepository().findRef(trackingBranch);
            if (trackingRef == null) {
                return result;
            }

            ObjectId trackingId = trackingRef.getObjectId();
            ObjectId headId = git.getRepository().resolve("HEAD");
            if (headId == null || headId.equals(trackingId)) {
                return result;
            }

            try (RevWalk revWalk = new RevWalk(git.getRepository())) {
                RevCommit trackingCommit = revWalk.parseCommit(trackingId);
                RevCommit headCommit = revWalk.parseCommit(headId);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (DiffFormatter formatter = new DiffFormatter(out)) {
                    formatter.setRepository(git.getRepository());
                    formatter.setDiffComparator(RawTextComparator.DEFAULT);
                    formatter.setDetectRenames(true);

                    List<DiffEntry> diffs = formatter.scan(trackingCommit.getTree(), headCommit.getTree());
                    for (DiffEntry diff : diffs) {
                        String path = diff.getChangeType() == DiffEntry.ChangeType.DELETE
                                ? diff.getOldPath() : diff.getNewPath();
                        String changeType = diff.getChangeType().name();
                        File f = new File(repoDir, path);
                        long lastMod = f.exists() ? f.lastModified() : 0;
                        result.add(new FileChangeInfo(path, changeType, moduleName, repoPath, branch, repoDir, lastMod));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting unpushed files: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取未推送到远程的 commit ID 集合
     */
    public static Set<String> getUnpushedCommitIds(File repoDir) {
        Set<String> result = new HashSet<>();
        try (Git git = Git.open(repoDir)) {
            String branch = getCurrentBranch(repoDir);
            String trackingBranch = "refs/remotes/origin/" + branch;
            Ref trackingRef = git.getRepository().findRef(trackingBranch);
            if (trackingRef == null) return result;

            ObjectId trackingId = trackingRef.getObjectId();
            ObjectId headId = git.getRepository().resolve("HEAD");
            if (headId == null || headId.equals(trackingId)) return result;

            try (RevWalk revWalk = new RevWalk(git.getRepository())) {
                revWalk.markStart(revWalk.parseCommit(headId));
                revWalk.markUninteresting(revWalk.parseCommit(trackingId));
                for (RevCommit commit : revWalk) {
                    result.add(commit.getName());
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting unpushed commits: " + e.getMessage());
        }
        return result;
    }

    /**
     * 获取工作区文件 vs HEAD 的 diff
     */
    public static String getWorkingTreeDiff(File repoDir, String filePath) {
        try (Git git = Git.open(repoDir)) {
            Repository repo = git.getRepository();

            ObjectId headId = repo.resolve("HEAD");
            if (headId == null) {
                return getUntrackedFileDiff(repoDir, filePath);
            }

            // 尝试用 DiffFormatter 直接比较 HEAD tree vs 工作区
            try {
                RevWalk rw = new RevWalk(repo);
                RevCommit headCommit = rw.parseCommit(headId);
                ObjectId headTreeId = headCommit.getTree().getId();
                rw.close();

                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                ObjectReader reader = repo.newObjectReader();
                oldTreeIter.reset(reader, headTreeId);
                reader.close();

                org.eclipse.jgit.treewalk.WorkingTreeIterator workingTreeIter =
                        new org.eclipse.jgit.treewalk.FileTreeIterator(repo);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (DiffFormatter formatter = new DiffFormatter(out)) {
                    formatter.setRepository(repo);
                    formatter.setDiffComparator(RawTextComparator.DEFAULT);
                    formatter.setDetectRenames(true);

                    List<DiffEntry> diffs = git.diff()
                            .setOldTree(oldTreeIter)
                            .setNewTree(workingTreeIter)
                            .setPathFilter(org.eclipse.jgit.treewalk.filter.PathFilter.create(filePath))
                            .call();

                    for (DiffEntry diff : diffs) {
                        formatter.format(diff);
                    }
                    formatter.flush();
                }

                String diffStr = out.toString("UTF-8");
                if (!diffStr.isEmpty()) {
                    return diffStr;
                }
            } catch (Exception e) {
                System.err.println("DiffFormatter failed (" + e.getMessage() + "), using manual diff");
            }

            // Fallback: 从 HEAD 读取旧版本内容，手动生成 diff
            return buildManualDiff(repo, repoDir, filePath, headId);
        } catch (Exception e) {
            System.err.println("Error getting working tree diff: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 手动对比 HEAD 版本和工作区文件，生成 diff
     */
    private static String buildManualDiff(Repository repo, File repoDir, String filePath, ObjectId headId) {
        File workingFile = new File(repoDir, filePath);
        if (!workingFile.exists()) {
            return buildDeleteDiff(filePath);
        }

        // 从 HEAD tree 读取文件旧版本
        String headContent = null;
        try {
            RevWalk rw = new RevWalk(repo);
            RevCommit headCommit = rw.parseCommit(headId);
            org.eclipse.jgit.treewalk.TreeWalk treeWalk = new org.eclipse.jgit.treewalk.TreeWalk(repo);
            treeWalk.addTree(headCommit.getTree());
            treeWalk.setRecursive(true);
            treeWalk.setFilter(org.eclipse.jgit.treewalk.filter.PathFilter.create(filePath));
            if (treeWalk.next()) {
                ObjectId blobId = treeWalk.getObjectId(0);
                ObjectLoader loader = repo.open(blobId);
                headContent = new String(loader.getBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").replace("\r", "\n");
            }
            treeWalk.close();
            rw.close();
        } catch (Exception e) {
            System.err.println("Cannot read HEAD version: " + e.getMessage());
        }

        // HEAD 中没有这个文件 → 当作新文件
        if (headContent == null) {
            return getUntrackedFileDiff(repoDir, filePath);
        }

        // 读取工作区文件
        try {
            if (workingFile.length() > 1024 * 1024) {
                return "diff --git a/" + filePath + " b/" + filePath + "\n"
                        + "(Binary or large file - " + (workingFile.length() / 1024) + " KB)\n";
            }
            String workingContent = new String(Files.readAllBytes(workingFile.toPath()), StandardCharsets.UTF_8).replace("\r\n", "\n").replace("\r", "\n");

            // 逐行对比，生成 unified diff
            String[] oldLines = headContent.split("\n");
            String[] newLines = workingContent.split("\n");
            return generateUnifiedDiff(filePath, oldLines, newLines);
        } catch (Exception e) {
            return "Error comparing files: " + e.getMessage();
        }
    }

    private static String buildDeleteDiff(String filePath) {
        return "diff --git a/" + filePath + " b/" + filePath + "\n"
                + "deleted file mode 100644\n"
                + "--- a/" + filePath + "\n"
                + "+++ /dev/null\n";
    }

    /**
     * 生成 unified diff 格式
     */
    private static String generateUnifiedDiff(String filePath, String[] oldLines, String[] newLines) {
        StringBuilder sb = new StringBuilder();
        sb.append("diff --git a/").append(filePath).append(" b/").append(filePath).append("\n");
        sb.append("--- a/").append(filePath).append("\n");
        sb.append("+++ b/").append(filePath).append("\n");

        int m = oldLines.length, n = newLines.length;

        // LCS 动态规划
        int[][] dp = new int[m + 1][n + 1];
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (oldLines[i - 1].equals(newLines[j - 1])) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }

        // 回溯得到 diff 操作序列
        List<String> ops = new ArrayList<>();
        int i = m, j = n;
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldLines[i - 1].equals(newLines[j - 1])) {
                ops.add(" " + oldLines[i - 1]);
                i--; j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                ops.add("+" + newLines[j - 1]);
                j--;
            } else {
                ops.add("-" + oldLines[i - 1]);
                i--;
            }
        }
        java.util.Collections.reverse(ops);

        // 找到所有变化行的索引
        List<Integer> changeIndices = new ArrayList<>();
        for (int k = 0; k < ops.size(); k++) {
            if (!ops.get(k).startsWith(" ")) {
                changeIndices.add(k);
            }
        }

        if (changeIndices.isEmpty()) {
            // 没有变化
            return sb.toString();
        }

        // 将相邻的变化合并为 hunks，每个 hunk 包含前后 3 行上下文
        int context = 3;
        List<int[]> hunks = new ArrayList<>(); // [start, end)
        int hunkStart = Math.max(0, changeIndices.get(0) - context);
        int hunkEnd = changeIndices.get(0) + 1;

        for (int c = 1; c < changeIndices.size(); c++) {
            int ci = changeIndices.get(c);
            if (ci - context <= hunkEnd) {
                // 合并到当前 hunk
                hunkEnd = ci + 1;
            } else {
                hunks.add(new int[]{hunkStart, Math.min(ops.size(), hunkEnd + context)});
                hunkStart = Math.max(0, ci - context);
                hunkEnd = ci + 1;
            }
        }
        hunks.add(new int[]{hunkStart, Math.min(ops.size(), hunkEnd + context)});

        // 输出每个 hunk
        for (int[] hunk : hunks) {
            int start = hunk[0], end = hunk[1];

            // 计算这个 hunk 范围内的 old/new 行号
            int oldLine = 1, newLine = 1;
            for (int k = 0; k < start; k++) {
                if (!ops.get(k).startsWith("+")) oldLine++;
                if (!ops.get(k).startsWith("-")) newLine++;
            }
            int oldStart = oldLine, newStart = newLine;
            int oldCount = 0, newCount = 0;
            for (int k = start; k < end; k++) {
                if (!ops.get(k).startsWith("+")) oldCount++;
                if (!ops.get(k).startsWith("-")) newCount++;
            }

            sb.append("@@ -").append(oldStart).append(",").append(oldCount)
              .append(" +").append(newStart).append(",").append(newCount).append(" @@\n");
            for (int k = start; k < end; k++) {
                sb.append(ops.get(k)).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 获取远程跟踪分支 vs HEAD 的 diff（已 commit 未 push 的变更）
     */
    public static String getUnpushedDiff(File repoDir, String filePath) {
        try (Git git = Git.open(repoDir)) {
            Repository repo = git.getRepository();
            String branch = getCurrentBranch(repoDir);
            String trackingBranch = "refs/remotes/origin/" + branch;
            Ref trackingRef = repo.findRef(trackingBranch);
            if (trackingRef == null) {
                return "";
            }

            ObjectId trackingId = trackingRef.getObjectId();
            ObjectId headId = repo.resolve("HEAD");
            if (headId == null || headId.equals(trackingId)) {
                return "";
            }

            // 从 tracking branch 和 HEAD 读取文件内容
            String trackingContent = readFileFromCommit(repo, trackingId, filePath);
            String headContent = readFileFromCommit(repo, headId, filePath);

            if (trackingContent == null && headContent == null) {
                return "";
            }

            // tracking 中没有 → 新增文件
            if (trackingContent == null) {
                return buildNewFileDiff(filePath, headContent != null ? headContent : "");
            }

            // HEAD 中没有 → 删除文件
            if (headContent == null) {
                return buildDeleteDiff(filePath);
            }

            // 两边都有 → 对比
            String[] oldLines = trackingContent.split("\n");
            String[] newLines = headContent.split("\n");
            return generateUnifiedDiff(filePath, oldLines, newLines);
        } catch (Exception e) {
            System.err.println("Error getting unpushed diff: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 从指定 commit 中读取文件内容
     */
    private static String readFileFromCommit(Repository repo, ObjectId commitId, String filePath) {
        try {
            RevWalk rw = new RevWalk(repo);
            RevCommit commit = rw.parseCommit(commitId);
            org.eclipse.jgit.treewalk.TreeWalk treeWalk = new org.eclipse.jgit.treewalk.TreeWalk(repo);
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(true);
            treeWalk.setFilter(org.eclipse.jgit.treewalk.filter.PathFilter.create(filePath));
            if (treeWalk.next()) {
                ObjectId blobId = treeWalk.getObjectId(0);
                ObjectLoader loader = repo.open(blobId);
                String content = new String(loader.getBytes(), StandardCharsets.UTF_8)
                        .replace("\r\n", "\n").replace("\r", "\n");
                treeWalk.close();
                rw.close();
                return content;
            }
            treeWalk.close();
            rw.close();
        } catch (Exception e) {
            System.err.println("Error reading file from commit: " + e.getMessage());
        }
        return null;
    }

    private static String buildNewFileDiff(String filePath, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("diff --git a/").append(filePath).append(" b/").append(filePath).append("\n");
        sb.append("new file mode 100644\n");
        sb.append("--- /dev/null\n");
        sb.append("+++ b/").append(filePath).append("\n");
        String[] lines = content.split("\n");
        sb.append("@@ -0,0 +1,").append(lines.length).append(" @@\n");
        for (String line : lines) {
            sb.append("+").append(line).append("\n");
        }
        return sb.toString();
    }

    public static String getUntrackedFileDiff(File repoDir, String filePath) {
        File file = new File(repoDir, filePath);
        if (!file.exists()) return "";
        try {
            // 跳过大于 1MB 的文件
            if (file.length() > 1024 * 1024) {
                return "diff --git a/" + filePath + " b/" + filePath + "\n"
                        + "new file mode 100644\n"
                        + "--- /dev/null\n"
                        + "+++ b/" + filePath + "\n"
                        + "@@ -0,0 +1 @@\n"
                        + "+(Binary or large file - " + (file.length() / 1024) + " KB)\n";
            }
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8).replace("\r\n", "\n").replace("\r", "\n");
            StringBuilder sb = new StringBuilder();
            sb.append("diff --git a/").append(filePath).append(" b/").append(filePath).append("\n");
            sb.append("new file mode 100644\n");
            sb.append("--- /dev/null\n");
            sb.append("+++ b/").append(filePath).append("\n");
            String[] lines = content.split("\n");
            sb.append("@@ -0,0 +1,").append(lines.length).append(" @@\n");
            for (String line : lines) {
                sb.append("+").append(line).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    /**
     * 执行 add + commit + push
     */
    public static boolean commitAndPush(File repoDir, List<String> filesToAdd, String commitMessage) {
        try (Git git = Git.open(repoDir)) {
            String remoteUrl = getRemoteUrl(git);

            // add files (check if file exists to decide add vs rm)
            boolean hasChanges = false;
            for (String file : filesToAdd) {
                File diskFile = new File(repoDir, file);
                if (diskFile.exists()) {
                    git.add().addFilepattern(file).call();
                    hasChanges = true;
                } else {
                    git.rm().setCached(true).addFilepattern(file).call();
                    hasChanges = true;
                }
            }

            // commit (only if there are changes to commit)
            if (hasChanges) {
                git.commit().setMessage(commitMessage).call();
            }

            // push (with auto-pull-retry for NONFASTFORWARD)
            final String[] errorMsg = {null};
            boolean pushOk = doPushWithRetry(git, remoteUrl, errorMsg);

            if (!pushOk && errorMsg[0] != null) {
                throw new RuntimeException(errorMsg[0]);
            }
            return pushOk;
        } catch (Exception e) {
            System.err.println("Error in commit and push: " + e.getMessage());
            throw new RuntimeException(translateError(e.getMessage()));
        }
    }

    /**
     * Push with auto-pull-retry: 如果 NONFASTFORWARD 则自动 pull 后重试
     */
    private static boolean doPushWithRetry(Git git, String remoteUrl, String[] errorMsg) {
        boolean ok = doPushOnce(git, remoteUrl, errorMsg);
        if (ok) return true;

        // 如果是 NONFASTFORWARD，自动 pull 后重试
        if (errorMsg[0] != null && errorMsg[0].contains("REJECTED_NONFASTFORWARD")) {
            System.err.println("NONFASTFORWARD detected, auto-pulling...");
            try {
                pull(git.getRepository().getWorkTree());
                // pull 成功后重试 push
                errorMsg[0] = null;
                return doPushOnce(git, remoteUrl, errorMsg);
            } catch (Exception e) {
                errorMsg[0] = "Auto-pull failed: " + translateError(e.getMessage());
                return false;
            }
        }
        return false;
    }

    private static boolean doPushOnce(Git git, String remoteUrl, String[] errorMsg) {
        boolean result = executeWithAuth((credentialsProvider) -> {
            Iterable<PushResult> results;
            try {
                if (credentialsProvider != null) {
                    results = git.push().setCredentialsProvider(credentialsProvider).call();
                } else {
                    results = git.push().call();
                }
            } catch (Exception e) {
                errorMsg[0] = e.getMessage();
                return false;
            }

            for (PushResult pr : results) {
                for (RemoteRefUpdate rru : pr.getRemoteUpdates()) {
                    if (rru.getStatus() == RemoteRefUpdate.Status.REJECTED_NONFASTFORWARD) {
                        errorMsg[0] = "REJECTED_NONFASTFORWARD:" + rru.getRemoteName();
                        return false;
                    }
                    if (rru.getStatus() != RemoteRefUpdate.Status.OK
                            && rru.getStatus() != RemoteRefUpdate.Status.UP_TO_DATE) {
                        errorMsg[0] = rru.getRemoteName() + ": " + rru.getStatus().name();
                        return false;
                    }
                }
            }
            return true;
        }, remoteUrl);

        // executeWithAuth 吞掉异常时 errorMsg 可能为空
        if (!result && errorMsg[0] == null) {
            errorMsg[0] = "Push failed - authentication or network error";
        }
        return result;
    }

    /**
     * 回滚工作区文件到 HEAD 版本（丢弃本地修改）
     */
    public static boolean rollbackToHead(File repoDir, String filePath, boolean isUntracked) {
        try (Git git = Git.open(repoDir)) {
            if (isUntracked) {
                File file = new File(repoDir, filePath);
                if (file.exists()) {
                    file.delete();
                }
            } else {
                // 从 HEAD 读取文件内容写回工作区
                Repository repo = git.getRepository();
                ObjectId headId = repo.resolve("HEAD");
                if (headId == null) return false;

                RevWalk rw = new RevWalk(repo);
                RevCommit headCommit = rw.parseCommit(headId);
                org.eclipse.jgit.treewalk.TreeWalk treeWalk = new org.eclipse.jgit.treewalk.TreeWalk(repo);
                treeWalk.addTree(headCommit.getTree());
                treeWalk.setRecursive(true);
                treeWalk.setFilter(org.eclipse.jgit.treewalk.filter.PathFilter.create(filePath));

                if (treeWalk.next()) {
                    ObjectId blobId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repo.open(blobId);
                    byte[] content = loader.getBytes();
                    treeWalk.close();
                    rw.close();

                    File targetFile = new File(repoDir, filePath);
                    targetFile.getParentFile().mkdirs();
                    Files.write(targetFile.toPath(), content);
                } else {
                    // HEAD 中没有这个文件（可能是 Added）→ 删除
                    treeWalk.close();
                    rw.close();
                    File targetFile = new File(repoDir, filePath);
                    if (targetFile.exists()) targetFile.delete();
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error rolling back to HEAD: " + e.getMessage());
            return false;
        }
    }

    /**
     * 回滚已 commit 未 push 的文件到远程跟踪分支版本，并自动 commit
     */
    public static boolean rollbackToRemote(File repoDir, String filePath) {
        try (Git git = Git.open(repoDir)) {
            Repository repo = git.getRepository();
            String branch = getCurrentBranch(repoDir);
            String trackingBranch = "refs/remotes/origin/" + branch;
            Ref trackingRef = repo.findRef(trackingBranch);
            if (trackingRef == null) {
                System.err.println("No tracking branch found");
                return false;
            }

            ObjectId trackingId = trackingRef.getObjectId();

            // 从 tracking branch 读取文件内容
            RevWalk rw = new RevWalk(repo);
            RevCommit trackingCommit = rw.parseCommit(trackingId);
            org.eclipse.jgit.treewalk.TreeWalk treeWalk = new org.eclipse.jgit.treewalk.TreeWalk(repo);
            treeWalk.addTree(trackingCommit.getTree());
            treeWalk.setRecursive(true);
            treeWalk.setFilter(org.eclipse.jgit.treewalk.filter.PathFilter.create(filePath));

            if (treeWalk.next()) {
                // tracking branch 有这个文件 → 恢复内容
                ObjectId blobId = treeWalk.getObjectId(0);
                ObjectLoader loader = repo.open(blobId);
                byte[] content = loader.getBytes();
                treeWalk.close();
                rw.close();

                File targetFile = new File(repoDir, filePath);
                targetFile.getParentFile().mkdirs();
                Files.write(targetFile.toPath(), content);
            } else {
                // tracking branch 没有这个文件 → 删除（文件是新增的）
                treeWalk.close();
                rw.close();

                File targetFile = new File(repoDir, filePath);
                if (targetFile.exists()) {
                    targetFile.delete();
                }
            }

            // add + commit
            git.add().addFilepattern(filePath).call();
            String commitMsg = "Rollback: " + filePath + " to remote/" + branch;
            git.commit().setMessage(commitMsg).call();

            return true;
        } catch (Exception e) {
            System.err.println("Error rolling back to remote: " + e.getMessage());
            return false;
        }
    }

    /**
     * 将文件路径添加到 .gitignore
     */
    public static boolean addToGitignore(File repoDir, List<String> filePaths) {
        File gitignore = new File(repoDir, ".gitignore");
        try {
            List<String> existingLines = new ArrayList<>();
            if (gitignore.exists()) {
                existingLines = Files.readAllLines(gitignore.toPath(), StandardCharsets.UTF_8);
            }

            List<String> newLines = new ArrayList<>();
            for (String path : filePaths) {
                boolean alreadyIgnored = false;
                for (String line : existingLines) {
                    if (line.trim().equals(path) || line.trim().equals("/" + path)) {
                        alreadyIgnored = true;
                        break;
                    }
                }
                if (!alreadyIgnored) {
                    newLines.add(path);
                }
            }

            if (!newLines.isEmpty()) {
                if (!existingLines.isEmpty() && !existingLines.get(existingLines.size() - 1).isEmpty()) {
                    newLines.add(0, "");
                }
                Files.write(gitignore.toPath(), newLines, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            return true;
        } catch (Exception e) {
            System.err.println("Error adding to .gitignore: " + e.getMessage());
            return false;
        }
    }

    /**
     * 从 git 追踪中移除文件（git rm --cached），但不删除本地文件
     * @return 成功移除的文件数
     */
    public static int removeFromTracking(File repoDir, List<String> filePaths) {
        int removed = 0;
        try (Git git = Git.open(repoDir)) {
            for (String path : filePaths) {
                try {
                    git.rm().setCached(true).addFilepattern(path).call();
                    removed++;
                } catch (Exception e) {
                    System.err.println("Error removing from tracking: " + path + " - " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error opening repo for rm: " + e.getMessage());
        }
        return removed;
    }

    /**
     * 读取 .gitignore 中的规则列表
     */
    public static List<String> readGitignorePatterns(File repoDir) {
        List<String> patterns = new ArrayList<>();
        File gitignore = new File(repoDir, ".gitignore");
        if (!gitignore.exists()) return patterns;
        try {
            List<String> lines = Files.readAllLines(gitignore.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    patterns.add(trimmed);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading .gitignore: " + e.getMessage());
        }
        return patterns;
    }

    /**
     * 检查文件路径是否匹配 .gitignore 规则（简单匹配）
     */
    public static boolean matchesGitignore(String filePath, List<String> patterns) {
        String normalizedPath = filePath.replace('\\', '/');
        for (String pattern : patterns) {
            String p = pattern.replace('\\', '/');
            // 去掉末尾的 /
            if (p.endsWith("/")) p = p.substring(0, p.length() - 1);

            // 精确匹配
            if (normalizedPath.equals(p)) return true;

            // 前缀匹配：pattern 是目录名
            if (normalizedPath.startsWith(p + "/")) return true;

            // 通配符匹配
            if (p.contains("*")) {
                String regex = p.replace(".", "\\.").replace("*", ".*").replace("?", ".");
                if (normalizedPath.matches(regex)) return true;
                // 匹配文件名部分
                String fileName = normalizedPath.contains("/")
                        ? normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1)
                        : normalizedPath;
                if (fileName.matches(regex)) return true;
            }

            // pattern 没有路径分隔符时，匹配任意目录下的同名文件
            if (!p.contains("/")) {
                String fileName = normalizedPath.contains("/")
                        ? normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1)
                        : normalizedPath;
                if (fileName.equals(p)) return true;
                // 也匹配文件名部分
                if (fileName.startsWith(p)) return true;
            }
        }
        return false;
    }
}
