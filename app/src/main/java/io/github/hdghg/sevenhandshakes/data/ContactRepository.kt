package io.github.hdghg.sevenhandshakes.data

import android.database.sqlite.SQLiteConstraintException
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getContactsStream(): Flow<List<ContactRecord>>

    /**
     * Saves a new contact.
     * @throws DuplicateContactException if a contact with the same public key already exists.
     */
    suspend fun addContact(publicKey: String, name: String?, isFavorite: Boolean, isIgnored: Boolean)

    /**
     * Returns the id of the contact with [publicKey], creating an unnamed contact first if none exists.
     */
    suspend fun getOrCreateContactId(publicKey: String): InsertResult
}

sealed interface InsertResult {
    class Success(val id: Int) : InsertResult

    class Error(val e: SQLiteConstraintException) : InsertResult
}

class DuplicateContactException(publicKey: String) : Exception("Contact with public key $publicKey already exists")
