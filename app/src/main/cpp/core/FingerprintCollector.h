#ifndef SPHUNTER_FINGERPRINTCOLLECTOR_H
#define SPHUNTER_FINGERPRINTCOLLECTOR_H

#include <string>

/**
 * 指纹收集器主类
 * 负责协调各个子收集器并生成最终的指纹信息
 */
class FingerprintCollector {
public:
    /**
     * 收集所有指纹信息
     * @return 完整的指纹信息字符串
     */
    static std::string CollectAllFingerprints();
    
    /**
     * 收集C层指纹信息（主要用于JNI调用）
     * @return C层指纹信息字符串
     */
    static std::string CollectNativeFingerprint();
};

#endif // SPHUNTER_FINGERPRINTCOLLECTOR_H

