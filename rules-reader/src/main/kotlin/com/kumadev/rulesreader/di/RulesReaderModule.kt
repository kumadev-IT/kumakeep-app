package com.kumadev.rulesreader.di

import android.content.Context
import androidx.room.Room
import com.kumadev.rulesreader.db.RulesReaderDatabase
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RulesReaderModule {

    @Provides
    @Singleton
    fun provideRulesReaderDatabase(
        @ApplicationContext context: Context
    ): RulesReaderDatabase {
        // Inizializza PdfBox (richiede il contesto applicazione)
        PDFBoxResourceLoader.init(context)

        return Room.databaseBuilder(
            context,
            RulesReaderDatabase::class.java,
            RulesReaderDatabase.DATABASE_NAME
        ).build()
    }
}
