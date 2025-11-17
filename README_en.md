# spHunter - Android Device Fingerprint Collection Tool

[English](README_en.md) | [中文](README.md)

## 📖 Project Introduction

spHunter is a powerful Android device fingerprint collection tool capable of comprehensively gathering device fingerprint information from both the Java layer and the Native layer. The project uses a modular architectural design, invokes Hidden APIs via reflection, and uses JNI technology to access underlying system information, providing comprehensive data support for device identification and security analysis.

## ✨ Key Features

- **Java Layer Fingerprinting**: Obtains device information through Android SDK APIs and reflection.
- **Native Layer Fingerprinting**: Accesses underlying system information directly by calling C++ code via JNI.
- **Hidden API Invocation**: Uses reflection to bypass Android restrictions and access hidden system APIs.
- **Data Cleaning & Structuring**: Cleans and formats the collected raw data into JSON format.
- **Security Analysis**: Detects security risks such as emulators, Root, debug mode, and Zygisk injection.

## 🏗️ Project Architecture

```
com.sheep.sphunter/
├── ui/                          # UI Layer
│   └── MainActivity.java        # Main interface
├── fingerprint/                 # Core fingerprinting module
│   ├── FingerprintService.java  # Fingerprint service (unified entry point)
│   ├── device/                  # Device information collectors
│   │   ├── SettingsCollector.java      # Settings information
│   │   ├── BluetoothCollector.java     # Bluetooth information
│   │   ├── SerialNumberCollector.java  # Serial number
│   │   ├── PhoneInfoCollector.java     # Telephony information
│   │   ├── BuildInfoCollector.java     # Build information
│   │   ├── AccountCollector.java       # Account information
│   │   ├── MediaCollector.java         # Media information (Volume, DRM)
│   │   ├── SensorCollector.java        # Sensor information
│   │   ├── FileInfoCollector.java      # File information
│   │   ├── glendererCollector.java      # OpenGL Renderer information
│   │   ├── batteryCollector.java        # Battery information
│   │   └── MemoryCollector.java         # Memory information
│   └── jni/                     # Native Layer Interface
│       └── NativeFingerprint.java       # JNI interface wrapper
├── model/                       # Data Model
│   └── FingerprintResult.java   # Fingerprint result data model
└── util/                        # Utility Classes
    └── Constants.java            # Constant definitions
```

## 📋 Java Layer Fingerprint Field Details

### 1. Settings Information (SettingsCollector)

Obtained via `Settings.Secure` and `Settings.Global`:

| Field Name          | Description       | API Call                                                     |
| ------------------- | ----------------- | ------------------------------------------------------------ |
| `android_id`        | Android ID        | `Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID)` |
| `mi_health_id`      | Xiaomi Health ID  | `Settings.Global.getString(resolver, "mi_health_id")`        |
| `gcbooster_uuid`    | GC Booster UUID   | `Settings.Global.getString(resolver, "gcbooster_uuid")`      |
| `key_mqs_uuid`      | MQS UUID          | `Settings.Global.getString(resolver, "key_mqs_uuid")`        |
| `ad_aaid`           | Advertising AAID  | `Settings.Global.getString(resolver, "ad_aaid")`             |
| `bluetooth_name`    | Bluetooth Name    | `Settings.Global.getString(resolver, "bluetooth_name")`      |
| `bluetooth_address` | Bluetooth Address | `Settings.Global.getString(resolver, "bluetooth_address")`   |

**Hidden API Call**:

- `ContentResolver.call(Uri.parse("content://settings/secure"), "GET_secure", "android_id", Bundle)` - Get Android ID via ContentProvider call

### 2. Bluetooth Information (BluetoothCollector)

| Field Name         | Description           | API Call                                            |
| ------------------ | --------------------- | --------------------------------------------------- |
| `bluetoothAddress` | Bluetooth MAC Address | `BluetoothAdapter.getDefaultAdapter().getAddress()` |

### 3. Serial Number Information (SerialNumberCollector)

| Field Name     | Description          | API Call                                                     |
| -------------- | -------------------- | ------------------------------------------------------------ |
| `serialNumber` | Device Serial Number | `Build.getSerial()` (Android 8.0+) or `Build.SERIAL` (Below Android 8.0) |

**Permission Required**: `READ_PHONE_STATE` is required on Android 8.0+

### 4. Telephony Information (PhoneInfoCollector)

#### Public API Fields

