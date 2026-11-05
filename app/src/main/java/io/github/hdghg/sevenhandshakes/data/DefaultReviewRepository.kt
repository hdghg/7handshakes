package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDao
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewRecord
import io.github.hdghg.sevenhandshakes.di.ApplicationScope
import io.github.hdghg.sevenhandshakes.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import timber.log.Timber
import java.security.SecureRandom
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
        override fun getGratitudesStream(): Flow<List<ReviewRecord>> =
            reviewDao.observeAll().onEach { Timber.d("Loaded %d reviews from DB", it.size) }

        override suspend fun placeNewReview(reviewText: String, gratitude: Byte, contactId: Int) {
            Timber.d(
                "Placing review: %d chars, gratitude=%d, contactId=%d",
                reviewText.length,
                gratitude,
                contactId,
            )
            val keyPairGenerator = Ed25519KeyPairGenerator()
            keyPairGenerator.init(Ed25519KeyGenerationParameters(SecureRandom()))
            val keyPair = keyPairGenerator.generateKeyPair()
            val privateKey = keyPair.private as Ed25519PrivateKeyParameters

            val message = reviewText.toByteArray(Charsets.UTF_8)
            val signer = Ed25519Signer()
            signer.init(true, privateKey)
            signer.update(message, 0, message.size)
            val signature = signer.generateSignature()

            val r = ReviewRecord(
                id = 0,
                uuid = UUID.randomUUID().toString(),
                sourceContactId = 111,
                targetContactId = contactId,
                text = reviewText,
                gratitude = gratitude,
                signature = signature,
            )

            reviewDao.insert(r)
            Timber.d("Review %s saved, signature %d bytes", r.uuid, signature.size)
        }
    }
