package com.example.aiprojectcoder.llm

import com.example.aiprojectcoder.data.ModelConfig
import com.example.aiprojectcoder.files.ProjectSnapshot
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

class GeminiClient : LlmClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
    }

    override suspend fun requestPatch(
        config: ModelConfig,
        apiKey: String,
        snapshot: ProjectSnapshot,
        task: String,
        attachments: List<PromptAttachment>
    ): LlmResponse {
        val prompt = CodingPrompt.system() + "\n\n" + CodingPrompt.user(snapshot, task, attachments)
        val inlineAttachments = attachments.filter { it.canSendInlineBinary }
        val body = buildJsonObject {
            put("contents", buildJsonArray {
                addJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        addJsonObject { put("text", prompt) }
                        inlineAttachments.forEach { attachment ->
                            addJsonObject {
                                put("inlineData", buildJsonObject {
                                    put("mimeType", attachment.mimeType)
                                    put("data", attachment.base64Content ?: "")
                                })
                            }
                        }
                    })
                }
            })
            put("generationConfig", buildJsonObject {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            })
        }
        val model = config.model.ifBlank { "gemini-2.5-pro" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val responseText: String = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

        val root = json.parseToJsonElement(responseText).jsonObject
        val content = root["candidates"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("content")
            ?.jsonObject
            ?.get("parts")
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.contentOrNull
            ?: responseText
        val usage = root["usageMetadata"]?.jsonObject
        return LlmResponse(
            content = content,
            inputTokens = usage?.get("promptTokenCount")?.jsonPrimitive?.longOrNull,
            outputTokens = usage?.get("candidatesTokenCount")?.jsonPrimitive?.longOrNull,
            rawResponse = responseText
        )
    }
}
