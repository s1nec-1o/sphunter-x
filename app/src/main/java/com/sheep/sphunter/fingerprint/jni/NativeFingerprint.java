package com.sheep.sphunter.fingerprint.jni;

import androidx.annotation.NonNull;

/**
 * Native 层指纹采集接口
 * 封装 JNI 调用
 */
public class NativeFingerprint {
    static {
        System.loadLibrary("sphunter");
    }

    /**
     * 获取 Native 层指纹信息
     *
     * @return 指纹信息字符串
     */
    @NonNull
    public native String getCFingerprint();

    /**
     * 获取 MAC 地址
     *
     * @return MAC 地址字符串
     */
    @NonNull
    public native String getMacAddress();
}

