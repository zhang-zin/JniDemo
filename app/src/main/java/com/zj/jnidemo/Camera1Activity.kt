package com.zj.jnidemo

import android.Manifest
import android.hardware.Camera
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View

/**
 * Camera1相机的开启与预览
 */
class Camera1Activity : AppCompatActivity(), SurfaceHolder.Callback {

    var mFontCameraId: Int = 0
    var mFontCameraInfo: Camera.CameraInfo? = null

    var mBackCameraId: Int = 0
    var mBackCameraInfo: Camera.CameraInfo? = null

    var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1)
        requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
        val surfaceView = findViewById<SurfaceView>(R.id.surfaceView)
        surfaceView.holder.addCallback(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val numberOfCameras = Camera.getNumberOfCameras()
            for (index in 0 until numberOfCameras) {
                val info = Camera.CameraInfo()
                //获取摄像头信息
                Camera.getCameraInfo(index, info)
                if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    //前置摄像头
                    mFontCameraId = index
                    mFontCameraInfo = info
                } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    //后置摄像头
                    mBackCameraId = index
                    mBackCameraInfo = info
                    camera =  Camera.open(mBackCameraId)
                }
            }
        }
    }

    fun switchCamera(view: View) {}

    override fun surfaceCreated(holder: SurfaceHolder) {
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }
}