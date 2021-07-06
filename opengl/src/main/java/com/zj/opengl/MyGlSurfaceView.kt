package com.zj.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.SurfaceHolder

/**
 * 自定义控件显示Camera预览画面
 */
class MyGlSurfaceView(context: Context?, attrs: AttributeSet?) : GLSurfaceView(context, attrs) {

    private val glRenderer: MyGlRenderer

    private var mSpeed = Speed.MODE_NORMAL

    enum class Speed {
        MODE_EXTRA_SLOW, MODE_SLOW, MODE_NORMAL, MODE_FAST, MODE_EXTRA_FAST
    }

    init {
        //设置EGL版本 2.0
        setEGLContextClientVersion(2)
        //设置渲染器
        //EGL开启一个GLThread.start 同步这个线程回调渲染
        glRenderer = MyGlRenderer(this)
        setRenderer(glRenderer)
        //设置渲染模式
        //RENDERMODE_WHEN_DIRTY 按需渲染，有帧数据才会去渲染，后面需要手动调用
        //RENDERMODE_CONTINUOUSLY 每隔16毫秒，读取更新一次，
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    /**
     * 设置录制视频速度
     * [mode] 极慢 慢 标准 快 极快
     */
    fun setSpeed(mode: Speed) {
        mSpeed = mode
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)
        glRenderer.surfaceDestroyed()
    }

    /**
     * 开始录制
     */
    fun startRecording() {
        val speed = when (mSpeed) {
            Speed.MODE_EXTRA_SLOW -> 0.3F
            Speed.MODE_SLOW -> 0.5F
            Speed.MODE_NORMAL -> 1.0F
            Speed.MODE_FAST -> 1.5F
            Speed.MODE_EXTRA_FAST -> 2F
        }
        glRenderer.startRecording(speed)
    }

    /**
     * 停止录制
     */
    fun stopRecording() {
        glRenderer.stopRecording()
    }

}