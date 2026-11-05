package io.github.hdghg.sevenhandshakes.data

import androidx.room.withTransaction
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactDao
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.data.source.gratitude.ReviewDatabase
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityDao
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityRecord
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityWithContact
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultIdentityRepository
    @Inject
    constructor(
        private val database: ReviewDatabase,
        private val identityDao: IdentityDao,
        private val contactDao: ContactDao,
    ) : IdentityRepository {
        override fun getIdentitiesStream(): Flow<List<IdentityWithContact>> =
            identityDao.observeAllWithContact().onEach { Timber.d("Loaded %d identities from DB", it.size) }

        override suspend fun addIdentity(privateKey: String, name: String): AddIdentityResult {
            val canonicalKey = Ed25519Keys.canonicalPrivateKey(privateKey)
                ?: return AddIdentityResult.InvalidPrivateKey
            val publicKey = Ed25519Keys.publicKeyOf(canonicalKey)
                ?: return AddIdentityResult.InvalidPrivateKey
            // Room transactions are exclusive, so nothing can insert the same key
            // between the existence check and the insert.
            return database.withTransaction {
                if (identityDao.existsByPrivateKey(canonicalKey)) {
                    return@withTransaction AddIdentityResult.Duplicate
                }
                val contact = contactDao.getByPublicKey(publicKey)
                val contactId = if (contact == null) {
                    contactDao.insert(
                        ContactRecord(publicKey = publicKey, name = name, isFavorite = false, isIgnored = false),
                    ).toInt()
                } else {
                    if (contact.name != name) contactDao.updateName(contact.id, name)
                    contact.id
                }
                val id = identityDao.insert(IdentityRecord(privateKey = canonicalKey, contactId = contactId)).toInt()
                Timber.d("Identity %d saved for contact %d", id, contactId)
                AddIdentityResult.Success(id)
            }
        }
    }
