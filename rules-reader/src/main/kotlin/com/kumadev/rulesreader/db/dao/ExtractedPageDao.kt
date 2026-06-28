package com.kumadev.rulesreader.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kumadev.rulesreader.db.entity.ExtractedPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExtractedPageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<ExtractedPageEntity>)

    @Query("SELECT * FROM extracted_pages WHERE rulebookId = :rulebookId ORDER BY pageNum ASC")
    fun observeByRulebookId(rulebookId: Long): Flow<List<ExtractedPageEntity>>

    @Query("SELECT * FROM extracted_pages WHERE rulebookId = :rulebookId ORDER BY pageNum ASC")
    suspend fun getByRulebookId(rulebookId: Long): List<ExtractedPageEntity>

    @Query("SELECT COUNT(*) FROM extracted_pages WHERE rulebookId = :rulebookId")
    suspend fun countByRulebookId(rulebookId: Long): Int

    @Query("DELETE FROM extracted_pages WHERE rulebookId = :rulebookId")
    suspend fun deleteByRulebookId(rulebookId: Long)
}
