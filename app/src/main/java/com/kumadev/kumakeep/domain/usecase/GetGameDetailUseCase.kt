package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import javax.inject.Inject

class GetGameDetailUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    suspend operator fun invoke(bggId: Long): Result<BoardGame> =
        repository.getGameDetail(bggId)
}