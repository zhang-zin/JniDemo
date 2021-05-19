package com.zj.ffmpeg_lib;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;

import com.zj.ffmpeg_lib.player.MediaPlayer;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private final MediaPlayer player = new MediaPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getLifecycle().addObserver(player);
        String path = new File(Environment.getExternalStorageDirectory() + File.separator + "demo.mp4").getAbsolutePath();
        player.setDataSource(path);
        player.prepare();
        player.setOnPreparedListener(() -> {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "准备成功", Toast.LENGTH_SHORT).show());
            player.start();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
//        player.stop();
    }
}