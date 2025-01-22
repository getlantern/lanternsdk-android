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

# Specify include directories
include_directories(src/main/cpp/include)

# Link native libraries
find_library(log-lib log)
target_link_libraries(
    gojni
    ${log-lib}
)