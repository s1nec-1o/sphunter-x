# spHunter - Android 设备指纹采集工具

## 📖 项目简介

spHunter 是一个功能强大的 Android 设备指纹采集工具，能够从 Java 层和 Native 层全面收集设备指纹信息。该项目采用模块化架构设计，通过反射机制调用 Hidden API，并使用 JNI 技术访问底层系统信息，为设备识别和安全分析提供全面的数据支持。

## ✨ 主要功能

- **Java 层指纹采集**：通过 Android SDK API 和反射机制获取设备信息
- **Native 层指纹采集**：通过 JNI 调用 C++ 代码直接访问系统底层信息
- **Hidden API 调用**：使用反射机制绕过 Android 限制，访问隐藏的系统 API
- **数据清洗与结构化**：将采集的原始数据清洗并格式化为 JSON 格式
- **安全分析**：检测模拟器、Root、调试模式、Zygisk 注入等安全风险

## 🏗️ 项目架构

```
com.sheep.sphunter/
├── ui/                          # UI 层
│   └── MainActivity.java        # 主界面
├── fingerprint/                 # 指纹采集核心模块
│   ├── FingerprintService.java  # 指纹采集服务（统一入口）
│   ├── device/                  # 设备信息采集器
│   │   ├── SettingsCollector.java      # Settings 信息采集
│   │   ├── BluetoothCollector.java     # 蓝牙信息采集
│   │   ├── SerialNumberCollector.java  # 序列号采集
│   │   ├── PhoneInfoCollector.java     # 电话信息采集
│   │   ├── BuildInfoCollector.java     # Build 信息采集
│   │   ├── AccountCollector.java       # 账户信息采集
│   │   ├── MediaCollector.java         # 媒体信息采集（音量、DRM）
│   │   ├── SensorCollector.java        # 传感器信息采集
│   │   ├── FileInfoCollector.java      # 文件信息采集
│   │   ├── glendererCollector.java      # OpenGL 渲染器信息采集
│   │   ├── batteryCollector.java        # 电池信息采集
│   │   └── MemoryCollector.java         # 内存信息采集
│   └── jni/                     # Native 层接口
│       └── NativeFingerprint.java       # JNI 接口封装
├── model/                       # 数据模型
│   └── FingerprintResult.java   # 指纹结果数据模型
└── util/                        # 工具类
    └── Constants.java            # 常量定义
```

## 📋 Java 层指纹采集字段详情

### 1. Settings 信息 (SettingsCollector)

通过 `Settings.Secure` 和 `Settings.Global` 获取：

| 字段名 | 说明 | API 调用 |
|--------|------|----------|
| `android_id` | Android ID | `Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID)` |
| `mi_health_id` | 小米健康 ID | `Settings.Global.getString(resolver, "mi_health_id")` |
| `gcbooster_uuid` | GC Booster UUID | `Settings.Global.getString(resolver, "gcbooster_uuid")` |
| `key_mqs_uuid` | MQS UUID | `Settings.Global.getString(resolver, "key_mqs_uuid")` |
| `ad_aaid` | 广告 AAID | `Settings.Global.getString(resolver, "ad_aaid")` |
| `bluetooth_name` | 蓝牙名称 | `Settings.Global.getString(resolver, "bluetooth_name")` |
| `bluetooth_address` | 蓝牙地址 | `Settings.Global.getString(resolver, "bluetooth_address")` |

**Hidden API 调用**：
- `ContentResolver.call(Uri.parse("content://settings/secure"), "GET_secure", "android_id", Bundle)` - 通过 ContentProvider 调用获取 Android ID

### 2. 蓝牙信息 (BluetoothCollector)

| 字段名 | 说明 | API 调用 |
|--------|------|----------|
| `bluetoothAddress` | 蓝牙 MAC 地址 | `BluetoothAdapter.getDefaultAdapter().getAddress()` |

### 3. 序列号信息 (SerialNumberCollector)

| 字段名 | 说明 | API 调用 |
|--------|------|----------|
| `serialNumber` | 设备序列号 | `Build.getSerial()` (Android 8.0+) 或 `Build.SERIAL` (Android 8.0 以下) |

**权限要求**：Android 8.0+ 需要 `READ_PHONE_STATE` 权限

### 4. 电话信息 (PhoneInfoCollector)

#### 公开 API 字段

