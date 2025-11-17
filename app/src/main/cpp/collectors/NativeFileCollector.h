#ifndef SPHUNTER_NATIVEFILECOLLECTOR_H
#define SPHUNTER_NATIVEFILECOLLECTOR_H

#include <string>

/**
 * Native文件指纹收集器
 * 负责通过 cat 命令收集系统文件指纹信息
 * 包括：核心硬件、环境检测、挂载与进程三大类
 */
class NativeFileCollector {
public:
    /**
     * 文件指纹结构体
     */
    struct FileFingerprint {
        std::string path;           // 文件路径
        std::string content;         // 文件内容
        int exit_code;              // cat 命令的退出码
        bool accessible;            // 文件是否可访问
        
        FileFingerprint() : exit_code(-1), accessible(false) {}
    };
    
    /**
     * 收集所有Native文件指纹
     * @return 格式化的指纹信息字符串
     */
    static std::string CollectAllNativeFiles();
    
    /**
     * 收集单个文件的指纹
     * @param filePath 文件路径
     * @return FileFingerprint 结构体
     */
    static FileFingerprint CollectFileFingerprint(const char* filePath);
    
    /**
     * 收集核心硬件与内核特征
     * @return 格式化的硬件信息字符串
     */
    static std::string CollectHardwareInfo();
    
    /**
     * 收集环境与安全检测信息
     * @return 格式化的环境信息字符串
     */
    static std::string CollectEnvironmentInfo();
    
    /**
     * 收集挂载点与输入设备信息
     * @return 格式化的挂载信息字符串
     */
    static std::string CollectMountInfo();
    
    /**
     * 使用 uname() 系统调用收集内核信息（绕过 cat 限制）
     * @return 格式化的内核信息字符串
     */
    static std::string CollectKernelInfoWithUname();
    
    /**
     * 使用 sysconf() 系统调用收集系统配置信息
     * @return 格式化的系统配置信息字符串
     */
    static std::string CollectSystemConfigWithSysconf();
    
    /**
     * 检测 Zygisk 注入
     * 遍历 /proc/self/maps 查找异常动态库
     * @return 格式化的 Zygisk 检测结果字符串
     */
    static std::string DetectZygiskInjection();
    
private:
    /**
     * 使用 cat 命令读取文件内容
     * @param filePath 文件路径
     * @param fingerprint 输出参数，存储指纹信息
     */
    static void ReadFileWithCat(const char* filePath, FileFingerprint& fingerprint);
    
    /**
     * 格式化指纹信息为字符串
     * @param fingerprint 指纹结构体
     * @return 格式化的字符串
     */
    static std::string FormatFingerprint(const FileFingerprint& fingerprint);
    
    /**
     * 去除字符串首尾空白字符
     * @param str 输入字符串
     * @return 处理后的字符串
     */
    static std::string TrimString(const std::string& str);
    
    /**
     * 检查路径是否为系统库路径
     * @param path 库路径
     * @return true 如果是系统库路径
     */
    static bool IsSystemLibraryPath(const std::string& path);
    
    /**
     * 检查库路径是否可疑（可能为 Zygisk 注入）
     * @param path 库路径
     * @return true 如果可疑
     */
    static bool IsSuspiciousLibraryPath(const std::string& path);
};

#endif // SPHUNTER_NATIVEFILECOLLECTOR_H

