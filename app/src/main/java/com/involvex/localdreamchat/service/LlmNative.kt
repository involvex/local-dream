package com.involvex.localdreamchat.service

/**
 * JNI bridge to the MNN-LLM native engine.
 * All native methods are synchronized to ensure thread safety.
 */
class LlmNative {
    companion object {
        init {
            System.loadLibrary("stable_diffusion_core")
        }

        @JvmStatic
        external fun nativeCreate(configPath: String): Long

        @JvmStatic
        external fun nativeLoad(ptr: Long)

        @JvmStatic
        external fun nativeResponse(ptr: Long, prompt: String, maxTokens: Int): String

        @JvmStatic
        external fun nativeResponseStream(
            ptr: Long,
            prompt: String,
            callback: TokenCallback,
            maxTokens: Int,
        )

        @JvmStatic
        external fun nativeReset(ptr: Long)

        @JvmStatic
        external fun nativeDestroy(ptr: Long)

        @JvmStatic
        external fun nativeIsLoaded(ptr: Long): Boolean

        @JvmStatic
        external fun nativeSetConfig(ptr: Long, configJson: String): String
    }
}

/**
 * Callback interface for streaming LLM token generation.
 */
interface TokenCallback {
    fun onToken(token: String)
    fun onComplete()
    fun onError(error: String)
}
