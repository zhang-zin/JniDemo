package com.zj.opencv

import android.view.Surface

/**
 * 人脸追踪器
 * [model] 人脸定位模型
 */
class FaceTracker(private val model: String) {

    /**
     * c++ 对象的指针
     */
    private var nativeObject: Long

    init {
        System.loadLibrary("native-lib")
        nativeObject = nativeCreateObject(model)
    }

    /**
     * 设置显示的画布
     */
    fun setSurface(surface: Surface?) {
        nativeSetSurface(nativeObject, surface)
    }

    /**
     * 开启人脸跟踪
     */
    fun start() {
        nativeStart(nativeObject)
    }

    fun stop() {
        nativeStop(nativeObject)
    }

    /**
     * 释放
     */
    fun release() {
        nativeRelease(nativeObject)
        nativeObject = 0
    }

    /**
     * 人脸定位
     * [inputImage]:       摄像头采集到的数据
     * [width]:            采集数据的宽
     * [height]:           采集数据的高
     * [rotationDegrees]:  摄像头旋转角度
     */
    fun detect(inputImage: ByteArray, width: Int, height: Int, rotationDegrees: Int, mirror: Boolean) {
        nativeDetect(nativeObject, inputImage, width, height, rotationDegrees,mirror)
    }

    private external fun nativeCreateObject(model: String): Long
    private external fun nativeSetSurface(nativeObject: Long, surface: Surface?)
    private external fun nativeStart(nativeObject: Long)
    private external fun nativeStop(nativeObject: Long)
    private external fun nativeRelease(nativeObject: Long)
    private external fun nativeDetect(nativeObject: Long, inputImage: ByteArray, width: Int, height: Int, rotationDegrees: Int, mirror: Boolean)

}