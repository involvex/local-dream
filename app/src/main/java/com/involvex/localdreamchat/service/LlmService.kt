package com.involvex.localdreamchat.service

import android.content.Context
import android.util.Log
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Service wrapper for the MNN-LLM on-device language model.
 * Manages model lifecycle (load/unload) and provides text generation.
 *
 * Only one LLM instance should be active at a time due to memory constraints.
 * When entering chat, call [loadModel]; when leaving, call [unload].
 */
class LlmService(private val context: Context) {

    companion object {
        private const val TAG = "LlmService"
        private const val DEFAULT_MAX_TOKENS = 512
        private const val MODEL_DIR_NAME = "llm_model"

        @Volatile
        private var instance: LlmService? = null

        fun get(context: Context): LlmService = instance ?: synchronized(this) {
            instance ?: LlmService(context.applicationContext).also { instance = it }
        }

        @Deprecated("Use get(context) instead", ReplaceWith("get(context)"))
        fun getInstance(context: Context): LlmService = get(context)
    }

    private var nativePtr: Long = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<LlmState>(LlmState.Unloaded)
    val state: StateFlow<LlmState> = _state.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    /**
     * Load the LLM model from the app's internal storage.
     * The model directory should contain: llm.mnn, llm.mnn.weight, llm_config.json, tokenizer.txt
     */
    fun load() = loadModel()

    fun loadModel() {
        if (_state.value is LlmState.Loading || _state.value is LlmState.Ready) return

        _state.value = LlmState.Loading
        scope.launch {
            try {
                val modelDir = getModelDir()
                if (!modelDir.exists()) {
                    _state.value = LlmState.Error("Model directory not found. Please download the LLM model first.")
                    return@launch
                }

                val configFile = File(modelDir, "llm_config.json")
                if (!configFile.exists()) {
                    _state.value = LlmState.Error("Model config not found: ${configFile.absolutePath}")
                    return@launch
                }

                Log.d(TAG, "Loading LLM model from: ${modelDir.absolutePath}")
                nativePtr = LlmNative.nativeCreate(configFile.absolutePath)

                if (nativePtr == 0L) {
                    _state.value = LlmState.Error("Failed to create LLM instance")
                    return@launch
                }

                LlmNative.nativeLoad(nativePtr)
                _state.value = LlmState.Ready
                Log.d(TAG, "LLM model loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load LLM model", e)
                _state.value = LlmState.Error("Failed to load model: ${e.message}")
                if (nativePtr != 0L) {
                    try {
                        LlmNative.nativeDestroy(nativePtr)
                    } catch (_: Exception) {}
                    nativePtr = 0
                }
            }
        }
    }

    /**
     * Generate a response to the given prompt (blocking, non-streaming).
     */
    suspend fun generate(
        prompt: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
    ): String = withContext(Dispatchers.Default) {
        if (nativePtr == 0L) return@withContext ""
        _isGenerating.value = true
        try {
            LlmNative.nativeResponse(nativePtr, prompt, maxTokens)
        } catch (e: Exception) {
            Log.e(TAG, "Generation failed", e)
            ""
        } finally {
            _isGenerating.value = false
        }
    }

    /**
     * Multi-turn chat generation: passes the full transcript (system +
     * history + latest user message) through the model's chat template and
     * resets the KV cache per call, so responses depend on the actual
     * conversation instead of degrading across turns.
     */
    suspend fun generateChat(
        messages: List<LlmChatMessage>,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
    ): String = withContext(Dispatchers.Default) {
        if (nativePtr == 0L || messages.isEmpty()) return@withContext ""
        val payload = org.json.JSONArray().apply {
            for (message in messages) {
                put(
                    org.json.JSONObject()
                        .put("role", message.role)
                        .put("content", message.content),
                )
            }
        }
        _isGenerating.value = true
        try {
            LlmNative.nativeResponseChat(nativePtr, payload.toString(), maxTokens)
        } catch (e: Exception) {
            Log.e(TAG, "Chat generation failed", e)
            ""
        } finally {
            _isGenerating.value = false
        }
    }

