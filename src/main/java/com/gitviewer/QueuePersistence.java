package com.gitviewer;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 队列持久化工具类
 * 负责将 Build Queue 状态读写到本地 JSON 文件
 * 文件路径：~/.gitviewer/pending_build_queue.json
 * JSON 格式：{ "pollingIntervalSeconds": 10, "entries": [...] }
 */
public class QueuePersistence {

    private static final Logger logger = LoggerFactory.getLogger(QueuePersistence.class);

    /** 持久化文件路径 */
    public static final String FILE_PATH =
            System.getProperty("user.home") + "/.gitviewer/pending_build_queue.json";

    /** 默认轮询间隔（秒） */
    public static final int DEFAULT_POLLING_INTERVAL = 20;

    /**
     * load() 的返回值，包含条目列表和轮询间隔
     */
    public static class Data {
        public final List<QueueEntry> entries;
        public final int pollingIntervalSeconds;

        public Data(List<QueueEntry> entries, int pollingIntervalSeconds) {
            this.entries = entries;
            this.pollingIntervalSeconds = pollingIntervalSeconds;
        }
    }

    /**
     * 将队列条目和轮询间隔序列化并覆盖写入持久化文件
     *
     * @param entries                队列条目列表
     * @param pollingIntervalSeconds 轮询间隔（秒）
     */
    public static void save(List<QueueEntry> entries, int pollingIntervalSeconds) {
        try {
            // 确保目录存在
            File file = new File(FILE_PATH);
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 构建顶层 JSON 对象
            JSONObject root = new JSONObject();
            root.put("pollingIntervalSeconds", pollingIntervalSeconds);

            JSONArray arr = new JSONArray();
            for (QueueEntry entry : entries) {
                arr.put(entryToJson(entry));
            }
            root.put("entries", arr);

            // 写入文件
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(root.toString(2));
            }

            logger.debug("Queue persisted: {} entries, pollingInterval={}s", entries.size(), pollingIntervalSeconds);
        } catch (IOException e) {
            logger.error("Failed to save queue persistence file: {}", FILE_PATH, e);
        }
    }

    /**
     * 从持久化文件读取队列数据
     * 文件不存在时返回空列表和默认轮询间隔
     *
     * @return Data 对象（entries + pollingIntervalSeconds）
     */
    public static Data load() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new Data(new ArrayList<>(), DEFAULT_POLLING_INTERVAL);
        }

        try (FileReader reader = new FileReader(file)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = reader.read(buf)) != -1) {
                sb.append(buf, 0, n);
            }

            JSONObject root = new JSONObject(sb.toString());
            int pollingInterval = root.optInt("pollingIntervalSeconds", DEFAULT_POLLING_INTERVAL);

            List<QueueEntry> entries = new ArrayList<>();
            JSONArray arr = root.optJSONArray("entries");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    entries.add(jsonToEntry(arr.getJSONObject(i)));
                }
            }

            logger.debug("Queue loaded: {} entries, pollingInterval={}s", entries.size(), pollingInterval);
            return new Data(entries, pollingInterval);
        } catch (Exception e) {
            logger.error("Failed to load queue persistence file: {}", FILE_PATH, e);
            return new Data(new ArrayList<>(), DEFAULT_POLLING_INTERVAL);
        }
    }

    /**
     * 删除持久化文件
     */
    public static void delete() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                logger.debug("Queue persistence file deleted: {}", FILE_PATH);
            } else {
                logger.warn("Failed to delete queue persistence file: {}", FILE_PATH);
            }
        }
    }

    /**
     * 判断持久化文件中是否存在未完成的条目（PENDING 或 BUILDING）
     *
     * @return true 表示存在未完成条目
     */
    public static boolean hasUnfinished() {
        Data data = load();
        for (QueueEntry entry : data.entries) {
            QueueEntry.QueueStatus s = entry.getStatus();
            if (s == QueueEntry.QueueStatus.PENDING || s == QueueEntry.QueueStatus.BUILDING) {
                return true;
            }
        }
        return false;
    }

    // ===== 私有序列化辅助方法 =====

    /**
     * 将 QueueEntry 序列化为 JSONObject
     */
    private static JSONObject entryToJson(QueueEntry entry) {
        JSONObject obj = new JSONObject();
        obj.put("groupName", entry.getGroupName() != null ? entry.getGroupName() : "");
        obj.put("branch", entry.getBranch() != null ? entry.getBranch() : "");
        obj.put("version", entry.getVersion() != null ? entry.getVersion() : "");
        obj.put("tenant", entry.getTenant() != null ? entry.getTenant() : "");
        obj.put("status", entry.getStatus() != null ? entry.getStatus().name() : QueueEntry.QueueStatus.PENDING.name());
        obj.put("triggeredAt", entry.getTriggeredAt() != null ? entry.getTriggeredAt() : "");

        JSONArray apps = new JSONArray();
        if (entry.getAppNames() != null) {
            for (String app : entry.getAppNames()) {
                apps.put(app);
            }
        }
        obj.put("appNames", apps);

        return obj;
    }

    /**
     * 将 JSONObject 反序列化为 QueueEntry
     */
    private static QueueEntry jsonToEntry(JSONObject obj) {
        QueueEntry entry = new QueueEntry();
        entry.setGroupName(obj.optString("groupName", ""));
        entry.setBranch(obj.optString("branch", ""));
        entry.setVersion(obj.optString("version", ""));
        entry.setTenant(obj.optString("tenant", ""));
        entry.setTriggeredAt(obj.optString("triggeredAt", ""));

        // 解析状态枚举，未知值默认 PENDING
        String statusStr = obj.optString("status", QueueEntry.QueueStatus.PENDING.name());
        try {
            entry.setStatus(QueueEntry.QueueStatus.valueOf(statusStr));
        } catch (IllegalArgumentException e) {
            entry.setStatus(QueueEntry.QueueStatus.PENDING);
        }

        // 解析应用名称列表
        List<String> appNames = new ArrayList<>();
        JSONArray apps = obj.optJSONArray("appNames");
        if (apps != null) {
            for (int i = 0; i < apps.length(); i++) {
                appNames.add(apps.getString(i));
            }
        }
        entry.setAppNames(appNames);

        return entry;
    }
}
