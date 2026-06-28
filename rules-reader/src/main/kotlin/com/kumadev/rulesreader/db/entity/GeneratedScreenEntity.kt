package com.kumadev.rulesreader.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Schermata di apprendimento generata dall'LLM per un singolo regolamento.
 *
 * Generata una sola volta per gioco e salvata in Room → lettura offline,
 * costo LLM una sola volta per gioco.
 */
@Entity(
    tableName = "generated_screens",
    indices = [Index(value = ["rulebookId", "screenIndex"], unique = true)]
)
data class GeneratedScreenEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rulebookId: Long,
    val screenIndex: Int,
    val title: String,
    val body: String,
    /** Numeri di pagina sorgente, separati da virgola. Es: "1,2,5" */
    val sourcePageNums: String,
    val generatedAt: Long,
    val rulesReaderVersion: String
)
