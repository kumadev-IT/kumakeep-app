package com.kumadev.kumakeep.domain.usecase

import android.net.Uri
import com.kumadev.kumakeep.domain.model.Rulebook
import com.kumadev.kumakeep.domain.repository.RulebookRepository
import javax.inject.Inject

class ImportRulebookUseCase @Inject constructor(
    private val repository: RulebookRepository
) {
    suspend operator fun invoke(uri: Uri, gameId: Long, fileName: String): Result<Rulebook> =
        repository.importRulebook(uri, gameId, fileName)
}
