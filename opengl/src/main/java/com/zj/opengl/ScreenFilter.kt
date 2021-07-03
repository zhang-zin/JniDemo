package com.zj.opengl

import android.content.Context
import android.content.res.Resources
import android.opengl.GLES11Ext
import android.opengl.GLES20.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer


/**
 * 显示到GLSurfaceView
 *
 */
class ScreenFilter(private val context: Context) {

    private val mProgram: Int // 着色器程序ID

    private val vPosition: Int // 顶点着色器：位置
    private val vCoord: Int    // 顶点着色器：纹理
    private val vMatrix: Int   // 顶点着色器：矩阵
    private val vTexture: Int  // 片元着色器：采样器 摄像头数据

    private val mVertexBuffer: FloatBuffer // 顶点坐标 nio的buffer缓存
    private val mTexturBuffer: FloatBuffer // 纹理坐标 nio的buffer缓存

    private var mWidth = 0 // 宽度
    private var mHeight = 0 // 高度

    init {
        // 查找到（顶点着色器）的代码字符串
        val vertexSource: String = readTextFileFromResource(R.raw.camera_vertex)
        // 查找到（片元着色器）的代码字符串
        val fragmentSource: String = readTextFileFromResource(R.raw.camera_fragment)

        //1.配置顶点着色器
        //1.1 创建顶点着色器
        val vShaderId = glCreateShader(GL_VERTEX_SHADER)
        //1.2 绑定顶点着色器
        glShaderSource(vShaderId, vertexSource)
        //1.3 编译着色器代码（编译成功就会拿到顶点着色器id）
        glCompileShader(vShaderId)
        val status = IntArray(1)
        glGetShaderiv(vShaderId, GL_COMPILE_STATUS, status, 0)
        check(status[0] == GL_TRUE) { "顶点着色器配置失败！" }

        //2. 配置片元着色器
        //2.1 创建片元着色器
        val fShaderId = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fShaderId, fragmentSource)
        glCompileShader(fShaderId)
        glGetShaderiv(fShaderId, GL_COMPILE_STATUS, status, 0)
        check(status[0] == GL_TRUE) { "片元着色器配置失败！" }

        //3. 配置着色器程序
        //3.1 创建一个着色器程序
        mProgram = glCreateProgram()
        //3.2 将前面配置的顶点和片元着色器附加到新的程序上
        glAttachShader(mProgram, vShaderId)
        glAttachShader(mProgram, fShaderId)
        //3.3 链接着色器
        glLinkProgram(mProgram)
        glGetShaderiv(mProgram, GL_LINK_STATUS, status, 0)
        check(status[0] == GL_TRUE) { "着色器程序链接失败！" }

        //4. 释放着色器
        glDeleteShader(vShaderId)
        glDeleteShader(fShaderId)

        //获取变量的索引值
        //顶点着色器
        vPosition = glGetAttribLocation(mProgram, "vPosition")
        vCoord = glGetAttribLocation(mProgram, "vCoord")
        vMatrix = glGetUniformLocation(mProgram, "vMatrix")

        //片元着色器
        vTexture = glGetUniformLocation(mProgram, "vTexture")

        //顶点坐标缓存
        mVertexBuffer = ByteBuffer.allocateDirect(4 * 2 * 4)
            .order(ByteOrder.nativeOrder()) // 使用本地字节序
            .asFloatBuffer()
        mVertexBuffer.clear()
        val v = floatArrayOf( // OpenGL世界坐标
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
        )
        mVertexBuffer.put(v)

        //纹理坐标缓存
        mTexturBuffer = ByteBuffer.allocateDirect(4 * 2 * 4)
            .order(ByteOrder.nativeOrder()) // 使用本地字节序
            .asFloatBuffer()
        mTexturBuffer.clear()
        // 画面是颠倒的
        /*val t = floatArrayOf(
            // 屏幕坐标系
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
        )*/

        // 旋转 180度 就纠正了
        val t = floatArrayOf( // 屏幕坐标系
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        )
        mTexturBuffer.put(t)
    }

    fun onReady(width: Int, height: Int) {
        mWidth = width
        mHeight = height
    }

    /**
     * 绘制操作
     * [textureID] 画布 纹理ID
     * [mtx] 矩阵数据
     */
    fun onDrawFrame(textureID: Int, mtx: FloatArray) {
        //设置视窗大小
        glViewport(0, 0, mWidth, mHeight)
        glUseProgram(mProgram) //执行着色器程序

        //顶点坐标赋值
        mVertexBuffer.position(0) //每次使用时，从零开始

        /**
         * 传值，把mVertexBuffer传递到vPosition
         * vPosition：着色器代码里面的标记变量
         * 2：xy
         * false：
         * 0：跳步
         */
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, mVertexBuffer)
        glEnableVertexAttribArray(vPosition) //激活

        //纹理坐标赋值
        mTexturBuffer.position(0)
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, mTexturBuffer)
        glEnableVertexAttribArray(vCoord)

        // 变换矩阵 把mtx矩阵数据 传递到 vMatrix
        glUniformMatrix4fv(vMatrix, 1, false, mtx, 0)

        // 激活图层
        glActiveTexture(GL_TEXTURE0)
        // 绑定纹理ID --- glBindTexture(GL_TEXTURE_2D ,textureId); 如果在片元着色器中的vTexture，不是samplerExternalOES类型，就可以这样写
        // 由于我们的着色器代码是 使用了 samplerExternalOES
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID)

        glUniform1i(vTexture, 0) // 传递参数 给 片元着色器：采样器

        // 通知 opengl 绘制 ，从0开始，共四个点绘制
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
    }

    private fun readTextFileFromResource(resourceId: Int): String {
        val body = StringBuilder()

        var inputStream: InputStream? = null
        var inputStreamReader: InputStreamReader? = null
        var reader: BufferedReader? = null
        try {
            inputStream = context.resources.openRawResource(resourceId)
            inputStreamReader = InputStreamReader(inputStream)
            reader = BufferedReader(inputStreamReader)
            var nextLine: String?
            while (reader.readLine().also { nextLine = it } != null) {
                body.appendLine(nextLine)
            }
        } catch (e: IOException) {
            throw RuntimeException("Could not open resource: $resourceId", e)
        } catch (nfe: Resources.NotFoundException) {
            throw java.lang.RuntimeException("Resource not found: $resourceId", nfe)
        } finally {
            inputStream?.close()
            inputStreamReader?.close()
            reader?.close()
        }

        return body.toString()
    }
}