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

    private VideoChannel videoChannel;
    private AudioChannel audioChannel;

    public Pusher(FragmentActivity activity, int cameraId, int width, int height, int fps, int bitrate) {
        activity.getLifecycle().addObserver(this);
        native_init();
        videoChannel = new VideoChannel(this, activity, cameraId, width, height, fps, bitrate);
    }


    public void setPreviewDisplay(SurfaceHolder holder) {
        videoChannel.setPreviewDisplay(holder);
    }

    public void switchCamera() {
        videoChannel.switchCamera();
    }

    public void startLive(String path) {
        Log.e("JNI", "开启直播");
        native_start(path);
        videoChannel.startLive();
    }

    public void stopLive() {
        native_stop();
        videoChannel.stopLive();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void release() {
        videoChannel.release();
        native_release();
    }

    //region native 方法

    public native void native_init();

    public native void native_start(String path);

    public native void native_stop();

    public native void native_release();

    public native void native_initVideoEncoder(int width, int height, int fps, int bitrate);

    public native void native_pushVideo(byte[] data);
    //endregion

}
