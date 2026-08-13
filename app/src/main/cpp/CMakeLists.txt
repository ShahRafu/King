cmake_minimum_required(VERSION 3.10)
project(llmbridge)

# Build both JNI bridge sources. The real LLM backend and WASM runtime should be
# linked when available. For now these are simple stubs that allow the app to build.

add_library(llmbridge SHARED
    llm_bridge.cpp
    wasm_bridge.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_include_directories(llmbridge PRIVATE ${CMAKE_SOURCE_DIR})

target_link_libraries(llmbridge ${log-lib} ${android-lib})