| Field Name              | Description             | API Call                                                     |
| ----------------------- | ----------------------- | ------------------------------------------------------------ |
| `DeviceId(IMEI)`        | Device ID/IMEI          | `TelephonyManager.getImei()` (Android 8.0+) or `TelephonyManager.getDeviceId()` |
| `DeviceSoftwareVersion` | Device Software Version | `TelephonyManager.getDeviceSoftwareVersion()`                |
| `Line1Number`           | Phone Number            | `TelephonyManager.getLine1Number()`                          |
| `NetworkCountryIso`     | Network Country Code    | `TelephonyManager.getNetworkCountryIso()`                    |
| `NetworkOperator`       | Network Operator Code   | `TelephonyManager.getNetworkOperator()`                      |
| `NetworkOperatorName`   | Network Operator Name   | `TelephonyManager.getNetworkOperatorName()`                  |
| `NetworkType`           | Network Type            | `TelephonyManager.getNetworkType()`                          |
| `PhoneType`             | Phone Type              | `TelephonyManager.getPhoneType()`                            |
| `SimCountryIso`         | SIM Country Code        | `TelephonyManager.getSimCountryIso()`                        |
| `SimOperator`           | SIM Operator Code       | `TelephonyManager.getSimOperator()`                          |
| `SimOperatorName`       | SIM Operator Name       | `TelephonyManager.getSimOperatorName()`                      |
| `SimSerialNumber`       | SIM Serial Number       | `TelephonyManager.getSimSerialNumber()`                      |
| `SimState`              | SIM State               | `TelephonyManager.getSimState()`                             |
| `SubscriberId(IMSI)`    | Subscriber ID/IMSI      | `TelephonyManager.getSubscriberId()`                         |
| `VoiceMailNumber`       | Voicemail Number        | `TelephonyManager.getVoiceMailNumber()`                      |

#### Hidden API Fields (Called via Reflection)

| Field Name               | Description                        | Hidden API Call                                        |
| ------------------------ | ---------------------------------- | ------------------------------------------------------ |
| `MEID`                   | Mobile Equipment Identifier (CDMA) | `TelephonyManager.getMeid()`                           |
| `NAI`                    | Network Access Identifier          | `TelephonyManager.getNai()`                            |
| `DataNetworkType`        | Data Network Type                  | `TelephonyManager.getDataNetworkType()`                |
| `PhoneCount`             | Phone Count (Dual SIM)             | `TelephonyManager.getPhoneCount()`                     |
| `ActiveModemCount`       | Active Modem Count                 | `TelephonyManager.getActiveModemCount()` (Android 11+) |
| `IMEI[Slot0/1]`          | Dual SIM IMEI                      | `TelephonyManager.getImei(int slotIndex)`              |
| `DeviceId[Slot0/1]`      | Dual SIM Device ID                 | `TelephonyManager.getDeviceId(int slotIndex)`          |
| `MEID[Slot0/1]`          | Dual SIM MEID                      | `TelephonyManager.getMeid(int slotIndex)`              |
| `SubscriberId[SubId0/1]` | Dual SIM IMSI                      | `TelephonyManager.getSubscriberId(int subId)`          |
| `CarrierConfig`          | Carrier Configuration              | `TelephonyManager.getCarrierConfig()`                  |

**Reflection Call Example**:

```
Method method = TelephonyManager.class.getDeclaredMethod("getMeid");
method.setAccessible(true);
String meid = (String) method.invoke(telephonyManager);
```

### 5. Build Information (BuildInfoCollector)

Obtains system properties by reflecting `android.os.SystemProperties`:

#### USB Configuration Related

| Property Name               | Description                         |
| --------------------------- | ----------------------------------- |
| `sys.usb.config`            | USB Configuration                   |
| `sys.usb.state`             | USB State                           |
| `persist.sys.usb.config`    | Persistent USB Configuration        |
| `persist.sys.usb.qmmi.func` | USB QMMI Function                   |
| `vendor.usb.mimode`         | Vendor USB Mode                     |
| `persist.vendor.usb.config` | Persistent Vendor USB Configuration |

#### Security Related

| Property Name            | Description           |
| ------------------------ | --------------------- |
| `ro.debuggable`          | Is Debuggable         |
| `init.svc.adbd`          | ADB Daemon State      |
| `ro.secure`              | Secure Mode           |
| `ro.boot.flash.locked`   | Bootloader Lock State |
| `sys.oem_unlock_allowed` | Is OEM Unlock Allowed |

#### Build ID Related

| Property Name            | Description         |
| ------------------------ | ------------------- |
| `ro.build.id`            | Build ID            |
| `ro.build.build.id`      | Build Build ID      |
| `ro.bootimage.build.id`  | Boot Image Build ID |
| `ro.odm.build.id`        | ODM Build ID        |
| `ro.product.build.id`    | Product Build ID    |
| `ro.system_ext.build.id` | System Ext Build ID |
| `ro.system.build.id`     | System Build ID     |
| `ro.vendor.build.id`     | Vendor Build ID     |

#### Security Patch

| Property Name                     | Description            |
| --------------------------------- | ---------------------- |
| `ro.build.version.security_patch` | Security Patch Version |

#### Other System Information

| Property Name           | Description          |
| ----------------------- | -------------------- |
| `ro.boot.vbmeta.digest` | VBMeta Digest        |
| `ro.netflix.bsp_rev`    | Netflix BSP Revision |
| `gsm.version.baseband`  | Baseband Version     |