| 字段名 | 说明 | API 调用 |
|--------|------|----------|
| `DeviceId(IMEI)` | 设备 ID/IMEI | `TelephonyManager.getImei()` (Android 8.0+) 或 `TelephonyManager.getDeviceId()` |
| `DeviceSoftwareVersion` | 设备软件版本 | `TelephonyManager.getDeviceSoftwareVersion()` |
| `Line1Number` | 电话号码 | `TelephonyManager.getLine1Number()` |
| `NetworkCountryIso` | 网络国家代码 | `TelephonyManager.getNetworkCountryIso()` |
| `NetworkOperator` | 网络运营商代码 | `TelephonyManager.getNetworkOperator()` |
| `NetworkOperatorName` | 网络运营商名称 | `TelephonyManager.getNetworkOperatorName()` |
| `NetworkType` | 网络类型 | `TelephonyManager.getNetworkType()` |
| `PhoneType` | 电话类型 | `TelephonyManager.getPhoneType()` |
| `SimCountryIso` | SIM 卡国家代码 | `TelephonyManager.getSimCountryIso()` |
| `SimOperator` | SIM 卡运营商代码 | `TelephonyManager.getSimOperator()` |
| `SimOperatorName` | SIM 卡运营商名称 | `TelephonyManager.getSimOperatorName()` |
| `SimSerialNumber` | SIM 卡序列号 | `TelephonyManager.getSimSerialNumber()` |
| `SimState` | SIM 卡状态 | `TelephonyManager.getSimState()` |
| `SubscriberId(IMSI)` | 用户 ID/IMSI | `TelephonyManager.getSubscriberId()` |
| `VoiceMailNumber` | 语音信箱号码 | `TelephonyManager.getVoiceMailNumber()` |

#### Hidden API 字段（通过反射调用）

| 字段名 | 说明 | Hidden API 调用 |
|--------|------|-----------------|
| `MEID` | 移动设备标识符（CDMA） | `TelephonyManager.getMeid()` |
| `NAI` | 网络访问标识符 | `TelephonyManager.getNai()` |
| `DataNetworkType` | 数据网络类型 | `TelephonyManager.getDataNetworkType()` |
| `PhoneCount` | 电话数量（双卡） | `TelephonyManager.getPhoneCount()` |
| `ActiveModemCount` | 活动调制解调器数量 | `TelephonyManager.getActiveModemCount()` (Android 11+) |
| `IMEI[Slot0/1]` | 双卡 IMEI | `TelephonyManager.getImei(int slotIndex)` |
| `DeviceId[Slot0/1]` | 双卡设备 ID | `TelephonyManager.getDeviceId(int slotIndex)` |
| `MEID[Slot0/1]` | 双卡 MEID | `TelephonyManager.getMeid(int slotIndex)` |
| `SubscriberId[SubId0/1]` | 双卡 IMSI | `TelephonyManager.getSubscriberId(int subId)` |
| `CarrierConfig` | 运营商配置信息 | `TelephonyManager.getCarrierConfig()` |

**反射调用示例**：
```java
Method method = TelephonyManager.class.getDeclaredMethod("getMeid");
method.setAccessible(true);
String meid = (String) method.invoke(telephonyManager);
```

### 5. Build 信息 (BuildInfoCollector)

通过反射调用 `android.os.SystemProperties` 获取系统属性：

#### USB 配置相关

| 属性名 | 说明 |
|--------|------|
| `sys.usb.config` | USB 配置 |
| `sys.usb.state` | USB 状态 |
| `persist.sys.usb.config` | 持久化 USB 配置 |
| `persist.sys.usb.qmmi.func` | USB QMMI 功能 |
| `vendor.usb.mimode` | 厂商 USB 模式 |
| `persist.vendor.usb.config` | 持久化厂商 USB 配置 |

#### 安全相关

| 属性名 | 说明 |
|--------|------|
| `ro.debuggable` | 是否可调试 |
| `init.svc.adbd` | ADB 守护进程状态 |
| `ro.secure` | 安全模式 |
| `ro.boot.flash.locked` | 引导加载程序锁定状态 |
| `sys.oem_unlock_allowed` | 是否允许 OEM 解锁 |

#### Build ID 相关

| 属性名 | 说明 |
|--------|------|
| `ro.build.id` | Build ID |
| `ro.build.build.id` | Build Build ID |
| `ro.bootimage.build.id` | Boot Image Build ID |
| `ro.odm.build.id` | ODM Build ID |
| `ro.product.build.id` | Product Build ID |
| `ro.system_ext.build.id` | System Ext Build ID |
| `ro.system.build.id` | System Build ID |
| `ro.vendor.build.id` | Vendor Build ID |

#### 安全补丁

