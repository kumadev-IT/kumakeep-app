package com.kumadev.kumakeep.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tag definito dall'utente (nome + colore). Dato utente puro, mai collegato a
 * [BoardGameEntity]: vedi vincolo "boardgames solo dati BGG".
 */
@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val createdAt: Long = System.currentTimeMillis()
)
