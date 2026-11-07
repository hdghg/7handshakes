package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun getGratitudesStream(): Flow<List<ReviewRecord>>

    /**
     * @param sourceContactId id of the author in the contact table (the selected identity's contact).
     * @param authorPrivateKey the selected identity's private key; the review is signed with it.
     * @param contactId id of the receiver in the contact table.
     */
    suspend fun placeNewReview(
        reviewText: String,
        gratitude: Byte,
        sourceContactId: Int,
        authorPrivateKey: String,
        contactId: Int,
    )
}
