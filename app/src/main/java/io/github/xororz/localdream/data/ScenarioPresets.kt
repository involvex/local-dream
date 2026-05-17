package io.github.xororz.localdream.data

data class Scenario(
    val name: String,
    val tags: String
)

object ScenarioPresets {
    val ALL = listOf(
        Scenario("Portrait (Girl)", "1girl, solo, masterpiece, best quality, highres"),
        Scenario("Portrait (Boy)", "1boy, solo, masterpiece, best quality, highres"),
        Scenario("Group", "2girls, 1boy, solo, masterpiece, best quality"),
        Scenario("Landscape", "landscape, scenery, masterpiece, best quality, highres"),
        Scenario("Realistic", "photorealistic, portrait, masterpiece, best quality"),
        Scenario("Anime Style", "anime, masterpiece, best quality, vibrant"),
        Scenario("Chibi", "chibi, cute, masterpiece, best quality"),
        Scenario("Full Body", "full body, 1girl, solo, masterpiece"),
        Scenario("Top 20 Tags", "1girl, highres, solo, long_hair, breasts, looking_at_viewer, blush, smile, open_mouth, short_hair, simple_background, shirt, absurdres, blue_eyes, large_breasts, long_sleeves, skirt, blonde_hair, multiple_girls, black_hair")
    )
}
