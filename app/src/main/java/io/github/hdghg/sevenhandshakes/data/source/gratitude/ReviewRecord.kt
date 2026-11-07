package io.github.hdghg.sevenhandshakes.data.source.gratitude

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "review",
    // A review is identified by its uuid together with its author.
    indices = [Index(value = ["uuid", "sourceContactId"], unique = true)],
)
data class ReviewRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uuid: String, // UUIDv7
    val sourceContactId: Int,
    val targetContactId: Int,
    val text: String,
    val gratitude: Byte,
    /**
     * signed uuid + sourceContactId + rate + text
     */
    val signature: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReviewRecord

        if (sourceContactId != other.sourceContactId) return false
        if (targetContactId != other.targetContactId) return false
        if (gratitude != other.gratitude) return false
        if (id != other.id) return false
        if (uuid != other.uuid) return false
        if (text != other.text) return false
        if (!signature.contentEquals(other.signature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sourceContactId
        result = 31 * result + targetContactId
        result = 31 * result + gratitude
        result = 31 * result + id.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + text.hashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}
