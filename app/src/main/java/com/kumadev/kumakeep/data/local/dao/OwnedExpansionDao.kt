package com.kumadev.kumakeep.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kumadev.kumakeep.data.local.entity.BoardGameEntity
import com.kumadev.kumakeep.data.local.entity.OwnedExpansionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OwnedExpansionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entry: OwnedExpansionEntity)

    /** Scollega una singola espansione da un singolo gioco base. */
    @Query("DELETE FROM owned_expansions WHERE expansionBggId = :expansionBggId AND baseBggId = :baseBggId")
    suspend fun unlink(expansionBggId: Long, baseBggId: Long)

    /** Scollega un'espansione da tutti i suoi giochi base (rimozione completa). */
    @Query("DELETE FROM owned_expansions WHERE expansionBggId = :expansionBggId")
    suspend fun unlinkAll(expansionBggId: Long)

    /** Espansioni possedute per un dato gioco base (dati BGG da boardgames). */
    @Query(
        """
        SELECT b.* FROM boardgames b
        INNER JOIN owned_expansions oe ON b.bggId = oe.expansionBggId
        WHERE oe.baseBggId = :baseBggId
        ORDER BY b.primaryName COLLATE NOCASE
        """
    )
    fun getExpansionsForBase(baseBggId: Long): Flow<List<BoardGameEntity>>

    /** Id dei giochi base a cui questa espansione è collegata (0 = non posseduta). */
    @Query("SELECT baseBggId FROM owned_expansions WHERE expansionBggId = :expansionBggId")
    fun getBaseIdsForExpansion(expansionBggId: Long): Flow<List<Long>>

    /** Numero di espansioni distinte possedute (una collegata a più basi conta 1). */
    @Query("SELECT COUNT(DISTINCT expansionBggId) FROM owned_expansions")
    fun getOwnedExpansionCount(): Flow<Int>
}
