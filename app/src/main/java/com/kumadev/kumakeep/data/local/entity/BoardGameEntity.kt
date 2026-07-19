package com.kumadev.kumakeep.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "boardgames",
    indices = [Index(value = ["bggId"], unique = true)]
)
data class BoardGameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bggId: Long,
    val primaryName: String,
    val alias: String? = null,
    val yearPublished: Int? = null,
    val minPlayers: Int? = null,
    val maxPlayers: Int? = null,
    val minAge: Int? = null,
    val playingTime: Int? = null,
    val complexity: Float? = null,
    val bggRating: Float? = null,
    val thumbnail: String? = null,
    val image: String? = null,
    val description: String? = null,
    val designers: String? = null,
    val artists: String? = null,
    val publishers: String? = null,
    val categories: String? = null,
    val mechanics: String? = null,
    val families: String? = null,
    // classificazione BGG (dato esterno, sovrascritto al refresh)
    val isExpansion: Boolean = false,
    // giochi base collegati (per un'espansione), serializzati con separatori di
    // controllo (RS tra record, US tra id e nome). Null per i giochi base.
    // Serializzazione/parsing centralizzati in BggMapper.kt.
    val baseGamesRef: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
