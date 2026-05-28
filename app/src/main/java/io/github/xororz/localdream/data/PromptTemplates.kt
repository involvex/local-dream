package io.github.xororz.localdream.data

import androidx.compose.runtime.Immutable

enum class PromptTemplateCategory {
    ANIME,
    REALISTIC,
    LANDSCAPE,
    PORTRAIT,
    NSFW_TEMPLATES,
    CUSTOM
}

@Immutable
data class PromptTemplate(
    val id: String,
    val name: String,
    val category: PromptTemplateCategory,
    val prompt: String,
    val negativePrompt: String = ""
)

object PromptTemplates {
    val ANIME_TEMPLATES = listOf(
        PromptTemplate(
            id = "anime_girl_blue_hair",
            name = "Blue Hair Anime Girl",
            category = PromptTemplateCategory.ANIME,
            prompt = "masterpiece, best quality, 1girl, solo, blue twintails, very long hair, bangs, blue eyes, jewelry, necklace, hair bow, off-shoulder white frilled dress, bare shoulders, collarbone, underwater, floating hair, reaching towards viewer, air bubbles, blue theme, blurry foreground",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, missing arms, poorly drawn face, bad face, fused face, cloned face, three crus, fused feet, fused thigh, extra crus, ugly fingers, horn, huge eyes, worst face, 2girl, long fingers, disconnected limbs,"
        ),
        PromptTemplate(
            id = "anime_boy_spiky_hair",
            name = "Spiky Hair Anime Boy",
            category = PromptTemplateCategory.ANIME,
            prompt = "masterpiece, best quality, 1boy, solo, spiky blonde hair, sharp eyes, confident smile, school uniform, blue sky background, dramatic lighting, anime style",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, missing arms, poorly drawn face, bad face, fused face, cloned face, three crus, fused feet, fused thigh, extra crus, ugly fingers, horn, huge eyes, worst face, 2boy, long fingers, disconnected limbs,"
        ),
        PromptTemplate(
            id = "anime_sunset_view",
            name = "Sunset View",
            category = PromptTemplateCategory.ANIME,
            prompt = "masterpiece, best quality, 1girl, solo, silhouetted against sunset, flowing hair, peaceful expression, orange sky, distant mountains, anime style, cinematic lighting",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, missing arms, poorly drawn face, bad face, fused face, cloned face, three crus, fused feet, fused thigh, extra crus, ugly fingers, horn, huge eyes, worst face, 2girl, long fingers, disconnected limbs,"
        )
    )

    val REALISTIC_TEMPLATES = listOf(
        PromptTemplate(
            id = "realistic_portrait",
            name = "Realistic Portrait",
            category = PromptTemplateCategory.REALISTIC,
            prompt = "RAW photo, best quality, realistic, photo-realistic, masterpiece, 1girl, upper body, facing front, portrait, white shirt, soft natural lighting, shallow depth of field",
            negativePrompt = "paintings, cartoon, anime, lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, skin spots, acnes, skin blemishes"
        ),
        PromptTemplate(
            id = "realistic_landscape",
            name = "Landscape Photo",
            category = PromptTemplateCategory.REALISTIC,
            prompt = "masterpiece, best quality, ultra-detailed, realistic, 8k, a majestic cat sitting on a windowsill at sunset, warm golden light, cozy atmosphere, photorealistic",
            negativePrompt = "worst quality, low quality, normal quality, poorly drawn, lowres, low resolution, signature, watermarks, ugly, out of focus, error, blurry, unclear photo, bad photo, unrealistic, semi realistic, pixelated, cartoon, anime, cgi, drawing, 2d, 3d, censored, duplicate,"
        ),
        PromptTemplate(
            id = "realistic_street",
            name = "Street Photography",
            category = PromptTemplateCategory.REALISTIC,
            prompt = "street photography, best quality, realistic, photo-realistic, masterpiece, urban scene, candid moment, natural lighting, documentary style, high detail, 35mm film look",
            negativePrompt = "painting, cartoon, anime, lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username"
        )
    )

