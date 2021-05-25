package com.zj.ffmpeg_lib;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.zj.ffmpeg_lib.player.MediaPlayer;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private final MediaPlayer player = new MediaPlayer();
    SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceView);

        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        getLifecycle().addObserver(player);
        String path = new File(Environment.getExternalStorageDirectory() + File.separator + "demo.mp4").getAbsolutePath();
        player.setDataSource(path);
        player.setSurfaceHolder(surfaceView);
        player.prepare();
        player.setOnPreparedListener(() -> {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "准备成功", Toast.LENGTH_SHORT).show());
            player.start();
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    protected void onStop() {
        super.onStop();
//        player.stop();
    }
}