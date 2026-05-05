package com.example.aiprojectcoder.data

import android.content.Context
import android.net.Uri
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val secrets = SecretStore(appContext)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun readConfig(): ModelConfig {
        val raw = prefs.getString("model_config", null) ?: return ModelConfig()
        return runCatching { json.decodeFromString<ModelConfig>(raw) }.getOrDefault(ModelConfig())
    }

    fun saveConfig(config: ModelConfig) {
        prefs.edit().putString("model_config", json.encodeToString(config)).apply()
    }

    fun readApiKey(): String = secrets.readApiKey("active")

    fun saveApiKey(apiKey: String) = secrets.saveApiKey("active", apiKey)

    fun readProjectUri(): Uri? = prefs.getString("project_uri", null)?.let(Uri::parse)

    fun saveProjectUri(uri: Uri) {
        prefs.edit().putString("project_uri", uri.toString()).apply()
    }
}
