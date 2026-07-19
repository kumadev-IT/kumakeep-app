package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.model.BaseGameRef
import javax.inject.Inject

/**
 * Come [GetOwnedBaseCandidatesUseCase] ma partendo dal solo id dell'espansione:
 * scarica il dettaglio (per avere i giochi base dai link inbound) e filtra a
 * quelli posseduti. Usato dalla ricerca, dove i dettagli non sono ancora caricati.
 */
class GetOwnedBaseCandidatesForExpansionUseCase @Inject constructor(
    private val getGameDetailUseCase: GetGameDetailUseCase,
    private val getOwnedBaseCandidatesUseCase: GetOwnedBaseCandidatesUseCase
) {
    suspend operator fun invoke(expansionBggId: Long): Result<List<BaseGameRef>> = runCatching {
        val game = getGameDetailUseCase(expansionBggId).getOrThrow()
        getOwnedBaseCandidatesUseCase(game.baseGames)
    }
}
