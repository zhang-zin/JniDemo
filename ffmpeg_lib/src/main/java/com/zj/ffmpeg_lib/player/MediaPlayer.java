package com.zj.ffmpeg_lib.player;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

public class MediaPlayer implements LifecycleObserver {

    static {
        System.loadLibrary("ffmpeg-lib");
    }

    private String path;
    private OnPreparedListener mOnPreparedListener;

    public void setDataSource(String path) {
        this.path = path;
    }

    public void setOnPreparedListener(OnPreparedListener mOnPreparedListener) {
        this.mOnPreparedListener = mOnPreparedListener;
    }

    public void prepare() {
        nativePrepare(path);
    }

    public void start() {
        nativeStart();
    }

    public void stop() {
        nativeStop();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void release() {
        nativeRelease();
    }

    /**
     * 给jni调用，通知准备成功
     */
    public void onPrepare() {
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared();
        }
    }

    public void onError(String msg, int code) {

    }

    public interface OnPreparedListener {
        void onPrepared();
    }

    private native void nativePrepare(String path);

    private native void nativeStart();

    private native void nativeStop();

    private native void nativeRelease();

}
