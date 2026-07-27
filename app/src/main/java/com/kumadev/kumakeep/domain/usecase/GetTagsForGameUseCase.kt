package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.model.Tag
import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Tag assegnati a un gioco specifico (per i chip nel dettaglio). */
class GetTagsForGameUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    operator fun invoke(bggId: Long): Flow<List<Tag>> = repository.getTagsForGame(bggId)
}
