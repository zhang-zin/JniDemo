//通用显示

//修饰符
//attribute 属性变量。只能用于顶点着色器中，一般用改变量表示一些顶点数据，如：顶点坐标、纹理坐标、颜色等
//uniforms  一致变量。在着色器执行期间一致变量的值是不变的。与const常量不同的是，这个值在编译时期是未知的是由着色器外部初始化的。
//varying   易变变量。是从顶点着色器传递到片元着色器的数据变量

//内置函数
//texture2D(采样器，坐标) 采样指定位置的纹理

//内置变量
//gl_Position  vec4类型，表示顶点着色器的顶点位置
//gl_FragColor vec4类型，表示片元着色器中的颜色

attribute vec4 vPosition; // 顶点坐标，相当于：相机的四个点位置排版

attribute vec2 vCoord; // 纹理坐标，用来图形上色的

varying vec2 aCoord; // 把这个最终的计算成果，给片元着色器 【不需要Java传递，他是计算出来的】

void main() {
    gl_Position = vPosition; // 确定好位置排版   gl_Position OpenGL着色器语言内置的变量

    // 着色器语言基础语法
    // aCoord = vCoord.xy;
    // aCoord是2个分量的    vCoord是四个分量的.xy取出两个分量
    aCoord = vCoord;
}
