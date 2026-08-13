#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>

#define LOG_TAG "llm_bridge"
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool model_loaded = false;
static std::string loaded_model_path;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_shahrafuking_kingassistant_llm_LocalLLMManager_nativeLoadModel(JNIEnv *env, jobject /* this */, jstring path) {
    const char *cpath = env->GetStringUTFChars(path, nullptr);
    if (cpath == nullptr) {
        ALOGE("nativeLoadModel: path is null");
        return JNI_FALSE;
    }

    ALOGI("nativeLoadModel called with path=%s", cpath);

    // Best-effort: check the file exists and is readable
    bool ok = (access(cpath, R_OK) == 0);
    if (ok) {
        model_loaded = true;
        loaded_model_path = std::string(cpath);
        ALOGI("Model marked as loaded (mock): %s", cpath);
    } else {
        model_loaded = false;
        loaded_model_path.clear();
        ALOGE("Model file not accessible: %s", cpath);
    }

    env->ReleaseStringUTFChars(path, cpath);
    return model_loaded ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_shahrafuking_kingassistant_llm_LocalLLMManager_nativeGenerate(JNIEnv *env, jobject /* this */, jstring prompt, jint maxTokens) {
    const char *cprompt = env->GetStringUTFChars(prompt, nullptr);
    if (cprompt == nullptr) return env->NewStringUTF("(error: null prompt)");

    ALOGI("nativeGenerate called prompt=%s maxTokens=%d", cprompt, maxTokens);

    if (!model_loaded) {
        std::string err = "ERROR: model not loaded (native stub).";
        env->ReleaseStringUTFChars(prompt, cprompt);
        return env->NewStringUTF(err.c_str());
    }

    // Mock generation: produce deterministic, safe output so the app flow can be tested.
    std::string out = "[LLM-MOCK RESPONSE]\nModel: ";
    out += loaded_model_path.empty() ? std::string("(unknown)") : loaded_model_path;
    out += "\n--- Prompt start ---\n";
    out += cprompt;
    out += "\n--- Prompt end ---\n";
    out += "// NOTE: This is a mock response from the native stub. Replace with real llama.cpp integration.\n";

    env->ReleaseStringUTFChars(prompt, cprompt);
    return env->NewStringUTF(out.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_shahrafuking_kingassistant_llm_LocalLLMManager_nativeUnload(JNIEnv *env, jobject /* this */) {
    ALOGI("nativeUnload called");
    model_loaded = false;
    loaded_model_path.clear();
}
