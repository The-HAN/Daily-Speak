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
data class DeepSeekModelsResponse(
    @SerialName("object") val objectType: String = "list",
    val data: List<DeepSeekModelInfo>? = emptyList(),
)

@Serializable
data class DeepSeekModelInfo(
    val id: String = "",
    @SerialName("object") val objectType: String = "model",
    val created: Long = 0L,
    @SerialName("owned_by") val ownedBy: String = "",
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