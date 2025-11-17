package com.sheep.sphunter.fingerprint;

import android.util.Log;

import androidx.annotation.NonNull;

import com.sheep.sphunter.model.FingerprintResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 指纹数据清洗器
 * 负责对采集的指纹信息进行去噪、归一化和结构化处理
 */
public class FingerprintDataCleaner {
    private static final String TAG = "FingerprintDataCleaner";

    /**
     * 清洗并结构化指纹数据
     * @param rawResult 原始指纹结果
     * @return 清洗后的结构化 JSON 数据
     */
    @NonNull
    public static JSONObject cleanFingerprint(@NonNull FingerprintResult rawResult) {
        try {
            JSONObject cleaned = new JSONObject();
            
            // 1. Identity - 身份标识信息
            JSONObject identity = new JSONObject();
            identity.put("android_id", cleanString(rawResult.getAndroidId()));
            identity.put("serial_number", cleanString(rawResult.getSerialNumber()));
            identity.put("bluetooth_address", cleanString(rawResult.getBluetoothAddress()));
            // 提取 DRM ID（最重要的设备标识符，刷机后不变）
            identity.put("drm_device_id", extractDrmDeviceId(rawResult.getDrmInfo()));
            cleaned.put("identity", identity);
            
            // 2. Hardware - 硬件信息
            JSONObject hardware = new JSONObject();
            hardware.put("gpu", cleanGpuInfo(rawResult.getglendererInfo()));
            hardware.put("memory", cleanMemoryInfo(rawResult.getMemoryInfo()));
            hardware.put("battery", cleanBatteryInfo(rawResult.getBatteryInfo()));
            cleaned.put("hardware", hardware);
            
            // 3. System - 系统信息（结构化 Build 信息）
            JSONObject system = new JSONObject();
            system.put("build_properties", cleanBuildInfo(rawResult.getBuildInfo()));
            system.put("phone_info", cleanString(rawResult.getPhoneInfo()));
            system.put("settings", cleanString(rawResult.getSettings()));
            cleaned.put("system", system);
            
            // 4. Media - 多媒体信息
            JSONObject media = new JSONObject();
            media.put("volume_info", cleanString(rawResult.getVolumeInfo()));
            media.put("drm_info", cleanString(rawResult.getDrmInfo()));
            cleaned.put("media", media);
            
            // 5. Sensors - 传感器信息（结构化为数组）
            JSONObject sensors = new JSONObject();
            sensors.put("sensor_list", cleanSensorInfo(rawResult.getSensorInfo()));
            cleaned.put("sensors", sensors);
            
            // 6. Account - 账户信息
            JSONObject account = new JSONObject();
            account.put("account_info", cleanString(rawResult.getAccountInfo()));
            cleaned.put("account", account);
            
            // 7. Native - Native 层指纹信息（清洗后的结构化数据）
            if (rawResult.getNativeBuildInfo() != null && !rawResult.getNativeBuildInfo().trim().isEmpty()) {
                JSONObject nativeFingerprint = NativeFileDataCleaner.cleanNativeFingerprint(rawResult.getNativeBuildInfo());
                if (nativeFingerprint.length() > 0) {
                    cleaned.put("native", nativeFingerprint);
                }
            }
            
            return cleaned;
            
        } catch (JSONException e) {
            Log.e(TAG, "清洗指纹数据失败", e);
            return new JSONObject();
        }
    }

    /**
     * 提取 DRM Device ID（最重要的设备标识符）
     * @param raw 原始 DRM 信息字符串
     * @return DRM Device ID，失败返回 null
     */
    private static String extractDrmDeviceId(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 格式: "MediaDrm Device Unique ID: <hex_string>\nLength: <bytes> bytes"
            String pattern = "MediaDrm Device Unique ID:\\s*([a-fA-F0-9]+)";
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(raw);
            
            if (m.find()) {
                String drmId = m.group(1).trim();
                // 验证是否为有效的十六进制字符串
                if (drmId.length() > 0 && drmId.matches("[a-fA-F0-9]+")) {
                    return drmId.toLowerCase(); // 统一转为小写
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "提取 DRM Device ID 失败", e);
        }
        
        return null;
    }

