
#ifndef JNIDEMO_MEDIAPLAYER_H
#define JNIDEMO_MEDIAPLAYER_H

#include <cstring>
#include <pthread.h>
#include "AudioChannel.h"
#include "VideoChannel.h"
#include "JNICallback.h"
#include "util.h"

extern "C" {
#include <libavformat/avformat.h>
}

class MediaPlayer {

private:
    char *data_source = 0; // 指针 请赋初始值
    pthread_t pid_prepare;
    pthread_t pid_start;
    AVFormatContext *formatContext = 0;
    AudioChannel *audio_channel = 0;
    VideoChannel *video_channel = 0;
    JNICallback *callback = 0;
    bool isPlaying; //是否播放

public:
    MediaPlayer(const char *string, JNICallback *pCallback);

    ~MediaPlayer();

    void prepare();

    void prepare_(); //子线程调用

    void start();

    void start_(); //子线程调用

    void errorCallback(int r, int thread_mode, int code);
};


#endif