| 属性名 | 说明 |
|--------|------|
| `ro.build.version.security_patch` | 安全补丁版本 |

#### 其他系统信息

| 属性名 | 说明 |
|--------|------|
| `ro.boot.vbmeta.digest` | VBMeta 摘要 |
| `ro.netflix.bsp_rev` | Netflix BSP 版本 |
| `gsm.version.baseband` | 基带版本 |

#### Build 日期 UTC

| 属性名 | 说明 |
|--------|------|
| `ro.build.date.utc` | Build 日期 UTC |
| `ro.build.build.date.utc` | Build Build 日期 UTC |
| `ro.bootimage.build.date.utc` | Boot Image Build 日期 UTC |
| `ro.odm.build.date.utc` | ODM Build 日期 UTC |
| `ro.product.build.date.utc` | Product Build 日期 UTC |
| `ro.system_ext.build.date.utc` | System Ext Build 日期 UTC |
| `ro.system.build.date.utc` | System Build 日期 UTC |
| `ro.vendor.build.date.utc` | Vendor Build 日期 UTC |

#### Display ID 和 Tags

| 属性名 | 说明 |
|--------|------|
| `ro.build.display.id` | Build Display ID |
| `ro.build.tags` | Build Tags |
| `ro.build.build.tags` | Build Build Tags |
| `ro.bootimage.build.tags` | Boot Image Build Tags |
| `ro.odm.build.tags` | ODM Build Tags |
| `ro.product.build.tags` | Product Build Tags |
| `ro.system_ext.build.tags` | System Ext Build Tags |
| `ro.system.build.tags` | System Build Tags |
| `ro.vendor.build.tags` | Vendor Build Tags |

#### Build Host 和 User

| 属性名 | 说明 |
|--------|------|
| `ro.build.host` | Build 主机名 |
| `ro.build.user` | Build 用户名 |
| `ro.config.ringtone` | 默认铃声 |
| `ro.miui.ui.version.name` | MIUI UI 版本名称 |

#### Build Version Incremental

| 属性名 | 说明 |
|--------|------|
| `ro.build.version.incremental` | Build 版本增量 |
| `ro.build.build.version.incremental` | Build Build 版本增量 |
| `ro.bootimage.build.version.incremental` | Boot Image Build 版本增量 |
| `ro.odm.build.version.incremental` | ODM Build 版本增量 |
| `ro.product.build.version.incremental` | Product Build 版本增量 |
| `ro.system_ext.build.version.incremental` | System Ext Build 版本增量 |
| `ro.system.build.version.incremental` | System Build 版本增量 |
| `ro.vendor.build.version.incremental` | Vendor Build 版本增量 |

#### Build Description

| 属性名 | 说明 |
|--------|------|
| `ro.build.description` | Build 描述 |

#### Build Fingerprint

| 属性名 | 说明 |
|--------|------|
| `ro.build.fingerprint` | Build 指纹 |
| `ro.build.build.fingerprint` | Build Build 指纹 |
| `ro.bootimage.build.fingerprint` | Boot Image Build 指纹 |
| `ro.odm.build.fingerprint` | ODM Build 指纹 |
| `ro.product.build.fingerprint` | Product Build 指纹 |
| `ro.system_ext.build.fingerprint` | System Ext Build 指纹 |
| `ro.system.build.fingerprint` | System Build 指纹 |
| `ro.vendor.build.fingerprint` | Vendor Build 指纹 |

#### 序列号和硬件信息

| 属性名 | 说明 |
|--------|------|
| `ro.boot.serialno` | 引导序列号 |
| `ro.serialno` | 序列号 |
| `ro.boot.hardware` | 引导硬件 |
| `ro.hardware` | 硬件 |

#### CPU ABI 信息

| 属性名 | 说明 |
|--------|------|
| `ro.product.cpu.abilist` | CPU ABI 列表 |
| `ro.product.cpu.abilist32` | CPU ABI 32 位列表 |
| `ro.product.cpu.abilist64` | CPU ABI 64 位列表 |

**Hidden API 调用**：
```java
// 获取系统属性（字符串）
Class<?> systemProperties = Class.forName("android.os.SystemProperties");
Method get = systemProperties.getMethod("get", String.class, String.class);
String value = (String) get.invoke(null, "ro.build.id", "null");

// 获取系统属性（长整型）
Method getLong = systemProperties.getMethod("getLong", String.class, long.class);
long value = (Long) getLong.invoke(null, "property.key", 0L);

// 获取系统属性（整型）
Method getInt = systemProperties.getMethod("getInt", String.class, int.class);
int value = (Integer) getInt.invoke(null, "property.key", 0);

// 获取系统属性（布尔型）
Method getBoolean = systemProperties.getMethod("getBoolean", String.class, boolean.class);
boolean value = (Boolean) getBoolean.invoke(null, "property.key", false);
```

