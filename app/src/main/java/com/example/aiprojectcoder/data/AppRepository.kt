package com.example.aiprojectcoder.data

import android.content.Context
import android.net.Uri
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val secrets = SecretStore(appContext)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun readProjects(): List<ProjectProfile> {
        val raw = prefs.getString("projects", null)
        val stored = raw?.let { runCatching { json.decodeFromString<List<ProjectProfile>>(it) }.getOrNull() }.orEmpty()
        if (stored.isNotEmpty()) return stored

        val oldUri = prefs.getString("project_uri", null) ?: return emptyList()
        val project = ProjectProfile(
            id = "legacy-project",
            name = "Legacy Project",
            uri = oldUri
        )
        saveProjects(listOf(project))
        saveActiveProjectId(project.id)
        return listOf(project)
    }

    fun saveProjects(projects: List<ProjectProfile>) {
        prefs.edit().putString("projects", json.encodeToString(projects)).apply()
    }

    fun readActiveProjectId(): String? = prefs.getString("active_project_id", null)

    fun saveActiveProjectId(projectId: String?) {
        val editor = prefs.edit()
        if (projectId == null) editor.remove("active_project_id") else editor.putString("active_project_id", projectId)
        editor.apply()
    }

    fun readModelConfigs(): List<ModelConfig> {
        val raw = prefs.getString("model_configs", null)
        val stored = raw?.let { runCatching { json.decodeFromString<List<ModelConfig>>(it) }.getOrNull() }.orEmpty()
        if (stored.isNotEmpty()) return stored

        val oldRaw = prefs.getString("model_config", null)
        val migrated = oldRaw
            ?.let { runCatching { json.decodeFromString<ModelConfig>(it) }.getOrNull() }
            ?.copy(id = "default")
            ?: ModelConfig(id = "default")
        saveModelConfigs(listOf(migrated))
        saveActiveModelConfigId(migrated.id)

        val oldKey = secrets.readApiKey("active")
        if (oldKey.isNotBlank()) secrets.saveApiKey(migrated.id, oldKey)
        return listOf(migrated)
    }

    fun saveModelConfigs(configs: List<ModelConfig>) {
        prefs.edit().putString("model_configs", json.encodeToString(configs)).apply()
    }

    fun readActiveModelConfigId(): String? = prefs.getString("active_model_config_id", null)

    fun saveActiveModelConfigId(configId: String?) {
        val editor = prefs.edit()
        if (configId == null) editor.remove("active_model_config_id") else editor.putString("active_model_config_id", configId)
        editor.apply()
    }

    fun newModelConfig(provider: ProviderType = ProviderType.OPENAI_COMPATIBLE): ModelConfig {
        val id = UUID.randomUUID().toString()
        return when (provider) {
            ProviderType.OPENAI_COMPATIBLE -> ModelConfig(
                provider = provider,
                displayName = "OpenAI compatible",
                model = "gpt-4.1",
                baseUrl = "https://api.openai.com/v1/chat/completions",
                id = id
            )
            ProviderType.GEMINI -> ModelConfig(
                provider = provider,
                displayName = "Gemini",
                model = "gemini-2.5-pro",
                baseUrl = "",
                id = id
            )
            ProviderType.ANTHROPIC -> ModelConfig(
                provider = provider,
                displayName = "Claude",
                model = "claude-sonnet-4-5",
                baseUrl = "",
                id = id
            )
        }
    }

    fun readApiKey(configId: String): String = secrets.readApiKey(configId)

    fun saveApiKey(configId: String, apiKey: String) = secrets.saveApiKey(configId, apiKey)

    fun clearApiKey(configId: String) = secrets.clearApiKey(configId)

    fun readProjectUri(): Uri? = readProjects()
        .firstOrNull { it.id == readActiveProjectId() }
        ?.uri
        ?.let(Uri::parse)

    fun saveProjectUri(uri: Uri) {
        val project = ProjectProfile(
            id = UUID.randomUUID().toString(),
            name = "Project",
            uri = uri.toString()
        )
        saveProjects(readProjects() + project)
        saveActiveProjectId(project.id)
    }

    fun readAutoApplyWithoutConfirmation(): Boolean = prefs.getBoolean("auto_apply_without_confirmation", false)

    fun saveAutoApplyWithoutConfirmation(enabled: Boolean) {
        prefs.edit().putBoolean("auto_apply_without_confirmation", enabled).apply()
    }

    fun readChatMessages(projectId: String): List<ChatMessage> {
        val raw = prefs.getString("chat_messages_$projectId", null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ChatMessage>>(raw) }.getOrDefault(emptyList())
    }

    fun saveChatMessages(projectId: String, messages: List<ChatMessage>) {
        prefs.edit().putString("chat_messages_$projectId", json.encodeToString(messages)).apply()
    }

    fun clearChatMessages(projectId: String) {
        prefs.edit().remove("chat_messages_$projectId").apply()
    }
}
