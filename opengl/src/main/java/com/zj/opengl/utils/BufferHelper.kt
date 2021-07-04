package com.zj.opengl.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object BufferHelper {

    /**
     * 获取浮点类型缓冲数据
     */
    fun getFloatBuffer(vertexes: FloatArray): FloatBuffer {
        // 分配一块本地内存（不受 GC 管理）
        // 顶点坐标个数 * 坐标数据类型（float占4字节）
        val byteBuffer = ByteBuffer.allocateDirect(vertexes.size * 4)
        // 设置使用设备硬件的本地字节序（保证数据排序一致）
        byteBuffer.order(ByteOrder.nativeOrder())
        // 从ByteBuffer中创建一个浮点缓冲区
        val floatBuffer = byteBuffer.asFloatBuffer()
        // 写入坐标数组
        floatBuffer.put(vertexes)
        // 设置默认的读取位置，从第一个坐标开始
        floatBuffer.position(0)
        return floatBuffer
    }
}