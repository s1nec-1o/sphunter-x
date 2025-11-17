package com.sheep.sphunter.fingerprint.device;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.os.Process;
import android.util.Log;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MemoryCollector {
    private static final String TAG = "MemoryCollector";
    private Context context;

    public MemoryCollector(Context context) {
        this.context = context;
    }

    /**
     * 获取内存和存储信息
     * @return JSON 格式的内存存储信息
     */
    public String getMemoryInfo() {
        try {
            JSONObject memoryJson = new JSONObject();
            
            // A. 收集 RAM 信息
            collectRAMInfo(memoryJson);
            
            // B. 收集 ROM 信息
            collectROMInfo(memoryJson);
            
            return memoryJson.toString();
        } catch (Exception e) {
            Log.e(TAG, "获取内存信息失败", e);
            return "{}";
        }
    }

    /**
     * A. 收集 RAM (运行内存) 信息
     */
    private void collectRAMInfo(JSONObject memoryJson) {
        try {
            // 1. 获取服务
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            
            if (activityManager != null) {
                // 2. 准备容器
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                
                // 3. 填充数据
                activityManager.getMemoryInfo(memoryInfo);
                
                // 4. 读取字段并格式化
                long totalMem = memoryInfo.totalMem;        // 总内存 (byte)
                long availMem = memoryInfo.availMem;        // 可用内存 (byte)
                long usedMem = totalMem - availMem;         // 已使用内存
                
                // 存储原始值 (byte)
                memoryJson.put("ram_total_bytes", totalMem);
                memoryJson.put("ram_available_bytes", availMem);
                memoryJson.put("ram_used_bytes", usedMem);
                
                // 存储格式化值 (GB/MB)
                memoryJson.put("ram_total", formatBytes(totalMem));
                memoryJson.put("ram_available", formatBytes(availMem));
                memoryJson.put("ram_used", formatBytes(usedMem));
                
                // 内存使用率
                double usagePercent = (double) usedMem / totalMem * 100;
                memoryJson.put("ram_usage_percent", String.format("%.2f%%", usagePercent));
                
                // 低内存标志
                memoryJson.put("ram_low_memory", memoryInfo.lowMemory);
                memoryJson.put("ram_threshold", formatBytes(memoryInfo.threshold));
                memoryJson.put("ram_threshold_bytes", memoryInfo.threshold);
                
                // 5. 收集 hidden 字段（通过反射）
                collectHiddenMemoryFields(memoryInfo, memoryJson);
                
                // 6. 收集 hidden 方法（通过反射）
                collectHiddenMemoryMethods(activityManager, memoryJson);
                
                Log.d(TAG, "RAM 总内存: " + formatBytes(totalMem) + 
                          ", 可用: " + formatBytes(availMem) + 
                          ", 使用率: " + String.format("%.2f%%", usagePercent));
            }
        } catch (Exception e) {
            Log.e(TAG, "收集 RAM 信息失败", e);
        }
    }

    /**
     * 通过反射收集 MemoryInfo 的 hidden 字段
     */
    private void collectHiddenMemoryFields(ActivityManager.MemoryInfo memoryInfo, JSONObject memoryJson) {
        try {
            // 1. hiddenAppThreshold - 隐藏应用阈值
            try {
                Field hiddenAppThresholdField = ActivityManager.MemoryInfo.class.getDeclaredField("hiddenAppThreshold");
                hiddenAppThresholdField.setAccessible(true);
                long hiddenAppThreshold = hiddenAppThresholdField.getLong(memoryInfo);
                memoryJson.put("ram_hidden_app_threshold_bytes", hiddenAppThreshold);
                memoryJson.put("ram_hidden_app_threshold", formatBytes(hiddenAppThreshold));
                Log.d(TAG, "hiddenAppThreshold: " + formatBytes(hiddenAppThreshold));
            } catch (NoSuchFieldException e) {
                Log.w(TAG, "hiddenAppThreshold 字段不存在（可能是 API 版本问题）");
                memoryJson.put("ram_hidden_app_threshold", "N/A");
            }
            
            // 2. secondaryServerThreshold - 二级服务器阈值
            try {
                Field secondaryServerThresholdField = ActivityManager.MemoryInfo.class.getDeclaredField("secondaryServerThreshold");
                secondaryServerThresholdField.setAccessible(true);
                long secondaryServerThreshold = secondaryServerThresholdField.getLong(memoryInfo);
                memoryJson.put("ram_secondary_server_threshold_bytes", secondaryServerThreshold);
                memoryJson.put("ram_secondary_server_threshold", formatBytes(secondaryServerThreshold));
                Log.d(TAG, "secondaryServerThreshold: " + formatBytes(secondaryServerThreshold));
            } catch (NoSuchFieldException e) {
                Log.w(TAG, "secondaryServerThreshold 字段不存在（可能是 API 版本问题）");
                memoryJson.put("ram_secondary_server_threshold", "N/A");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "收集 hidden 字段失败", e);
        }
    }

    /**
     * 通过反射收集 ActivityManager 的 hidden 方法信息
     */
    private void collectHiddenMemoryMethods(ActivityManager activityManager, JSONObject memoryJson) {
        try {
            // 1. getMemoryClass() - 获取应用可用的标准内存大小（MB）
            try {
                Method getMemoryClassMethod = ActivityManager.class.getMethod("getMemoryClass");
                int memoryClass = (int) getMemoryClassMethod.invoke(activityManager);
                memoryJson.put("ram_memory_class_mb", memoryClass);
                memoryJson.put("ram_memory_class", memoryClass + " MB");
                Log.d(TAG, "Memory Class: " + memoryClass + " MB");
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "getMemoryClass 方法不存在");
                memoryJson.put("ram_memory_class", "N/A");
            }
            
            // 2. getLargeMemoryClass() - 获取应用可用的大内存大小（MB）
            // 需要在 manifest 中设置 android:largeHeap="true"
            try {
                Method getLargeMemoryClassMethod = ActivityManager.class.getMethod("getLargeMemoryClass");
                int largeMemoryClass = (int) getLargeMemoryClassMethod.invoke(activityManager);
                memoryJson.put("ram_large_memory_class_mb", largeMemoryClass);
                memoryJson.put("ram_large_memory_class", largeMemoryClass + " MB");
                Log.d(TAG, "Large Memory Class: " + largeMemoryClass + " MB");
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "getLargeMemoryClass 方法不存在");
                memoryJson.put("ram_large_memory_class", "N/A");
            }
            
            // 3. getMemoryInfo(int uid) - 获取特定 UID 的内存信息（当前应用）
            try {
                int currentUid = Process.myUid();
                memoryJson.put("app_uid", currentUid);
                
                // 尝试调用 getMemoryInfo(int uid) - 这是一个 hidden API
                Method getMemoryInfoForUidMethod = ActivityManager.class.getDeclaredMethod("getMemoryInfo", int.class, ActivityManager.MemoryInfo.class);
                getMemoryInfoForUidMethod.setAccessible(true);
                
                ActivityManager.MemoryInfo uidMemoryInfo = new ActivityManager.MemoryInfo();
                getMemoryInfoForUidMethod.invoke(activityManager, currentUid, uidMemoryInfo);
                
                memoryJson.put("app_memory_total_bytes", uidMemoryInfo.totalMem);
                memoryJson.put("app_memory_available_bytes", uidMemoryInfo.availMem);
                memoryJson.put("app_memory_total", formatBytes(uidMemoryInfo.totalMem));
                memoryJson.put("app_memory_available", formatBytes(uidMemoryInfo.availMem));
                
                Log.d(TAG, "App UID " + currentUid + " Memory: " + formatBytes(uidMemoryInfo.totalMem));
            } catch (NoSuchMethodException e) {
                Log.w(TAG, "getMemoryInfo(int uid) 方法不存在");
            } catch (Exception e) {
                Log.w(TAG, "调用 getMemoryInfo(int uid) 失败: " + e.getMessage());
            }
            
            // 4. 获取 Runtime 内存信息（Java 堆内存）
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();      // 应用最大可用内存
            long totalMemory = runtime.totalMemory();  // 当前已分配内存
            long freeMemory = runtime.freeMemory();    // 当前空闲内存
            long usedMemory = totalMemory - freeMemory; // 当前已使用内存
            
            memoryJson.put("app_heap_max_bytes", maxMemory);
            memoryJson.put("app_heap_total_bytes", totalMemory);
            memoryJson.put("app_heap_free_bytes", freeMemory);
            memoryJson.put("app_heap_used_bytes", usedMemory);
            
            memoryJson.put("app_heap_max", formatBytes(maxMemory));
            memoryJson.put("app_heap_total", formatBytes(totalMemory));
            memoryJson.put("app_heap_free", formatBytes(freeMemory));
            memoryJson.put("app_heap_used", formatBytes(usedMemory));
            
            double heapUsagePercent = (double) usedMemory / maxMemory * 100;
            memoryJson.put("app_heap_usage_percent", String.format("%.2f%%", heapUsagePercent));
            
            Log.d(TAG, "Java Heap: 最大=" + formatBytes(maxMemory) + 
                      ", 已用=" + formatBytes(usedMemory) + 
                      ", 使用率=" + String.format("%.2f%%", heapUsagePercent));
            
        } catch (Exception e) {
            Log.e(TAG, "收集 hidden 方法信息失败", e);
        }
    }

    /**
     * B. 收集 ROM (存储空间) 信息
     */
    private void collectROMInfo(JSONObject memoryJson) {
        try {
            // 1. 定位路径 - 内部存储 /data 目录
            String dataPath = Environment.getDataDirectory().getPath();
            
            // 2. 获取状态
            StatFs dataStat = new StatFs(dataPath);
            
            // 3. 计算大小
            long dataBlockSize = dataStat.getBlockSizeLong();          // 块大小
            long dataTotalBlocks = dataStat.getBlockCountLong();       // 总块数
            long dataAvailableBlocks = dataStat.getAvailableBlocksLong(); // 可用块数
            
            long dataTotalSize = dataTotalBlocks * dataBlockSize;      // 总空间
            long dataAvailableSize = dataAvailableBlocks * dataBlockSize; // 可用空间
            long dataUsedSize = dataTotalSize - dataAvailableSize;     // 已使用空间
            
            // 内部存储信息
            memoryJson.put("internal_storage_total_bytes", dataTotalSize);
            memoryJson.put("internal_storage_available_bytes", dataAvailableSize);
            memoryJson.put("internal_storage_used_bytes", dataUsedSize);
            
            memoryJson.put("internal_storage_total", formatBytes(dataTotalSize));
            memoryJson.put("internal_storage_available", formatBytes(dataAvailableSize));
            memoryJson.put("internal_storage_used", formatBytes(dataUsedSize));
            
            double storageUsagePercent = (double) dataUsedSize / dataTotalSize * 100;
            memoryJson.put("internal_storage_usage_percent", String.format("%.2f%%", storageUsagePercent));
            
            Log.d(TAG, "内部存储 总空间: " + formatBytes(dataTotalSize) + 
                      ", 可用: " + formatBytes(dataAvailableSize) + 
                      ", 使用率: " + String.format("%.2f%%", storageUsagePercent));
            
            // 如果外部存储可用，也收集外部存储信息
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String externalPath = Environment.getExternalStorageDirectory().getPath();
                StatFs externalStat = new StatFs(externalPath);
                
                long externalBlockSize = externalStat.getBlockSizeLong();
                long externalTotalBlocks = externalStat.getBlockCountLong();
                long externalAvailableBlocks = externalStat.getAvailableBlocksLong();
                
                long externalTotalSize = externalTotalBlocks * externalBlockSize;
                long externalAvailableSize = externalAvailableBlocks * externalBlockSize;
                long externalUsedSize = externalTotalSize - externalAvailableSize;
                
                // 外部存储信息
                memoryJson.put("external_storage_total_bytes", externalTotalSize);
                memoryJson.put("external_storage_available_bytes", externalAvailableSize);
                memoryJson.put("external_storage_used_bytes", externalUsedSize);
                
                memoryJson.put("external_storage_total", formatBytes(externalTotalSize));
                memoryJson.put("external_storage_available", formatBytes(externalAvailableSize));
                memoryJson.put("external_storage_used", formatBytes(externalUsedSize));
                
                double externalUsagePercent = (double) externalUsedSize / externalTotalSize * 100;
                memoryJson.put("external_storage_usage_percent", String.format("%.2f%%", externalUsagePercent));
                
                memoryJson.put("external_storage_state", "mounted");
                
                Log.d(TAG, "外部存储 总空间: " + formatBytes(externalTotalSize) + 
                          ", 可用: " + formatBytes(externalAvailableSize));
            } else {
                memoryJson.put("external_storage_state", Environment.getExternalStorageState());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "收集 ROM 信息失败", e);
        }
    }

    /**
     * 格式化字节大小为可读格式
     * @param bytes 字节数
     * @return 格式化后的字符串 (如 "2.5 GB", "512 MB")
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        
        double value = bytes / Math.pow(1024, exp);
        return String.format("%.2f %sB", value, unit);
    }

    /**
     * 获取 RAM 信息的 JSON 对象
     * @return RAM 信息
     */
    public JSONObject getRAMInfoJson() {
        try {
            JSONObject ramJson = new JSONObject();
            collectRAMInfo(ramJson);
            return ramJson;
        } catch (Exception e) {
            Log.e(TAG, "获取 RAM 信息失败", e);
            return new JSONObject();
        }
    }

    /**
     * 获取 ROM 信息的 JSON 对象
     * @return ROM 信息
     */
    public JSONObject getROMInfoJson() {
        try {
            JSONObject romJson = new JSONObject();
            collectROMInfo(romJson);
            return romJson;
        } catch (Exception e) {
            Log.e(TAG, "获取 ROM 信息失败", e);
            return new JSONObject();
        }
    }
}
