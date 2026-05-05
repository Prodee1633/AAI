package com.example.aiprojectcoder.llm

import com.example.aiprojectcoder.files.ProjectSnapshot

object CodingPrompt {
    fun system(): String = """
        You are an autonomous coding assistant running inside an Android app.
        You may edit only the user-selected project folder. Never use absolute paths.
        Return ONLY a JSON object matching this Kotlin data model:
        {
          "summary": "short human-readable summary",
          "operations": [
            {"op":"write|append|delete|mkdir", "path":"relative/path", "content":"full file text when needed"}
          ]
        }
        Rules:
        - Use single-step mode: return at most one small, atomic file operation in operations.
        - Do not batch a long chain of changes; choose the next safest single behavior needed for the user's request.
        - Prefer small, targeted changes.
        - For op=write, content must be the complete replacement file content.
        - Do not include Markdown fences unless the JSON is the only fenced block.
        - Do not modify generated build folders, .git, .gradle, build, node_modules, or binary files.
        - If attachments are provided, use them as context. Only reference file names in your summary when relevant.
    """.trimIndent()

    fun user(snapshot: ProjectSnapshot, task: String, attachments: List<PromptAttachment> = emptyList()): String {
        val files = snapshot.files.joinToString("\n\n") { file ->
            "--- FILE: ${file.path} ---\n${file.content}"
        }
        val attachmentText = attachments
            .filter { it.textContent != null }
            .joinToString("\n\n") { attachment ->
                "--- ATTACHMENT: ${attachment.name} (${attachment.mimeType}, ${attachment.byteSize} bytes) ---\n${attachment.textContent}"
            }
        val binaryText = attachments
            .filter { it.textContent == null }
            .joinToString("\n") { attachment ->
                "- ${attachment.name} (${attachment.mimeType}, ${attachment.byteSize} bytes)"
            }
        return buildString {
            appendLine("Project root: ${snapshot.rootName}")
            appendLine()
            appendLine("File tree:")
            appendLine(snapshot.treeText())
            appendLine()
            appendLine("Task:")
            appendLine(task.ifBlank { "Use the attached context to decide the requested code changes." })
            if (attachmentText.isNotBlank()) {
                appendLine()
                appendLine("Text attachments:")
                appendLine(attachmentText)
            }
            if (binaryText.isNotBlank()) {
                appendLine()
                appendLine("Binary or multimodal attachments also provided:")
                appendLine(binaryText)
            }
            appendLine()
            appendLine("Current files:")
            appendLine(files)
        }.trim()
    }
}
