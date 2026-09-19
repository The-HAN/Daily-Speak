package com.thehan.dailyspeak.data.remote.deepseek

import com.thehan.dailyspeak.BuildConfig
import com.thehan.dailyspeak.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class DeepSeekRemoteDataSource @Inject constructor(
    private val api: DeepSeekApi,
    private val settingsRepository: SettingsRepository,
    private val json: Json,
) {
    suspend fun complete(
        systemPrompt: String,
        userPrompt: String,
        temperature: Double,
    ): String {
        val settings = settingsRepository.settings.first()
        val apiKey = settings.deepSeekApiKey
            .ifBlank { BuildConfig.DEEPSEEK_API_KEY }
            .trim()
        check(apiKey.isNotBlank()) {
            "尚未配置 DeepSeek API Key。请前往“我的”页面配置。"
        }

        val model = BuildConfig.DEEPSEEK_MODEL.trim()
        check(model.isNotBlank()) {
            "尚未配置 DeepSeek 模型名。请在 local.properties 中设置 DEEPSEEK_MODEL，并以官方文档为准。"
        }

        return try {
            val response = api.createChatCompletion(
                authorization = "Bearer $apiKey",
                request = ChatCompletionRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage(role = "system", content = systemPrompt),
                        ChatMessage(role = "user", content = userPrompt),
                    ),
                    temperature = temperature,
                    stream = false,
                ),
            )
            response.choices.firstOrNull()?.message?.content
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: error("DeepSeek 返回了空内容。")
        } catch (error: HttpException) {
            val message = runCatching {
                error.response()?.errorBody()?.string()?.let { body ->
                    json.decodeFromString<DeepSeekErrorResponse>(body).error?.message
                }
            }.getOrNull()
            throw IllegalStateException(
                message?.takeIf { it.isNotBlank() }
                    ?: "DeepSeek 请求失败（HTTP ${error.code()}）。",
                error,
            )
        }
    }
}
