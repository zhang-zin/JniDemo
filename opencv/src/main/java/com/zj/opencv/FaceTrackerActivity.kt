package com.zj.opencv

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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

/**
 * 采集摄像头，使用opencv人脸识别
 */
class FaceTrackerActivity : AppCompatActivity(), SurfaceHolder.Callback, ImageAnalysis.Analyzer {

    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var surfaceView: SurfaceView
    lateinit var faceTracker: FaceTracker

    private var cameraProvider: ProcessCameraProvider? = null
    private var curCameraSelector: CameraSelector? = null
    private var imageAnalysis: ImageAnalysis? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_tracker)

        val model = Utils.copyAssest2Dir(this, "lbpcascade_frontalface.xml")
        faceTracker = FaceTracker(model)
        faceTracker.start()

        surfaceView = findViewById(R.id.surfaceView)
        surfaceView.holder.addCallback(this)
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
        this.cameraProvider = cameraProvider
        cameraProvider?.run {
            //STRATEGY_KEEP_ONLY_LATEST :非阻塞模式，每次获得最新帧
            //STRATEGY_BLOCK_PRODUCER : 阻塞模式，处理不及时会导致降低帧率
            imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(surfaceView.width, surfaceView.height))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
            imageAnalysis!!.setAnalyzer(
                    ContextCompat.getMainExecutor(this@FaceTrackerActivity),
                    this@FaceTrackerActivity
            )

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                    this@FaceTrackerActivity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageAnalysis
            )
            curCameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }

    }

    fun switchCamera(view: View) {
        curCameraSelector = if (curCameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        cameraProvider?.run {
            unbindAll()
            bindToLifecycle(this@FaceTrackerActivity, curCameraSelector!!, imageAnalysis)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        faceTracker.setSurface(holder.surface)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        faceTracker.setSurface(null)
    }

    override fun analyze(image: ImageProxy) {
        Log.e("zhang", "analyze")
        val dataFormImage = Utils.getDataFormImage(image)
        faceTracker.detect(dataFormImage, image.width, image.height, image.imageInfo.rotationDegrees, curCameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA)
        image.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        faceTracker.release()
    }
}