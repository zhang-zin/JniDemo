package com.zj.ffmpeg_lib;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("ffmpeg-lib");
    }

    public native static String getFFmpegVersion();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView ffmpegVer = findViewById(R.id.ffmpeg_ver);
        ffmpegVer.setText("当前 FFmpeg 版本为:" + getFFmpegVersion());
    }
}