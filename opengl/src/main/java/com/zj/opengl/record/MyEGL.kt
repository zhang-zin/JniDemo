package com.zj.opengl.record

import android.content.Context
import android.opengl.*
import android.opengl.EGL14.*
import android.view.Surface
import com.zj.opengl.filter.ScreenFilter

/**
 * 管理EGL环境工具类
 */
class MyEGL(
    private val eglContext: EGLContext,
    surface: Surface,
    context: Context,
    width: Int,
    height: Int
) {

    private lateinit var mEGLDisplay: EGLDisplay
    private lateinit var mEGLConfig: EGLConfig
    private lateinit var mEGLContext: EGLContext
    private var mEGLSurface: EGLSurface
    private var mScreenFilter: ScreenFilter

    init {
        // 1、创建EGL环境
        createEGL()
        // 2、创建窗口，绘制线程中的图像
        val attributeList = intArrayOf(EGL_NONE)
        // 关联EGL和surface
        mEGLSurface = eglCreateWindowSurface(
            mEGLDisplay,   // EGL显示链接
            mEGLConfig,    // EGL最终选择配置的成果
            surface,       // MediaCodec的输入surface画布
            attributeList, //
            0
        )
        // 3、让画布盖住屏幕（让 mEGLDisplay(EGL显示链接) 和 mEGLSurface(EGL的独有画布) 发生绑定关系）
        check(eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            "eglMakeCurrent fail"
        }
        mScreenFilter = ScreenFilter(context)
        mScreenFilter.onReady(width, height)
    }

    /**
     * 创建EGL
     */
    private fun createEGL() {
        // 1、获取EGL显示设备 EGL_DEFAULT_DISPLAY：默认设备
        mEGLDisplay = eglGetDisplay(EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        // 2、初始化设备
        check(eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            "eglInitialize fail"
        }
        // 3、选择配置
        val attributeList = intArrayOf(
            // 像素格式 rgba
            EGL_RED_SIZE, 8,     // value 颜色深度都设置为八位
            EGL_GREEN_SIZE, 8,   // value 颜色深度都设置为八位
            EGL_BLUE_SIZE, 8,    // value 颜色深度都设置为八位
            EGL_ALPHA_SIZE, 8,   // value 颜色深度都设置为八位
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, // EGL 2.0版本号
            EGLExt.EGL_RECORDABLE_ANDROID, 1,  // 以Android兼容方式才创建
            EGL_NONE // 结尾符
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfig = IntArray(1)
        check(
            eglChooseConfig(
                mEGLDisplay,     // EGL显示链接
                attributeList,   // 属性列表
                0,  // attributeList 从数组第零个下标开始找
                configs,         // 输出的配置选项成果
                0,   // configs 从数组第零个下标开始找
                configs.size,    // 配置的数量，只有一个
                numConfig,       // 需要的配置int数组，他需要什么就给他什么
                0 // numConfig 从数组第零个下标开始找
            )
        ) {
            "eglChooseConfig fail"
        }
        mEGLConfig = configs[0]!! // 最终EGL选择配置的成果

        // 4、创建上下文
        val ctxAttributeList = intArrayOf(
            EGL_CONTEXT_CLIENT_VERSION, 2,  //EGL 上下文客户端版本 2.0
            EGL_NONE
        )
        mEGLContext = eglCreateContext(mEGLDisplay, mEGLConfig, eglContext, ctxAttributeList, 0)
        if (mEGLContext == EGL_NO_CONTEXT) {
            throw RuntimeException("eglCreateContext fail")
        }
    }

    /**
     * 绘制
     * [texture]   纹理ID
     * [timestamp] 时间戳
     */
    fun draw(texture: Int, timestamp: Long) {
        // 在虚拟屏幕上渲染
        mScreenFilter.onDrawFrame(texture, FloatArray(1))
        // 刷新时间戳（如果设置不合理，编码时会采取丢帧或降低视频质量方式进行编码）
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, timestamp)
        // 交换缓冲区数据
        eglSwapBuffers(mEGLDisplay, mEGLSurface) // 绘制操作
    }

    /**
     * 释放资源
     */
    fun release() {
        eglMakeCurrent(mEGLDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT)
        eglDestroySurface(mEGLDisplay, mEGLSurface)
        eglDestroyContext(mEGLDisplay, eglContext)
        eglReleaseThread()
        eglTerminate(mEGLDisplay)
    }
}