#### Build Date UTC

| Property Name                  | Description               |
| ------------------------------ | ------------------------- |
| `ro.build.date.utc`            | Build Date UTC            |
| `ro.build.build.date.utc`      | Build Build Date UTC      |
| `ro.bootimage.build.date.utc`  | Boot Image Build Date UTC |
| `ro.odm.build.date.utc`        | ODM Build Date UTC        |
| `ro.product.build.date.utc`    | Product Build Date UTC    |
| `ro.system_ext.build.date.utc` | System Ext Build Date UTC |
| `ro.system.build.date.utc`     | System Build Date UTC     |
| `ro.vendor.build.date.utc`     | Vendor Build Date UTC     |

#### Display ID and Tags

| Property Name              | Description           |
| -------------------------- | --------------------- |
| `ro.build.display.id`      | Build Display ID      |
| `ro.build.tags`            | Build Tags            |
| `ro.build.build.tags`      | Build Build Tags      |
| `ro.bootimage.build.tags`  | Boot Image Build Tags |
| `ro.odm.build.tags`        | ODM Build Tags        |
| `ro.product.build.tags`    | Product Build Tags    |
| `ro.system_ext.build.tags` | System Ext Build Tags |
| `ro.system.build.tags`     | System Build Tags     |
| `ro.vendor.build.tags`     | Vendor Build Tags     |

#### Build Host and User

| Property Name             | Description          |
| ------------------------- | -------------------- |
| `ro.build.host`           | Build Host           |
| `ro.build.user`           | Build User           |
| `ro.config.ringtone`      | Default Ringtone     |
| `ro.miui.ui.version.name` | MIUI UI Version Name |

#### Build Version Incremental

| Property Name                             | Description                          |
| ----------------------------------------- | ------------------------------------ |
| `ro.build.version.incremental`            | Build Version Incremental            |
| `ro.build.build.version.incremental`      | Build Build Version Incremental      |
| `ro.bootimage.build.version.incremental`  | Boot Image Build Version Incremental |
| `ro.odm.build.version.incremental`        | ODM Build Version Incremental        |
| `ro.product.build.version.incremental`    | Product Build Version Incremental    |
| `ro.system_ext.build.version.incremental` | System Ext Build Version Incremental |
| `ro.system.build.version.incremental`     | System Build Version Incremental     |
| `ro.vendor.build.version.incremental`     | Vendor Build Version Incremental     |

#### Build Description

| Property Name          | Description       |
| ---------------------- | ----------------- |
| `ro.build.description` | Build Description |

#### Build Fingerprint

| Property Name                     | Description                  |
| --------------------------------- | ---------------------------- |
| `ro.build.fingerprint`            | Build Fingerprint            |
| `ro.build.build.fingerprint`      | Build Build Fingerprint      |
| `ro.bootimage.build.fingerprint`  | Boot Image Build Fingerprint |
| `ro.odm.build.fingerprint`        | ODM Build Fingerprint        |
| `ro.product.build.fingerprint`    | Product Build Fingerprint    |
| `ro.system_ext.build.fingerprint` | System Ext Build Fingerprint |
| `ro.system.build.fingerprint`     | System Build Fingerprint     |
| `ro.vendor.build.fingerprint`     | Vendor Build Fingerprint     |

#### Serial Number and Hardware Information

| Property Name      | Description        |
| ------------------ | ------------------ |
| `ro.boot.serialno` | Boot Serial Number |
| `ro.serialno`      | Serial Number      |
| `ro.boot.hardware` | Boot Hardware      |
| `ro.hardware`      | Hardware           |

#### CPU ABI Information

| Property Name              | Description         |
| -------------------------- | ------------------- |
| `ro.product.cpu.abilist`   | CPU ABI List        |
| `ro.product.cpu.abilist32` | CPU ABI 32-bit List |
| `ro.product.cpu.abilist64` | CPU ABI 64-bit List |

**Hidden API Call**:

```
// Get system property (String)
Class<?> systemProperties = Class.forName("android.os.SystemProperties");
Method get = systemProperties.getMethod("get", String.class, String.class);
String value = (String) get.invoke(null, "ro.build.id", "null");

// Get system property (Long)
Method getLong = systemProperties.getMethod("getLong", String.class, long.class);
long value = (Long) getLong.invoke(null, "property.key", 0L);

// Get system property (Int)
Method getInt = systemProperties.getMethod("getInt", String.class, int.class);
int value = (Integer) getInt.invoke(null, "property.key", 0);

// Get system property (Boolean)
Method getBoolean = systemProperties.getMethod("getBoolean", String.class, boolean.class);
boolean value = (Boolean) getBoolean.invoke(null, "property.key", false);
```

### 6. Account Information (AccountCollector)