    val LANDSCAPE_TEMPLATES = listOf(
        PromptTemplate(
            id = "fantasy_landscape",
            name = "Fantasy Landscape",
            category = PromptTemplateCategory.LANDSCAPE,
            prompt = "masterpiece, best quality, fantasy landscape, floating islands, waterfalls cascading into clouds, magical crystals, ethereal light, misty atmosphere, vibrant colors, ultra detailed, digital painting",
            negativePrompt = "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry, deformed"
        ),
        PromptTemplate(
            id = "mountain_vista",
            name = "Mountain Vista",
            category = PromptTemplateCategory.LANDSCAPE,
            prompt = "masterpiece, best quality, mountain vista, snow-capped peaks, pine forest, crystal clear lake reflection, dramatic clouds, golden hour lighting, ultra detailed, photorealistic",
            negativePrompt = "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry, deformed"
        ),
        PromptTemplate(
            id = "desert_oasis",
            name = "Desert Oasis",
            category = PromptTemplateCategory.LANDSCAPE,
            prompt = "masterpiece, best quality, desert oasis, palm trees, turquoise water pool, golden sand dunes, dramatic sky, mirage effect, warm sunlight, ultra detailed, cinematic composition",
            negativePrompt = "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry, deformed"
        )
    )

    val PORTRAIT_TEMPLATES = listOf(
        PromptTemplate(
            id = "cute_anime_portrait",
            name = "Cute Anime Portrait",
            category = PromptTemplateCategory.PORTRAIT,
            prompt = "masterpiece, best quality, 1girl, solo, cute anime style, big beautiful eyes, soft smile, pastel colors, kawaii aesthetic, starry background, sparkles",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, missing arms, poorly drawn face, bad face, fused face, cloned face, three crus, fused feet, fused thigh, extra crus, ugly fingers, horn, huge eyes, worst face, 2girl, long fingers, disconnected limbs,"
        ),
        PromptTemplate(
            id = "professional_headshot",
            name = "Professional Headshot",
            category = PromptTemplateCategory.PORTRAIT,
            prompt = "professional headshot, best quality, realistic, photo-realistic, 1person, sharp focus, neutral background, studio lighting, confident expression, business attire",
            negativePrompt = "cartoon, anime, lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark"
        ),
        PromptTemplate(
            id = "artistic_portrait",
            name = "Artistic Portrait",
            category = PromptTemplateCategory.PORTRAIT,
            prompt = "artistic portrait, best quality, masterpiece, oil painting style, dramatic lighting, bokeh background, chiaroscuro technique, expressive brushstrokes, museum quality",
            negativePrompt = "lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark, username, blurry, deformed"
        )
    )

    val NSFW_TEMPLATES = listOf(
        PromptTemplate(
            id = "nsfw_couch_play",
            name = "Couch Play",
            category = PromptTemplateCategory.CUSTOM,
            prompt = "masterpiece, best quality, 1girl, solo, wearing lingerie, lying on a plush couch, playing with herself, hands between legs, intimate pose, soft room lighting, high detail, photorealistic, cinematic shot",
            negativePrompt = "lowres, bad anatomy, bad hands, clothing, dress, skirt, pants, fully clothed, blurry, distorted, low quality, worst quality, text, watermark"
        ),
        PromptTemplate(
            id = "nsfw_bedtime_tease",
            name = "Bedtime Tease",
            category = PromptTemplateCategory.CUSTOM,
            prompt = "masterpiece, best quality, 1girl, solo, wearing lace lingerie, lying on bed, seductive look, messy hair, pillow, warm ambient light, highly detailed, raw photo",
            negativePrompt = "lowres, bad anatomy, bad hands, clothing, fully clothed, blurry, distorted, low quality, worst quality, text, watermark"
        )
    )

    fun getAllTemplates(): List<PromptTemplate> =
        ANIME_TEMPLATES + REALISTIC_TEMPLATES + LANDSCAPE_TEMPLATES + PORTRAIT_TEMPLATES + NSFW_TEMPLATES

    fun getTemplatesByCategory(category: PromptTemplateCategory): List<PromptTemplate> =
        when (category) {
            PromptTemplateCategory.ANIME -> ANIME_TEMPLATES
            PromptTemplateCategory.REALISTIC -> REALISTIC_TEMPLATES
            PromptTemplateCategory.LANDSCAPE -> LANDSCAPE_TEMPLATES
            PromptTemplateCategory.PORTRAIT -> PORTRAIT_TEMPLATES
            PromptTemplateCategory.NSFW_TEMPLATES -> NSFW_TEMPLATES
            PromptTemplateCategory.CUSTOM -> NSFW_TEMPLATES
        }

    fun getTemplateById(id: String): PromptTemplate? =
        getAllTemplates().find { it.id == id }
}