package com.kumadev.kumakeep.domain.repository

import android.net.Uri
import com.kumadev.kumakeep.domain.model.Rulebook
import kotlinx.coroutines.flow.Flow

interface RulebookRepository {
    fun getRulebook(gameId: Long): Flow<Rulebook?>
    suspend fun importRulebook(uri: Uri, gameId: Long, fileName: String): Result<Rulebook>
    suspend fun deleteRulebook(gameId: Long): Result<Unit>
}
