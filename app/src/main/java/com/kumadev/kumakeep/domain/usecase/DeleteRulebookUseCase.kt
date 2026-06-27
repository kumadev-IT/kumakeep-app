package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.repository.RulebookRepository
import javax.inject.Inject

class DeleteRulebookUseCase @Inject constructor(
    private val repository: RulebookRepository
) {
    suspend operator fun invoke(gameId: Long): Result<Unit> =
        repository.deleteRulebook(gameId)
}