| Field Name       | Description              | API Call                              |
| ---------------- | ------------------------ | ------------------------------------- |
| `Total accounts` | Total number of accounts | `AccountManager.getAccounts().length` |
| `Account Type`   | Account Type             | `Account.type`                        |
| `Account Name`   | Account Name             | `Account.name`                        |

**Permission Required**: `GET_ACCOUNTS` is required on Android 6.0+

### 7. Media Information (MediaCollector)

#### Volume Information

| Field Name   | Description         | API Call                                                  |
| ------------ | ------------------- | --------------------------------------------------------- |
| `VolumeInfo` | Music stream volume | `AudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)` |

#### DRM Information

| Field Name                  | Description          | API Call                                                     |
| --------------------------- | -------------------- | ------------------------------------------------------------ |
| `MediaDrm Device Unique ID` | DRM Device Unique ID | `MediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)` |

**DRM UUID**: Widevine UUID = `0xedef8ba979d64aceL, 0xa3c827dcd51d21edL`

### 8. Sensor Information (SensorCollector)

| Field Name    | Description         | API Call                                       |
| ------------- | ------------------- | ---------------------------------------------- |
| `Sensor List` | List of all sensors | `SensorManager.getSensorList(Sensor.TYPE_ALL)` |

### 9. OpenGL Renderer Information (glendererCollector)

| Field Name | Description                         | API Call                                 |
| ---------- | ----------------------------------- | ---------------------------------------- |
| `Renderer` | Renderer Name (Graphics Card Model) | `GLES20.glGetString(GLES20.GL_RENDERER)` |
| `Vendor`   | Renderer Vendor                     | `GLES20.glGetString(GLES20.GL_VENDOR)`   |
| `Version`  | OpenGL Version                      | `GLES20.glGetString(GLES20.GL_VERSION)`  |

**Implementation**: Obtains OpenGL information by creating an off-screen rendering context via EGL.

### 10. Battery Information (batteryCollector)

| Field Name      | Description              | API Call                                                   |
| --------------- | ------------------------ | ---------------------------------------------------------- |
| `Battery Level` | Battery level percentage | `Intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)`       |
| `Status`        | Charging status          | `Intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)`      |
| `Plugged`       | Plugged status           | `Intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)`     |
| `Health`        | Health status            | `Intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)`      |
| `Voltage`       | Voltage (mV)             | `Intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)`     |
| `Temperature`   | Temperature (0.1°C)      | `Intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)` |

**Implementation**: Obtains battery status via sticky broadcast `ACTION_BATTERY_CHANGED`.

### 11. Memory Information (MemoryCollector)

#### RAM Information

| Field Name                             | Description                        | API Call                                                     |
| -------------------------------------- | ---------------------------------- | ------------------------------------------------------------ |
| `ram_total_bytes`                      | Total RAM (bytes)                  | `ActivityManager.MemoryInfo.totalMem`                        |
| `ram_available_bytes`                  | Available RAM (bytes)              | `ActivityManager.MemoryInfo.availMem`                        |
| `ram_used_bytes`                       | Used RAM (bytes)                   | `totalMem - availMem`                                        |
| `ram_usage_percent`                    | RAM Usage (%)                      | `(usedMem / totalMem) * 100`                                 |
| `ram_low_memory`                       | Low memory flag                    | `ActivityManager.MemoryInfo.lowMemory`                       |
| `ram_threshold_bytes`                  | Low memory threshold (bytes)       | `ActivityManager.MemoryInfo.threshold`                       |
| `ram_hidden_app_threshold_bytes`       | Hidden app threshold (bytes)       | `ActivityManager.MemoryInfo.hiddenAppThreshold` (Hidden)     |
| `ram_secondary_server_threshold_bytes` | Secondary server threshold (bytes) | `ActivityManager.MemoryInfo.secondaryServerThreshold` (Hidden) |
| `ram_memory_class_mb`                  | Standard memory class (MB)         | `ActivityManager.getMemoryClass()`                           |
| `ram_large_memory_class_mb`            | Large memory class (MB)            | `ActivityManager.getLargeMemoryClass()`                      |
| `app_memory_total_bytes`               | App total memory (bytes)           | `ActivityManager.getMemoryInfo(int uid, MemoryInfo)` (Hidden) |
| `app_memory_available_bytes`           | App available memory (bytes)       | `ActivityManager.getMemoryInfo(int uid, MemoryInfo)` (Hidden) |
| `app_heap_max_bytes`                   | Java heap max memory (bytes)       | `Runtime.maxMemory()`                                        |
| `app_heap_total_bytes`                 | Java heap total memory (bytes)     | `Runtime.totalMemory()`                                      |
| `app_heap_free_bytes`                  | Java heap free memory (bytes)      | `Runtime.freeMemory()`                                       |
| `app_heap_used_bytes`                  | Java heap used memory (bytes)      | `totalMemory - freeMemory`                                   |
| `app_heap_usage_percent`               | Java heap usage (%)                | `(usedMemory / maxMemory) * 100`                             |

