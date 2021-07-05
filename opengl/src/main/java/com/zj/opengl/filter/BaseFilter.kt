package com.zj.opengl.filter

import android.content.Context
import android.opengl.GLES20.*
import com.zj.opengl.utils.*
import java.nio.FloatBuffer

open class BaseFilter(context: Context, vertexSourceId: Int, fragmentSourceId: Int) {

    protected val vertexBuffer: FloatBuffer   //顶点坐标数据缓冲区
    protected val textureBuffer: FloatBuffer  //纹理坐标数据缓冲区

    protected val mProgramId: Int //着色器程序
    protected val vPosition: Int  //顶点着色器：顶点位置
    protected val vCoord: Int     //顶点着色器：纹理坐标
    protected val vMatrix: Int    //顶点着色器：变换矩阵
    protected val vTexture: Int   //片元着色器：采样器

    protected var mWidth: Int = 0
    protected var mHeight: Int = 0

    init {
        // OpenGL世界坐标
        val vertex = floatArrayOf(
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f
        )
        vertexBuffer = BufferHelper.getFloatBuffer(vertex)

        // 画面是颠倒的
        /*val t = floatArrayOf(
            // 屏幕坐标系
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
        )*/
        // 旋转 180度 就纠正了
        // 屏幕坐标系
        val texture = floatArrayOf(
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f
        )
        textureBuffer = BufferHelper.getFloatBuffer(texture)

        // 查找到（顶点着色器）的代码字符串
        val vertexSource = readTextFileFormResource(context, vertexSourceId)
        // 查找到（片元着色器）的代码字符串
        val fragmentSource = readTextFileFormResource(context, fragmentSourceId)

        //编译顶点着色器代码字符串
        val vertexShaderId = compileVertexShader(vertexSource)
        //编译片元着色器代码字符串
        val fragmentShaderId = compileFragmentShader(fragmentSource)
        mProgramId = if (vertexShaderId != 0 && fragmentShaderId != 0)
            linkProgram(vertexShaderId, fragmentShaderId)
        else 0

        // 删除 顶点 片元 着色器ID
        glDeleteShader(vertexShaderId)
        glDeleteShader(fragmentShaderId)

        // 顶点着色器：的索引值
        vPosition = glGetAttribLocation(mProgramId, "vPosition")
        // 顶点着色器：纹理坐标，采样器采样图片的坐标 的索引值
        vCoord = glGetAttribLocation(mProgramId, "vCoord")
        // 顶点着色器：变换矩阵 的索引值
        vMatrix = glGetUniformLocation(mProgramId, "vMatrix")

        // 片元着色器：采样器
        vTexture = glGetUniformLocation(mProgramId, "vTexture")
    }

    /**
     * 更新宽高信息
     */
    open fun onReady(width: Int, height: Int) {
        mWidth = width
        mHeight = height
    }

    /**
     * 绘制
     * [textureId] 纹理Id
     */
    open fun onDrawFrame(textureId: Int, mtx: FloatArray): Int {
        //设置视窗大小
        glViewport(0, 0, mWidth, mHeight)
        glUseProgram(mProgramId)

        //顶点坐标赋值
        vertexBuffer.position(0)
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, vertexBuffer)
        glEnableVertexAttribArray(vPosition)

        //纹理坐标赋值
        textureBuffer.position(0) // 传值
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, textureBuffer)
        glEnableVertexAttribArray(vCoord)// 激活

        glActiveTexture(GL_TEXTURE) //激活图层
        glBindTexture(GL_TEXTURE_2D, textureId)

        glUniform1i(vTexture, 0) // 传递采样器
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4) // 通知 opengl 绘制

        return textureId
    }

}