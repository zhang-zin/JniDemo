#include "MediaPlayer.h"

MediaPlayer::MediaPlayer(const char *path, JNICallback *pCallback) {
    LOGE("MediaPlayer");
    this->data_source = new char[strlen(path) + 1];
    stpcpy(this->data_source, path);
    this->callback = pCallback;
}

MediaPlayer::~MediaPlayer() {
    LOGE("~MediaPlayer");
    delete data_source;
    delete callback;
}

void MediaPlayer::setRenderCallback(RenderCallback renderCallback) {
    this->renderCallback = renderCallback;
}

void MediaPlayer::errorCallback(int r, int thread_mode, int code) {
    if (callback) {
        char *msg = av_err2str(r);
        LOGE("prepare_：打开媒体地址失败, msg: %s", msg);
        callback->onError(thread_mode, code, msg);
    }
}

//region prepare
void *task_prepare(void *args) {
    auto *player = static_cast<MediaPlayer *>(args);
    player->prepare_();
    return nullptr;
}

void MediaPlayer::prepare() {
    LOGE("prepare：开启子线程");
    pthread_create(&pid_prepare, nullptr, task_prepare, this);
}

void MediaPlayer::prepare_() {
    LOGE("prepare_：子线程");
    //子线程
    //第一步：打开媒体地址
    formatContext = avformat_alloc_context();
    AVDictionary *avDictionary = nullptr;

    av_dict_set(&avDictionary, "timeout", "5000000", 0); //单位微秒
    /**
     * 参数1：AVFormatContext *s
     * 参数2：路劲
     * 参数3：Windows、mac打开摄像头，麦克风
     * 参数4：AVDictionary **options 各种设置，http链接超时，打开rtmp超时
     */
    int r = avformat_open_input(&formatContext, data_source, nullptr, &avDictionary);

    av_dict_free(&avDictionary); //释放字典
    if (r) {
        //打开媒体地址失败
        errorCallback(r, THREAD_CHILD, FFMPEG_CAN_NOT_OPEN_URL);
        return;
    }

    //第二步：查找媒体中的音视频流信息
    r = avformat_find_stream_info(formatContext, nullptr);
    if (r < 0) {
        //查找媒体中的音视频流信息失败
        errorCallback(r, THREAD_CHILD, FFMPEG_CAN_NOT_FIND_STREAMS);
        return;
    }

    //第三步：根据流信息，流个数循环来找
    for (int i = 0; i < formatContext->nb_streams; ++i) {
        //第四步：获取媒体流（音频、视频）
        AVStream *stream = formatContext->streams[i];
        //第五步：从流中获取编解码参数
        AVCodecParameters *parameters = stream->codecpar;
        //第六步：根据编解码参数获取解码器
        AVCodec *avCodec = avcodec_find_decoder(parameters->codec_id);
        if (!avCodec) {
            errorCallback(r, THREAD_CHILD, FFMPEG_FIND_DECODER_FAIL);
        }
        //第七步：编解码上下文，真正干活的
        AVCodecContext *codecContext = avcodec_alloc_context3(avCodec);
        if (!codecContext) {
            //编解码上下文失败
            errorCallback(r, THREAD_CHILD, FFMPEG_ALLOC_CODEC_CONTEXT_FAIL);
            return;
        }
        //第八步：编解码器设置参数
        r = avcodec_parameters_to_context(codecContext, parameters);
        if (r < 0) {
            errorCallback(r, THREAD_CHILD, FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL);
            return;
        }
        //第九步：打开编解码器
        r = avcodec_open2(codecContext, avCodec, nullptr);
        if (r) {
            errorCallback(r, THREAD_CHILD, FFMPEG_OPEN_DECODER_FAIL);
            return;
        }

        //第十步：从编解码器参数中获取流的类型
        if (parameters->codec_type == AVMediaType::AVMEDIA_TYPE_AUDIO) {
            //音频
            audio_channel = new AudioChannel(i, codecContext);
        } else if (parameters->codec_type == AVMediaType::AVMEDIA_TYPE_VIDEO) {
            //视频
            video_channel = new VideoChannel(i, codecContext);
            video_channel->setRenderCallback(renderCallback);
        }
    }

    if (!audio_channel && !video_channel) {
        errorCallback(r, THREAD_CHILD, FFMPEG_NOMEDIA);
        return;
    }

    if (callback) {
        callback->onPrepared(THREAD_CHILD);
    }
}
//endregion

//region start
void *task_start(void *args) {
    auto *player = static_cast<MediaPlayer *>(args);

    player->start_();
    return nullptr;
}

void MediaPlayer::start() {
    LOGE("start: 开启子线程");
    isPlaying = true;

    if (video_channel) {
        video_channel->start();
    }

    if (audio_channel) {
        audio_channel->start();
    }

    //把音视频的压缩包加入到队列
    pthread_create(&pid_start, nullptr, task_start, this);
}

void MediaPlayer::start_() {
    LOGE("将压缩包放入队列");
    while (isPlaying) {
        //AVPacket 压缩包，可能是视频或音频
        AVPacket *packet = av_packet_alloc();
        int ret = av_read_frame(formatContext, packet);
        if (!ret) {
            //ret == 0 ok
            if (video_channel && video_channel->stream_index == packet->stream_index) {
                video_channel->packets.insertToQueue(packet);
            } else if (audio_channel && audio_channel->stream_index == packet->stream_index) {

            }
        } else if (ret == AVERROR_EOF) {
            //todo
        } else {
            LOGE("读取压缩包失败");
            break; //av_read_frame 出现了错误，结束当前循环
        }
    }

    isPlaying = false;
    video_channel->stop();
    audio_channel->stop();
}

//endregion
