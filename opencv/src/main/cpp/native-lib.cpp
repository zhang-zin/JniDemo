#include <jni.h>

extern "C"  JNIEXPORT void JNICALL Java_org_opencv_android_Utils_nBitmapToMat2
        (JNIEnv *env, jclass, jobject bitmap, jlong m_addr, jboolean needUnPremultiplyAlpha);
extern "C"  JNIEXPORT void JNICALL Java_org_opencv_android_Utils_nMatToBitmap
        (JNIEnv *env, jclass, jlong m_addr, jobject bitmap);


extern "C"
JNIEXPORT jobject JNICALL
Java_com_zj_opencv_ImageProcess_getIdNumber(JNIEnv *env, jobject thiz, jobject src,
                                            jobject config) {

    jclass type = env->GetObjectClass(thiz);
    //Java_org_opencv_android_Utils_nBitmapToMat2(env, type, src, (jlong) &src_img, 0);


}