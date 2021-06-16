#include <jni.h>
#include <string>

#include <rtmp.h> // 查找系统的环境变量 <// >
#include <x 264.h>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_zj_push_MainActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from c++";
    char version[50];
    sprintf(version, "librtmp version: %d", RTMP_LibVersion());
    return env->NewStringUTF(version);
}