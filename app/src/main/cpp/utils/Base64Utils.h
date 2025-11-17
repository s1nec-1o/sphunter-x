#ifndef SPHUNTER_BASE64UTILS_H
#define SPHUNTER_BASE64UTILS_H

#include <string>
#include <cstdint>
#include <sstream>
#include <iomanip>

/**
 * Base64编码和字符串转换工具类
 */
class Base64Utils {
public:
    /**
     * 将字节数组编码为Base64字符串
     * @param data 待编码的字节数组
     * @param length 字节数组长度
     * @return Base64编码的字符串
     */
    static std::string Encode(const uint8_t* data, size_t length);
    
    /**
     * 将字节数组转换为十六进制字符串
     * @param data 待转换的字节数组
     * @param length 字节数组长度
     * @return 十六进制字符串
     */
    static std::string ToHexString(const uint8_t* data, size_t length);
    
private:
    static const char BASE64_CHARS[];
};

#endif // SPHUNTER_BASE64UTILS_H

