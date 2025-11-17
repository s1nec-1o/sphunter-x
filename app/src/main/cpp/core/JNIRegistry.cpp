#include "JNIRegistry.h"
#include "FingerprintCollector.h"
#include "../collectors/MacAddressCollector.h"
#include "../utils/Log.h"

// JNI方法实现
static jstring getCFingerprint(JNIEnv* env, jobject thiz) {
    std::string result = FingerprintCollector::CollectNativeFingerprint();
    return env->NewStringUTF(result.c_str());
}

static jstring getMacAddress(JNIEnv* env, jobject thiz) {
    std::string result = MacAddressCollector::GetMacAddress();
    return env->NewStringUTF(result.c_str());
}

// JNIRegistry实现
jint JNIRegistry::RegisterNativeMethods(
    JNIEnv* env, 
    const char* className,
    JNINativeMethod* methods, 
    int numMethods) {
    
    jclass clazz = env->FindClass(className);
    if (clazz == nullptr) {
        LOGE("Failed to find class: %s", className);
        return JNI_FALSE;
    }
    
    if (env->RegisterNatives(clazz, methods, numMethods) < 0) {
        LOGE("Failed to register natives for class: %s", className);
        return JNI_FALSE;
    }
    
    LOGI("Successfully registered natives for class: %s", className);
    return JNI_TRUE;
}

jint JNIRegistry::RegisterFingerprintMethods(JNIEnv* env) {
    const char* className = "com/sheep/sphunter/fingerprint/jni/NativeFingerprint";
    
    JNINativeMethod methods[] = {
        {
            "getCFingerprint",
            "()Ljava/lang/String;",
            reinterpret_cast<void*>(getCFingerprint)
        },
        {
            "getMacAddress",
            "()Ljava/lang/String;",
            reinterpret_cast<void*>(getMacAddress)
        }
    };
    
    return RegisterNativeMethods(
        env, 
        className, 
        methods, 
        sizeof(methods) / sizeof(methods[0])
    );
}

jint JNIRegistry::RegisterAllNatives(JNIEnv* env) {
    // 注册指纹相关方法
    if (RegisterFingerprintMethods(env) != JNI_TRUE) {
        LOGE("Failed to register fingerprint methods");
        return JNI_FALSE;
    }
    
    LOGI("All native methods registered successfully");
    return JNI_TRUE;
}

