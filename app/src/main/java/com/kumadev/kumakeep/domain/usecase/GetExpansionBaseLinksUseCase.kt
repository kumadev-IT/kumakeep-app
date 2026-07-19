package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Id dei giochi base a cui un'espansione è collegata (vuoto = non posseduta). */
class GetExpansionBaseLinksUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    operator fun invoke(expansionBggId: Long): Flow<List<Long>> =
        repository.getExpansionBaseLinks(expansionBggId)
}