### 6. 账户信息 (AccountCollector)

| 字段名 | 说明 | API 调用 |
|--------|------|----------|
| `Total accounts` | 账户总数 | `AccountManager.getAccounts().length` |
| `Account Type` | 账户类型 | `Account.type` |
| `Account Name` | 账户名称 | `Account.name` |

**权限要求**：Android 6.0+ 需要 `GET_ACCOUNTS` 权限

### 7. 媒体信息 (MediaCollector)

#### 音量信息

| 字段名 | 说明 | API 调用 |
|--------|------|----------|
| `VolumeInfo` | 音乐流音量 | `AudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)` |

#### DRM 信息

| 字段名 | 说明 | API 调用 |
|--------|------|----------|
| `MediaDrm Device Unique ID` | DRM 设备唯一 ID | `MediaDrm.getPropertyByteArray(MediaDrm.PROPERTY_DEVICE_UNIQUE_ID)` |

**DRM UUID**：Widevine UUID = `0xedef8ba979d64aceL, 0xa3c827dcd51d21edL`

### 8. 传感器信息 (SensorCollector)

| 字段名 | 说明 | API 调用 |
|--------|------|----------|
| `Sensor List` | 所有传感器列表 | `SensorManager.getSensorList(Sensor.TYPE_ALL)` |

### 9. OpenGL 渲染器信息 (glendererCollector)

| 字段名 | 说明 | API 调用 |
|--------|------|----------|
| `Renderer` | 渲染器名称（显卡型号） | `GLES20.glGetString(GLES20.GL_RENDERER)` |
| `Vendor` | 渲染器厂商 | `GLES20.glGetString(GLES20.GL_VENDOR)` |
| `Version` | OpenGL 版本 | `GLES20.glGetString(GLES20.GL_VERSION)` |

**实现方式**：通过 EGL 创建离屏渲染上下文获取 OpenGL 信息

### 10. 电池信息 (batteryCollector)

| 字段名 | 说明 | API 调用 |
|--------|------|----------|
| `Battery Level` | 电池电量百分比 | `Intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)` |
| `Status` | 充电状态 | `Intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)` |
| `Plugged` | 插拔状态 | `Intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)` |
| `Health` | 健康状态 | `Intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)` |
| `Voltage` | 电压（毫伏） | `Intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)` |
| `Temperature` | 温度（0.1°C） | `Intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)` |

**实现方式**：通过粘性广播 `ACTION_BATTERY_CHANGED` 获取电池状态

### 11. 内存信息 (MemoryCollector)

#### RAM 信息

| 字段名 | 说明 | API 调用 |
|--------|------|----------|
| `ram_total_bytes` | 总内存（字节） | `ActivityManager.MemoryInfo.totalMem` |
| `ram_available_bytes` | 可用内存（字节） | `ActivityManager.MemoryInfo.availMem` |
| `ram_used_bytes` | 已使用内存（字节） | `totalMem - availMem` |
| `ram_usage_percent` | 内存使用率 | `(usedMem / totalMem) * 100` |
| `ram_low_memory` | 低内存标志 | `ActivityManager.MemoryInfo.lowMemory` |
| `ram_threshold_bytes` | 低内存阈值（字节） | `ActivityManager.MemoryInfo.threshold` |
| `ram_hidden_app_threshold_bytes` | 隐藏应用阈值（字节） | `ActivityManager.MemoryInfo.hiddenAppThreshold` (Hidden) |
| `ram_secondary_server_threshold_bytes` | 二级服务器阈值（字节） | `ActivityManager.MemoryInfo.secondaryServerThreshold` (Hidden) |
| `ram_memory_class_mb` | 标准内存类（MB） | `ActivityManager.getMemoryClass()` |
| `ram_large_memory_class_mb` | 大内存类（MB） | `ActivityManager.getLargeMemoryClass()` |
| `app_memory_total_bytes` | 应用总内存（字节） | `ActivityManager.getMemoryInfo(int uid, MemoryInfo)` (Hidden) |
| `app_memory_available_bytes` | 应用可用内存（字节） | `ActivityManager.getMemoryInfo(int uid, MemoryInfo)` (Hidden) |
| `app_heap_max_bytes` | Java 堆最大内存（字节） | `Runtime.maxMemory()` |
| `app_heap_total_bytes` | Java 堆总内存（字节） | `Runtime.totalMemory()` |
| `app_heap_free_bytes` | Java 堆空闲内存（字节） | `Runtime.freeMemory()` |
| `app_heap_used_bytes` | Java 堆已使用内存（字节） | `totalMemory - freeMemory` |
| `app_heap_usage_percent` | Java 堆使用率 | `(usedMemory / maxMemory) * 100` |

