#include "JNICallback.h"

JNICallback::JNICallback(JavaVM *vm, JNIEnv *env, jobject job) {
    this->vm = vm;
    this->env = env;
    this->job = env->NewGlobalRef(job);
    jclass pJclass = env->GetObjectClass(job);
    methodPrepareId = env->GetMethodID(pJclass, "onPrepare", "()V");
}

JNICallback::~JNICallback() {
    vm = nullptr;
    env->DeleteGlobalRef(job);
    job = nullptr;
    env = nullptr;
}

void JNICallback::onError(int thread_mode, char *errorMsg) {

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
