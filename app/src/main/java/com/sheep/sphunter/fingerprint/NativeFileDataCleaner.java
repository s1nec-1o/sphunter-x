package com.sheep.sphunter.fingerprint;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Native 文件数据清洗器
 * 负责对采集的 Native 层文件指纹信息进行去噪、归一化和结构化处理
 * 实现标准化的指纹数据清洗与特征提取流程
 */
public class NativeFileDataCleaner {
    private static final String TAG = "NativeFileDataCleaner";

    // 状态码枚举
    private static final String STATUS_OK = "OK";
    private static final String STATUS_PERM_DENIED = "PERM_DENIED";
    private static final String STATUS_NOT_FOUND = "NOT_FOUND";
    private static final String STATUS_ERROR = "ERROR";

    /**
     * 清洗并结构化 Native 文件指纹数据
     * @param rawNativeInfo 原始 Native 文件信息字符串
     * @return 清洗后的结构化 JSON 数据
     */
    @NonNull
    public static JSONObject cleanNativeFingerprint(@NonNull String rawNativeInfo) {
        try {
            JSONObject cleaned = new JSONObject();
            
            // 解析原始数据
            ParsedNativeData parsed = parseRawNativeData(rawNativeInfo);
            
            // 1. Device Identity - 设备身份信息
            JSONObject deviceIdentity = buildDeviceIdentity(parsed);
            cleaned.put("device_identity", deviceIdentity);
            
            // 2. Security States - 安全状态
            JSONObject securityStates = buildSecurityStates(parsed);
            cleaned.put("security_states", securityStates);
            
            // 3. Native Probes - Native 层探针结果
            JSONObject nativeProbes = buildNativeProbes(parsed);
            cleaned.put("native_probes", nativeProbes);
            
            // 4. Kernel Properties - 内核属性
            JSONObject kernelProps = buildKernelProperties(parsed);
            cleaned.put("kernel_props", kernelProps);
            
            // 5. Risk Tags - 风险标签
            JSONArray riskTags = buildRiskTags(parsed);
            if (riskTags.length() > 0) {
                cleaned.put("risk_tags", riskTags);
            }
            
            return cleaned;
            
        } catch (JSONException e) {
            Log.e(TAG, "清洗 Native 指纹数据失败", e);
            return new JSONObject();
        }
    }

    /**
     * 解析原始 Native 数据字符串
     */
    private static ParsedNativeData parseRawNativeData(String raw) {
        ParsedNativeData parsed = new ParsedNativeData();
        
        if (raw == null || raw.trim().isEmpty()) {
            return parsed;
        }
        
        // 使用正则表达式分割 section，保留标题
        String[] lines = raw.split("\n");
        StringBuilder currentSection = new StringBuilder();
        String currentSectionTitle = "";
        
        for (String line : lines) {
            // 检查是否是新的 section 标题
            if (line.trim().startsWith("===") && line.trim().endsWith("===")) {
                // 处理上一个 section
                if (currentSection.length() > 0) {
                    processSection(currentSectionTitle, currentSection.toString(), parsed);
                }
                
                // 开始新 section
                currentSectionTitle = line.trim().replaceAll("^===\\s*", "").replaceAll("\\s*===$", "").trim();
                currentSection = new StringBuilder();
            } else {
                // 添加内容到当前 section
                if (currentSection.length() > 0) {
                    currentSection.append("\n");
                }
                currentSection.append(line);
            }
        }
        
        // 处理最后一个 section
        if (currentSection.length() > 0) {
            processSection(currentSectionTitle, currentSection.toString(), parsed);
        }
        
        Log.d(TAG, "解析完成: 系统属性=" + parsed.systemProperties.size() + 
                   ", 文件探针=" + parsed.fileProbes.size());
        
        return parsed;
    }
    
