package com.example.aiprojectcoder.llm

data class LlmResponse(
    val content: String,
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val rawResponse: String = content
)
