package com.zj.jnidemo

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.abs

class Camera1Helper(private val activity: ComponentActivity) : SurfaceHolder.Callback, LifecycleObserver {
    var mFontCameraId: Int = 0
    var mFontCameraInfo: Camera.CameraInfo? = null

    var mBackCameraId: Int = 0
    var mBackCameraInfo: Camera.CameraInfo? = null

    var curCameraId = 0

    var camera: Camera? = null

    var holder: SurfaceHolder? = null

    var preViewWidth = 0
    var preViewHeight = 0


    init {
        activity.lifecycle.addObserver(this)
    }

    /**
     * 设置前置摄像头信息
     */
    fun setFont(fontCameraId: Int, fontCameraInfo: Camera.CameraInfo) {
        mFontCameraId = fontCameraId
        mFontCameraInfo = fontCameraInfo
    }

    fun setBack(backCameraId: Int, backCameraInfo: Camera.CameraInfo) {
        mBackCameraId = backCameraId
        mBackCameraInfo = backCameraInfo
    }

    fun setSurfaceHolder(holder: SurfaceHolder?) {
        this.holder = holder
        holder?.addCallback(this)
        curCameraId = mBackCameraId
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.e("zhang", "surfaceCreated")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        preViewWidth = width
        preViewHeight = height
        startPreView(width, height)
        Log.e("zhang", "surfaceChanged")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.e("zhang", "surfaceDestroyed")
        stopPreView()
    }

    private fun startPreView(width: Int, height: Int) {
        camera = Camera.open(curCameraId)
        initPreviewParams(width, height)
        camera?.run {
            setPreviewDisplay(holder)
            startPreview()
        }
    }

    private fun stopPreView() {
        camera?.run {
            this.stopPreview()
            release()
        }
    }

    /**
     * 配置camera参数
     * [shortSize] SurfaceHolder的宽
     * [longSize]  SurfaceHolder的高
     */
    private fun initPreviewParams(shortSize: Int, longSize: Int) {
        camera?.run {
            // 摄像头支持的尺寸
            val parameters = parameters
            val size = parameters.supportedPreviewSizes
            val bestSize: Camera.Size = getBestSize(shortSize, longSize, size)
            // 设置预览大小
            parameters.setPreviewSize(bestSize.width, bestSize.height)
            //设置角度
            adjustCameraOrientation(curCameraId)
            // 设置拍照大小
            parameters.setPictureSize(bestSize.width, bestSize.height)
            // 设置格式，NV21
            parameters.previewFormat = ImageFormat.NV21
            // 设置聚焦
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            setParameters(parameters)
        }
    }

    /**
     * 获取相机要设置的预览大小
     * 相机的width > height
     */
    private fun getBestSize(shortSize: Int, longSize: Int, sizes: List<Camera.Size>): Camera.Size {
        val uiRatio = longSize * 1.0f / shortSize
        var minRatio = uiRatio
        var bestSize: Camera.Size? = null
        Log.e("zhang", "预览画面分辨率，shortSize: " + shortSize + " longSize: " + longSize)
        for (previewSize in sizes) {
            Log.e("zhang", "摄像头支持的分辨率，cameraWidth: " + previewSize.width + " cameraHeight: " + previewSize.height)
            val cameraRatio = previewSize.width * 1.0f / previewSize.height

            //如果找不到比例相同的，找一个最近的,防止预览变形
            val offset = abs(cameraRatio - uiRatio)
            if (offset < minRatio) {
                minRatio = offset
                bestSize = previewSize
            }

            //比例相同
            if (uiRatio == cameraRatio) {
                bestSize = previewSize
                break
            }
        }
        return bestSize!!
    }

    /**
     * 调整预览的方向
     */
    private fun adjustCameraOrientation(curCameraId: Int) {
        val info = CameraInfo()
        Camera.getCameraInfo(curCameraId, info)
        //判断当前横竖屏
        val rotation = activity.windowManager.defaultDisplay.rotation
        var degrees = 0
        //获取手机方向
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }
        val result = if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
            //后置摄像头
            (info.orientation - degrees + 360) % 360
        } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //前置摄像头
            val r = (info.orientation + degrees) % 360
            //镜像
            (360 - r) % 360
        } else {
            0
        }
        camera?.setDisplayOrientation(result)
    }

    /**
     * 切换摄像头
     */
    fun switchCamera() {
        curCameraId = if (curCameraId == mBackCameraId) {
            mFontCameraId
        } else {
            mBackCameraId
        }
        stopPreView()
        startPreView(preViewWidth, preViewHeight)
    }

    /**
     * 拍照
     */
    fun picture(context: Activity) {
        camera?.run {
            /**
             * ShutterCallback shutter : 拍照瞬间调用，如果空回调，则由声音，传 null ，则没效果
             * PictureCallback raw :     图片的原始数据，即没处理过的
             * PictureCallback jpeg :    图片的 JPEG 数据
             */
            takePicture(
                    { },
                    null
            ) { data, camera ->
                HiExecutor.execute(runnable = object : HiExecutor.Callable<File?>() {

                    lateinit var imageFile: File

                    override fun onPrepare() {
                        super.onPrepare()
                        val parentPath = context.getExternalFilesDir("")?.absolutePath
                                ?: Environment.getExternalStorageDirectory().absolutePath
                        val parent = File("$parentPath/image")
                        if (!parent.exists()) {
                            parent.mkdirs()
                        }
                        imageFile = File(parent, "text.jpg")
                    }

                    override fun onBackground(): File? {
                        var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                                ?: return null
                        var fos: FileOutputStream? = null
                        try {
                            fos = FileOutputStream(imageFile)
                            //保存前先调整方向
                            val info = if (curCameraId == mFontCameraId) mFontCameraInfo else mBackCameraInfo!!
                            bitmap = if (info?.facing == CameraInfo.CAMERA_FACING_BACK) {
                                rotate(bitmap, 90f)
                            } else {
                                rotate(bitmap, 270f)
                            }
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        } finally {
                            fos?.close()
                            bitmap.recycle()
                        }
                        return imageFile
                    }

                    override fun onCompleted(t: File?) {
                        if (t != null) {
                            Toast.makeText(context, "图片保存成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "图片保存失败", Toast.LENGTH_SHORT).show();
                        }

                    }
                })
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        stopPreView()
        camera = null
    }

    fun rotate(bitmap: Bitmap, degress: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degress)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true);
    }

}