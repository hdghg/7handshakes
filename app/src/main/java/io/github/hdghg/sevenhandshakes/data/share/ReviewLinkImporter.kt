package io.github.hdghg.sevenhandshakes.data.share

import androidx.room.withTransaction
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactDao
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDao
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDatabase
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord

/** Saves a review that came in a [ReviewLink]. The review must have been checked; [ReviewLink.decode] does. */
class ReviewLinkImporter
    constructor(
        private val database: ReviewDatabase,
        private val contactDao: ContactDao,
        private val reviewDao: ReviewDao,
    ) {
        /** The id of [review] in the review table, or null if the app does not have it. */
        suspend fun findId(review: SharedReview): Int? {
            val author = contactDao.getByPublicKey(review.authorPublicKey) ?: return null
            return reviewDao.getId(review.uuid, author.id)
        }

        /**
         * Saves [review] unless the app already has it, and returns its id in the review table.
         * An author or receiver the app does not know yet is added as a contact without a name.
         */
        suspend fun save(review: SharedReview): Int = database.withTransaction {
            val authorId = contactIdOf(review.authorPublicKey)
            reviewDao.insertIfAbsent(
                ReviewRecord(
                    uuid = review.uuid,
                    sourceContactId = authorId,
                    targetContactId = contactIdOf(review.receiverPublicKey),
                    text = review.text,
                    gratitude = review.rating,
                    signature = review.signature,
                ),
            )
            checkNotNull(reviewDao.getId(review.uuid, authorId))
        }

        private suspend fun contactIdOf(publicKey: String): Int =
            contactDao.getByPublicKey(publicKey)?.id
                ?: contactDao.insert(
                    ContactRecord(publicKey = publicKey, name = null, isFavorite = false, isIgnored = false),
                ).toInt()
    }
