#include <jni.h>
#include <string>
#include <iostream>
#include "JNICallback.h"
#include "MediaPlayer.h"

extern "C" {
#include <libavutil/avutil.h>
}


JavaVM *vm = nullptr;
MediaPlayer *mediaPlayer = nullptr;

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
    mediaPlayer->prepare();
    env->ReleaseStringUTFChars(path, sourcePath);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_ffmpeg_1lib_player_MediaPlayer_nativeStart(JNIEnv *env, jobject thiz) {
    if (mediaPlayer){

    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_ffmpeg_1lib_player_MediaPlayer_nativeStop(JNIEnv *env, jobject thiz) {

}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_ffmpeg_1lib_player_MediaPlayer_nativeRelease(JNIEnv *env, jobject thiz) {


}