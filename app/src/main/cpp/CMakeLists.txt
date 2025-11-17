# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)
set(CMAKE_CXX_STANDARD 14)

# 添加内联支持
enable_language(C ASM)

# 开始优化
SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O2")
# inline
SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility-inlines-hidden")
# 添加警告选项
SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra")

# 设置包含目录
include_directories(
    ./
    ./utils/
    ./netlink/
    ./collectors/
    ./core/
    ./private/
    ./async_safe/
    ./cutils/
)

# 声明项目名称
project("sphunter")

# 收集源文件
set(NATIVE_SOURCES
    # 主入口
    native-lib.cpp
    
    # 核心模块
    core/FingerprintCollector.cpp
    core/JNIRegistry.cpp
    
    # 收集器模块
    collectors/SystemPropertyCollector.cpp
    collectors/DRMCollector.cpp
    collectors/MacAddressCollector.cpp
    collectors/NativeFileCollector.cpp
    
    # 工具模块
    utils/Base64Utils.cpp
    
    # Netlink模块
    netlink/ifaddrs.cpp
    netlink/bionic_netlink.cpp
)

# 创建共享库
add_library(${CMAKE_PROJECT_NAME} SHARED ${NATIVE_SOURCES})

# 根据架构添加汇编文件
if (${CMAKE_ANDROID_ARCH_ABI} STREQUAL "arm64-v8a")
    target_sources(${CMAKE_PROJECT_NAME} PRIVATE netlink/syscall64.s)
elseif (${CMAKE_ANDROID_ARCH_ABI} STREQUAL "armeabi-v7a")
    target_sources(${CMAKE_PROJECT_NAME} PRIVATE netlink/syscall32.s)
endif()

# 链接系统库
target_link_libraries(${CMAKE_PROJECT_NAME}
    # Android系统库
    android
    log
    mediandk
)

# 打印构建信息
message(STATUS "Building for architecture: ${CMAKE_ANDROID_ARCH_ABI}")
message(STATUS "C++ Standard: ${CMAKE_CXX_STANDARD}")
message(STATUS "Optimization flags: ${CMAKE_CXX_FLAGS}")