    /**
     * 清洗字符串 - 去噪和归一化
     * @param raw 原始字符串
     * @return 清洗后的字符串
     */
    private static String cleanString(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        
        String cleaned = raw.trim();
        
        // 去噪：检测错误信息
        if (isErrorString(cleaned)) {
            return null;
        }
        
        return cleaned;
    }

    /**
     * 判断字符串是否为错误信息
     * @param str 待检测字符串
     * @return true 表示是错误信息
     */
    private static boolean isErrorString(String str) {
        if (str == null) {
            return true;
        }
        
        String lower = str.toLowerCase();
        
        // 常见的错误标记
        return lower.contains("securityexception") ||
               lower.contains("android 10+ restricted") ||
               lower.contains("permission denied") ||
               lower.contains("error:") ||
               lower.contains("not available") ||
               lower.contains("unknown") ||
               lower.equals("null") ||
               lower.equals("none") ||
               lower.equals("-1") ||
               lower.equals("unavailable");
    }

    /**
     * 清洗 GPU 信息 - 提取显卡型号，去除驱动版本号
     * @param raw 原始 GPU 信息 (格式: "Renderer: Mali-G78 | Vendor: ARM | Version: OpenGL ES 3.2 v1.r32p1-01eac0.ab5309d622697df1444e83f8b7c2e5f7")
     * @return 清洗后的 GPU 信息对象
     */
    private static JSONObject cleanGpuInfo(String raw) {
        JSONObject gpu = new JSONObject();
        
        try {
            if (raw == null || isErrorString(raw)) {
                gpu.put("renderer", null);
                gpu.put("vendor", null);
                return gpu;
            }
            
            // 提取 Renderer (显卡型号)
            String renderer = extractValue(raw, "Renderer:\\s*([^|]+)");
            if (renderer != null) {
                // 只保留型号，去除驱动版本号
                // 例如: "Mali-G78" 保留，"Mali-G78 MP12 r32p1" 只保留 "Mali-G78"
                renderer = extractGpuModel(renderer);
            }
            gpu.put("renderer", cleanString(renderer));
            
            // 提取 Vendor (厂商)
            String vendor = extractValue(raw, "Vendor:\\s*([^|]+)");
            gpu.put("vendor", cleanString(vendor));
            
        } catch (JSONException e) {
            Log.e(TAG, "清洗 GPU 信息失败", e);
        }
        
        return gpu;
    }

    /**
     * 提取 GPU 型号（去除驱动版本号）
     * @param fullRenderer 完整的渲染器字符串
     * @return 纯净的 GPU 型号
     */
    private static String extractGpuModel(String fullRenderer) {
        if (fullRenderer == null) {
            return null;
        }
        
        // 常见模式:
        // "Mali-G78 MP12 r32p1" -> "Mali-G78"
        // "Adreno (TM) 650" -> "Adreno 650"
        // "PowerVR Rogue GE8320" -> "PowerVR GE8320"
        
        String cleaned = fullRenderer.trim();
        
        // Mali 系列: 保留 Mali-Gxx 或 Mali-Txx
        Pattern maliPattern = Pattern.compile("(Mali-[GT]\\d+)");
        Matcher maliMatcher = maliPattern.matcher(cleaned);
        if (maliMatcher.find()) {
            return maliMatcher.group(1);
        }
        
        // Adreno 系列: 保留 Adreno + 数字
        Pattern adrenoPattern = Pattern.compile("Adreno\\s*(?:\\(TM\\))?\\s*(\\d+)");
        Matcher adrenoMatcher = adrenoPattern.matcher(cleaned);
        if (adrenoMatcher.find()) {
            return "Adreno " + adrenoMatcher.group(1);
        }
        
        // PowerVR 系列: 保留 PowerVR + 型号
        Pattern powervPattern = Pattern.compile("PowerVR\\s+(?:\\w+\\s+)?(\\w+\\d+)");
        Matcher powervMatcher = powervPattern.matcher(cleaned);
        if (powervMatcher.find()) {
            return "PowerVR " + powervMatcher.group(1);
        }
        
        // 如果没有匹配到特定模式，返回第一个单词组合
        String[] parts = cleaned.split("\\s+");
        if (parts.length >= 2) {
            return parts[0] + " " + parts[1];
        }
        
        return cleaned;
    }

