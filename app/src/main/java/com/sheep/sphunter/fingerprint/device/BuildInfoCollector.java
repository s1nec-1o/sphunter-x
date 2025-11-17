package com.sheep.sphunter.fingerprint.device;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

/**
 * Build 信息采集器
 */
public class BuildInfoCollector {
    private final Context context;

    public BuildInfoCollector(@NonNull Context context) {
        this.context = context;
    }

    /**
     * 通过反射获取系统属性（字符串）
     * 注意：某些受限属性可能无法访问，会返回默认值
     *
     * @param key 属性键名
     * @param defaultValue 默认值
     * @return 属性值
     */
    private String getSystemProperty(String key, String defaultValue) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method get = systemProperties.getMethod("get", String.class, String.class);
            String value = (String) get.invoke(null, key, defaultValue);
            // 如果返回的是默认值或空字符串，可能表示访问被拒绝
            if (value == null || value.isEmpty() || value.equals(defaultValue)) {
                return defaultValue;
            }
            return value;
        } catch (Exception e) {
            // 静默处理异常，避免日志污染
            return defaultValue;
        }
    }

    /**
     * 安全地获取系统属性（字符串），优先使用 Android API
     * 对于序列号等敏感属性，优先使用 Build API
     *
     * @param key 属性键名
     * @param defaultValue 默认值
     * @return 属性值
     */
    private String getSystemPropertySafe(String key, String defaultValue) {
        // 对于序列号，优先使用 Build API
        if ("ro.boot.serialno".equals(key) || "ro.serialno".equals(key)) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    String serial = Build.getSerial();
                    if (serial != null && !serial.isEmpty() && !serial.equals("unknown")) {
                        return serial;
                    }
                } else {
                    if (Build.SERIAL != null && !Build.SERIAL.isEmpty() && !Build.SERIAL.equals("unknown")) {
                        return Build.SERIAL;
                    }
                }
            } catch (SecurityException e) {
                // 没有权限，继续尝试系统属性
            } catch (Exception e) {
                // 其他异常，继续尝试系统属性
            }
        }
        // 回退到系统属性
        return getSystemProperty(key, defaultValue);
    }

    /**
     * 通过反射获取系统属性（长整型）
     *
     * @param key 属性键名
     * @param defaultValue 默认值
     * @return 属性值
     */
    private long getSystemPropertyLong(String key, long defaultValue) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getLong = systemProperties.getMethod("getLong", String.class, long.class);
            return (Long) getLong.invoke(null, key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 通过反射获取系统属性（整型）
     *
     * @param key 属性键名
     * @param defaultValue 默认值
     * @return 属性值
     */
    private int getSystemPropertyInt(String key, int defaultValue) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getInt = systemProperties.getMethod("getInt", String.class, int.class);
            return (Integer) getInt.invoke(null, key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 通过反射获取系统属性（布尔型）
     *
     * @param key 属性键名
     * @param defaultValue 默认值
     * @return 属性值
     */
    private boolean getSystemPropertyBoolean(String key, boolean defaultValue) {
        try {
            Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            Method getBoolean = systemProperties.getMethod("getBoolean", String.class, boolean.class);
            return (Boolean) getBoolean.invoke(null, key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 获取 Build 相关的系统属性信息
     *
     * @return 包含所有 Build 相关属性的字符串
     */
    @NonNull
    public String getBuildInfo() {
        StringBuilder result = new StringBuilder();

        // USB 相关配置
        result.append("=== USB Config ===\n");
        result.append("sys.usb.config = ").append(getSystemProperty(com.sheep.sphunter.util.Constants.SystemProperties.SYS_USB_CONFIG, "null")).append("\n");
        result.append("sys.usb.state = ").append(getSystemProperty(com.sheep.sphunter.util.Constants.SystemProperties.SYS_USB_STATE, "null")).append("\n");
        result.append("persist.sys.usb.config = ").append(getSystemProperty(com.sheep.sphunter.util.Constants.SystemProperties.PERSIST_SYS_USB_CONFIG, "null")).append("\n");
        result.append("persist.sys.usb.qmmi.func = ").append(getSystemProperty(com.sheep.sphunter.util.Constants.SystemProperties.PERSIST_SYS_USB_QMMI_FUNC, "null")).append("\n");

        String vendorUsbMimode = getSystemProperty(com.sheep.sphunter.util.Constants.SystemProperties.VENDOR_USB_MIMODE, null);
        if (vendorUsbMimode != null) {
            result.append("vendor.usb.mimode = ").append(vendorUsbMimode).append("\n");
        }
        String persistVendorUsbConfig = getSystemProperty(com.sheep.sphunter.util.Constants.SystemProperties.PERSIST_VENDOR_USB_CONFIG, null);
        if (persistVendorUsbConfig != null) {
            result.append("persist.vendor.usb.config = ").append(persistVendorUsbConfig).append("\n");
        }

        // 安全相关
        result.append("\n=== Security ===\n");
        result.append("ro.debuggable = ").append(getSystemProperty(com.sheep.sphunter.util.Constants.SystemProperties.RO_DEBUGGABLE, "null")).append("\n");
        result.append("init.svc.adbd = ").append(getSystemProperty(com.sheep.sphunter.util.Constants.SystemProperties.INIT_SVC_ADBD, "null")).append("\n");
        result.append("ro.secure = ").append(getSystemProperty(com.sheep.sphunter.util.Constants.SystemProperties.RO_SECURE, "null")).append("\n");
        result.append("ro.boot.flash.locked = ").append(getSystemProperty(com.sheep.sphunter.util.Constants.SystemProperties.RO_BOOT_FLASH_LOCKED, "null")).append("\n");
        result.append("sys.oem_unlock_allowed = ").append(getSystemProperty(com.sheep.sphunter.util.Constants.SystemProperties.SYS_OEM_UNLOCK_ALLOWED, "null")).append("\n");

        // Build ID 相关
        result.append("\n=== Build ID ===\n");
        appendBuildProperty(result, "ro.build.id");
        appendBuildProperty(result, "ro.build.build.id");
        appendBuildProperty(result, "ro.bootimage.build.id");
        appendBuildProperty(result, "ro.odm.build.id");
        appendBuildProperty(result, "ro.product.build.id");
        appendBuildProperty(result, "ro.system_ext.build.id");
        appendBuildProperty(result, "ro.system.build.id");
        appendBuildProperty(result, "ro.vendor.build.id");

        // 安全补丁
        result.append("\n=== Security Patch ===\n");
        result.append("ro.build.version.security_patch = ").append(getSystemProperty("ro.build.version.security_patch", "null")).append("\n");

        // 其他系统信息
        result.append("\n=== Other System Info ===\n");
        result.append("ro.boot.vbmeta.digest = ").append(getSystemProperty("ro.boot.vbmeta.digest", "null")).append("\n");
        result.append("ro.netflix.bsp_rev = ").append(getSystemProperty("ro.netflix.bsp_rev", "null")).append("\n");
        result.append("gsm.version.baseband = ").append(getSystemProperty("gsm.version.baseband", "null")).append("\n");

        // Build Date UTC
        result.append("\n=== Build Date UTC ===\n");
        appendBuildProperty(result, "ro.build.date.utc");
        appendBuildProperty(result, "ro.build.build.date.utc");
        appendBuildProperty(result, "ro.bootimage.build.date.utc");
        appendBuildProperty(result, "ro.odm.build.date.utc");
        appendBuildProperty(result, "ro.product.build.date.utc");
        appendBuildProperty(result, "ro.system_ext.build.date.utc");
        appendBuildProperty(result, "ro.system.build.date.utc");
        appendBuildProperty(result, "ro.vendor.build.date.utc");

        // Display ID 和 Tags
        result.append("\n=== Display ID and Tags ===\n");
        result.append("ro.build.display.id = ").append(getSystemProperty("ro.build.display.id", "null")).append("\n");
        appendBuildProperty(result, "ro.build.tags");
        appendBuildProperty(result, "ro.build.build.tags");
        appendBuildProperty(result, "ro.bootimage.build.tags");
        appendBuildProperty(result, "ro.odm.build.tags");
        appendBuildProperty(result, "ro.product.build.tags");
        appendBuildProperty(result, "ro.system_ext.build.tags");
        appendBuildProperty(result, "ro.system.build.tags");
        appendBuildProperty(result, "ro.vendor.build.tags");

        // Build Host 和 User
        result.append("\n=== Build Host and User ===\n");
        result.append("ro.build.host = ").append(getSystemProperty("ro.build.host", "null")).append("\n");
        result.append("ro.build.user = ").append(getSystemProperty("ro.build.user", "null")).append("\n");
        result.append("ro.config.ringtone = ").append(getSystemProperty("ro.config.ringtone", "null")).append("\n");
        result.append("ro.miui.ui.version.name = ").append(getSystemProperty("ro.miui.ui.version.name", "null")).append("\n");

        // Build Version Incremental
        result.append("\n=== Build Version Incremental ===\n");
        appendBuildProperty(result, "ro.build.version.incremental");
        appendBuildProperty(result, "ro.build.build.version.incremental");
        appendBuildProperty(result, "ro.bootimage.build.version.incremental");
        appendBuildProperty(result, "ro.odm.build.version.incremental");
        appendBuildProperty(result, "ro.product.build.version.incremental");
        appendBuildProperty(result, "ro.system_ext.build.version.incremental");
        appendBuildProperty(result, "ro.system.build.version.incremental");
        appendBuildProperty(result, "ro.vendor.build.version.incremental");

        // Build Description
        result.append("\n=== Build Description ===\n");
        result.append("ro.build.description = ").append(getSystemProperty("ro.build.description", "null")).append("\n");

        // Build Fingerprint
        result.append("\n=== Build Fingerprint ===\n");
        appendBuildProperty(result, "ro.build.fingerprint");
        appendBuildProperty(result, "ro.build.build.fingerprint");
        appendBuildProperty(result, "ro.bootimage.build.fingerprint");
        appendBuildProperty(result, "ro.odm.build.fingerprint");
        appendBuildProperty(result, "ro.product.build.fingerprint");
        appendBuildProperty(result, "ro.system_ext.build.fingerprint");
        appendBuildProperty(result, "ro.system.build.fingerprint");
        appendBuildProperty(result, "ro.vendor.build.fingerprint");

        // 序列号和硬件信息
        result.append("\n=== Serial Number & Hardware ===\n");
        // 使用安全方法获取序列号，优先使用 Build API，避免访问受限的系统属性
        String bootSerial = getSystemPropertySafe("ro.boot.serialno", "null");
        String serial = getSystemPropertySafe("ro.serialno", "null");
        result.append("ro.boot.serialno = ").append(bootSerial).append("\n");
        result.append("ro.serialno = ").append(serial).append("\n");
        result.append("ro.boot.hardware = ").append(getSystemProperty("ro.boot.hardware", "null")).append("\n");
        result.append("ro.hardware = ").append(getSystemProperty("ro.hardware", "null")).append("\n");

        // CPU ABI 信息
        result.append("\n=== CPU ABI ===\n");
        result.append("ro.product.cpu.abilist = ").append(getSystemProperty("ro.product.cpu.abilist", "null")).append("\n");
        result.append("ro.product.cpu.abilist32 = ").append(getSystemProperty("ro.product.cpu.abilist32", "null")).append("\n");
        result.append("ro.product.cpu.abilist64 = ").append(getSystemProperty("ro.product.cpu.abilist64", "null")).append("\n");

        return result.toString();
    }

    private void appendBuildProperty(StringBuilder result, String key) {
        result.append(key).append(" = ").append(getSystemProperty(key, "null")).append("\n");
    }
}

