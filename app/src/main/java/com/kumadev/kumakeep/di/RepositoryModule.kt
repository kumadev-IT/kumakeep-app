package com.kumadev.kumakeep.di

import com.kumadev.kumakeep.data.repository.BoardGameRepositoryImpl
import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBoardGameRepository(
        impl: BoardGameRepositoryImpl
    ): BoardGameRepository
}