#include "MediaPlayer.h"

MediaPlayer::MediaPlayer(const char *path, JNICallback *pCallback) {
    LOGE("MediaPlayer");
    this->data_source = new char[strlen(path) + 1];
    stpcpy(this->data_source, path);
    this->callback = pCallback;
    pthread_mutex_init(&seek_mutex, nullptr);
}

MediaPlayer::~MediaPlayer() {
    LOGE("~MediaPlayer");
    delete data_source;
    delete callback;

    pthread_mutex_destroy(&seek_mutex);
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
        avformat_close_input(&formatContext);
        return;
    }

    //第二步：查找媒体中的音视频流信息
    r = avformat_find_stream_info(formatContext, nullptr);
    if (r < 0) {
        //查找媒体中的音视频流信息失败
        errorCallback(r, THREAD_CHILD, FFMPEG_CAN_NOT_FIND_STREAMS);
        avformat_close_input(&formatContext);
        return;
    }

    duration = formatContext->duration / AV_TIME_BASE; //获取视频总时长
    AVCodecContext *codecContext = nullptr;

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
        codecContext = avcodec_alloc_context3(avCodec);
        if (!codecContext) {
            //编解码上下文失败
            errorCallback(r, THREAD_CHILD, FFMPEG_ALLOC_CODEC_CONTEXT_FAIL);
            avcodec_free_context(&codecContext);
            avformat_close_input(&formatContext);
            return;
        }
        //第八步：编解码器设置参数
        r = avcodec_parameters_to_context(codecContext, parameters);
        if (r < 0) {
            errorCallback(r, THREAD_CHILD, FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL);
            avcodec_free_context(&codecContext);
            avformat_close_input(&formatContext);
            return;
        }
        //第九步：打开编解码器
        r = avcodec_open2(codecContext, avCodec, nullptr);
        if (r) {
            errorCallback(r, THREAD_CHILD, FFMPEG_OPEN_DECODER_FAIL);
            avcodec_free_context(&codecContext);
            avformat_close_input(&formatContext);
            return;
        }

        AVRational time_base = stream->time_base;

        //第十步：从编解码器参数中获取流的类型
        if (parameters->codec_type == AVMediaType::AVMEDIA_TYPE_AUDIO) {
            //音频
            audio_channel = new AudioChannel(i, codecContext, time_base);
            if (duration > 0) {
                audio_channel->setJNICallback(callback);
            }
            LOGE("创建音频流通道：%d", i);
        } else if (parameters->codec_type == AVMediaType::AVMEDIA_TYPE_VIDEO) {
            //视频

            if (stream->disposition & AV_DISPOSITION_ATTACHED_PIC) {
                //封面流
                continue;
            }

            AVRational fps_rational = stream->avg_frame_rate;  //fps
            int fps = av_q2d(fps_rational);

            video_channel = new VideoChannel(i, codecContext, time_base, fps);
            video_channel->setRenderCallback(renderCallback);
            if (duration > 0) {
                video_channel->setJNICallback(callback);
            }
            LOGE("创建视频流通道：%d", i);
        }
    }

    if (!audio_channel && !video_channel) {
        errorCallback(r, THREAD_CHILD, FFMPEG_NOMEDIA);
        if (codecContext) {
            avcodec_free_context(&codecContext);
        }
        avformat_close_input(&formatContext);
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
        video_channel->setAudioChannel(audio_channel);
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

        if (video_channel && video_channel->packets.size() > 100) {
            av_usleep(10 * 1000);
            continue;
        }

        if (audio_channel && audio_channel->packets.size() > 100) {
            av_usleep(10 * 1000);
            continue;
        }
        //AVPacket 压缩包，可能是视频或音频
        AVPacket *packet = av_packet_alloc();
        int ret = av_read_frame(formatContext, packet);
        if (!ret) {
            //ret == 0 ok
            if (video_channel && video_channel->stream_index == packet->stream_index) {
                video_channel->packets.insertToQueue(packet);
            } else if (audio_channel && audio_channel->stream_index == packet->stream_index) {
                audio_channel->packets.insertToQueue(packet);
            }
        } else if (ret == AVERROR_EOF) {
            if (video_channel->packets.empty() && audio_channel->packets.empty()) {
                break;
            }
        } else {
            LOGE("读取压缩包失败");
            break; //av_read_frame 出现了错误，结束当前循环
        }
    }
    LOGE("将压缩包放入结束");

    isPlaying = false;
    video_channel->stop();
    audio_channel->stop();
}

jint MediaPlayer::getDuration() {
    return duration;
}

void MediaPlayer::seek(int progress) {
    if (progress < 0 || progress > duration) {
        return;
    }
    if (!audio_channel) {
        return;
    }
    if (!video_channel) {
        return;
    }
    if (!formatContext) {
        return;
    }

    pthread_mutex_lock(&seek_mutex);

    /**
     * @param stream_index 代表默认情况，FFmpeg自动选择 音频 还是 视频
     * @param flags AVSEEK_FLAG_ANY 直接精准到 拖动的位置，问题：如果不是关键帧，B帧 可能会造成 花屏情况
     *              AVSEEK_FLAG_BACKWARD（则优  8的位置 B帧 ， 找附件的关键帧 6，如果找不到他也会花屏）
     *              AVSEEK_FLAG_FRAME 找关键帧（非常不准确，可能会跳的太多），一般不会直接用，但是会配合用
     */
    LOGE("拖动播放：%d", progress);
    int r = av_seek_frame(formatContext, -1, progress * AV_TIME_BASE, AVSEEK_FLAG_FRAME);
    if (r < 0) {
        pthread_mutex_unlock(&seek_mutex);
        return;
    }

    if (audio_channel) {
        audio_channel->packets.setWork(0);
        audio_channel->packets.clear();
        audio_channel->packets.setWork(1);

        audio_channel->frames.setWork(0);
        audio_channel->frames.clear();
        audio_channel->frames.setWork(1);
    }

    if (video_channel) {
        video_channel->packets.setWork(0);
        video_channel->packets.clear();
        video_channel->packets.setWork(1);

        video_channel->frames.setWork(0);
        video_channel->frames.clear();
        video_channel->frames.setWork(1);
    }

    pthread_mutex_unlock(&seek_mutex);
}

void *task_stop(void *args) {
    auto *player = static_cast<MediaPlayer *>(args);
    player->stop_(player);
    return nullptr;
}

void MediaPlayer::stop() {
    callback = nullptr;
    if (audio_channel) {
        audio_channel->setJNICallback(nullptr);
    }
    if (video_channel) {
        video_channel->setJNICallback(nullptr);
    }
    pthread_create(&pid_stop, nullptr, task_stop, this);
}

void MediaPlayer::stop_(MediaPlayer *pPlayer) {
    isPlaying = false;
    pthread_join(pid_prepare, nullptr);
    pthread_join(pid_start, nullptr);

    if (formatContext) {
        avformat_close_input(&formatContext);
        avformat_free_context(formatContext);
    }

    DELETE(audio_channel);
    DELETE(video_channel);
    DELETE(pPlayer);
}



//endregion
