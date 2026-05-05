package com.example.aiprojectcoder.data

import kotlinx.serialization.Serializable

@Serializable
data class ModelConfig(
    val provider: ProviderType = ProviderType.OPENAI_COMPATIBLE,
    val displayName: String = "OpenAI compatible",
    val model: String = "gpt-4.1",
    val baseUrl: String = "https://api.openai.com/v1/chat/completions"
)
