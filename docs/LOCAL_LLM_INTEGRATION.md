# Local On-Device LLM Integration — Design & Integration Guide

This document describes a secure, owner-controlled plan to integrate an on-device LLM runtime (example: llama.cpp / ggml) into the King Assistant Android app.

Goal
- Enable fully local LLM inference on the mobile device (no network calls) so the app can generate, modify, and validate code in multiple languages from natural-language prompts.
- Keep owner control and privacy: model files must live on-device under owner control; no automatic downloads are performed by this scaffold.
- Make the architecture portable/scalable so the same app-level API can use a more powerful native runtime or remote server later.

High-level architecture

app UI / Voice gatekeeper
  ↕ (owner voice confirmation via VoiceAuthGatekeeper)
LocalLLMManager (Kotlin)
  ↔ JNI bridge (llm_bridge) -> native runtime (llama.cpp / ggml compiled into .so)
  ↔ Local persistence: models placed into app internal storage or external SD (owner-installed)
  ↔ SelfHealingManager (patch staging + validation)

Design decisions & rationale
- Native backend (llama.cpp / ggml) is the recommended approach because it runs fully locally using a small native C/C++ runtime and supports quantized models for lower footprint.
- We expose a stable Kotlin API (LocalLLMManager) which calls a native JNI bridge. The native library is responsible for loading the .bin/ggml model and running inference.
- The app enforces the VoiceAuthGatekeeper before any prompt that triggers code generation or code-writing operations (owner must approve).
- For safety the native library will not have any network I/O: the JNI boundary will accept prompts and return text — no automatic remote calls.
- Model files are large; the app will not include them in the repo. The owner must manually copy a model file onto the device into a protected folder or install via ADB/USB. The docs provide recommended paths.

Model choices (recommendations)
- Small, mobile-friendly quantized models (recommended first phase):
  - ggml-alpaca-7b-q4_0.bin (requires ~4–6 GB RAM depending on quantization and thread use)
  - llama-2-7b/ggml quantized variants (check license) — Llama 2 has special license; confirm compliance.
  - For truly constrained phones, use 3B or 2B models (if available) or distilled models.
- GPU / Vulkan acceleration: llama.cpp has Vulkan support on Android; enabling it improves speed significantly but requires building with Vulkan and testing on target devices.
- Licensing: verify the model license (LLaMA/LLaMA2/Vicuna/Alpaca etc.) before using. This scaffold enforces owner responsibility to provide licensed model files.

Where to place model files on device
- Recommended path (owner must copy):
  - /sdcard/Android/data/com.shahrafuking.kingassistant/files/models/<model.bin>
  - or app internal storage (if owner installs model via app UI using SAF or direct file copy): context.filesDir/llm_models/<model.bin>
- The app will only load models from these pre-approved local locations. No network downloads are attempted.

Native build & integration options
1) Build native library using Android NDK (recommended for production):
   - Use llama.cpp or a fork that provides C API and Android-friendly build with CMake & NDK.
   - Enable Vulkan support for GPU acceleration (optional).
   - Produce a shared object libllmbridge.so with a C API the JNI bridge calls.

2) Build on-device (Termux) — advanced users only:
   - It is possible to compile llama.cpp on-device in Termux but CPU/ram/time constraints make it difficult. For most workflows, cross-compile via NDK on a desktop and include .so.

Security & privacy
- Owner must provide model files; the app will refuse to load models from unknown network sources.
- Use VoiceAuthGatekeeper: any prompt that requests code generation, staging, or file writes must pass voice approval first.
- Consider additional approval: device unlock, 2FA PIN.
- Model files may contain sensitive prompts; keep them encrypted at rest if desired (owner choice).

API design (Kotlin)
- LocalLLMManager
  - suspend fun loadModel(path: String): Boolean
  - suspend fun generate(prompt: String, maxTokens: Int = 256, stream: ((String)->Unit)? = null): String
  - fun unload()

