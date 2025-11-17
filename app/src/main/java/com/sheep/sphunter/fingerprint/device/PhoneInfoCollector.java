package com.sheep.sphunter.fingerprint.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;

import java.lang.reflect.Method;

/**
 * 电话信息采集器
 * 包含公开 API 和通过反射调用的 hidden API
 * 但是基本都收集不到数据，因为普通应用没有权限获取这些信息（如果有获取这些数据的方法，请多指教）
 */
public class PhoneInfoCollector {
    private final Context context;

    public PhoneInfoCollector(@NonNull Context context) {
        this.context = context;
    }

    /**
     * 获取IMEI , IMSI ,ICCID,Line1Number
     * 只返回成功收集到的数据，过滤掉所有错误和 null 值
     *
     * @return 电话信息字符串
     */
    @SuppressLint("HardwareIds")
    @NonNull
    public String getPhoneInfo() {
        TelephonyManager tm = getTelephonyManager();
        if (tm == null) {
            return "";
        }

        StringBuilder str = new StringBuilder();

        // DeviceId(IMEI) - Android 10+ 需要特殊权限，普通应用无法获取
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+ 使用 getImei()，但需要 READ_PHONE_STATE 权限
                // Android 10+ 普通应用无法获取 IMEI，跳过
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    try {
                        String deviceId = tm.getImei();
                        if (deviceId != null && !deviceId.isEmpty()) {
                            appendIfValid(str, "DeviceId(IMEI)", deviceId);
                        }
                    } catch (SecurityException e) {
                        // 权限不足，跳过
                    }
                }
            } else {
                // Android 8.0 以下使用 getDeviceId()
                String deviceId = tm.getDeviceId();
                if (deviceId != null && !deviceId.isEmpty()) {
                    appendIfValid(str, "DeviceId(IMEI)", deviceId);
                }
            }
        } catch (Exception e) {
            // 发生异常，跳过
        }

        // DeviceSoftwareVersion
        appendInfoClean(str, "DeviceSoftwareVersion", () -> tm.getDeviceSoftwareVersion());

        // Line1Number
        appendInfoCleanWithSecurity(str, "Line1Number", () -> tm.getLine1Number());

        // NetworkCountryIso
        appendInfoClean(str, "NetworkCountryIso", () -> tm.getNetworkCountryIso());

        // NetworkOperator
        appendInfoClean(str, "NetworkOperator", () -> tm.getNetworkOperator());

        // NetworkOperatorName
        appendInfoClean(str, "NetworkOperatorName", () -> tm.getNetworkOperatorName());

        // NetworkType
        appendInfoClean(str, "NetworkType", () -> String.valueOf(tm.getNetworkType()));

        // PhoneType
        appendInfoClean(str, "PhoneType", () -> String.valueOf(tm.getPhoneType()));

        // SimCountryIso
        appendInfoClean(str, "SimCountryIso", () -> tm.getSimCountryIso());

        // SimOperator
        appendInfoClean(str, "SimOperator", () -> tm.getSimOperator());

        // SimOperatorName
        appendInfoClean(str, "SimOperatorName", () -> tm.getSimOperatorName());

        // SimSerialNumber
        appendInfoCleanWithSecurity(str, "SimSerialNumber", () -> tm.getSimSerialNumber());

        // SimState
        appendInfoClean(str, "SimState", () -> String.valueOf(tm.getSimState()));

        // SubscriberId(IMSI)
        appendInfoCleanWithSecurity(str, "SubscriberId(IMSI)", () -> tm.getSubscriberId());

        // VoiceMailNumber
        try {
            String voiceMailNumber = tm.getVoiceMailNumber();
            if (voiceMailNumber != null && !voiceMailNumber.isEmpty()) {
                appendIfValid(str, "VoiceMailNumber", voiceMailNumber);
            }
        } catch (Exception e) {
            // 发生异常，跳过
        }

        // ==================== Hidden API 部分 (通过反射调用) ====================
        // 只收集成功的数据，不输出错误信息

        // MEID (适用于 CDMA 设备) - Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appendReflectionInfoClean(str, "MEID", tm, "getMeid");
        }

        // Network Access Identifier
        appendReflectionInfoClean(str, "NAI", tm, "getNai");

        // Data Network Type
        appendReflectionInfoClean(str, "DataNetworkType", tm, "getDataNetworkType");
        
        // 尝试获取 Phone Count (双卡设备数量)
        appendReflectionInfoClean(str, "PhoneCount", tm, "getPhoneCount");
        
        // 尝试获取 Active Modem Count
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            appendReflectionInfoClean(str, "ActiveModemCount", tm, "getActiveModemCount");
        }

        // 多卡设备信息
        // 尝试获取双卡 IMEI (slotIndex 0 和 1) - Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (int slot = 0; slot < 2; slot++) {
                appendReflectionInfoWithParamClean(str, "IMEI[Slot" + slot + "]", tm, "getImei", int.class, slot);
            }
        }

        // 尝试获取双卡 DeviceId (slotIndex 0 和 1)
        for (int slot = 0; slot < 2; slot++) {
            appendReflectionInfoWithParamClean(str, "DeviceId[Slot" + slot + "]", tm, "getDeviceId", int.class, slot);
        }
        
        // 尝试获取双卡 MEID (slotIndex 0 和 1) - Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            for (int slot = 0; slot < 2; slot++) {
                appendReflectionInfoWithParamClean(str, "MEID[Slot" + slot + "]", tm, "getMeid", int.class, slot);
            }
        }

        // 尝试获取双卡 IMSI - 注意：这个方法签名在不同版本可能不同
        for (int subId = 0; subId < 2; subId++) {
            appendMultipleMethodAttemptsClean(str, "SubscriberId[SubId" + subId + "]", tm, 
                new String[]{"getSubscriberId"}, 
                new Class[][]{new Class[]{int.class}}, 
                new Object[][]{new Object[]{subId}});
        }

        // 运营商配置信息
        appendCarrierConfigClean(str, tm);

        return str.toString();
    }

    /**
     * 尝试多个方法签名，直到成功或全部失败（清洗版本：只返回成功的数据）
     *
     * @param str         字符串构建器
     * @param label       标签
     * @param tm          TelephonyManager 实例
     * @param methodNames 要尝试的方法名数组
     * @param paramTypes  每个方法对应的参数类型数组
     * @param paramValues 每个方法对应的参数值数组
     */
    private void appendMultipleMethodAttemptsClean(StringBuilder str, String label, TelephonyManager tm,
                                                    String[] methodNames, Class<?>[][] paramTypes, Object[][] paramValues) {
        for (int i = 0; i < methodNames.length; i++) {
            try {
                Method method = TelephonyManager.class.getDeclaredMethod(methodNames[i], paramTypes[i]);
                method.setAccessible(true);
                Object result = method.invoke(tm, paramValues[i]);
                if (result != null) {
                    String value = result.toString();
                    if (!value.isEmpty() && !value.equals("null")) {
                        str.append(label).append(" = ").append(value).append("\n");
                        return; // 成功获取，退出
                    }
                }
            } catch (Exception e) {
                // 继续尝试下一个方法
            }
        }
        // 所有方法都失败，不添加任何内容
    }


    private TelephonyManager getTelephonyManager() {
        try {
            if (context == null) {
                return null;
            }
            return (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private interface InfoProvider {
        String get() throws Exception;
    }

    /**
     * 只在值有效时才添加到输出（清洗版本）
     */
    private void appendIfValid(StringBuilder str, String label, String value) {
        if (value != null && !value.isEmpty() && !value.equals("null")) {
            str.append(label).append(" = ").append(value).append("\n");
        }
    }

    /**
     * 清洗版本：只在成功获取有效数据时才添加
     */
    private void appendInfoClean(StringBuilder str, String label, InfoProvider provider) {
        try {
            String value = provider.get();
            if (value != null && !value.isEmpty() && !value.equals("null")) {
                str.append(label).append(" = ").append(value).append("\n");
            }
        } catch (Exception e) {
            // 发生异常，不添加任何内容
        }
    }

    /**
     * 清洗版本：只在成功获取有效数据时才添加（带安全异常处理）
     */
    private void appendInfoCleanWithSecurity(StringBuilder str, String label, InfoProvider provider) {
        try {
            String value = provider.get();
            if (value != null && !value.isEmpty() && !value.equals("null")) {
                str.append(label).append(" = ").append(value).append("\n");
            }
        } catch (SecurityException e) {
            // 权限不足，不添加任何内容
        } catch (Exception e) {
            // 发生异常，不添加任何内容
        }
    }

    /**
     * 通过反射调用无参数的 hidden 方法（清洗版本：只返回成功的数据）
     *
     * @param str        字符串构建器
     * @param label      标签
     * @param tm         TelephonyManager 实例
     * @param methodName 方法名
     */
    private void appendReflectionInfoClean(StringBuilder str, String label, TelephonyManager tm, String methodName) {
        try {
            // 使用 getDeclaredMethod 来访问 hidden API
            Method method = TelephonyManager.class.getDeclaredMethod(methodName);
            method.setAccessible(true);  // 设置可访问
            Object result = method.invoke(tm);
            if (result != null) {
                String value = result.toString();
                if (!value.isEmpty() && !value.equals("null")) {
                    str.append(label).append(" = ").append(value).append("\n");
                }
            }
        } catch (Exception e) {
            // 发生任何异常，不添加任何内容（静默失败）
        }
    }

    /**
     * 通过反射调用带单个参数的 hidden 方法（清洗版本：只返回成功的数据）
     *
     * @param str        字符串构建器
     * @param label      标签
     * @param tm         TelephonyManager 实例
     * @param methodName 方法名
     * @param paramType  参数类型
     * @param paramValue 参数值
     */
    private void appendReflectionInfoWithParamClean(StringBuilder str, String label, TelephonyManager tm,
                                                     String methodName, Class<?> paramType, Object paramValue) {
        try {
            // 使用 getDeclaredMethod 来访问 hidden API
            Method method = TelephonyManager.class.getDeclaredMethod(methodName, paramType);
            method.setAccessible(true);  // 设置可访问
            Object result = method.invoke(tm, paramValue);
            if (result != null) {
                String value = result.toString();
                if (!value.isEmpty() && !value.equals("null")) {
                    str.append(label).append(" = ").append(value).append("\n");
                }
            }
        } catch (Exception e) {
            // 发生任何异常，不添加任何内容（静默失败）
        }
    }

    /**
     * 获取运营商配置信息（清洗版本：只返回成功的数据）
     *
     * @param str 字符串构建器
     * @param tm  TelephonyManager 实例
     */
    @SuppressLint("MissingPermission")
    private void appendCarrierConfigClean(StringBuilder str, TelephonyManager tm) {
        try {
            // 使用 getDeclaredMethod 来访问 hidden API
            Method method = TelephonyManager.class.getDeclaredMethod("getCarrierConfig");
            method.setAccessible(true);
            Object result = method.invoke(tm);
            
            if (result instanceof PersistableBundle) {
                PersistableBundle bundle = (PersistableBundle) result;
                String configStr = bundleToString(bundle);
                if (configStr != null && !configStr.isEmpty() && !configStr.equals("null")) {
                    str.append("CarrierConfig = ").append(configStr).append("\n");
                }
            } else if (result != null) {
                String value = result.toString();
                if (!value.isEmpty() && !value.equals("null")) {
                    str.append("CarrierConfig = ").append(value).append("\n");
                }
            }
        } catch (Exception e) {
            // 发生任何异常，不添加任何内容（静默失败）
        }
    }

    /**
     * 将 PersistableBundle 转换为字符串（只显示部分关键信息）
     *
     * @param bundle PersistableBundle 对象
     * @return 字符串表示
     */
    private String bundleToString(PersistableBundle bundle) {
        if (bundle == null) {
            return "null";
        }
        
        try {
            // 只提取一些关键信息，避免输出过长
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            
            // 获取 bundle 的大小
            int size = bundle.size();
            sb.append("size=").append(size);
            
            // 可以尝试提取一些常见的配置项
            String[] commonKeys = {
                "carrier_name_string",
                "carrier_config_version_string",
                "allow_non_emergency_calls_in_ecm_bool",
                "carrier_volte_available_bool",
                "carrier_vt_available_bool"
            };
            
            for (String key : commonKeys) {
                if (bundle.containsKey(key)) {
                    Object value = bundle.get(key);
                    sb.append(", ").append(key).append("=").append(value);
                }
            }
            
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return "Error parsing bundle: " + e.getMessage();
        }
    }
}

