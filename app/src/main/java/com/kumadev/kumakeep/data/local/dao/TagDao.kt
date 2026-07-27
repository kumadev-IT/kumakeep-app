package com.kumadev.kumakeep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kumadev.kumakeep.data.local.entity.GameTagEntity
import com.kumadev.kumakeep.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    // ─── CRUD tag ───────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    /** Elimina il tag; le associazioni gioco↔tag vengono rimosse a cascata (FK). */
    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTag(tagId: Long)

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM tags WHERE name = :name COLLATE NOCASE)")
    suspend fun existsByName(name: String): Boolean

    // ─── Associazione gioco↔tag ─────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assignTag(entry: GameTagEntity)

    @Query("DELETE FROM game_tags WHERE bggId = :bggId AND tagId = :tagId")
    suspend fun removeTag(bggId: Long, tagId: Long)

    @Query(
        """
        SELECT t.* FROM tags t
        INNER JOIN game_tags gt ON t.id = gt.tagId
        WHERE gt.bggId = :bggId
        ORDER BY t.name COLLATE NOCASE
        """
    )
    fun getTagsForGame(bggId: Long): Flow<List<TagEntity>>

    /** Fetch bulk: tag per un insieme di giochi (es. lista libreria), in un'unica query. */
    @Query(
        """
        SELECT gt.bggId as bggId, t.id as tagId, t.name as name, t.colorHex as colorHex
        FROM game_tags gt
        INNER JOIN tags t ON t.id = gt.tagId
        WHERE gt.bggId IN (:bggIds)
        ORDER BY t.name COLLATE NOCASE
        """
    )
    fun getTagsForGames(bggIds: List<Long>): Flow<List<GameTagRow>>

    @Query("SELECT COUNT(*) FROM game_tags WHERE tagId = :tagId")
    suspend fun getGameCountForTag(tagId: Long): Int
}
