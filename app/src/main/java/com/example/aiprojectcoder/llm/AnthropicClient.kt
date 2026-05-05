package com.example.aiprojectcoder.llm

import com.example.aiprojectcoder.data.ModelConfig
import com.example.aiprojectcoder.files.ProjectSnapshot
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class AnthropicClient : LlmClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    override suspend fun requestPatch(
        config: ModelConfig,
        apiKey: String,
        snapshot: ProjectSnapshot,
        task: String
    ): String {
        val body = buildJsonObject {
            put("model", config.model.ifBlank { "claude-sonnet-4-5" })
            put("max_tokens", 8192)
            put("temperature", 0.1)
            put("system", CodingPrompt.system())
            put("messages", buildJsonArray {
                addJsonObject {
                    put("role", "user")
                    put("content", CodingPrompt.user(snapshot, task))
                }
            })
        }

        val responseText: String = client.post("https://api.anthropic.com/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(body)
        }.body()

        val root = json.parseToJsonElement(responseText).jsonObject
        return root["content"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.contentOrNull
            ?: responseText
    }
}
