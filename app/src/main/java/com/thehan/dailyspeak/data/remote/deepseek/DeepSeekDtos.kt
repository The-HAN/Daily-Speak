package com.thehan.dailyspeak.data.remote.deepseek

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    val stream: Boolean = false,
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class ChatCompletionResponse(
    val id: String = "",
    val model: String = "",
    val choices: List<ChatChoice> = emptyList(),
)

@Serializable
data class ChatChoice(
    val index: Int = 0,
    val message: ChatResponseMessage,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class ChatResponseMessage(
    val role: String = "assistant",
    val content: String = "",
)

@Serializable
data class DeepSeekErrorResponse(
    val error: DeepSeekError? = null,
)

@Serializable
data class DeepSeekError(
    val message: String = "",
    val type: String = "",
    val code: String? = null,
)
