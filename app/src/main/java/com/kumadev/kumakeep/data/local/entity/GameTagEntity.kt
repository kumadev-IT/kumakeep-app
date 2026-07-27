package com.kumadev.kumakeep.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Associazione molti-a-molti gioco↔tag. Tabella ponte, stesso spirito di
 * [OwnedExpansionEntity]: i dati BGG restano su [BoardGameEntity], qui c'è
 * solo il legame gioco↔tag utente.
 */
@Entity(
    tableName = "game_tags",
    indices = [
        Index(value = ["bggId", "tagId"], unique = true),
        Index(value = ["tagId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = BoardGameEntity::class,
            parentColumns = ["bggId"],
            childColumns = ["bggId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GameTagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bggId: Long,
    val tagId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
