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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import io.github.hdghg.sevenhandshakes.data.source.gratitude.EdgeCount
import kotlinx.coroutines.flow.transformLatest
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

        override fun getReviewStream(id: Int): Flow<ReviewRecord?> = reviewDao.observeById(id)

        override fun getHandshakeRatingsStream(
            contactId: Int,
            myContactId: Int,
            depth: Int,
        ): Flow<List<ReviewExchange>> =
            // Room re-emits a query whenever its table is written, even with the same result; skip those,
            // so editing a contact's name or adding a review elsewhere does not start the search over.
            combine(
                reviewDao.observeEdgeCounts().distinctUntilChanged(),
                contactDao.observeIgnoredIds().map { it.toSet() }.distinctUntilChanged(),
            ) { edges, ignored -> GraphInput(edges, ignored) }
                .distinctUntilChanged()
                .transformLatest { input ->
                    val context = currentCoroutineContext()
                    val graph by lazy { input.toGraph() }
                    val finished = ArrayList<ReviewExchange>(depth)
                    for (level in 1..depth) {
                        context.ensureActive()
                        finished += exchangeCache.getOrPut(input, ExchangeKey(contactId, myContactId, level)) {
                            graph.exchange(contactId, myContactId, handshakes = level, ignored = input.ignored) {
                                context.ensureActive()
                            }
                        }
                        emit(finished.toList())
                    }
                }.flowOn(dispatcher)

        /** Keeps found ratings, so going back to a contact or raising the depth does not search again. */
        private val exchangeCache = ExchangeCache()

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
            // The screen cleans the text as it is typed, so this only fails on a programming error.
            require(isValidReviewText(reviewText)) { "The review text is too long or holds a forbidden character" }
            // Identity keys are validated when saved, so this only fails on a corrupted record.
            val privateKey = requireNotNull(Ed25519Keys.parsePrivateKey(authorPrivateKey)) {
                "Identity of contact $sourceContactId has an unreadable private key"
            }

            val receiver = requireNotNull(contactDao.getById(contactId)) { "Contact $contactId does not exist" }
            val uuid = UuidV7.generate().toString()
            val signature = ReviewSignature.sign(
                uuid = uuid,
                authorPublicKey = requireNotNull(Ed25519Keys.publicKeyOf(authorPrivateKey)),
                // A contact typed in by hand may hold text that is not a key; it is then signed as it is.
                receiverPublicKey = Ed25519Keys.canonicalPublicKey(receiver.publicKey) ?: receiver.publicKey.trim(),
                rating = gratitude,
                text = reviewText,
                authorPrivateKey = privateKey,
            )

            val r = ReviewRecord(
                id = 0,
                uuid = uuid,
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

/** Everything a handshake search depends on besides its two contacts and depth. */
private data class GraphInput(val edges: List<EdgeCount>, val ignored: Set<Int>) {
    fun toGraph() = HandshakeGraph(
        edges.map { ReviewEdge(it.sourceContactId, it.targetContactId, rateLevelOf(it.gratitude), it.count) },
    )
}

private data class ExchangeKey(val contactId: Int, val myContactId: Int, val handshakes: Int)

/**
 * The most recent ratings found for one [GraphInput]. When the reviews or ignored contacts change,
 * everything kept is dropped, since any rating may then be different. Safe to use from several threads.
 */
private class ExchangeCache {
    private var input: GraphInput? = null
    private val entries = object : LinkedHashMap<ExchangeKey, ReviewExchange>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<ExchangeKey, ReviewExchange>?) =
            size > MAX_ENTRIES
    }

    /** The kept rating for [key], or the result of [compute] (run without holding the lock), which is then kept. */
    fun getOrPut(input: GraphInput, key: ExchangeKey, compute: () -> ReviewExchange): ReviewExchange {
        synchronized(this) {
            if (input != this.input) {
                this.input = input
                entries.clear()
            }
            entries[key]?.let { return it }
        }
        val result = compute()
        synchronized(this) {
            // The data may have changed while computing; a result for old data is not kept.
            if (input == this.input) entries[key] = result
        }
        return result
    }

    private companion object {
        /** Enough for every level of a few dozen contacts. */
        const val MAX_ENTRIES = 256
    }
}