#### ROM 信息

| 字段名 | 说明 | API 调用 |
|--------|------|----------|
| `internal_storage_total_bytes` | 内部存储总空间（字节） | `StatFs.getBlockCountLong() * getBlockSizeLong()` |
| `internal_storage_available_bytes` | 内部存储可用空间（字节） | `StatFs.getAvailableBlocksLong() * getBlockSizeLong()` |
| `internal_storage_used_bytes` | 内部存储已使用空间（字节） | `totalSize - availableSize` |
| `internal_storage_usage_percent` | 内部存储使用率 | `(usedSize / totalSize) * 100` |
| `external_storage_total_bytes` | 外部存储总空间（字节） | `StatFs.getBlockCountLong() * getBlockSizeLong()` |
| `external_storage_available_bytes` | 外部存储可用空间（字节） | `StatFs.getAvailableBlocksLong() * getBlockSizeLong()` |
| `external_storage_used_bytes` | 外部存储已使用空间（字节） | `totalSize - availableSize` |
| `external_storage_usage_percent` | 外部存储使用率 | `(usedSize / totalSize) * 100` |
| `external_storage_state` | 外部存储状态 | `Environment.getExternalStorageState()` |

**Hidden API 调用**：
```java
// 获取 MemoryInfo 隐藏字段
Field hiddenAppThresholdField = ActivityManager.MemoryInfo.class.getDeclaredField("hiddenAppThreshold");
hiddenAppThresholdField.setAccessible(true);
long hiddenAppThreshold = hiddenAppThresholdField.getLong(memoryInfo);

// 获取特定 UID 的内存信息
Method getMemoryInfoForUidMethod = ActivityManager.class.getDeclaredMethod("getMemoryInfo", int.class, ActivityManager.MemoryInfo.class);
getMemoryInfoForUidMethod.setAccessible(true);
getMemoryInfoForUidMethod.invoke(activityManager, uid, memoryInfo);
```

## 📋 Native 层指纹采集字段详情

### 1. 系统属性信息 (SystemPropertyCollector)

通过 `__system_property_get()` 获取系统属性，与 Java 层类似但直接从 Native 层读取：

#### USB 配置相关
- `sys.usb.config`
- `sys.usb.state`
- `persist.sys.usb.config`
- `persist.sys.usb.qmmi.func`
- `vendor.usb.mimode`
- `persist.vendor.usb.config`

#### 安全相关
- `ro.debuggable`
- `init.svc.adbd`
- `ro.secure`
- `ro.boot.flash.locked`
- `sys.oem_unlock_allowed`

#### Build ID 相关
- `ro.build.id`
- `ro.build.build.id`
- `ro.bootimage.build.id`
- `ro.odm.build.id`
- `ro.product.build.id`
- `ro.system_ext.build.id`
- `ro.system.build.id`
- `ro.vendor.build.id`

#### SDK 版本
- `ro.build.version.sdk`

#### 安全补丁
- `ro.build.version.security_patch`

#### 其他系统信息
- `ro.boot.vbmeta.digest`
- `ro.netflix.bsp_rev`
- `gsm.version.baseband`

#### Build 日期 UTC
- `ro.build.date.utc`
- `ro.build.build.date.utc`
- `ro.bootimage.build.date.utc`
- `ro.odm.build.date.utc`
- `ro.product.build.date.utc`
- `ro.system_ext.build.date.utc`
- `ro.system.build.date.utc`
- `ro.vendor.build.date.utc`

#### Display ID 和 Tags
- `ro.build.display.id`
- `ro.build.tags`
- `ro.build.build.tags`
- `ro.bootimage.build.tags`
- `ro.odm.build.tags`
- `ro.product.build.tags`
- `ro.system_ext.build.tags`
- `ro.system.build.tags`
- `ro.vendor.build.tags`

#### Build Host 和 User
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

#### 其他系统属性
- `ro.board.platform`
- `ro.product.cpu.abi`
- `ro.boot.verifiedbootstate`
- `ro.boot.vbmeta.device_state`
- `ro.treble.enabled`

