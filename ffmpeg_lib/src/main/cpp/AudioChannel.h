//
// Created by zhangjin on 2021/5/19.
//

#ifndef JNIDEMO_AUDIOCHANNEL_H
#define JNIDEMO_AUDIOCHANNEL_H

#include "BaseChannel.h"

class AudioChannel: public BaseChannel {

public:
    AudioChannel(int stream_index,AVCodecContext * avCodecContext);

    virtual ~AudioChannel();

    void start();

    void stop();
};


#endif //JNIDEMO_AUDIOCHANNEL_H
