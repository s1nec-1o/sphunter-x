#include "NativeFileCollector.h"
#include "../utils/Log.h"
#include <cstdio>
#include <cstring>
#include <sstream>
#include <algorithm>
#include <cctype>
#include <sys/wait.h>
#include <sys/utsname.h>
#include <unistd.h>
#include <fstream>
#include <vector>
#include <cerrno>

// 核心硬件与内核特征文件路径
static const char* HARDWARE_FILES[] = {
    "/proc/cpuinfo",
    "/proc/version",
    "/proc/meminfo",
    "/proc/iomem",
    "/proc/misc",
    "/sys/devices/soc0/family",
    "/sys/devices/soc0/machine",
    "/sys/class/power_supply/battery/capacity",
    "/sys/class/power_supply/battery/status"
};

static const int HARDWARE_FILES_COUNT = sizeof(HARDWARE_FILES) / sizeof(HARDWARE_FILES[0]);

// 环境与安全检测文件路径
static const char* ENVIRONMENT_FILES[] = {
    "/proc/sys/kernel/random/boot_id",
    "/proc/sys/kernel/osrelease",
    "/sys/fs/selinux/enforce",
    "/proc/sys/kernel/random/entropy_avail",
    "/proc/uptime",
    "/sys/class/thermal/thermal_zone0/temp",
    "/proc/sys/vm/overcommit_memory"
};

static const int ENVIRONMENT_FILES_COUNT = sizeof(ENVIRONMENT_FILES) / sizeof(ENVIRONMENT_FILES[0]);

// 挂载点与输入设备文件路径
static const char* MOUNT_FILES[] = {
    "/proc/self/mountinfo",
    "/proc/mounts",
    "/proc/filesystems",
    "/proc/bus/input/devices",
    "/proc/net/unix"
};

static const int MOUNT_FILES_COUNT = sizeof(MOUNT_FILES) / sizeof(MOUNT_FILES[0]);

void NativeFileCollector::ReadFileWithCat(const char* filePath, FileFingerprint& fingerprint) {
    fingerprint.path = filePath;
    fingerprint.content = "";
    fingerprint.accessible = false;
    fingerprint.exit_code = -1;
    
    if (!filePath) {
        fingerprint.exit_code = -1;
        return;
    }
    
    // 构建 cat 命令
    std::string cmd = "cat ";
    cmd += filePath;
    cmd += " 2>&1";  // 将错误输出重定向到标准输出
    
    FILE* pipe = popen(cmd.c_str(), "r");
    if (!pipe) {
        fingerprint.exit_code = -1;
        LOGW("Failed to open pipe for: %s", filePath);
        return;
    }
    
    // 读取文件内容
    char buffer[4096];
    std::string result = "";
    while (fgets(buffer, sizeof(buffer), pipe) != nullptr) {
        result += buffer;
    }
    
    // 获取退出码
    int returnCode = pclose(pipe);
    
    // 处理退出码：pclose 返回的是 wait status，需要提取实际退出码
    if (WIFEXITED(returnCode)) {
        fingerprint.exit_code = WEXITSTATUS(returnCode);
    } else {
        fingerprint.exit_code = returnCode;
    }
    
    // 设置内容和可访问性
    fingerprint.content = result;
    fingerprint.accessible = (fingerprint.exit_code == 0);
    
    // 如果退出码不为0，记录日志
    if (fingerprint.exit_code != 0) {
        LOGD("File access failed: %s, exit_code: %d", filePath, fingerprint.exit_code);
    }
}

std::string NativeFileCollector::TrimString(const std::string& str) {
    if (str.empty()) {
        return str;
    }
    
    size_t start = str.find_first_not_of(" \t\n\r");
    if (start == std::string::npos) {
        return "";
    }
    
    size_t end = str.find_last_not_of(" \t\n\r");
    return str.substr(start, end - start + 1);
}

std::string NativeFileCollector::FormatFingerprint(const FileFingerprint& fingerprint) {
    std::ostringstream oss;
    oss << "Path: " << fingerprint.path << "\n";
    oss << "Exit Code: " << fingerprint.exit_code << "\n";
    oss << "Accessible: " << (fingerprint.accessible ? "true" : "false") << "\n";
    
    std::string trimmedContent = TrimString(fingerprint.content);
    if (trimmedContent.empty()) {
        oss << "Content: [EMPTY]\n";
    } else {
        // 限制内容长度，避免输出过长
        if (trimmedContent.length() > 2048) {
            oss << "Content (truncated): " << trimmedContent.substr(0, 2048) << "...\n";
        } else {
            oss << "Content: " << trimmedContent << "\n";
        }
    }
    oss << "---\n";
    return oss.str();
}

NativeFileCollector::FileFingerprint NativeFileCollector::CollectFileFingerprint(const char* filePath) {
    FileFingerprint fingerprint;
    ReadFileWithCat(filePath, fingerprint);
    return fingerprint;
}

