package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import javax.inject.Inject

/** Rimuove un tag da un gioco (non elimina il tag). */
class RemoveTagFromGameUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    suspend operator fun invoke(bggId: Long, tagId: Long): Result<Unit> =
        repository.removeTagFromGame(bggId, tagId)
}