    /**
     * 处理单个 section
     */
    private static void processSection(String title, String content, ParsedNativeData parsed) {
        if (title.isEmpty() || content.trim().isEmpty()) {
            return;
        }
        
        Log.d(TAG, "处理 Section: " + title + " (内容长度: " + content.length() + ")");
        
        // 解析系统属性相关的 section
        if (title.contains("Native Build Info") || title.contains("USB Config") || 
            title.contains("Security") || title.contains("Build ID") ||
            title.contains("SDK Version") || title.contains("Security Patch") ||
            title.contains("Other System") || title.contains("Display ID") ||
            title.contains("Build Host") || title.contains("Build Version") ||
            title.contains("Build Description") || title.contains("Build Fingerprint") ||
            title.contains("Other System Property")) {
            parseSystemProperties(content, parsed);
        }
        
        // 解析 DRM 信息
        if (title.contains("DRM Info")) {
            parseDrmInfo(content, parsed);
        }
        
        // 解析硬件信息
        if (title.contains("核心硬件与内核特征") || title.contains("Hardware & Kernel")) {
            parseHardwareInfo(content, parsed);
        }
        
        // 解析环境信息
        if (title.contains("环境与安全检测") || title.contains("Environment & Security")) {
            parseEnvironmentInfo(content, parsed);
        }
        
        // 解析挂载信息
        if (title.contains("挂载点与输入设备") || title.contains("Mounts & Inputs")) {
            parseMountInfo(content, parsed);
        }
        
        // 解析内核信息
        if (title.contains("内核信息") || title.contains("Kernel Info")) {
            parseKernelInfo(content, parsed);
        }
        
        // 解析系统配置信息
        if (title.contains("系统配置信息") || title.contains("System Config")) {
            parseSystemConfig(content, parsed);
        }
    }

    /**
     * 解析系统属性
     */
    private static void parseSystemProperties(String section, ParsedNativeData parsed) {
        String[] lines = section.split("\n");
        int parsedCount = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("===")) continue;
            
