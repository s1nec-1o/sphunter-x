package com.sheep.sphunter.analyse;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Native 层指纹分析器
 * 负责分析清洗后的 Native 层指纹数据，生成设备唯一标识和风险报告
 */
public class NativeHunterAnalysis {

    public static class Result {
        public String nativeDeviceId;        // Native 层设备唯一标识
        public String riskReport;            // 风险分析报告
        public boolean isEmulator;           // 是否为模拟器
        public boolean isRooted;             // 是否已 Root
        public boolean isDebugMode;          // 是否处于调试模式
        public boolean hasZygiskInjection;   // 是否检测到 Zygisk 注入
        public int riskScore;                // 风险评分 (0-100)
    }

    /**
     * 核心分析方法：传入清洗后的 Native 层 JSON 字符串，返回分析结果
     */
    public static Result analyze(String jsonString) {
        Result result = new Result();
        StringBuilder riskLog = new StringBuilder();
        int riskScore = 0;

        try {
            JSONObject data = new JSONObject(jsonString);

            // ==========================================
            // 1. 生成 Native 层唯一设备 ID
            // ==========================================
            result.nativeDeviceId = generateNativeDeviceId(data);

            // ==========================================
            // 2. 风险环境检测 (Risk Detection)
            // ==========================================

            // --- A. 模拟器检测 (Emulator Detection) ---
            EmulatorCheckResult emulatorCheck = checkEmulator(data);
            result.isEmulator = emulatorCheck.isEmulator;
            if (emulatorCheck.isEmulator) {
                riskLog.append(emulatorCheck.reason);
                riskScore += 40; // 模拟器高危
            }

            // --- B. Root/解锁检测 (Root Detection) ---
            RootCheckResult rootCheck = checkRoot(data);
            result.isRooted = rootCheck.isRooted;
            if (rootCheck.isRooted) {
                riskLog.append(rootCheck.reason);
                riskScore += 30; // Root 中危
            }

            // --- C. 调试模式检测 (Debug Detection) ---
            DebugCheckResult debugCheck = checkDebugMode(data);
            result.isDebugMode = debugCheck.isDebugMode;
            if (debugCheck.isDebugMode) {
                riskLog.append(debugCheck.reason);
                riskScore += 20; // 调试模式中危
            }

            // --- D. Zygisk 注入检测 (Zygisk Injection) ---
            ZygiskCheckResult zygiskCheck = checkZygiskInjection(data);
            result.hasZygiskInjection = zygiskCheck.hasInjection;
            if (zygiskCheck.hasInjection) {
                riskLog.append(zygiskCheck.reason);
                riskScore += 50; // Zygisk 注入高危
            }

            // --- E. 其他风险标签检测 ---
            String otherRisks = checkOtherRisks(data);
            if (!otherRisks.isEmpty()) {
                riskLog.append(otherRisks);
                riskScore += 10;
            }

            // 限制风险评分在 0-100 之间
            result.riskScore = Math.min(100, riskScore);

            // 生成最终报告
            if (riskLog.length() == 0) {
                result.riskReport = "✅ Native 层环境安全 (风险评分: " + result.riskScore + "/100)";
            } else {
                result.riskReport = "⚠️ Native 层发现风险 (风险评分: " + result.riskScore + "/100):\n" + riskLog.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
            result.riskReport = "Native 层分析失败: " + e.getMessage();
            result.riskScore = -1;
        }

        return result;
    }

    /**
     * 生成 Native 层设备唯一标识
     */
    private static String generateNativeDeviceId(JSONObject data) {
        try {
            JSONObject deviceIdentity = data.optJSONObject("device_identity");
            JSONObject nativeProbes = data.optJSONObject("native_probes");
            JSONObject kernelProps = data.optJSONObject("kernel_props");
            JSONObject securityStates = data.optJSONObject("security_states");

            if (deviceIdentity == null) deviceIdentity = new JSONObject();
            if (nativeProbes == null) nativeProbes = new JSONObject();
            if (kernelProps == null) kernelProps = new JSONObject();
            if (securityStates == null) securityStates = new JSONObject();

            // 1. DRM Device ID (最稳定的硬件标识)
            String drmId = deviceIdentity.optString("drm_device_id", "");

            // 2. CPU 结构哈希 (硬件特征)
            String cpuStructureHash = "";
            JSONObject cpuStructure = nativeProbes.optJSONObject("cpu_structure");
            if (cpuStructure != null) {
                cpuStructureHash = cpuStructure.optString("cpu_structure_hash", "");
            }

            // 3. 内存信息 (取整防止小波动)
            int totalRamMb = 0;
            JSONObject memStructure = nativeProbes.optJSONObject("memory_structure");
            if (memStructure != null) {
                totalRamMb = (int) Math.round(memStructure.optDouble("total_ram_mb", 0) / 100.0) * 100;
            }

            // 4. 内核版本 (系统特征)
            String kernelRelease = kernelProps.optString("uname_release", "");

            // 5. CPU 架构
            String cpuAbi = deviceIdentity.optString("cpu_abi", "");

            // 6. Build Fingerprint (厂商定制信息)
            String buildFingerprint = deviceIdentity.optString("fingerprint_string", "");

            // 7. VBMeta Digest (设备启动验证摘要，每台设备唯一)
            String vbmetaDigest = securityStates.optString("vbmeta_digest", "");

            // 拼接指纹因子
            String rawFingerprint = String.format(
                "%s|%s|%d|%s|%s|%s|%s",
                drmId,
                cpuStructureHash,
                totalRamMb,
                kernelRelease,
                cpuAbi,
                buildFingerprint,
                vbmetaDigest
            );

            // 计算 SHA-256
            return sha256(rawFingerprint);

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 模拟器检测
     */
    private static EmulatorCheckResult checkEmulator(JSONObject data) {
        EmulatorCheckResult result = new EmulatorCheckResult();
        StringBuilder reasons = new StringBuilder();

        try {
            JSONObject nativeProbes = data.optJSONObject("native_probes");
            JSONObject kernelProps = data.optJSONObject("kernel_props");
            JSONObject deviceIdentity = data.optJSONObject("device_identity");

            if (nativeProbes == null || kernelProps == null || deviceIdentity == null) {
                return result;
            }

            // 1. CPU 特征检测
            JSONObject cpuStructure = nativeProbes.optJSONObject("cpu_structure");
            if (cpuStructure != null) {
                String hardware = cpuStructure.optString("hardware", "");
                
                // 模拟器常见 Hardware 特征
                if (hardware.toLowerCase().contains("goldfish") ||
                    hardware.toLowerCase().contains("ranchu") ||
                    hardware.toLowerCase().contains("vbox") ||
                    hardware.toLowerCase().contains("virtual")) {
                    result.isEmulator = true;
                    reasons.append("[高危] CPU Hardware 显示为模拟器特征: ").append(hardware).append("\n");
                }

                // CPU parts 数量异常（模拟器通常只有 1-2 个核心）
                JSONArray cpuParts = cpuStructure.optJSONArray("cpu_parts");
                if (cpuParts != null && cpuParts.length() < 2) {
                    result.isEmulator = true;
                    reasons.append("[疑点] CPU parts 数量异常 (").append(cpuParts.length()).append("), 疑似模拟器\n");
                }
            }

            // 2. 内核版本检测（模拟器通常使用旧内核或特殊内核）
            String kernelRelease = kernelProps.optString("uname_release", "");
            if (kernelRelease.contains("-generic") || kernelRelease.contains("ranchu")) {
                result.isEmulator = true;
                reasons.append("[疑点] 内核版本包含模拟器特征: ").append(kernelRelease).append("\n");
            }

            // 3. Build 标签检测
            String buildTags = deviceIdentity.optString("build_tags", "");
            if (buildTags.contains("test-keys")) {
                result.isEmulator = true;
                reasons.append("[疑点] Build 使用 test-keys 签名，疑似模拟器或定制 ROM\n");
            }

            // 4. 文件访问模式检测（模拟器某些系统文件可能不存在）
            JSONObject fileAccessMap = nativeProbes.optJSONObject("file_access_map");
            if (fileAccessMap != null) {
                int notFoundCount = 0;
                String[] criticalFiles = {
                    "/proc/cpuinfo",
                    "/proc/meminfo",
                    "/sys/devices/system/cpu/possible"
                };
                
                for (String file : criticalFiles) {
                    String status = fileAccessMap.optString(file, "");
                    if (status.equals("NOT_FOUND")) {
                        notFoundCount++;
                    }
                }
                
                if (notFoundCount > 1) {
                    result.isEmulator = true;
                    reasons.append("[疑点] 多个关键系统文件不存在 (").append(notFoundCount).append("), 疑似模拟器\n");
                }
            }

            // 5. Build Host 检测（模拟器通常有特定的构建主机名）
            String buildHost = deviceIdentity.optString("build_host", "");
            if (buildHost.toLowerCase().contains("ubuntu") ||
                buildHost.toLowerCase().contains("localhost") ||
                buildHost.toLowerCase().contains("android-build")) {
                reasons.append("[低危] Build Host 显示为常见模拟器构建环境: ").append(buildHost).append("\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        result.reason = reasons.toString();
        return result;
    }

    /**
     * Root 检测
     */
    private static RootCheckResult checkRoot(JSONObject data) {
        RootCheckResult result = new RootCheckResult();
        StringBuilder reasons = new StringBuilder();

        try {
            JSONObject securityStates = data.optJSONObject("security_states");
            if (securityStates == null) {
                return result;
            }

            // 1. Bootloader 锁定状态
            boolean bootloaderLocked = securityStates.optBoolean("bootloader_locked", true);
            if (!bootloaderLocked) {
                result.isRooted = true;
                reasons.append("[高危] Bootloader 未锁定，设备可能已 Root\n");
            }

            // 2. Verified Boot 状态
            String vbState = securityStates.optString("vb_state", "");
            if (!vbState.isEmpty() && !vbState.equalsIgnoreCase("green")) {
                result.isRooted = true;
                reasons.append("[高危] Verified Boot 状态异常: ").append(vbState).append(" (正常应为 green)\n");
            }

            // 3. VBMeta Device State
            String vbmetaDeviceState = securityStates.optString("vbmeta_device_state", "");
            if (vbmetaDeviceState.equalsIgnoreCase("unlocked")) {
                result.isRooted = true;
                reasons.append("[高危] VBMeta Device State 为 unlocked，设备已解锁\n");
            }

            // 4. ro.secure 状态
            boolean roSecure = securityStates.optBoolean("ro_secure", true);
            if (!roSecure) {
                result.isRooted = true;
                reasons.append("[中危] ro.secure = 0，系统安全模式已关闭\n");
            }

            // 5. SELinux 状态
            boolean selinuxEnforcing = securityStates.optBoolean("selinux_enforcing", true);
            if (!selinuxEnforcing) {
                result.isRooted = true;
                reasons.append("[中危] SELinux 未处于强制模式，可能已被篡改\n");
            }

            // 6. OEM Unlock 允许状态
            boolean oemUnlockAllowed = securityStates.optBoolean("oem_unlock_allowed", false);
            if (oemUnlockAllowed) {
                reasons.append("[低危] OEM 解锁已允许，设备可能即将被解锁\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        result.reason = reasons.toString();
        return result;
    }

    /**
     * 调试模式检测
     */
    private static DebugCheckResult checkDebugMode(JSONObject data) {
        DebugCheckResult result = new DebugCheckResult();
        StringBuilder reasons = new StringBuilder();

        try {
            JSONObject securityStates = data.optJSONObject("security_states");
            if (securityStates == null) {
                return result;
            }

            // 1. ADB 启用检测
            boolean adbEnabled = securityStates.optBoolean("adb_enabled", false);
            if (adbEnabled) {
                result.isDebugMode = true;
                reasons.append("[高危] USB 调试模式已开启 (ADB Enabled)\n");
            }

            // 2. ro.debuggable 状态
            boolean debuggable = securityStates.optBoolean("debuggable", false);
            if (debuggable) {
                result.isDebugMode = true;
                reasons.append("[高危] 系统可调试 (ro.debuggable = 1)，这是非正式版本\n");
            }

            // 3. ADBD 服务状态
            String adbdServiceStatus = securityStates.optString("adbd_service_status", "");
            if (adbdServiceStatus.equalsIgnoreCase("running")) {
                result.isDebugMode = true;
                reasons.append("[中危] ADBD 服务正在运行\n");
            }

            // 4. USB 状态检测
            String usbState = securityStates.optString("usb_state", "");
            if (usbState.contains("adb")) {
                reasons.append("[中危] USB 状态包含 ADB: ").append(usbState).append("\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        result.reason = reasons.toString();
        return result;
    }

    /**
     * Zygisk 注入检测
     */
    private static ZygiskCheckResult checkZygiskInjection(JSONObject data) {
        ZygiskCheckResult result = new ZygiskCheckResult();
        StringBuilder reasons = new StringBuilder();

        try {
            // 检查风险标签
            JSONArray riskTags = data.optJSONArray("risk_tags");
            if (riskTags != null) {
                for (int i = 0; i < riskTags.length(); i++) {
                    String tag = riskTags.optString(i);
                    if (tag.contains("ZYGISK") || tag.contains("MAGISK") || tag.contains("SUSPICIOUS_LIB")) {
                        result.hasInjection = true;
                        reasons.append("[高危] 检测到可疑注入: ").append(tag).append("\n");
                    }
                }
            }

            // 检查 Native Probes 中的文件访问模式
            JSONObject nativeProbes = data.optJSONObject("native_probes");
            if (nativeProbes != null) {
                JSONObject fileAccessMap = nativeProbes.optJSONObject("file_access_map");
                if (fileAccessMap != null) {
                    // 检查是否能访问某些 Magisk/Zygisk 相关路径
                    String[] suspiciousPaths = {
                        "/sbin/.magisk",
                        "/system/xbin/su",
                        "/system/bin/su"
                    };
                    
                    for (String path : suspiciousPaths) {
                        String status = fileAccessMap.optString(path, "");
                        if (status.equals("OK")) {
                            result.hasInjection = true;
                            reasons.append("[高危] 检测到可疑文件: ").append(path).append("\n");
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        result.reason = reasons.toString();
        return result;
    }

    /**
     * 其他风险检测
     */
    private static String checkOtherRisks(JSONObject data) {
        StringBuilder risks = new StringBuilder();

        try {
            JSONArray riskTags = data.optJSONArray("risk_tags");
            if (riskTags != null && riskTags.length() > 0) {
                for (int i = 0; i < riskTags.length(); i++) {
                    String tag = riskTags.optString(i);
                    if (!tag.contains("ZYGISK") && !tag.contains("MAGISK") && !tag.contains("SUSPICIOUS_LIB")) {
                        risks.append("[提示] 风险标签: ").append(tag).append("\n");
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return risks.toString();
    }

    // ==========================================
    // 辅助类与工具方法
    // ==========================================

    private static class EmulatorCheckResult {
        boolean isEmulator = false;
        String reason = "";
    }

    private static class RootCheckResult {
        boolean isRooted = false;
        String reason = "";
    }

    private static class DebugCheckResult {
        boolean isDebugMode = false;
        String reason = "";
    }

    private static class ZygiskCheckResult {
        boolean hasInjection = false;
        String reason = "";
    }

    /**
     * SHA256 工具方法
     */
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

