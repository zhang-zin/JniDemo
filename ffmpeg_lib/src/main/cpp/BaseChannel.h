#ifndef JNIDEMO_BASECHANNEL_H
#define JNIDEMO_BASECHANNEL_H

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/time.h>
}

#include "util.h"
#include "safe_queue.h"
#include "JNICallback.h"

class BaseChannel {

public:
    int stream_index; // 音频或视频在流中的下标
    SafeQueue<AVPacket *> packets; //压缩数据包
    SafeQueue<AVFrame *> frames; //原始包
    bool isPlaying; //播放标记
    AVCodecContext *codecContext = 0; //音视频解码器上下文

    AVRational time_base; //音视频同步，时间基（时间单位）

    JNICallback *callback = 0;

    void setJNICallback(JNICallback *jniCallback) {
        callback = jniCallback;
    }

    BaseChannel(int stream_index, AVCodecContext *codecContext, AVRational time_base) :
            stream_index(stream_index),
            codecContext(codecContext),
            time_base(time_base) {
        packets.setReleaseCallback(releaseAVPacket);
        frames.setReleaseCallback(releaseAVFrame);
    }

    virtual ~BaseChannel() {
        packets.clear();
        frames.clear();
    }

    static void releaseAVPacket(AVPacket **p) {
        if (p) {
            av_packet_free(p);
            *p = 0;
        }
    }

    static void releaseAVFrame(AVFrame **f) {
        if (f) {
            av_frame_free(f);
            *f = 0;
        }
    }
};

#endif