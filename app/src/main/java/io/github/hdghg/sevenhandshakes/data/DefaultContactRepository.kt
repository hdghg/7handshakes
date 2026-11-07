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

        override fun getContactStream(id: Int): Flow<ContactRecord?> = contactDao.observeById(id)

        override suspend fun getContactByPublicKey(publicKey: String): ContactRecord? =
            contactDao.getByPublicKey(publicKey)

        override suspend fun addContact(
            publicKey: String,
            name: String?,
            isFavorite: Boolean,
            isIgnored: Boolean,
        ): AddContactResult {
            val record = ContactRecord(
                publicKey = publicKey,
                name = name,
                isFavorite = isFavorite,
                isIgnored = isIgnored,
            )
            // publicKey is the only unique column, so a constraint failure means a duplicate.
            val id = try {
                contactDao.insert(record).toInt()
            } catch (e: SQLiteConstraintException) {
                Timber.w(e, "Contact already exists")
                return AddContactResult.Duplicate
            }
            Timber.d("Contact saved, favorite=%b, ignored=%b", isFavorite, isIgnored)
            return AddContactResult.Success(id)
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
