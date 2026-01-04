package com.gitviewer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Git操作工具类
 * 用于执行分支切换、拉取等Git操作
 */
public class GitOperations {

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

        // 如果有保存的认证信息，使用它
        if (GitCredentialsDialog.hasSavedCredentials()) {
            globalCredentialsProvider = new UsernamePasswordCredentialsProvider(
                GitCredentialsDialog.getSavedUsername(),
                GitCredentialsDialog.getSavedPassword()
            );
            return globalCredentialsProvider;
        }

        // 显示认证对话框
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
            org.eclipse.jgit.api.Status status = git.status().call();

            // 获取冲突文件集合
            java.util.Set<String> conflicting = status.getConflicting();
            for (String file : conflicting) {
                conflictedFiles.add(file);
            }
        } catch (Exception e) {
            System.err.println("Error getting conflicted files: " + e.getMessage());
            e.printStackTrace();
        }
        return conflictedFiles;
    }
}
