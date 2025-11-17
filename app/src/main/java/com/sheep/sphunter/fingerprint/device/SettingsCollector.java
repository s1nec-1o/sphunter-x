package com.sheep.sphunter.fingerprint.device;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;

import androidx.annotation.NonNull;

import org.json.JSONObject;

/**
 * Settings 信息采集器
 */
public class SettingsCollector {
    private final Context context;

    public SettingsCollector(@NonNull Context context) {
        this.context = context;
    }

    /**
     * 获取 Android ID
     */
    public String getAndroidId() {
        try {
            android.os.Bundle callResult = context.getContentResolver().call(
                    android.net.Uri.parse("content://settings/secure"), 
                    "GET_secure", 
                    "android_id", 
                    new android.os.Bundle()
            );
            String androidIdValue = callResult.getString("value");
            return androidIdValue != null ? androidIdValue : "null";
        } catch (Exception e) {
            e.printStackTrace();
            return "null";
        }
    }

    /**
     * 采集 Settings 中的指纹信息
     *
     * @return 包含所有采集到的 Settings 字段的 JSON 字符串
     */
    @NonNull
    public String collectSettings() {
        if (context == null) {
            return "{}";
        }

        ContentResolver resolver = context.getContentResolver();
        JSONObject settingsData = new JSONObject();

        try {
            String androidId = Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID);
            settingsData.put("android_id", androidId != null ? androidId : "null");

            String[] globalKeys = {
                    com.sheep.sphunter.util.Constants.SettingsKeys.MI_HEALTH_ID,
                    com.sheep.sphunter.util.Constants.SettingsKeys.GCBOOSTER_UUID,
                    com.sheep.sphunter.util.Constants.SettingsKeys.KEY_MQS_UUID,
                    com.sheep.sphunter.util.Constants.SettingsKeys.AD_AAID,
                    com.sheep.sphunter.util.Constants.SettingsKeys.BLUETOOTH_NAME,
                    com.sheep.sphunter.util.Constants.SettingsKeys.BLUETOOTH_ADDRESS,
            };

            for (String key : globalKeys) {
                String value = "null";
                try {
                    String fetchedValue = Settings.Global.getString(resolver, key);
                    if (fetchedValue != null) {
                        value = fetchedValue;
                    }
                } catch (Exception e) {
                    // 忽略单个键的错误
                }
                settingsData.put(key, value);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return settingsData.toString();
    }
}

