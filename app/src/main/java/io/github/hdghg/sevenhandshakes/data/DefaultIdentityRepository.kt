package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityDao
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityRecord
import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityWithContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultIdentityRepository
    @Inject
    constructor(
        private val identityDao: IdentityDao,
    ) : IdentityRepository {
        override fun getIdentitiesStream(): Flow<List<IdentityWithContact>> =
            identityDao.observeAllWithContact().onEach { Timber.d("Loaded %d identities from DB", it.size) }

        override suspend fun addIdentity(privateKey: String, contactId: Int): Int {
            val id = identityDao.insert(IdentityRecord(privateKey = privateKey, contactId = contactId))
            Timber.d("Identity %d saved for contact %d", id, contactId)
            return id.toInt()
        }
    }
