package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun getGratitudesStream(): Flow<List<ReviewRecord>>

    /**
     * @param contactId id of the receiver in the contact table.
     */
    suspend fun placeNewReview(reviewText: String, gratitude: Byte, contactId: Int)
}
