package io.github.hdghg.sevenhandshakes.data

import android.database.sqlite.SQLiteConstraintException
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getContactsStream(): Flow<List<ContactRecord>>

    /** Returns the contact with [publicKey], or null if there is none. */
    suspend fun getContactByPublicKey(publicKey: String): ContactRecord?

    /**
     * Saves a new contact.
     */
    suspend fun addContact(publicKey: String, name: String?, isFavorite: Boolean, isIgnored: Boolean): AddContactResult

    /**
     * Returns the id of the contact with [publicKey], creating an unnamed contact first if none exists.
     */
    suspend fun getOrCreateContactId(publicKey: String): InsertResult
}

sealed interface InsertResult {
    class Success(val id: Int) : InsertResult

    class Error(val e: SQLiteConstraintException) : InsertResult
}

sealed interface AddContactResult {
    data class Success(val id: Int) : AddContactResult

    /** A contact with the same public key already exists. Nothing was saved. */
    data object Duplicate : AddContactResult
}
