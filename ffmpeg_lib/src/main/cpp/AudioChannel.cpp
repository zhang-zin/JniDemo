#include "AudioChannel.h"


AudioChannel::AudioChannel(int stream_index, AVCodecContext *avCodecContext) : BaseChannel(
        stream_index, avCodecContext) {

}

AudioChannel::~AudioChannel() {

}


void AudioChannel::start() {

}

void AudioChannel::stop() {

}