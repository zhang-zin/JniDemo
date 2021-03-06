#ifndef JNIDEMO_VIDEOCHANNEL_H
#define JNIDEMO_VIDEOCHANNEL_H

#include "BaseChannel.h"
#include "AudioChannel.h"

extern "C" {
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
}

typedef void(*RenderCallback)(uint8_t *, int, int, int); //函数指针 视频渲染回调

class VideoChannel : public BaseChannel {

private:
    pthread_t pid_video_decode{};
    pthread_t pid_video_play{};
    RenderCallback renderCallback;

    AudioChannel *audioChannel;

    int fps;

public:
    VideoChannel(int stream_index, AVCodecContext *codecContext, AVRational time_base, int fps);

    ~VideoChannel();

    void start();

    void stop();

    void video_decode();

    void video_play();

    void setRenderCallback(RenderCallback renderCallback);

    void setAudioChannel(AudioChannel *audio_channel);
};

#endif
