/*
 * Simple async_safe/log.h implementation for NDK compatibility
 */

#pragma once

#include <android/log.h>
#include <string.h>
#include <stdarg.h>

#ifdef __cplusplus
extern "C" {
#endif

// Simple async_safe_format_log implementation
static inline void async_safe_format_log(int priority, const char* tag, const char* format, ...) {
    va_list args;
    va_start(args, format);
    __android_log_vprint(priority, tag, format, args);
    va_end(args);
}

#ifdef __cplusplus
}
#endif

