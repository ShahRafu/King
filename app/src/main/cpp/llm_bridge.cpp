#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "llm_bridge"
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jboolean JNICALL
Java_com_shahrafuking_kingassistant_llm_LocalLLMManager_nativeLoadModel(JNIEnv *env, jobject /* this */, jstring path) {
    const char *cpath = env->GetStringUTFChars(path, nullptr);
    ALOGI("nativeLoadModel called with path=%s", cpath);
    // Placeholder: real implementation must initialize ggml/llama runtime and load model from cpath
    env->ReleaseStringUTFChars(path, cpath);
    return JNI_FALSE; // false indicates not loaded in stub
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_shahrafuking_kingassistant_llm_LocalLLMManager_nativeGenerate(JNIEnv *env, jobject /* this */, jstring prompt, jint maxTokens) {
    const char *cprompt = env->GetStringUTFChars(prompt, nullptr);
    ALOGI("nativeGenerate called prompt=%s maxTokens=%d", cprompt, maxTokens);
    // Placeholder: actual inference not implemented here
    std::string out = std::string("[MOCK RESPONSE] Received prompt: ") + cprompt;
    env->ReleaseStringUTFChars(prompt, cprompt);
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_shahrafuking_kingassistant_llm_LocalLLMManager_nativeUnload(JNIEnv *env, jobject /* this */) {
    ALOGI("nativeUnload called");
    // Placeholder
}
