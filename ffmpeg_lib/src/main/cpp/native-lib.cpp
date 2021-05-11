#include <jni.h>
#include <string>
#include <android/log.h>
#include <iostream>

extern "C" {
#include <libavutil/avutil.h>
}

#define TAG "JNI"

//__VA_ARGS__ 代表可变参数
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

/**
 * 拿到 ffmpeg 当前版本
 * @return
 */
const char *getFFmpegVer() {
    return av_version_info();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_zj_ffmpeg_1lib_MainActivity_getFFmpegVersion(JNIEnv *env, jclass clazz) {
    return env->NewStringUTF(getFFmpegVer());
}