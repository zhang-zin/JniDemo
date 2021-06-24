package com.zj.push;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("native-lib");
    }

    private Pusher pusher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, 0);
        pusher = new Pusher(this, Camera.CameraInfo.CAMERA_FACING_BACK, 640, 480, 25, 800_000);
        pusher.setPreviewDisplay(surfaceView.getHolder());
    }

    public void switchCamera(View view) {
        pusher.switchCamera();
    }

    public void startLive(View view) {
        pusher.startLive("rtmp://115.159.144.229/zhang");
    }

    public void stopLive(View view) {
        pusher.stopLive();
    }

}