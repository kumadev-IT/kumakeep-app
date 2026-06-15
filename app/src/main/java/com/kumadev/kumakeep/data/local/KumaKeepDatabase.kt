package com.kumadev.kumakeep.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kumadev.kumakeep.data.local.converter.Converters
import com.kumadev.kumakeep.data.local.dao.BoardGameDao
import com.kumadev.kumakeep.data.local.dao.LibraryDao
import com.kumadev.kumakeep.data.local.dao.WishlistDao
import com.kumadev.kumakeep.data.local.entity.BoardGameEntity
import com.kumadev.kumakeep.data.local.entity.LibraryEntity
import com.kumadev.kumakeep.data.local.entity.WishlistEntity
import com.kumadev.kumakeep.data.local.entity.WishlistEntryEntity

@Database(
    entities = [
        BoardGameEntity::class,
        LibraryEntity::class,
        WishlistEntity::class,
        WishlistEntryEntity::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class KumaKeepDatabase : RoomDatabase() {
    abstract fun boardGameDao(): BoardGameDao
    abstract fun libraryDao(): LibraryDao
    abstract fun wishlistDao(): WishlistDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE wishlist_entries ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}