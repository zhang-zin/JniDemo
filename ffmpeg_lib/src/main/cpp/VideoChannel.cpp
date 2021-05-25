
#include "VideoChannel.h"

VideoChannel::VideoChannel(int stream_index, AVCodecContext *codecContext) : BaseChannel(
        stream_index, codecContext) {

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
        int ret = packets.getQueueAndDel(packet); //阻塞式
        if (!isPlaying) {
            break;
        }

        if (!ret) {
            continue;
        }

        ret = avcodec_send_packet(codecContext, packet); //发送AVPacket压缩包到缓冲区
        releaseAVPacket(&packet); //FFmpeg 缓存了一份AVPacket
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
            break;
        }
        //拿到了原始包
        frames.insertToQueue(frame);
    }
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

        //渲染工作
        renderCallback(dst_data[0], codecContext->width, codecContext->height, dst_lineSize[0]);
        releaseAVFrame(&frame);
    }

    releaseAVFrame(&frame);
    isPlaying = false;
    av_free(&dst_data[0]);
    sws_freeContext(sws_ctx);
}
