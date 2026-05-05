package com.example.aiprojectcoder.files

import kotlinx.serialization.Serializable

@Serializable
data class PatchOperation(
    val op: String,
    val path: String,
    val content: String? = null
)

@Serializable
data class PatchPlan(
    val summary: String = "",
    val operations: List<PatchOperation> = emptyList()
)
