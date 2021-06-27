cmake_minimum_required(VERSION 3.10.2)

include_directories(include/opencv include/opencv2)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${opencv}/libs/${CMAKE_ANDROID_ARCH_ABI}")

file(GLOB allCpp *.cpp .c)

add_library(
        native-lib
        SHARED
        ${allCpp}
)

find_library(
        log_lib
        log
)

target_link_libraries(
        native-lib
        log
        jnigraphics
        opencv_java3
)


