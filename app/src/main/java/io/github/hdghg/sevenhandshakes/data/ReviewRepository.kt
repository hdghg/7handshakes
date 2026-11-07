package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun getGratitudesStream(): Flow<List<ReviewRecord>>

    /**
     * Emits the reviews exchanged between [contactId] and the selected identity's contact [myContactId].
     *
     * @param level how many handshakes the rating spans, 1..[MAX_HANDSHAKES].
     * @param previous the result for [level] - 1, or null when [level] is 1.
     *   Not used yet: every level currently returns the direct exchange.
     */
    fun getRateCountsStream(
        contactId: Int,
        myContactId: Int,
        level: Int,
        previous: ReviewExchange?,
    ): Flow<ReviewExchange>

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
