package com.zj.jnidemo

import android.Manifest
import android.content.Intent
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.*
import java.lang.Exception

/**
 * 进行录音和播放
 * 音频数据的承载信号，脉冲编码调制，PCM
 * 模拟信号->采样->量化->编码->数字信号
 */
class AudioTestActivity : AppCompatActivity() {

    lateinit var audioRecord: AudioRecord
    lateinit var pcmToWavUtil: PcmToWavUtil
    lateinit var pcmFile: File
    lateinit var wavFile: File
    var minBufferSize: Int = 0
    var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_test)
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO),
                100)

        minBufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT) * 2
        Log.e("zhang", "minBufferSize: $minBufferSize")
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize)
        pcmToWavUtil = PcmToWavUtil(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)

        val parentPath = getExternalFilesDir("")?.absolutePath
                ?: Environment.getExternalStorageDirectory().absolutePath
        val parent = File("$parentPath/testPCM")
        if (!parent.exists()) {
            parent.mkdirs()
        }
        pcmFile = File(parent, "test.pcm")
        wavFile = File(parent, "test.wav")

        findViewById<Button>(R.id.bt_record).setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRecord()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    stopRecord()
                }
            }
            true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecord.release()
    }

    private fun startRecord() {
        isRecording = true
        HiExecutor.execute(runnable = AudioTask())
    }

    private fun stopRecord() {
        isRecording = false
    }

    inner class AudioTask : Runnable {
        override fun run() {
            var fos: FileOutputStream? = null
            try {

                fos = FileOutputStream(pcmFile)

                audioRecord.startRecording()
                val byteArray = ByteArray(minBufferSize)
                while (isRecording) {
                    val read = audioRecord.read(byteArray, 0, byteArray.size)
                    Log.e("zhang", "写入文件: " + byteArray.size)
                    if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                        fos.write(byteArray, 0, read)
                    }
                }
                pcmToWavUtil.pcmToWav(pcmFile.absolutePath, wavFile.absolutePath)
                audioRecord.stop()
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } finally {
                fos?.close()
                audioRecord.stop()
            }
        }
    }

    /**
     * pcm -> wav
     * 需要在pcm的文件起始位置加上至少44个字节的WAV头信息
     */
    fun startPlayWAV(view: View) {

        if (wavFile.exists()) {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            val uri = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(this, "com.zj.jnidemo.fileprovider", wavFile)
            } else {
                Uri.fromFile(wavFile.absoluteFile)
            }
            intent.setDataAndType(uri, "audio");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);
        } else {
            Toast.makeText(this, "请先录制", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 播放PCM音频(STREAM模式)
     * 流模式：当使用write()方法时，会向AudioTrack写入连续的数据流，数据流从Java层传输到底层，并派队阻塞等待播放；
     * 在播放音频快数据时，流模式比较好用
     * 音频数据过大过长，无法存入内存时
     * 由于音频数据的特性(高采样率，每采样位…)，太大而无法装入内存。
     * 接收或生成时，先前排队的音频正在播放。
     */
    fun startStreamPCM(view: View) {
        /**
         * 设置音频属性
         * 1、设置支持多媒体属性，比如audio、video
         * 2、设置音频格式
         */
        val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

        /**
         * 设置音频格式
         * 1、设置采样率
         * 2、设置采样位数
         * 3、设置声道
         */
        val format = AudioFormat.Builder()
                .setSampleRate(44100)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                .build()
        val audioTrack = AudioTrack(
                attributes,
                format,
                minBufferSize,
                AudioTrack.MODE_STREAM,  //设置为流模式
                AudioManager.AUDIO_SESSION_ID_GENERATE //音频识别id
        )
        audioTrack.play()
        HiExecutor.execute(runnable = {
            val ins = FileInputStream(pcmFile)
            val baos = ByteArrayOutputStream()
            try {
                val buffer = ByteArray(1024)
                var len: Int
                while ((ins.read(buffer).also { len = it } > 0)) {
                    audioTrack.write(buffer, 0, len)
                }
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                baos.close()
                ins.close()
            }
        })
    }

    /**
     * 播放PCM音频(STATIC模式)
     * 静态模式，它需要一次性把数据写到buffer中，适合小音频，小延迟的音频播放，常用在UI和游戏中比较实用。
     */
    fun startStaticPCM(view: View) {
        if (!pcmFile.exists()) {
            return
        }
        val ins = FileInputStream(pcmFile)
        val baos = ByteArrayOutputStream()
        try {
            val buffer = ByteArray(1024)
            var len: Int
            while ((ins.read(buffer).also { len = it } > 0)) {
                //把数据存到ByteArrayOutputStream中
                baos.write(buffer, 0, len)
            }
            //拿到音频数据
            val pcmData = baos.toByteArray()

            /**
             * 设置音频属性
             * 1、设置支持多媒体属性，比如audio、video
             * 2、设置音频格式
             */
            val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()

            /**
             * 设置音频格式
             * 1、设置采样率
             * 2、设置采样位数
             * 3、设置声道
             */
            val format = AudioFormat.Builder()
                    .setSampleRate(44100)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .build()
            val audioTrack = AudioTrack(
                    attributes,
                    format,
                    pcmData.size,
                    AudioTrack.MODE_STATIC,  //设置为静态模式
                    AudioManager.AUDIO_SESSION_ID_GENERATE //音频识别id
            )
            audioTrack.write(pcmData, 0, pcmData.size)
            audioTrack.play()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            baos.close()
            ins.close()
        }
    }
}