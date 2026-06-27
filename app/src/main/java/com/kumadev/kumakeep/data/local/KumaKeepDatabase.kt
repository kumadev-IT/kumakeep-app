package com.kumadev.kumakeep.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kumadev.kumakeep.data.local.converter.Converters
import com.kumadev.kumakeep.data.local.dao.BoardGameDao
import com.kumadev.kumakeep.data.local.dao.LibraryDao
import com.kumadev.kumakeep.data.local.dao.RulebookDao
import com.kumadev.kumakeep.data.local.dao.WishlistDao
import com.kumadev.kumakeep.data.local.entity.BoardGameEntity
import com.kumadev.kumakeep.data.local.entity.LibraryEntity
import com.kumadev.kumakeep.data.local.entity.RulebookEntity
import com.kumadev.kumakeep.data.local.entity.WishlistEntity
import com.kumadev.kumakeep.data.local.entity.WishlistEntryEntity

@Database(
    entities = [
        BoardGameEntity::class,
        LibraryEntity::class,
        WishlistEntity::class,
        WishlistEntryEntity::class,
        RulebookEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KumaKeepDatabase : RoomDatabase() {
    abstract fun boardGameDao(): BoardGameDao
    abstract fun libraryDao(): LibraryDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun rulebookDao(): RulebookDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE wishlist_entries ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS rulebooks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        gameId INTEGER NOT NULL,
                        filePath TEXT NOT NULL,
                        fileName TEXT NOT NULL,
                        pageCount INTEGER NOT NULL,
                        sizeBytes INTEGER NOT NULL,
                        importedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_rulebooks_gameId ON rulebooks (gameId)"
                )
            }
        }
    }
}