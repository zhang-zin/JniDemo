#include "AudioChannel.h"


AudioChannel::AudioChannel(int stream_index, AVCodecContext *avCodecContext, AVRational time_base)
        : BaseChannel(
        stream_index, avCodecContext, time_base) {
    out_channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO); //返回通道数
    out_sample_size = av_get_bytes_per_sample(AV_SAMPLE_FMT_S16); //返回每个样本的字节数
    out_sample_rate = 44100; //采样率

    out_buffers_size = out_sample_rate * out_sample_size * out_channels;
    out_buffers = static_cast<uint8_t *>(malloc(out_buffers_size));

    // FFmpeg 音频 重采样  音频重采样上下文
    swr_ctx = swr_alloc_set_opts(
            //输出
            nullptr,
            AV_CH_LAYOUT_STEREO, //声道布局类型，双声道
            AV_SAMPLE_FMT_S16, //采样大小
            out_sample_rate, //采样率

            //输入
            avCodecContext->channel_layout, //声道布局类型
            avCodecContext->sample_fmt,     // 采样大小
            avCodecContext->sample_rate,    // 采样率
            0, nullptr
    );

    //初始化重采样上下文
    swr_init(swr_ctx);

}

AudioChannel::~AudioChannel() {

}

void *task_audio_decode(void *args) {
    auto *audio_channel = static_cast<AudioChannel * >( args);
    audio_channel->audio_decode();
    return nullptr;
}

void *task_audio_play(void *args) {
    auto *audio_channel = static_cast<AudioChannel *>( args);
    audio_channel->audio_play();
    return nullptr;
}

void AudioChannel::start() {
    isPlaying = true;

    packets.setWork(1);
    frames.setWork(1);

    //取出队列的压缩包，进行解码 解码之后的原始包 push队列中去
    pthread_create(&pid_audio_decode, nullptr, task_audio_decode, this);
    //从队列中取出原始包播放
    pthread_create(&pid_audio_play, nullptr, task_audio_play, this);

}

void AudioChannel::stop() {

}

void AudioChannel::audio_decode() {
    LOGE("开启子线程进行音频原始包解码");
    AVPacket *packet = nullptr;
    while (isPlaying) {

        if (isPlaying && frames.size() > 100) {
            av_usleep(10 * 1000);
            continue;
        }

        int ret = packets.getQueueAndDel(packet); //阻塞式
        if (!isPlaying) {
            break;
        }

        if (!ret) {
            continue;
        }

        ret = avcodec_send_packet(codecContext, packet); //发送AVPacket压缩包到缓冲区
        if (ret) {
            break; //avcodec_send_packet出现错误
        }
        //从缓冲区获取原始包
        AVFrame *frame = av_frame_alloc();
        ret = avcodec_receive_frame(codecContext, frame);
        if (ret == AVERROR(EAGAIN)) {
            continue;
        } else if (ret != 0) {
            LOGE("原始包解码失败");
            releaseAVPacket(&packet);
            break;
        }
        //拿到了原始包
        frames.insertToQueue(frame);

        av_packet_unref(packet);
        releaseAVPacket(&packet); //FFmpeg 缓存了一份AVPacket

    }
    av_packet_unref(packet);
    releaseAVPacket(&packet);
}

/**
 * 回调函数
 * @param bq  队列
 * @param args  this // 给回调函数的参数
 */
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *args) {

    auto *audio_channel = static_cast<AudioChannel *>(args);

    int pcm_size = audio_channel->getPCM();

    // 添加数据到缓冲区里面去
    (*bq)->Enqueue(
            bq, // 传递自己，为什么（因为没有this，为什么没有this，因为不是C++对象，所以需要传递自己） JNI讲过了
            audio_channel->out_buffers, // PCM数据
            pcm_size); // PCM数据对应的大小，缓冲区大小怎么定义？（复杂）
}

/**
 * 给out_buffers赋值和确定大小
 * @return 返回重采样之后的大小
 */
int AudioChannel::getPCM() {
    int pcm_data_size = 0;
    AVFrame *frame = nullptr;
    if (isPlaying) {
        int ret = frames.getQueueAndDel(frame);
        if (!ret) {
            return 0;
        }

        //开始重采样
        // 获取单通道的样本数 (计算目标样本数： ？ 10个48000 --->  48000/44100因为除不尽  11个44100)
        int dst_nb_samples = av_rescale_rnd(
                swr_get_delay(swr_ctx, frame->sample_rate) +
                frame->nb_samples, // 获取下一个输入样本相对于下一个输出样本将经历的延迟
                out_sample_size,     // 输出采样率
                frame->sample_rate,  // 输入采样率
                AV_ROUND_UP     // 先上取 取去11个才能容纳的上
        );

        int samples_per_channel = swr_convert(
                swr_ctx,
                //输出
                &out_buffers,   //重采样之后的buffers
                dst_nb_samples, //重采样之后的单通道的样本数
                //输出
                (const uint8_t **) frame->data, // 队列的AVFrame * 那的  PCM数据 未重采样的
                frame->nb_samples // 输入的样本数
        );
        pcm_data_size = samples_per_channel * out_sample_size * out_channels;

        // 音视频同步时间基TimeBase fps25 一秒钟25帧，每一帧25分之1 ，25分之1就是时间基
        audio_time = frame->best_effort_timestamp * av_q2d(time_base);//时间有单位，ffmepg中有自己的单位：时间基
    }
    return pcm_data_size;
}

