package com.zj.push;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.List;

public class CameraHelper implements SurfaceHolder.Callback, Camera.PreviewCallback {

    private static String TAG = "Camera";

    private final Activity activity;
    private int cameraId;
    private int width;
    private int height;
    private Camera camera;
    private int rotation;
    private byte[] buffer;
    private SurfaceHolder surfaceHolder;
    private Camera.PreviewCallback previewCallback;

    public CameraHelper(Activity activity, int cameraId, int width, int height) {
        this.activity = activity;
        this.cameraId = cameraId;
        this.width = width;
        this.height = height;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (previewCallback != null) {
            previewCallback.onPreviewFrame(data, camera);
        }
        camera.addCallbackBuffer(buffer);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        stopPreview();
        startPreview();
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        stopPreview();
    }

    public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
        surfaceHolder.addCallback(this);
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        this.previewCallback = previewCallback;
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        stopPreview();
        startPreview();
    }

    private void startPreview() {
        try {
            //获取camera
            camera = Camera.open(cameraId);
            //配置camera属性
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);

            //设置摄像头宽高
            setPreviewSize(parameters);
            //设置角度
            setPreviewOrientation(parameters);
            camera.setParameters(parameters);

            //数据缓存区
            buffer = new byte[width * height * 3 / 2];
            camera.addCallbackBuffer(buffer);

            //设置预览画面
            camera.setPreviewDisplay(surfaceHolder);
            //开启预览
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopPreview() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    /**
     * 设置预览宽高
     */
    private void setPreviewSize(Camera.Parameters parameters) {
        //获取摄像头支持的宽、高
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        Camera.Size size = supportedPictureSizes.get(0);
        Log.e(TAG, String.format("摄像头支持的分辨率，x:%d，y:%d", size.width, size.height));

        int m = Math.abs(size.height * size.width - width * height);
        supportedPictureSizes.remove(0);
        for (Camera.Size next : supportedPictureSizes) {
            Log.e(TAG, String.format("摄像头支持的分辨率，x:%d，y:%d", next.width, next.height));
            int n = Math.abs(next.height * next.width - width * height);
            if (n < m) {
                m = n;
                size = next;
            }
        }
        width = size.width;
        height = size.height;
        parameters.setPreviewSize(width, height);
        Log.e(TAG, String.format("预览分辨率，x:%d，y:%d", width, height));
    }

    private void setPreviewOrientation(Camera.Parameters parameters) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}
