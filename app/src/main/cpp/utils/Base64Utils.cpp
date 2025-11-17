#include "Base64Utils.h"

const char Base64Utils::BASE64_CHARS[] = 
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

std::string Base64Utils::Encode(const uint8_t* data, size_t length) {
    if (data == nullptr || length == 0) {
        return "";
    }
    
    std::string result;
    result.reserve(((length + 2) / 3) * 4);
    
    for (size_t i = 0; i < length; i += 3) {
        uint32_t value = 0;
        int bytes = 0;
        
        // 读取最多3个字节
        for (int j = 0; j < 3 && (i + j) < length; j++) {
            value = (value << 8) | data[i + j];
            bytes++;
        }
        
        // 转换为4个Base64字符
        value <<= (3 - bytes) * 8;  // 补齐到24位
        for (int j = 0; j < 4; j++) {
            if (j < (bytes * 8 + 5) / 6) {
                int shift = (3 - j) * 6;
                result += BASE64_CHARS[(value >> shift) & 0x3F];
            } else {
                result += '=';
            }
        }
    }
    
    return result;
}

std::string Base64Utils::ToHexString(const uint8_t* data, size_t length) {
    if (data == nullptr || length == 0) {
        return "";
    }
    
    std::ostringstream oss;
    oss << std::hex << std::setfill('0');
    for (size_t i = 0; i < length; i++) {
        oss << std::setw(2) << static_cast<int>(data[i]);
    }
    return oss.str();
}

