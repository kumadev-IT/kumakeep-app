package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import javax.inject.Inject

/** Rimuove un'espansione posseduta da tutti i suoi giochi base. */
class RemoveOwnedExpansionUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    suspend operator fun invoke(expansionBggId: Long): Result<Unit> =
        repository.removeOwnedExpansion(expansionBggId)
}
