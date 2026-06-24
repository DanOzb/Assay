package org.example.gen.pbt

import kotlinx.serialization.json.JsonObject

enum class Role { SYSTEM, USER, ASSISTANT }

data class LlmMessage(val role: Role, val content: String)

data class LlmRequest(
    val messages: List<LlmMessage>,
    val jsonSchema: JsonObject? = null,
    val temperature: Double = 0.2,
    val model: String? = null,
)

data class LlmResponse(
    val model: String,
    val content: String,
)