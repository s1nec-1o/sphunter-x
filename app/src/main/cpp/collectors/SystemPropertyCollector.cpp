#include "SystemPropertyCollector.h"
#include <sys/system_properties.h>

std::string SystemPropertyCollector::GetSystemProperty(const char* key, const char* defaultValue) {
    char value[PROP_VALUE_MAX] = {0};
    int len = __system_property_get(key, value);
    if (len > 0) {
        return std::string(value);
    }
    return defaultValue != nullptr ? std::string(defaultValue) : std::string("null");
}

std::string SystemPropertyCollector::CollectOtherSystemProperty() {
    std::string result = "\n=== Other System Property ===\n";
    result += "ro.board.platform = " + GetSystemProperty("ro.board.platform") + "\n";
    result += "ro.product.cpu.abi = " + GetSystemProperty("ro.product.cpu.abi") + "\n";
    result += "ro.boot.verifiedbootstate = " + GetSystemProperty("ro.boot.verifiedbootstate") + "\n";
    result += "ro.boot.vbmeta.device_state = " + GetSystemProperty("ro.boot.vbmeta.device_state") + "\n";
    result += "ro.treble.enabled = " + GetSystemProperty("ro.treble.enabled") + "\n";
    return result;
}

std::string SystemPropertyCollector::CollectUsbConfig() {
    std::string result = "=== USB Config ===\n";
    result += "sys.usb.config = " + GetSystemProperty("sys.usb.config") + "\n";
    result += "sys.usb.state = " + GetSystemProperty("sys.usb.state") + "\n";
    result += "persist.sys.usb.config = " + GetSystemProperty("persist.sys.usb.config") + "\n";
    result += "persist.sys.usb.qmmi.func = " + GetSystemProperty("persist.sys.usb.qmmi.func") + "\n";
    
    std::string vendorUsbMimode = GetSystemProperty("vendor.usb.mimode");
    if (vendorUsbMimode != "null") {
        result += "vendor.usb.mimode = " + vendorUsbMimode + "\n";
    }
    
    std::string persistVendorUsbConfig = GetSystemProperty("persist.vendor.usb.config");
    if (persistVendorUsbConfig != "null") {
        result += "persist.vendor.usb.config = " + persistVendorUsbConfig + "\n";
    }
    
    return result;
}

std::string SystemPropertyCollector::CollectSecurityInfo() {
    std::string result = "\n=== Security ===\n";
    result += "ro.debuggable = " + GetSystemProperty("ro.debuggable") + "\n";
    result += "init.svc.adbd = " + GetSystemProperty("init.svc.adbd") + "\n";
    result += "ro.secure = " + GetSystemProperty("ro.secure") + "\n";
    result += "ro.boot.flash.locked = " + GetSystemProperty("ro.boot.flash.locked") + "\n";
    result += "sys.oem_unlock_allowed = " + GetSystemProperty("sys.oem_unlock_allowed") + "\n";
    return result;
}

std::string SystemPropertyCollector::CollectBuildIdInfo() {
    std::string result = "\n=== Build ID ===\n";
    result += "ro.build.id = " + GetSystemProperty("ro.build.id") + "\n";
    result += "ro.build.build.id = " + GetSystemProperty("ro.build.build.id") + "\n";
    result += "ro.bootimage.build.id = " + GetSystemProperty("ro.bootimage.build.id") + "\n";
    result += "ro.odm.build.id = " + GetSystemProperty("ro.odm.build.id") + "\n";
    result += "ro.product.build.id = " + GetSystemProperty("ro.product.build.id") + "\n";
    result += "ro.system_ext.build.id = " + GetSystemProperty("ro.system_ext.build.id") + "\n";
    result += "ro.system.build.id = " + GetSystemProperty("ro.system.build.id") + "\n";
    result += "ro.vendor.build.id = " + GetSystemProperty("ro.vendor.build.id") + "\n";
    return result;
}

std::string SystemPropertyCollector::CollectSdkVersion() {
    std::string result = "\n=== SDK Version ===\n";
    result += "ro.build.version.sdk = " + GetSystemProperty("ro.build.version.sdk") + "\n";
    return result;
}

std::string SystemPropertyCollector::CollectSecurityPatch() {
    std::string result = "\n=== Security Patch ===\n";
    result += "ro.build.version.security_patch = " + GetSystemProperty("ro.build.version.security_patch") + "\n";
    return result;
}

std::string SystemPropertyCollector::CollectOtherSystemInfo() {
    std::string result = "\n=== Other System Info ===\n";
    result += "ro.boot.vbmeta.digest = " + GetSystemProperty("ro.boot.vbmeta.digest") + "\n";
    result += "ro.netflix.bsp_rev = " + GetSystemProperty("ro.netflix.bsp_rev") + "\n";
    result += "gsm.version.baseband = " + GetSystemProperty("gsm.version.baseband") + "\n";
    return result;
}

