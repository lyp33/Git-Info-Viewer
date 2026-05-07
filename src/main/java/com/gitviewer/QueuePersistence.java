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
 * 按租户隔离，每个租户一个独立文件
 * 文件路径：~/.gitviewer/pending_build_queue_{tenant}.json
 */
public class QueuePersistence {

    private static final Logger logger = LoggerFactory.getLogger(QueuePersistence.class);

    private static final String DIR_PATH = System.getProperty("user.home") + "/.gitviewer";

    /** 默认轮询间隔（秒） */
    public static final int DEFAULT_POLLING_INTERVAL = 20;

    /** 兼容旧版单文件路径（迁移用） */
    private static final String LEGACY_FILE_PATH = DIR_PATH + "/pending_build_queue.json";

    /**
     * 获取指定租户的持久化文件路径
     */
    private static String filePath(String tenant) {
        // 清理 tenant 名称中的特殊字符，避免文件名问题
        String safeTenant = tenant.replaceAll("[^a-zA-Z0-9_\\-.]", "_");
        return DIR_PATH + "/pending_build_queue_" + safeTenant + ".json";
    }

    public static class Data {
        public final List<QueueEntry> entries;
        public final int pollingIntervalSeconds;

        public Data(List<QueueEntry> entries, int pollingIntervalSeconds) {
            this.entries = entries;
            this.pollingIntervalSeconds = pollingIntervalSeconds;
        }
    }

    /**
     * 保存指定租户的队列数据
     */
    public static void save(String tenant, List<QueueEntry> entries, int pollingIntervalSeconds) {
        try {
            File file = new File(filePath(tenant));
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            JSONObject root = new JSONObject();
            root.put("tenant", tenant);
            root.put("pollingIntervalSeconds", pollingIntervalSeconds);

            JSONArray arr = new JSONArray();
            for (QueueEntry entry : entries) {
                arr.put(entryToJson(entry));
            }
            root.put("entries", arr);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(root.toString(2));
            }

            logger.debug("Queue persisted for tenant '{}': {} entries", tenant, entries.size());
        } catch (IOException e) {
            logger.error("Failed to save queue for tenant '{}': {}", tenant, e.getMessage());
        }
    }

    /**
     * 兼容旧版无 tenant 参数的 save（供未改造的调用方过渡使用）
     */
    public static void save(List<QueueEntry> entries, int pollingIntervalSeconds) {
        // 从 entries 中提取 tenant
        String tenant = entries.stream()
                .map(QueueEntry::getTenant)
                .filter(t -> t != null && !t.isEmpty())
                .findFirst()
                .orElse("unknown");
        save(tenant, entries, pollingIntervalSeconds);
    }

    /**
     * 加载指定租户的队列数据
     */
    public static Data load(String tenant) {
        File file = new File(filePath(tenant));
        if (!file.exists()) {
            // 尝试从旧版单文件迁移
            return tryLoadLegacy(tenant);
        }
        return readFile(file);
    }

    /**
     * 判断指定租户是否有未完成条目
     */
    public static boolean hasUnfinished(String tenant) {
        Data data = load(tenant);
        for (QueueEntry entry : data.entries) {
            QueueEntry.QueueStatus s = entry.getStatus();
            if (s == QueueEntry.QueueStatus.PENDING || s == QueueEntry.QueueStatus.BUILDING) {
                return true;
            }
        }
        return false;
    }

    /**
     * 删除指定租户的持久化文件
     */
    public static void delete(String tenant) {
        File file = new File(filePath(tenant));
        if (file.exists()) {
            if (file.delete()) {
                logger.debug("Queue file deleted for tenant '{}'", tenant);
            } else {
                logger.warn("Failed to delete queue file for tenant '{}'", tenant);
            }
        }
    }

    // ===== 兼容旧版 =====

    /**
     * 尝试从旧版单文件中加载匹配 tenant 的条目
     */
    private static Data tryLoadLegacy(String tenant) {
        File legacyFile = new File(LEGACY_FILE_PATH);
        if (!legacyFile.exists()) {
            return new Data(new ArrayList<>(), DEFAULT_POLLING_INTERVAL);
        }

        Data allData = readFile(legacyFile);
        // 过滤出属于该 tenant 的条目
        List<QueueEntry> tenantEntries = new ArrayList<>();
        for (QueueEntry e : allData.entries) {
            if (tenant.equals(e.getTenant())) {
                tenantEntries.add(e);
            }
        }

        if (!tenantEntries.isEmpty()) {
            logger.info("Migrated {} entries from legacy file for tenant '{}'", tenantEntries.size(), tenant);
            // 迁移：保存到新文件，然后清理旧文件中该 tenant 的数据
            save(tenant, tenantEntries, allData.pollingIntervalSeconds);
        }

        return new Data(tenantEntries, allData.pollingIntervalSeconds);
    }

    /**
     * 兼容旧版无 tenant 参数的方法
     */
    public static boolean hasUnfinished() {
        // 旧版：扫描旧文件
        File legacyFile = new File(LEGACY_FILE_PATH);
        if (legacyFile.exists()) {
            Data data = readFile(legacyFile);
            for (QueueEntry entry : data.entries) {
                QueueEntry.QueueStatus s = entry.getStatus();
                if (s == QueueEntry.QueueStatus.PENDING || s == QueueEntry.QueueStatus.BUILDING) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void delete() {
        File legacyFile = new File(LEGACY_FILE_PATH);
        if (legacyFile.exists()) {
            legacyFile.delete();
        }
    }

    // ===== 内部工具方法 =====

    private static Data readFile(File file) {
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
            return new Data(entries, pollingInterval);
        } catch (Exception e) {
            logger.error("Failed to read queue file: {}", file.getPath(), e);
            return new Data(new ArrayList<>(), DEFAULT_POLLING_INTERVAL);
        }
    }

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

    private static QueueEntry jsonToEntry(JSONObject obj) {
        QueueEntry entry = new QueueEntry();
        entry.setGroupName(obj.optString("groupName", ""));
        entry.setBranch(obj.optString("branch", ""));
        entry.setVersion(obj.optString("version", ""));
        entry.setTenant(obj.optString("tenant", ""));
        entry.setTriggeredAt(obj.optString("triggeredAt", ""));

        String statusStr = obj.optString("status", QueueEntry.QueueStatus.PENDING.name());
        try {
            entry.setStatus(QueueEntry.QueueStatus.valueOf(statusStr));
        } catch (IllegalArgumentException e) {
            entry.setStatus(QueueEntry.QueueStatus.PENDING);
        }

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
