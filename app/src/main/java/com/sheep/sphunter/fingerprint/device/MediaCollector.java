package com.sheep.sphunter.fingerprint.device;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaDrm;
import android.media.UnsupportedSchemeException;

import androidx.annotation.NonNull;

import java.util.UUID;

/**
 * 媒体信息采集器（音量、DRM等）
 */
public class MediaCollector {
    private final Context context;

    public MediaCollector(@NonNull Context context) {
        this.context = context;
    }

    /**
     * 获取音量相关信息
     *
     * @return 音量相关信息字符串
     */
    @NonNull
    public String getVolumeInfo() {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) {
                return "AudioManager is null";
            }
            return "VolumeInfo: " + audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        } catch (Exception e) {
            return "VolumeInfo: Error - " + e.getMessage();
        }
    }

    /**
     * 获取DRM相关信息
     *
     * @return DRM相关信息字符串
     */
    @NonNull
    public String getDRMInfo() {
        MediaDrm mediaDrm = null;

        // Widevine UUID
        UUID widevineUuid = new UUID(0xedef8ba979d64aceL, 0xa3c827dcd51d21edL);

        if (!MediaDrm.isCryptoSchemeSupported(widevineUuid)) {
            return "Widevine DRM not supported on this device";
        }

        try {
            mediaDrm = new MediaDrm(widevineUuid);

            byte[] deviceId = mediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID);

            if (deviceId == null || deviceId.length == 0) {
                return "Device Unique ID is null or empty";
            }

            StringBuilder hexString = new StringBuilder();
            for (byte b : deviceId) {
                hexString.append(String.format("%02x", b & 0xff));
            }

            return "MediaDrm Device Unique ID: " + hexString.toString() +
                "\nLength: " + deviceId.length + " bytes";

        } catch (MediaDrm.MediaDrmStateException e) {
            return "MediaDrmStateException: " + e.getMessage() +
                "\nDiagnostic Info: " + e.getDiagnosticInfo();
        } catch (IllegalArgumentException e) {
            return "IllegalArgumentException: " + e.getMessage() +
                "\n(Property may not be available for this DRM scheme)";
        } catch (UnsupportedSchemeException e) {
            return "UnsupportedSchemeException: " + e.getMessage();
        } catch (Exception e) {
            return "Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        } finally {
            if (mediaDrm != null) {
                try {
                    mediaDrm.release();
                } catch (Exception e) {
                    // 忽略释放错误
                }
            }
        }
    }
}

