package com.kumadev.kumakeep.di

import android.content.Context
import androidx.room.Room
import com.kumadev.kumakeep.data.local.KumaKeepDatabase
import com.kumadev.kumakeep.data.local.dao.BoardGameDao
import com.kumadev.kumakeep.data.local.dao.LibraryDao
import com.kumadev.kumakeep.data.local.dao.WishlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KumaKeepDatabase {
        return Room.databaseBuilder(
            context,
            KumaKeepDatabase::class.java,
            "kumakeep.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideBoardGameDao(db: KumaKeepDatabase): BoardGameDao = db.boardGameDao()

    @Provides
    @Singleton
    fun provideLibraryDao(db: KumaKeepDatabase): LibraryDao = db.libraryDao()

    @Provides
    @Singleton
    fun provideWishlistDao(db: KumaKeepDatabase): WishlistDao = db.wishlistDao()
}