package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import javax.inject.Inject

/** Elimina un tag; rimuove a cascata tutte le associazioni ai giochi. */
class DeleteTagUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    suspend operator fun invoke(tagId: Long): Result<Unit> = repository.deleteTag(tagId)
}
