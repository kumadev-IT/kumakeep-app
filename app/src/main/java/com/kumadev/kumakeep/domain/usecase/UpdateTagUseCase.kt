package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import javax.inject.Inject

/** Rinomina/ricolora un tag esistente. */
class UpdateTagUseCase @Inject constructor(
    private val repository: BoardGameRepository
) {
    suspend operator fun invoke(tagId: Long, name: String, colorHex: String): Result<Unit> =
        repository.updateTag(tagId, name, colorHex)
}
