cmake_minimum_required(VERSION 3.18)
set(CMAKE_POLICY_VERSION_MINIMUM 3.5)
project(stable_diffusion_core C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Android settings
if(NOT DEFINED CMAKE_ANDROID_ARCH_ABI)
    set(CMAKE_ANDROID_ARCH_ABI "arm64-v8a" CACHE STRING "Android ABI" FORCE)
endif()
if(NOT DEFINED ANDROID_ABI)
    set(ANDROID_ABI ${CMAKE_ANDROID_ARCH_ABI} CACHE STRING "Android ABI" FORCE)
endif()
if(NOT DEFINED CMAKE_SYSTEM_PROCESSOR)
    if(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
        set(CMAKE_SYSTEM_PROCESSOR "aarch64")
    elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
        set(CMAKE_SYSTEM_PROCESSOR "armv7-a")
    endif()
endif()

# NDK path without spaces
set(ANDROID_NDK_ROOT "C:/ndk")

add_compile_options(
    -O3
    -Wall
    -fno-rtti
    -fPIC
    -fvisibility=hidden
)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-error=for-loop-analysis")

# ZSTD
set(ZSTD_ROOT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../3rdparty/zstd)
set(ZSTD_BUILD_PROGRAMS OFF CACHE BOOL "Build zstd programs" FORCE)
set(ZSTD_BUILD_STATIC ON CACHE BOOL "Build static zstd library" FORCE)
set(ZSTD_MULTITHREAD_SUPPORT OFF CACHE BOOL "Enable multithreading support" FORCE)
set(ZSTD_BUILD_TESTS OFF CACHE BOOL "Build zstd tests" FORCE)
add_subdirectory(${ZSTD_ROOT_DIR}/build/cmake zstd EXCLUDE_FROM_ALL)
set(ZSTD_INCLUDE_DIR ${ZSTD_ROOT_DIR}/lib)

# MNN
set(MNN_ROOT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../3rdparty/MNN)
set(MNN_BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(MNN_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(MNN_LOW_MEMORY ON CACHE BOOL "" FORCE)
set(MNN_BUILD_FOR_ANDROID ON CACHE BOOL "" FORCE)
set(MNN_BUILD_FOR_ANDROID_COMMAND ON CACHE BOOL "" FORCE)
set(MNN_USE_LOGCAT OFF CACHE BOOL "" FORCE)
set(MNN_OPENCL ON CACHE BOOL "" FORCE)
set(MNN_SUPPORT_TRANSFORMER_FUSE ON CACHE BOOL "" FORCE)
set(MNN_BUILD_LLM ON CACHE BOOL "" FORCE)
set(LLM_USE_MINJA ON CACHE BOOL "" FORCE)
add_subdirectory(${MNN_ROOT_DIR} mnn_build EXCLUDE_FROM_ALL)

set(OUTPUT_SUFFIX ${CMAKE_ANDROID_ARCH_ABI})
set(PLATFORM_LIBS log android dl m)

# shared library
add_library(stable_diffusion_core SHARED
    ${CMAKE_CURRENT_SOURCE_DIR}/../src/llm_jni.cpp
)

target_include_directories(stable_diffusion_core PRIVATE
    ${MNN_ROOT_DIR}/include
    ${MNN_ROOT_DIR}/transformers/llm/engine/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../include
    ${CMAKE_CURRENT_SOURCE_DIR}/../src
    ${CMAKE_CURRENT_SOURCE_DIR}/../3rdparty/json/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../3rdparty/xtensor/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../3rdparty/xtl/include
    ${CMAKE_CURRENT_SOURCE_DIR}/../3rdparty/xsimd/include
    ${ZSTD_INCLUDE_DIR}
)

target_link_libraries(stable_diffusion_core
    PRIVATE
    ${PLATFORM_LIBS}
    MNN
    libzstd
)

set_target_properties(stable_diffusion_core PROPERTIES
    LIBRARY_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/obj/local/${OUTPUT_SUFFIX}"
)
