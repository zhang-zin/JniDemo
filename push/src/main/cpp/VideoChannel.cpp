#include "VideoChannel.h"

VideoChannel::VideoChannel() {
    pthread_mutex_init(&mutex, nullptr);
}

VideoChannel::~VideoChannel() {
    pthread_mutex_destroy(&mutex);
}

/**
 * 初始化x264编码器
 * @param width
 * @param height
 * @param fps
 * @param bitrate
 */
void VideoChannel::initVideoEncoder(int width, int height, int fps, int bitrate) {
    pthread_mutex_lock(&mutex);

    // 4:2:0采样格式
    y_len = width * height;  // y分量长度
    uv_len = y_len / 4;      // u、v分量长度

    if (videoEncoder) {
        x264_encoder_close(videoEncoder);
        videoEncoder = nullptr;
    }
    if (pic_in) {
        x264_picture_clean(pic_in);
        delete pic_in;
    }

    // 初始化x264编码器参数
    x264_param_t param;
    /**
     * 设置编码器属性
     * ultrafast：最快
     * zerolatency：零延迟
     */
    x264_param_default_preset(&param, "ultrafast", "zerolatency");
    param.i_level_idc = 32;  // 编码规格 https://wikipedia.tw.wjbk.site/wiki/H.264
    param.i_csp = X264_CSP_I420; // 输入数据格式
    param.i_width = width;
    param.i_height = height;

    // b帧需要前后参数影响编解码效率，不需要b帧
    param.i_bframe = 0;
    // 码率控制方式：CQP(恒定质量)，CRF(恒定码率)，ABR(平均码率)
    param.rc.i_rc_method = X264_RC_CRF;
    // 设置码率
    param.rc.i_bitrate = bitrate / 1000;
    // 瞬时最大码率
    param.rc.i_vbv_max_bitrate = bitrate / 1000 * 1.2;
    // 设置了i_vbv_max_bitrate就必须设置buffer大小，码率控制区大小，单位Kb/s
    param.rc.i_vbv_buffer_size = bitrate / 1000;
    // 码率控制不是通过 timebase 和 timestamp，而是通过 fps 来控制 码率
    param.b_vfr_input = 0;

    // 帧率分母
    param.i_fps_num = fps;
    // 帧率分子
    param.i_fps_den = 1;
    param.i_timebase_num = param.i_fps_num;
    param.i_timebase_den = param.i_fps_den;
    // 计算关键帧距离
    param.i_keyint_max = fps * 2;
    // sps序列参数 是否复制sps和pps放在每个关键帧的前面 该参数设置是让每个关键帧(I帧)都附带sps/pps
    param.b_repeat_headers = 1;
    // 并行编码线程数
    param.i_threads = 1;
    // profile级别，baseline级别 (把我们上面的参数进行提交)
    x264_param_apply_profile(&param, "baseline");

    //输入图像初始化
    pic_in = new x264_picture_t;
    x264_picture_alloc(pic_in, param.i_csp, param.i_width, param.i_height);

    videoEncoder = x264_encoder_open(&param);
    if (videoEncoder) {
        LOGE("x264编码器打开成功");
    }

    pthread_mutex_unlock(&mutex);
}

void VideoChannel::setVideoCallback(VideoCallback videoCallback) {
    this->videoCallback = videoCallback;
}

/**
 * 编码，Android摄像头是NV21的YUV420SP格式，先存储Y分量，再VU交替存储
 * x264编码是YUV420P的格式，先存储Y分量，再存储所有的U分量或者V分量。
 * 所以需要将摄像头的数据转换
 * @param data 摄像头的YUV数据
 */
void VideoChannel::encodeData(signed char *data) {
    pthread_mutex_lock(&mutex);

    // 把nv21的y分量拷贝到i420的y分量
    memcpy(pic_in->img.plane[0], data, y_len);

    //todo libyuv旋转

    // 拷贝uv分量
    for (int i = 0; i < uv_len; ++i) {
        // u分量
        *(pic_in->img.plane[1] + i) = *(data + y_len + i * 2 + 1);
        // v分量
        *(pic_in->img.plane[2] + i) = *(data + y_len + i * 2);
    }

    x264_nal_t *nal = nullptr; // 通过H.264编码得到NAL数组
    int pi_nal; // pi_nal是nal中输出的NAL单元的数量
    x264_picture_t pic_out; // 输出编码之后的图片

    /*
     * videoEncoder：编码器
     * &nal：NAL数组
     * &pi_nal：pi_nal是nal中输
     * pic_in：输入原始的图片
     * &pic_out：输出编码后图片
     */
    int ret = x264_encoder_encode(videoEncoder, &nal, &pi_nal, pic_in, &pic_out);
    if (ret < 0) {
        LOGE("x264编码失败");
        pthread_mutex_unlock(&mutex);
        return;
    }

    // 发送Packet到队列
    // sps(序列参数集) pps(图像参数集) 说白了就是：告诉我们如何解码图像数据
    int sps_len, pps_len; // sps 和 pps 的长度
    uint8_t sps[100]; // 用于接收 sps 的数组定义
    uint8_t pps[100]; // 用于接收 pps 的数组定义
    pic_in->i_pts += 1; // pts显示的时间（+=1 目的是每次都累加下去）， dts编码的时间

    for (int i = 0; i < pi_nal; ++i) {
        if (nal[i].i_type == NAL_SPS) {
            sps_len = nal[i].i_payload - 4; // 去掉起始码（之前我们学过的内容：00 00 00 01）
            memcpy(sps, nal[i].p_payload + 4, sps_len); // 由于上面减了4，所以+4挪动这里的位置开始
        } else if (nal[i].i_type == NAL_PPS) {
            pps_len = nal[i].i_payload - 4; // 去掉起始码 之前我们学过的内容：00 00 00 01）
            memcpy(pps, nal[i].p_payload + 4, pps_len); // 由于上面减了4，所以+4挪动这里的位置开始
            // sps + pps
            sendSpsPps(sps, pps, sps_len, pps_len); // pps是跟在sps后面的，这里拿到的pps表示前面的sps肯定拿到了
        } else {
            // 发送 I帧 P帧
            sendFrame(nal[i].i_type, nal[i].i_payload, nal[i].p_payload);
        }
    }
    pthread_mutex_unlock(&mutex);
}

