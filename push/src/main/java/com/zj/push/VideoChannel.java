package com.zj.push;

import android.app.Activity;
import android.hardware.Camera;
import android.view.SurfaceHolder;

import androidx.fragment.app.FragmentActivity;

/**
 * 视频推流
 *
 * @author 张锦
 */
public class VideoChannel implements Camera.PreviewCallback, CameraHelper.OnChangedSizeListener {

    private final Pusher pusher;
    private final int fps;
    private final int bitrate;
    private final CameraHelper cameraHelper;
    private boolean isLive;

    public VideoChannel(Pusher pusher, Activity activity, int cameraId, int width, int height, int fps, int bitrate) {
        this.pusher = pusher;
        this.fps = fps;
        this.bitrate = bitrate;
        cameraHelper = new CameraHelper(activity, cameraId, width, height);
        cameraHelper.setPreviewCallback(this);
        cameraHelper.setOnChangedSizeListener(this);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (isLive) {
            // 图像数据推送
            pusher.native_pushVideo();
        }
    }

    @Override
    public void onChanged(int width, int height) {
        //初始化x264编码器
        pusher.native_initVideoEncoder(width, height, fps, bitrate);
    }

    /**
     * 摄像头与SurfaceView绑定
     */
    public void setPreviewDisplay(SurfaceHolder holder) {
        cameraHelper.setPreviewDisplay(holder);
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        cameraHelper.switchCamera();
    }

    public void startLive() {
        isLive = true;
    }

    public void stopLive() {
        isLive = false;
    }

    public void release() {
        cameraHelper.stopPreview();
    }

}
