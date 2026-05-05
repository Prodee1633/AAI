package com.example.aiprojectcoder.data

import kotlinx.serialization.Serializable

@Serializable
data class ProjectProfile(
    val id: String,
    val name: String,
    val uri: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastOpenedAtMillis: Long = System.currentTimeMillis()
)
