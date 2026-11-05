package io.github.hdghg.sevenhandshakes.data.source.identity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.RESTRICT
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord

@Entity(
    tableName = "identity",
    indices = [Index(value = ["privateKey"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = ContactRecord::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = RESTRICT,
        ),
    ],
)
data class IdentityRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val privateKey: String,
    val contactId: Int,
)
