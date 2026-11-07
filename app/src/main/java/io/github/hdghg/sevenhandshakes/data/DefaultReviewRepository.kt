package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDao
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import io.github.hdghg.sevenhandshakes.di.ApplicationScope
import io.github.hdghg.sevenhandshakes.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import io.github.hdghg.sevenhandshakes.util.UuidV7
import org.bouncycastle.crypto.signers.Ed25519Signer
import timber.log.Timber
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
        override fun getGratitudesStream(): Flow<List<ReviewRecord>> =
            reviewDao.observeAll().onEach { Timber.d("Loaded %d reviews from DB", it.size) }

        override fun getRateCountsStream(sourceContactId: Int, targetContactId: Int): Flow<Map<Byte, Int>> =
            reviewDao.observeRateCounts(sourceContactId, targetContactId).map { counts ->
                counts
                    .groupBy({ rateLevelOf(it.gratitude) }, { it.count })
                    .mapValues { (_, values) -> values.sum() }
            }

        override suspend fun placeNewReview(
            reviewText: String,
            gratitude: Byte,
            sourceContactId: Int,
            authorPrivateKey: String,
            contactId: Int,
        ) {
            Timber.d(
                "Placing review: %d chars, gratitude=%d, sourceContactId=%d, contactId=%d",
                reviewText.length,
                gratitude,
                sourceContactId,
                contactId,
            )
            // Identity keys are validated when saved, so this only fails on a corrupted record.
            val privateKey = requireNotNull(Ed25519Keys.parsePrivateKey(authorPrivateKey)) {
                "Identity of contact $sourceContactId has an unreadable private key"
            }

            val message = reviewText.toByteArray(Charsets.UTF_8)
            val signer = Ed25519Signer()
            signer.init(true, privateKey)
            signer.update(message, 0, message.size)
            val signature = signer.generateSignature()

            val r = ReviewRecord(
                id = 0,
                uuid = UuidV7.generate().toString(),
                sourceContactId = sourceContactId,
                targetContactId = contactId,
                text = reviewText,
                gratitude = gratitude,
                signature = signature,
            )

            reviewDao.insert(r)
            Timber.d("Review %s saved, signature %d bytes", r.uuid, signature.size)
        }
    }
