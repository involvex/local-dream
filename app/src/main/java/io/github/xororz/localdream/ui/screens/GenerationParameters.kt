package io.github.xororz.localdream.ui.screens

import io.github.xororz.localdream.data.GenerationMode

data class GenerationParameters(
    val steps: Int,
    val cfg: Float,
    val seed: Long? = null,
    val prompt: String = "",
    val negativePrompt: String = "",
    val generationTime: String? = null,
    val width: Int,
    val height: Int,
    val runOnCpu: Boolean = false,
    val denoiseStrength: Float = 0.6f,
    val useOpenCL: Boolean = false,
    val scheduler: String = "dpm",
    val mode: GenerationMode = GenerationMode.UNKNOWN,
    val batchCount: Int = 1,
)