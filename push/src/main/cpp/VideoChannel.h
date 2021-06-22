#ifndef JNIDEMO_VIDEOCHANNEL_H
#define JNIDEMO_VIDEOCHANNEL_H

#include <rtmp.h>
#include <sys/types.h>
#include <x264.h>

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
    x264_picture_t *pic_in;    // 每一张图片
    VideoCallback videoCallback;
};

#endif
