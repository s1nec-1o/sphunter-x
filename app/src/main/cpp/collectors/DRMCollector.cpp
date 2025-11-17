#include "DRMCollector.h"
#include "../utils/Base64Utils.h"
#include <media/NdkMediaDrm.h>

// Widevine UUID: 0xedef8ba979d64aceL, 0xa3c827dcd51d21edL
const uint8_t DRMCollector::WIDEVINE_UUID[] = {
    0xed, 0xef, 0x8b, 0xa9, 0x79, 0xd6, 0x4a, 0xce,
    0xa3, 0xc8, 0x27, 0xdc, 0xd5, 0x1d, 0x21, 0xed
};

std::string DRMCollector::CollectDrmInfo() {
    AMediaDrm* mediaDrm = AMediaDrm_createByUUID(WIDEVINE_UUID);
    if (mediaDrm == nullptr) {
        return "\n=== DRM Info ===\nWidevine DRM not supported on this device\n";
    }
    
    std::string result = "\n=== DRM Info ===\n";
    
    try {
        // 检查是否支持Widevine
        if (!AMediaDrm_isCryptoSchemeSupported(WIDEVINE_UUID, nullptr)) {
            AMediaDrm_release(mediaDrm);
            return result + "Widevine DRM not supported on this device\n";
        }
        
        // 获取deviceUniqueId
        AMediaDrmByteArray byteArray;
        media_status_t status = AMediaDrm_getPropertyByteArray(
            mediaDrm, 
            PROPERTY_DEVICE_UNIQUE_ID, 
            &byteArray
        );
        
        if (status == AMEDIA_OK && byteArray.ptr != nullptr && byteArray.length > 0) {
            // 转换为Base64
            std::string base64 = Base64Utils::Encode(byteArray.ptr, byteArray.length);
            // 转换为十六进制
            std::string hex = Base64Utils::ToHexString(byteArray.ptr, byteArray.length);
            
            result += "MediaDrm Device Unique ID (Base64): " + base64 + "\n";
            result += "MediaDrm Device Unique ID (Hex): " + hex + "\n";
            result += "Length: " + std::to_string(byteArray.length) + " bytes\n";
        } else {
            result += "Failed to get Device Unique ID\n";
            result += "Status: " + std::to_string(status) + "\n";
        }
        
        AMediaDrm_release(mediaDrm);
    } catch (const std::exception& e) {
        result += "Exception occurred: ";
        result += e.what();
        result += "\n";
        AMediaDrm_release(mediaDrm);
    }
    
    return result;
}

