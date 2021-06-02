#include <jni.h>
#include <string>
#include <iostream>
#include <android/native_window_jni.h>
#include "JNICallback.h"
#include "MediaPlayer.h"

extern "C" {
#include <libavutil/avutil.h>
}

JavaVM *vm = nullptr;
MediaPlayer *mediaPlayer = nullptr;
ANativeWindow *window = nullptr;
pthread_mutex_t mutex_t = PTHREAD_MUTEX_INITIALIZER; // 静态初始化

/**
 * 视频渲染回调
 * @param src_data RGBA视频数据
 * @param width 视频分辨率的宽
 * @param height 视频分辨率的高
 * @param src_lineSize 视频
 * @return
 */
void renderFrame(uint8_t *src_data, int width, int height, int src_lineSize) {
    pthread_mutex_lock(&mutex_t);

    if (!window) {
        //window为空
        LOGE("window为空");
        pthread_mutex_unlock(&mutex_t);
        return;
    }

    // 设置窗口的大小、各个属性
    ANativeWindow_setBuffersGeometry(window, width, height, WINDOW_FORMAT_RGBA_8888);

    ANativeWindow_Buffer windowBuffer;

    if (ANativeWindow_lock(window, &windowBuffer, nullptr)) {
        // 窗口被锁住无法渲染，需要先释放，防止出现死锁
        ANativeWindow_release(window);
        window = nullptr;
        pthread_mutex_unlock(&mutex_t);
        return;
    }

    // 填充WindowBuffer，画面就出来了
    auto *dst_data = static_cast<uint8_t *>(windowBuffer.bits);
    int dst_line_size = windowBuffer.stride * 4;

    for (int i = 0; i < windowBuffer.height; ++i) { //一行一行的显示
        /**
         * ANativeWindow_Buffer64字节对齐的算法
         * 视频分辨率：426*240
         * 视频宽：426
         * 426 * 4（RGBA）= 1704
         * memcpy(dst_data + i * 1704, src_data + i * 1704, 1704); //花屏
         * memcpy(dst_data + i * 1792, src_data + i * 1792, 1704); //正常
         */

        memcpy(dst_data + i * dst_line_size, src_data + i * src_lineSize, dst_line_size);
    }
    ANativeWindow_unlockAndPost(window); // 解锁后 并且刷新 window_buffer的数据显示画面

    pthread_mutex_unlock(&mutex_t);
}

jint JNI_OnLoad(JavaVM *javaVm, void *args) {
    ::vm = javaVm;
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT  void JNICALL
Java_com_zj_ffmpeg_1lib_player_MediaPlayer_nativePrepare(JNIEnv *env, jobject job, jstring path) {
    auto *pCallback = new JNICallback(vm, env, job);
    const char *sourcePath = env->GetStringUTFChars(path, nullptr);
    LOGE("path: %s", sourcePath);
    mediaPlayer = new MediaPlayer(sourcePath, pCallback);
    mediaPlayer->setRenderCallback(renderFrame);
    mediaPlayer->prepare();
    env->ReleaseStringUTFChars(path, sourcePath);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_ffmpeg_1lib_player_MediaPlayer_nativeStart(JNIEnv *env, jobject thiz) {
    if (mediaPlayer) {
        mediaPlayer->start();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_ffmpeg_1lib_player_MediaPlayer_nativeStop(JNIEnv *env, jobject thiz) {
    if (mediaPlayer) {
        mediaPlayer->stop();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_ffmpeg_1lib_player_MediaPlayer_nativeRelease(JNIEnv *env, jobject thiz) {


}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_ffmpeg_1lib_player_MediaPlayer_setSurfaceNative(JNIEnv *env, jobject thiz,
                                                            jobject surface) {
    pthread_mutex_lock(&mutex_t);

    if (window) {
        ANativeWindow_release(window); // 释放窗口
        window = nullptr;
    }

    // 创建新的窗口，用于显示视频
    window = ANativeWindow_fromSurface(env, surface);
    LOGE("赋值window");

    pthread_mutex_unlock(&mutex_t);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_zj_ffmpeg_1lib_player_MediaPlayer_getDurationNative(JNIEnv *env, jobject thiz) {
    if (mediaPlayer) {
        return mediaPlayer->getDuration();
    }
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_ffmpeg_1lib_player_MediaPlayer_nativeSeek(JNIEnv *env, jobject thiz, jint progress) {
    if (mediaPlayer) {
        mediaPlayer->seek(progress);
    }
}