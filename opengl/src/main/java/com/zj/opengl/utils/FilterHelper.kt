package com.zj.opengl.utils

import android.content.Context
import android.content.res.Resources
import android.opengl.GLES20.*
import android.util.Log
import com.zj.opengl.BuildConfig
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

private const val TAG = "ShaderHelper"

/**
 * 读取GLSL文件中的着色器代码
 * [resourceId] 资源Id
 * return: 着色器字符串代码
 */
fun readTextFileFormResource(context: Context, resourceId: Int): String {
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

/**
 * 加载并编译顶点着色器代码
 * [shaderCode]: 顶点着色器glsl代码
 * return:顶点着色器id（返回0表示链接失败）
 */
fun compileVertexShader(shaderCode: String): Int {
    return compileShader(GL_VERTEX_SHADER, shaderCode)
}

/**
 * 加载并编译片元着色器
 * [shaderCode]: 片元着色器代码
 * return:片元着色器（返回0表示链接失败）
 */
fun compileFragmentShader(shaderCode: String): Int {
    return compileShader(GL_FRAGMENT_SHADER, shaderCode)
}

fun compileShader(type: Int, shaderCode: String): Int {
    //1.配置顶点着色器
    //1.1 创建顶点着色器
    val vShaderId = glCreateShader(type)
    if (vShaderId == 0) {
        if (BuildConfig.DEBUG) Log.w(TAG, "创建着色器失败")
        return 0
    }
    //1.2 绑定顶点着色器
    glShaderSource(vShaderId, shaderCode)
    //1.3 编译着色器代码（编译成功就会拿到顶点着色器id）
    glCompileShader(vShaderId)
    val status = IntArray(1)
    glGetShaderiv(vShaderId, GL_COMPILE_STATUS, status, 0)
    if (BuildConfig.DEBUG) Log.v(TAG, "编译着色器代码".trimIndent())
    if (BuildConfig.DEBUG) Log.i(TAG, shaderCode)

    if (status[0] == GL_FALSE) {
        if (BuildConfig.DEBUG) Log.w(
            TAG, " 着色器编译失败:" + type.toString().trimIndent()
        )
        if (BuildConfig.DEBUG) Log.e(TAG, glGetShaderInfoLog(vShaderId))
        // 如果失败，删除着色器对象
        glDeleteShader(vShaderId)
        return 0
    }

    return vShaderId
}

/**
 * 将顶点着色器和片元着色器一起链接到 OpenGL 程序中
 * [vertexShaderId] 顶点着色器id
 * [fragmentShaderId] 片元着色器id
 * return: OpenGL 程序id（返回0表示链接失败）
 */
fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
    // 创建一个新的OpenGL程序对象

    // 创建一个新的OpenGL程序对象
    val programObjectId = glCreateProgram()
    if (programObjectId == 0) {
        if (BuildConfig.DEBUG) Log.e(
            TAG,
            "创建程序失败"
        )
        return 0
    }
    // Attach 顶点着色器
    // Attach 顶点着色器
    glAttachShader(programObjectId, vertexShaderId)
    // Attach 片元着色器
    // Attach 片元着色器
    glAttachShader(programObjectId, fragmentShaderId)
    // 将两个着色器一起链接到OpenGL程序
    // 将两个着色器一起链接到OpenGL程序
    glLinkProgram(programObjectId)
    // 获取链接状态
    // 获取链接状态
    val linkStatus = IntArray(1)
    glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0)
    // 判断链接状态
    // 判断链接状态
    if (linkStatus[0] == GL_FALSE) {
        if (BuildConfig.DEBUG) Log.w(
            TAG,
            """
            链接程序失败:
            
            """.trimIndent()
        )
        if (BuildConfig.DEBUG) Log.e(
            TAG,
            glGetProgramInfoLog(programObjectId)
        )
        //如果失败，删除程序对象
        glDeleteProgram(programObjectId)
        return 0
    }
    return programObjectId
}

/**
 * 验证程序（开发过程可以调用）
 */
fun validateProgram(programObjectId: Int): Boolean {
    glValidateProgram(programObjectId)
    val validateStatus = IntArray(1)
    glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0)
    if (BuildConfig.DEBUG)
        Log.d(
            TAG,
            "验证程序状态：" + validateStatus[0] + "\n程序日志信息：" + glGetShaderInfoLog(programObjectId)
        )
    return validateStatus[0] != 0
}

/**
 * 生成并配置纹理
 */
fun genTextures(textures: IntArray) {
    glGenTextures(textures.size, textures, 0)
    for (texture in textures) {
        //1、绑定纹理
        glBindTexture(GL_TEXTURE_2D, texture)
        //2、配置纹理
        /**
         * [target] 纹理目标
         * [pname]  参数名
         * [param]  参数值
         */
        // GL_TEXTURE_MAG_FILTER 放大过滤（线性过滤）
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        // GL_TEXTURE_MIN_FILTER 缩小过滤（最近点过滤）
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)

        // GL_TEXTURE_WRAP_S GL_TEXTURE_WRAP_T分别为纹理的x, y 方向
        // GL_REPEAT 重复拉伸（平铺）
        // GL_CLAMP_TO_EDGE 截取拉伸（边缘拉伸）
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE) // 纹理中的S--X轴
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE) // 纹理中的T--Y轴

        // 解绑纹理（传0 表示与当前纹理解绑）
        glBindTexture(GL_TEXTURE_2D, 0)

    }
}

