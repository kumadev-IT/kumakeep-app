package com.kumadev.kumakeep.di

import com.kumadev.kumakeep.BuildConfig
import com.kumadev.kumakeep.network.gemini.GeminiFlashClient
import com.kumadev.rulesreader.llm.LlmClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

/**
 * Modulo Hilt che lega [LlmClient] a [GeminiFlashClient].
 *
 * Separato da NetworkModule per mantenere la responsabilità chiara:
 * questo modulo fornisce SOLO i binding relativi all'LLM cloud.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GeminiModule {

    @Binds
    @Singleton
    abstract fun bindLlmClient(impl: GeminiFlashClient): LlmClient

    companion object {
        @Provides
        @Named("gemini_api_key")
        fun provideGeminiApiKey(): String = BuildConfig.GEMINI_API_KEY
    }
}
