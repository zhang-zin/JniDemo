
#include "VideoChannel.h"

/**
 * 丢包，原始包不需要考虑关键帧
 * @param q
 */
void dropAVFrame(queue<AVFrame *> &q) {
    if (!q.empty()) {
        AVFrame *frame = q.front();
        BaseChannel::releaseAVFrame(&frame);
        q.pop();
    }
}

/**
 * 丢包 AVPacket * 压缩包 考虑关键帧
 * @param q
 */
void dropAVPacket(queue<AVPacket *> &q) {
    while (!q.empty()) {
        AVPacket *pkt = q.front();
        if (pkt->flags != AV_PKT_FLAG_KEY) { // 非关键帧，可以丢弃
            BaseChannel::releaseAVPacket(&pkt);
            q.pop();
        } else {
            break; // 如果是关键帧，不能丢，那就结束
        }
    }
}

VideoChannel::VideoChannel(int stream_index, AVCodecContext *codecContext, AVRational time_base,
                           int fps) : BaseChannel(
        stream_index, codecContext, time_base) {
    this->fps = fps;
    frames.setSyncCallback(dropAVFrame);
    packets.setSyncCallback(dropAVPacket);
}

VideoChannel::~VideoChannel() {

}

void VideoChannel::setRenderCallback(RenderCallback renderCallback) {
    this->renderCallback = renderCallback;
}

void *task_video_decode(void *args) {
    auto *video_channel = static_cast<VideoChannel * >( args);
    video_channel->video_decode();
    return nullptr;
}

void *task_video_play(void *args) {
    auto *video_channel = static_cast<VideoChannel *>( args);
    video_channel->video_play();
    return nullptr;
}

void VideoChannel::start() {
    isPlaying = true;

    packets.setWork(1);
    frames.setWork(1);

    //取出队列的压缩包，进行解码 解码之后的原始包 push队列中去
    pthread_create(&pid_video_decode, nullptr, task_video_decode, this);
    //从队列中取出原始包播放
    pthread_create(&pid_video_play, nullptr, task_video_play, this);

}

void VideoChannel::stop() {

}

void VideoChannel::video_decode() {
    LOGE("开启子线程进行视频原始包解码");
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
            //B帧参考前面成功，参考后面时报，可能是P帧没有出来需要再拿一次
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

void VideoChannel::video_play() {
    LOGE("从队列取出原始包，播放");
    //从队列取出原始包，播放

    // SWS_FAST_BILINEAR == 很快 可能会模糊
    // SWS_BILINEAR 适中算法
    AVFrame *frame = nullptr;
    uint8_t *dst_data[4]; // RGBA
    int dst_lineSize[4];

    //原始包 YUV数据  ---->[libswscale]  Android屏幕 RGBA数据
    av_image_alloc(dst_data, dst_lineSize, codecContext->width, codecContext->height,
                   AV_PIX_FMT_RGBA, 1);

    LOGE("开始格式转换：YUV --> RGBA");
    SwsContext *sws_ctx = sws_getContext(
            //输入
            codecContext->width,
            codecContext->height,
            codecContext->pix_fmt,

            //输出
            codecContext->width,
            codecContext->height,
            AV_PIX_FMT_RGBA,
            SWS_BILINEAR, nullptr, nullptr, nullptr
    );

    while (isPlaying) {
        int ret = frames.getQueueAndDel(frame);

        if (!isPlaying) {
            break;
        }

        if (!ret) {
            continue;
        }

        //格式转换 yuv-> rgba
        sws_scale(
                sws_ctx,
                //输入，YUV数据
                frame->data,
                frame->linesize,
                0,
                codecContext->height,
                //输出 RGBA数据
                dst_data,
                dst_lineSize
        );

        //音视频同步 fps间隔时间加入
        double extra_delay = frame->repeat_pict / (2 * fps); //额外延迟时间
        double fps_delay = 1.0 / fps;
        double real_delay = extra_delay + fps_delay; //当前帧的延迟时间

        //av_usleep(real_delay * 1000000); //fps间隔时间

        double video_time = frame->best_effort_timestamp * av_q2d(time_base);
        double audio_time = audioChannel->audio_time;

        //判断音频和视频时间插值
        double time_diff = video_time - audio_time;
        LOGE("视频时间：%f", video_time);
        LOGE("音频时间：%f", audio_time);
        LOGE("音视频时间差：%f", time_diff);

        if (time_diff > 0) {
            // 视频时间 > 音频时间： 要等音频，所以控制视频播放慢一点（等音频） 【睡眠】
            if (time_diff > 1)
            {   // 说明：音频预视频插件很大，TODO 拖动条 特色场景  音频 和 视频 差值很大，我不能睡眠那么久，否则是大Bug
                // av_usleep((real_delay + time_diff) * 1000000);

                // 如果 音频 和 视频 差值很大，我不会睡很久，我就是稍微睡一下
                av_usleep((real_delay * 2) * 1000000);
            }
            else
            {   // 说明：0~1之间：音频与视频差距不大，所以可以那（当前帧实际延时时间 + 音视频差值）
                av_usleep((real_delay + time_diff) * 1000000); // 单位是微妙：所以 * 1000000
            }
        } if (time_diff < 0) {
            // 视频时间 < 音频时间： 要追音频，所以控制视频播放快一点（追音频） 【丢包】
            // 丢帧：不能睡意丢，I帧是绝对不能丢
            // 丢包：在frames 和 packets 中的队列

            // 经验值 0.05
            // -0.234454   fabs == 0.234454
            if (fabs(time_diff) <= 0.05) { // fabs对负数的操作（对浮点数取绝对值）
                // 多线程（安全 同步丢包）
                frames.sync();
                continue; // 丢完取下一个包
            }
        } else {
            // 百分百同步，这个基本上很难做的
            LOGI("百分百同步了");
        }


        //渲染工作
        renderCallback(dst_data[0], codecContext->width, codecContext->height, dst_lineSize[0]);
        av_frame_unref(frame);
        releaseAVFrame(&frame);
    }

    av_frame_unref(frame);
    releaseAVFrame(&frame);
    isPlaying = false;
    av_free(&dst_data[0]);
    sws_freeContext(sws_ctx);
}

void VideoChannel::setAudioChannel(AudioChannel *audio_channel) {
    this->audioChannel = audio_channel;
}
