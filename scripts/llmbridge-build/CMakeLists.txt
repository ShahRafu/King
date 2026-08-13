cmake_minimum_required(VERSION 3.10)
project(llmbridge-builder C CXX)

# This helper CMake project builds a small JNI wrapper that links with the
# llama.cpp Android build artifacts. You may need to edit the LLAMA_CPP_BUILD_DIR
# and link libraries depending on the llama.cpp CMake outputs.

if(NOT DEFINED LLAMA_CPP_BUILD_DIR)
  message(FATAL_ERROR "LLAMA_CPP_BUILD_DIR must be provided - point to the llama.cpp android build directory")
endif()

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find JNI headers provided by Android NDK toolchain - CMake toolchain handles this

add_library(llmbridge SHARED
    ../../app/src/main/cpp/llm_bridge.cpp
)

# Example: link to static libs or object files produced by llama.cpp build
# You may need to adjust these names to match the output of your llama.cpp build

# try to find libggml.a or similar inside LLAMA_CPP_BUILD_DIR
find_library(LLAMA_STATIC_LIB NAMES ggml PATHS ${LLAMA_CPP_BUILD_DIR} NO_DEFAULT_PATH)
if(LLAMA_STATIC_LIB)
  message(STATUS "Found llama static lib: ${LLAMA_STATIC_LIB}")
  target_link_libraries(llmbridge PUBLIC ${LLAMA_STATIC_LIB})
else()
  message(WARNING "Could not automatically find a llama static lib in ${LLAMA_CPP_BUILD_DIR}. You may need to edit this CMakeLists to point at the right artifacts.")
endif()

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(llmbridge PRIVATE ${log-lib} ${android-lib})
