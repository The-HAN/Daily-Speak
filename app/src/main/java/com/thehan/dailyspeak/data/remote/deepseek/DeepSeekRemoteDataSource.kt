package com.thehan.dailyspeak.data.remote.deepseek

import com.thehan.dailyspeak.BuildConfig
import com.thehan.dailyspeak.domain.repository.SettingsRepository
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
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
    suspend fun listModels(apiKeyOverride: String? = null): List<String> {
        val apiKey = resolveApiKey(apiKeyOverride)
        return try {
            api.listModels("Bearer $apiKey")
                .data.orEmpty()
                .map { it.id.trim() }
                .filter(String::isNotBlank)
                .distinct()
                .sorted()
        } catch (error: HttpException) {
            throw error.toUserFacingError("读取模型列表失败")
        } catch (error: SocketTimeoutException) {
            throw IllegalStateException(
                "读取模型列表超时，请检查网络后重试。",
                error,
            )
        } catch (error: InterruptedIOException) {
            throw IllegalStateException(
                "读取模型列表超时，请检查网络后重试。",
                error,
            )
        } catch (error: IOException) {
            throw IllegalStateException(
                "无法连接 DeepSeek 模型列表接口：${error.message ?: "网络异常"}。",
                error,
            )
        }
    }

    suspend fun complete(
        systemPrompt: String,
        userPrompt: String,
        temperature: Double,
    ): String {
        val apiKey = resolveApiKey()
        val model = resolveModel()

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
            throw error.toUserFacingError("DeepSeek 请求失败")
        } catch (error: SocketTimeoutException) {
            throw IllegalStateException(
                "DeepSeek 响应超时。当前模型生成内容较慢，请稍后重试，或改用其他可用模型。",
                error,
            )
        } catch (error: InterruptedIOException) {
            throw IllegalStateException(
                "DeepSeek 请求超时。请检查网络，或减少每日题量后重试。",
                error,
            )
        } catch (error: IOException) {
            throw IllegalStateException(
                "无法连接 DeepSeek：${error.message ?: "请检查网络和 API 地址"}。",
                error,
            )
        }
    }

    private suspend fun resolveApiKey(apiKeyOverride: String? = null): String =
        apiKeyOverride
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: settingsRepository.settings.first().deepSeekApiKey
                .ifBlank { BuildConfig.DEEPSEEK_API_KEY }
                .trim()
                .also { key ->
                    check(key.isNotBlank()) {
                        "尚未配置 DeepSeek API Key。请前往“我的 → DeepSeek API”配置。"
                    }
                }

    private suspend fun resolveModel(): String =
        settingsRepository.settings.first().deepSeekModel
            .ifBlank { BuildConfig.DEEPSEEK_MODEL }
            .trim()
            .also { model ->
                check(model.isNotBlank()) {
                    "尚未选择 DeepSeek 模型。请前往“我的 → DeepSeek API”保存 API Key 并选择可用模型。"
                }
            }

    private fun HttpException.toUserFacingError(prefix: String): IllegalStateException {
        val message = runCatching {
            response()?.errorBody()?.string()?.let { body ->
                json.decodeFromString<DeepSeekErrorResponse>(body).error?.message
            }
        }.getOrNull()
        return IllegalStateException(
            message?.takeIf(String::isNotBlank)
                ?: "$prefix（HTTP ${code()}）。",
            this,
        )
    }
}