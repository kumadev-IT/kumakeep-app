package com.kumadev.rulesreader.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rulebook_chunks",
    indices = [Index(value = ["rulebookId", "chunkIndex"], unique = true)]
)
data class RulebookChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rulebookId: Long,
    val chunkIndex: Int,
    /** Numero di pagina del primo token del chunk */
    val pageNum: Int,
    val text: String,
    /** FloatArray serializzato in little-endian (4 byte per float). Null se embedding non disponibile. */
    val embeddingBlob: ByteArray?,
    val rulesReaderVersion: String
) {
    // Override necessario perché ByteArray non ha equals strutturale
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RulebookChunkEntity) return false
        return id == other.id &&
            rulebookId == other.rulebookId &&
            chunkIndex == other.chunkIndex &&
            pageNum == other.pageNum &&
            text == other.text &&
            embeddingBlob.contentEquals(other.embeddingBlob) &&
            rulesReaderVersion == other.rulesReaderVersion
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + rulebookId.hashCode()
        result = 31 * result + chunkIndex
        result = 31 * result + pageNum
        result = 31 * result + text.hashCode()
        result = 31 * result + (embeddingBlob?.contentHashCode() ?: 0)
        result = 31 * result + rulesReaderVersion.hashCode()
        return result
    }
}
