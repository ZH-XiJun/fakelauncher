# keyintercept：物理按键独占抓取器（独立可执行文件）
#
# 说明：Android 只会把 *.so 打进 APK 的 lib/<abi>/，所以这里用 add_executable
#       产出 libkeyintercept.so（本体是可执行文件，不是动态库）。

cmake_minimum_required(VERSION 3.22.1)
project(keyintercept C)

add_executable(keyintercept keyintercept.c)

# 16 KB 页面对齐（Android 15+ 设备 / Google Play 要求）
target_link_options(keyintercept PRIVATE
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384")

target_link_libraries(keyintercept log)

# 输出 libkeyintercept.so，交给 AGP 打进 lib/<abi>/
set_target_properties(keyintercept PROPERTIES
        PREFIX "lib"
        SUFFIX ".so")
