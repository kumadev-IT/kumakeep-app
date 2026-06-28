package com.kumadev.rulesreader.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kumadev.rulesreader.db.dao.ExtractedPageDao
import com.kumadev.rulesreader.db.dao.GeneratedScreenDao
import com.kumadev.rulesreader.db.dao.RulebookChunkDao
import com.kumadev.rulesreader.db.entity.ExtractedPageEntity
import com.kumadev.rulesreader.db.entity.GeneratedScreenEntity
import com.kumadev.rulesreader.db.entity.RulebookChunkEntity

@Database(
    entities = [
        ExtractedPageEntity::class,
        RulebookChunkEntity::class,
        GeneratedScreenEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class RulesReaderDatabase : RoomDatabase() {

    abstract fun extractedPageDao(): ExtractedPageDao
    abstract fun rulebookChunkDao(): RulebookChunkDao
    abstract fun generatedScreenDao(): GeneratedScreenDao

    companion object {
        const val DATABASE_NAME = "rules_reader.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `generated_screens` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `rulebookId` INTEGER NOT NULL,
                        `screenIndex` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `sourcePageNums` TEXT NOT NULL,
                        `generatedAt` INTEGER NOT NULL,
                        `rulesReaderVersion` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS
                    `index_generated_screens_rulebookId_screenIndex`
                    ON `generated_screens` (`rulebookId`, `screenIndex`)
                    """.trimIndent()
                )
            }
        }

        /**
         * v2 → v3: aggiunge la colonna `sectionType` ai chunk (Structure-Aware Chunker, Fase 1).
         * Nullable: i chunk pre-esistenti restano a NULL finché il regolamento non viene ri-elaborato.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `rulebook_chunks` ADD COLUMN `sectionType` TEXT"
                )
            }
        }
    }
}
