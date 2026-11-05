package io.github.hdghg.sevenhandshakes.data.source.identity

import androidx.room.Embedded
import androidx.room.Relation
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord

/**
 * An identity together with the contact that holds its public key.
 */
data class IdentityWithContact(
    @Embedded val identity: IdentityRecord,
    @Relation(parentColumn = "contactId", entityColumn = "id")
    val contact: ContactRecord,
)
