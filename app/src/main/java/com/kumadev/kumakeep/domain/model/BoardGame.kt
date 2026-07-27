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
    // classificazione BGG: true se è un'espansione (non compare in collection,
    // vive solo nel dettaglio del gioco base)
    val isExpansion: Boolean = false,
    // giochi base che questa espansione estende (vuoto per i giochi base)
    val baseGames: List<BaseGameRef> = emptyList(),
    // stato utente — null se il gioco non è in collezione
    val libraryEntry: LibraryEntry? = null,
    // tag utente assegnati a questo gioco (dato utente, mai da BGG)
    val tags: List<Tag> = emptyList()
)

data class Tag(
    val id: Long,
    val name: String,
    val colorHex: String
)

// riferimento leggero a un gioco base collegato a un'espansione
data class BaseGameRef(
    val bggId: Long,
    val name: String
)

data class LibraryEntry(
    val id: Long,
    val rate: UserRate,
    val numPlays: NumPlays,
    val notes: String?,
    val createdAt: Long = 0L
)

enum class UserRate {
    BAH, MID, YES, TOP, WOW, NOT_RATED
}

enum class NumPlays {
    ZERO, ONE, MANY, PLENTY, NOT_CLASSIFIED
}

// risultato leggero per la lista di ricerca BGG (no dettagli)
data class SearchResult(
    val bggId: Long,
    val name: String,
    val yearPublished: Int?,
    // true se BGG classifica questo id come espansione (tag EXP in lista)
    val isExpansion: Boolean = false,
    // true se già posseduto: in libreria (giochi base) o collegato a una base (espansioni)
    val isOwned: Boolean = false
)