            int equalIndex = line.indexOf(" = ");
            if (equalIndex > 0) {
                String key = line.substring(0, equalIndex).trim();
                String value = line.substring(equalIndex + 3).trim();
                
                if (!value.equals("null") && !value.contains("SecurityException")) {
                    parsed.systemProperties.put(key, value);
                    parsedCount++;
                }
            }
        }
        if (parsedCount > 0) {
            Log.d(TAG, "解析了 " + parsedCount + " 个系统属性");
        }
    }

    /**
     * 解析硬件信息（包括 /proc/cpuinfo, /proc/meminfo 等）
     */
    private static void parseHardwareInfo(String section, ParsedNativeData parsed) {
        String[] lines = section.split("\n");
        String currentPath = null;
        StringBuilder currentContent = new StringBuilder();
        int exitCode = -1;
        boolean accessible = false;
        boolean inContent = false;
        
        for (String line : lines) {
            String originalLine = line;
            line = line.trim();
            
            if (line.startsWith("Path:")) {
                // 保存上一个文件的数据
                if (currentPath != null) {
                    FileProbe probe = new FileProbe();
                    probe.path = currentPath;
                    probe.content = currentContent.toString().trim();
                    probe.exitCode = exitCode;
                    probe.accessible = accessible;
                    parsed.fileProbes.put(currentPath, probe);
                }
                
                // 开始新文件
                currentPath = line.substring(5).trim();
                currentContent = new StringBuilder();
                exitCode = -1;
                accessible = false;
                inContent = false;
            } else if (line.startsWith("Exit Code:")) {
                try {
                    exitCode = Integer.parseInt(line.substring(10).trim());
                } catch (NumberFormatException e) {
                    exitCode = -1;
                }
                inContent = false;
            } else if (line.startsWith("Accessible:")) {
                accessible = line.contains("true");
                inContent = false;
            } else if (line.startsWith("Content:") || line.startsWith("Content (truncated):")) {
                inContent = true;
                String content = line.substring(line.indexOf(":") + 1).trim();
                if (!content.equals("[EMPTY]")) {
                    if (currentContent.length() > 0) {
                        currentContent.append("\n");
                    }
                    currentContent.append(content);
                }
            } else if (line.equals("---")) {
                // 文件结束标记
                inContent = false;
            } else if (inContent && currentPath != null) {
                // 继续读取内容（保留原始行的格式）
                if (currentContent.length() > 0) {
                    currentContent.append("\n");
                }
                currentContent.append(originalLine);
            }
        }
        
        // 保存最后一个文件
        if (currentPath != null) {
            FileProbe probe = new FileProbe();
            probe.path = currentPath;
            probe.content = currentContent.toString().trim();
            probe.exitCode = exitCode;
            probe.accessible = accessible;
            parsed.fileProbes.put(currentPath, probe);
        }
    }

    /**
     * 解析环境信息
     */
    private static void parseEnvironmentInfo(String section, ParsedNativeData parsed) {
        parseHardwareInfo(section, parsed); // 使用相同的解析逻辑
    }

    /**
     * 解析挂载信息
     */
    private static void parseMountInfo(String section, ParsedNativeData parsed) {
        parseHardwareInfo(section, parsed); // 使用相同的解析逻辑
    }

    /**
     * 解析内核信息（uname）
     */
    private static void parseKernelInfo(String section, ParsedNativeData parsed) {
        String[] lines = section.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Release:")) {
                parsed.kernelRelease = line.substring(8).trim();
            } else if (line.startsWith("Machine:")) {
                parsed.kernelMachine = line.substring(8).trim();
            } else if (line.startsWith("System Name:")) {
                String value = line.substring(12).trim();
                if (!value.equals("null") && !value.isEmpty()) {
                    parsed.systemProperties.put("uname.sysname", value);
                }
            } else if (line.startsWith("Node Name:")) {
                String value = line.substring(10).trim();
                if (!value.equals("null") && !value.isEmpty()) {
                    parsed.systemProperties.put("uname.nodename", value);
                }
            } else if (line.startsWith("Version:")) {
                String value = line.substring(8).trim();
                if (!value.equals("null") && !value.isEmpty()) {
                    parsed.systemProperties.put("uname.version", value);
                }
            } else if (line.startsWith("Domain Name:")) {
                String value = line.substring(12).trim();
                if (!value.equals("null") && !value.isEmpty()) {
                    parsed.systemProperties.put("uname.domainname", value);
                }
            }
        }
    }

    /**
     * 解析系统配置信息（sysconf）
     */
    private static void parseSystemConfig(String section, ParsedNativeData parsed) {
        String[] lines = section.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Page Size:")) {
                try {
                    String value = line.substring(10).trim().replace(" bytes", "").trim();
                    parsed.pageSize = Long.parseLong(value);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            } else if (line.startsWith("Physical Pages:")) {
                try {
                    String value = line.substring(15).trim();
                    parsed.physPages = Long.parseLong(value);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            } else if (line.startsWith("Total Physical Memory:")) {
                try {
                    String value = line.substring(22).trim().replace(" MB", "").trim();
                    parsed.totalRamMb = Long.parseLong(value);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            } else if (line.startsWith("CPU Cores (Online):")) {
                try {
                    String value = line.substring(19).trim();
                    parsed.cpuCores = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        }
    }

    /**
     * 解析 DRM 信息
     */
    private static void parseDrmInfo(String section, ParsedNativeData parsed) {
        String[] lines = section.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("MediaDrm Device Unique ID (Hex):")) {
                String value = line.substring(33).trim();
                if (!value.isEmpty() && !value.equals("null")) {
                    parsed.drmDeviceId = value;
                }
            }
        }
    }

    /**
     * 构建设备身份信息
     */
    private static JSONObject buildDeviceIdentity(ParsedNativeData parsed) throws JSONException {
        JSONObject identity = new JSONObject();
        
        // Board / Platform
        String board = parsed.systemProperties.get("ro.board.platform");
        if (board != null && !board.equals("null")) {
            identity.put("board", board);
        }
        
        // Product Name
        String product = parsed.systemProperties.get("ro.product.name");
        if (product == null || product.equals("null")) {
            product = parsed.systemProperties.get("ro.product.device");
        }
        if (product != null && !product.equals("null")) {
            identity.put("product", product);
        }
        
        // Model
        String model = parsed.systemProperties.get("ro.product.model");
        if (model != null && !model.equals("null")) {
            identity.put("model", model);
        }
        
        // Build Fingerprint
        String fingerprint = parsed.systemProperties.get("ro.build.fingerprint");
        if (fingerprint == null || fingerprint.equals("null")) {
            fingerprint = parsed.systemProperties.get("ro.build.build.fingerprint");
        }
        if (fingerprint != null && !fingerprint.equals("null")) {
            identity.put("fingerprint_string", fingerprint);
        }
        
        // Build ID
        String buildId = parsed.systemProperties.get("ro.build.id");
        if (buildId != null && !buildId.equals("null")) {
            identity.put("build_id", buildId);
        }
        
        // Build Display ID
        String displayId = parsed.systemProperties.get("ro.build.display.id");
        if (displayId != null && !displayId.equals("null")) {
            identity.put("display_id", displayId);
        }
        
        // Build Tags
        String tags = parsed.systemProperties.get("ro.build.tags");
        if (tags != null && !tags.equals("null")) {
            identity.put("build_tags", tags);
        }
        
        // Build Description
        String description = parsed.systemProperties.get("ro.build.description");
        if (description != null && !description.equals("null")) {
            identity.put("build_description", description);
        }
        
        // Security Patch
        String securityPatch = parsed.systemProperties.get("ro.build.version.security_patch");
        if (securityPatch != null && !securityPatch.equals("null")) {
            identity.put("security_patch", securityPatch);
        }
        
        // SDK Version
        String sdk = parsed.systemProperties.get("ro.build.version.sdk");
        if (sdk != null && !sdk.equals("null")) {
            try {
                identity.put("sdk_version", Integer.parseInt(sdk));
            } catch (NumberFormatException e) {
                identity.put("sdk_version", sdk);
            }
        }
        
        // Build Incremental
        String incremental = parsed.systemProperties.get("ro.build.version.incremental");
        if (incremental != null && !incremental.equals("null")) {
            identity.put("incremental", incremental);
        }
        
        // CPU ABI
        String cpuAbi = parsed.systemProperties.get("ro.product.cpu.abi");
        if (cpuAbi != null && !cpuAbi.equals("null")) {
            identity.put("cpu_abi", cpuAbi);
        }
        
        // Baseband Version
        String baseband = parsed.systemProperties.get("gsm.version.baseband");
        if (baseband != null && !baseband.equals("null")) {
            identity.put("baseband", baseband);
        }
        
        // DRM Device ID
        if (parsed.drmDeviceId != null && !parsed.drmDeviceId.isEmpty()) {
            identity.put("drm_device_id", parsed.drmDeviceId);
        }
        
        // Build Host & User
        String buildHost = parsed.systemProperties.get("ro.build.host");
        if (buildHost != null && !buildHost.equals("null")) {
            identity.put("build_host", buildHost);
        }
        
        String buildUser = parsed.systemProperties.get("ro.build.user");
        if (buildUser != null && !buildUser.equals("null")) {
            identity.put("build_user", buildUser);
        }
        
        // Build Date UTC
        String buildDateUtc = parsed.systemProperties.get("ro.build.date.utc");
        if (buildDateUtc != null && !buildDateUtc.equals("null")) {
            try {
                identity.put("build_date_utc", Long.parseLong(buildDateUtc));
            } catch (NumberFormatException e) {
                identity.put("build_date_utc", buildDateUtc);
            }
        }
        
        return identity;
    }

    /**
     * 构建安全状态
     */
    private static JSONObject buildSecurityStates(ParsedNativeData parsed) throws JSONException {
        JSONObject security = new JSONObject();
        
        // Bootloader 锁定状态
        String flashLocked = parsed.systemProperties.get("ro.boot.flash.locked");
        if (flashLocked != null && !flashLocked.equals("null")) {
            security.put("bootloader_locked", flashLocked.equals("1"));
        }
        
        // OEM Unlock 允许状态
        String oemUnlock = parsed.systemProperties.get("sys.oem_unlock_allowed");
        if (oemUnlock != null && !oemUnlock.equals("null")) {
            security.put("oem_unlock_allowed", oemUnlock.equals("1"));
        }
        
        // Verified Boot 状态
        String vbState = parsed.systemProperties.get("ro.boot.verifiedbootstate");
        if (vbState != null && !vbState.equals("null")) {
            security.put("vb_state", vbState);
        }
        
        // VBMeta Device State
        String vbmetaDeviceState = parsed.systemProperties.get("ro.boot.vbmeta.device_state");
        if (vbmetaDeviceState != null && !vbmetaDeviceState.equals("null")) {
            security.put("vbmeta_device_state", vbmetaDeviceState);
        }
        
        // VBMeta Digest
        String vbmetaDigest = parsed.systemProperties.get("ro.boot.vbmeta.digest");
        if (vbmetaDigest != null && !vbmetaDigest.equals("null")) {
            security.put("vbmeta_digest", vbmetaDigest);
        }
        
        // ro.secure
        String roSecure = parsed.systemProperties.get("ro.secure");
        if (roSecure != null && !roSecure.equals("null")) {
            security.put("ro_secure", roSecure.equals("1"));
        }
        
        // ro.debuggable
        String debuggable = parsed.systemProperties.get("ro.debuggable");
        if (debuggable != null && !debuggable.equals("null")) {
            security.put("debuggable", debuggable.equals("1"));
        }
        
        // ADB 启用状态
        String usbConfig = parsed.systemProperties.get("sys.usb.config");
        boolean adbEnabled = usbConfig != null && usbConfig.contains("adb");
        security.put("adb_enabled", adbEnabled);
        
        // ADB 服务状态
        String adbdService = parsed.systemProperties.get("init.svc.adbd");
        if (adbdService != null && !adbdService.equals("null")) {
            security.put("adbd_service_status", adbdService);
        }
        
        // USB 状态
        String usbState = parsed.systemProperties.get("sys.usb.state");
        if (usbState != null && !usbState.equals("null")) {
            security.put("usb_state", usbState);
        }
        
        // SELinux 强制模式（通过文件访问推断）
        FileProbe selinuxProbe = parsed.fileProbes.get("/sys/fs/selinux/enforce");
        boolean selinuxEnforcing = false;
        if (selinuxProbe != null && !selinuxProbe.accessible) {
            // 如果无法读取，可能是强制模式（权限拒绝）
            selinuxEnforcing = selinuxProbe.exitCode == 1;
        }
        security.put("selinux_enforcing", selinuxEnforcing);
        
        // Treble 支持
        String trebleEnabled = parsed.systemProperties.get("ro.treble.enabled");
        if (trebleEnabled != null && !trebleEnabled.equals("null")) {
            security.put("treble_enabled", trebleEnabled.equals("true"));
        }
        
        return security;
    }

    /**
     * 构建 Native 探针结果
     */
    private static JSONObject buildNativeProbes(ParsedNativeData parsed) throws JSONException {
        JSONObject probes = new JSONObject();
        
        // 1. CPU 结构哈希
        FileProbe cpuinfoProbe = parsed.fileProbes.get("/proc/cpuinfo");
        if (cpuinfoProbe != null && cpuinfoProbe.accessible) {
            JSONObject cpuInfo = cleanCpuInfo(cpuinfoProbe.content);
            probes.put("cpu_structure", cpuInfo);
        }
        
        // 2. 挂载信息哈希
        FileProbe mountinfoProbe = parsed.fileProbes.get("/proc/self/mountinfo");
        if (mountinfoProbe != null && mountinfoProbe.accessible) {
            String mountsHash = cleanMountInfo(mountinfoProbe.content);
            probes.put("mounts_hash", mountsHash);
        }
        
        // 3. 文件访问映射
        JSONObject fileAccessMap = new JSONObject();
        for (String path : parsed.fileProbes.keySet()) {
            FileProbe probe = parsed.fileProbes.get(path);
            if (probe != null) {
                String status = mapProbeStatus(probe);
                fileAccessMap.put(path, status);
            }
        }
        probes.put("file_access_map", fileAccessMap);
        
        // 4. 内存信息
        FileProbe meminfoProbe = parsed.fileProbes.get("/proc/meminfo");
        if (meminfoProbe != null && meminfoProbe.accessible) {
            JSONObject memInfo = cleanMemInfo(meminfoProbe.content, parsed);
            probes.put("memory_structure", memInfo);
        }
        
        return probes;
    }

    /**
     * 清洗 CPU 信息
     */
    private static JSONObject cleanCpuInfo(String rawCpuinfo) throws JSONException {
        JSONObject cpuInfo = new JSONObject();
        
        Set<String> cpuParts = new HashSet<>();
        String features = "";
        String hardware = "";
        
        String[] lines = rawCpuinfo.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 提取 CPU part
            if (line.startsWith("CPU part")) {
                String part = line.substring(line.indexOf(":") + 1).trim();
                if (!part.isEmpty()) {
                    cpuParts.add(part);
                }
            }
            
            // 提取 Features（只取第一个）
            if (line.startsWith("Features") && features.isEmpty()) {
                features = line.substring(line.indexOf(":") + 1).trim();
            }
            
            // 提取 Hardware
            if (line.startsWith("Hardware")) {
                hardware = line.substring(line.indexOf(":") + 1).trim();
            }
        }
        
        // 排序 CPU parts
        List<String> sortedParts = new ArrayList<>(cpuParts);
        Collections.sort(sortedParts);
        JSONArray cpuPartsArray = new JSONArray();
        for (String part : sortedParts) {
            cpuPartsArray.put(part);
        }
        cpuInfo.put("cpu_parts", cpuPartsArray);
        
        // Features 哈希
        if (!features.isEmpty()) {
            cpuInfo.put("features_hash", sha256Hash(features));
        }
        
        // Hardware
        if (!hardware.isEmpty()) {
            cpuInfo.put("hardware", hardware);
        }
        
        // CPU 结构哈希（用于整体匹配）
        String cpuStructure = sortedParts.toString() + "|" + features;
        cpuInfo.put("cpu_structure_hash", sha256Hash(cpuStructure));
        
        return cpuInfo;
    }

    /**
     * 清洗挂载信息
     */
    private static String cleanMountInfo(String rawMountinfo) {
        if (rawMountinfo == null || rawMountinfo.trim().isEmpty()) {
            return "";
        }
        
        List<String> cleanedLines = new ArrayList<>();
        String[] lines = rawMountinfo.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            // 使用正则去除行首的动态ID: "数字 数字 数字:数字 " -> 替换为空
            // 模式: ^\d+\s+\d+\s+\d+:\d+\s+
            String cleaned = line.replaceAll("^\\d+\\s+\\d+\\s+\\d+:\\d+\\s+", "");
            if (!cleaned.isEmpty()) {
                cleanedLines.add(cleaned);
            }
        }
        
        // 排序，确保顺序不影响哈希
        Collections.sort(cleanedLines);
        
        // 合并并哈希
        String fullText = String.join("\n", cleanedLines);
        return sha256Hash(fullText);
    }

    /**
     * 清洗内存信息
     */
    private static JSONObject cleanMemInfo(String rawMeminfo, ParsedNativeData parsed) throws JSONException {
        JSONObject memInfo = new JSONObject();
        
        // 使用 sysconf 获取的总内存（更准确）
        if (parsed.totalRamMb > 0) {
            // 向下取整到最近的 100MB
            long roundedRam = (parsed.totalRamMb / 100) * 100;
            memInfo.put("total_ram_mb", roundedRam);
        } else {
            // 从 meminfo 解析 MemTotal
            String[] lines = rawMeminfo.split("\n");
            for (String line : lines) {
                if (line.startsWith("MemTotal:")) {
                    try {
                        String value = line.substring(9).trim().replace("kB", "").trim();
                        long memTotalKb = Long.parseLong(value);
                        long memTotalMb = memTotalKb / 1024;
                        long roundedRam = (memTotalMb / 100) * 100;
                        memInfo.put("total_ram_mb", roundedRam);
                        break;
                    } catch (NumberFormatException e) {
                        // 忽略
                    }
                }
            }
        }
        
        // 检查是否有 Swap
        boolean hasSwap = rawMeminfo.contains("SwapTotal:") && 
                         !rawMeminfo.contains("SwapTotal: 0 kB");
        memInfo.put("has_swap", hasSwap);
        
        // 字段计数（用于检测裁剪过的内核）
        int fieldCount = 0;
        String[] lines = rawMeminfo.split("\n");
        for (String line : lines) {
            if (line.contains(":") && !line.trim().isEmpty()) {
                fieldCount++;
            }
        }
        memInfo.put("field_count", fieldCount);
        
        return memInfo;
    }

    /**
     * 映射探针状态
     */
    private static String mapProbeStatus(FileProbe probe) {
        if (probe.accessible && probe.exitCode == 0) {
            return STATUS_OK;
        } else if (probe.exitCode == 1) {
            // Permission Denied
            return STATUS_PERM_DENIED;
        } else if (probe.exitCode > 0) {
            // File not found 或其他错误
            return STATUS_NOT_FOUND;
        } else {
            return STATUS_ERROR;
        }
    }

    /**
     * 构建内核属性
     */
    private static JSONObject buildKernelProperties(ParsedNativeData parsed) throws JSONException {
        JSONObject kernelProps = new JSONObject();
        
        // 内核版本
        if (parsed.kernelRelease != null && !parsed.kernelRelease.isEmpty()) {
            kernelProps.put("uname_release", parsed.kernelRelease);
        }
        
        // 机器架构
        if (parsed.kernelMachine != null && !parsed.kernelMachine.isEmpty()) {
            kernelProps.put("machine", parsed.kernelMachine);
        }
        
        // 页面大小
        if (parsed.pageSize > 0) {
            kernelProps.put("page_size", parsed.pageSize);
        }
        
        // 物理页数
        if (parsed.physPages > 0) {
            kernelProps.put("phys_pages", parsed.physPages);
        }
        
        // CPU 核心数
        if (parsed.cpuCores > 0) {
            kernelProps.put("cpu_cores", parsed.cpuCores);
        }
        
        // Boot ID（系统启动后的唯一标识）
        FileProbe bootIdProbe = parsed.fileProbes.get("/proc/sys/kernel/random/boot_id");
        if (bootIdProbe != null && bootIdProbe.accessible) {
            String bootId = bootIdProbe.content.trim();
            if (!bootId.isEmpty()) {
                // Boot ID 是动态的，只记录格式是否正常
                kernelProps.put("boot_id_format", bootId.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") ? "UUID" : "NON_UUID");
            }
        }
        
        // 熵可用性
        FileProbe entropyProbe = parsed.fileProbes.get("/proc/sys/kernel/random/entropy_avail");
        if (entropyProbe != null && entropyProbe.accessible) {
            try {
                int entropy = Integer.parseInt(entropyProbe.content.trim());
                // 记录熵的范围而不是精确值
                String entropyLevel;
                if (entropy < 100) {
                    entropyLevel = "LOW";
                } else if (entropy < 1000) {
                    entropyLevel = "MEDIUM";
                } else {
                    entropyLevel = "HIGH";
                }
                kernelProps.put("entropy_level", entropyLevel);
            } catch (NumberFormatException e) {
                // 忽略
            }
        }
        
        return kernelProps;
    }

    /**
     * 构建风险标签
     */
    private static JSONArray buildRiskTags(ParsedNativeData parsed) {
        JSONArray riskTags = new JSONArray();
        
        // USB Debug 启用
        String usbConfig = parsed.systemProperties.get("sys.usb.config");
        if (usbConfig != null && usbConfig.contains("adb")) {
            riskTags.put("USB_DEBUG_ENABLED");
        }
        
        // Bootloader 解锁
        String flashLocked = parsed.systemProperties.get("ro.boot.flash.locked");
        if (flashLocked != null && !flashLocked.equals("1")) {
            riskTags.put("BOOTLOADER_UNLOCKED");
        }
        
        // 检测可疑库（从 Zygisk 检测结果推断）
        // 这里可以扩展，检查是否有可疑的挂载点或文件
        
        return riskTags;
    }

    /**
     * SHA256 哈希
     */
    private static String sha256Hash(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.e(TAG, "计算 SHA256 哈希失败", e);
            return "";
        }
    }

    /**
     * 解析后的 Native 数据结构
     */
    private static class ParsedNativeData {
        java.util.Map<String, String> systemProperties = new java.util.HashMap<>();
        java.util.Map<String, FileProbe> fileProbes = new java.util.HashMap<>();
        String kernelRelease = "";
        String kernelMachine = "";
        long pageSize = 0;
        long physPages = 0;
        long totalRamMb = 0;
        int cpuCores = 0;
        String drmDeviceId = "";
    }

    /**
     * 文件探针结构
     */
    private static class FileProbe {
        String path;
        String content;
        int exitCode;
        boolean accessible;
    }
}

