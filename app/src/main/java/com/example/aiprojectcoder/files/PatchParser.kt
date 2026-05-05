package com.example.aiprojectcoder.files

import kotlinx.serialization.json.Json

object PatchParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): PatchPlan {
        val candidate = extractJsonObject(text)
        return json.decodeFromString<PatchPlan>(candidate)
    }

    private fun extractJsonObject(text: String): String {
        val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
            .find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
        if (!fenced.isNullOrBlank() && fenced.startsWith("{")) return fenced

        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        require(start >= 0 && end > start) { "模型没有返回 JSON PatchPlan。" }
        return text.substring(start, end + 1)
    }
}
