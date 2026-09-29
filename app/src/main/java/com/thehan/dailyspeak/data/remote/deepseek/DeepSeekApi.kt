package com.thehan.dailyspeak.data.remote.deepseek

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface DeepSeekApi {
    @GET("models")
    suspend fun listModels(
        @Header("Authorization") authorization: String,
    ): DeepSeekModelsResponse

    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest,
    ): ChatCompletionResponse
}