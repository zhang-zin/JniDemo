package com.zj.opengl.filter

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20.*
import com.zj.opengl.R
import com.zj.opengl.utils.genTextures

class CameraFilter(context: Context) :
    BaseFilter(context, R.raw.camera_vertex, R.raw.camera_fragment) {

    private val frameBuffers = IntArray(1) // FBO帧缓冲区
    private val frameBufferTextures = IntArray(1) // fbo的纹理ID（和直接显示不同），最终是要返回

    override fun onReady(width: Int, height: Int) {
        super.onReady(width, height)
        //1、创建FBO（看不见的离屏屏幕）
        /**
         * 实例化创建帧缓冲区，FBO缓冲区
         * 参数1：int n, fbo 个数
         * 参数2：int[] framebuffers, 用来保存 fbo id 的数组
         * 参数3：int offset 从数组中第几个id来保存,从零下标开始
         */
        glGenFramebuffers(frameBuffers.size, frameBuffers, 0)

        //2、创建属于fbo的纹理
        genTextures(frameBufferTextures)

        //3、绑定fpo缓冲区和fbo纹理
        glBindTexture(GL_TEXTURE_2D, frameBufferTextures[0])
        //生产2d纹理图像
        /**
         * int target,         要绑定的纹理目标
         * int level,          level一般都是0
         * int internalformat, 纹理图像内部处理的格式是什么，rgba
         * int width,          宽
         * int height,         高
         * int border,         边界
         * int format,         纹理图像格式是什么，rgba
         * int type,           无符号字节的类型
         * java.nio.Buffer pixels
         */
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null)

        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffers[0])
        /**
         * int target,     fbo的纹理目标
         * int attachment, 附属到哪里
         * int textarget,  要绑定的纹理目标
         * int texture,    纹理
         * int level       level一般都是0
         */
        glFramebufferTexture2D(
            GL_FRAMEBUFFER,
            GL_COLOR_ATTACHMENT0,
            GL_TEXTURE_2D,
            frameBufferTextures.get(0),
            0
        )

        //4、解绑操作
        glBindTexture(GL_TEXTURE_2D, 0)
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    override fun onDrawFrame(textureId: Int, matrix: FloatArray): Int {
        glViewport(0, 0, mWidth, mHeight) //设置视窗大小

        //渲染到FBO离线缓存中
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffers[0])
        glUseProgram(mProgramId)

        //绘制
        vertexBuffer.position(0)
        glVertexAttribPointer(vPosition, 2, GL_FLOAT, false, 0, vertexBuffer)
        glEnableVertexAttribArray(vPosition)

        textureBuffer.position(0)
        glVertexAttribPointer(vCoord, 2, GL_FLOAT, false, 0, textureBuffer)
        glEnableVertexAttribArray(vCoord)

        //变换矩阵
        glUniformMatrix4fv(vMatrix, 1, false, matrix, 0)

        //片元
        glActiveTexture(GL_TEXTURE0) //激活图层
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        glUniform1i(vTexture, 0)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4) // 通知 opengl 绘制
        // 解绑 fbo
        glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        glBindFramebuffer(GL_FRAMEBUFFER, 0)

        // FBO的纹理ID，返回了
        return frameBufferTextures.get(0)
    }
}