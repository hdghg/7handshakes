package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDao
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import io.github.hdghg.sevenhandshakes.di.ApplicationScope
import io.github.hdghg.sevenhandshakes.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultReviewRepository
    @Inject
    constructor(
        private val reviewDao: ReviewDao,
        @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) : ReviewRepository {
        override fun getGratitudesStream(): Flow<List<ReviewRecord>> = reviewDao.observeAll()

        override suspend fun placeNewReview(reviewText: String, gratitude: Byte) {
            // generate ed25519 keypair and sign reviewText with it

            val r = ReviewRecord(
                id = 0,
                uuid = UUID.randomUUID().toString(),
                signerPublicKeyId = 111,
                text = reviewText,
                gratitude = gratitude,
                signature = ByteArray(0),
            )

            reviewDao.upsert(r)
        }
    }
