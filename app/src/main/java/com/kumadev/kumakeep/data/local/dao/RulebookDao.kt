package com.kumadev.kumakeep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kumadev.kumakeep.data.local.entity.RulebookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RulebookDao {

    @Query("SELECT * FROM rulebooks WHERE gameId = :gameId")
    fun getByGameId(gameId: Long): Flow<RulebookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RulebookEntity)

    @Query("DELETE FROM rulebooks WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)
}
