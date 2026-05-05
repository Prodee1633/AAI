package com.example.aiprojectcoder.llm

import com.example.aiprojectcoder.data.ModelConfig
import com.example.aiprojectcoder.data.ProviderType
import com.example.aiprojectcoder.files.ProjectSnapshot

interface LlmClient {
    suspend fun requestPatch(config: ModelConfig, apiKey: String, snapshot: ProjectSnapshot, task: String): String
}

object LlmClientFactory {
    fun create(provider: ProviderType): LlmClient = when (provider) {
        ProviderType.OPENAI_COMPATIBLE -> OpenAiCompatibleClient()
        ProviderType.GEMINI -> GeminiClient()
        ProviderType.ANTHROPIC -> AnthropicClient()
    }
}
