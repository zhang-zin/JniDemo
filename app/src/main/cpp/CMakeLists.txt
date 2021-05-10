
cmake_minimum_required(VERSION 3.10.2)

project("jnidemo")

# 打印日志
message("当前CMake的路径是：${CMAKE_SOURCE_DIR}")
message("当前 CMAKE_ANDROID_ARCH_ABI 的路径是：${CMAKE_ANDROID_ARCH_ABI}")

file(GLOB allCpp *.cpp)

add_library(
        native-lib
        SHARED
        ${allCpp})

# 导入静态库
# add_library(test_a STATIC IMPORTED)
# 开始真正的导入
# set_target_properties(test_a PROPERTIES IMPORTED_LOCATION ${CMAKE_SOURCE_DIR}/main.a)

# 导入动态库
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/../jniLibs/${CMAKE_ANDROID_ARCH_ABI}")

find_library(
        log-lib
        log)

target_link_libraries(
        native-lib
        ${log-lib}
        Test
)