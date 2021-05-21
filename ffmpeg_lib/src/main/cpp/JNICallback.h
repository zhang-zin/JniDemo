
#ifndef JNIDEMO_JNICALLBACK_H
#define JNIDEMO_JNICALLBACK_H

#include <jni.h>
#include "util.h"

class JNICallback {

private:
    JavaVM *vm = 0;
    JNIEnv *env = 0;
    jobject job;
    jmethodID methodPrepareId;
    jmethodID methodErrorId;

public:

    JNICallback(JavaVM *vm, JNIEnv *env, jobject job);

    ~JNICallback();

    void onPrepared(int thread_mode);

    void onError(int thread_mode,int code,char * errorMsg);
};


#endif
