package com.zj.push;

import android.util.Log;
import android.view.SurfaceHolder;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * 推流
 *
 * @author 张锦
 */
public class Pusher implements LifecycleObserver {

    private final VideoChannel videoChannel;
    private AudioChannel audioChannel;

    public Pusher(FragmentActivity activity, int cameraId, int width, int height, int fps, int bitrate) {
        activity.getLifecycle().addObserver(this);
        native_init();
        videoChannel = new VideoChannel(this, activity, cameraId, width, height, fps, bitrate);
        audioChannel = new AudioChannel(this);
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        videoChannel.setPreviewDisplay(holder);
    }

    public void switchCamera() {
        videoChannel.switchCamera();
    }

    public void startLive(String path) {
        Log.e("JNI", "开启直播");
        videoChannel.startLive();
        audioChannel.startLive();
        native_start(path);
    }

    public void stopLive() {
        videoChannel.stopLive();
        audioChannel.stopLive();
        native_stop();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void release() {
        videoChannel.release();
        audioChannel.release();
        native_release();
    }

    //region native 方法

    public native void native_init();

    public native void native_start(String path);

    public native void native_stop();

    public native void native_release();

    public native void native_initVideoEncoder(int width, int height, int fps, int bitrate);

    public native void native_pushVideo(byte[] data);

    public native void native_initAudioEncoder(int sampleRate, int channels);

    public native int getInputSamples();

    public native void native_pushAudio(byte[] bytes);
    //endregion

}
