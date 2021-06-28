cmake_minimum_required(VERSION 3.10.2)

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

include_directories(include)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS}  -L${CMAKE_SOURCE_DIR}/../jinLibs/${CMAKE_ANDROID_ARCH_ABI}")
#add_library(lib_opencv SHARED IMPORTED)
#set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/include/libs/${CMAKE_ANDROID_ARCH_ABI}/libopencv_java4.so)


find_library(
        log-lib

        log)

target_link_libraries(
        native-lib

        log
        jnigraphics
        opencv_java4
        #lib_opencv
)


