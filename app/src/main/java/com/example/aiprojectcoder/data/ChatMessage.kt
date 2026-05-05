package com.example.aiprojectcoder.data

import kotlinx.serialization.Serializable

@Serializable
enum class ChatRole(val label: String) {
    USER("我"),
    ASSISTANT("AI")
}

@Serializable
data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val content: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val inputTokens: Long? = null,
    val outputTokens: Long? = null,
    val thinkingMillis: Long? = null,
    val thinkingSteps: List<String> = emptyList(),
    val rawModelOutput: String = ""
)
