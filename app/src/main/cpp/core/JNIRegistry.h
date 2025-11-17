#ifndef SPHUNTER_JNIREGISTRY_H
#define SPHUNTER_JNIREGISTRY_H

#include <jni.h>

/**
 * JNI方法注册管理器
 * 负责管理所有Native方法的注册
 */
class JNIRegistry {
public:
    /**
     * 注册所有JNI方法
     * @param env JNI环境指针
     * @return 成功返回JNI_TRUE，失败返回JNI_FALSE
     */
    static jint RegisterAllNatives(JNIEnv* env);
    
private:
    /**
     * 注册Native方法到指定类
     * @param env JNI环境指针
     * @param className 类名
     * @param methods 方法数组
     * @param numMethods 方法数量
     * @return 成功返回JNI_TRUE，失败返回JNI_FALSE
     */
    static jint RegisterNativeMethods(
        JNIEnv* env, 
        const char* className,
        JNINativeMethod* methods, 
        int numMethods
    );
    
    /**
     * 注册指纹相关的Native方法
     * @param env JNI环境指针
     * @return 成功返回JNI_TRUE，失败返回JNI_FALSE
     */
    static jint RegisterFingerprintMethods(JNIEnv* env);
};

#endif // SPHUNTER_JNIREGISTRY_H

