package com.zj.push;

import androidx.appcompat.app.AppCompatActivity;

import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }

    CameraHelper cameraHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String jni = stringFromJNI();
        Log.e("zhang", jni);

        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        cameraHelper = new CameraHelper(this, Camera.CameraInfo.CAMERA_FACING_BACK, 640, 480);
        cameraHelper.setPreviewDisplay(surfaceView.getHolder());
    }

    public native String stringFromJNI();

    public void stopLive(View view) {
    }

    public void startLive(View view) {
    }

    public void switchCamera(View view) {
        cameraHelper.switchCamera();
    }
}