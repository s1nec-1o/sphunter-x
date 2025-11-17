/*
 * Simple ErrnoRestorer implementation for NDK compatibility
 * This is a minimal implementation that doesn't actually restore errno
 * since the code doesn't seem to use it.
 */

#pragma once

#include <errno.h>

// Simple RAII class to save and restore errno
class ErrnoRestorer {
public:
    ErrnoRestorer() : saved_errno_(errno) {}
    ~ErrnoRestorer() { errno = saved_errno_; }
    
private:
    int saved_errno_;
};

