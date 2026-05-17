package io.github.xororz.localdream.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TagRecommender(private val repository: TagAutocompleteRepository) {

    data class Recommendation(
        val tag: String,
        val reason: Reason
    )

    enum class Reason {
        POPULARITY,
        QUALITY_MISSING,
        CATEGORY_BALANCE,
        GENDER_MISSING
    }

    suspend fun recommend(currentPrompt: String, limit: Int = 8): List<Recommendation> = withContext(Dispatchers.Default) {
        val entries = repository.getAllEntries()
        if (entries.isEmpty()) return@withContext emptyList()

        val promptTags = parsePrompt(currentPrompt).toSet()
        val presentEntries = entries.filter { promptTags.contains(it.english) }

        val recommendations = mutableListOf<Recommendation>()

        // 1. Quality tags missing
        val qualityTags = listOf("masterpiece", "best quality", "highres")
        val hasQuality = promptTags.any { qualityTags.contains(it.lowercase()) }
        if (!hasQuality) {
            qualityTags.firstOrNull { tag ->
                entries.any { it.normalizedEnglish == tag }
            }?.let { quality ->
                recommendations += Recommendation(quality, Reason.QUALITY_MISSING)
            }
        }

        // 2. Gender suggestion if none present
        val genderTags = listOf("1girl", "1boy", "2girls", "multiple girls", "many")
        val hasGender = presentEntries.any { entry ->
            genderTags.any { gender -> 
                entry.normalizedEnglish.contains(gender) || entry.english.lowercase().contains(gender)
            }
        }
        if (!hasGender) {
            val topGirl = entries.firstOrNull { it.normalizedEnglish == "1girl" }
            topGirl?.let {
                recommendations += Recommendation(it.english, Reason.GENDER_MISSING)
            }
        }

        // 3. Category balance
        val categoryWeights = presentEntries.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { repository.popularityScore(it.postCount) } }
        
        if (categoryWeights.isNotEmpty()) {
            val dominantCategory = categoryWeights.entries.maxByOrNull { it.value }?.key
            dominantCategory?.let { cat ->
                val topUnused = entries
                    .filter { it.category == cat && !promptTags.contains(it.english) }
                    .sortedWith(compareByDescending<TagEntry> { repository.popularityScore(it.postCount) })
                    .take(3)
                for (entry in topUnused) {
                    recommendations += Recommendation(entry.english, Reason.CATEGORY_BALANCE)
                }
            }
        }

        // 4. Popular tags
        val topPopular = entries
            .filter { !promptTags.contains(it.english) && it.postCount >= 100_000 }
            .sortedWith(compareByDescending<TagEntry> { repository.popularityScore(it.postCount) })
            .take(2)
        for (entry in topPopular) {
            recommendations += Recommendation(entry.english, Reason.POPULARITY)
        }

        return@withContext recommendations
            .distinctBy { it.tag }
            .take(limit)
    }

    private fun parsePrompt(prompt: String): List<String> {
        return prompt.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