**Native API 调用**：
```cpp
#include <sys/system_properties.h>

char value[PROP_VALUE_MAX] = {0};
int len = __system_property_get("ro.build.id", value);
```

### 2. DRM 信息 (DRMCollector)

| 字段名 | 说明 | Native API 调用 |
|--------|------|----------------|
| `MediaDrm Device Unique ID (Base64)` | DRM 设备唯一 ID (Base64) | `AMediaDrm_getPropertyByteArray(mediaDrm, PROPERTY_DEVICE_UNIQUE_ID, &byteArray)` |
| `MediaDrm Device Unique ID (Hex)` | DRM 设备唯一 ID (十六进制) | 同上，转换为十六进制 |
| `Length` | ID 长度（字节） | `byteArray.length` |

**Native API 调用**：
```cpp
#include <media/NdkMediaDrm.h>

AMediaDrm* mediaDrm = AMediaDrm_createByUUID(WIDEVINE_UUID);
AMediaDrmByteArray byteArray;
AMediaDrm_getPropertyByteArray(mediaDrm, PROPERTY_DEVICE_UNIQUE_ID, &byteArray);
```

### 3. 网络接口信息 (MacAddressCollector)

| 字段名 | 说明 | Native API 调用 |
|--------|------|----------------|
| `Interface Name` | 网络接口名称 | `getifaddrs()` / `ioctl(SIOCGIFHWADDR)` |
| `MAC Address` | MAC 地址 | `getifaddrs()` (AF_PACKET) / `ioctl(SIOCGIFHWADDR)` |
| `IPv4 Address` | IPv4 地址 | `getifaddrs()` (AF_INET) + `getnameinfo()` |
| `IPv6 Address` | IPv6 地址 | `getifaddrs()` (AF_INET6) + `getnameinfo()` |

**Native API 调用**：
```cpp
#include <sys/socket.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <netpacket/packet.h>

// 方法1: 使用 getifaddrs (netlink)
struct ifaddrs* ifap;
getifaddrs(&ifap);
// 遍历获取 MAC 地址和 IP 地址

// 方法2: 使用 ioctl (备选)
struct ifreq ifr;
ioctl(sockfd, SIOCGIFHWADDR, &ifr);
```

### 4. 文件指纹信息 (NativeFileCollector)

#### 核心硬件与内核特征文件

| 文件路径 | 说明 |
|----------|------|
| `/proc/cpuinfo` | CPU 信息 |
| `/proc/version` | 内核版本信息 |
| `/proc/meminfo` | 内存信息 |
| `/proc/iomem` | I/O 内存映射 |
| `/proc/misc` | 杂项设备 |
| `/sys/devices/soc0/family` | SoC 系列 |
| `/sys/devices/soc0/machine` | SoC 机器型号 |
| `/sys/class/power_supply/battery/capacity` | 电池容量 |
| `/sys/class/power_supply/battery/status` | 电池状态 |

#### 环境与安全检测文件

| 文件路径 | 说明 |
|----------|------|
| `/proc/sys/kernel/random/boot_id` | 引导 ID |
| `/proc/sys/kernel/osrelease` | 操作系统版本 |
| `/sys/fs/selinux/enforce` | SELinux 强制模式 |
| `/proc/sys/kernel/random/entropy_avail` | 可用熵 |
| `/proc/uptime` | 系统运行时间 |
| `/sys/class/thermal/thermal_zone0/temp` | CPU 温度 |
| `/proc/sys/vm/overcommit_memory` | 内存过度提交设置 |

#### 挂载点与输入设备文件

| 文件路径 | 说明 |
|----------|------|
| `/proc/self/mountinfo` | 挂载信息 |
| `/proc/mounts` | 挂载点 |
| `/proc/filesystems` | 文件系统类型 |
| `/proc/bus/input/devices` | 输入设备 |
| `/proc/net/unix` | Unix 域套接字 |

#### 内核信息（通过 uname）

| 字段名 | 说明 | Native API 调用 |
|--------|------|----------------|
| `System Name` | 系统名称 | `uname(&info)` -> `info.sysname` |
| `Node Name` | 节点名称 | `uname(&info)` -> `info.nodename` |
| `Release` | 内核版本 | `uname(&info)` -> `info.release` |
| `Version` | 内核版本详细信息 | `uname(&info)` -> `info.version` |
| `Machine` | 机器架构 | `uname(&info)` -> `info.machine` |
| `Domain Name` | 域名 | `uname(&info)` -> `info.domainname` (GNU) |

**Native API 调用**：
```cpp
#include <sys/utsname.h>

struct utsname info;
uname(&info);
```

