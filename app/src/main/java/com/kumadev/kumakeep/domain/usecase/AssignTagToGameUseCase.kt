package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import javax.inject.Inject

/** Assegna un tag esistente a un gioco. */
class AssignTagToGameUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    suspend operator fun invoke(bggId: Long, tagId: Long): Result<Unit> =
        repository.assignTagToGame(bggId, tagId)
}
