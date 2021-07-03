//顶点着色器
attribute vec4 vPosition;//顶点坐标

attribute vec4 vCoord;//纹理坐标

uniform mat4 vMatrix;//变换矩阵

varying vec2 aCoord;//最终的计算结果

void main(){
    //确定好位置
    gl_Position = vPosition;

    aCoord = (vMatrix * vCoord).xy;
}