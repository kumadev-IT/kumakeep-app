package com.kumadev.kumakeep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kumadev.kumakeep.data.local.entity.BoardGameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BoardGameDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(game: BoardGameEntity)

    @Update
    suspend fun update(game: BoardGameEntity)

    @Query("SELECT * FROM boardgames WHERE bggId = :bggId")
    suspend fun getByBggId(bggId: Long): BoardGameEntity?

    @Query("SELECT * FROM boardgames WHERE bggId IN (:bggIds)")
    fun getByBggIds(bggIds: List<Long>): Flow<List<BoardGameEntity>>

    @Query("SELECT * FROM boardgames WHERE primaryName LIKE '%' || :query || '%'")
    fun searchLocal(query: String): Flow<List<BoardGameEntity>>
}