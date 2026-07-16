package com.kumadev.kumakeep.di

import com.kumadev.kumakeep.network.ludomancer.MockLudomancerClient
import com.kumadev.rulesreader.ludomancer.LudomancerClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Modulo Hilt che lega [LudomancerClient] al mock corrente.
 *
 * Mirror di [GeminiModule]: quando il backend Ludomancer sarà pronto,
 * sostituire [MockLudomancerClient] con l'implementazione REST reale
 * (es. `LudomancerRestClient`) — è l'unica riga da cambiare.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class LudomancerModule {

    @Binds
    @Singleton
    abstract fun bindLudomancerClient(impl: MockLudomancerClient): LudomancerClient
}
