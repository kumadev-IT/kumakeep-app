package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.model.Tag
import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import javax.inject.Inject

/** Crea un nuovo tag utente (nome + colore). Fallisce se il nome esiste già. */
class CreateTagUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    suspend operator fun invoke(name: String, colorHex: String): Result<Tag> =
        repository.createTag(name, colorHex)
}
