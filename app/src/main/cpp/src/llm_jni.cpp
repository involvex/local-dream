#include <jni.h>
#include <android/log.h>
#include <string>
#include <mutex>
#include <atomic>
#include <sstream>

#include <nlohmann/json.hpp>

#include "llm/llm.hpp"

#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, "LocalDreamLLM", __VA_ARGS__)

using namespace MNN::Transformer;

// Global mutex for LLM operations (only one LLM instance at a time)
static std::mutex g_llm_mutex;

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_involvex_localdreamchat_service_LlmNative_nativeCreate(
    JNIEnv *env, jobject thiz, jstring config_path) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);
    const char *path = env->GetStringUTFChars(config_path, nullptr);
    Llm *llm = Llm::createLLM(std::string(path));
    env->ReleaseStringUTFChars(config_path, path);
    return reinterpret_cast<jlong>(llm);
}

JNIEXPORT void JNICALL
Java_com_involvex_localdreamchat_service_LlmNative_nativeLoad(
    JNIEnv *env, jobject thiz, jlong ptr) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);
    Llm *llm = reinterpret_cast<Llm *>(ptr);
    if (llm) {
        llm->load();
    }
}

JNIEXPORT jstring JNICALL
Java_com_involvex_localdreamchat_service_LlmNative_nativeResponse(
    JNIEnv *env, jobject thiz, jlong ptr, jstring prompt, jint max_tokens) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);
    Llm *llm = reinterpret_cast<Llm *>(ptr);
    if (!llm) return env->NewStringUTF("");

    const char *p = env->GetStringUTFChars(prompt, nullptr);
    std::string prompt_str(p);
    env->ReleaseStringUTFChars(prompt, p);

    std::ostringstream output;
    llm->response(prompt_str, &output, nullptr, max_tokens);
    return env->NewStringUTF(output.str().c_str());
}

JNIEXPORT void JNICALL
Java_com_involvex_localdreamchat_service_LlmNative_nativeResponseStream(
    JNIEnv *env, jobject thiz, jlong ptr, jstring prompt,
    jobject callback, jint max_tokens) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);
    Llm *llm = reinterpret_cast<Llm *>(ptr);
    if (!llm) return;

    const char *p = env->GetStringUTFChars(prompt, nullptr);
    std::string prompt_str(p);
    env->ReleaseStringUTFChars(prompt, p);

    // Get callback method
    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");
    jmethodID onCompleteMethod = env->GetMethodID(callbackClass, "onComplete", "()V");
    jmethodID onErrorMethod = env->GetMethodID(callbackClass, "onError", "(Ljava/lang/String;)V");

    // Custom stream buffer that calls the Kotlin callback
    class JniStreamBuffer : public std::streambuf {
    public:
        JniStreamBuffer(JNIEnv *env, jobject callback, jmethodID onTokenMethod)
            : mEnv(env), mCallback(callback), mOnTokenMethod(onTokenMethod) {}

    protected:
        int overflow(int c) override {
            if (c != EOF) {
                std::string s(1, static_cast<char>(c));
                jstring token = mEnv->NewStringUTF(s.c_str());
                mEnv->CallVoidMethod(mCallback, mOnTokenMethod, token);
                mEnv->DeleteLocalRef(token);
            }
            return c;
        }

        int sync() override { return 0; }

    private:
        JNIEnv *mEnv;
        jobject mCallback;
        jmethodID mOnTokenMethod;
    };

    try {
        JniStreamBuffer stream_buffer(env, callback, onTokenMethod);
        std::ostream output_ostream(&stream_buffer);
        llm->response(prompt_str, &output_ostream, nullptr, max_tokens);
        env->CallVoidMethod(callback, onCompleteMethod);
    } catch (const std::exception &e) {
        jstring error = env->NewStringUTF(e.what());
        env->CallVoidMethod(callback, onErrorMethod, error);
        env->DeleteLocalRef(error);
    }
}

JNIEXPORT void JNICALL
Java_com_involvex_localdreamchat_service_LlmNative_nativeReset(
    JNIEnv *env, jobject thiz, jlong ptr) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);
    Llm *llm = reinterpret_cast<Llm *>(ptr);
    if (llm) {
        llm->reset();
    }
}

JNIEXPORT void JNICALL
Java_com_involvex_localdreamchat_service_LlmNative_nativeDestroy(
    JNIEnv *env, jobject thiz, jlong ptr) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);
    Llm *llm = reinterpret_cast<Llm *>(ptr);
    if (llm) {
        Llm::destroy(llm);
    }
}

JNIEXPORT jboolean JNICALL
Java_com_involvex_localdreamchat_service_LlmNative_nativeIsLoaded(
    JNIEnv *env, jobject thiz, jlong ptr) {
    return ptr != 0 ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL
Java_com_involvex_localdreamchat_service_LlmNative_nativeSetConfig(
    JNIEnv *env, jobject thiz, jlong ptr, jstring config_json) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);
    Llm *llm = reinterpret_cast<Llm *>(ptr);
    if (!llm) return env->NewStringUTF("{}");

    const char *config_str = env->GetStringUTFChars(config_json, nullptr);
    std::string result = llm->set_config(std::string(config_str)) ? "ok" : "error";
    env->ReleaseStringUTFChars(config_json, config_str);
    return env->NewStringUTF(result.c_str());
}

// Multi-turn chat: messages_json is a JSON array of {role, content}. MNN
// applies the model's chat template across all turns, and the KV cache is
// reset first so each call is a stateless full-transcript generation.
JNIEXPORT jstring JNICALL
Java_com_involvex_localdreamchat_service_LlmNative_nativeResponseChat(
    JNIEnv *env, jobject thiz, jlong ptr, jstring messages_json, jint max_tokens) {
    std::lock_guard<std::mutex> lock(g_llm_mutex);
    Llm *llm = reinterpret_cast<Llm *>(ptr);
    if (!llm) return env->NewStringUTF("");

    const char *mj = env->GetStringUTFChars(messages_json, nullptr);
    if (!mj) return env->NewStringUTF("");
    std::string json_str(mj);
    env->ReleaseStringUTFChars(messages_json, mj);

    try {
        nlohmann::json parsed = nlohmann::json::parse(json_str);
        ChatMessages prompts;
        for (const auto &item : parsed) {
            prompts.emplace_back(
                item.at("role").get<std::string>(),
                item.at("content").get<std::string>());
        }

        llm->reset();

        std::ostringstream output;
        llm->response(prompts, &output, nullptr, max_tokens);
        return env->NewStringUTF(output.str().c_str());
    } catch (const std::exception &e) {
        ALOGE("nativeResponseChat failed: %s", e.what());
        return env->NewStringUTF("");
    }
}

} // extern "C"
