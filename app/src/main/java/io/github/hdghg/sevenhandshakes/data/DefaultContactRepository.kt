package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.contact.ContactDao
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultContactRepository
    @Inject
    constructor(
        private val contactDao: ContactDao,
    ) : ContactRepository {
        override fun getContactsStream(): Flow<List<ContactRecord>> =
            contactDao.observeAll().onEach { Timber.d("Loaded %d contacts from DB", it.size) }
    }