#### ROM Information

| Field Name                         | Description                        | API Call                                               |
| ---------------------------------- | ---------------------------------- | ------------------------------------------------------ |
| `internal_storage_total_bytes`     | Internal storage total (bytes)     | `StatFs.getBlockCountLong() * getBlockSizeLong()`      |
| `internal_storage_available_bytes` | Internal storage available (bytes) | `StatFs.getAvailableBlocksLong() * getBlockSizeLong()` |
| `internal_storage_used_bytes`      | Internal storage used (bytes)      | `totalSize - availableSize`                            |
| `internal_storage_usage_percent`   | Internal storage usage (%)         | `(usedSize / totalSize) * 100`                         |
| `external_storage_total_bytes`     | External storage total (bytes)     | `StatFs.getBlockCountLong() * getBlockSizeLong()`      |
| `external_storage_available_bytes` | External storage available (bytes) | `StatFs.getAvailableBlocksLong() * getBlockSizeLong()` |
| `external_storage_used_bytes`      | External storage used (bytes)      | `totalSize - availableSize`                            |
| `external_storage_usage_percent`   | External storage usage (%)         | `(usedSize / totalSize) * 100`                         |
| `external_storage_state`           | External storage state             | `Environment.getExternalStorageState()`                |

**Hidden API Call**:

```
// Get MemoryInfo hidden fields
Field hiddenAppThresholdField = ActivityManager.MemoryInfo.class.getDeclaredField("hiddenAppThreshold");
hiddenAppThresholdField.setAccessible(true);
long hiddenAppThreshold = hiddenAppThresholdField.getLong(memoryInfo);

// Get memory info for a specific UID
Method getMemoryInfoForUidMethod = ActivityManager.class.getDeclaredMethod("getMemoryInfo", int.class, ActivityManager.MemoryInfo.class);
getMemoryInfoForUidMethod.setAccessible(true);
getMemoryInfoForUidMethod.invoke(activityManager, uid, memoryInfo);
```

## 📋 Native Layer Fingerprint Field Details

### 1. System Property Information (SystemPropertyCollector)

Obtains system properties via `__system_property_get()`, similar to the Java layer but read directly from Native:

#### USB Configuration Related

- `sys.usb.config`
- `sys.usb.state`
- `persist.sys.usb.config`
- `persist.sys.usb.qmmi.func`
- `vendor.usb.mimode`
- `persist.vendor.usb.config`

#### Security Related

- `ro.debuggable`
- `init.svc.adbd`
- `ro.secure`
- `ro.boot.flash.locked`
- `sys.oem_unlock_allowed`

#### Build ID Related

- `ro.build.id`
- `ro.build.build.id`
- `ro.bootimage.build.id`
- `ro.odm.build.id`
- `ro.product.build.id`
- `ro.system_ext.build.id`
- `ro.system.build.id`
- `ro.vendor.build.id`

#### SDK Version

- `ro.build.version.sdk`

#### Security Patch

- `ro.build.version.security_patch`

#### Other System Information

- `ro.boot.vbmeta.digest`
- `ro.netflix.bsp_rev`
- `gsm.version.baseband`

#### Build Date UTC

- `ro.build.date.utc`
- `ro.build.build.date.utc`
- `ro.bootimage.build.date.utc`
- `ro.odm.build.date.utc`
- `ro.product.build.date.utc`
- `ro.system_ext.build.date.utc`
- `ro.system.build.date.utc`
- `ro.vendor.build.date.utc`

#### Display ID and Tags

- `ro.build.display.id`
- `ro.build.tags`
- `ro.build.build.tags`
- `ro.bootimage.build.tags`
- `ro.odm.build.tags`
- `ro.product.build.tags`
- `ro.system_ext.build.tags`
- `ro.system.build.tags`
- `ro.vendor.build.tags`

#### Build Host and User

- `ro.build.host`
- `ro.build.user`
- `ro.config.ringtone`
- `ro.miui.ui.version.name`

#### Build Version Incremental

- `ro.build.version.incremental`
- `ro.build.build.version.incremental`
- `ro.bootimage.build.version.incremental`
- `ro.odm.build.version.incremental`
- `ro.product.build.version.incremental`
- `ro.system_ext.build.version.incremental`
- `ro.system.build.version.incremental`
- `ro.vendor.build.version.incremental`

#### Build Description

- `ro.build.description`

#### Build Fingerprint

- `ro.build.fingerprint`
- `ro.build.build.fingerprint`
- `ro.bootimage.build.fingerprint`
- `ro.odm.build.fingerprint`
- `ro.product.build.fingerprint`
- `ro.system_ext.build.fingerprint`
- `ro.system.build.fingerprint`
- `ro.vendor.build.fingerprint`

#### Other System Properties