    /**
     * 清洗内存信息 - 转换为 GB 并归一化，包含 hidden API 字段
     * @param raw 原始内存信息 JSON 字符串
     * @return 清洗后的内存信息对象
     */
    private static JSONObject cleanMemoryInfo(String raw) {
        JSONObject memory = new JSONObject();
        
        try {
            if (raw == null || raw.trim().isEmpty() || raw.equals("{}")) {
                Log.w(TAG, "内存信息为空或无效");
                return memory;
            }
            
            // 解析原始 JSON 字符串
            JSONObject rawMemory = new JSONObject(raw);
            
            // ==================== 1. RAM 基础信息 ====================
            JSONObject ram = new JSONObject();
            long ramTotal = rawMemory.optLong("ram_total_bytes", -1);
            long ramAvailable = rawMemory.optLong("ram_available_bytes", -1);
            long ramUsed = rawMemory.optLong("ram_used_bytes", -1);
            
            if (ramTotal > 0) {
                // 转换为 GB（保留两位小数）
                ram.put("total_gb", bytesToGB(ramTotal));
                ram.put("available_gb", bytesToGB(ramAvailable));
                ram.put("used_gb", bytesToGB(ramUsed));
                ram.put("usage_percent", parsePercentage(rawMemory.optString("ram_usage_percent", null)));
                
                // 低内存标志
                ram.put("low_memory", rawMemory.optBoolean("ram_low_memory", false));
                ram.put("threshold_gb", bytesToGB(rawMemory.optLong("ram_threshold_bytes", -1)));
                
                // Hidden 字段 - 隐藏应用阈值
                String hiddenAppThreshold = rawMemory.optString("ram_hidden_app_threshold", "N/A");
                if (!hiddenAppThreshold.equals("N/A")) {
                    long hiddenAppThresholdBytes = rawMemory.optLong("ram_hidden_app_threshold_bytes", -1);
                    if (hiddenAppThresholdBytes > 0) {
                        ram.put("hidden_app_threshold_gb", bytesToGB(hiddenAppThresholdBytes));
                    }
                }
                
                // Hidden 字段 - 二级服务器阈值
                String secondaryServerThreshold = rawMemory.optString("ram_secondary_server_threshold", "N/A");
                if (!secondaryServerThreshold.equals("N/A")) {
                    long secondaryServerThresholdBytes = rawMemory.optLong("ram_secondary_server_threshold_bytes", -1);
                    if (secondaryServerThresholdBytes > 0) {
                        ram.put("secondary_server_threshold_gb", bytesToGB(secondaryServerThresholdBytes));
                    }
                }
                
                memory.put("ram", ram);
            } else {
                Log.w(TAG, "RAM 信息无效: ramTotal = " + ramTotal);
            }
            
            // ==================== 2. Memory Class 信息 ====================
            JSONObject memoryClass = new JSONObject();
            
            String memoryClassStr = rawMemory.optString("ram_memory_class", null);
            if (memoryClassStr != null && !memoryClassStr.equals("N/A")) {
                int memoryClassMb = rawMemory.optInt("ram_memory_class_mb", -1);
                if (memoryClassMb > 0) {
                    memoryClass.put("standard_mb", memoryClassMb);
                }
            }
            
            String largeMemoryClassStr = rawMemory.optString("ram_large_memory_class", null);
            if (largeMemoryClassStr != null && !largeMemoryClassStr.equals("N/A")) {
                int largeMemoryClassMb = rawMemory.optInt("ram_large_memory_class_mb", -1);
                if (largeMemoryClassMb > 0) {
                    memoryClass.put("large_mb", largeMemoryClassMb);
                }
            }
            
            if (memoryClass.length() > 0) {
                memory.put("memory_class", memoryClass);
            }
            
            // ==================== 3. 内部存储信息 ====================
            JSONObject internalStorage = new JSONObject();
            long internalTotal = rawMemory.optLong("internal_storage_total_bytes", -1);
            long internalAvailable = rawMemory.optLong("internal_storage_available_bytes", -1);
            long internalUsed = rawMemory.optLong("internal_storage_used_bytes", -1);
            
            if (internalTotal > 0) {
                // 转换为 GB
                internalStorage.put("total_gb", bytesToGB(internalTotal));
                internalStorage.put("available_gb", bytesToGB(internalAvailable));
                internalStorage.put("used_gb", bytesToGB(internalUsed));
                internalStorage.put("usage_percent", parsePercentage(rawMemory.optString("internal_storage_usage_percent", null)));
                
                memory.put("internal_storage", internalStorage);
            }
            
            // ==================== 4. 外部存储信息 ====================
            long externalTotal = rawMemory.optLong("external_storage_total_bytes", -1);
            String externalState = rawMemory.optString("external_storage_state", null);
            
            if (externalTotal > 0) {
                JSONObject externalStorage = new JSONObject();
                long externalAvailable = rawMemory.optLong("external_storage_available_bytes", -1);
                long externalUsed = rawMemory.optLong("external_storage_used_bytes", -1);
                
                externalStorage.put("total_gb", bytesToGB(externalTotal));
                externalStorage.put("available_gb", bytesToGB(externalAvailable));
                externalStorage.put("used_gb", bytesToGB(externalUsed));
                externalStorage.put("usage_percent", parsePercentage(rawMemory.optString("external_storage_usage_percent", null)));
                
                if (externalState != null && !externalState.equals("mounted")) {
                    externalStorage.put("state", externalState);
                }
                
                memory.put("external_storage", externalStorage);
            } else if (externalState != null) {
                // 外部存储不可用，记录状态
                JSONObject externalStorage = new JSONObject();
                externalStorage.put("state", externalState);
                memory.put("external_storage", externalStorage);
            }
            
            // ==================== 5. 应用堆内存信息（Java Heap）====================
            JSONObject appHeap = new JSONObject();
            
            long appHeapMax = rawMemory.optLong("app_heap_max_bytes", -1);
            long appHeapTotal = rawMemory.optLong("app_heap_total_bytes", -1);
            long appHeapFree = rawMemory.optLong("app_heap_free_bytes", -1);
            long appHeapUsed = rawMemory.optLong("app_heap_used_bytes", -1);
            
            if (appHeapMax > 0) {
                // 转换为 MB（堆内存通常用 MB 表示更直观）
                appHeap.put("max_mb", bytesToMB(appHeapMax));
                appHeap.put("allocated_mb", bytesToMB(appHeapTotal));
                appHeap.put("free_mb", bytesToMB(appHeapFree));
                appHeap.put("used_mb", bytesToMB(appHeapUsed));
                appHeap.put("usage_percent", parsePercentage(rawMemory.optString("app_heap_usage_percent", null)));
                
                memory.put("app_heap", appHeap);
            }
            
            // ==================== 6. 应用级别信息 ====================
            JSONObject appInfo = new JSONObject();
            
            // 应用 UID
            int appUid = rawMemory.optInt("app_uid", -1);
            if (appUid > 0) {
                appInfo.put("uid", appUid);
            }
            
            // 应用特定内存信息（如果 getMemoryInfo(int uid) 成功）
            long appMemoryTotal = rawMemory.optLong("app_memory_total_bytes", -1);
            long appMemoryAvailable = rawMemory.optLong("app_memory_available_bytes", -1);
            
            if (appMemoryTotal > 0) {
                appInfo.put("memory_total_gb", bytesToGB(appMemoryTotal));
                appInfo.put("memory_available_gb", bytesToGB(appMemoryAvailable));
            }
            
            if (appInfo.length() > 0) {
                memory.put("app_info", appInfo);
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "清洗内存信息失败: " + e.getMessage(), e);
        }
        
        return memory;
    }

