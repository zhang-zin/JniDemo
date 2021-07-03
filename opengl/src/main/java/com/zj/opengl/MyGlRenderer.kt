package com.zj.opengl

import android.app.Activity
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGlRenderer(private val glSurfaceView: MyGlSurfaceView) : GLSurfaceView.Renderer,
    SurfaceTexture.OnFrameAvailableListener {

    // 纹理id
    private val textureId = IntArray(1)
    private var surfaceTexture: SurfaceTexture? = null
    private var screenFilter: ScreenFilter? = null
    var mtx = FloatArray(16) // 矩阵数据，变换矩阵


    /**
     * Surface创建时
     * [gl] OpenGL 1.0
     * [config] 配置项
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

        glGenTextures(textureId.size, textureId, 0)
        //实例化纹理对象
        surfaceTexture = SurfaceTexture(textureId[0])
        surfaceTexture!!.setOnFrameAvailableListener(this)
        screenFilter = ScreenFilter(glSurfaceView.context)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        val cameraHelper = CameraHelper(
            glSurfaceView.context as Activity,
            Camera.CameraInfo.CAMERA_FACING_FRONT,
            800,
            480
        )
        cameraHelper.startPreview(surfaceTexture)
        screenFilter?.onReady(width, height)
    }

    /**
     * 绘制一帧图像
     */
    override fun onDrawFrame(gl: GL10?) {
        //每次绘制时，清空之前的
        glClearColor(255f, 0f, 0f, 0f)
        // https://blog.csdn.net/z136411501/article/details/83273874
        // GL_COLOR_BUFFER_BIT 颜色缓冲区
        // GL_DEPTH_BUFFER_BIT 深度缓冲区
        // GL_STENCIL_BUFFER_BIT 模型缓冲区
        glClear(GL_COLOR_BUFFER_BIT)

        surfaceTexture?.run {
            //绘制摄像头数据
            updateTexImage()
            getTransformMatrix(mtx)
            screenFilter?.onDrawFrame(textureId[0], mtx)
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        glSurfaceView.requestRender()
    }
}