package com.zj.ffmpeg_lib;

import android.Manifest;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.zj.ffmpeg_lib.player.MediaPlayer;

import java.io.File;

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private final MediaPlayer player = new MediaPlayer();
    SurfaceView surfaceView;
    TextView tvTime;
    SeekBar seekBar;

    private int duration;
    private boolean isTouch; // 用户是否拖拽了 拖动条，（默认是没有拖动false）

    private static String TAG = "player";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceView);

        tvTime = findViewById(R.id.tv_time);
        seekBar = findViewById(R.id.seekBar);

        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        getLifecycle().addObserver(player);
        String path = new File(Environment.getExternalStorageDirectory() + File.separator + "demo.mp4").getAbsolutePath();
//        player.setDataSource(path);
        player.setDataSource("rtmp://58.200.131.2:1935/livetv/hunantv");

        player.setSurfaceHolder(surfaceView);
        player.prepare();
        player.setOnPreparedListener(() -> {
            Log.e(TAG, "OnPrepared ");
            duration = player.getDuration();
            runOnUiThread(() -> {
                if (duration > 0) {
                    tvTime.setVisibility(View.VISIBLE);
                    seekBar.setVisibility(View.VISIBLE);
                    tvTime.setText(String.format("00:00/%s:%s", getMinutes(duration), getSeconds(duration)));
                }
                Toast.makeText(MainActivity.this, "准备成功", Toast.LENGTH_SHORT).show();
            });
            player.start();
        });

        player.setOnProgressListener(progress -> {
            if (!isTouch) {
                runOnUiThread(() -> {
                    tvTime.setText(String.format("%s:%s/%s:%s", getMinutes(progress), getSeconds(progress), getMinutes(duration), getSeconds(duration)));
                    int curProgress = (int) (progress * 1.0f / duration * 100);
                    seekBar.setProgress(curProgress);
                });
            }
        });

        player.setOnErrorListener((code, errorMsg) -> Log.e(TAG, "errorMsg: " + errorMsg));

        seekBar.setOnSeekBarChangeListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onStop() {
        super.onStop();
        player.stop();
    }

    private String getMinutes(int duration) {
        int minutes = duration / 60;
        if (minutes <= 9) {
            return "0" + minutes;
        }
        return "" + minutes;
    }

    private String getSeconds(int duration) {
        int seconds = duration % 60;
        if (seconds <= 9) {
            return "0" + seconds;
        }
        return "" + seconds;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            int userProgress = progress / 100 * duration;
            tvTime.setText(String.format("%s:%s/%s:%s", getMinutes(userProgress), getSeconds(userProgress), getMinutes(duration), getSeconds(duration)));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        isTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        isTouch = false;

        int userProgress = (int) (seekBar.getProgress() * 1.0f / 100 * duration);
        player.seek(userProgress);
    }
}