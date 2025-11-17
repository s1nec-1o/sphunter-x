package com.sheep.sphunter.fingerprint.device;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

/**
 * 序列号采集器
 */
public class SerialNumberCollector {
    private final Context context;

    public SerialNumberCollector(@NonNull Context context) {
        this.context = context;
    }

    /**
     * 获取serial number
     * 注意：Android 8.0+ 需要 READ_PHONE_STATE 权限，且普通应用可能无法获取
     *
     * @return serial number，如果无法获取则返回 "null"
     */
    @NonNull
    public String getSerialNumber() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+ 需要 READ_PHONE_STATE 权限
                String serial = Build.getSerial();
                // 如果返回 "unknown" 或空字符串，也视为失败
                if (serial == null || serial.isEmpty() || serial.equals("unknown")) {
                    return "null";
                }
                return serial;
            } else {
                // Android 8.0 以下版本
                return Build.SERIAL != null ? Build.SERIAL : "null";
            }
        } catch (SecurityException e) {
            // Android 8.0+ 如果没有 READ_PHONE_STATE 权限会抛出 SecurityException
            e.printStackTrace();
            return "null";
        } catch (Exception e) {
            e.printStackTrace();
            return "null";
        }
    }
}

