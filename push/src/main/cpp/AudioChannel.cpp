#include <cstring>
#include "AudioChannel.h"
#include "util.h"

AudioChannel::AudioChannel() {

}

AudioChannel::~AudioChannel() {
    if (audioEncoder) {
        faacEncClose(audioEncoder);
        audioEncoder = nullptr;
    }
    DELETE(buffer)
}

void AudioChannel::setAudioCallback(AudioCallback callback) {
    audioCallback = callback;
}

void AudioChannel::initAudioEncoder(int sample_rate, int channels) {
    this->channels = channels;
    // 1. 打开faac编码器
    audioEncoder = faacEncOpen(sample_rate, channels, &inputSamples, &maxOutputBytes);
    if (!audioEncoder) {
        LOGE("打开音频编码器失败");
    }
    // 2. 配置编码器参数
    faacEncConfigurationPtr config = faacEncGetCurrentConfiguration(audioEncoder);
    config->mpegVersion = MPEG4;
    config->aacObjectType = LOW;
    config->inputFormat = FAAC_INPUT_16BIT;
    config->outputFormat = 0;
    // 开启降噪
    config->useTns = 1;
    config->useLfe = 0;

    // 3.配置参数给编码器
    int ret = faacEncSetConfiguration(audioEncoder, config);
    if (!ret) {
        LOGE("初始化编码器失败");
        return;
    }
    LOGE("初始化编码器成功");
    buffer = new u_char(maxOutputBytes);
}

int AudioChannel::getInputSamples() {
    return inputSamples;
}

void AudioChannel::encodeData(int8_t *data) {
    /**
     * audioEncoder：编码器
     * data：麦克风采集数据
     * inputSamples：样本数
     * buffer：输出缓冲区
     * maxOutputBytes：输出缓冲区大小
     * return：返回编码后数据字节长度
     */
    int byteLne = faacEncEncode(audioEncoder, reinterpret_cast<int32_t *>(data), inputSamples,
                                buffer, maxOutputBytes);
    if (byteLne > 0) {
        RTMPPacket *packet = new RTMPPacket();
        int bodyLen = 2 + byteLne;
        RTMPPacket_Alloc(packet, bodyLen);

        packet->m_body[0] = 0xaf;
        if (channels == 1) {
            packet->m_body[0] = 0xae;
        }
        packet->m_body[1] = 0x01;
        memcpy(&packet->m_body[2], buffer, bodyLen);

        packet->m_packetType = RTMP_PACKET_TYPE_AUDIO; // 包类型，音频
        packet->m_nBodySize = bodyLen;
        packet->m_nChannel = 11; // 通道ID
        packet->m_nTimeStamp = -1; // 帧数据有时间戳
        packet->m_hasAbsTimestamp = 0; // 一般都不用
        packet->m_headerType = RTMP_PACKET_SIZE_LARGE; // 大包的类型，如果是头信息，可以给一个小包

        LOGE("发送音频流数据");
        audioCallback(packet);
    }
}

/**
 * 序列头
 */
RTMPPacket * AudioChannel::getAudioSeqHeader() {
    u_char *ppBuffer;
    u_long len;

    // 获取编码器的解码配置信息
    faacEncGetDecoderSpecificInfo(audioEncoder, &ppBuffer, &len);

    //看图表，拼数据
    RTMPPacket *packet = new RTMPPacket;

    int body_size = 2 + len;

    RTMPPacket_Alloc(packet, body_size);

    packet->m_body[0] = 0xAF; // 双声道
    if (channels == 1) {
        packet->m_body[0] = 0xAE; // 单声道
    }

    packet->m_body[1] = 0x00;

    memcpy(&packet->m_body[2], ppBuffer, 2); // 16bit == 2个字节 可以写死

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO; // 包类型，音频
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 11; // 通道ID
    packet->m_nTimeStamp = 0; // 头一般都是没有时间搓的
    packet->m_hasAbsTimestamp = 0; // 一般都不用
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE; // 大包的类型，如果是头信息，可以给一个小包

    return packet;
}

