#include <jni.h>
#include <string>

#include <rtmp.h> // 查找系统的环境变量 <// >
#include <pthread.h>
#include "VideoChannel.h"
#include "util.h"
#include "safe_queue.h"

typedef char *string;
extern "C"
JNIEXPORT jstring JNICALL
Java_com_zj_push_MainActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from c++";
    char version[50];
    sprintf(version, "librtmp version: %d", RTMP_LibVersion());
    return env->NewStringUTF(version);
}
bool isStart;
bool readyPushing;
uint32_t start_time; // 记录时间戳
SafeQueue<RTMPPacket *> packets;

VideoChannel *videoChannel = nullptr;
pthread_t pid_start;

/**
 * 释放工作
 * @param pRtmpPacket
 */
void releasePackets(RTMPPacket **pRtmpPacket) {
    if (pRtmpPacket) {
        RTMPPacket_Free(*pRtmpPacket);
        delete pRtmpPacket;
    }
}

/**
 * 子线程连接推流服务器并开始推流
 * @param args 推流的地址
 * @return
 */
void *task_start(void *args) {
    char *url = static_cast<char *>(args);

    RTMP *rtmp = nullptr;
    int ret; // 返回值判断是否成功
    do {
        // 1. rtmp初始化
        rtmp = RTMP_Alloc();
        if (!rtmp) {
            LOGE("rtmp 初始化失败");
            break;
        }
        // 2. rtmp设置流媒体地址
        RTMP_Init(rtmp);
        rtmp->Link.timeout = 5; // 设置连接的超时时间 5s
        ret = RTMP_SetupURL(rtmp, url);
        if (!ret) {
            LOGE("rtmp 设置流媒体地址失败");
            break;
        }

        // 3. 开启输出模式
        RTMP_EnableWrite(rtmp);

        // 4. 建立连接
        ret = RTMP_Connect(rtmp, nullptr);
        if (!ret) {
            LOGE("rtmp 建立连接失败:%d, url: %s", ret, url);
            break;
        }

        // 5. 连接流
        ret = RTMP_ConnectStream(rtmp, 0);
        if (!ret) {
            LOGE("rtmp 连接流失败");
            break;
        }
        start_time = RTMP_GetTime();
        readyPushing = true; // 准备好了，可以向服务器开始推流
        packets.setWork(1); //队列开始工作

        RTMPPacket *packet = nullptr;
        while (readyPushing) {
            packets.getQueueAndDel(packet);
            if (!readyPushing) {
                break;
            }
            if (!packet) {
                continue;
            }

            // 给rtmp的流id
            packet->m_nInfoField2 = rtmp->m_stream_id;
            ret = RTMP_SendPacket(rtmp, packet, 1); // queue = 1，开启内部缓冲
            releasePackets(&packet);
            if (!ret) {
                LOGE("rtmp 推流失败");
                break;
            }
        }
        releasePackets(&packet);
    } while (false);

    // 推流结束，开始释放
    isStart = false;
    readyPushing = false;
    packets.setWork(0);
    packets.clear();

    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete url;
    return nullptr;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_push_Pusher_native_1init(JNIEnv *env, jobject thiz) {
    videoChannel = new VideoChannel();

    packets.setReleaseCallback(releasePackets);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_push_Pusher_native_1start(JNIEnv *env, jobject thiz, jstring path_) {
    if (isStart) {
        return;
    }
    isStart = true;
    const char *path = env->GetStringUTFChars(path_, nullptr);
    char *url = new char(strlen(path) + 1);
    strcpy(url, path);
    pthread_create(&pid_start, nullptr, task_start, url);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_push_Pusher_native_1stop(JNIEnv *env, jobject thiz) {
    // TODO: implement native_stop()
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_push_Pusher_native_1release(JNIEnv *env, jobject thiz) {
    // TODO: implement native_release()
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_push_Pusher_native_1pushVideo(JNIEnv *env, jobject thiz) {
    // TODO: implement native_pushVideo()
}

extern "C"
JNIEXPORT void JNICALL
Java_com_zj_push_Pusher_native_1initVideoEncoder(JNIEnv *env, jobject thiz, jint width, jint height,
                                                 jint fps, jint bitrate) {
    // TODO: implement native_initVideoEncoder()
}