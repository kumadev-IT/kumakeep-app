package com.kumadev.kumakeep.data.remote.mapper

import com.kumadev.kumakeep.data.local.entity.BoardGameEntity
import com.kumadev.kumakeep.data.remote.dto.BggItemDto
import com.kumadev.kumakeep.domain.model.BaseGameRef

// Serializzazione dei riferimenti ai giochi base in un'unica colonna TEXT
// (baseGamesRef). Formato per record: "<id>:<nome>", record separati da newline.
// Robusto perché: l'id è numerico (nessun ':'), lo split usa solo il PRIMO ':'
// (i nomi possono contenere ':', es. "Forest Shuffle: Alpine"), e i nomi non
// contengono mai newline.
private const val RECORD_SEP = '\n'
private const val FIELD_SEP = ':'

fun BggItemDto.toEntity(): BoardGameEntity {
    return BoardGameEntity(
        bggId = id,
        primaryName = primaryName(),
        yearPublished = yearPublished?.value,
        minPlayers = minPlayers?.value,
        maxPlayers = maxPlayers?.value,
        minAge = minAge?.value,
        playingTime = playingTime?.value,
        complexity = statistics?.ratings?.averageWeight?.value,
        bggRating = statistics?.ratings?.average?.value,
        thumbnail = thumbnail?.trim(),
        image = image?.trim(),
        description = description?.trim(),
        designers = designers().joinToString(","),
        artists = artists().joinToString(","),
        publishers = publishers().joinToString(","),
        categories = categories().joinToString(","),
        mechanics = mechanics().joinToString(","),
        families = families().joinToString(","),
        isExpansion = isExpansion(),
        baseGamesRef = serializeBaseGames(baseGames())
    )
}

/** Serializza i giochi base (id + nome) in un'unica stringa. Null se vuoto. */
fun serializeBaseGames(bases: List<Pair<Long, String>>): String? =
    bases.takeIf { it.isNotEmpty() }
        ?.joinToString(RECORD_SEP.toString()) { (id, name) -> "$id$FIELD_SEP$name" }

/** Ricostruisce i riferimenti ai giochi base dalla stringa serializzata. */
fun parseBaseGames(raw: String?): List<BaseGameRef> {
    if (raw.isNullOrEmpty()) return emptyList()
    return raw.split(RECORD_SEP).mapNotNull { record ->
        val id = record.substringBefore(FIELD_SEP).toLongOrNull() ?: return@mapNotNull null
        val name = record.substringAfter(FIELD_SEP)
        BaseGameRef(id, name)
    }
}
