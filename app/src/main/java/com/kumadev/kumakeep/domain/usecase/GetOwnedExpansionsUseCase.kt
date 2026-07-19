package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Espansioni possedute collegate a un gioco base (per la sezione nel dettaglio). */
class GetOwnedExpansionsUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    operator fun invoke(baseBggId: Long): Flow<List<BoardGame>> =
        repository.getOwnedExpansions(baseBggId)
}
