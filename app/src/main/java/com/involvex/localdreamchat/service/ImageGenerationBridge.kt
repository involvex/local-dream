package com.involvex.localdreamchat.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.involvex.localdreamchat.data.repository.ChatRepository
import com.involvex.localdreamchat.utils.Http
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        // Check if backend is running before attempting generation
        val backendRunning = BackendService.backendState.value is BackendService.BackendState.Running
        if (!backendRunning) {
            Log.w(TAG, "Image generation triggered but backend is not running, skipping")
            return@withContext null
        }

        Log.i(TAG, "Triggered by keyword: '$matchedKeyword' in message: '${userMessage.take(50)}'")

        val prompt = extractPrompt(userMessage, matchedKeyword)
        if (prompt.isBlank()) {
            Log.w(TAG, "Extracted prompt is blank, skipping generation")
            return@withContext null
        }

        try {
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
     * Extracts the image generation prompt from the user message.
     * Strips the trigger keyword and cleans up the remaining text.
     */
    private fun extractPrompt(message: String, keyword: String): String {
        // Remove the trigger keyword and clean up
        var prompt = message.replaceFirst(keyword, "", ignoreCase = true).trim()

        // Remove common filler phrases
        val fillers = listOf("of a", "of an", "of the", "a picture of", "an image of", "an illustration of")
        for (filler in fillers) {
            prompt = prompt.replaceFirst(filler, "", ignoreCase = true).trim()
        }

        // If prompt is too short, use the full message as prompt
        if (prompt.length < 3) {
            prompt = message
        }

        return prompt
    }

    /**
     * Makes the HTTP request to the backend /generate endpoint.
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

        // Read streaming response — the final chunk contains the complete image
        val reader = body.byteStream().bufferedReader()
        var lastImageData: String? = null

        reader.useLines { lines ->
            for (line in lines) {
                if (line.isBlank()) continue
                try {
                    val json = JSONObject(line)
                    if (json.has("image")) {
                        lastImageData = json.getString("image")
                    }
                    if (json.optBoolean("done", false)) {
                        break
                    }
                } catch (_: Exception) {
                    // Not JSON, skip
                }
            }
        }

        val base64Data = lastImageData ?: throw IOException("No image data received")

        // Decode base64 to bitmap
        val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            ?: throw IOException("Failed to decode image bitmap")
    }

    /**
     * Saves the generated bitmap to internal storage.
     * Returns the absolute file path.
     */
    private suspend fun saveImage(conversationId: String, bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            val imagesDir = File(context.filesDir, "chat_images").apply { mkdirs() }
            val fileName = "${conversationId}_${UUID.randomUUID()}.jpg"
            val file = File(imagesDir, fileName)

            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            file.absolutePath
        }
}
