package com.kumadev.kumakeep.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kumadev.kumakeep.data.local.converter.Converters
import com.kumadev.kumakeep.data.local.dao.BoardGameDao
import com.kumadev.kumakeep.data.local.dao.LibraryDao
import com.kumadev.kumakeep.data.local.dao.OwnedExpansionDao
import com.kumadev.kumakeep.data.local.dao.RulebookDao
import com.kumadev.kumakeep.data.local.dao.TagDao
import com.kumadev.kumakeep.data.local.dao.WishlistDao
import com.kumadev.kumakeep.data.local.entity.BoardGameEntity
import com.kumadev.kumakeep.data.local.entity.GameTagEntity
import com.kumadev.kumakeep.data.local.entity.LibraryEntity
import com.kumadev.kumakeep.data.local.entity.OwnedExpansionEntity
import com.kumadev.kumakeep.data.local.entity.RulebookEntity
import com.kumadev.kumakeep.data.local.entity.TagEntity
import com.kumadev.kumakeep.data.local.entity.WishlistEntity
import com.kumadev.kumakeep.data.local.entity.WishlistEntryEntity

@Database(
    entities = [
        BoardGameEntity::class,
        LibraryEntity::class,
        WishlistEntity::class,
        WishlistEntryEntity::class,
        RulebookEntity::class,
        OwnedExpansionEntity::class,
        TagEntity::class,
        GameTagEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KumaKeepDatabase : RoomDatabase() {
    abstract fun boardGameDao(): BoardGameDao
    abstract fun libraryDao(): LibraryDao
    abstract fun wishlistDao(): WishlistDao
    abstract fun rulebookDao(): RulebookDao
    abstract fun ownedExpansionDao(): OwnedExpansionDao
    abstract fun tagDao(): TagDao

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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE boardgames ADD COLUMN isExpansion INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE boardgames ADD COLUMN baseGamesRef TEXT"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS owned_expansions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        expansionBggId INTEGER NOT NULL,
                        baseBggId INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(expansionBggId) REFERENCES boardgames(bggId) ON DELETE CASCADE,
                        FOREIGN KEY(baseBggId) REFERENCES boardgames(bggId) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_owned_expansions_expansionBggId_baseBggId ON owned_expansions (expansionBggId, baseBggId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_owned_expansions_baseBggId ON owned_expansions (baseBggId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_owned_expansions_expansionBggId ON owned_expansions (expansionBggId)"
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags (name)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS game_tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bggId INTEGER NOT NULL,
                        tagId INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY(bggId) REFERENCES boardgames(bggId) ON DELETE CASCADE,
                        FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_game_tags_bggId_tagId ON game_tags (bggId, tagId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_game_tags_tagId ON game_tags (tagId)"
                )
            }
        }
    }
}