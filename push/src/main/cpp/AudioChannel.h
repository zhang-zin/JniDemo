#ifndef JNIDEMO_AUDIOCHANNEL_H
#define JNIDEMO_AUDIOCHANNEL_H

#include <faac.h>
#include <rtmp.h>
#include <sys/types.h>

class AudioChannel {

public:
    AudioChannel();

    ~AudioChannel();

    typedef void (*AudioCallback)(RTMPPacket *packet);

private:
    AudioCallback audioCallback;
    u_long inputSamples;   // faac编码器 输出的样本数
    u_long maxOutputBytes;
    int channels;
    faacEncHandle audioEncoder = 0;
    u_char *buffer = 0;

public:
    void setAudioCallback(void (*param)(RTMPPacket *));

    void initAudioEncoder(int sample_rate, int channels);

    void encodeData(signed char *data);

    RTMPPacket * getAudioSeqHeader();

    int getInputSamples();
};

#endif