package com.involvex.localdreamchat.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.involvex.localdreamchat.data.Model
import com.involvex.localdreamchat.data.repository.ChatRepository
import com.involvex.localdreamchat.utils.Http
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ImageGenerationBridge(
    private val context: Context,
    private val repository: ChatRepository,
) {

    companion object {
        private const val TAG = "ImageGenBridge"
        private const val BACKEND_HOST = "localhost:8081"
        private const val DEFAULT_WIDTH = 512
        private const val DEFAULT_HEIGHT = 512
        private const val DEFAULT_STEPS = 20
        private const val DEFAULT_CFG = 7.0f
        private const val BACKEND_START_TIMEOUT_MS = 120_000L
    }

    private val client by lazy {
        Http.client.newBuilder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

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
            val started = ensureBackendRunning()
            if (!started) {
                Log.w(TAG, "Backend could not be started for image generation")
                return@withContext null
            }

            val bitmap = generateImage(prompt)
            val path = saveImage(conversationId, bitmap)
            Log.i(TAG, "Image generated and saved: $path")
            path
        } catch (e: Exception) {
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
        val prompt = explicitPrompt
            ?: "portrait of $characterName, $characterDescription, masterpiece, best quality, ultra-detailed, 8k"
                .trim()

        Log.i(TAG, "Character image request: $prompt")

        try {
            val started = ensureBackendRunning()
            if (!started) {
                Log.w(TAG, "Backend could not be started for character image")
                return@withContext null
            }

            val bitmap = generateImage(prompt)
            val path = saveImage(conversationId, bitmap)
            Log.i(TAG, "Character image generated and saved: $path")
            path
        } catch (e: Exception) {
            Log.e(TAG, "Character image generation failed: ${e.message}", e)
            null
        }
    }

    /**
     * Finds a suitable downloaded model and starts the backend if needed.
     * Returns true when the backend is Running.
     */
    private suspend fun ensureBackendRunning(): Boolean {
        val state = BackendService.backendState.value
        if (state is BackendService.BackendState.Running) {
            return true
        }

        val modelConfig = findDownloadedImageModel() ?: run {
            Log.e(TAG, "No downloaded image model found")
            return false
        }

        Log.i(TAG, "Starting backend for model: ${modelConfig.modelId} (${modelConfig.backendType})")
        startBackendForModel(modelConfig)

        return withContext(Dispatchers.IO) {
            withTimeout(BACKEND_START_TIMEOUT_MS) {
                BackendService.backendState.first { it is BackendService.BackendState.Running }
            }
        }.let { true }
    }

    private fun startBackendForModel(config: BackendModelConfig) {
        val intent = Intent(context, BackendService::class.java).apply {
            putExtra("modelId", config.modelId)
            putExtra("backendType", config.backendType)
            putExtra("width", DEFAULT_WIDTH)
            putExtra("height", DEFAULT_HEIGHT)
        }
        context.startForegroundService(intent)
    }

    private data class BackendModelConfig(
        val modelId: String,
        val backendType: String,
    )

    private fun findDownloadedImageModel(): BackendModelConfig? {
        val modelsDir = Model.getModelsDir(context)
        val preferredCpuIds = listOf("absoluterealitycpu", "chilloutmixcpu")
        val preferredNpuIds = listOf("absolutereality", "chilloutmix", "qteamix", "anythingv5", "cuteyukimix")

        for (id in preferredCpuIds) {
            val dir = File(modelsDir, id)
            if (dir.exists() && dir.isDirectory && dir.listFiles()?.isNotEmpty() == true) {
                return BackendModelConfig(modelId = id, backendType = "sd15cpu")
            }
        }

        for (id in preferredNpuIds) {
            val dir = File(modelsDir, id)
            if (dir.exists() && dir.isDirectory && dir.listFiles()?.isNotEmpty() == true) {
                return BackendModelConfig(modelId = id, backendType = "sd15npu")
            }
        }

        val anyDir = modelsDir.listFiles()?.firstOrNull { dir ->
            dir.isDirectory && dir.listFiles()?.isNotEmpty() == true
        }
        return anyDir?.let { dir ->
            BackendModelConfig(modelId = dir.name, backendType = "sd15cpu")
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
    private fun generateImage(prompt: String): Bitmap {
        val jsonObject = JSONObject().apply {
            put("prompt", prompt)
            put("negative_prompt", "ugly, blurry, low quality, deformed")
            put("steps", DEFAULT_STEPS)
            put("cfg", DEFAULT_CFG.toDouble())
            put("width", DEFAULT_WIDTH)
            put("height", DEFAULT_HEIGHT)
            put("preview_format", "jpeg")
        }

        val request = Request.Builder()
            .url("http://$BACKEND_HOST/generate")
            .post(jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Generate request failed with code: ${response.code}")
        }

        val body = response.body ?: throw IOException("Empty response body")

        var base64Data: String? = null
        val reader = body.byteStream().bufferedReader()

        reader.useLines { lines ->
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
                            val format = json.optString("format", "raw")
                            if (base64Data.isNullOrEmpty()) {
                                throw IOException("no image data in complete message")
                            }
                            break
                        }
                    }
                } catch (_: Exception) {
                    Log.w(TAG, "Failed to parse SSE data: $data")
                }
            }
        }

        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            ?: throw IOException("Failed to decode image bitmap")
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
}
