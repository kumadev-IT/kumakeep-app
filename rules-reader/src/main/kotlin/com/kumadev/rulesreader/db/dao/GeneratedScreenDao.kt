package com.kumadev.rulesreader.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kumadev.rulesreader.db.entity.GeneratedScreenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeneratedScreenDao {

    @Query("SELECT * FROM generated_screens WHERE rulebookId = :rulebookId ORDER BY screenIndex ASC")
    fun observeByRulebookId(rulebookId: Long): Flow<List<GeneratedScreenEntity>>

    @Query("SELECT * FROM generated_screens WHERE rulebookId = :rulebookId ORDER BY screenIndex ASC")
    suspend fun getByRulebookId(rulebookId: Long): List<GeneratedScreenEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(screens: List<GeneratedScreenEntity>)

    @Query("DELETE FROM generated_screens WHERE rulebookId = :rulebookId")
    suspend fun deleteByRulebookId(rulebookId: Long)

    @Query("SELECT COUNT(*) FROM generated_screens WHERE rulebookId = :rulebookId")
    suspend fun countByRulebookId(rulebookId: Long): Int
}
