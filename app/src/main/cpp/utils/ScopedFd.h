/*
 * Simple ScopedFd implementation for RAII file descriptor management
 */

#pragma once

#include <unistd.h>

class ScopedFd {
public:
    explicit ScopedFd(int fd = -1) : fd_(fd) {}
    
    ~ScopedFd() {
        if (fd_ != -1) {
            close(fd_);
        }
    }
    
    // Move constructor
    ScopedFd(ScopedFd&& other) noexcept : fd_(other.fd_) {
        other.fd_ = -1;
    }
    
    // Move assignment
    ScopedFd& operator=(ScopedFd&& other) noexcept {
        if (this != &other) {
            if (fd_ != -1) {
                close(fd_);
            }
            fd_ = other.fd_;
            other.fd_ = -1;
        }
        return *this;
    }
    
    // Delete copy constructor and assignment
    ScopedFd(const ScopedFd&) = delete;
    ScopedFd& operator=(const ScopedFd&) = delete;
    
    int get() const { return fd_; }
    
    void reset(int fd = -1) {
        if (fd_ != -1) {
            close(fd_);
        }
        fd_ = fd;
    }
    
    int release() {
        int fd = fd_;
        fd_ = -1;
        return fd;
    }
    
private:
    int fd_;
};

