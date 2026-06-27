package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.model.Rulebook
import com.kumadev.kumakeep.domain.repository.RulebookRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRulebookUseCase @Inject constructor(
    private val repository: RulebookRepository
) {
    operator fun invoke(gameId: Long): Flow<Rulebook?> = repository.getRulebook(gameId)
}
