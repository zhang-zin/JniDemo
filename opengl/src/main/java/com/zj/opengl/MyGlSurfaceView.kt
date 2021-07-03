package com.zj.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log

/**
 * 自定义控件显示Camera预览画面
 */
class MyGlSurfaceView(context: Context?, attrs: AttributeSet?) : GLSurfaceView(context, attrs) {

    init {
        //设置EGL版本 2.0
        setEGLContextClientVersion(2)
        //设置渲染器
        //EGL开启一个GLThread.start 同步这个线程回调渲染
        setRenderer(MyGlRenderer(this))
        //设置渲染模式
        //RENDERMODE_WHEN_DIRTY 按需渲染，有帧数据才会去渲染，后面需要手动调用
        //RENDERMODE_CONTINUOUSLY 每隔16毫秒，读取更新一次，
        renderMode = RENDERMODE_WHEN_DIRTY
    }

}