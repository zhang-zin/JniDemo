package com.zj.jnidemo

import android.Manifest
import android.hardware.Camera
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.SurfaceView
import android.view.View

/**
 * Camera1相机的开启与预览
 */
class Camera1Activity : AppCompatActivity() {

    lateinit var camera1Helper: Camera1Helper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera1)
        requestPermissions(arrayOf(Manifest.permission.CAMERA), 100)
        val surfaceView = findViewById<SurfaceView>(R.id.surfaceView)
        camera1Helper = Camera1Helper(this)
        camera1Helper.setSurfaceHolder(surfaceView.holder)
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
                    camera1Helper.setFont(index, info)
                } else if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    //后置摄像头
                    camera1Helper.setBack(index, info)
                }
            }
        }
    }

    fun switchCamera(view: View) {
        camera1Helper.switchCamera()
    }

    fun picture(view: View) {
        camera1Helper.picture(this)
    }
}