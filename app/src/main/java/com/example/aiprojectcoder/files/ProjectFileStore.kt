package com.example.aiprojectcoder.files

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader

class ProjectFileStore(private val context: Context, private val rootUri: Uri) {
    private val resolver = context.contentResolver
    private val root: DocumentFile = requireNotNull(DocumentFile.fromTreeUri(context, rootUri)) {
        "无法打开项目目录。请重新选择文件夹。"
    }

    fun snapshot(maxFiles: Int = 80, maxBytesPerFile: Int = 60_000): ProjectSnapshot {
        val out = mutableListOf<ProjectFile>()
        walk(root, "", out, maxFiles, maxBytesPerFile)
        return ProjectSnapshot(root.name ?: "project", out)
    }

    fun apply(plan: PatchPlan): List<String> {
        val logs = mutableListOf<String>()
        for (operation in plan.operations) {
            val path = cleanPath(operation.path)
            when (operation.op.lowercase()) {
                "write" -> {
                    writeText(path, operation.content.orEmpty())
                    logs += "WRITE $path"
                }
                "append" -> {
                    val existing = readText(path).getOrDefault("")
                    writeText(path, existing + operation.content.orEmpty())
                    logs += "APPEND $path"
                }
                "delete" -> {
                    delete(path)
                    logs += "DELETE $path"
                }
                "mkdir" -> {
                    ensureDirectory(path)
                    logs += "MKDIR $path"
                }
                else -> error("不支持的操作：${operation.op}")
            }
        }
        return logs
    }

    private fun walk(
        dir: DocumentFile,
        prefix: String,
        out: MutableList<ProjectFile>,
        maxFiles: Int,
        maxBytesPerFile: Int
    ) {
        if (out.size >= maxFiles) return
        val children = dir.listFiles().sortedWith(compareBy<DocumentFile> { !it.isDirectory }.thenBy { it.name.orEmpty() })
        for (child in children) {
            if (out.size >= maxFiles) return
            val name = child.name ?: continue
            if (shouldSkip(name)) continue
            val path = if (prefix.isBlank()) name else "$prefix/$name"
            if (child.isDirectory) {
                walk(child, path, out, maxFiles, maxBytesPerFile)
            } else if (isTextFile(name) && (child.length() in 0..maxBytesPerFile.toLong())) {
                val content = readText(child.uri).getOrDefault("")
                out += ProjectFile(path, content)
            }
        }
    }

    fun readText(path: String): Result<String> = runCatching {
        val file = find(cleanPath(path)) ?: error("文件不存在：$path")
        readText(file.uri).getOrThrow()
    }

    private fun readText(uri: Uri): Result<String> = runCatching {
        resolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        } ?: ""
    }

    private fun writeText(path: String, content: String) {
        val file = ensureFile(cleanPath(path))
        resolver.openOutputStream(file.uri, "wt")?.use { output ->
            output.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("无法写入文件：$path")
    }

    private fun delete(path: String) {
        val file = find(cleanPath(path)) ?: return
        if (!file.delete()) error("删除失败：$path")
    }

    private fun find(path: String): DocumentFile? {
        var current = root
        for (part in path.split('/').filter { it.isNotBlank() }) {
            current = current.findFile(part) ?: return null
        }
        return current
    }

    private fun ensureFile(path: String): DocumentFile {
        val parts = path.split('/').filter { it.isNotBlank() }
        require(parts.isNotEmpty()) { "路径不能为空" }
        val parentPath = parts.dropLast(1).joinToString("/")
        val parent = if (parentPath.isBlank()) root else ensureDirectory(parentPath)
        val name = parts.last()
        parent.findFile(name)?.let { return it }
        val mime = guessMime(name)
        return parent.createFile(mime, name) ?: error("创建文件失败：$path")
    }

    private fun ensureDirectory(path: String): DocumentFile {
        var current = root
        for (part in cleanPath(path).split('/').filter { it.isNotBlank() }) {
            val next = current.findFile(part)
            current = when {
                next == null -> current.createDirectory(part) ?: error("创建目录失败：$path")
                next.isDirectory -> next
                else -> error("路径已存在但不是目录：$part")
            }
        }
        return current
    }

    private fun cleanPath(path: String): String {
        val normalized = path.replace('\\', '/').trim().trimStart('/')
        require(!normalized.contains("..")) { "拒绝访问上级目录：$path" }
        return normalized
    }

    private fun shouldSkip(name: String): Boolean {
        val lower = name.lowercase()
        return lower in setOf(".git", ".gradle", "build", ".idea", "node_modules", ".DS_Store".lowercase()) ||
            lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
            lower.endsWith(".webp") || lower.endsWith(".gif") || lower.endsWith(".apk") ||
            lower.endsWith(".aab") || lower.endsWith(".jar") || lower.endsWith(".class") ||
            lower.endsWith(".so") || lower.endsWith(".zip")
    }

    private fun isTextFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".kt") || lower.endsWith(".java") || lower.endsWith(".xml") ||
            lower.endsWith(".gradle") || lower.endsWith(".kts") || lower.endsWith(".toml") ||
            lower.endsWith(".md") || lower.endsWith(".txt") || lower.endsWith(".json") ||
            lower.endsWith(".yaml") || lower.endsWith(".yml") || lower.endsWith(".properties") ||
            lower.endsWith(".html") || lower.endsWith(".css") || lower.endsWith(".js") ||
            lower.endsWith(".ts") || lower.endsWith(".py") || lower.endsWith(".rs") ||
            lower.endsWith(".go") || lower.endsWith(".c") || lower.endsWith(".cpp") ||
            lower.endsWith(".h") || lower.endsWith(".swift")
    }

    private fun guessMime(name: String): String = when {
        name.endsWith(".json") -> "application/json"
        name.endsWith(".xml") -> "application/xml"
        name.endsWith(".html") -> "text/html"
        else -> "text/plain"
    }
}
