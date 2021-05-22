package com.zj.ffmpeg_lib.player;

import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

public class MediaPlayer implements LifecycleObserver {

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

    public void setOnPreparedListener(OnPreparedListener onPreparedListener) {
        this.mOnPreparedListener = mOnPreparedListener;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        this.mOnErrorListener = onErrorListener;
    }

    public void setDataSource(String path) {
        this.path = path;
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
            }
            mOnErrorListener.onError(code, msg);
        }
    }

    public interface OnPreparedListener {
        void onPrepared();
    }

    public interface OnErrorListener {
        void onError(int code, String errorMsg);
    }

    //region native方法

    private native void nativePrepare(String path);

    private native void nativeStart();

    private native void nativeStop();

    private native void nativeRelease();
    //endregion

}
