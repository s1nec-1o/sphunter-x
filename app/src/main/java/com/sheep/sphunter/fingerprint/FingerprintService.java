package com.sheep.sphunter.fingerprint;

import android.content.Context;

import androidx.annotation.NonNull;

import com.sheep.sphunter.fingerprint.jni.NativeFingerprint;
import com.sheep.sphunter.model.FingerprintResult;
import com.sheep.sphunter.fingerprint.device.*;

import org.json.JSONObject;

/**
 * 指纹采集服务
 * 统一管理所有指纹采集功能
 */
public class FingerprintService {
    private static Context context;
    private final SettingsCollector settingsCollector;
    private final BluetoothCollector bluetoothCollector;
    private final SerialNumberCollector serialNumberCollector;
    private final PhoneInfoCollector phoneInfoCollector;
    private final BuildInfoCollector buildInfoCollector;
    private final AccountCollector accountCollector;
    private final MediaCollector mediaCollector;
    private final SensorCollector sensorCollector;
    private final NativeFingerprint nativeFingerprint;
    private final glendererCollector glendererCollector;
    private final batteryCollector batteryCollector;
    private final MemoryCollector memoryCollector;

    public FingerprintService(@NonNull Context context) {
        this.context = context;
        this.settingsCollector = new SettingsCollector(context);
        this.bluetoothCollector = new BluetoothCollector(context);
        this.serialNumberCollector = new SerialNumberCollector(context);
        this.phoneInfoCollector = new PhoneInfoCollector(context);
        this.buildInfoCollector = new BuildInfoCollector(context);
        this.accountCollector = new AccountCollector(context);
        this.mediaCollector = new MediaCollector(context);
        this.sensorCollector = new SensorCollector(context);
        this.nativeFingerprint = new NativeFingerprint();
        this.glendererCollector = new glendererCollector();
        this.batteryCollector = new batteryCollector();
        this.memoryCollector = new MemoryCollector(context);
    }

    /**
     * 采集 Java 层指纹信息
     *
     * @return 指纹结果对象
     */
    @NonNull
    public FingerprintResult collectJavaFingerprint() {
        FingerprintResult result = new FingerprintResult();

        result.setSettings(settingsCollector.collectSettings());
        result.setAndroidId(settingsCollector.getAndroidId());
        result.setBluetoothAddress(bluetoothCollector.getBluetoothAddress());
        result.setSerialNumber(serialNumberCollector.getSerialNumber());
        result.setPhoneInfo(phoneInfoCollector.getPhoneInfo());
        result.setBuildInfo(buildInfoCollector.getBuildInfo());
        result.setAccountInfo(accountCollector.getAccountInfo());
        result.setVolumeInfo(mediaCollector.getVolumeInfo());
        result.setSensorInfo(sensorCollector.getSensorInfo());
        result.setDrmInfo(mediaCollector.getDRMInfo());
        result.setGlendererInfo(glendererCollector.getGlendererInfo());
        result.setBatteryInfo(batteryCollector.getBatteryInfo(context));
        result.setMemoryInfo(memoryCollector.getMemoryInfo());
        return result;
    }

    /**
     * 采集 Native 层指纹信息
     *
     * @return 指纹结果对象
     */
    @NonNull
    public FingerprintResult collectNativeFingerprint() {
        FingerprintResult result = new FingerprintResult();
        String nativeInfo = nativeFingerprint.getCFingerprint();
        result.setNativeBuildInfo(nativeInfo);
        return result;
    }

    /**
     * 获取 MAC 地址（Native 层）
     *
     * @return MAC 地址字符串
     */
    @NonNull
    public String getMacAddress() {
        return nativeFingerprint.getMacAddress();
    }

    /**
     * 采集并清洗 Java 层指纹信息
     * 返回结构化的清洗后数据
     *
     * @return 清洗后的指纹数据 JSON 对象
     */
    @NonNull
    public JSONObject collectAndCleanJavaFingerprint() {
        // 1. 采集原始指纹数据
        FingerprintResult rawResult = collectJavaFingerprint();
        
        // 2. 清洗并结构化数据
        return FingerprintDataCleaner.cleanFingerprint(rawResult);
    }

    /**
     * 获取格式化的清洗后指纹数据字符串
     *
     * @return 格式化的 JSON 字符串
     */
    @NonNull
    public String getCleanedFingerprintString() {
        JSONObject cleanedData = collectAndCleanJavaFingerprint();
        return FingerprintDataCleaner.formatCleanedData(cleanedData);
    }

    /**
     * 采集并清洗完整的指纹信息（包括 Java 层和 Native 层）
     * 返回结构化的清洗后数据
     *
     * @return 清洗后的完整指纹数据 JSON 对象
     */
    @NonNull
    public JSONObject collectAndCleanAllFingerprint() {
        // 1. 采集 Java 层指纹数据
        FingerprintResult javaResult = collectJavaFingerprint();
        
        // 2. 采集 Native 层指纹数据
        FingerprintResult nativeResult = collectNativeFingerprint();
        
        // 3. 合并结果
        javaResult.setNativeBuildInfo(nativeResult.getNativeBuildInfo());
        
        // 4. 清洗并结构化数据
        return FingerprintDataCleaner.cleanFingerprint(javaResult);
    }

    /**
     * 获取清洗后的 Native 层指纹数据
     *
     * @return 清洗后的 Native 指纹数据 JSON 对象
     */
    @NonNull
    public JSONObject getCleanedNativeFingerprint() {
        FingerprintResult nativeResult = collectNativeFingerprint();
        String nativeInfo = nativeResult.getNativeBuildInfo();
        
        if (nativeInfo != null && !nativeInfo.trim().isEmpty()) {
            return NativeFileDataCleaner.cleanNativeFingerprint(nativeInfo);
        }
        
        return new JSONObject();
    }
}

