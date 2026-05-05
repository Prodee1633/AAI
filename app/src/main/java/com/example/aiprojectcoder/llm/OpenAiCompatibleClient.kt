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
import io.ktor.http.HttpHeaders
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

class OpenAiCompatibleClient : LlmClient {
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
            put("model", config.model)
            put("temperature", 0.1)
            put("stream", false)
            put("messages", buildJsonArray {
                addJsonObject {
                    put("role", "system")
                    put("content", CodingPrompt.system())
                }
                addJsonObject {
                    put("role", "user")
                    put("content", CodingPrompt.user(snapshot, task))
                }
            })
        }

        val responseText: String = client.post(config.baseUrl) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(body)
        }.body()

        val root = json.parseToJsonElement(responseText).jsonObject
        return root["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.contentOrNull
            ?: responseText
    }
}
