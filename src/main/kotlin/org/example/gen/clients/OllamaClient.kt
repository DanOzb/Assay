package org.example.gen.clients

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.example.gen.pbt.LlmRequest
import org.example.gen.pbt.LlmResponse
import org.example.gen.pbt.Role

class OllamaClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val defaultModel: String = DEFAULT_MODEL,
    private val httpClient: HttpClient = defaultHttpClient(),
) : AutoCloseable {

    suspend fun complete(request: LlmRequest): LlmResponse {
        val body = OllamaChatRequest(
            model = request.model ?: defaultModel,
            messages = request.messages.map { OllamaMessage(it.role.wire(), it.content) },
            stream = false,
            format = request.jsonSchema,
            options = OllamaOptions(temperature = request.temperature),
        )

        val response: OllamaChatResponse = httpClient.post("$baseUrl/api/chat") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

        return LlmResponse(model = response.model, content = response.message.content)
    }

    override fun close() = httpClient.close()

    private fun Role.wire(): String = when (this) {
        Role.SYSTEM -> "system"
        Role.USER -> "user"
        Role.ASSISTANT -> "assistant"
    }

    @Serializable
    private data class OllamaChatRequest(
        val model: String,
        val messages: List<OllamaMessage>,
        val stream: Boolean = false,
        val format: JsonElement? = null,
        val options: OllamaOptions? = null,
    )

    @Serializable
    private data class OllamaMessage(val role: String, val content: String)

    @Serializable
    private data class OllamaOptions(val temperature: Double)

    @Serializable
    private data class OllamaChatResponse(
        val model: String,
        val message: OllamaMessage,
        val done: Boolean = false,
    )

    companion object {
        const val DEFAULT_BASE_URL = "http://localhost:11434"
        const val DEFAULT_MODEL = "qwen3:14b"

        fun defaultHttpClient(): HttpClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        encodeDefaults = true
                        explicitNulls = false
                    }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 120_000
            }
        }
    }
}