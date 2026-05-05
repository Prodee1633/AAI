package com.example.aiprojectcoder.data

import kotlinx.serialization.Serializable

@Serializable
enum class ProviderType(val label: String) {
    OPENAI_COMPATIBLE("OpenAI / OpenRouter / Mistral compatible"),
    GEMINI("Google Gemini"),
    ANTHROPIC("Anthropic Claude")
}
