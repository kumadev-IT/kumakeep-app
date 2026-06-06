package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLibraryUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    operator fun invoke(): Flow<List<BoardGame>> =
        repository.getLibraryGames()
}