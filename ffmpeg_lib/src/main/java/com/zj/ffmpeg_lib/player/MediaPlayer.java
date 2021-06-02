package com.zj.ffmpeg_lib.player;

import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

public class MediaPlayer implements LifecycleObserver, SurfaceHolder.Callback {

    static {
        System.loadLibrary("ffmpeg-lib");
    }

    private static String TAG = "MediaPlayer";

    //region 错误代码
    private static final int FFMPEG_CAN_NOT_OPEN_URL = 1;     // 打不开视频
    private static final int FFMPEG_CAN_NOT_FIND_STREAMS = 2;     // 找不到流媒体
    private static final int FFMPEG_FIND_DECODER_FAIL = 3;    // 找不到解码器
    private static final int FFMPEG_ALLOC_CODEC_CONTEXT_FAIL = 4;    // 无法根据解码器创建上下文
    private static final int FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL = 6;    //  根据流信息 配置上下文参数失败
    private static final int FFMPEG_OPEN_DECODER_FAIL = 7;    // 打开解码器失败
    private static final int FFMPEG_NOMEDIA = 8;    // 没有音视频
    //endregion

    private String path;
    private OnPreparedListener mOnPreparedListener;
    private OnErrorListener mOnErrorListener;
    private OnProgressListener mOnProgressListener;
    private SurfaceHolder surfaceHolder;

    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {
        this.mOnPreparedListener = onPreparedListener;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.mOnErrorListener = onErrorListener;
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.mOnProgressListener = onProgressListener;
    }

    public void setSurfaceHolder(SurfaceView surfaceView) {
        if (surfaceHolder != null) {
            surfaceHolder.removeCallback(this);
        }

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
    }

    public void setDataSource(String path) {
        this.path = path;
    }

    public void prepare() {
        nativePrepare(path);
    }

    public int getDuration() {
        return getDurationNative();
    }

    public void start() {
        nativeStart();
    }

    public void stop() {
        nativeStop();
    }

    public void seek(int progress) {
        nativeSeek(progress);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void release() {
        nativeRelease();
        mOnErrorListener = null;
        mOnPreparedListener = null;
    }

    /**
     * 给jni调用，通知准备成功
     */
    public void onPrepare() {
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared();
        }
    }

    public void onError(int code, String msg) {
        Log.e(TAG, String.format("code: %d, msg: %s", code, msg));
        if (null != this.mOnErrorListener) {
            switch (code) {
                case FFMPEG_CAN_NOT_OPEN_URL:
                    msg += ",打不开视频";
                    break;
                case FFMPEG_CAN_NOT_FIND_STREAMS:
                    msg += ",找不到流媒体";
                    break;
                case FFMPEG_FIND_DECODER_FAIL:
                    msg += ",找不到解码器";
                    break;
                case FFMPEG_ALLOC_CODEC_CONTEXT_FAIL:
                    msg += ",无法根据解码器创建上下文";
                    break;
                case FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL:
                    msg += ",根据流信息 配置上下文参数失败";
                    break;
                case FFMPEG_OPEN_DECODER_FAIL:
                    msg += ",打开解码器失败";
                    break;
                case FFMPEG_NOMEDIA:
                    msg += ",没有音视频";
                    break;
                default:
                    break;
            }
            mOnErrorListener.onError(code, msg);
        }
    }

    public void onProgress(int progress) {
        if (mOnProgressListener != null)
            mOnProgressListener.onProgress(progress);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.e("JNI", "surfaceChanged");
        setSurfaceNative(holder.getSurface());
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
    }

    public interface OnPreparedListener {
        void onPrepared();
    }

    public interface OnErrorListener {
        void onError(int code, String errorMsg);
    }

    public interface OnProgressListener {
        void onProgress(int progress);
    }

    //region native方法

    private native void nativePrepare(String path);

    private native void nativeStart();

    private native void nativeStop();

    private native void nativeRelease();

    private native void setSurfaceNative(Surface surface);

    private native int getDurationNative();

    private native void nativeSeek(int progress);
    //endregion

}
