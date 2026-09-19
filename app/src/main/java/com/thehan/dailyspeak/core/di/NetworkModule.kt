package com.thehan.dailyspeak.core.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.thehan.dailyspeak.BuildConfig
import com.thehan.dailyspeak.data.remote.deepseek.DeepSeekApi
import com.thehan.dailyspeak.data.remote.deepseek.DeepSeekServiceImpl
import com.thehan.dailyspeak.domain.service.DeepSeekService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
                redactHeader("Authorization")
            },
        )
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.DEEPSEEK_BASE_URL)
        .client(client)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType()),
        )
        .build()

    @Provides
    @Singleton
    fun provideDeepSeekApi(retrofit: Retrofit): DeepSeekApi =
        retrofit.create(DeepSeekApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DeepSeekServiceModule {
    @Binds
    @Singleton
    abstract fun bindDeepSeekService(
        service: DeepSeekServiceImpl,
    ): DeepSeekService
}
