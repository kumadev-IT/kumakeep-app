package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.model.Tag
import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Tutti i tag definiti dall'utente (per il picker nel dettaglio e il filtro in libreria). */
class GetAllTagsUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    operator fun invoke(): Flow<List<Tag>> = repository.getAllTags()
}
