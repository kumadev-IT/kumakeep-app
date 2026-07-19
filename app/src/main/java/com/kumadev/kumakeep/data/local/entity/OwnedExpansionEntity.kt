package com.kumadev.kumakeep.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Possesso + relazione base↔espansione. Tabella separata (l'entità "espansione
 * posseduta"): i dati BGG restano su [BoardGameEntity], qui c'è solo il legame.
 *
 * Relazione molti-a-molti: una promo può estendere più basi, una base ha più
 * espansioni. Un'espansione compare SOLO nel dettaglio dei suoi giochi base
 * (non entra mai nella collection, che legge solo la tabella `library`).
 */
@Entity(
    tableName = "owned_expansions",
    indices = [
        Index(value = ["expansionBggId", "baseBggId"], unique = true),
        Index(value = ["baseBggId"]),
        Index(value = ["expansionBggId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = BoardGameEntity::class,
            parentColumns = ["bggId"],
            childColumns = ["expansionBggId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BoardGameEntity::class,
            parentColumns = ["bggId"],
            childColumns = ["baseBggId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class OwnedExpansionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val expansionBggId: Long,
    val baseBggId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