- `ro.board.platform`
- `ro.product.cpu.abi`
- `ro.boot.verifiedbootstate`
- `ro.boot.vbmeta.device_state`
- `ro.treble.enabled`

**Native API Call**:

```
#include <sys/system_properties.h>

char value[PROP_VALUE_MAX] = {0};
int len = __system_property_get("ro.build.id", value);
```

### 2. DRM Information (DRMCollector)

| Field Name                           | Description                   | Native API Call                                              |
| ------------------------------------ | ----------------------------- | ------------------------------------------------------------ |
| `MediaDrm Device Unique ID (Base64)` | DRM Device Unique ID (Base64) | `AMediaDrm_getPropertyByteArray(mediaDrm, PROPERTY_DEVICE_UNIQUE_ID, &byteArray)` |
| `MediaDrm Device Unique ID (Hex)`    | DRM Device Unique ID (Hex)    | Same as above, converted to hex                              |
| `Length`                             | ID Length (bytes)             | `byteArray.length`                                           |

**Native API Call**:

```
#include <media/NdkMediaDrm.h>

AMediaDrm* mediaDrm = AMediaDrm_createByUUID(WIDEVINE_UUID);
AMediaDrmByteArray byteArray;
AMediaDrm_getPropertyByteArray(mediaDrm, PROPERTY_DEVICE_UNIQUE_ID, &byteArray);
```

### 3. Network Interface Information (MacAddressCollector)

| Field Name       | Description            | Native API Call                                     |
| ---------------- | ---------------------- | --------------------------------------------------- |
| `Interface Name` | Network Interface Name | `getifaddrs()` / `ioctl(SIOCGIFHWADDR)`             |
| `MAC Address`    | MAC Address            | `getifaddrs()` (AF_PACKET) / `ioctl(SIOCGIFHWADDR)` |
| `IPv4 Address`   | IPv4 Address           | `getifaddrs()` (AF_INET) + `getnameinfo()`          |
| `IPv6 Address`   | IPv6 Address           | `getifaddrs()` (AF_INET6) + `getnameinfo()`         |

**Native API Call**:

```
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <netpacket/packet.h>

// Method 1: Using getifaddrs (netlink)
struct ifaddrs* ifap;
getifaddrs(&ifap);
// Iterate to get MAC and IP addresses

// Method 2: Using ioctl (fallback)
struct ifreq ifr;
ioctl(sockfd, SIOCGIFHWADDR, &ifr);
```

### 4. File Fingerprint Information (NativeFileCollector)

#### Core Hardware & Kernel Feature Files

| File Path                                  | Description                |
| ------------------------------------------ | -------------------------- |
| `/proc/cpuinfo`                            | CPU Information            |
| `/proc/version`                            | Kernel Version Information |
| `/proc/meminfo`                            | Memory Information         |
| `/proc/iomem`                              | I/O Memory Map             |
| `/proc/misc`                               | Miscellaneous Devices      |
| `/sys/devices/soc0/family`                 | SoC Family                 |
| `/sys/devices/soc0/machine`                | SoC Machine Model          |
| `/sys/class/power_supply/battery/capacity` | Battery Capacity           |
| `/sys/class/power_supply/battery/status`   | Battery Status             |

#### Environment & Security Detection Files

| File Path                               | Description               |
| --------------------------------------- | ------------------------- |
| `/proc/sys/kernel/random/boot_id`       | Boot ID                   |
| `/proc/sys/kernel/osrelease`            | OS Release                |
| `/sys/fs/selinux/enforce`               | SELinux Enforce Mode      |
| `/proc/sys/kernel/random/entropy_avail` | Available Entropy         |
| `/proc/uptime`                          | System Uptime             |
| `/sys/class/thermal/thermal_zone0/temp` | CPU Temperature           |
| `/proc/sys/vm/overcommit_memory`        | Memory Overcommit Setting |

#### Mount Points & Input Device Files

| File Path                 | Description         |
| ------------------------- | ------------------- |
| `/proc/self/mountinfo`    | Mount Information   |
| `/proc/mounts`            | Mount Points        |
| `/proc/filesystems`       | File System Types   |
| `/proc/bus/input/devices` | Input Devices       |
| `/proc/net/unix`          | Unix Domain Sockets |

#### Kernel Information (via uname)

| Field Name    | Description            | Native API Call                           |
| ------------- | ---------------------- | ----------------------------------------- |
| `System Name` | System Name            | `uname(&info)` -> `info.sysname`          |
| `Node Name`   | Node Name              | `uname(&info)` -> `info.nodename`         |
| `Release`     | Kernel Release         | `uname(&info)` -> `info.release`          |
| `Version`     | Kernel Version Details | `uname(&info)` -> `info.version`          |
| `Machine`     | Machine Architecture   | `uname(&info)` -> `info.machine`          |
| `Domain Name` | Domain Name            | `uname(&info)` -> `info.domainname` (GNU) |

**Native API Call**:

```
#include <sys/utsname.h>

struct utsname info;
uname(&info);
```

#### System Configuration (via sysconf)

| Field Name                  | Description                    | Native API Call                          |
| --------------------------- | ------------------------------ | ---------------------------------------- |
| `CPU Cores (Online)`        | Online CPU Cores               | `sysconf(_SC_NPROCESSORS_ONLN)`          |
| `CPU Cores (Configured)`    | Configured CPU Cores           | `sysconf(_SC_NPROCESSORS_CONF)`          |
| `Page Size`                 | Page Size (bytes)              | `sysconf(_SC_PAGESIZE)`                  |
| `Clock Ticks per Second`    | Clock Ticks per Second         | `sysconf(_SC_CLK_TCK)`                   |
| `Physical Pages`            | Physical Pages                 | `sysconf(_SC_PHYS_PAGES)`                |
| `Total Physical Memory`     | Total Physical Memory (MB)     | `phys_pages * page_size / 1024 / 1024`   |
| `Available Physical Pages`  | Available Physical Pages       | `sysconf(_SC_AVPHYS_PAGES)`              |
| `Available Physical Memory` | Available Physical Memory (MB) | `avphys_pages * page_size / 1024 / 1024` |

**Native API Call**:

```
#include <unistd.h>

long nproc = sysconf(_SC_NPROCESSORS_ONLN);
long page_size = sysconf(_SC_PAGESIZE);
long phys_pages = sysconf(_SC_PHYS_PAGES);
```

#### Zygisk Injection Detection

Detects suspicious library injections by reading `/proc/self/maps`:

| Field Name                   | Description                             |
| ---------------------------- | --------------------------------------- |
| `Total Mappings`             | Total Mappings                          |
| `Library Mappings`           | Library Mappings                        |
| `Suspicious Libraries Found` | Number of suspicious libraries detected |
| `Suspicious Libraries`       | List of suspicious libraries            |

**Suspicious Keywords Detected**:

- `magisk`
- `zygisk`
- `riru`
- `lsposed`
- `edxposed`
- `xposed`
- `/data/local/tmp/`
- `/data/adb/`
- `/sbin/`
- `/dev/`

## 🔓 Hidden API Call Details

### 1. Bypassing Hidden API Restrictions

The project uses the `FreeReflection` library to bypass Android Hidden API restrictions:

```
// FuckHideApi.java
public class FuckHideApi extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Reflection.unseal(base);  // Bypass Hidden API restrictions
    }
}
```

**Dependency**: `com.github.tiann:FreeReflection:3.2.0`

### 2. TelephonyManager Hidden APIs

| Method Name                  | Parameters      | Description                             | Invocation                                                   |
| ---------------------------- | --------------- | --------------------------------------- | ------------------------------------------------------------ |
| `getMeid()`                  | None            | Get MEID                                | `TelephonyManager.class.getDeclaredMethod("getMeid")`        |
| `getNai()`                   | None            | Get NAI                                 | `TelephonyManager.class.getDeclaredMethod("getNai")`         |
| `getDataNetworkType()`       | None            | Get data network type                   | `TelephonyManager.class.getDeclaredMethod("getDataNetworkType")` |
| `getPhoneCount()`            | None            | Get phone count                         | `TelephonyManager.class.getDeclaredMethod("getPhoneCount")`  |
| `getActiveModemCount()`      | None            | Get active modem count                  | `TelephonyManager.class.getDeclaredMethod("getActiveModemCount")` (Android 11+) |
| `getImei(int slotIndex)`     | `int slotIndex` | Get IMEI for a specific slot            | `TelephonyManager.class.getDeclaredMethod("getImei", int.class)` |
| `getDeviceId(int slotIndex)` | `int slotIndex` | Get Device ID for a specific slot       | `TelephonyManager.class.getDeclaredMethod("getDeviceId", int.class)` |
| `getMeid(int slotIndex)`     | `int slotIndex` | Get MEID for a specific slot            | `TelephonyManager.class.getDeclaredMethod("getMeid", int.class)` |
| `getSubscriberId(int subId)` | `int subId`     | Get IMSI for a specific subscription ID | `TelephonyManager.class.getDeclaredMethod("getSubscriberId", int.class)` |
| `getCarrierConfig()`         | None            | Get carrier configuration               | `TelephonyManager.class.getDeclaredMethod("getCarrierConfig")` |

### 3. SystemProperties Hidden API

| Method Name                                    | Parameters                         | Description          | Invocation                                                   |
| ---------------------------------------------- | ---------------------------------- | -------------------- | ------------------------------------------------------------ |
| `get(String key, String defaultValue)`         | `String key, String defaultValue`  | Get string property  | `SystemProperties.class.getMethod("get", String.class, String.class)` |
| `getLong(String key, long defaultValue)`       | `String key, long defaultValue`    | Get long property    | `SystemProperties.class.getMethod("getLong", String.class, long.class)` |
| `getInt(String key, int defaultValue)`         | `String key, int defaultValue`     | Get int property     | `SystemProperties.class.getMethod("getInt", String.class, int.class)` |
| `getBoolean(String key, boolean defaultValue)` | `String key, boolean defaultValue` | Get boolean property | `SystemProperties.class.getMethod("getBoolean", String.class, boolean.class)` |

