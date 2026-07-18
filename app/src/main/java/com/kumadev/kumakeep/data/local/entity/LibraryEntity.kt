package com.kumadev.kumakeep.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kumadev.kumakeep.domain.model.NumPlays
import com.kumadev.kumakeep.domain.model.UserRate

@Entity(
    tableName = "library",
    indices = [Index(value = ["bggId"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = BoardGameEntity::class,
        parentColumns = ["bggId"],
        childColumns = ["bggId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class LibraryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bggId: Long,
    val rate: UserRate = UserRate.NOT_RATED,
    val numPlays: NumPlays = NumPlays.NOT_CLASSIFIED,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)