    /**
     * 字节转 MB (保留两位小数)
     * @param bytes 字节数
     * @return MB 字符串
     */
    private static String bytesToMB(long bytes) {
        if (bytes <= 0) {
            return null;
        }
        double mb = bytes / (1024.0 * 1024.0);
        return String.format("%.2f", mb);
    }

    /**
     * 清洗电池信息
     * @param raw 原始电池信息字符串
     * @return 清洗后的电池信息对象
     */
    private static JSONObject cleanBatteryInfo(String raw) {
        JSONObject battery = new JSONObject();
        
        try {
            if (raw == null || isErrorString(raw)) {
                return battery;
            }
            
            // 提取电量百分比
            String levelStr = extractValue(raw, "Battery Level:\\s*([\\d.]+)%");
            if (levelStr != null) {
                battery.put("level_percent", Double.parseDouble(levelStr));
            }
            
            // 提取充电状态
            String status = extractValue(raw, "Status:\\s*([^\n]+)");
            battery.put("status", cleanString(status));
            
            // 提取插拔状态
            String plugged = extractValue(raw, "Plugged:\\s*([^\n]+)");
            battery.put("plugged", cleanString(plugged));
            
            // 提取健康状态
            String health = extractValue(raw, "Health:\\s*([^\n]+)");
            battery.put("health", cleanString(health));
            
            // 提取电压 (mV -> V)
            String voltageStr = extractValue(raw, "Voltage:\\s*(\\d+)\\s*mV");
            if (voltageStr != null) {
                double voltage = Double.parseDouble(voltageStr) / 1000.0;
                battery.put("voltage_v", String.format("%.2f", voltage));
            }
            
            // 提取温度
            String tempStr = extractValue(raw, "Temperature:\\s*([\\d.]+)°C");
            if (tempStr != null) {
                battery.put("temperature_celsius", Double.parseDouble(tempStr));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "清洗电池信息失败", e);
        }
        
        return battery;
    }

