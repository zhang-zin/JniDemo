#ifndef NE_PLAYER_MACRO_H
#define NE_PLAYER_MACRO_H

#include <android/log.h>

#define THREAD_MAIN 1 // 主线程
#define THREAD_CHILD 2 // 子线程

#define TAG "JNI"

//__VA_ARGS__ 代表可变参数
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)

#define DELETE(object) if (object) { delete object; object = 0;}

#endif
