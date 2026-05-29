package io.github.xororz.localdream.data

import androidx.compose.runtime.Immutable

enum class PromptTemplateCategory {
    ANIME,
    REALISTIC,
    LANDSCAPE,
    PORTRAIT,
    NSFW,
    ENVIRONMENT,
    CHARACTER,
    STYLE,
    CUSTOM,
    NSFW_TEMPLATES
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
    // ============ ANIME TEMPLATES ============
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
        ),
        PromptTemplate(
            id = "anime_maid",
            name = "Cute Maid",
            category = PromptTemplateCategory.ANIME,
            prompt = "masterpiece, best quality, 1girl, solo, maid outfit, frilled apron, headdress, cat ears, shy expression, holding tray, detailed interior, baroque style mansion background, soft lighting, anime illustration",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn face, bad face, fused face, cloned face, ugly fingers, huge eyes, worst quality, low quality, blurry, deformed"
        ),
        PromptTemplate(
            id = "anime_witch",
            name = "Witch Fantasy",
            category = PromptTemplateCategory.ANIME,
            prompt = "masterpiece, best quality, 1girl, solo, witch outfit, black dress with purple accents, pointed hat with stars, holding magic wand, magical particles, floating spell circles, mystical atmosphere, dark fantasy, anime style",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn face, bad face, fused face, cloned face, ugly fingers, huge eyes, worst quality, low quality, blurry, deformed"
        ),
        PromptTemplate(
            id = "anime_idol",
            name = "Stage Idol",
            category = PromptTemplateCategory.ANIME,
            prompt = "masterpiece, best quality, 1girl, solo, idol outfit, sparkling dress, microphone in hand, stage lights, concert stage, crowd silhouette background, energetic pose, confident smile, anime style, vibrant colors",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn face, bad face, fused face, cloned face, ugly fingers, huge eyes, worst quality, low quality, blurry, deformed"
        ),
        PromptTemplate(
            id = "anime_yandere",
            name = "Yandere Girl",
            category = PromptTemplateCategory.ANIME,
            prompt = "masterpiece, best quality, 1girl, solo, yandere expression, long dark hair, red rose in hair, school uniform, knife in hand, psychological horror atmosphere, intense eyes, unsettling smile, anime, detailed shadows",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn face, bad face, fused face, cloned face, ugly fingers, huge eyes, worst quality, low quality, blurry"
        ),
        PromptTemplate(
            id = "anime_neko",
            name = "Neko Girl",
            category = PromptTemplateCategory.ANIME,
            prompt = "masterpiece, best quality, 1girl, solo, cat ears, fluffy tail, bell collar, oversized sweater, cute pose, sitting position, animal ears, tail swaying, soft expression, cozy bedroom background, anime style",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn face, bad face, fused face, cloned face, ugly fingers, huge eyes, worst quality, low quality, blurry"
        ),
        PromptTemplate(
            id = "anime_cyberpunk",
            name = "Cyberpunk Girl",
            category = PromptTemplateCategory.ANIME,
            prompt = "masterpiece, best quality, 1girl, solo, cyberpunk outfit, neon lights, glowing eyes, futuristic city background, holographic interfaces, chrome accessories, night city, rain effects, anime style, detailed mecha aesthetic",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn face, bad face, fused face, cloned face, ugly fingers, huge eyes, worst quality, low quality, blurry"
        ),
        PromptTemplate(
            id = "anime_vampire",
            name = "Vampire Gothic",
            category = PromptTemplateCategory.ANIME,
            prompt = "masterpiece, best quality, 1girl, solo, vampire outfit, gothic lolita dress, bat accessories, pale skin, red eyes, fangs visible, moonlit castle background, night sky, dramatic lighting, anime gothic style",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn face, bad face, fused face, cloned face, ugly fingers, huge eyes, worst quality, low quality, blurry"
        )
    )

    // ============ REALISTIC TEMPLATES ============
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
        ),
        PromptTemplate(
            id = "realistic_headshot",
            name = "Professional Headshot",
            category = PromptTemplateCategory.REALISTIC,
            prompt = "professional photography, best quality, realistic, photo-realistic, 1person, headshot, sharp focus, neutral background, studio lighting, confident expression, high resolution, DSLR quality",
            negativePrompt = "cartoon, anime, lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality, normal quality, jpeg artifacts, signature, watermark"
        ),
        PromptTemplate(
            id = "realistic_fashion",
            name = "Fashion Photography",
            category = PromptTemplateCategory.REALISTIC,
            prompt = "fashion photography, best quality, realistic, photo-realistic, magazine cover style, model pose, designer clothing, professional lighting, fashion editorial, high-end boutique background",
            negativePrompt = "cartoon, anime, painting, lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality, jpeg artifacts, signature, watermark"
        ),
        PromptTemplate(
            id = "realistic_product",
            name = "Product Shot",
            category = PromptTemplateCategory.REALISTIC,
            prompt = "product photography, best quality, realistic, photo-realistic, commercial product shot, studio lighting, white background, clean composition, high detail, professional lighting, 8k resolution",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, signature, watermark, ugly, blurry, shadow, reflection, distorted"
        )
    )

    // ============ LANDSCAPE TEMPLATES ============
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
        ),
        PromptTemplate(
            id = "night_city",
            name = "Night City",
            category = PromptTemplateCategory.LANDSCAPE,
            prompt = "cyberpunk cityscape, best quality, ultra detailed, night scene, neon lights, towering skyscrapers, flying vehicles, rain-slicked streets, reflections on wet surfaces, futuristic metropolis, cinematic",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, amateur, poorly drawn, day, sunny, bright"
        ),
        PromptTemplate(
            id = "tropical_beach",
            name = "Tropical Beach",
            category = PromptTemplateCategory.LANDSCAPE,
            prompt = "masterpiece, best quality, tropical beach, crystal clear turquoise water, white sand, palm trees, hammock, sunset colors, coconuts, paradise, exotic location, photorealistic, cinematic lighting",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, cold, winter, snow, industrial, ugly"
        ),
        PromptTemplate(
            id = "enchanted_forest",
            name = "Enchanted Forest",
            category = PromptTemplateCategory.LANDSCAPE,
            prompt = "masterpiece, best quality, enchanted forest, glowing mushrooms, bioluminescent plants, mystical fog, ancient trees, fairy lights, magical atmosphere, fireflies, fantasy nature, ultra detailed, dreamy",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, modern, urban, building, ugly, dirty"
        ),
        PromptTemplate(
            id = "aurora_sky",
            name = "Aurora Borealis",
            category = PromptTemplateCategory.LANDSCAPE,
            prompt = "masterpiece, best quality, aurora borealis, northern lights, green and purple sky, starry night, snowy landscape, frozen lake, silhouetted pine trees, Iceland scenery, magical atmosphere, photorealistic",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, day, bright, sunny, warm colors"
        ),
        PromptTemplate(
            id = "volcanic_eruption",
            name = "Volcanic Landscape",
            category = PromptTemplateCategory.LANDSCAPE,
            prompt = "masterpiece, best quality, volcanic eruption, lava flow, erupting volcano, smoke and ash, dramatic sky, molten rock, volcanic landscape, apocalyptic atmosphere, cinematic, ultra detailed",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, peaceful, calm, gentle, cold, snow"
        )
    )

    // ============ PORTRAIT TEMPLATES ============
    val PORTRAIT_TEMPLATES = listOf(
        PromptTemplate(
            id = "cute_anime_portrait",
            name = "Cute Anime Portrait",
            category = PromptTemplateCategory.PORTRAIT,
            prompt = "masterpiece, best quality, 1girl, solo, cute anime style, big beautiful eyes, soft smile, pastel colors, kawaii aesthetic, starry background, sparkles",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, missing arms, poorly drawn face, bad face, fused face, cloned face, three crus, fused feet, fused thigh, extra crus, ugly fingers, horn, huge eyes, worst face, 2girl, long fingers, disconnected limbs,"
        ),
        PromptTemplate(
            id = "professional_headshot_portrait",
            name = "Professional Headshot Portrait",
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
        ),
        PromptTemplate(
            id = "glamour_portrait",
            name = "Glamour Portrait",
            category = PromptTemplateCategory.PORTRAIT,
            prompt = "glamour photography, best quality, realistic, photo-realistic, elegant pose, soft makeup, beautiful lighting, sophisticated background, fashion magazine style, high detail",
            negativePrompt = "cartoon, anime, painting, lowres, bad anatomy, bad hands, text, error, missing fingers, extra digit, cropped, worst quality, low quality, jpeg artifacts, watermark"
        ),
        PromptTemplate(
            id = "vintage_portrait",
            name = "Vintage Portrait",
            category = PromptTemplateCategory.PORTRAIT,
            prompt = "vintage portrait, best quality, retro style, 1950s aesthetic, sepia tones, classic Hollywood glamour, film grain, elegant pose, vintage clothing, nostalgic atmosphere",
            negativePrompt = "modern, lowres, bad anatomy, bad hands, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, cartoon, anime"
        ),
        PromptTemplate(
            id = "moody_portrait",
            name = "Moody Portrait",
            category = PromptTemplateCategory.PORTRAIT,
            prompt = "moody portrait, best quality, dramatic lighting, dark atmosphere, single light source, dramatic shadows, mysterious expression, cinematic, high contrast, editorial style",
            negativePrompt = "bright, cheerful, lowres, bad anatomy, bad hands, text, error, worst quality, low quality, jpeg artifacts, blurry, flat lighting"
        )
    )

    // ============ NSFW TEMPLATES ============
    val NSFW_TEMPLATES = listOf(
        PromptTemplate(
            id = "nsfw_lingerie",
            name = "Lingerie Portrait",
            category = PromptTemplateCategory.NSFW,
            prompt = "masterpiece, best quality, 1girl, solo, lace lingerie, sheer fabric details, sitting on bed, seductive pose, warm ambient lighting, high detail, photorealistic, soft shadows, intimate bedroom setting, tasteful erotic",
            negativePrompt = "lowres, bad anatomy, bad hands, extra limbs, fused body, bad proportions, missing arms, missing legs, blurry, distorted, low quality, worst quality, text, watermark, fully nude, explicit, deformed"
        ),
        PromptTemplate(
            id = "nsfw_boudoir",
            name = "Boudoir Style",
            category = PromptTemplateCategory.NSFW,
            prompt = "masterpiece, best quality, 1girl, solo, boudoir photography, wearing babydoll lingerie, silk sheets, romantic lighting, elegant pose, soft focus, tasteful erotic art, intimate atmosphere, professional photography",
            negativePrompt = "lowres, bad anatomy, bad hands, extra limbs, fused body, bad proportions, missing arms, missing legs, blurry, distorted, low quality, worst quality, text, watermark, fully nude, deformed, ugly"
        ),
        PromptTemplate(
            id = "nsfw_shower",
            name = "Post-Shower",
            category = PromptTemplateCategory.NSFW,
            prompt = "masterpiece, best quality, 1girl, solo, wet hair, wearing towel, post-shower scene, water droplets on skin, steamy bathroom setting, mirror reflection, soft natural lighting, tasteful, intimate moment, high detail",
            negativePrompt = "lowres, bad anatomy, bad hands, extra limbs, fused body, bad proportions, missing arms, missing legs, blurry, distorted, low quality, worst quality, text, watermark, fully nude, deformed, cartoon"
        ),
        PromptTemplate(
            id = "nsfw_underwear",
            name = "Casual Underwear",
            category = PromptTemplateCategory.NSFW,
            prompt = "masterpiece, best quality, 1girl, solo, wearing panties and bra, casual pose, bedroom setting, natural lighting, soft shadows, relaxed intimate atmosphere, tasteful erotic photography, high detail, photorealistic",
            negativePrompt = "lowres, bad anatomy, bad hands, extra limbs, fused body, bad proportions, missing arms, missing legs, blurry, distorted, low quality, worst quality, text, watermark, fully nude, deformed"
        ),
        PromptTemplate(
            id = "nsfw_yoga",
            name = "Yoga Pose",
            category = PromptTemplateCategory.NSFW,
            prompt = "masterpiece, best quality, 1girl, solo, athletic wear, yoga pose, stretching pose, sports bra and leggings, fit body, yoga mat, natural lighting, tasteful fitness photography, high detail, photorealistic",
            negativePrompt = "lowres, bad anatomy, bad hands, extra limbs, fused body, bad proportions, missing arms, missing legs, blurry, distorted, low quality, worst quality, text, watermark, deformed"
        ),
        PromptTemplate(
            id = "nsfw_sleepwear",
            name = "Sleepwear",
            category = PromptTemplateCategory.NSFW,
            prompt = "masterpiece, best quality, 1girl, solo, wearing silk nightgown, transparent fabric, dim bedroom lighting, romantic atmosphere, seductive pose, soft focus, tasteful erotic art, high detail, photorealistic",
            negativePrompt = "lowres, bad anatomy, bad hands, extra limbs, fused body, bad proportions, missing arms, missing legs, blurry, distorted, low quality, worst quality, text, watermark, fully nude, deformed"
        ),
        PromptTemplate(
            id = "nsfw_tight_clothes",
            name = "Tight Clothes",
            category = PromptTemplateCategory.NSFW,
            prompt = "masterpiece, best quality, 1girl, solo, tight fitting clothes, bodycon dress, form fitting outfit, walking pose, street fashion, urban background, confident expression, high detail, photorealistic fashion",
            negativePrompt = "lowres, bad anatomy, bad hands, extra limbs, fused body, bad proportions, missing arms, missing legs, blurry, distorted, low quality, worst quality, text, watermark, deformed"
        ),
        PromptTemplate(
            id = "nsfw_swimsuit",
            name = "Swimsuit Beach",
            category = PromptTemplateCategory.NSFW,
            prompt = "masterpiece, best quality, 1girl, solo, bikini swimsuit, beach setting, sandy beach background, tropical location, ocean view, warm sunlight, attractive pose, vacation vibes, high detail, photorealistic",
            negativePrompt = "lowres, bad anatomy, bad hands, extra limbs, fused body, bad proportions, missing arms, missing legs, blurry, distorted, low quality, worst quality, text, watermark, deformed, cartoon"
        ),
        PromptTemplate(
            id = "nsfw_pantyhose",
            name = "Pantyhose Elegance",
            category = PromptTemplateCategory.NSFW,
            prompt = "masterpiece, best quality, 1girl, solo, wearing pantyhose, elegant dress, formal attire, standing pose, sophisticated setting, dramatic lighting, fashion photography, high detail, photorealistic",
            negativePrompt = "lowres, bad anatomy, bad hands, extra limbs, fused body, bad proportions, missing arms, missing legs, blurry, distorted, low quality, worst quality, text, watermark, deformed"
        ),
        PromptTemplate(
            id = "nsfw_cosplay",
            name = "Tasteful Cosplay",
            category = PromptTemplateCategory.NSFW,
            prompt = "masterpiece, best quality, 1girl, solo, cosplay outfit, revealing costume, anime-inspired outfit, convention hall background, playful pose, enthusiastic expression, high detail, anime style illustration",
            negativePrompt = "lowres, bad anatomy, bad hands, extra limbs, fused body, bad proportions, missing arms, missing legs, blurry, distorted, low quality, worst quality, text, watermark, deformed, ugly"
        )
    )

    // ============ ENVIRONMENT TEMPLATES ============
    val ENVIRONMENT_TEMPLATES = listOf(
        PromptTemplate(
            id = "env_cozy_room",
            name = "Cozy Living Room",
            category = PromptTemplateCategory.ENVIRONMENT,
            prompt = "interior design, cozy living room, warm lighting, fireplace, comfortable sofa, bookshelf, wooden floors, plants, curtains, inviting atmosphere, soft lighting, photorealistic, 8k resolution",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, outdoor, cold, sterile, ugly"
        ),
        PromptTemplate(
            id = "env_cafe",
            name = "Cozy Cafe",
            category = PromptTemplateCategory.ENVIRONMENT,
            prompt = "interior design, cozy cafe, warm lighting, wooden tables, coffee cups, pastries display, plants, vintage decor, relaxed atmosphere, people in background, soft natural light, photorealistic",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, outdoor, ugly, dirty, industrial"
        ),
        PromptTemplate(
            id = "env_library",
            name = "Grand Library",
            category = PromptTemplateCategory.ENVIRONMENT,
            prompt = "grand library interior, tall bookshelves, spiral staircase, reading nook, warm lighting, vintage books, globe, antique furniture, classical architecture, majestic atmosphere, photorealistic",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, modern, ugly, outdoor"
        ),
        PromptTemplate(
            id = "env_japanese_room",
            name = "Japanese Room",
            category = PromptTemplateCategory.ENVIRONMENT,
            prompt = "traditional Japanese room, tatami floor, shoji screens, zen garden view, low table, floor cushions, cherry blossom outside window, peaceful atmosphere, natural lighting, minimalist design, photorealistic",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, western style, ugly"
        ),
        PromptTemplate(
            id = "env_garden",
            name = "English Garden",
            category = PromptTemplateCategory.ENVIRONMENT,
            prompt = "english garden, manicured hedges, colorful flowers, stone pathway, garden shed, vintage bench, pond with koi fish, lush greenery, overcast sky, romantic atmosphere, photorealistic",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, indoor, ugly, desert"
        ),
        PromptTemplate(
            id = "env_attic",
            name = "Vintage Attic",
            category = PromptTemplateCategory.ENVIRONMENT,
            prompt = "cozy attic room, sloped ceiling, vintage furniture, old wooden beams, warm lighting, trunk chest, dusty books, skylight window, nostalgic atmosphere, afternoon sunlight, photorealistic",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, modern, clean, ugly"
        ),
        PromptTemplate(
            id = "env_rooftop",
            name = "City Rooftop",
            category = PromptTemplateCategory.ENVIRONMENT,
            prompt = "rooftop terrace, city skyline background, string lights, comfortable seating, potted plants, evening atmosphere, golden hour lighting, urban landscape, modern furniture, photorealistic",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, rural, ugly, dirty"
        ),
        PromptTemplate(
            id = "env_greenhouse",
            name = "Botanical Greenhouse",
            category = PromptTemplateCategory.ENVIRONMENT,
            prompt = "botanical greenhouse, glass ceiling, exotic plants, tropical flowers, Victorian architecture, stone floor, watering can, humid atmosphere, filtered sunlight, lush greenery, photorealistic",
            negativePrompt = "lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, modern, ugly, desert"
        )
    )

    // ============ CHARACTER TEMPLATES ============
    val CHARACTER_TEMPLATES = listOf(
        PromptTemplate(
            id = "char_warrior",
            name = "Fantasy Warrior",
            category = PromptTemplateCategory.CHARACTER,
            prompt = "masterpiece, best quality, 1girl, warrior outfit, detailed armor, sword, battle-worn appearance, dramatic pose, fantasy setting, cinematic lighting, ultra detailed, digital art",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn, bad proportions, blurry, distorted, worst quality, low quality"
        ),
        PromptTemplate(
            id = "char_elf",
            name = "Elven Archer",
            category = PromptTemplateCategory.CHARACTER,
            prompt = "masterpiece, best quality, 1girl, elf character, pointy ears, elegant bow, forest background, leafy crown, green and brown robes, nature magic aura, fantasy, cinematic, ultra detailed",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn, bad proportions, blurry, distorted, worst quality, low quality"
        ),
        PromptTemplate(
            id = "char_knight",
            name = "Paladin Knight",
            category = PromptTemplateCategory.CHARACTER,
            prompt = "masterpiece, best quality, 1girl, paladin knight, holy armor, glowing sword, cape, shield with emblem, cathedral background, divine light, fantasy RPG style, ultra detailed",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn, bad proportions, blurry, distorted, worst quality, low quality"
        ),
        PromptTemplate(
            id = "char_ninja",
            name = "Ninja Assassin",
            category = PromptTemplateCategory.CHARACTER,
            prompt = "masterpiece, best quality, 1girl, ninja outfit, dark clothing, katana, masked face, hidden identity, moonlit rooftop, throwing stars, stealth pose, Japanese architecture background, anime style",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn, bad proportions, blurry, distorted, worst quality, low quality"
        ),
        PromptTemplate(
            id = "char_queen",
            name = "Fantasy Queen",
            category = PromptTemplateCategory.CHARACTER,
            prompt = "masterpiece, best quality, 1girl, queen attire, royal crown, elegant gown, jewels, throne room background, royal guards in distance, majestic atmosphere, regal pose, fantasy royalty",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn, bad proportions, blurry, distorted, worst quality, low quality"
        ),
        PromptTemplate(
            id = "char_pirate",
            name = "Pirate Captain",
            category = PromptTemplateCategory.CHARACTER,
            prompt = "masterpiece, best quality, 1girl, pirate outfit, tricorn hat, coat with gold trim, pistol, treasure map, ship deck background, ocean view, adventurous pose, swashbuckling style",
            negativePrompt = "lowres, bad anatomy, bad hands, missing fingers, extra fingers, bad arms, missing legs, poorly drawn, bad proportions, blurry, distorted, worst quality, low quality"
        )
    )

    // ============ STYLE TEMPLATES ============
    val STYLE_TEMPLATES = listOf(
        PromptTemplate(
            id = "style_oil_painting",
            name = "Oil Painting Style",
            category = PromptTemplateCategory.STYLE,
            prompt = "oil painting style, masterpiece, classic art technique, visible brushstrokes, rich textures, dramatic chiaroscuro lighting, museum quality, Renaissance inspired, artistic painting",
            negativePrompt = "photograph, realistic, lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, digital art, cartoon"
        ),
        PromptTemplate(
            id = "style_watercolor",
            name = "Watercolor Style",
            category = PromptTemplateCategory.STYLE,
            prompt = "watercolor painting, soft edges, flowing colors, delicate washes, artistic illustration style, dreamy atmosphere, hand-painted feel, watercolor on paper texture, elegant",
            negativePrompt = "photograph, realistic, lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, harsh edges, digital art"
        ),
        PromptTemplate(
            id = "style_sketch",
            name = "Pencil Sketch Style",
            category = PromptTemplateCategory.STYLE,
            prompt = "pencil sketch, hand-drawn, detailed linework, shading, graphite on paper texture, artistic sketch style, monochrome, delicate strokes, professional drawing",
            negativePrompt = "color, photograph, lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, flat"
        ),
        PromptTemplate(
            id = "style_pixel_art",
            name = "Pixel Art Style",
            category = PromptTemplateCategory.STYLE,
            prompt = "pixel art, 16-bit style, retro game aesthetic, detailed sprites, nostalgic video game feel, vibrant colors, chiptune music vibes, nostalgic gaming",
            negativePrompt = "photograph, realistic, smooth, lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, non-pixel"
        ),
        PromptTemplate(
            id = "style_comic",
            name = "Comic Book Style",
            category = PromptTemplateCategory.STYLE,
            prompt = "comic book art, bold outlines, halftone dots, dynamic action lines, vibrant colors, Marvel/DC inspired style, dramatic poses, comic panel composition, inked art",
            negativePrompt = "photograph, realistic, lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, smooth"
        ),
        PromptTemplate(
            id = "style_ink_wash",
            name = "Chinese Ink Wash",
            category = PromptTemplateCategory.STYLE,
            prompt = "chinese ink wash painting style, shuimo hua, traditional east asian art, minimalist, elegant brushwork, black ink gradients, rice paper texture, zen aesthetic",
            negativePrompt = "color, photograph, realistic, lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, western art style"
        ),
        PromptTemplate(
            id = "style_steampunk",
            name = "Steampunk Style",
            category = PromptTemplateCategory.STYLE,
            prompt = "steampunk art style, vintage machinery, brass and copper tones, gears and cogs, Victorian era technology, airships, goggles, brown and orange color palette, industrial fantasy",
            negativePrompt = "modern, lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, futuristic, sci-fi"
        ),
        PromptTemplate(
            id = "style_art_nouveau",
            name = "Art Nouveau Style",
            category = PromptTemplateCategory.STYLE,
            prompt = "art nouveau style, elegant curves, organic lines, decorative flourishes, Alphonse Mucha inspired, ornamental frames, pastel colors, vintage poster aesthetic",
            negativePrompt = "modern, lowres, bad anatomy, text, error, worst quality, low quality, jpeg artifacts, blurry, deformed, minimalist, stark"
        )
    )

    fun getAllTemplates(): List<PromptTemplate> =
        ANIME_TEMPLATES + REALISTIC_TEMPLATES + LANDSCAPE_TEMPLATES + PORTRAIT_TEMPLATES + 
        NSFW_TEMPLATES + ENVIRONMENT_TEMPLATES + CHARACTER_TEMPLATES + STYLE_TEMPLATES

    fun getTemplatesByCategory(category: PromptTemplateCategory): List<PromptTemplate> =
        when (category) {
            PromptTemplateCategory.ANIME -> ANIME_TEMPLATES
            PromptTemplateCategory.REALISTIC -> REALISTIC_TEMPLATES
            PromptTemplateCategory.LANDSCAPE -> LANDSCAPE_TEMPLATES
            PromptTemplateCategory.PORTRAIT -> PORTRAIT_TEMPLATES
            PromptTemplateCategory.NSFW -> NSFW_TEMPLATES
            PromptTemplateCategory.NSFW_TEMPLATES -> NSFW_TEMPLATES
            PromptTemplateCategory.ENVIRONMENT -> ENVIRONMENT_TEMPLATES
            PromptTemplateCategory.CHARACTER -> CHARACTER_TEMPLATES
            PromptTemplateCategory.STYLE -> STYLE_TEMPLATES
            PromptTemplateCategory.CUSTOM -> NSFW_TEMPLATES
        }

    fun getTemplateById(id: String): PromptTemplate? =
        getAllTemplates().find { it.id == id }
}