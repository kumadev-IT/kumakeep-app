package com.kumadev.rulesreader.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kumadev.rulesreader.db.entity.RulebookChunkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RulebookChunkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chunk: RulebookChunkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(chunks: List<RulebookChunkEntity>)

    @Query("SELECT * FROM rulebook_chunks WHERE rulebookId = :rulebookId ORDER BY chunkIndex ASC")
    fun observeByRulebookId(rulebookId: Long): Flow<List<RulebookChunkEntity>>

    @Query("SELECT * FROM rulebook_chunks WHERE rulebookId = :rulebookId ORDER BY chunkIndex ASC")
    suspend fun getByRulebookId(rulebookId: Long): List<RulebookChunkEntity>

    @Query("SELECT COUNT(*) FROM rulebook_chunks WHERE rulebookId = :rulebookId")
    suspend fun countByRulebookId(rulebookId: Long): Int

    @Query("DELETE FROM rulebook_chunks WHERE rulebookId = :rulebookId")
    suspend fun deleteByRulebookId(rulebookId: Long)
}
