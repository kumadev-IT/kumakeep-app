package com.kumadev.kumakeep.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.kumadev.kumakeep.data.local.entity.BoardGameEntity
import com.kumadev.kumakeep.data.local.entity.LibraryEntity
import com.kumadev.kumakeep.data.local.entity.UserRate
import com.kumadev.kumakeep.data.local.entity.NumPlays
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: LibraryEntity)

    @Update
    suspend fun update(entry: LibraryEntity)

    @Delete
    suspend fun delete(entry: LibraryEntity)

    @Query("SELECT * FROM library WHERE bggId = :bggId")
    suspend fun getByBggId(bggId: Long): LibraryEntity?

    @Query("SELECT * FROM library")
    fun getAll(): Flow<List<LibraryEntity>>

    @Query("SELECT * FROM library WHERE rate = :rate")
    fun getByRate(rate: UserRate): Flow<List<LibraryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM library WHERE bggId = :bggId)")
    fun isInLibrary(bggId: Long): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM library")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM library WHERE numPlays NOT IN ('NOT_CLASSIFIED', 'ZERO')")
    fun getPlayedCount(): Flow<Int>
}