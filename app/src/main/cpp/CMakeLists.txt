cmake_minimum_required(VERSION 3.10)
project(llmbridge)

# Simple CMake file for JNI bridge. The real LLM backend (llama.cpp/ggml) must be
# added and linked here in a full integration.

add_library(llmbridge SHARED
    llm_bridge.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_include_directories(llmbridge PRIVATE ${CMAKE_SOURCE_DIR})

target_link_libraries(llmbridge ${log-lib} ${android-lib})
