#ifndef JNIDEMO_VIDEOCHANNEL_H
#define JNIDEMO_VIDEOCHANNEL_H

#include "BaseChannel.h"

extern "C" {
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
}

class VideoChannel : public BaseChannel {

private:
    pthread_t pid_video_decode;
    pthread_t pid_video_play;

public:
    VideoChannel(int stream_index, AVCodecContext *codecContext);

    ~VideoChannel();

    void start();

    void stop();

    void video_decode();

    void video_play();

};

#endif
