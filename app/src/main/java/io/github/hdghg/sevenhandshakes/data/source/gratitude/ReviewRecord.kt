package io.github.hdghg.sevenhandshakes.data.source.gratitude

import androidx.room.Entity
import androidx.room.PrimaryKey

// Create unique index on uuid+signerPublicKeyId
@Entity(tableName = "review")
data class ReviewRecord(
    @PrimaryKey val id: Int,
    val uuid: String, // UUIDv7
    val signerPublicKeyId: Int,
    val text: String,
    val gratitude: Byte,
    /**
     * signed uuid + signerPublicKeyId + rate + text
     */
    val signature: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReviewRecord

        if (signerPublicKeyId != other.signerPublicKeyId) return false
        if (gratitude != other.gratitude) return false
        if (id != other.id) return false
        if (uuid != other.uuid) return false
        if (text != other.text) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = signerPublicKeyId
        result = 31 * result + gratitude
        result = 31 * result + id.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}
