package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun getGratitudesStream(): Flow<List<ReviewRecord>>

    /**
     * Emits handshake ratings between [contactId] and the selected identity's contact [myContactId]:
     * index 0 is one handshake, index 1 two handshakes, up to [MAX_HANDSHAKES].
     *
     * Each level is emitted as soon as it is ready, so the list grows one level at a time.
     * When reviews or ignored contacts change, the calculation starts over from level 1.
     */
    fun getHandshakeRatingsStream(
        contactId: Int,
        myContactId: Int,
    ): Flow<List<ReviewExchange>>

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
