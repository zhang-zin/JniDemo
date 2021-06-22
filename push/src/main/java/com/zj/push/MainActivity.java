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

    private Pusher pusher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SurfaceView surfaceView = findViewById(R.id.surfaceView);
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