std::string NativeFileCollector::CollectHardwareInfo() {
    std::ostringstream result;
    result << "\n=== 核心硬件与内核特征 (Hardware & Kernel) ===\n\n";
    
    for (int i = 0; i < HARDWARE_FILES_COUNT; i++) {
        FileFingerprint fingerprint = CollectFileFingerprint(HARDWARE_FILES[i]);
        result << FormatFingerprint(fingerprint);
    }
    
    return result.str();
}

std::string NativeFileCollector::CollectEnvironmentInfo() {
    std::ostringstream result;
    result << "\n=== 环境与安全检测 (Environment & Security) ===\n\n";
    
    for (int i = 0; i < ENVIRONMENT_FILES_COUNT; i++) {
        FileFingerprint fingerprint = CollectFileFingerprint(ENVIRONMENT_FILES[i]);
        result << FormatFingerprint(fingerprint);
    }
    
    return result.str();
}

std::string NativeFileCollector::CollectMountInfo() {
    std::ostringstream result;
    result << "\n=== 挂载点与输入设备 (Mounts & Inputs) ===\n\n";
    
    for (int i = 0; i < MOUNT_FILES_COUNT; i++) {
        FileFingerprint fingerprint = CollectFileFingerprint(MOUNT_FILES[i]);
        result << FormatFingerprint(fingerprint);
    }
    
    return result.str();
}



std::string NativeFileCollector::CollectKernelInfoWithUname() {
    std::ostringstream result;
    result << "\n=== 内核信息 (Kernel Info via uname) ===\n\n";
    
    struct utsname info;
    if (uname(&info) == 0) {
        result << "System Name: " << info.sysname << "\n";
        result << "Node Name: " << info.nodename << "\n";
        result << "Release: " << info.release << "\n";
        result << "Version: " << info.version << "\n";
        result << "Machine: " << info.machine << "\n";
        
        // 在某些系统上，domainname 可能可用
        #ifdef _GNU_SOURCE
        result << "Domain Name: " << info.domainname << "\n";
        #endif
        result << "---\n";
    } else {
        result << "Failed to get uname info (errno: " << errno << ")\n";
        result << "---\n";
        LOGE("uname() failed with errno: %d", errno);
    }
    
    return result.str();
}

std::string NativeFileCollector::CollectSystemConfigWithSysconf() {
    std::ostringstream result;
    result << "\n=== 系统配置信息 (System Config via sysconf) ===\n\n";
    
    // CPU 核心数
    long nproc = sysconf(_SC_NPROCESSORS_ONLN);
    if (nproc > 0) {
        result << "CPU Cores (Online): " << nproc << "\n";
    } else {
        result << "CPU Cores (Online): [UNAVAILABLE]\n";
    }
    
    long nproc_conf = sysconf(_SC_NPROCESSORS_CONF);
    if (nproc_conf > 0) {
        result << "CPU Cores (Configured): " << nproc_conf << "\n";
    } else {
        result << "CPU Cores (Configured): [UNAVAILABLE]\n";
    }
    
    // 页大小
    long page_size = sysconf(_SC_PAGESIZE);
    if (page_size > 0) {
        result << "Page Size: " << page_size << " bytes\n";
    } else {
        result << "Page Size: [UNAVAILABLE]\n";
    }
    
    // 时钟频率 (每秒时钟滴答数)
    long clk_tck = sysconf(_SC_CLK_TCK);
    if (clk_tck > 0) {
        result << "Clock Ticks per Second: " << clk_tck << "\n";
    } else {
        result << "Clock Ticks per Second: [UNAVAILABLE]\n";
    }
    
    // 物理页数
    long phys_pages = sysconf(_SC_PHYS_PAGES);
    if (phys_pages > 0) {
        result << "Physical Pages: " << phys_pages << "\n";
        if (page_size > 0) {
            result << "Total Physical Memory: " << (phys_pages * page_size / 1024 / 1024) << " MB\n";
        }
    } else {
        result << "Physical Pages: [UNAVAILABLE]\n";
    }
    
    // 可用页数
    long avphys_pages = sysconf(_SC_AVPHYS_PAGES);
    if (avphys_pages > 0) {
        result << "Available Physical Pages: " << avphys_pages << "\n";
        if (page_size > 0) {
            result << "Available Physical Memory: " << (avphys_pages * page_size / 1024 / 1024) << " MB\n";
        }
    } else {
        result << "Available Physical Pages: [UNAVAILABLE]\n";
    }
    
    result << "---\n";
    return result.str();
}

bool NativeFileCollector::IsSystemLibraryPath(const std::string& path) {
    if (path.empty()) {
        return false;
    }
    
    // 系统库路径前缀
    const char* system_prefixes[] = {
        "/system/",
        "/vendor/",
        "/apex/",
        "/product/",
        "/system_ext/",
        "/odm/",
        "/data/dalvik-cache/",
        "/data/app/",
        "/data/data/",
        "[anon:",
        "[stack]",
        "[vdso]",
        "[vsyscall]"
    };
    
    for (const char* prefix : system_prefixes) {
        if (path.find(prefix) == 0) {
            return true;
        }
    }
    
    return false;
}

