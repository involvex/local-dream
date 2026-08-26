package com.involvex.localdreamchat.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.core.graphics.createBitmap
import com.involvex.localdreamchat.data.Model
import com.involvex.localdreamchat.data.repository.ChatRepository
import com.involvex.localdreamchat.utils.Http
import com.involvex.localdreamchat.utils.rgbBytesToPixels
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ImageGenerationBridge(
    private val context: Context,
    // Reserved for persisting generation metadata with chat messages.
    @Suppress("UnusedPrivateProperty")
    private val repository: ChatRepository,
) {

    companion object {
        private const val TAG = "ImageGenBridge"
        private const val BACKEND_HOST = "localhost:8081"
        private const val DEFAULT_WIDTH_NPU = 512
        private const val DEFAULT_HEIGHT_NPU = 512

        // Matches the main screen's defaultGenerationSize(runOnCpu = true):
        // 512x512 on the CPU backend takes minutes per image, which made chat
        // look stuck while it was merely generating slowly.
        private const val GENERATION_SIZE_CPU = 256
        private const val DEFAULT_STEPS = 20
        private const val DEFAULT_CFG = 7.0f
        private const val BACKEND_START_TIMEOUT_MS = 120_000L
    }

    /** Aborts a bridge operation after recording [message] in [lastError]. */
    private class BridgeAbort(message: String) : IOException(message)

    private val client by lazy {
        Http.client.newBuilder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val healthClient by lazy {
        Http.client.newBuilder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    // Human-readable reason for the most recent failure, surfaced by the
    // ViewModel so chat can show something better than a generic message.
    @Volatile
    var lastError: String? = null
        private set

    /**
     * Checks if the user message contains any image trigger keywords
     * for the given character, and if so, generates an image.
     * Returns the saved image path, or null if no generation was triggered.
     */
    suspend fun checkAndGenerate(
        conversationId: String,
        userMessage: String,
        imageTriggerKeywords: List<String>,
    ): String? = withContext(Dispatchers.IO) {
        lastError = null

        val matchedKeyword = imageTriggerKeywords.firstOrNull { keyword ->
            userMessage.contains(keyword, ignoreCase = true)
        } ?: return@withContext null

        Log.i(TAG, "Triggered by keyword: '$matchedKeyword' in message: '${userMessage.take(50)}'")

        val prompt = extractPrompt(userMessage, matchedKeyword)
        if (prompt.isBlank()) {
            Log.w(TAG, "Extracted prompt is blank, skipping generation")
            return@withContext null
        }

        try {
            val config = ensureBackendRunning()
            val bitmap = generateImage(prompt, config.generationWidth, config.generationHeight)
            val path = saveImage(conversationId, bitmap)
            Log.i(TAG, "Image generated and saved: $path")
            path
        } catch (e: Exception) {
            lastError = e.message ?: "Image generation failed"
            Log.e(TAG, "Image generation failed: ${e.message}", e)
            null
        }
    }

    /**
     * Explicit image generation request (e.g. from a button tap).
     * Uses the character description as prompt if no explicit prompt is given.
     */
    suspend fun generateCharacterImage(
        conversationId: String,
        characterName: String,
        characterDescription: String,
        explicitPrompt: String? = null,
    ): String? = withContext(Dispatchers.IO) {
        lastError = null

        val prompt = explicitPrompt
            ?: "portrait of $characterName, $characterDescription, masterpiece, best quality, ultra-detailed, 8k"
                .trim()

        Log.i(TAG, "Character image request: $prompt")

        try {
            val config = ensureBackendRunning()
            val bitmap = generateImage(prompt, config.generationWidth, config.generationHeight)
            val path = saveImage(conversationId, bitmap)
            Log.i(TAG, "Character image generated and saved: $path")
            path
        } catch (e: Exception) {
            lastError = e.message ?: "Image generation failed"
            Log.e(TAG, "Character image generation failed: ${e.message}", e)
            null
        }
    }

    /**
     * Finds a suitable downloaded model, starts or reuses the backend, and
     * blocks until its /health endpoint actually answers. Returns the config
     * that ended up running.
     *
     * @throws BridgeAbort (an [IOException]) on failure, with [lastError] set.
     */
    private suspend fun ensureBackendRunning(): BackendModelConfig {
        var modelConfig = findDownloadedImageModel() ?: failConfig("No downloaded image model found")

        if (!isDeviceSupportedForBackend(modelConfig.backendType)) {
            Log.w(TAG, "Device does not support ${modelConfig.backendType} backend")
            modelConfig = findCpuModel() ?: failConfig(
                "Device does not support ${modelConfig.backendType} and no CPU model is downloaded",
            )
        }

        val state = BackendService.backendState.value
        val servingSameModel =
            state is BackendService.BackendState.Running &&
                BackendService.servingModelId.value == modelConfig.modelId

        if (!servingSameModel) {
            if (state is BackendService.BackendState.Error) {
                Log.w(TAG, "Restarting backend after previous error: ${state.message}")
            }
            Log.i(TAG, "Starting backend for model: ${modelConfig.modelId} (${modelConfig.backendType})")
            startBackendForModel(modelConfig)
        } else {
            Log.i(TAG, "Reusing running backend for ${modelConfig.modelId}")
        }

        return awaitBackendHealthy(modelConfig)
    }

    /**
     * Waits until the backend serves a 200 from /health. BackendService flips
     * to Running as soon as the OS process spawns - long before the model
     * finishes loading and binds port 8081 - so POSTing /generate without this
     * gate fails with ECONNREFUSED.
     */
    private suspend fun awaitBackendHealthy(config: BackendModelConfig): BackendModelConfig {
        return withContext(Dispatchers.IO) {
            val deadline = System.currentTimeMillis() + BACKEND_START_TIMEOUT_MS
            var effectiveConfig = config
            var triedCpuFallback = false
            var loggedWaiting = false

            while (true) {
                val state = BackendService.backendState.value
                if (state is BackendService.BackendState.Error &&
                    (state.modelId == null || state.modelId == effectiveConfig.modelId)
                ) {
                    // SIGILL on a CPU backend is terminal; anything else may be
                    // recoverable once by switching to a downloaded CPU model.
                    val terminalCpuCrash = effectiveConfig.backendType == Model.BACKEND_SD15_CPU &&
                        state.message.contains("SIGILL")
                    val cpuModel = findCpuModel()
                    if (terminalCpuCrash || triedCpuFallback ||
                        effectiveConfig.backendType != Model.BACKEND_SD15_NPU || cpuModel == null
                    ) {
                        throw BridgeAbort(state.message.ifBlank { "Backend failed to start" })
                    }
                    triedCpuFallback = true
                    Log.w(TAG, "NPU backend failed (${state.message}), retrying with CPU model")
                    effectiveConfig = cpuModel
                    startBackendForModel(effectiveConfig)
                }

                if (isHealthy()) {
                    Log.i(TAG, "Backend healthy: ${effectiveConfig.modelId}")
                    return@withContext effectiveConfig
                }

                if (!loggedWaiting) {
                    Log.i(TAG, "Waiting for ${effectiveConfig.modelId} backend to become healthy")
                    loggedWaiting = true
                }

                if (System.currentTimeMillis() > deadline) {
                    throw BridgeAbort(
                        "Backend did not become ready within ${BACKEND_START_TIMEOUT_MS / 1000}s",
                    )
                }

                delay(500)
            }
            @Suppress("UNREACHABLE_CODE")
            effectiveConfig
        }
    }

    private fun isHealthy(): Boolean = try {
        healthClient.newCall(
            Request.Builder()
                .url("http://$BACKEND_HOST/health")
                .get()
                .build(),
        ).execute().use { it.isSuccessful }
    } catch (_: Exception) {
        false
    }

    private fun findCpuModel(): BackendModelConfig? {
        val modelsDir = Model.getModelsDir(context)
        for (id in Model.cpuFallbackModelIds) {
            val dir = File(modelsDir, id)
            if (dir.exists() && dir.isDirectory && dir.listFiles()?.isNotEmpty() == true) {
                return BackendModelConfig(modelId = id, backendType = Model.BACKEND_SD15_CPU)
            }
        }
        return null
    }

    /**
     * Checks whether the device is compatible with the requested backend type.
     */
    private fun isDeviceSupportedForBackend(backendType: String): Boolean {
        if (backendType == Model.BACKEND_SD15_CPU) return true
        return Model.isDeviceSupported()
    }

    private fun startBackendForModel(config: BackendModelConfig) {
        val intent = Intent(context, BackendService::class.java).apply {
            putExtra("modelId", config.modelId)
            putExtra("backendType", config.backendType)
            putExtra("width", config.generationWidth)
            putExtra("height", config.generationHeight)
        }
        context.startForegroundService(intent)
    }

    private data class BackendModelConfig(
        val modelId: String,
        val backendType: String,
    ) {
        // CPU inference is far slower per pixel than NPU; 512x512 took minutes
        // per chat image, 256x256 matches what the main screen uses.
        val generationWidth: Int
            get() = if (backendType == Model.BACKEND_SD15_CPU) GENERATION_SIZE_CPU else DEFAULT_WIDTH_NPU

        val generationHeight: Int
            get() = if (backendType == Model.BACKEND_SD15_CPU) GENERATION_SIZE_CPU else DEFAULT_HEIGHT_NPU
    }

    private fun findDownloadedImageModel(): BackendModelConfig? {
        val modelsDir = Model.getModelsDir(context)
        val preferredCpuIds = listOf("absoluterealitycpu", "chilloutmixcpu")
        val preferredNpuIds = listOf("absolutereality", "chilloutmix", "qteamix", "anythingv5", "cuteyukimix")

        for (id in preferredCpuIds) {
            val dir = File(modelsDir, id)
            if (dir.exists() && dir.isDirectory && dir.listFiles()?.isNotEmpty() == true) {
                return BackendModelConfig(modelId = id, backendType = Model.BACKEND_SD15_CPU)
            }
        }

        for (id in preferredNpuIds) {
            val dir = File(modelsDir, id)
            if (dir.exists() && dir.isDirectory && dir.listFiles()?.isNotEmpty() == true) {
                return BackendModelConfig(modelId = id, backendType = Model.BACKEND_SD15_NPU)
            }
        }

        val anyDir = modelsDir.listFiles()?.firstOrNull { dir ->
            dir.isDirectory && dir.listFiles()?.isNotEmpty() == true
        }
        return anyDir?.let { dir ->
            BackendModelConfig(modelId = dir.name, backendType = Model.BACKEND_SD15_CPU)
        }
    }

    /**
     * Extracts the image generation prompt from the user message.
     * Strips the trigger keyword and cleans up the remaining text.
     */
    private fun extractPrompt(message: String, keyword: String): String {
        var prompt = message.replaceFirst(keyword, "", ignoreCase = true).trim()

        val fillers = listOf("of a", "of an", "of the", "a picture of", "an image of", "an illustration of")
        for (filler in fillers) {
            prompt = prompt.replaceFirst(filler, "", ignoreCase = true).trim()
        }

        if (prompt.length < 3) {
            prompt = message
        }

        return prompt
    }

    /**
     * Makes the HTTP request to the backend /generate endpoint.
     * Parses the SSE stream (data: <json>) and returns the final image.
     */
    private fun generateImage(prompt: String, width: Int, height: Int): Bitmap {
        val jsonObject = JSONObject().apply {
            put("prompt", prompt)
            put("negative_prompt", "ugly, blurry, low quality, deformed")
            put("steps", DEFAULT_STEPS)
            put("cfg", DEFAULT_CFG.toDouble())
            put("width", width)
            put("height", height)
            put("preview_format", "jpeg")
        }

        val request = Request.Builder()
            .url("http://$BACKEND_HOST/generate")
            .post(jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val code = response.code
            response.close()
            throw IOException("Generate request failed with code: $code")
        }

        val body = response.body ?: run {
            response.close()
            throw IOException("Empty response body")
        }

        var base64Data: String? = null
        var format = "raw"

        body.byteStream().bufferedReader().useLines { lines ->
            for (line in lines) {
                if (line.isBlank()) continue

                val data = parseSseLine(line) ?: continue
                if (data == "[DONE]") break

                try {
                    val json = JSONObject(data)
                    when (json.optString("type")) {
                        "progress" -> {
                            val step = json.optInt("step")
                            val total = json.optInt("total_steps")
                            Log.d(TAG, "Generation progress: $step/$total")
                        }

                        "complete" -> {
                            base64Data = json.optString("image")
                            format = json.optString("format", "raw")
                            if (base64Data.isNullOrEmpty()) {
                                throw IOException("no image data in complete message")
                            }
                            return@useLines
                        }

                        "error" -> throw IOException(json.optString("message", "backend error"))
                    }
                } catch (e: IOException) {
                    throw e
                } catch (_: Exception) {
                    Log.w(TAG, "Failed to parse SSE data: $data")
                }
            }
        }

        if (base64Data.isNullOrBlank()) {
            throw IOException("Backend did not return image data")
        }

        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)

        // The default payload format is raw RGB bytes, which BitmapFactory
        // cannot read - mirror BackgroundGenerationService's conversion.
        val bitmap = if (format == "raw") {
            val pixels = IntArray(width * height)
            rgbBytesToPixels(decodedBytes, pixels)
            createBitmap(width, height).also {
                it.setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } else {
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }
        return bitmap ?: throw IOException("Failed to decode image bitmap")
    }

    private fun parseSseLine(line: String): String? {
        var trimmed = line.trim()
        if (trimmed.isEmpty()) return null

        if (trimmed.startsWith("data: ")) {
            trimmed = trimmed.substring(6).trim()
        }

        return trimmed.takeIf { it.isNotEmpty() }
    }

    /**
     * Saves the generated bitmap to internal storage.
     * Returns the absolute file path.
     */
    private suspend fun saveImage(conversationId: String, bitmap: Bitmap): String = withContext(Dispatchers.IO) {
        val imagesDir = File(context.filesDir, "chat_images").apply { mkdirs() }
        val fileName = "${conversationId}_${UUID.randomUUID()}.jpg"
        val file = File(imagesDir, fileName)

        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        file.absolutePath
    }

    /** Records [message] as [lastError] and aborts the operation. */
    private fun failConfig(message: String): Nothing = throw BridgeAbort(message)
}
