package com.sheep.sphunter.util;

/**
 * 应用常量定义
 */
public class Constants {
    private Constants() {
        // 工具类，禁止实例化
    }

    /**
     * Settings Global 键名
     */
    public static class SettingsKeys {
        public static final String MI_HEALTH_ID = "mi_health_id";
        public static final String GCBOOSTER_UUID = "gcbooster_uuid";
        public static final String KEY_MQS_UUID = "key_mqs_uuid";
        public static final String AD_AAID = "ad_aaid";
        public static final String BLUETOOTH_NAME = "bluetooth_name";
        public static final String BLUETOOTH_ADDRESS = "bluetooth_address";
    }

    /**
     * 系统属性键名
     */
    public static class SystemProperties {
        // USB 相关
        public static final String SYS_USB_CONFIG = "sys.usb.config";
        public static final String SYS_USB_STATE = "sys.usb.state";
        public static final String PERSIST_SYS_USB_CONFIG = "persist.sys.usb.config";
        public static final String PERSIST_SYS_USB_QMMI_FUNC = "persist.sys.usb.qmmi.func";
        public static final String VENDOR_USB_MIMODE = "vendor.usb.mimode";
        public static final String PERSIST_VENDOR_USB_CONFIG = "persist.vendor.usb.config";

        // 安全相关
        public static final String RO_DEBUGGABLE = "ro.debuggable";
        public static final String INIT_SVC_ADBD = "init.svc.adbd";
        public static final String RO_SECURE = "ro.secure";
        public static final String RO_BOOT_FLASH_LOCKED = "ro.boot.flash.locked";
        public static final String SYS_OEM_UNLOCK_ALLOWED = "sys.oem_unlock_allowed";
    }
}

