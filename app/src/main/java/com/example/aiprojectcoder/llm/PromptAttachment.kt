package com.example.aiprojectcoder.llm

data class PromptAttachment(
    val name: String,
    val mimeType: String,
    val byteSize: Long,
    val textContent: String? = null,
    val base64Content: String? = null
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isPdf: Boolean get() = mimeType.equals("application/pdf", ignoreCase = true)
    val canSendInlineBinary: Boolean get() = base64Content != null && (isImage || isPdf)
}
