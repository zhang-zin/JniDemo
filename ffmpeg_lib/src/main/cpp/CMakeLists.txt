
cmake_minimum_required(VERSION 3.10.2)

# 打印日志
message("当前CMake的路径是：${CMAKE_SOURCE_DIR}")
message("当前 CMAKE_ANDROID_ARCH_ABI 的路径是：${CMAKE_ANDROID_ARCH_ABI}")

file(GLOB allCpp *.cpp)

add_library(
        ffmpeg-lib
        SHARED
        ${allCpp})

find_library(
        log-lib
        log)

#引入FFmpeg的头文件
include_directories(${CMAKE_SOURCE_DIR}/include)

# 引入FFmpeg的库文件，设置内部的方式引入，指定库的目录是 -L  指定具体的库-l
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/${CMAKE_ANDROID_ARCH_ABI}")

target_link_libraries(
        ffmpeg-lib

        # 先把有依赖的库，先依赖进来
        avformat avcodec avfilter avutil swresample swscale

        ${log-lib}
)