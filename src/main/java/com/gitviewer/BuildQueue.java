package com.gitviewer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class BuildQueue {
    private static final Logger logger = LoggerFactory.getLogger(BuildQueue.class);

    public interface BuildQueueListener {
        void onEntryStatusChanged(QueueEntry entry);
        void onQueueCompleted(boolean allSuccess);
        void onQueueFailed(QueueEntry failedEntry, String failedApp);
        void onPollingError(String errorMessage);
    }

    private final List<QueueEntry> entries;
    private final PortalApiClient apiClient;
    private final String token;
    private final String tenant;
    private final BuildQueueListener listener;
    private int currentIndex = -1;
    private javax.swing.Timer pollingTimer;
    private int pollingIntervalMs;
    private boolean running = false;
    private boolean paused = false;
    private boolean autoPollingEnabled = true;

    public BuildQueue(List<QueueEntry> entries, PortalApiClient apiClient, String token, String tenant, BuildQueueListener listener) {
        this(entries, apiClient, token, tenant, listener, QueuePersistence.DEFAULT_POLLING_INTERVAL);
    }

    public BuildQueue(List<QueueEntry> entries, PortalApiClient apiClient, String token, String tenant, BuildQueueListener listener, int pollingIntervalSeconds) {
        this.entries = new ArrayList<>(entries);
        this.apiClient = apiClient;
        this.token = token;
        this.tenant = tenant;
        this.listener = listener;
        this.pollingIntervalMs = Math.max(5, pollingIntervalSeconds) * 1000;
    }

    public void start() {
        if (running) return;
        running = true;
        paused = false;
        logger.info("BuildQueue started, {} entries", entries.size());
        persistState();
        executeNext();
    }

    public void cancel() {
        stopTimer();
        running = false;
        paused = false;
        for (QueueEntry entry : entries) {
            if (entry.getStatus() == QueueEntry.QueueStatus.PENDING) {
                entry.setStatus(QueueEntry.QueueStatus.CANCELLED);
                fireEntryStatusChanged(entry);
            }
        }
        persistState();
    }

    public void pause() {
        if (!running || paused) return;
        paused = true;
        logger.info("BuildQueue paused");
    }

    public void resume() {
        if (!running || !paused) return;
        paused = false;
        logger.info("BuildQueue resumed");
        if (findBuildingEntry() == null) {
            executeNext();
        }
    }

    public void pausePolling() { autoPollingEnabled = false; stopTimer(); }

    public void resumeAutoPolling() {
        autoPollingEnabled = true;
        if (currentIndex >= 0 && currentIndex < entries.size()) {
            QueueEntry current = entries.get(currentIndex);
            if (current.getStatus() == QueueEntry.QueueStatus.BUILDING) startTimer();
        }
    }

    public void manualRefresh() {
        QueueEntry buildingEntry = findBuildingEntry();
        if (buildingEntry != null) pollBuildStatus(buildingEntry);
    }

    public void resumePolling() {
        running = true;
        paused = false;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getStatus() == QueueEntry.QueueStatus.BUILDING) { currentIndex = i; startTimer(); return; }
        }
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getStatus() == QueueEntry.QueueStatus.PENDING) { currentIndex = i; return; }
        }
        running = false;
    }

    public void setPollingInterval(int seconds) {
        this.pollingIntervalMs = Math.max(5, seconds) * 1000;
        if (pollingTimer != null && pollingTimer.isRunning()) {
            pollingTimer.setDelay(pollingIntervalMs);
            pollingTimer.setInitialDelay(pollingIntervalMs);
            pollingTimer.restart();
        }
    }

    public boolean isRunning() { return running; }
    public boolean isPaused() { return paused; }
    public List<QueueEntry> getEntries() { return new ArrayList<>(entries); }

    private void executeNext() {
        if (paused) {
            logger.info("BuildQueue is paused, skipping executeNext");
            return;
        }
        int nextIndex = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getStatus() == QueueEntry.QueueStatus.PENDING) { nextIndex = i; break; }
        }
        if (nextIndex == -1) {
            running = false; stopTimer();
            boolean allSuccess = entries.stream().allMatch(e -> e.getStatus() == QueueEntry.QueueStatus.SUCCESS || e.getStatus() == QueueEntry.QueueStatus.CANCELLED);
            if (entries.stream().allMatch(e -> isTerminalStatus(e.getStatus()))) QueuePersistence.delete();
            fireQueueCompleted(allSuccess);
            return;
        }
        currentIndex = nextIndex;
        submitBuild(entries.get(currentIndex));
    }

    private void submitBuild(QueueEntry entry) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                JSONObject requestBody = new JSONObject();
                JSONArray appsArray = new JSONArray();
                for (String appName : entry.getAppNames()) {
                    JSONObject appObj = new JSONObject();
                    appObj.put("app_name", appName);
                    appObj.put("build_type", "build_only");
                    appObj.put("git_branch", entry.getBranch());
                    appObj.put("issues", new JSONArray());
                    appObj.put("popconVisible", false);
                    appObj.put("user_name", tenant);
                    appObj.put("version", entry.getVersion());
                    appsArray.put(appObj);
                }
                requestBody.put("apps", appsArray);
                requestBody.put("description", "");
                requestBody.put("need_release_plan", false);
                requestBody.put("plan_id", "");
                requestBody.put("title", entry.getVersion());
                apiClient.submitMultiBuild(tenant, token, requestBody.toString());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    entry.setStatus(QueueEntry.QueueStatus.BUILDING);
                    entry.setTriggeredAt(Instant.now().toString());
                    persistState();
                    fireEntryStatusChanged(entry);
                    if (autoPollingEnabled) startTimer();
                } catch (Exception e) {
                    logger.error("Failed to submit build for group", e);
                    entry.setStatus(QueueEntry.QueueStatus.FAILED);
                    persistState(); fireEntryStatusChanged(entry);
                    running = false;
                    fireQueueFailed(entry, "API submission failed: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void startTimer() {
        stopTimer();
        pollingTimer = new javax.swing.Timer(pollingIntervalMs, e -> {
            QueueEntry buildingEntry = findBuildingEntry();
            if (buildingEntry != null) pollBuildStatus(buildingEntry);
        });
        pollingTimer.setRepeats(true);
        pollingTimer.start();
    }

    private void stopTimer() {
        if (pollingTimer != null) { pollingTimer.stop(); pollingTimer = null; }
    }

    private void pollBuildStatus(QueueEntry entry) {
        // 与主页面 Search 使用相同 API：getBuildResultByApp
        // creator = portal 登录用户名（邮箱），不过滤 appName，pageSize=50，pageNumber=0
        String creator = AppSettings.getInstance().getPortalUsername();
        logger.info("[Queue Poll] START group='{}' version='{}' creator='{}' apps={}",
                entry.getGroupName(), entry.getVersion(), creator, entry.getAppNames());

        SwingWorker<List<BuildResult>, Void> worker = new SwingWorker<>() {
            @Override protected List<BuildResult> doInBackground() throws Exception {
                AppBuildResult result = apiClient.getBuildResultByApp(tenant, token, null, creator, 0, 50);
                return result.getData() != null ? result.getData() : new ArrayList<>();
            }
            @Override protected void done() {
                try {
                    List<BuildResult> allResults = get();
                    logger.info("[Queue Poll] API returned {} records for group='{}' version='{}'",
                            allResults.size(), entry.getGroupName(), entry.getVersion());

                    // 遍历所有结果，逐条打印匹配情况
                    List<BuildResult> matched = new ArrayList<>();
                    for (BuildResult r : allResults) {
                        boolean appMatch = entry.getAppNames().contains(r.getAppName());
                        boolean verMatch = entry.getVersion().equals(r.getVersion());
                        logger.info("[Queue Poll]   record: app='{}' version='{}' status='{}' | appMatch={} verMatch={}",
                                r.getAppName(), r.getVersion(), r.getBuildStatus(), appMatch, verMatch);
                        if (appMatch && verMatch) {
                            matched.add(r);
                            entry.updateAppBuildStatus(r.getAppName(), r.getBuildStatus());
                        }
                    }

                    logger.info("[Queue Poll] matched {}/{} apps for group='{}'",
                            matched.size(), entry.getAppNames().size(), entry.getGroupName());
                    for (BuildResult r : matched) {
                        logger.info("[Queue Poll]   matched: app='{}' status='{}' isSuccess={} isFailed={}",
                                r.getAppName(), r.getBuildStatus(),
                                isSuccessStatus(r.getBuildStatus()), isFailedStatus(r.getBuildStatus()));
                    }
                    // 还没匹配到的 app
                    for (String appName : entry.getAppNames()) {
                        boolean found = matched.stream().anyMatch(r -> appName.equals(r.getAppName()));
                        if (!found) {
                            logger.info("[Queue Poll]   no match yet: app='{}'", appName);
                        }
                    }

                    fireEntryStatusChanged(entry);

                    if (checkAllSuccess(entry, matched)) {
                        logger.info("[Queue Poll] ALL SUCCESS for group='{}'", entry.getGroupName());
                        stopTimer(); entry.setStatus(QueueEntry.QueueStatus.SUCCESS);
                        persistState(); fireEntryStatusChanged(entry); executeNext();
                    } else if (checkAnyFailed(entry, matched)) {
                        String failedApp = findFailedApp(entry, matched);
                        logger.info("[Queue Poll] FAILED for group='{}' failedApp='{}'", entry.getGroupName(), failedApp);
                        stopTimer(); entry.setStatus(QueueEntry.QueueStatus.FAILED);
                        persistState(); fireEntryStatusChanged(entry);
                        running = false; fireQueueFailed(entry, failedApp);
                    } else {
                        logger.info("[Queue Poll] still building, waiting next poll for group='{}'", entry.getGroupName());
                    }
                } catch (Exception e) {
                    logger.error("[Queue Poll] error for group='{}'", entry.getGroupName(), e);
                    firePollingError("Polling error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private static boolean isSuccessStatus(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase();
        return t.equals("success") || t.equals("build success");
    }

    private static boolean isFailedStatus(String s) {
        if (s == null) return false;
        String t = s.trim().toLowerCase();
        return t.equals("failure") || t.equals("failed") || t.equals("build fail")
                || t.equals("build failed") || t.equals("aborted");
    }

    private boolean checkAllSuccess(QueueEntry entry, List<BuildResult> matched) {
        if (matched.isEmpty()) return false;
        return entry.getAppNames().stream().allMatch(appName ->
                matched.stream().anyMatch(r -> appName.equals(r.getAppName()) && isSuccessStatus(r.getBuildStatus())));
    }

    private boolean checkAnyFailed(QueueEntry entry, List<BuildResult> matched) {
        return matched.stream().anyMatch(r -> isFailedStatus(r.getBuildStatus()));
    }

    private String findFailedApp(QueueEntry entry, List<BuildResult> matched) {
        for (BuildResult r : matched) {
            if (isFailedStatus(r.getBuildStatus())) return r.getAppName();
        }
        return "unknown";
    }

    private QueueEntry findBuildingEntry() {
        if (currentIndex >= 0 && currentIndex < entries.size()) {
            QueueEntry e = entries.get(currentIndex);
            if (e.getStatus() == QueueEntry.QueueStatus.BUILDING) return e;
        }
        for (QueueEntry e : entries) { if (e.getStatus() == QueueEntry.QueueStatus.BUILDING) return e; }
        return null;
    }

    private void persistState() { QueuePersistence.save(entries, pollingIntervalMs / 1000); }

    private boolean isTerminalStatus(QueueEntry.QueueStatus status) {
        return status == QueueEntry.QueueStatus.SUCCESS || status == QueueEntry.QueueStatus.FAILED || status == QueueEntry.QueueStatus.CANCELLED;
    }

    private void fireEntryStatusChanged(QueueEntry entry) { SwingUtilities.invokeLater(() -> listener.onEntryStatusChanged(entry)); }
    private void fireQueueCompleted(boolean allSuccess) { SwingUtilities.invokeLater(() -> listener.onQueueCompleted(allSuccess)); }
    private void fireQueueFailed(QueueEntry failedEntry, String failedApp) { SwingUtilities.invokeLater(() -> listener.onQueueFailed(failedEntry, failedApp)); }
    private void firePollingError(String errorMessage) { SwingUtilities.invokeLater(() -> listener.onPollingError(errorMessage)); }
}
