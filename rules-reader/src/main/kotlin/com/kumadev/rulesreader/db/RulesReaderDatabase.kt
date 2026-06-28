package com.kumadev.rulesreader.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kumadev.rulesreader.db.dao.ExtractedPageDao
import com.kumadev.rulesreader.db.dao.RulebookChunkDao
import com.kumadev.rulesreader.db.entity.ExtractedPageEntity
import com.kumadev.rulesreader.db.entity.RulebookChunkEntity

@Database(
    entities = [ExtractedPageEntity::class, RulebookChunkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RulesReaderDatabase : RoomDatabase() {

    abstract fun extractedPageDao(): ExtractedPageDao
    abstract fun rulebookChunkDao(): RulebookChunkDao

    companion object {
        const val DATABASE_NAME = "rules_reader.db"
    }
}
