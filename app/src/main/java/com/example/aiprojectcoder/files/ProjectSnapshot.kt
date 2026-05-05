package com.example.aiprojectcoder.files

import kotlinx.serialization.Serializable

@Serializable
data class ProjectFile(
    val path: String,
    val content: String
)

@Serializable
data class ProjectSnapshot(
    val rootName: String,
    val files: List<ProjectFile>
) {
    fun treeText(): String = files.joinToString("\n") { it.path }
}