std::string SystemPropertyCollector::CollectBuildDateUtc() {
    std::string result = "\n=== Build Date UTC ===\n";
    result += "ro.build.date.utc = " + GetSystemProperty("ro.build.date.utc") + "\n";
    result += "ro.build.build.date.utc = " + GetSystemProperty("ro.build.build.date.utc") + "\n";
    result += "ro.bootimage.build.date.utc = " + GetSystemProperty("ro.bootimage.build.date.utc") + "\n";
    result += "ro.odm.build.date.utc = " + GetSystemProperty("ro.odm.build.date.utc") + "\n";
    result += "ro.product.build.date.utc = " + GetSystemProperty("ro.product.build.date.utc") + "\n";
    result += "ro.system_ext.build.date.utc = " + GetSystemProperty("ro.system_ext.build.date.utc") + "\n";
    result += "ro.system.build.date.utc = " + GetSystemProperty("ro.system.build.date.utc") + "\n";
    result += "ro.vendor.build.date.utc = " + GetSystemProperty("ro.vendor.build.date.utc") + "\n";
    return result;
}

std::string SystemPropertyCollector::CollectDisplayIdAndTags() {
    std::string result = "\n=== Display ID and Tags ===\n";
    result += "ro.build.display.id = " + GetSystemProperty("ro.build.display.id") + "\n";
    result += "ro.build.tags = " + GetSystemProperty("ro.build.tags") + "\n";
    result += "ro.build.build.tags = " + GetSystemProperty("ro.build.build.tags") + "\n";
    result += "ro.bootimage.build.tags = " + GetSystemProperty("ro.bootimage.build.tags") + "\n";
    result += "ro.odm.build.tags = " + GetSystemProperty("ro.odm.build.tags") + "\n";
    result += "ro.product.build.tags = " + GetSystemProperty("ro.product.build.tags") + "\n";
    result += "ro.system_ext.build.tags = " + GetSystemProperty("ro.system_ext.build.tags") + "\n";
    result += "ro.system.build.tags = " + GetSystemProperty("ro.system.build.tags") + "\n";
    result += "ro.vendor.build.tags = " + GetSystemProperty("ro.vendor.build.tags") + "\n";
    return result;
}

std::string SystemPropertyCollector::CollectBuildHostAndUser() {
    std::string result = "\n=== Build Host and User ===\n";
    result += "ro.build.host = " + GetSystemProperty("ro.build.host") + "\n";
    result += "ro.build.user = " + GetSystemProperty("ro.build.user") + "\n";
    result += "ro.config.ringtone = " + GetSystemProperty("ro.config.ringtone") + "\n";
    result += "ro.miui.ui.version.name = " + GetSystemProperty("ro.miui.ui.version.name") + "\n";
    return result;
}

std::string SystemPropertyCollector::CollectBuildVersionIncremental() {
    std::string result = "\n=== Build Version Incremental ===\n";
    result += "ro.build.version.incremental = " + GetSystemProperty("ro.build.version.incremental") + "\n";
    result += "ro.build.build.version.incremental = " + GetSystemProperty("ro.build.build.version.incremental") + "\n";
    result += "ro.bootimage.build.version.incremental = " + GetSystemProperty("ro.bootimage.build.version.incremental") + "\n";
    result += "ro.odm.build.version.incremental = " + GetSystemProperty("ro.odm.build.version.incremental") + "\n";
    result += "ro.product.build.version.incremental = " + GetSystemProperty("ro.product.build.version.incremental") + "\n";
    result += "ro.system_ext.build.version.incremental = " + GetSystemProperty("ro.system_ext.build.version.incremental") + "\n";
    result += "ro.system.build.version.incremental = " + GetSystemProperty("ro.system.build.version.incremental") + "\n";
    result += "ro.vendor.build.version.incremental = " + GetSystemProperty("ro.vendor.build.version.incremental") + "\n";
    return result;
}

std::string SystemPropertyCollector::CollectBuildDescription() {
    std::string result = "\n=== Build Description ===\n";
    result += "ro.build.description = " + GetSystemProperty("ro.build.description") + "\n";
    return result;
}

std::string SystemPropertyCollector::CollectBuildFingerprint() {
    std::string result = "\n=== Build Fingerprint ===\n";
    result += "ro.build.fingerprint = " + GetSystemProperty("ro.build.fingerprint") + "\n";
    result += "ro.build.build.fingerprint = " + GetSystemProperty("ro.build.build.fingerprint") + "\n";
    result += "ro.bootimage.build.fingerprint = " + GetSystemProperty("ro.bootimage.build.fingerprint") + "\n";
    result += "ro.odm.build.fingerprint = " + GetSystemProperty("ro.odm.build.fingerprint") + "\n";
    result += "ro.product.build.fingerprint = " + GetSystemProperty("ro.product.build.fingerprint") + "\n";
    result += "ro.system_ext.build.fingerprint = " + GetSystemProperty("ro.system_ext.build.fingerprint") + "\n";
    result += "ro.system.build.fingerprint = " + GetSystemProperty("ro.system.build.fingerprint") + "\n";
    result += "ro.vendor.build.fingerprint = " + GetSystemProperty("ro.vendor.build.fingerprint") + "\n";
    return result;
}

std::string SystemPropertyCollector::CollectBuildInfo() {
    std::string result;

    result += CollectUsbConfig();
    result += CollectSecurityInfo();
    result += CollectBuildIdInfo();
    result += CollectSdkVersion();
    result += CollectSecurityPatch();
    result += CollectOtherSystemInfo();
    result += CollectBuildDateUtc();
    result += CollectDisplayIdAndTags();
    result += CollectBuildHostAndUser();
    result += CollectBuildVersionIncremental();
    result += CollectBuildDescription();
    result += CollectBuildFingerprint();
    result += CollectOtherSystemProperty();

    
    return result;
}