void AudioChannel::audio_play() {
    SLresult result; // 执行结果

    //region 1、创建引擎对象，并获取引擎接口
    //1.1 创建引擎对象
    result = slCreateEngine(&engineObject, 0, nullptr, 0, nullptr, nullptr);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("创建引擎对象失败");
        return;
    }
    //1.2 初始化引擎 SL_BOOLEAN_FALSE : 延迟等待创建成功
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("初始化引擎失败");
        return;
    }
    //1.3 获取引擎接口
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineInterface);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("获取引擎接口失败");
        return;
    }
    if (engineInterface) {
        LOGD("创建引擎接口 create success");
    } else {
        LOGD("创建引擎接口 create error");
        return;
    }
    //endregion

    //region 2、设置混音器
    //2.1 创建混音器
    result = (*engineInterface)->CreateOutputMix(engineInterface, &outputMixObject, 0, nullptr,
                                                 nullptr);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("创建混音器失败");
        return;
    }
    //2.1 初始化混音器
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
        LOGE("初始化混音器失败");
        return;
    }
    // 不启用混响可以不用获取混音器接口 【声音的效果】
    // 获得混音器接口
    /*
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
                                             &outputMixEnvironmentalReverb);
    if (SL_RESULT_SUCCESS == result) {
    // 设置混响 ： 默认。
    SL_I3DL2_ENVIRONMENT_PRESET_ROOM: 室内
    SL_I3DL2_ENVIRONMENT_PRESET_AUDITORIUM : 礼堂 等
    const SLEnvironmentalReverbSettings settings = SL_I3DL2_ENVIRONMENT_PRESET_DEFAULT;
    (*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(
           outputMixEnvironmentalReverb, &settings);
    }
    */
    LOGI("2、设置混音器 Success");
    //endregion

    //region 3、创建播放器
    //创建buffer缓存类型队列
    SLDataLocator_AndroidSimpleBufferQueue simpleBufferQueue = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 10
    };
    //PCM格式不能直接播放
    SLDataFormat_PCM formatPcm = {
            SL_DATAFORMAT_PCM, //PCM数据格式
            2,    //声道数
            SL_SAMPLINGRATE_44_1, //采样率
            SL_PCMSAMPLEFORMAT_FIXED_16, //每秒采样样本 16bit
            SL_PCMSAMPLEFORMAT_FIXED_16, //每个样本位数 16bit
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT, //前左声道  前右声道
            SL_BYTEORDER_LITTLEENDIAN //字节序（小端）
    };
    //3.1 将上面的配置信息放入到数据源，audioSrc最终配置的音频信息结果
    SLDataSource audioSrc = {&simpleBufferQueue, &formatPcm};

    //3.2 配置音轨（输出）
    //设置混音器 SL_DATALOCATOR_OUTPUTMIX: 输出混音器类型
    SLDataLocator_OutputMix locatorOutputMix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSink = {&locatorOutputMix, nullptr}; //混音器最终结果

    //创建操作队列需要的接口
    const SLInterfaceID ids[1] = {SL_IID_BUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};

    //3.3 创建播放器 SLObjectItf bqPlayerObject
    result = (*engineInterface)->CreateAudioPlayer(
            engineInterface, //引擎接口
            &bqPlayerObject, //播放器
            &audioSrc,       //音频配置信息
            &audioSink,      //混音器
            //打开队列的操作
            1,   //开放参数的个数
            ids, //需要buffer
            req  //上面的buffer需要开放出去
    );

    if (SL_RESULT_SUCCESS != result) {
        LOGD("创建播放器失败");
        return;
    }

    // 3.4 初始化播放器：SLObjectItf bqPlayerObject
    result = (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
        LOGD("实例化播放器失败");
        return;
    }
    LOGD("创建播放器CreateAudioPlayer成功");

    // 3.5 获取播放器接口 【以后播放全部使用】
    result = (*bqPlayerObject)->GetInterface(
            bqPlayerObject,
            SL_IID_PLAY,// SL_IID_PLAY:播放接口 == iplayer
            &bqPlayerPlay
    );
    if (SL_RESULT_SUCCESS != result) {
        LOGD("获取播放接口失败");
        return;
    }
    LOGD("3、创建播放器成功");

    //endregion

    //region 4、设置回调函数
    //4.1 获取播放器队列接口
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE,
                                             &bqPlayerBufferQueue);
    if (result) {
        LOGD("获取播放队列 GetInterface SL_IID_BUFFERQUEUE failed!");
        return;
    }

    //4.2 设置回调
    (*bqPlayerBufferQueue)->RegisterCallback(bqPlayerBufferQueue, bqPlayerCallback, this);
    LOGD("4、设置播放回调函数 Success");
    //endregion

    //region 5、设置播放器状态为播放状态
    (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
    LOGD("5、设置播放器状态为播放状态 Success");
    //endregion

    //region 6.手动激活回调函数
    bqPlayerCallback(bqPlayerBufferQueue, this);
    //endregion

}
