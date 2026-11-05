package io.github.hdghg.sevenhandshakes.data

import android.database.sqlite.SQLiteConstraintException
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

        override suspend fun getContactByPublicKey(publicKey: String): ContactRecord? =
            contactDao.getByPublicKey(publicKey)

        override suspend fun addContact(publicKey: String, name: String?, isFavorite: Boolean, isIgnored: Boolean) {
            val record = ContactRecord(
                publicKey = publicKey,
                name = name,
                isFavorite = isFavorite,
                isIgnored = isIgnored,
            )
            try {
                contactDao.insert(record)
            } catch (e: SQLiteConstraintException) {
                throw DuplicateContactException(publicKey).apply { initCause(e) }
            }
            Timber.d("Contact saved, favorite=%b, ignored=%b", isFavorite, isIgnored)
        }

        override suspend fun getOrCreateContactId(publicKey: String): InsertResult {
            contactDao.getByPublicKey(publicKey)?.let { return InsertResult.Success(it.id) }
            val record = ContactRecord(publicKey = publicKey, name = null, isFavorite = false, isIgnored = false)
            return try {
                val id = contactDao.insert(record)
                Timber.d("Created contact %d", id)
                InsertResult.Success(id.toInt())
            } catch (e: SQLiteConstraintException) {
                // Another caller inserted the same key between the lookup and the insert.
                val anotherAttempt = contactDao.getByPublicKey(publicKey)
                if (anotherAttempt != null) {
                    InsertResult.Success(anotherAttempt.id)
                } else {
                    InsertResult.Error(e)
                }
            }
        }
    }
