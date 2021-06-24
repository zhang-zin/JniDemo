package com.zj.push;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioChannel {

    private final Pusher pusher;
    private AudioRecord audioRecord;
    private boolean isLive;
    private int channels = 2;
    private final int inputSamples;

    public AudioChannel(Pusher pusher) {
        this.pusher = pusher;
        int channelConfig;
        if (channels == 2) {
            // 双声道
            channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        } else {
            // 单声道
            channelConfig = AudioFormat.CHANNEL_IN_MONO;
        }
        // 初始化faac音频编码器
        this.pusher.native_initAudioEncoder(44100, channels);

        // 单通道输出样本数2048
        inputSamples = this.pusher.getInputSamples() * 2;
        Log.e("JNI", "faac输出样本数：" + inputSamples);
        int minBufferSize = AudioRecord.getMinBufferSize(44100, channelConfig, AudioFormat.ENCODING_PCM_16BIT) * 2;

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                44100, channelConfig,
                AudioFormat.ENCODING_PCM_16BIT,
                Math.max(inputSamples, minBufferSize));
    }


    public void startLive() {
        isLive = true;
        HiExecutor.INSTANCE.execute(new AudioTask());
    }

    public void stopLive() {
        isLive = false;
    }

    public void release() {
        if (audioRecord != null) {
            audioRecord.release();
            audioRecord = null;
        }
    }

    private class AudioTask implements Runnable {
        @Override
        public void run() {
            audioRecord.startRecording();
            byte[] bytes = new byte[inputSamples];
            while (isLive) {
                int len = audioRecord.read(bytes, 0, bytes.length);
                if (len > 0) {
                    pusher.native_pushAudio(bytes);
                }
            }
            audioRecord.stop();
        }
    }
}
