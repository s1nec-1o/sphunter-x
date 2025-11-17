/*
 * Compatibility macros and functions for NDK
 */

#pragma once

#include <unistd.h>
#include <string.h>
#include <errno.h>

// TEMP_FAILURE_RETRY macro for retrying system calls that may be interrupted
#ifndef TEMP_FAILURE_RETRY
#define TEMP_FAILURE_RETRY(expression) \
  ({ \
    long int __result; \
    do \
      __result = (long int) (expression); \
    while (__result == -1L && errno == EINTR); \
    __result; \
  })
#endif

// Note: strlcpy is already available in NDK's string.h, so we don't need to define it

