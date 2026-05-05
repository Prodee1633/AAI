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
        - Prefer small, targeted changes.
        - For op=write, content must be the complete replacement file content.
        - Do not include Markdown fences unless the JSON is the only fenced block.
        - Do not modify generated build folders, .git, .gradle, build, node_modules, or binary files.
    """.trimIndent()

    fun user(snapshot: ProjectSnapshot, task: String): String {
        val files = snapshot.files.joinToString("\n\n") { file ->
            "--- FILE: ${file.path} ---\n${file.content}"
        }
        return """
            Project root: ${snapshot.rootName}

            File tree:
            ${snapshot.treeText()}

            Task:
            $task

            Current files:
            $files
        """.trimIndent()
    }
}
