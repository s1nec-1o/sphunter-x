package com.sheep.sphunter.model;

import androidx.annotation.NonNull;

/**
 * 指纹采集结果数据模型
 */
public class FingerprintResult {
    private String settings;
    private String androidId;
    private String bluetoothAddress;
    private String serialNumber;
    private String phoneInfo;
    private String buildInfo;
    private String accountInfo;
    private String volumeInfo;
    private String sensorInfo;
    private String drmInfo;
    private String nativeBuildInfo;
    private String nativeDrmInfo;
    private String glendererInfo;
    private String batteryInfo;
    private String memoryInfo;

    public FingerprintResult() {
    }

    public String getSettings() {
        return settings;
    }

    public void setSettings(String settings) {
        this.settings = settings;
    }

    public String getAndroidId() {
        return androidId;
    }

    public void setAndroidId(String androidId) {
        this.androidId = androidId;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getPhoneInfo() {
        return phoneInfo;
    }

    public void setPhoneInfo(String phoneInfo) {
        this.phoneInfo = phoneInfo;
    }

    public String getBuildInfo() {
        return buildInfo;
    }

    public void setBuildInfo(String buildInfo) {
        this.buildInfo = buildInfo;
    }

    public String getAccountInfo() {
        return accountInfo;
    }

    public void setAccountInfo(String accountInfo) {
        this.accountInfo = accountInfo;
    }

    public String getVolumeInfo() {
        return volumeInfo;
    }

    public void setVolumeInfo(String volumeInfo) {
        this.volumeInfo = volumeInfo;
    }

    public String getSensorInfo() {
        return sensorInfo;
    }

    public void setSensorInfo(String sensorInfo) {
        this.sensorInfo = sensorInfo;
    }

    public String getDrmInfo() {
        return drmInfo;
    }

    public void setDrmInfo(String drmInfo) {
        this.drmInfo = drmInfo;
    }

    public String getNativeBuildInfo() {
        return nativeBuildInfo;
    }

    public void setNativeBuildInfo(String nativeBuildInfo) {
        this.nativeBuildInfo = nativeBuildInfo;
    }

    public String getNativeDrmInfo() {
        return nativeDrmInfo;
    }

    public void setNativeDrmInfo(String nativeDrmInfo) {
        this.nativeDrmInfo = nativeDrmInfo;
    }

    public String getglendererInfo() {
        return glendererInfo;
    }

    public void setGlendererInfo(String glendererInfo) {
        this.glendererInfo = glendererInfo;
    }

    public String getBatteryInfo() {
        return batteryInfo;
    }

    public void setBatteryInfo(String batteryInfo) {
        this.batteryInfo = batteryInfo;
    }

    public void setMemoryInfo(String memoryInfo) {
        this.memoryInfo = memoryInfo;
    }

    public String getMemoryInfo() {
        return this.memoryInfo;
    }
    /**
     * 将结果格式化为字符串
     */
    @NonNull
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (settings != null) {
            result.append(settings);
        }
        if (androidId != null) {
            result.append("\nlevel3_Android: ").append(androidId);
        }
        if (bluetoothAddress != null) {
            result.append("\nbluetoothAddress: ").append(bluetoothAddress);
        }
        if (serialNumber != null) {
            result.append("\nserialNumber: ").append(serialNumber);
        }
        if (phoneInfo != null) {
            result.append("\nPhoneInfo: ").append(phoneInfo);
        }
        if (buildInfo != null) {
            result.append("\nBuildInfo: ").append(buildInfo);
        }
        if (accountInfo != null) {
            result.append("\nAccountInfo: ").append(accountInfo);
        }
        if (volumeInfo != null) {
            result.append("\nVolumeInfo: ").append(volumeInfo);
        }
        if (sensorInfo != null) {
            result.append("\nSensorInfo: ").append(sensorInfo);
        }
        if (drmInfo != null) {
            result.append("\nDRMInfo: ").append(drmInfo);
        }
        if (glendererInfo != null) {
            result.append("\n\nGlendererInfo: ").append(glendererInfo);
        }
        if (batteryInfo != null) {
            result.append("\n\nBatteryInfo: ").append(batteryInfo);
        }
        if (memoryInfo != null) {
            result.append("\n\nMemoryInfo: ").append(memoryInfo);
        }
        return result.toString();
    }

    /**
     * 获取Native层结果字符串
     */
    public String toNativeString() {
        StringBuilder result = new StringBuilder();
        if (nativeBuildInfo != null) {
            result.append(nativeBuildInfo);
        }
        if (nativeDrmInfo != null) {
            result.append(nativeDrmInfo);
        }
        return result.toString();
    }

}

