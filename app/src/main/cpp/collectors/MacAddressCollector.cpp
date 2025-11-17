#include "MacAddressCollector.h"
#include "../utils/Log.h"
#include "../netlink/ifaddrs.h"
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <netpacket/packet.h>
#include <net/ethernet.h>
#include <net/if.h>
#include <linux/if.h>
#include <linux/if_arp.h>
#include <netdb.h>
#include <cstring>
#include <cstdio>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>

std::string MacAddressCollector::FormatMacAddress(const uint8_t* addr, int length) {
    char macBuffer[18] = {0};
    int offset = 0;
    for (int i = 0; i < length && i < 6; i++) {
        offset += snprintf(macBuffer + offset, sizeof(macBuffer) - offset,
                          "%02X%s", addr[i], (i < length - 1) ? ":" : "");
    }
    return std::string(macBuffer);
}

std::string MacAddressCollector::GetMacAddressViaIoctl(const std::string& interfaceName) {
    int sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) {
        LOGE("Failed to create socket for ioctl: %s", strerror(errno));
        return "";
    }

    struct ifreq ifr;
    memset(&ifr, 0, sizeof(ifr));
    strncpy(ifr.ifr_name, interfaceName.c_str(), IFNAMSIZ - 1);
    ifr.ifr_name[IFNAMSIZ - 1] = '\0';

    if (ioctl(sockfd, SIOCGIFHWADDR, &ifr) < 0) {
        LOGE("ioctl(SIOCGIFHWADDR) failed for %s: %s", interfaceName.c_str(), strerror(errno));
        close(sockfd);
        return "";
    }

    close(sockfd);
    
    // 检查是否为以太网类型
    if (ifr.ifr_hwaddr.sa_family != ARPHRD_ETHER) {
        LOGI("Interface %s is not Ethernet type (family: %d)", interfaceName.c_str(), ifr.ifr_hwaddr.sa_family);
        return "";
    }

    uint8_t* mac = reinterpret_cast<uint8_t*>(ifr.ifr_hwaddr.sa_data);
    return FormatMacAddress(mac, 6);
}

std::vector<MacAddressCollector::NetworkInterfaceInfo> 
MacAddressCollector::CollectNetworkInterfacesViaIoctl() {
    std::vector<NetworkInterfaceInfo> interfaces;
    struct ifaddrs* ifap = nullptr;
    struct ifaddrs* ifaptr = nullptr;

    // 使用getifaddrs获取接口列表（即使netlink失败，getifaddrs可能仍能获取接口名）
    if (getifaddrs(&ifap) != 0) {
        LOGE("getifaddrs failed, trying alternative method");
        // 如果getifaddrs失败，尝试从/sys/class/net读取接口名
        // 这里简化处理，直接返回空列表，让调用者知道需要其他方法
        return interfaces;
    }

    // 收集所有接口名称
    std::vector<std::string> interfaceNames;
    for (ifaptr = ifap; ifaptr != nullptr; ifaptr = ifaptr->ifa_next) {
        if (ifaptr->ifa_name != nullptr) {
            std::string name = ifaptr->ifa_name;
            // 避免重复
            bool found = false;
            for (const auto& existing : interfaceNames) {
                if (existing == name) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                interfaceNames.push_back(name);
            }
        }
    }

    // 使用ioctl获取每个接口的MAC地址
    for (const auto& name : interfaceNames) {
        std::string macAddress = GetMacAddressViaIoctl(name);
        if (!macAddress.empty()) {
            NetworkInterfaceInfo info;
            info.name = name;
            info.macAddress = macAddress;
            interfaces.push_back(info);
            LOGI("Interface (ioctl): %s, MAC: %s", info.name.c_str(), info.macAddress.c_str());
        }
    }

    // 添加IP地址信息（从getifaddrs获取）
    for (ifaptr = ifap; ifaptr != nullptr; ifaptr = ifaptr->ifa_next) {
        if (ifaptr->ifa_addr == nullptr) {
            continue;
        }

        sa_family_t family = ifaptr->ifa_addr->sa_family;
        
        if (family == AF_INET || family == AF_INET6) {
            char host[NI_MAXHOST];
            int ret = getnameinfo(
                ifaptr->ifa_addr,
                (family == AF_INET) ? sizeof(struct sockaddr_in) : sizeof(struct sockaddr_in6),
                host, NI_MAXHOST,
                nullptr, 0, 
                NI_NUMERICHOST
            );
            
            if (ret == 0) {
                // 找到对应的接口并添加IP地址
                for (auto& info : interfaces) {
                    if (info.name == ifaptr->ifa_name) {
                        if (family == AF_INET) {
                            info.ipv4Address = host;
                            LOGI("Interface: %s, IPv4: %s", info.name.c_str(), host);
                        } else {
                            info.ipv6Address = host;
                            LOGI("Interface: %s, IPv6: %s", info.name.c_str(), host);
                        }
                        break;
                    }
                }
            }
        }
    }

    freeifaddrs(ifap);
    return interfaces;
}

