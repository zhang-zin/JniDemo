package com.zj.opengl;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
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
    private byte[] cameraBuffer_; // 数据 操作旋转
    private SurfaceHolder surfaceHolder;
    private Camera.PreviewCallback previewCallback;
    private OnChangedSizeListener onChangedSizeListener;
    private SurfaceTexture surfaceTexture;

    public CameraHelper(Activity activity, int cameraId, int width, int height) {
        this.activity = activity;
        this.cameraId = cameraId;
        this.width = width;
        this.height = height;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (rotation == Surface.ROTATION_0) {
            rotation90(data);
        }
        if (previewCallback != null) {
            previewCallback.onPreviewFrame(cameraBuffer_, camera);
        }
        camera.addCallbackBuffer(buffer);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        stopPreview();
        startPreview(surfaceTexture);
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
        startPreview(surfaceTexture);
    }

    public void startPreview(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
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
            cameraBuffer_ = new byte[width * height * 3 / 2];
            camera.addCallbackBuffer(buffer);
            camera.setPreviewCallbackWithBuffer(this);
            //设置预览画面
            //camera.setPreviewDisplay(surfaceHolder);
            //surfaceTexture纹理画布配合OpenGl渲染 OpenGL无法访问到相机的预览数据只能访问surfaceTexture
            camera.setPreviewTexture(surfaceTexture);
            //开启预览
            camera.startPreview();
            if (onChangedSizeListener != null) {
                onChangedSizeListener.onChanged(width, height);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopPreview() {
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
            default:
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            // compensate the mirror
            result = (360 - result) % 360;
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public void setOnChangedSizeListener(OnChangedSizeListener listener) {
        this.onChangedSizeListener = listener;
    }

    private void rotation90(byte[] data) {
        int index = 0;
        int ySize = width * height;
        // u和v
        int uvHeight = height / 2;
        // 后置摄像头顺时针旋转90度
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            //将y的数据旋转之后 放入新的byte数组
            for (int i = 0; i < width; i++) {
                for (int j = height - 1; j >= 0; j--) {
                    cameraBuffer_[index++] = data[width * j + i];
                }
            }

            // 每次处理两个数据
            for (int i = 0; i < width; i += 2) {
                for (int j = uvHeight - 1; j >= 0; j--) {
                    // v
                    cameraBuffer_[index++] = data[ySize + width * j + i];
                    // u
                    cameraBuffer_[index++] = data[ySize + width * j + i + 1];
                }
            }
        } else {
            // 逆时针旋转90度
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    cameraBuffer_[index++] = data[width * j + width - 1 - i];
                }
            }
            //  u v
            for (int i = 0; i < width; i += 2) {
                for (int j = 0; j < uvHeight; j++) {
                    cameraBuffer_[index++] = data[ySize + width * j + width - 1 - i - 1];
                    cameraBuffer_[index++] = data[ySize + width * j + width - 1 - i];
                }
            }
        }
    }

    public interface OnChangedSizeListener {
        /**
         * 摄像头采集的宽高发生变化
         *
         * @param width  当前的宽
         * @param height 当前的高
         */
        void onChanged(int width, int height);
    }
}
