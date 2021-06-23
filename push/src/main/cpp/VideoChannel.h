#ifndef JNIDEMO_VIDEOCHANNEL_H
#define JNIDEMO_VIDEOCHANNEL_H

#include <rtmp.h>
#include <sys/types.h>
#include <x264.h>
#include <pthread.h>
#include "util.h"
#include <rtmp.h>
#include <cstring>

class VideoChannel {

public:
    VideoChannel();

    ~VideoChannel();

    /**
     * 转换264格式回调
     */
    typedef void (*VideoCallback)(RTMPPacket *packet);

private:
    pthread_mutex_t mutex;
    int y_len;
    int uv_len;
    x264_t *videoEncoder = 0;  // x264编码器
    x264_picture_t *pic_in = 0;    // 每一张图片
    VideoCallback videoCallback;

public:

    void initVideoEncoder(int width, int height, int fps, int bitrate);

    void setVideoCallback(void (*param)(RTMPPacket *));

    void encodeData(signed char *data);

    void sendSpsPps(uint8_t sps[100], uint8_t pps[100], int len, int len1);

    void sendFrame(int type, int payload, uint8_t *payload1);
};

#endif