### 4. ActivityManager.MemoryInfo Hidden Fields

| Field Name                 | Type   | Description                | Access                                                       |
| -------------------------- | ------ | -------------------------- | ------------------------------------------------------------ |
| `hiddenAppThreshold`       | `long` | Hidden app threshold       | `MemoryInfo.class.getDeclaredField("hiddenAppThreshold")`    |
| `secondaryServerThreshold` | `long` | Secondary server threshold | `MemoryInfo.class.getDeclaredField("secondaryServerThreshold")` |

### 5. ActivityManager Hidden Methods

| Method Name                                  | Parameters                    | Description                        | Invocation                                                   |
| -------------------------------------------- | ----------------------------- | ---------------------------------- | ------------------------------------------------------------ |
| `getMemoryInfo(int uid, MemoryInfo outInfo)` | `int uid, MemoryInfo outInfo` | Get memory info for a specific UID | `ActivityManager.class.getDeclaredMethod("getMemoryInfo", int.class, ActivityManager.MemoryInfo.class)` |

### 6. ContentResolver Hidden API

| Method Name                                               | Parameters                                          | Description                   | Invocation                                                   |
| --------------------------------------------------------- | --------------------------------------------------- | ----------------------------- | ------------------------------------------------------------ |
| `call(Uri uri, String method, String arg, Bundle extras)` | `Uri uri, String method, String arg, Bundle extras` | Call a ContentProvider method | `ContentResolver.call(Uri.parse("content://settings/secure"), "GET_secure", "android_id", Bundle)` |

## 🚀 How to Use

### Basic Usage

```
// Initialize the service
FingerprintService fingerprintService = new FingerprintService(context);

// Collect Java layer fingerprint
FingerprintResult javaResult = fingerprintService.collectJavaFingerprint();
String javaOutput = javaResult.toString();

// Collect Native layer fingerprint
FingerprintResult nativeResult = fingerprintService.collectNativeFingerprint();
String nativeOutput = nativeResult.toNativeString();

// Get cleaned Java layer fingerprint (JSON format)
JSONObject cleanedJavaData = fingerprintService.collectAndCleanJavaFingerprint();

// Get cleaned complete fingerprint (includes Java and Native layers)
JSONObject cleanedAllData = fingerprintService.collectAndCleanAllFingerprint();

// Get MAC address (Native layer)
String macAddress = fingerprintService.getMacAddress();
```

### Using a Collector Individually

```
// Collect only Settings information
SettingsCollector settingsCollector = new SettingsCollector(context);
String settings = settingsCollector.collectSettings();
String androidId = settingsCollector.getAndroidId();

// Collect only Telephony information
PhoneInfoCollector phoneInfoCollector = new PhoneInfoCollector(context);
String phoneInfo = phoneInfoCollector.getPhoneInfo();
```

## 📦 Dependencies

### Gradle Dependencies

```
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'com.github.tiann:FreeReflection:3.2.0'  // Hidden API bypass
}
```

### Native Dependencies

- Android NDK
- CMake 3.22.1+
- C++ Standard Library
- Android Media NDK (for DRM)

## ⚙️ Build Requirements

- **Compile SDK**: 35
- **Min SDK**: 33
- **Target SDK**: 35
- **Java Version**: 11
- **NDK Version**: Latest stable
- **CMake Version**: 3.22.1

## 📝 Permissions Required

Some features require the following permissions (declare in `AndroidManifest.xml`):

```
<!-- Telephony info (Android 8.0+) -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />

<!-- Account info (Android 6.0+) -->
<uses-permission android:name="android.permission.GET_ACCOUNTS" />

<!-- Bluetooth info -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
```

**Note**: Android 10+ has strict restrictions on accessing sensitive information (like IMEI), and regular apps may not be able to obtain it.

## 🔒 Security Precautions

1. **Hidden API Usage**: The project uses reflection to call Hidden APIs, which may behave inconsistently across different Android versions.
2. **Permission Restrictions**: Android 10+ has strict restrictions on device identifier access; some information may be unobtainable.
3. **Privacy Protection**: The collected device fingerprint information may contain sensitive data; please handle it properly.
4. **Root Detection**: The project includes Root and Zygisk injection detection features, which can be used for security analysis.

## 📄 License

This project is for learning and research purposes only.

## 🤝 Contributing

Issues and Pull Requests are welcome.

## 📧 Contact

If you have questions or suggestions, please provide feedback via Issues.

**Disclaimer**: This project is intended for legitimate security research and device identification purposes only. Do not use it for illegal activities.