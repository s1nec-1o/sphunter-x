/**
 * spHunter Native Library
 * 
 * 主要功能：
 * - 系统指纹收集
 * - DRM信息获取
 * - 网络接口信息收集
 */

#include <jni.h>
#include "JNIRegistry.h"
#include "Log.h"

/**
 * JNI_OnLoad - JNI库加载时调用
 * 负责注册所有Native方法
 * 
 * @param vm JavaVM指针
 * @param reserved 保留参数
 * @return JNI版本号，失败返回JNI_ERR
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env = nullptr;
    
    // 获取JNI环境
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        LOGE("Failed to get JNI environment");
        return JNI_ERR;
    }
    
    // 注册所有Native方法
    if (JNIRegistry::RegisterAllNatives(env) != JNI_TRUE) {
        LOGE("Failed to register native methods");
        return JNI_ERR;
    }
    
    LOGI("JNI_OnLoad: Native library loaded successfully");
    return JNI_VERSION_1_6;
}

/**
 * JNI_OnUnload - JNI库卸载时调用
 * 负责清理资源
 * 
 * @param vm JavaVM指针
 * @param reserved 保留参数
 */
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    LOGI("JNI_OnUnload: Native library unloaded");
}
