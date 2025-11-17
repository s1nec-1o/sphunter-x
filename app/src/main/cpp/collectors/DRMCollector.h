#ifndef SPHUNTER_DRMCOLLECTOR_H
#define SPHUNTER_DRMCOLLECTOR_H

#include <string>

/**
 * DRM信息收集器
 * 负责收集Widevine DRM相关信息
 */
class DRMCollector {
public:
    /**
     * 收集DRM信息
     * @return 格式化的DRM信息字符串
     */
    static std::string CollectDrmInfo();
    
private:
    // Widevine UUID
    static const uint8_t WIDEVINE_UUID[];
};

#endif // SPHUNTER_DRMCOLLECTOR_H

