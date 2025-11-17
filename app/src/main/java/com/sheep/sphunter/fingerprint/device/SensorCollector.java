package com.sheep.sphunter.fingerprint.device;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import androidx.annotation.NonNull;

/**
 * 传感器信息采集器
 */
public class SensorCollector {
    private final Context context;

    public SensorCollector(@NonNull Context context) {
        this.context = context;
    }

    /**
     * 获取传感器相关信息
     *
     * @return 传感器相关信息字符串
     */
    @NonNull
    public String getSensorInfo() {
        try {
            SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager == null) {
                return "SensorManager is null";
            }
            java.util.List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
            StringBuilder result = new StringBuilder();
            for (Sensor sensor : sensors) {
                result.append("Sensor: ").append(sensor.toString()).append("\n");
            }
            return result.toString();
        } catch (Exception e) {
            return "SensorInfo: Error - " + e.getMessage();
        }
    }
}

