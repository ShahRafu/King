#!/usr/bin/env bash
set -euo pipefail

# scripts/build-llama-android.sh
# High-level automation helper to cross-compile llama.cpp for Android (arm64-v8a) and build a JNI
# wrapper libllmbridge.so. This script is opinionated and may require edits depending on your
# llama.cpp fork and NDK/CMake versions.

: ${ANDROID_NDK:?"ANDROID_NDK must be set and point to your Android NDK"}
ABI=${1:-arm64-v8a}
API_LEVEL=${2:-24}
NUM_JOBS=${NUM_JOBS:-$(nproc)}

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
SCRIPTS_DIR="$ROOT_DIR/scripts"
LLAMA_DIR="$SCRIPTS_DIR/llama.cpp"
BUILD_DIR="$LLAMA_DIR/build-android-$ABI"
LLM_BRIDGE_BUILD="$SCRIPTS_DIR/llmbridge-build/build"

echo "ROOT_DIR=$ROOT_DIR"

# 1) Clone llama.cpp if missing
if [ ! -d "$LLAMA_DIR" ]; then
  git clone https://github.com/ggerganov/llama.cpp.git "$LLAMA_DIR"
fi

# 2) Create build dir
mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

echo "Running cmake for llama.cpp (ABI=$ABI, API=$API_LEVEL)"
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=$ABI \
  -DANDROID_NATIVE_API_LEVEL=$API_LEVEL \
  -DCMAKE_BUILD_TYPE=Release \
  -DUSE_VULKAN=OFF

# 3) Build
cmake --build . -- -j${NUM_JOBS}

# 4) Build llmbridge JNI wrapper
mkdir -p "$LLM_BRIDGE_BUILD"
cd "$LLM_BRIDGE_BUILD"
cmake ../../llmbridge-build \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=$ABI \
  -DANDROID_NATIVE_API_LEVEL=$API_LEVEL \
  -DLLAMA_CPP_BUILD_DIR="$BUILD_DIR"

cmake --build . -- -j${NUM_JOBS}

# 5) Copy produced libllmbridge.so into app jniLibs
LIB_OUT="libllmbridge.so"
if [ -f "$LLM_BRIDGE_BUILD/$LIB_OUT" ]; then
  mkdir -p "$ROOT_DIR/app/src/main/jniLibs/$ABI"
  cp "$LLM_BRIDGE_BUILD/$LIB_OUT" "$ROOT_DIR/app/src/main/jniLibs/$ABI/"
  echo "Copied $LIB_OUT to app/src/main/jniLibs/$ABI/"
else
  echo "Could not find $LIB_OUT in $LLM_BRIDGE_BUILD"
  exit 1
fi

echo "Build finished. Now run: ./gradlew :app:assembleDebug"