/**
 * 发送sps和pps数据
 * @param sps
 * @param pps
 * @param sps_len
 * @param pps_len
 */
void VideoChannel::sendSpsPps(uint8_t *sps, uint8_t *pps, int sps_len, int pps_len) {
    //todo
    int body_size = 5 + 8 + sps_len + 3 + pps_len;
    RTMPPacket *packet = new RTMPPacket; // 开始封包
    RTMPPacket_Alloc(packet, body_size); // 堆区实例化

    int i = 0;
    packet->m_body[i++] = 0x17;
    packet->m_body[i++] = 0x00; // 1是帧类型，0是sps和pps
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    // sps和pps数据
    packet->m_body[i++] = 0x01; // 版本
    packet->m_body[i++] = sps[1];
    packet->m_body[i++] = sps[2];
    packet->m_body[i++] = sps[3];

    packet->m_body[i++] = 0xff;
    packet->m_body[i++] = 0xe1;

    // 两个字节表示一个sps长度
    packet->m_body[i++] = (sps_len >> 8) & 0xff; // 取高8位
    packet->m_body[i++] = sps_len & 0xff;        // 取低8位
    // 拷贝sps数据
    memcpy(&packet->m_body[i], sps, sps_len);

    i += sps_len;
    packet->m_body[i++] = 0x01; // pps个数
    // 两个字节表示一个pps长度
    packet->m_body[i++] = (pps_len >> 8) & 0xff; // 取高8位
    packet->m_body[i++] = pps_len & 0xff;        // 取低8位
    // 拷贝pps数据
    memcpy(&packet->m_body[i], pps, pps_len);

    i += pps_len;

    // 封包
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO; //包类型
    packet->m_nBodySize = body_size; // sps + pps总大小
    packet->m_nChannel = 10; // 通道ID
    packet->m_nTimeStamp = 0; // sps pps 包 没有时间戳
    packet->m_hasAbsTimestamp = 0; // 时间戳绝对或相对 也没有时间搓
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM; // 包的类型  中等大小的包
    LOGE("发送sps和pps数据");
    videoCallback(packet);
}

/**
 * 发送帧信息
 * @param type 帧类型
 * @param payload 帧数据长度
 * @param pPayload 帧数据
 */
void VideoChannel::sendFrame(int type, int payload, uint8_t *pPayload) {
    // 去掉起始码 00 00 00 01 或者 00 00 01
    if (pPayload[2] == 0x00) { // 00 00 00 01
        pPayload += 4; // 例如：共10个，挪动4个后，还剩6个
        // 保证 我们的长度是和上的数据对应，也要是6个，所以-= 4
        payload -= 4;
    } else if (pPayload[2] == 0x01) { // 00 00 01
        pPayload += 3; // 例如：共10个，挪动3个后，还剩7个
        // 保证 我们的长度是和上的数据对应，也要是7个，所以-= 3
        payload -= 3;
    }

    int body_size = 5 + 4 + payload;

    RTMPPacket *packet = new RTMPPacket;
    RTMPPacket_Alloc(packet, body_size);

    // 区分关键帧 和 非关键帧
    packet->m_body[0] = 0x27; // 普通帧 非关键帧
    if (type == NAL_SLICE_IDR) {
        packet->m_body[0] = 0x17; // 关键帧
    }

    packet->m_body[1] = 0x01;
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;

    // 四个字节表达一个长度，需要位移
    // 用四个字节来表达 payload帧数据的长度，所以就需要位运算
    packet->m_body[5] = (payload >> 24) & 0xFF;
    packet->m_body[6] = (payload >> 16) & 0xFF;
    packet->m_body[7] = (payload >> 8) & 0xFF;
    packet->m_body[8] = payload & 0xFF;

    memcpy(&packet->m_body[9], pPayload, payload); // 拷贝H264的裸数据

    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO; // 包类型，是视频类型
    packet->m_nBodySize = body_size; // 设置好 关键帧 或 普通帧 的总大小
    packet->m_nChannel = 10; // 通道ID，随便写一个，注意：不要写的和rtmp.c(里面的m_nChannel有冲突 4301行)
    packet->m_nTimeStamp = -1; // 帧数据有时间戳
    packet->m_hasAbsTimestamp = 0; // 时间戳绝对或相对 用不到，不需要
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE; // 包的类型：若是关键帧的话，数据量比较大，所以设置大包
    LOGE("发送帧信息");
    videoCallback(packet);
}

