package io.github.hdghg.sevenhandshakes.data.source.contact

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contact",
    indices = [Index(value = ["publicKey"], unique = true)],
)
class ContactRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val publicKey: String,
    val name: String?,
    val isFavorite: Boolean,
    val isIgnored: Boolean,
)