bool NativeFileCollector::IsSuspiciousLibraryPath(const std::string& path) {
    if (path.empty()) {
        return false;
    }
    
    // 可疑关键词
    const char* suspicious_keywords[] = {
        "magisk",
        "zygisk",
        "riru",
        "lsposed",
        "edxposed",
        "xposed",
        "/data/local/tmp/",
        "/data/adb/",
        "/sbin/",
        "/dev/"
    };
    
    std::string lower_path = path;
    std::transform(lower_path.begin(), lower_path.end(), lower_path.begin(), ::tolower);
    
    for (const char* keyword : suspicious_keywords) {
        if (lower_path.find(keyword) != std::string::npos) {
            return true;
        }
    }
    
    // 检查是否在非标准路径下的 .so 文件
    if (path.find(".so") != std::string::npos) {
        // 如果包含 .so 但不是系统库路径，且不在标准应用路径下，则可疑
        if (!IsSystemLibraryPath(path)) {
            // 排除一些正常的匿名映射
            if (path.find("[") == 0) {
                return false;
            }
            return true;
        }
    }
    
    return false;
}

std::string NativeFileCollector::DetectZygiskInjection() {
    std::ostringstream result;
    result << "\n=== Zygisk 注入检测 (Zygisk Injection Detection) ===\n\n";
    
    std::ifstream maps_file("/proc/self/maps");
    if (!maps_file.is_open()) {
        result << "Failed to open /proc/self/maps\n";
        result << "---\n";
        LOGE("Failed to open /proc/self/maps");
        return result.str();
    }
    
    std::vector<std::string> suspicious_libs;
    std::vector<std::string> all_loaded_libs;
    std::string line;
    int total_mappings = 0;
    int library_mappings = 0;
    
    while (std::getline(maps_file, line)) {
        total_mappings++;
        
        // 解析 maps 文件格式: address perms offset dev inode pathname
        // 查找包含库路径的行（通常在第6个字段之后）
        size_t last_space = line.find_last_of(' ');
        if (last_space == std::string::npos) {
            continue;
        }
        
        std::string path = line.substr(last_space + 1);
        path = TrimString(path);
        
        // 跳过空路径和匿名映射（除非是可疑的）
        if (path.empty() || path[0] == '[') {
            continue;
        }
        
        // 只关注 .so 文件或可执行文件
        if (path.find(".so") != std::string::npos || 
            path.find("/bin/") != std::string::npos ||
            path.find("/lib/") != std::string::npos) {
            library_mappings++;
            all_loaded_libs.push_back(path);
            
            // 检查是否可疑
            if (IsSuspiciousLibraryPath(path)) {
                suspicious_libs.push_back(path);
            }
        }
    }
    
    maps_file.close();
    
    // 输出统计信息
    result << "Total Mappings: " << total_mappings << "\n";
    result << "Library Mappings: " << library_mappings << "\n";
    result << "Suspicious Libraries Found: " << suspicious_libs.size() << "\n\n";
    
    // 输出可疑库列表
    if (suspicious_libs.empty()) {
        result << "No suspicious libraries detected.\n";
    } else {
        result << "Suspicious Libraries:\n";
        for (const auto& lib : suspicious_libs) {
            result << "  - " << lib << "\n";
        }
    }
    
    result << "\n";
    
    // 输出所有非系统库（用于调试）
    result << "Non-System Libraries (first 20):\n";
    int count = 0;
    for (const auto& lib : all_loaded_libs) {
        if (!IsSystemLibraryPath(lib) && count < 20) {
            result << "  - " << lib << "\n";
            count++;
        }
    }
    if (count == 0) {
        result << "  [None found]\n";
    }
    
    result << "---\n";
    
    if (!suspicious_libs.empty()) {
        LOGW("Zygisk injection detected! Found %zu suspicious libraries", suspicious_libs.size());
    } else {
        LOGI("No Zygisk injection detected");
    }
    
    return result.str();
}

std::string NativeFileCollector::CollectAllNativeFiles() {
    std::ostringstream result;
    
    try {
        LOGI("Starting Native file fingerprint collection");
        
        // 收集核心硬件信息
        result << CollectHardwareInfo();
        
        // 收集环境信息
        result << CollectEnvironmentInfo();
        
        // 收集挂载信息
        result << CollectMountInfo();
        
        // 使用 uname() 收集内核信息（绕过 cat 限制）
        result << CollectKernelInfoWithUname();
        
        // 使用 sysconf() 收集系统配置信息
        result << CollectSystemConfigWithSysconf();
        
        // 检测 Zygisk 注入
        result << DetectZygiskInjection();
        
        LOGI("Native file fingerprint collection completed");
    } catch (const std::exception& e) {
        LOGE("Exception in CollectAllNativeFiles: %s", e.what());
        result << "\nError: " << e.what() << "\n";
    } catch (...) {
        LOGE("Unknown exception in CollectAllNativeFiles");
        result << "\nUnknown error occurred\n";
    }
    
    return result.str();
}

