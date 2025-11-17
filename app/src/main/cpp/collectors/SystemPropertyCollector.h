#ifndef SPHUNTER_SYSTEMPROPERTYCOLLECTOR_H
#define SPHUNTER_SYSTEMPROPERTYCOLLECTOR_H

#include <string>
#include <vector>

/**
 * 系统属性收集器
 * 负责收集Android系统属性信息
 */
class SystemPropertyCollector {
public:
    /**
     * 收集所有系统构建信息
     * @return 格式化的系统信息字符串
     */
    static std::string CollectBuildInfo();
    
    /**
     * 获取单个系统属性
     * @param key 属性键名
     * @param defaultValue 默认值（属性不存在时返回）
     * @return 属性值
     */
    static std::string GetSystemProperty(const char* key, const char* defaultValue = "null");
    
private:
    // USB配置信息
    static std::string CollectUsbConfig();
    
    // 安全相关信息
    static std::string CollectSecurityInfo();
    
    // Build ID相关信息
    static std::string CollectBuildIdInfo();
    
    // SDK版本信息
    static std::string CollectSdkVersion();
    
    // 安全补丁信息
    static std::string CollectSecurityPatch();
    
    // 其他系统信息
    static std::string CollectOtherSystemInfo();
    
    // Build日期UTC信息
    static std::string CollectBuildDateUtc();
    
    // Display ID和Tags
    static std::string CollectDisplayIdAndTags();
    
    // Build Host和User
    static std::string CollectBuildHostAndUser();
    
    // Build版本增量
    static std::string CollectBuildVersionIncremental();
    
    // Build描述
    static std::string CollectBuildDescription();
    
    // Build指纹
    static std::string CollectBuildFingerprint();

    // 其他系统信息
    static std::string CollectOtherSystemProperty();
};

#endif // SPHUNTER_SYSTEMPROPERTYCOLLECTOR_H

