#include "JNICallback.h"

JNICallback::JNICallback(JavaVM *vm, JNIEnv *env, jobject job) {
    this->vm = vm;
    this->env = env;
    this->job = env->NewGlobalRef(job);
    jclass pJclass = env->GetObjectClass(job);
    methodPrepareId = env->GetMethodID(pJclass, "onPrepare", "()V");
    methodErrorId = env->GetMethodID(pJclass, "onError", "(ILjava/lang/String;)V");
    methodProgress = env->GetMethodID(pJclass, "onProgress", "(I)V");
}

JNICallback::~JNICallback() {
    vm = nullptr;
    env->DeleteGlobalRef(job);
    job = nullptr;
    env = nullptr;
}

void JNICallback::onPrepared(int thread_mode) {
    if (thread_mode == THREAD_MAIN) {
        //主线程
        env->CallVoidMethod(job, methodPrepareId);
    } else {
        //子线程
        JNIEnv *jniEnv;
        vm->AttachCurrentThread(&jniEnv, nullptr);
        jniEnv->CallVoidMethod(job, methodPrepareId);
        vm->DetachCurrentThread();
    }
}

void JNICallback::onError(int thread_mode, int code, char *errorMsg) {
    if (thread_mode == THREAD_MAIN) {
        LOGE("回调主线程");
        jstring msg = env->NewStringUTF(errorMsg);
        env->CallVoidMethod(job, methodErrorId, code, msg);
    } else {
        LOGE("回调子线程");
        JNIEnv *jniEnv;
        vm->AttachCurrentThread(&jniEnv, nullptr);
        jstring msg = jniEnv->NewStringUTF(errorMsg);
        jniEnv->CallVoidMethod(job, methodErrorId, code, msg);
        vm->DetachCurrentThread();
    }
}

void JNICallback::onProgress(int thread_mode, int time) {
    if (thread_mode == THREAD_MAIN) {
        env->CallVoidMethod(job, methodProgress, time);
    } else{
        JNIEnv *jniEnv;
        vm->AttachCurrentThread(&jniEnv, nullptr);
        jniEnv->CallVoidMethod(job, methodProgress, time);
        vm->DetachCurrentThread();
    }
}
