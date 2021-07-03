//片元着色器
#extension GL_OES_EGL_image_external : require

//float数据的精度 precision low：低精度；precision mediump：中精度，precision highp：高精度
precision mediump float;

//采集相机的数据
uniform samplerExternalOES vTexture;

varying vec2 aCoord;//最终的计算结果

void main(){
    //texture2D(采样器，坐标)
    //gl_FragColor = texture2D(vTexture, aCoord);

    // 305911公式：黑白电视效果，其实原理就是提取出Y分量
    /*vec4 rgba =texture2D(vTexture, aCoord);
    float gray = (0.30 * rgba.r   + 0.59 * rgba.g + 0.11* rgba.b); // 其实原理就是提取出Y分量 ,就是黑白电视
    gl_FragColor = vec4(gray, gray, gray, 1.0);*/

    // 在网上Copy的公式：底片效果
    vec4 rgba = texture2D(vTexture,aCoord);  // rgba
    gl_FragColor = vec4(1.-rgba.r, 1.-rgba.g, 1.-rgba.b, rgba.a);
}