- These methods call native JNI functions. Native implementation must:
  - Load ggml model from given path.
  - Run tokenization & forward passes.
  - Support incremental streaming if possible.

Native JNI bridge (llm_bridge) — responsibilities
- Provide functions callable from Kotlin: nativeLoadModel(path), nativeGenerate(prompt, maxTokens), nativeUnload().
- Perform necessary initialization and memory pooling.
- Strictly NO network calls.

On-device inference considerations
- Memory: quantized 4-bit models require multiple GB of RAM. Test on target device.
- Threads: tune number of threads based on CPU cores and memory.
- Thermal & battery: long-running inference may heat/throttle the device. Warn the owner in UI.

Fallback & portability
- The Kotlin API is the stable contract: if in the future you move to a server or more powerful device, implement the same LocalLLMManager interface to call a different backend (e.g., remote server or desktop native binary).

Step-by-step integration plan (concrete)
Phase 0 — Preparation (owner action)
- Decide on model: choose a compatible ggml-quantized model and confirm license.
- Copy the model to device: /sdcard/Android/data/com.shahrafuking.kingassistant/files/models/model.bin or via app UI (to be built later).

Phase 1 — Native JNI scaffold (we add now)
- Add JNI bridge (C++ minimal stubs) and CMake file to the app.
- Update app/build.gradle to build native library via externalNativeBuild.
- Add LocalLLMManager Kotlin wrapper which loads libllmbridge and exposes safe methods.

Phase 2 — Native runtime (owner or developer action)
- Build llama.cpp-based native lib with Android NDK and link it into the app as libllmbridge.so. Or cross-compile on desktop using NDK and copy .so into app/src/main/jniLibs/<abi>/. This step is outside automated repo changes because it requires building large native code and model files.

Phase 3 — Runtime testing & optimization (on-device)
- Load model via app UI or debug console: LocalLLMManager.loadModel(path)
- Try simple prompts, measure memory & latency, tune thread count.
- If GPU acceleration desired, rebuild native library with Vulkan support and test.

Phase 4 — Code generation integration
- Wire SelfHealingManager and LocalLLMManager: before calling generate for code-writing prompts, call VoiceAuthGatekeeper to get owner approval. On approval, call generate() and present output to owner for staged write.

Phase 5 — Promotion & commit
- After owner approves staged patch and local tests pass, provide UI/CLI to promote staged file to repo and create a local git commit (no push). Optionally offer to export patch for manual review.

Files added in this commit
- app/src/main/java/com/shahrafuking/kingassistant/llm/LocalLLMManager.kt (Kotlin API + voice gatekeeper integration example)
- app/src/main/cpp/CMakeLists.txt (minimal scaffold)
- app/src/main/cpp/llm_bridge.cpp (JNI stubs)
- docs/LOCAL_LLM_INTEGRATION.md (this file)
- app/build.gradle updated to include externalNativeBuild CMake config and default ndkVersion placeholder

Developer notes / next actions for me if you want me to continue
- If you want, I can attempt to add a small prebuilt libllmbridge.so for armeabi-v7a/arm64-v8a that returns mock responses, but a real lib must be produced by building llama.cpp for Android which is large and cannot be stored in this repo by default.
- I can add an in-app UI screen to load model files from storage and run simple prompts with voice authorization.

If you approve, I will now commit the JNI scaffold and Kotlin wrapper (stubs already prepared) and update the build file. Then I can proceed to either:
- A: Add an in-app UI for model loading + run simple prompt flow with voice approval (no real native inference yet). OR
- B: Provide step-by-step scripts and NDK build instructions to produce libllmbridge.so using llama.cpp (desktop + NDK cross-compile or Android Vulkan build), plus recommended settings for quantization and threading.

Please confirm whether I should proceed with A (in-app UI + integration test using mock native) or B (detailed native build instructions for owner to build lib with llama.cpp).