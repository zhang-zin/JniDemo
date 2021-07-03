package com.zj.jnidemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.util.Log
import android.util.Size
import android.view.TextureView
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import kotlin.math.abs

/**
 * camera2使用：
 * 1.context.getSystemService(Context.CAMERA_SERVICE)获取CameraManager
 * 2.getCameraCharacteristics()拿到相机的所有信息，比如支持的预览大小，level 等
 * 3.CameraManager#openCamera()，从回调中拿到CameraDevice，他表示当前相机设备
 * 4.CameraDevice 通过 createCaptureRequest 创建 CaptureRequest.Builder ，用来配置相机属性，通过 createCaptureSession 创建 CameraCaptureSession ，
 *   它是 Pipeline 的实例，然后交给底层处理
 */
class Camera2Helper(private val activity: ComponentActivity, private val textureView: TextureView) {

    //获取相机服务
    private val cameraManager: CameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var curCameraId = ""
    private var frontCameraId = ""
    private var frontCameraCharacteristics: CameraCharacteristics? = null
    private var backCameraId = ""
    private var backCameraCharacteristics: CameraCharacteristics? = null

    private var onLayoutChangeListener: View.OnLayoutChangeListener? = null

    init {
        //遍历设备支持的相机
        val cameraIdList = cameraManager.cameraIdList
        for (cameraId in cameraIdList) {
            //拿到相机的所有信息
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            //是否有闪光灯 FLASH_INFO_AVAILABLE
            //是否有 AE 模式 CONTROL_AE_AVAILABLE_MODES
            //拿到相机的方向，前置、后置、外置
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing != null) {
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    //前置摄像头
                    frontCameraId = cameraId
                    frontCameraCharacteristics = characteristics
                    curCameraId = frontCameraId
                } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    //后置摄像头
                    backCameraId = cameraId
                    backCameraCharacteristics = characteristics
                }
            }
            //是否支持 Camera2 的高级特性
            val level = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
            //不支持 Camera2 的特性
            if (level == null || level == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                Toast.makeText(activity, "手机不支持Camera2的高级特效", Toast.LENGTH_SHORT).show();
                continue
            }
        }
        onLayoutChangeListener = View.OnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            openCamera(right - left, bottom - top)
            textureView.removeOnLayoutChangeListener(onLayoutChangeListener)
        }
        textureView.addOnLayoutChangeListener(onLayoutChangeListener)
    }

    /**
     * 打开摄像头
     */
    fun openCamera(width: Int, height: Int) {
        if (curCameraId.isEmpty()) return
        val characteristics = when (curCameraId) {
            frontCameraId -> frontCameraCharacteristics
            backCameraId -> backCameraCharacteristics
            else -> return
        } ?: return
        //拿到配置map
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: return
        //拿到传感器的方向
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
        //拿到预览尺寸
        val previewSize = map.getOutputSizes(SurfaceTexture::class.java) ?: return
        val bestSize = getBestSize(width, height, previewSize)
        /**
         * 配置预览属性
         * 与 Cmaera1 不同的是，Camera2 是把尺寸信息给到 Surface (SurfaceView 或者 ImageReader)，
         * Camera2 会根据 Surface 配置的大小，输出对应尺寸的画面;
         * 注意摄像头的 width > height ，而我们使用竖屏，所以宽高要变化一下
         */
        textureView.surfaceTexture?.setDefaultBufferSize(bestSize.height, bestSize.width)

        // 设置图片尺寸，图片设置最大的
        val sizes = map.getOutputSizes(ImageFormat.JPEG)
        val largest = sizes.maxByOrNull {
            it.width * it.height
        } ?: sizes[0]
        //ImageReader，配置大小，且最大Image为 1，因为是 JPEG
        val imageReader = ImageReader.newInstance(largest.width, largest.width, ImageFormat.JPEG, 1)
        //设置拍照监听
        imageReader.setOnImageAvailableListener({
            Log.e("zhang", "OnImageAvailableListener")
        }, null)

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            /**
             * 打开摄像头，监听数据
             * cameraId: camera的ID，前置、后置、外置
             * CameraDevice.StateCallback: 当连接到相机时，该回调就会被回调，生成CameraDevice
             * handler: 调用 CameraDevice.StateCallback 的 Handler，传null，则调用主线程，
             */
            cameraManager.openCamera(curCameraId, CameraDeviceCallback(textureView,imageReader), null)
        }
    }

    private fun getBestSize(width: Int, height: Int, previewSize: Array<Size>): Size {
        val uiRatio = width * 1.0f / height
        var minRatio = uiRatio
        var bestSize: Size? = null
        Log.e("zhang", "预览画面分辨率，width: $width width: $width")
        for (size in previewSize) {
            Log.e("zhang", "摄像头支持的分辨率，cameraWidth: " + size.width + " cameraHeight: " + size.height)
            val cameraRatio = size.height * 1.0f / size.width

            //如果找不到比例相同的，找一个最近的,防止预览变形
            val offset = abs(cameraRatio - uiRatio)
            if (offset < minRatio) {
                minRatio = offset
                bestSize = size
            }

            //比例相同
            if (uiRatio == cameraRatio) {
                bestSize = size
                break
            }
        }
        return bestSize!!
    }

    /**
     *
     */
    fun switchCamera() {

    }
}