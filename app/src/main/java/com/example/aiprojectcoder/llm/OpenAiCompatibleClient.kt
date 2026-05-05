package com.example.aiprojectcoder.llm

import com.example.aiprojectcoder.data.ModelConfig
import com.example.aiprojectcoder.files.ProjectSnapshot
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class OpenAiCompatibleClient : LlmClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 10 * 60 * 1000L
            connectTimeoutMillis = 30 * 1000L
            socketTimeoutMillis = 10 * 60 * 1000L
        }
    }

    override suspend fun requestPatch(
        config: ModelConfig,
        apiKey: String,
        snapshot: ProjectSnapshot,
        task: String,
        attachments: List<PromptAttachment>
    ): LlmResponse {
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
                    val imageAttachments = attachments.filter { it.isImage && it.base64Content != null }
                    if (imageAttachments.isEmpty()) {
                        put("content", CodingPrompt.user(snapshot, task, attachments))
                    } else {
                        put("content", buildJsonArray {
                            addJsonObject {
                                put("type", "text")
                                put("text", CodingPrompt.user(snapshot, task, attachments))
                            }
                            imageAttachments.forEach { attachment ->
                                addJsonObject {
                                    put("type", "image_url")
                                    put("image_url", buildJsonObject {
                                        put("url", "data:${attachment.mimeType};base64,${attachment.base64Content}")
                                    })
                                }
                            }
                        })
                    }
                }
            })
        }

        val responseText: String = client.post(config.baseUrl) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            setBody(body)
        }.body()

        val root = json.parseToJsonElement(responseText).jsonObject
        val content = root["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.contentOrNull
            ?: responseText
        val usage = root["usage"]?.jsonObject
        return LlmResponse(
            content = content,
            inputTokens = usage?.get("prompt_tokens")?.jsonPrimitive?.longOrNull
                ?: usage?.get("input_tokens")?.jsonPrimitive?.longOrNull,
            outputTokens = usage?.get("completion_tokens")?.jsonPrimitive?.longOrNull
                ?: usage?.get("output_tokens")?.jsonPrimitive?.longOrNull,
            rawResponse = responseText
        )
    }
}
