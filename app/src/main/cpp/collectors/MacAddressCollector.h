#ifndef SPHUNTER_MACADDRESSCOLLECTOR_H
#define SPHUNTER_MACADDRESSCOLLECTOR_H

#include <string>
#include <vector>

/**
 * MAC地址收集器
 * 负责收集网络接口的MAC地址和IP地址信息
 */
class MacAddressCollector {
public:
    /**
     * 网络接口信息结构
     */
    struct NetworkInterfaceInfo {
        std::string name;          // 接口名称
        std::string macAddress;    // MAC地址
        std::string ipv4Address;   // IPv4地址
        std::string ipv6Address;   // IPv6地址
    };
    
    /**
     * 收集所有网络接口信息
     * @return 网络接口信息列表
     */
    static std::vector<NetworkInterfaceInfo> CollectNetworkInterfaces();
    
    /**
     * 获取MAC地址（主要用于JNI调用）
     * @return MAC地址字符串
     */
    static std::string GetMacAddress();
    
    /**
     * 打印所有网络接口信息到日志（用于调试）
     */
    static void LogAllNetworkInterfaces();
    
private:
    /**
     * 格式化MAC地址
     * @param addr MAC地址字节数组
     * @param length 字节数组长度
     * @return 格式化的MAC地址字符串（例如：AA:BB:CC:DD:EE:FF）
     */
    static std::string FormatMacAddress(const uint8_t* addr, int length);
    
    /**
     * 使用ioctl获取指定网络接口的MAC地址（libc API备选方案）
     * @param interfaceName 网络接口名称
     * @return MAC地址字符串，失败返回空字符串
     */
    static std::string GetMacAddressViaIoctl(const std::string& interfaceName);
    
    /**
     * 使用ioctl收集所有网络接口的MAC地址（libc API备选方案）
     * @return 网络接口信息列表
     */
    static std::vector<NetworkInterfaceInfo> CollectNetworkInterfacesViaIoctl();
};

#endif // SPHUNTER_MACADDRESSCOLLECTOR_H

