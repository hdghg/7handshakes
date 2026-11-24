package io.github.hdghg.sevenhandshakes.data.share

import androidx.room.withTransaction
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactDao
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDatabase

/**
 * Saves a contact that came in a [ContactLink]. The contact must have been checked; [ContactLink.decode] does.
 *
 * As with an imported file (see [ShareImporter]), a contact the app already has keeps its own
 * name, email, favorite and ignored marks; the link only fills in a name or email that is missing.
 */
class ContactLinkImporter
    constructor(
        private val database: ReviewDatabase,
        private val contactDao: ContactDao,
    ) {
        /** The contact the app has for the public key of [contact], or null if it has none. */
        suspend fun find(contact: SharedContact): ContactRecord? = contactDao.getByPublicKey(contact.publicKey)

        /** Saves [contact], or fills in what the app's own contact lacks. Returns its id in the contact table. */
        suspend fun save(contact: SharedContact): Int = database.withTransaction {
            val existing = contactDao.getByPublicKey(contact.publicKey)
            if (existing == null) {
                contactDao.insert(
                    ContactRecord(
                        publicKey = contact.publicKey,
                        name = contact.name,
                        isFavorite = false,
                        isIgnored = false,
                        email = contact.email,
                    ),
                ).toInt()
            } else {
                contactDao.updateDetails(
                    id = existing.id,
                    name = nameAfterSaving(existing, contact),
                    email = emailAfterSaving(existing, contact),
                    isFavorite = existing.isFavorite,
                    isIgnored = existing.isIgnored,
                )
                existing.id
            }
        }

        companion object {
            /** The name [existing] has once [contact] is saved: its own, or the link's if it has none. */
            fun nameAfterSaving(existing: ContactRecord?, contact: SharedContact): String? =
                existing?.name?.takeIf { it.isNotBlank() } ?: contact.name

            /** The email [existing] has once [contact] is saved: its own, or the link's if it has none. */
            fun emailAfterSaving(existing: ContactRecord?, contact: SharedContact): String? =
                existing?.email?.takeIf { it.isNotBlank() } ?: contact.email
        }
    }
