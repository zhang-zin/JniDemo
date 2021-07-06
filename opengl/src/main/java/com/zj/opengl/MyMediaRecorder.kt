package com.zj.opengl

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.EGLContext
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.zj.opengl.record.MyEGL

class MyMediaRecorder(
    private val width: Int,
    private val height: Int,
    private val outputPath: String,         //输出路径
    private val eglContext: EGLContext,
    private val context: Context
) {

    private var mSpeed: Float = 0F

    // 1、编码器 MediaFormat.MIMETYPE_AUDIO_AAC: H.264
    private val mMediaCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
    private val mInputSurface: Surface
    private var mMediaMuxer: MediaMuxer? = null
    private val mHandler: Handler
    private var isStart = false
    private var myEGL: MyEGL? = null

    init {
        // 2、配置编码器参数
        //视频格式
        val videoFormat =
            MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_AUDIO_AAC, width, height)
        //设置码率
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, 1500_000)
        //帧率
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25)
        //颜色格式
        videoFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        //关键帧间隔
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 20)
        //配置编码器
        mMediaCodec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        // 3、创建输入surface(虚拟屏幕)
        mInputSurface = mMediaCodec.createInputSurface()

        val handleThread = HandlerThread("MyMediaRecorder")
        handleThread.start()
        val looper = handleThread.looper
        mHandler = Handler(looper)
    }

    fun start(speed: Float) {
        mSpeed = speed
        // 4、创建封装器
        mMediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        // 5、配置EGL环境
        mHandler.post {
            // 在一个新的Thread中，初始化EGL环境
            myEGL = MyEGL(eglContext, mInputSurface, context, width, height)
            mMediaCodec.start() //启动编码器
            isStart = true
        }
    }

    fun stop() {

    }

    fun encodeFrame(textureId: Int, timestamp: Long) {

    }
}