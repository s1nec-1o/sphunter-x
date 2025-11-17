package com.sheep.sphunter.fingerprint.device;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import androidx.annotation.NonNull;

/**
 * 电池信息采集器
 * 使用粘性广播机制快速获取当前电池状态
 */
public class batteryCollector {

    /**
     * 获取电池信息
     * 通过粘性广播机制快速获取当前电池状态，无需注册真实广播接收器
     *
     * @param context 上下文对象
     * @return 电池信息字符串
     */
    @NonNull
    public String getBatteryInfo(@NonNull Context context) {
        try {
            // 1. 构建 IntentFilter
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

            // 2. 空注册 - 直接获取最后一次电池状态的 Intent (粘性广播)
            // 技巧：第一个参数传 null，系统会直接返回包含当前电池状态的 Intent 对象
            Intent batteryStatus = context.registerReceiver(null, filter);

            if (batteryStatus == null) {
                return "Battery info not available";
            }

            // 3. 解析 Intent 获取电池信息
            // 当前电量
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            // 总刻度（通常是100）
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            // 充电状态 (2=Charging, 3=Discharging, 5=Full 等)
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            // 插拔状态 (0=未插, 1=AC, 2=USB, 4=Wireless)
            int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            // 健康状态
            int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            // 电压（毫伏）
            int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            // 温度（单位：0.1摄氏度）
            int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);

            // 4. 计算电量百分比
            float batteryPct = -1;
            if (level != -1 && scale != -1) {
                batteryPct = (level * 100f) / scale;
            }

            // 5. 格式化返回结果
            StringBuilder result = new StringBuilder();
            result.append("Battery Level: ").append(String.format("%.1f%%", batteryPct))
                    .append(" (").append(level).append("/").append(scale).append(")");

            // 充电状态
            result.append("\nStatus: ").append(getStatusString(status));

            // 插拔状态
            result.append("\nPlugged: ").append(getPluggedString(plugged));

            // 健康状态
            result.append("\nHealth: ").append(getHealthString(health));

            // 电压
            if (voltage != -1) {
                result.append("\nVoltage: ").append(voltage).append(" mV");
            }

            // 温度
            if (temperature != -1) {
                float tempCelsius = temperature / 10f;
                result.append("\nTemperature: ").append(String.format("%.1f°C", tempCelsius));
            }

            return result.toString();

        } catch (Exception e) {
            return "Battery info error: " + e.getMessage();
        }
    }

    /**
     * 获取充电状态描述
     */
    @NonNull
    private String getStatusString(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "Discharging";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "Not Charging";
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
            default:
                return "Unknown";
        }
    }

    /**
     * 获取插拔状态描述
     */
    @NonNull
    private String getPluggedString(int plugged) {
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return "AC Adapter";
            case BatteryManager.BATTERY_PLUGGED_USB:
                return "USB";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return "Wireless";
            case 0:
                return "Unplugged";
            default:
                return "Unknown";
        }
    }

    /**
     * 获取健康状态描述
     */
    @NonNull
    private String getHealthString(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "Overheat";
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "Over Voltage";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "Unspecified Failure";
            case BatteryManager.BATTERY_HEALTH_COLD:
                return "Cold";
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
            default:
                return "Unknown";
        }
    }
}