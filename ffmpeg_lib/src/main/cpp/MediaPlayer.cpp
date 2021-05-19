#include "MediaPlayer.h"

MediaPlayer::MediaPlayer(const char *path, JNICallback *pCallback) {
    this->data_source = new char[strlen(path) + 1];
    stpcpy(this->data_source, path);
    this->callback = pCallback;
}

MediaPlayer::~MediaPlayer() {
    delete data_source;
    delete callback;
}

void *task_prepare(void *args) {
    auto *player = static_cast<MediaPlayer *>(args);
    player->prepare_();
    return nullptr;
}


void MediaPlayer::prepare() {
    pthread_create(&pid_prepare, nullptr, task_prepare, this);
}

void MediaPlayer::prepare_() {
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
        /*char *msg = "打开媒体地址失败";
        callback->onError(THREAD_CHILD, msg);*/
        return;
    }

    //第二步：查找媒体中的音视频流信息
    r = avformat_find_stream_info(formatContext, nullptr);
    if (r < 0) {
        /* char *msg = "查找媒体中的音视频流信息失败";
         callback->onError(THREAD_CHILD, msg);*/
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
        //第七步：编解码上下文，真正干活的
        AVCodecContext *codecContext = avcodec_alloc_context3(avCodec);
        if (!codecContext) {
            /*char *msg = "编解码上下文失败";
            callback->onError(THREAD_CHILD, msg);*/
            return;
        }
        //第八步：编解码器设置参数
        r = avcodec_parameters_to_context(codecContext, parameters);
        if (r < 0) {
            /*char *msg = "编解码上下文设置参数失败";
            callback->onError(THREAD_CHILD, msg);*/
            return;
        }
        //第九步：打开编解码器
        r = avcodec_open2(codecContext, avCodec, nullptr);
        if (r) {
            /*char *msg = "打开编解码器失败";
            callback->onError(THREAD_CHILD, msg);*/
            return;
        }

        //第十步：从编解码器参数中获取流的类型
        if (parameters->codec_type == AVMediaType::AVMEDIA_TYPE_AUDIO) {
            //音频
            audio_channel = new AudioChannel();
        } else if (parameters->codec_type == AVMediaType::AVMEDIA_TYPE_VIDEO) {
            //视频
            video_channel = new VideoChannel();
        }

    }

    if (!audio_channel && !video_channel) {
        /* char *msg = "失败";
         callback->onError(THREAD_CHILD, msg);*/
        return;
    }

    if (callback) {
        callback->onPrepared(THREAD_CHILD);
    }
}

