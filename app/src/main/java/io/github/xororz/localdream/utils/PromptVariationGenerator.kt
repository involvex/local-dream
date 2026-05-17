package io.github.xororz.localdream.utils

object PromptVariationGenerator {
    private val stylePrefixes = listOf(
        "masterpiece, best quality,",
        "masterpiece, ultra-detailed,",
        "best quality, high detail,",
        "masterpiece, cinematic lighting,",
        "intricate details, sharp focus,",
        "8k, ultra HD,",
        "trending on ArtStation,",
        "award winning photograph,"
    )

    private val qualitySuffixes = listOf(
        "4k, sharp focus",
        "ultra-detailed, crisp",
        "high resolution, clear",
        "professional quality, sharp",
        "cinematic, detailed"
    )

    private val lightingModify = mapOf(
        "sunlight" to listOf("golden hour sunlight", "warm afternoon light", "soft morning light", "dramatic backlighting"),
        "studio" to listOf("professional studio lighting", "softbox lighting", "rim lighting", "three-point lighting"),
        "candlelight" to listOf("warm candlelight", "flickering firelight", "intimate lamp glow", "cozy ambient lighting"),
        "neon" to listOf("neon lights", "cyberpunk lighting", "colorful LED strips", "glowing signs"),
        "moonlight" to listOf("silver moonlight", "moonbeams", "lunar glow", "starlight")
    )

    private val styleModifiers = listOf(
        "cinematic composition",
        "rule of thirds",
        "depth of field",
        "bokeh background",
        "dramatic perspective",
        "leading lines",
        "golden ratio",
        "wide angle lens"
    )

    fun generateVariations(prompt: String, count: Int = 5): List<String> {
        if (prompt.isBlank()) return emptyList()

        val variations = mutableListOf<String>()
        val basePrompt = prompt.trim()

        for (i in 0 until count) {
            val modified = when (i % 4) {
                0 -> addStylePrefix(basePrompt)
                1 -> addQualitySuffix(basePrompt)
                2 -> modifyLighting(basePrompt)
                3 -> addStyleModifier(basePrompt)
                else -> basePrompt
            }
            variations.add(modified)
        }

        return variations.distinct()
    }

    private fun addStylePrefix(prompt: String): String {
        val prefix = stylePrefixes.random()
        return if (prompt.startsWith("masterpiece") || prompt.startsWith("best quality")) {
            prompt
        } else {
            "$prefix $prompt"
        }
    }

    private fun addQualitySuffix(prompt: String): String {
        val suffix = qualitySuffixes.random()
        val cleanPrompt = prompt.trimEnd(',')
        return "$cleanPrompt, $suffix".trim()
    }

    private fun modifyLighting(prompt: String): String {
        for ((key, replacements) in lightingModify) {
            if (prompt.contains(key, ignoreCase = true)) {
                return prompt.replace(key, replacements.random(), ignoreCase = true)
            }
        }
        return prompt
    }

    private fun addStyleModifier(prompt: String): String {
        val modifier = styleModifiers.random()
        val cleanPrompt = prompt.trimEnd(',')
        return "$cleanPrompt, $modifier".trim()
    }

    fun enhancePrompt(prompt: String): String {
        if (prompt.isBlank()) return prompt

        return buildString {
            append(stylePrefixes.random())
            append(" ")
            append(prompt)
            append(", ")
            append(qualitySuffixes.random())
            append(", ")
            append(styleModifiers.random())
        }
    }
}