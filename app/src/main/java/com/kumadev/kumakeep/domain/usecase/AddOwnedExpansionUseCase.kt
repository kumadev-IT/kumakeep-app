package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import javax.inject.Inject

/** Collega un'espansione posseduta a un gioco base. */
class AddOwnedExpansionUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    suspend operator fun invoke(expansionBggId: Long, baseBggId: Long): Result<Unit> =
        repository.addOwnedExpansion(expansionBggId, baseBggId)
}
