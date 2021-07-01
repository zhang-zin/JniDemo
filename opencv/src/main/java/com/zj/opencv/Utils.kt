package com.zj.opencv

import android.content.Context
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import java.io.File
import java.io.FileOutputStream

object Utils {

    var data: ByteArray? = null
    var rowData: ByteArray? = null

    fun getDataFormImage(image: ImageProxy): ByteArray {
        val width = image.cropRect.width()
        val height = image.cropRect.height()
        val planes = image.planes

        val size = width * height * ImageFormat.getBitsPerPixel(image.format) / 8
        if (data == null || data?.size != size) {
            data = ByteArray(size)
        }
        if (rowData == null || rowData?.size != planes[0].rowStride) {
            rowData = ByteArray(planes[0].rowStride)
        }
        var channelOffset = 0 //偏移
        for (i in planes.indices) {
            when (i) {
                0 -> channelOffset = 0 // y 从0开始
                1 -> channelOffset = width * height // u 开始
                2 -> channelOffset = (width * height * 1.25).toInt() // v开始 w*h+ w*h/4（u数据长度）
            }
            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride //行跨度 每行的数据量
            val pixelStride = planes[i].pixelStride // 像素跨度 ,uv的存储间隔
            val shift = if (i == 0) 0 else 1
            val w = width shr shift // u与v只有一半
            val h = height shr shift
            buffer.position(rowStride * (image.cropRect.top shr shift) + pixelStride * (image.cropRect.left shr shift))
            var length: Int
            for (row in 0 until h) {
                if (pixelStride == 1) {
                    length = w
                    buffer[data, channelOffset, length]
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer[rowData, 0, length]
                    for (col in 0 until w) {
                        data!![channelOffset++] = rowData!![col * pixelStride]
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
        return data!!

    }

    /**
     * assets文件夹的文件拷贝到私有目录
     */
    fun copyAssest2Dir(context: Context, name: String): String {
        val cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE)
        val cascadeFile = File(cascadeDir, name)
        if (!cascadeFile.exists()) {
            val open = context.assets.open(name)
            val fileOutputStream = FileOutputStream(cascadeFile)
            try {
                var len: Int;
                val buffer = ByteArray(2048)
                while (open.read(buffer).also { len = it } > 0) {
                    fileOutputStream.write(buffer, 0, len)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                open.close()
                fileOutputStream.close()
            }

        }
        return cascadeFile.absolutePath
    }
}