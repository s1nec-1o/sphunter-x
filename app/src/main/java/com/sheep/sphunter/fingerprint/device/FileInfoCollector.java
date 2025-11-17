package com.sheep.sphunter.fingerprint.device;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * 文件信息采集器
 */
public class FileInfoCollector {
    private final Context context;

    public FileInfoCollector(@NonNull Context context) {
        this.context = context;
    }

    /**
     * 获取文件创建时间
     *
     * @return 文件创建时间信息字符串
     */
    @SuppressLint("ObsoleteSdkInt")
    @NonNull
    public String getFileCreationTimeInfo() {
        StringBuilder result = new StringBuilder();
        @SuppressLint("SdCardPath") String[] paths = {
            "/sdcard/DCIM",
            "/sdcard/DCIM/Camera",
            "/sdcard/Pictures",
            "/sdcard/Download",
            "/sdcard/Android",
            "/storage/emulated/0/DCIM/Camera",
        };

        for (String path : paths) {
            try {
                File file = new File(path);
                if (!file.exists()) continue;

                result.append(path).append(":\n");
                long lastMod = file.lastModified();
                result.append("  Modified: ").append(lastMod > 0 ? lastMod : "null").append("\n");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(
                                Paths.get(path),
                                BasicFileAttributes.class);
                        long creation = attrs.creationTime().toMillis();
                        result.append("  Creation: ").append(creation > 0 ? creation : "null").append("\n");
                    } catch (Exception e) {
                        result.append("  Creation: Error\n");
                    }
                }
                result.append("\n");
            } catch (Exception e) {
                result.append(path).append(": Error\n\n");
            }
        }
        return result.toString();
    }
}

