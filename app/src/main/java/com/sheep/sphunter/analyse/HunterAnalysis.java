package com.sheep.sphunter.analyse;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HunterAnalysis {

    public static class Result {
        public String deviceId;
        public String riskReport;
        public boolean isEmulator;
        public boolean isDebugMode;
    }

    /**
     * 核心方法：传入清洗后的 JSON 字符串，返回分析结果
     */
    public static Result analyze(String jsonString) {
        Result result = new Result();
        StringBuilder riskLog = new StringBuilder();

        try {
            JSONObject data = new JSONObject(jsonString);

            // ==========================================
            // 1. 生成唯一设备 ID (Stable Device ID)
            // ==========================================
            String drmId = data.optJSONObject("identity").optString("drm_device_id", "");
            String gpuRenderer = data.optJSONObject("hardware").optJSONObject("gpu").optString("renderer", "");

            // 内存和存储取整 (防止系统小更新导致小数位变化)
            double ramGb = data.optJSONObject("hardware").optJSONObject("memory").optJSONObject("ram").optDouble("total_gb", 0);
            double romGb = data.optJSONObject("hardware").optJSONObject("memory").optJSONObject("internal_storage").optDouble("total_gb", 0);
            int ramInt = (int) Math.round(ramGb);
            int romInt = (int) Math.round(romGb);

            // 传感器列表哈希 (防止篡改)
            List<String> sensorNames = new ArrayList<>();
            JSONArray sensors = data.optJSONObject("sensors").optJSONArray("sensor_list");
            if (sensors != null) {
                for (int i = 0; i < sensors.length(); i++) {
                    sensorNames.add(sensors.optJSONObject(i).optString("name"));
                }
            }
            Collections.sort(sensorNames); // 排序保证顺序一致

            // 拼接指纹因子
            String rawFingerprint = drmId + "|" + gpuRenderer + "|" + ramInt + "|" + romInt + "|" + sensorNames.toString();

            // 计算 SHA-256
            result.deviceId = sha256(rawFingerprint);

            // ==========================================
            // 2. 风险环境检测 (Risk Detection)
            // ==========================================

            // --- A. 调试检测 ---
            boolean adbEnabled = false;
            JSONObject usbObj = data.optJSONObject("system").optJSONObject("build_properties").optJSONObject("usb");
            if (usbObj != null) {
                String usbConfig = usbObj.optString("sys.usb.config", "");
                if (usbConfig.contains("adb")) adbEnabled = true;
            }

            String plugged = data.optJSONObject("hardware").optJSONObject("battery").optString("plugged", "");

            if (adbEnabled && "USB".equalsIgnoreCase(plugged)) {
                result.isDebugMode = true;
                riskLog.append("[高危] 检测到 USB 调试模式已开启且连接电脑\n");
            }

            // --- B. 模拟器检测 ---
            result.isEmulator = false;
            if (gpuRenderer.contains("Goldfish") || gpuRenderer.contains("llvmpipe") || gpuRenderer.contains("Ranchu")) {
                result.isEmulator = true;
                riskLog.append("[高危] GPU 渲染器显示为模拟器特征: ").append(gpuRenderer).append("\n");
            }

            // 检查 Sensor 数量 (模拟器通常很少)
            if (sensorNames.size() < 5) {
                result.isEmulator = true;
                riskLog.append("[疑点] 传感器数量过少 (" + sensorNames.size() + "), 疑似模拟器\n");
            }

            // --- C. Root/解锁检测 ---
            JSONObject security = data.optJSONObject("system").optJSONObject("build_properties").optJSONObject("security");
            if (security != null) {
                String locked = security.optString("ro.boot.flash.locked", "1");
                if (!"1".equals(locked)) {
                    riskLog.append("[中危] Bootloader 未锁定，可能已 Root\n");
                }
            }

            if (riskLog.length() == 0) {
                result.riskReport = "✅ 设备环境安全";
            } else {
                result.riskReport = "⚠️ 发现风险:\n" + riskLog.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.riskReport = "分析失败: " + e.getMessage();
        }

        return result;
    }

    // 辅助：SHA256 工具
    private static String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            return "";
        }
    }
}
