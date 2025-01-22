# XXX <14-10-21, soltzen> We're using gomobile, but we need to ensure
# libc++_shared.so is included in the binary
cmake_minimum_required(VERSION 3.10.2)
project("lantern-android")

# Add library
add_library(
    gojni
    SHARED
    src/main/cpp/native-lib.cpp
)

include(ndk-stl-config.cmake)

# Link native libraries
find_library(log-lib log)
target_link_libraries(
    gojni
    ${log-lib}
)