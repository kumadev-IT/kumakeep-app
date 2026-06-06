package com.kumadev.kumakeep.data.remote.mapper

import com.kumadev.kumakeep.data.local.entity.BoardGameEntity
import com.kumadev.kumakeep.data.remote.dto.BggItemDto

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
        families = families().joinToString(",")
    )
}