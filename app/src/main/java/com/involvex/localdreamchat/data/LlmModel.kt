package com.involvex.localdreamchat.data

import android.content.Context
import android.content.Intent
import com.involvex.localdreamchat.service.ModelDownloadService
import java.io.File

/**
 * LLM model catalog entry.
 * Models are downloaded as individual files and placed in filesDir/llm_model/.
 */
data class LlmModel(
    val id: String,
    val name: String,
    val description: String,
    val baseUrl: String,
    val fileUris: List<String>,
    val approximateSize: String,
) {
    val modelDir: (Context) -> File
        get() = { ctx -> File(ctx.filesDir, "llm_model") }

    fun isDownloaded(context: Context): Boolean {
        val dir = File(context.filesDir, "llm_model")
        return dir.exists() &&
            dir.listFiles()?.isNotEmpty() == true &&
            File(dir, "llm_config.json").exists() &&
            File(dir, "llm.mnn").exists()
    }

    fun startDownload(context: Context, baseUrl: String) {
        val urls = fileUris.map {
            if (it.startsWith("http")) it else "${baseUrl.removeSuffix("/")}/$it"
        }
        val intent = Intent(context, ModelDownloadService::class.java).apply {
            action = ModelDownloadService.ACTION_START_DOWNLOAD
            putExtra(ModelDownloadService.EXTRA_MODEL_ID, id)
            putExtra(ModelDownloadService.EXTRA_MODEL_NAME, name)
            putStringArrayListExtra(ModelDownloadService.EXTRA_FILE_URLS, ArrayList(urls))
            putExtra(ModelDownloadService.EXTRA_IS_ZIP, false)
            putExtra(ModelDownloadService.EXTRA_IS_NPU, false)
            putExtra(ModelDownloadService.EXTRA_MODEL_TYPE, "llm")
        }
        context.startForegroundService(intent)
    }

    companion object {
        val DEFAULT_MODEL = LlmModel(
            id = "qwen2.5_1.5b_instruct",
            name = "Qwen2.5-1.5B-Instruct",
            description = "A compact 1.5B parameter language model optimized for instruction following. Ideal for on-device chat.",
            baseUrl = "https://www.modelscope.cn/models/MNN/",
            fileUris = listOf(
                "Qwen2.5-1.5B-Instruct-MNN/resolve/main/llm.mnn",
                "Qwen2.5-1.5B-Instruct-MNN/resolve/main/llm_config.json",
                "Qwen2.5-1.5B-Instruct-MNN/resolve/main/tokenizer.txt",
            ),
            approximateSize = "~1.0 GB",
        )

        fun all(): List<LlmModel> = listOf(DEFAULT_MODEL)
    }
}