std::vector<MacAddressCollector::NetworkInterfaceInfo> 
MacAddressCollector::CollectNetworkInterfaces() {
    std::vector<NetworkInterfaceInfo> interfaces;
    struct ifaddrs* ifap = nullptr;
    struct ifaddrs* ifaptr = nullptr;

    if (getifaddrs(&ifap) != 0) {
        LOGE("Failed to get network interfaces via netlink, trying ioctl fallback");
        return CollectNetworkInterfacesViaIoctl();
    }

    // 第一遍：收集MAC地址
    bool hasMacAddress = false;
    for (ifaptr = ifap; ifaptr != nullptr; ifaptr = ifaptr->ifa_next) {
        if (ifaptr->ifa_addr == nullptr) {
            continue;
        }

        sa_family_t family = ifaptr->ifa_addr->sa_family;
        
        if (family == AF_PACKET) {
            // 获取MAC地址
            auto* sockaddr = reinterpret_cast<struct sockaddr_ll*>(ifaptr->ifa_addr);
            std::string macAddress = FormatMacAddress(sockaddr->sll_addr, sockaddr->sll_halen);
            
            NetworkInterfaceInfo info;
            info.name = ifaptr->ifa_name;
            info.macAddress = macAddress;
            interfaces.push_back(info);
            hasMacAddress = true;
            
            LOGI("Interface: %s, MAC: %s", info.name.c_str(), info.macAddress.c_str());
        }
    }

    // 如果netlink方法没有获取到任何MAC地址，尝试使用ioctl作为备选
    if (!hasMacAddress) {
        LOGI("No MAC addresses found via netlink, trying ioctl fallback");
        freeifaddrs(ifap);
        return CollectNetworkInterfacesViaIoctl();
    }

    // 第二遍：添加IP地址信息
    for (ifaptr = ifap; ifaptr != nullptr; ifaptr = ifaptr->ifa_next) {
        if (ifaptr->ifa_addr == nullptr) {
            continue;
        }

        sa_family_t family = ifaptr->ifa_addr->sa_family;
        
        if (family == AF_INET || family == AF_INET6) {
            char host[NI_MAXHOST];
            int ret = getnameinfo(
                ifaptr->ifa_addr,
                (family == AF_INET) ? sizeof(struct sockaddr_in) : sizeof(struct sockaddr_in6),
                host, NI_MAXHOST,
                nullptr, 0, 
                NI_NUMERICHOST
            );
            
            if (ret == 0) {
                // 找到对应的接口并添加IP地址
                for (auto& info : interfaces) {
                    if (info.name == ifaptr->ifa_name) {
                        if (family == AF_INET) {
                            info.ipv4Address = host;
                            LOGI("Interface: %s, IPv4: %s", info.name.c_str(), host);
                        } else {
                            info.ipv6Address = host;
                            LOGI("Interface: %s, IPv6: %s", info.name.c_str(), host);
                        }
                        break;
                    }
                }
            } else {
                LOGE("getnameinfo() failed for %s: %s", ifaptr->ifa_name, gai_strerror(ret));
            }
        }
    }

    freeifaddrs(ifap);
    return interfaces;
}

std::string MacAddressCollector::GetMacAddress() {
    auto interfaces = CollectNetworkInterfaces();
    
    // 优先返回wlan0的MAC地址
    for (const auto& info : interfaces) {
        if (info.name == "wlan0" && !info.macAddress.empty()) {
            return info.macAddress;
        }
    }
    
    // 如果没有wlan0，返回第一个非空的MAC地址
    for (const auto& info : interfaces) {
        if (!info.macAddress.empty()) {
            return info.macAddress;
        }
    }
    
    // 如果netlink方法失败，尝试直接使用ioctl获取wlan0
    LOGI("Trying ioctl fallback for wlan0");
    std::string mac = GetMacAddressViaIoctl("wlan0");
    if (!mac.empty()) {
        return mac;
    }
    
    // 尝试其他常见接口名
    const char* commonInterfaces[] = {"eth0", "wlan1", "usb0", nullptr};
    for (int i = 0; commonInterfaces[i] != nullptr; i++) {
        mac = GetMacAddressViaIoctl(commonInterfaces[i]);
        if (!mac.empty()) {
            return mac;
        }
    }
    
    return "Failed to get MAC address";
}

void MacAddressCollector::LogAllNetworkInterfaces() {
    auto interfaces = CollectNetworkInterfaces();
    
    LOGI("========== Network Interfaces ==========");
    for (const auto& info : interfaces) {
        LOGI("Interface: %s", info.name.c_str());
        LOGI("  MAC: %s", info.macAddress.c_str());
        if (!info.ipv4Address.empty()) {
            LOGI("  IPv4: %s", info.ipv4Address.c_str());
        }
        if (!info.ipv6Address.empty()) {
            LOGI("  IPv6: %s", info.ipv6Address.c_str());
        }
    }
    LOGI("========================================");
}

