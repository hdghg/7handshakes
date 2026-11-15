package io.github.hdghg.sevenhandshakes.data.share

import androidx.room.withTransaction
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactDao
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDao
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDatabase
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import javax.inject.Inject

/** The database as [ShareImporter] needs it. */
interface ShareImportStore {
    /** Runs [block] so that either all of its changes are saved or none. */
    suspend fun <T> transaction(block: suspend () -> T): T

    suspend fun contactByPublicKey(publicKey: String): ContactRecord?

    /** Returns the id of the new contact. */
    suspend fun insertContact(publicKey: String, name: String?, email: String?): Int

    /** Changes the name and email of [contact] and nothing else. */
    suspend fun updateContactDetails(contact: ContactRecord, name: String?, email: String?)

    /** Returns false, saving nothing, if the author already has a review with this uuid. */
    suspend fun insertReviewIfAbsent(review: ReviewRecord): Boolean
}

class RoomShareImportStore
    @Inject
    constructor(
        private val database: ReviewDatabase,
        private val contactDao: ContactDao,
        private val reviewDao: ReviewDao,
    ) : ShareImportStore {
        override suspend fun <T> transaction(block: suspend () -> T): T = database.withTransaction { block() }

        override suspend fun contactByPublicKey(publicKey: String): ContactRecord? =
            contactDao.getByPublicKey(publicKey)

        override suspend fun insertContact(publicKey: String, name: String?, email: String?): Int =
            contactDao.insert(
                ContactRecord(publicKey = publicKey, name = name, isFavorite = false, isIgnored = false, email = email),
            ).toInt()

        override suspend fun updateContactDetails(contact: ContactRecord, name: String?, email: String?) =
            contactDao.updateDetails(contact.id, name, email, contact.isFavorite, contact.isIgnored)

        override suspend fun insertReviewIfAbsent(review: ReviewRecord): Boolean =
            reviewDao.insertIfAbsent(review) != -1L
    }
