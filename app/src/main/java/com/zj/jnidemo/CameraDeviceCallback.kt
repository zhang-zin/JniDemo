package com.zj.jnidemo

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.util.Log
import android.view.Surface
import android.view.TextureView

/**
 * CameraDevice表示当前的相机设备：
 * 1.根据指定参数创建CameraCaptureSession
 * 2.根据指定的模板创建 CaptureRequest
 * 3.关闭相机设备
 * 4.监听相机状态，比如断开，开启成功失败的监听
 */
class CameraDeviceCallback(private val textureView: TextureView, private val imageReader: ImageReader) : CameraDevice.StateCallback() {

    private var mCamera: CameraDevice? = null
    private var surface: Surface? = null

    init {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                surface = Surface(surfaceTexture)

            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }
        }
    }

    override fun onOpened(camera: CameraDevice) {
        mCamera = camera
        //摄像头已经打开，可以预览
        createPreviewPipeline(mCamera, surface)
    }

    override fun onDisconnected(camera: CameraDevice) {
        camera.close()
        surface?.release()
    }

    override fun onError(camera: CameraDevice, error: Int) {
        camera.close()
    }

    private fun createPreviewPipeline(camera: CameraDevice?, surface: Surface?) {
        if (camera == null) return
        if (surface == null) return
        //创建作为预览的CaptureRequest.builder
        //TEMPLATE_PREVIEW：适用于配置预览的模板
        //TEMPLATE_RECORD：适用于视频录制的模板。
        //TEMPLATE_STILL_CAPTURE：适用于拍照的模板。
        //TEMPLATE_VIDEO_SNAPSHOT：适用于在录制视频过程中支持拍照的模板。
        //TEMPLATE_MANUAL：适用于希望自己手动配置大部分参数的模板。
        val captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        //添加surface容器
        captureBuilder.addTarget(surface)
        // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求,这个必须在创建 Seesion 之前就准备好，
        camera.createCaptureSession(listOf(this.surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                //设置自动聚焦
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                //设置自动曝光
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                //创建 CaptureRequest
                val build = captureBuilder.build()
                //设置预览时连续捕获图片数据
                Log.e("zhang", "onConfigured")
                session.setRepeatingRequest(build, null, null);
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("zhang", "配置失败")
            }

        }, null)
    }

}