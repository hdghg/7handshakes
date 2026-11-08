package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.contact.ContactDao
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDao
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import io.github.hdghg.sevenhandshakes.di.ApplicationScope
import io.github.hdghg.sevenhandshakes.di.DefaultDispatcher
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import io.github.hdghg.sevenhandshakes.util.UuidV7
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transformLatest
import org.bouncycastle.crypto.signers.Ed25519Signer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class DefaultReviewRepository
    @Inject
    constructor(
        private val reviewDao: ReviewDao,
        private val contactDao: ContactDao,
        @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) : ReviewRepository {
        override fun getGratitudesStream(): Flow<List<ReviewRecord>> =
            reviewDao.observeAll().onEach { Timber.d("Loaded %d reviews from DB", it.size) }

        override fun getHandshakeRatingsStream(contactId: Int, myContactId: Int): Flow<List<ReviewExchange>> =
            combine(reviewDao.observeEdgeCounts(), contactDao.observeIgnoredIds()) { edges, ignored ->
                HandshakeGraph(
                    edges.map {
                        ReviewEdge(
                            it.sourceContactId,
                            it.targetContactId,
                            rateLevelOf(it.gratitude),
                            it.count,
                        )
                    },
                ) to ignored.toSet()
            }.transformLatest { (graph, ignored) ->
                val finished = ArrayList<ReviewExchange>(MAX_HANDSHAKES)
                for (level in 1..MAX_HANDSHAKES) {
                    currentCoroutineContext().ensureActive()
                    finished += graph.exchange(contactId, myContactId, handshakes = level, ignored = ignored)
                    emit(finished.toList())
                }
            }.flowOn(dispatcher)

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
