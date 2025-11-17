#include "FingerprintCollector.h"
#include "../collectors/SystemPropertyCollector.h"
#include "../collectors/DRMCollector.h"
#include "../collectors/MacAddressCollector.h"
#include "../collectors/NativeFileCollector.h"
#include "../utils/Log.h"

std::string FingerprintCollector::CollectAllFingerprints() {
    std::string result;
    
    try {
        // 收集系统属性信息
        result += "=== System Properties ===\n";
        result += SystemPropertyCollector::CollectBuildInfo();
        
        // 收集DRM信息
        result += DRMCollector::CollectDrmInfo();
        
        // 记录网络接口信息到日志
        MacAddressCollector::LogAllNetworkInterfaces();
        
        // 收集Native文件指纹
        result += NativeFileCollector::CollectAllNativeFiles();
        
        LOGI("Fingerprint collection completed successfully");
    } catch (const std::exception& e) {
        LOGE("Exception in CollectAllFingerprints: %s", e.what());
        result += "\nError: ";
        result += e.what();
        result += "\n";
    } catch (...) {
        LOGE("Unknown exception in CollectAllFingerprints");
        result += "\nUnknown error occurred\n";
    }
    
    return result;
}

std::string FingerprintCollector::CollectNativeFingerprint() {
    try {
        std::string result = "=== Native Build Info ===\n";
        result += SystemPropertyCollector::CollectBuildInfo();
        result += DRMCollector::CollectDrmInfo();
        result += NativeFileCollector::CollectAllNativeFiles();
        return result;
    } catch (const std::exception& e) {
        LOGE("Exception in CollectNativeFingerprint: %s", e.what());
        return "Error collecting native fingerprint: " + std::string(e.what());
    } catch (...) {
        LOGE("Unknown exception in CollectNativeFingerprint");
        return "Error collecting native fingerprint: Unknown error";
    }
}

