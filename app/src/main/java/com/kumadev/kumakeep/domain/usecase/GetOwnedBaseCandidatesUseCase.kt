package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.model.BaseGameRef
import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import javax.inject.Inject

/**
 * Filtra i giochi base di un'espansione a quelli effettivamente posseduti
 * (in libreria): un'espansione può essere agganciata solo a una base posseduta,
 * perché compare esclusivamente nel dettaglio di quest'ultima.
 */
class GetOwnedBaseCandidatesUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    suspend operator fun invoke(baseGames: List<BaseGameRef>): List<BaseGameRef> =
        baseGames.filter { repository.isInLibrary(it.bggId) }
}
