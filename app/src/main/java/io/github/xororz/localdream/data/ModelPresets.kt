
package io.github.xororz.localdream.data

data class ModelPreset(
    val name: String,
    val steps: Int,
    val cfg: Float,
    val scheduler: String
)

val defaultPresets = listOf(
    ModelPreset("Fast Preview", steps = 10, cfg = 5.0f, scheduler = "euler_a"),
    ModelPreset("Balanced", steps = 20, cfg = 7.0f, scheduler = "dpm_karras"),
    ModelPreset("High Quality", steps = 40, cfg = 8.0f, scheduler = "dpm_sde_karras")
)
