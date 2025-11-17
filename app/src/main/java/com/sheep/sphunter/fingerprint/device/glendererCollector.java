package com.sheep.sphunter.fingerprint.device;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;

public class glendererCollector {
    
    /**
     * 获取 OpenGL 渲染器信息 (GL_RENDERER)
     * 通过创建离屏渲染上下文来获取显卡信息
     * @return 格式: "Renderer: xxx | Vendor: xxx | Version: xxx"
     */
    public String getGlendererInfo() {
        EGLDisplay eglDisplay = null;
        EGLContext eglContext = null;
        EGLSurface eglSurface = null;
        
        try {
            // 1. 获取 Display (默认屏幕句柄)
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                return "Error: Unable to get EGL display";
            }
            
            // 2. 初始化 EGL (激活连接)
            int[] version = new int[2];
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                return "Error: Unable to initialize EGL";
            }
            
            // 3. 选择配置 (Config)
            int[] configAttribs = {
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,  // 使用 ES 2.0
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
            };
            
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            if (!EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)) {
                return "Error: Unable to choose EGL config";
            }
            
            if (numConfigs[0] == 0) {
                return "Error: No EGL configs found";
            }
            
            // 4. 创建上下文 (Context)
            int[] contextAttribs = {
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,  // OpenGL ES 2.0
                EGL14.EGL_NONE
            };
            
            eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, contextAttribs, 0);
            if (eglContext == EGL14.EGL_NO_CONTEXT) {
                return "Error: Unable to create EGL context";
            }
            
            // 5. 创建离屏画布 (PBuffer Surface) - 1x1 像素，用户看不见
            int[] surfaceAttribs = {
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE
            };
            
            eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttribs, 0);
            if (eglSurface == EGL14.EGL_NO_SURFACE) {
                return "Error: Unable to create EGL surface";
            }
            
            // 6. 绑定环境到当前线程
            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                return "Error: Unable to make EGL context current";
            }
            
            // 7. 读取 OpenGL 信息
            String renderer = GLES20.glGetString(GLES20.GL_RENDERER);    // 显卡型号 (重点)
            String vendor = GLES20.glGetString(GLES20.GL_VENDOR);        // 显卡厂商
            String version2 = GLES20.glGetString(GLES20.GL_VERSION);      // OpenGL 版本
            
            // 格式化返回结果
            StringBuilder result = new StringBuilder();
            result.append("Renderer: ").append(renderer != null ? renderer : "Unknown");
            result.append(" | Vendor: ").append(vendor != null ? vendor : "Unknown");
            result.append(" | Version: ").append(version2 != null ? version2 : "Unknown");
            
            return result.toString();
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            // 8. 清理资源 (防止内存泄漏)
            if (eglDisplay != null) {
                // 解绑当前上下文
                EGL14.eglMakeCurrent(eglDisplay, 
                    EGL14.EGL_NO_SURFACE, 
                    EGL14.EGL_NO_SURFACE, 
                    EGL14.EGL_NO_CONTEXT);
                
                // 销毁 Surface
                if (eglSurface != null) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface);
                }
                
                // 销毁 Context
                if (eglContext != null) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext);
                }
                
                // 终止 EGL
                EGL14.eglTerminate(eglDisplay);
            }
        }
    }
    
    /**
     * 仅获取渲染器名称 (简化版)
     * @return 显卡型号字符串
     */
    public String getRendererOnly() {
        String fullInfo = getGlendererInfo();
        if (fullInfo.startsWith("Renderer: ")) {
            // 提取 Renderer 部分
            int endIndex = fullInfo.indexOf(" | Vendor:");
            if (endIndex > 0) {
                return fullInfo.substring(10, endIndex);
            }
        }
        return fullInfo;
    }
}