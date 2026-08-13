#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "wasm_bridge"
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_shahrafuking_kingassistant_wasm_PythonWasmAdapter_nativeRunPythonWasm(JNIEnv *env, jobject /* this */, jstring code, jint timeoutMs) {
    const char *ccode = env->GetStringUTFChars(code, nullptr);
    if (ccode == nullptr) return env->NewStringUTF("(error: null code)");

    ALOGI("nativeRunPythonWasm called (mock). code len=%zu timeout=%d", strlen(ccode), timeoutMs);

    // Mock execution: do not run arbitrary code here. Return a safe mock result.
    std::string out = "[PY_WASM_MOCK]\nExecuted code (mock) length=";
    out += std::to_string(strlen(ccode));
    out += "\nOutput:\n";
    out += "(mock) Python execution successful. Replace with real WASM runtime integration.";

    env->ReleaseStringUTFChars(code, ccode);
    return env->NewStringUTF(out.c_str());
}
