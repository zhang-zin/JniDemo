package com.zj.opencv

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture

class FaceTrackerActivity : AppCompatActivity(), SurfaceHolder.Callback, ImageAnalysis.Analyzer {

    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var surfaceView: SurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_tracker)
        surfaceView = findViewById(R.id.surfaceView)
        requestPermissions(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            100
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            initCamera()
        }
    }

    private fun initCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            kotlin.runCatching {
                val cameraProvider = cameraProviderFuture.get()
                bindAnalysis(cameraProvider)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindAnalysis(cameraProvider: ProcessCameraProvider?) {
        cameraProvider?.run {
            //STRATEGY_KEEP_ONLY_LATEST :非阻塞模式，每次获得最新帧
            //STRATEGY_BLOCK_PRODUCER : 阻塞模式，处理不及时会导致降低帧率
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(surfaceView.width, surfaceView.height))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(
                ContextCompat.getMainExecutor(this@FaceTrackerActivity),
                this@FaceTrackerActivity
            )

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this@FaceTrackerActivity,
                CameraSelector.DEFAULT_BACK_CAMERA,
                imageAnalysis
            )
            // TODO: 2021/6/30 切换摄像头
            /*cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this@FaceTrackerActivity,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                imageAnalysis
            )*/
        }

    }

    fun switchCamera(view: View) {}

    override fun surfaceCreated(holder: SurfaceHolder) {
        TODO("Not yet implemented")
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        TODO("Not yet implemented")
    }

    override fun analyze(image: ImageProxy) {
        TODO("Not yet implemented")
    }
}