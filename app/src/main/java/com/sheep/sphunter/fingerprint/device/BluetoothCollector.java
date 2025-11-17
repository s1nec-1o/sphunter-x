package com.sheep.sphunter.fingerprint.device;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import androidx.annotation.NonNull;

/**
 * 蓝牙信息采集器
 */
public class BluetoothCollector {
    private final Context context;

    public BluetoothCollector(@NonNull Context context) {
        this.context = context;
    }

    /**
     * 获取蓝牙MAC地址
     *
     * @return 蓝牙MAC地址，如果无法获取则返回 "null"
     */
    @NonNull
    public String getBluetoothAddress() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                return "null";
            }
            String bluetoothAddress = bluetoothAdapter.getAddress();
            return bluetoothAddress != null ? bluetoothAddress : "null";
        } catch (Exception e) {
            e.printStackTrace();
            return "null";
        }
    }
}