    /**
     * 字节转 GB (保留两位小数)
     * @param bytes 字节数
     * @return GB 字符串
     */
    private static String bytesToGB(long bytes) {
        if (bytes <= 0) {
            return null;
        }
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);
        return String.format("%.2f", gb);
    }

    /**
     * 解析百分比字符串
     * @param percentStr 百分比字符串 (如 "45.67%")
     * @return 数值 (如 45.67)
     */
    private static Double parsePercentage(String percentStr) {
        if (percentStr == null || percentStr.trim().isEmpty()) {
            return null;
        }
        
        try {
            String numStr = percentStr.replace("%", "").trim();
            return Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 使用正则表达式提取值
     * @param source 源字符串
     * @param pattern 正则表达式模式
     * @return 提取的值，如果未找到返回 null
     */
    private static String extractValue(String source, String pattern) {
        if (source == null) {
            return null;
        }
        
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(source);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (Exception e) {
            Log.e(TAG, "正则提取失败: " + pattern, e);
        }
        
        return null;
    }

    /**
     * 清洗传感器信息 - 结构化为 JSON 数组
     * @param raw 原始传感器信息字符串（多行文本）
     * @return 清洗后的传感器数组
     */
    private static org.json.JSONArray cleanSensorInfo(String raw) {
        org.json.JSONArray sensorArray = new org.json.JSONArray();
        
        try {
            if (raw == null || isErrorString(raw)) {
                return sensorArray;
            }
            
            // 按行分割
            String[] lines = raw.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || !line.startsWith("Sensor:")) {
                    continue;
                }
                
                // 解析传感器信息
                // 格式示例: "Sensor: {Sensor name=\"Accelerometer\", vendor=\"Google\", version=1, type=1, ...}"
                JSONObject sensor = parseSensorLine(line);
                if (sensor != null && sensor.length() > 0) {
                    sensorArray.put(sensor);
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "清洗传感器信息失败", e);
        }
        
        return sensorArray;
    }

    /**
     * 解析单行传感器信息
     * @param line 传感器信息行
     * @return 传感器 JSON 对象
     */
    private static JSONObject parseSensorLine(String line) {
        JSONObject sensor = new JSONObject();
        
        try {
            // 移除 "Sensor: {Sensor " 前缀和结尾的 "}"
            String content = line.substring(line.indexOf("{Sensor") + 8);
            if (content.endsWith("}")) {
                content = content.substring(0, content.length() - 1);
            }
            
            // 提取关键字段：name, vendor, type, version, maxRange, power
            String name = extractKeyValue(content, "name");
            String vendor = extractKeyValue(content, "vendor");
            String type = extractKeyValue(content, "type");
            String version = extractKeyValue(content, "version");
            String maxRange = extractKeyValue(content, "maxRange");
            String power = extractKeyValue(content, "power");
            
            if (name != null) sensor.put("name", name);
            if (vendor != null) sensor.put("vendor", vendor);
            if (type != null) sensor.put("type", parseInteger(type));
            if (version != null) sensor.put("version", parseInteger(version));
            if (maxRange != null) sensor.put("max_range", parseDouble(maxRange));
            if (power != null) sensor.put("power", parseDouble(power));
            
        } catch (Exception e) {
            Log.e(TAG, "解析传感器行失败: " + line, e);
        }
        
        return sensor;
    }

    /**
     * 从传感器字符串中提取键值对
     * @param content 内容字符串
     * @param key 键名
     * @return 值
     */
    private static String extractKeyValue(String content, String key) {
        try {
            Pattern pattern = Pattern.compile(key + "=\"([^\"]+)\"|" + key + "=([^,}\\s]+)");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String value = matcher.group(1);
                if (value == null) {
                    value = matcher.group(2);
                }
                return value != null ? value.trim() : null;
            }
        } catch (Exception e) {
            Log.e(TAG, "提取键值失败: " + key, e);
        }
        return null;
    }

    /**
     * 清洗 Build 信息 - 结构化为键值对对象
     * @param raw 原始 Build 信息字符串（多行 key = value 格式）
     * @return 清洗后的 Build 信息对象
     */
    private static JSONObject cleanBuildInfo(String raw) {
        JSONObject buildInfo = new JSONObject();
        
        try {
            if (raw == null || raw.trim().isEmpty()) {
                return buildInfo;
            }
            
            // 核心字段分类
            JSONObject security = new JSONObject();
            JSONObject usb = new JSONObject();
            JSONObject version = new JSONObject();
            JSONObject fingerprints = new JSONObject();
            JSONObject buildIds = new JSONObject();
            JSONObject dates = new JSONObject();
            JSONObject other = new JSONObject();
            
            // 按行解析
            String[] lines = raw.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                
                // 跳过分隔符行和空行
                if (line.isEmpty() || line.startsWith("===") || line.endsWith("===")) {
                    continue;
                }
                
                // 解析 key = value
                int equalIndex = line.indexOf(" = ");
                if (equalIndex == -1) {
                    continue;
                }
                
                String key = line.substring(0, equalIndex).trim();
                String value = line.substring(equalIndex + 3).trim();
                
                // 过滤无效值
                if (value.equals("null") || value.contains("SecurityException")) {
                    value = null;
                }
                
                // 分类存储
                if (key.contains("usb")) {
                    usb.put(key, value);
                } else if (key.contains("secure") || key.contains("debuggable") || 
                           key.contains("adbd") || key.contains("unlock") || 
                           key.contains("flash.locked")) {
                    security.put(key, value);
                } else if (key.contains("fingerprint")) {
                    fingerprints.put(key, value);
                } else if (key.contains("build.id") && !key.contains("display")) {
                    buildIds.put(key, value);
                } else if (key.contains("date.utc")) {
                    // 日期转换为时间戳（如果是数字）
                    Long timestamp = parseLong(value);
                    if (timestamp != null) {
                        dates.put(key, timestamp);
                    }
                } else if (key.contains("version")) {
                    version.put(key, value);
                } else {
                    // 其他重要字段
                    if (key.equals("ro.build.description") || 
                        key.equals("ro.build.display.id") ||
                        key.equals("ro.build.host") ||
                        key.equals("ro.build.user") ||
                        key.contains("baseband") ||
                        key.contains("security_patch")) {
                        other.put(key, value);
                    }
                }
            }
            
            // 只添加非空的分类
            if (security.length() > 0) buildInfo.put("security", security);
            if (usb.length() > 0) buildInfo.put("usb", usb);
            if (version.length() > 0) buildInfo.put("version", version);
            if (fingerprints.length() > 0) buildInfo.put("fingerprints", fingerprints);
            if (buildIds.length() > 0) buildInfo.put("build_ids", buildIds);
            if (dates.length() > 0) buildInfo.put("build_dates", dates);
            if (other.length() > 0) buildInfo.put("other", other);
            
        } catch (Exception e) {
            Log.e(TAG, "清洗 Build 信息失败", e);
        }
        
        return buildInfo;
    }

    /**
     * 解析整数
     * @param str 字符串
     * @return 整数，失败返回 null
     */
    private static Integer parseInteger(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析长整数
     * @param str 字符串
     * @return 长整数，失败返回 null
     */
    private static Long parseLong(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析浮点数
     * @param str 字符串
     * @return 浮点数，失败返回 null
     */
    private static Double parseDouble(String str) {
        if (str == null || str.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 格式化输出清洗后的指纹数据为易读格式
     * @param cleanedData 清洗后的 JSON 数据
     * @return 格式化的字符串
     */
    @NonNull
    public static String formatCleanedData(@NonNull JSONObject cleanedData) {
        try {
            return cleanedData.toString(2); // 缩进2个空格
        } catch (JSONException e) {
            return cleanedData.toString();
        }
    }
}

