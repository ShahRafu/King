# On-Device LLM Build & Integration — Quick Start

This README explains how to build a real native LLM backend (llama.cpp / ggml) for Android (arm64-v8a) and produce a libllmbridge.so you can include in the King app. The repository includes a mock JNI bridge for quick testing; follow the steps below to replace it with a real native runtime.

High-level summary
- You will build (on your desktop) a native LLM runtime (based on ggerganov/llama.cpp) for Android arm64-v8a using the Android NDK and CMake.
- Quantize the model on desktop (using the quantize tool) to produce a ggml-quantized model (.bin) that is small enough to run on mobile.
- Build a small JNI wrapper (llmbridge) that exposes three functions to Kotlin: nativeLoadModel(path), nativeGenerate(prompt,maxTokens), nativeUnload().
- Copy the resulting libllmbridge.so into app/src/main/jniLibs/arm64-v8a/ and rebuild the APK.

IMPORTANT SAFETY & LICENSE NOTES
- You MUST obtain and verify licensing for any model you use (LLaMA/LLaMA-2/Vicuna/Alpaca). This repo does NOT download or distribute model files.
- This guide intentionally disables any network downloads in the app. Model files are owner-provided and loaded from approved local paths only.

Prerequisites (desktop)
- Unix-like desktop (Linux or macOS recommended)
- Android NDK (r21+ recommended; this repo used NDK 25.x in build.gradle examples)
- CMake (3.18+)
- Git, make, gcc/clang
- Python 3 (for utility scripts)

Environment variables you should set
- ANDROID_NDK: path to your Android NDK
- ANDROID_SDK_ROOT: path to your Android SDK (if using adb)

Recommended target config
- ABI: arm64-v8a
- Android API level: 24 or higher
- Vulkan: OFF (per your request)

Directory layout produced by the helper script
- scripts/llama.cpp/          => cloned llama.cpp sources
- scripts/build-android-arm64/ => cross-compile build dir
- scripts/llmbridge-build/    => small CMake project that builds libllmbridge.so linking the built llama.cpp artifacts

Steps (high-level)
1) Clone this repo locally and cd to its root.
2) Make sure you have ANDROID_NDK set and a compatible CMake installed.
3) Prepare a local directory for building:
   mkdir -p scripts && cd scripts
4) Clone llama.cpp into scripts/llama.cpp:
   git clone https://github.com/ggerganov/llama.cpp.git

5) Build quant tools on desktop (optional but recommended):
   cd llama.cpp
   mkdir build && cd build
   cmake ..
   make -j$(nproc)
   # The `quantize` tool will be available to quantize original model files on desktop

6) Quantize your model (desktop step):
   # Example (quantize requires a lot of RAM; run on a desktop with enough memory)
   ./quantize /path/to/original/model.bin /path/to/out/model-q4_0.bin q4_0

7) Cross-compile llama.cpp for Android arm64-v8a
   # From scripts/llama.cpp
   mkdir -p build-android-arm64 && cd build-android-arm64
   cmake .. \
     -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
     -DANDROID_ABI=arm64-v8a \
     -DANDROID_NATIVE_API_LEVEL=24 \
     -DCMAKE_BUILD_TYPE=Release \
     -DUSE_CUBLAS=OFF \
     -DUSE_VULKAN=OFF
   cmake --build . -- -j$(nproc)

   # After this you may have static or shared artifacts in the build dir. The exact target
   # names depend on the llama.cpp version and CMakeLists in the cloned repo.

8) Build the JNI wrapper (llmbridge)
   # A helper CMake project is provided in scripts/llmbridge-build/. It expects either
   # (a) to find an installed/shared llama runtime (rare), or (b) to link with static
   # objects/artifacts produced by the Android build above. Edit scripts/llmbridge-build/CMakeLists.txt
   # if your llama.cpp build artifacts are in a different place.

   mkdir -p scripts/llmbridge-build/build && cd scripts/llmbridge-build/build
   cmake .. \
     -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
     -DANDROID_ABI=arm64-v8a \
     -DANDROID_NATIVE_API_LEVEL=24 \
     -DLLAMA_CPP_BUILD_DIR=../../llama.cpp/build-android-arm64
   cmake --build . -- -j$(nproc)

   # If successful you'll get libllmbridge.so in scripts/llmbridge-build/build/.

9) Copy libllmbridge.so to the Android app
   mkdir -p ../app/src/main/jniLibs/arm64-v8a
   cp scripts/llmbridge-build/build/libllmbridge.so app/src/main/jniLibs/arm64-v8a/

10) Rebuild the Android app
   ./gradlew assembleDebug

11) Push your quantized model to the device (owner action)
   adb push /path/to/model-q4_0.bin /sdcard/Android/data/com.shahrafuking.kingassistant/files/models/model-q4_0.bin

12) Install and test the app on your device
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.shahrafuking.kingassistant/.ui.LLMTestActivity

Notes about bridging and API compatibility
- The provided JNI shim (app/src/main/cpp/llm_bridge.cpp) is a stub and a real replacement should map to the llama.cpp C++ API.
- Different llama.cpp commits/forks have different CMake targets. Review the cloned llama.cpp CMake output to identify the built targets (libggml, libllama, etc.) and link them when building libllmbridge.so.

Troubleshooting
- If cmake can't find headers/libraries, double-check your ANDROID_NDK path and the LOCATION of the build artifacts.
- If linking fails due to missing symbols, ensure you linked all required object files from the llama.cpp Android build.

If you want, I can produce a Dockerfile that bundles the NDK and builds these steps in a reproducible CI container — this is helpful if you don't want to install large native toolchains locally.
