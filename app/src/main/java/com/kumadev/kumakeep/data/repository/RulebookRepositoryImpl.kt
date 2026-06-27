package com.kumadev.kumakeep.data.repository

import android.net.Uri
import com.kumadev.kumakeep.data.local.dao.RulebookDao
import com.kumadev.kumakeep.data.local.entity.RulebookEntity
import com.kumadev.kumakeep.data.rulebook.RulebookFileManager
import com.kumadev.kumakeep.domain.model.Rulebook
import com.kumadev.kumakeep.domain.repository.RulebookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RulebookRepositoryImpl @Inject constructor(
    private val dao: RulebookDao,
    private val fileManager: RulebookFileManager
) : RulebookRepository {

    override fun getRulebook(gameId: Long): Flow<Rulebook?> =
        dao.getByGameId(gameId).map { it?.toDomain() }

    override suspend fun importRulebook(uri: Uri, gameId: Long, fileName: String): Result<Rulebook> =
        runCatching {
            val file = fileManager.copyFromUri(uri, gameId)
            val pageCount = fileManager.getPageCount(file)
            val entity = RulebookEntity(
                gameId = gameId,
                filePath = file.absolutePath,
                fileName = fileName,
                pageCount = pageCount,
                sizeBytes = file.length()
            )
            dao.insert(entity)
            entity.toDomain()
        }

    override suspend fun deleteRulebook(gameId: Long): Result<Unit> =
        runCatching {
            dao.deleteByGameId(gameId)
            fileManager.delete(gameId)
        }
}

private fun RulebookEntity.toDomain() = Rulebook(
    id = id,
    gameId = gameId,
    filePath = filePath,
    fileName = fileName,
    pageCount = pageCount,
    sizeBytes = sizeBytes,
    importedAt = importedAt
)