    /**
     * Generate a response with streaming token callbacks.
     */
    fun generateStream(
        prompt: String,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
        onToken: (String) -> Unit = {},
        onComplete: () -> Unit = {},
        onError: (String) -> Unit = {},
    ) {
        if (nativePtr == 0L) {
            onError("LLM not loaded")
            return
        }
        _isGenerating.value = true
        scope.launch {
            try {
                val callback = object : TokenCallback {
                    override fun onToken(token: String) {
                        onToken(token)
                    }

                    override fun onComplete() {
                        _isGenerating.value = false
                        onComplete()
                    }

                    override fun onError(error: String) {
                        _isGenerating.value = false
                        onError(error)
                    }
                }
                LlmNative.nativeResponseStream(nativePtr, prompt, callback, maxTokens)
            } catch (e: Exception) {
                Log.e(TAG, "Stream generation failed", e)
                _isGenerating.value = false
                onError("Generation failed: ${e.message}")
            }
        }
    }

    /**
     * Generate a response with a TokenCallback for chat-style usage.
     */
    fun generateStream(
        systemPrompt: String,
        messages: List<LlmChatMessage>,
        callback: TokenCallback,
        maxTokens: Int = DEFAULT_MAX_TOKENS,
    ) {
        if (nativePtr == 0L) {
            callback.onError("LLM not loaded")
            return
        }

        // Build the full prompt from system + conversation history
        val promptBuilder = StringBuilder()
        promptBuilder.appendLine(systemPrompt)
        promptBuilder.appendLine()
        for (msg in messages) {
            val roleLabel = if (msg.role == "user") "User" else "Assistant"
            promptBuilder.appendLine("$roleLabel: ${msg.content}")
        }
        promptBuilder.append("Assistant:")

        val prompt = promptBuilder.toString()

        _isGenerating.value = true
        scope.launch {
            try {
                val wrappedCallback = object : TokenCallback {
                    private val responseBuilder = StringBuilder()

                    override fun onToken(token: String) {
                        responseBuilder.append(token)
                        callback.onToken(token)
                    }

                    override fun onComplete() {
                        _isGenerating.value = false
                        callback.onComplete()
                    }

                    override fun onError(error: String) {
                        _isGenerating.value = false
                        callback.onError(error)
                    }
                }
                LlmNative.nativeResponseStream(nativePtr, prompt, wrappedCallback, maxTokens)
            } catch (e: Exception) {
                Log.e(TAG, "Stream generation failed", e)
                _isGenerating.value = false
                callback.onError("Generation failed: ${e.message}")
            }
        }
    }

    /**
     * Reset the conversation history (clears KV cache).
     */
    fun reset() {
        if (nativePtr == 0L) return
        try {
            LlmNative.nativeReset(nativePtr)
        } catch (e: Exception) {
            Log.e(TAG, "Reset failed", e)
        }
    }

    /**
     * Unload the model and free memory.
     */
    fun unload() {
        if (nativePtr == 0L) return
        try {
            LlmNative.nativeDestroy(nativePtr)
            nativePtr = 0
            _state.value = LlmState.Unloaded
            Log.d(TAG, "LLM model unloaded")
        } catch (e: Exception) {
            Log.e(TAG, "Unload failed", e)
        }
    }

    /**
     * Get the app-internal directory where the LLM model files should be placed.
     * Expected structure: filesDir/llm_model/{llm.mnn, llm.mnn.weight, llm_config.json, tokenizer.txt}
     */
    fun getModelDir(): File = File(context.filesDir, MODEL_DIR_NAME)

    /**
     * Check if the LLM model is available in local storage.
     */
    fun isModelDownloaded(): Boolean {
        val dir = getModelDir()
        return dir.exists() &&
            File(dir, "llm_config.json").exists() &&
            File(dir, "tokenizer.txt").exists()
    }

    fun destroy() {
        unload()
        scope.cancel()
        instance = null
    }
}

sealed class LlmState {
    data object Unloaded : LlmState()
    data object Loading : LlmState()
    data object Ready : LlmState()
    data class Error(val message: String) : LlmState()
}

data class LlmChatMessage(
    val role: String,
    val content: String,
)