#### 系统配置信息（通过 sysconf）

| 字段名 | 说明 | Native API 调用 |
|--------|------|----------------|
| `CPU Cores (Online)` | 在线 CPU 核心数 | `sysconf(_SC_NPROCESSORS_ONLN)` |
| `CPU Cores (Configured)` | 配置的 CPU 核心数 | `sysconf(_SC_NPROCESSORS_CONF)` |
| `Page Size` | 页大小（字节） | `sysconf(_SC_PAGESIZE)` |
| `Clock Ticks per Second` | 每秒时钟滴答数 | `sysconf(_SC_CLK_TCK)` |
| `Physical Pages` | 物理页数 | `sysconf(_SC_PHYS_PAGES)` |
| `Total Physical Memory` | 总物理内存（MB） | `phys_pages * page_size / 1024 / 1024` |
| `Available Physical Pages` | 可用物理页数 | `sysconf(_SC_AVPHYS_PAGES)` |
| `Available Physical Memory` | 可用物理内存（MB） | `avphys_pages * page_size / 1024 / 1024` |

**Native API 调用**：
```cpp
#include <unistd.h>

long nproc = sysconf(_SC_NPROCESSORS_ONLN);
long page_size = sysconf(_SC_PAGESIZE);
long phys_pages = sysconf(_SC_PHYS_PAGES);
```

#### Zygisk 注入检测

通过读取 `/proc/self/maps` 检测可疑库注入：

| 字段名 | 说明 |
|--------|------|
| `Total Mappings` | 总映射数 |
| `Library Mappings` | 库映射数 |
| `Suspicious Libraries Found` | 检测到的可疑库数量 |
| `Suspicious Libraries` | 可疑库列表 |

**检测的可疑关键词**：
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

## 🔓 Hidden API 调用详情

### 1. Hidden API 解除限制

项目使用 `FreeReflection` 库解除 Android Hidden API 限制：

```java
// FuckHideApi.java
public class FuckHideApi extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Reflection.unseal(base);  // 解除 Hidden API 限制
    }
}
```

**依赖**：`com.github.tiann:FreeReflection:3.2.0`

### 2. TelephonyManager Hidden API

| 方法名 | 参数 | 说明 | 调用方式 |
|--------|------|------|----------|
| `getMeid()` | 无 | 获取 MEID | `TelephonyManager.class.getDeclaredMethod("getMeid")` |
| `getNai()` | 无 | 获取 NAI | `TelephonyManager.class.getDeclaredMethod("getNai")` |
| `getDataNetworkType()` | 无 | 获取数据网络类型 | `TelephonyManager.class.getDeclaredMethod("getDataNetworkType")` |
| `getPhoneCount()` | 无 | 获取电话数量 | `TelephonyManager.class.getDeclaredMethod("getPhoneCount")` |
| `getActiveModemCount()` | 无 | 获取活动调制解调器数量 | `TelephonyManager.class.getDeclaredMethod("getActiveModemCount")` (Android 11+) |
| `getImei(int slotIndex)` | `int slotIndex` | 获取指定卡槽的 IMEI | `TelephonyManager.class.getDeclaredMethod("getImei", int.class)` |
| `getDeviceId(int slotIndex)` | `int slotIndex` | 获取指定卡槽的设备 ID | `TelephonyManager.class.getDeclaredMethod("getDeviceId", int.class)` |
| `getMeid(int slotIndex)` | `int slotIndex` | 获取指定卡槽的 MEID | `TelephonyManager.class.getDeclaredMethod("getMeid", int.class)` |
| `getSubscriberId(int subId)` | `int subId` | 获取指定订阅 ID 的 IMSI | `TelephonyManager.class.getDeclaredMethod("getSubscriberId", int.class)` |
| `getCarrierConfig()` | 无 | 获取运营商配置 | `TelephonyManager.class.getDeclaredMethod("getCarrierConfig")` |

### 3. SystemProperties Hidden API

| 方法名 | 参数 | 说明 | 调用方式 |
|--------|------|------|----------|
| `get(String key, String defaultValue)` | `String key, String defaultValue` | 获取字符串属性 | `SystemProperties.class.getMethod("get", String.class, String.class)` |
| `getLong(String key, long defaultValue)` | `String key, long defaultValue` | 获取长整型属性 | `SystemProperties.class.getMethod("getLong", String.class, long.class)` |
| `getInt(String key, int defaultValue)` | `String key, int defaultValue` | 获取整型属性 | `SystemProperties.class.getMethod("getInt", String.class, int.class)` |
| `getBoolean(String key, boolean defaultValue)` | `String key, boolean defaultValue` | 获取布尔型属性 | `SystemProperties.class.getMethod("getBoolean", String.class, boolean.class)` |

