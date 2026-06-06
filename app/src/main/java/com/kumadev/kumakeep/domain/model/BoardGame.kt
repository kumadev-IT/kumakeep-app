package com.kumadev.kumakeep.domain.model

data class BoardGame(
    val bggId: Long,
    val primaryName: String,
    val yearPublished: Int?,
    val minPlayers: Int?,
    val maxPlayers: Int?,
    val minAge: Int?,
    val playingTime: Int?,
    val complexity: Float?,
    val bggRating: Float?,
    val thumbnail: String?,
    val image: String?,
    val description: String?,
    val designers: List<String>,
    val publishers: List<String>,
    val categories: List<String>,
    val mechanics: List<String>,
    // stato utente — null se il gioco non è in collezione
    val libraryEntry: LibraryEntry? = null
)

data class LibraryEntry(
    val id: Long,
    val rate: String,
    val numPlays: String,
    val notes: String?
)

// risultato leggero per la lista di ricerca BGG (no dettagli)
data class SearchResult(
    val bggId: Long,
    val name: String,
    val yearPublished: Int?
)