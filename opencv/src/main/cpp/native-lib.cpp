#include <jni.h>
#include <android/bitmap.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc/types_c.h>
#include "util.h"

#define DEFAULT_CARD_WIDTH 640
#define DEFAULT_CARD_HEIGHT 400
#define FIX_ID_CARD_SIZE Size(DEFAULT_CARD_WIDTH,DEFAULT_CARD_HEIGHT)
#define FIX_TEMPLATE_SIZE  Size(153, 28)

using namespace cv;
using namespace std;

extern "C" JNIEXPORT void JNICALL Java_org_opencv_android_Utils_nBitmapToMat2
        (JNIEnv *env, jclass, jobject bitmap, jlong m_addr, jboolean needUnPremultiplyAlpha);
extern "C" JNIEXPORT void JNICALL Java_org_opencv_android_Utils_nMatToBitmap
        (JNIEnv *env, jclass, jlong m_addr, jobject bitmap);

jobject createBitmap(JNIEnv *env, Mat srcData, jobject config) {
    // Image Details
    int imgWidth = srcData.cols;
    int imgHeight = srcData.rows;
    int numPix = imgWidth * imgHeight;
    jclass bmpCls = env->FindClass("android/graphics/Bitmap");
    jmethodID createBitmapMid = env->GetStaticMethodID(bmpCls, "createBitmap",
                                                       "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jobject jBmpObj = env->CallStaticObjectMethod(bmpCls, createBitmapMid, imgWidth, imgHeight,
                                                  config);

    Java_org_opencv_android_Utils_nMatToBitmap(env, 0, (jlong) &srcData, jBmpObj);
    return jBmpObj;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_zj_opencv_ImageProcess_getIdNumber(JNIEnv *env, jobject thiz, jobject src,
                                            jobject config) {
    Mat src_img;
    Mat dst_img;
    jclass type = env->GetObjectClass(thiz);
    Java_org_opencv_android_Utils_nBitmapToMat2(env, type, src, (jlong) &src_img, 0);

    Mat dst;
    //无损压缩 640x400
    resize(src_img, src_img, FIX_ID_CARD_SIZE);
    //灰度化
    cvtColor(src_img, dst, COLOR_BGR2GRAY);
    //二值化
    threshold(dst, dst, 100, 255, CV_THRESH_BINARY);
    //膨胀
    Mat erodeElement = getStructuringElement(MORPH_RECT, Size(20, 10));
    erode(dst, dst, erodeElement);

    //轮廓检测
    vector<vector<Point>> contours;
    vector<Rect> rects;
    findContours(dst, contours, RETR_TREE, CHAIN_APPROX_NONE, Point(0, 0));

    for (auto &contour : contours) {
        Rect rect = boundingRect(contour);
        //rectangle(dst, rect, Scalar(0, 0, 255));  // 在dst 图片上显示 rect 矩形
        if (rect.width > rect.height * 9) {
            rects.push_back(rect);
            //rectangle(dst, rect, Scalar(0, 255, 255));
            //dst_img = src_img(rect);
        }
    }

    if (rects.size() == 1) {
        dst_img = src_img(rects.at(0));
    } else {
        int lowPoint = 0;
        Rect finalRect;
        for (auto rect : rects) {
            if (rect.tl().y > lowPoint) {
                lowPoint = rect.tl().y;
                finalRect = rect;
            }
        }
        //rectangle(dst, finalRect, Scalar(0, 255, 255));
        dst_img = src_img(finalRect);
    }

    jobject bitmap = createBitmap(env, dst_img, config);

    end:
    src_img.release();
    dst_img.release();
    dst.release();

    return bitmap;
}