### 4. ActivityManager.MemoryInfo Hidden 字段

| 字段名 | 类型 | 说明 | 访问方式 |
|--------|------|------|----------|
| `hiddenAppThreshold` | `long` | 隐藏应用阈值 | `MemoryInfo.class.getDeclaredField("hiddenAppThreshold")` |
| `secondaryServerThreshold` | `long` | 二级服务器阈值 | `MemoryInfo.class.getDeclaredField("secondaryServerThreshold")` |

### 5. ActivityManager Hidden 方法

| 方法名 | 参数 | 说明 | 调用方式 |
|--------|------|------|----------|
| `getMemoryInfo(int uid, MemoryInfo outInfo)` | `int uid, MemoryInfo outInfo` | 获取指定 UID 的内存信息 | `ActivityManager.class.getDeclaredMethod("getMemoryInfo", int.class, ActivityManager.MemoryInfo.class)` |

### 6. ContentResolver Hidden API

| 方法名 | 参数 | 说明 | 调用方式 |
|--------|------|------|----------|
| `call(Uri uri, String method, String arg, Bundle extras)` | `Uri uri, String method, String arg, Bundle extras` | 调用 ContentProvider 方法 | `ContentResolver.call(Uri.parse("content://settings/secure"), "GET_secure", "android_id", Bundle)` |

## 🚀 使用方法

### 基本使用

```java
// 初始化服务
FingerprintService fingerprintService = new FingerprintService(context);

// 采集 Java 层指纹
FingerprintResult javaResult = fingerprintService.collectJavaFingerprint();
String javaOutput = javaResult.toString();

// 采集 Native 层指纹
FingerprintResult nativeResult = fingerprintService.collectNativeFingerprint();
String nativeOutput = nativeResult.toNativeString();

// 获取清洗后的 Java 层指纹（JSON 格式）
JSONObject cleanedJavaData = fingerprintService.collectAndCleanJavaFingerprint();

// 获取清洗后的完整指纹（包括 Java 和 Native 层）
JSONObject cleanedAllData = fingerprintService.collectAndCleanAllFingerprint();

// 获取 MAC 地址（Native 层）
String macAddress = fingerprintService.getMacAddress();
```

### 单独使用某个采集器

```java
// 只采集 Settings 信息
SettingsCollector settingsCollector = new SettingsCollector(context);
String settings = settingsCollector.collectSettings();
String androidId = settingsCollector.getAndroidId();

// 只采集电话信息
PhoneInfoCollector phoneInfoCollector = new PhoneInfoCollector(context);
String phoneInfo = phoneInfoCollector.getPhoneInfo();
```

## 📦 依赖项

### Gradle 依赖

```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'com.github.tiann:FreeReflection:3.2.0'  // Hidden API 解除限制
}
```

### Native 依赖

- Android NDK
- CMake 3.22.1+
- C++ 标准库
- Android Media NDK (用于 DRM)

## ⚙️ 构建要求

- **编译 SDK**：35
- **最小 SDK**：33
- **目标 SDK**：35
- **Java 版本**：11
- **NDK 版本**：最新稳定版
- **CMake 版本**：3.22.1

## 📝 权限要求

部分功能需要以下权限（在 `AndroidManifest.xml` 中声明）：

```xml
<!-- 电话信息（Android 8.0+） -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />

<!-- 账户信息（Android 6.0+） -->
<uses-permission android:name="android.permission.GET_ACCOUNTS" />

<!-- 蓝牙信息 -->
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
```

**注意**：Android 10+ 对某些敏感信息（如 IMEI）的访问有严格限制，普通应用可能无法获取。

## 🔒 安全注意事项

1. **Hidden API 使用**：项目使用反射机制调用 Hidden API，可能在不同 Android 版本上表现不一致
2. **权限限制**：Android 10+ 对设备标识符访问有严格限制，部分信息可能无法获取
3. **隐私保护**：采集的设备指纹信息可能包含敏感数据，请妥善处理
4. **Root 检测**：项目包含 Root 和 Zygisk 注入检测功能，可用于安全分析

## 📄 许可证

本项目仅供学习和研究使用。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request。

## 📧 联系方式

如有问题或建议，请通过 Issue 反馈。

---

**注意**：本项目仅用于合法的安全研究和设备识别目的，请勿用于非法用途。

