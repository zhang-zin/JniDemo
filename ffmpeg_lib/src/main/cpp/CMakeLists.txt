
cmake_minimum_required(VERSION 3.10.2)

# 打印日志
message("当前CMake的路径是：${CMAKE_SOURCE_DIR}")
message("当前 CMAKE_ANDROID_ARCH_ABI 的路径是：${CMAKE_ANDROID_ARCH_ABI}")

set(FFMPEG ${CMAKE_SOURCE_DIR}/ffmpeg) # ffmpeg的路径
set(RTMP ${CMAKE_SOURCE_DIR}/rtmp) # rtmp的路径

include_directories(${FFMPEG}/include) # 导入ffmpeg的头文件

# 引入FFmpeg的库文件，设置内部的方式引入，指定库的目录是 -L  指定具体的库-l
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${FFMPEG}/libs/${CMAKE_ANDROID_ARCH_ABI}")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${RTMP}/libs/${CMAKE_ANDROID_ARCH_ABI}")  #rtmp库指定

file(GLOB allCpp *.cpp)

add_library(
        ffmpeg-lib
        SHARED
        ${allCpp})

find_library(
        log-lib
        log)

# 先把有依赖的库，先依赖进来
# avformat avcodec avfilter avutil swresample swscale
target_link_libraries(
        ffmpeg-lib

        # 忽略顺序的方式，导入
        -Wl,--start-group
        avcodec avfilter avformat avutil swresample swscale
        -Wl,--end-group

        z         #libz.so库，是FFmpeg需要用ndk的z库，FFMpeg需要额外支持  libz.so
        rtmp      #rtmp
        android   #用来渲染画面
        OpenSLES  #用来播放声音

        ${log-lib}
)