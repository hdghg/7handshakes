package io.github.hdghg.sevenhandshakes.data

import android.database.sqlite.SQLiteConstraintException
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getContactsStream(): Flow<List<ContactRecord>>

    /** Emits the contact with [id], or null if there is none. */
    fun getContactStream(id: Int): Flow<ContactRecord?>

    /** Returns the contact with [id], or null if there is none. */
    suspend fun getContact(id: Int): ContactRecord?

    /** Returns the contact with [publicKey], or null if there is none. */
    suspend fun getContactByPublicKey(publicKey: String): ContactRecord?

    /**
     * Saves a new contact.
     */
    suspend fun addContact(
        publicKey: String,
        name: String?,
        email: String?,
        isFavorite: Boolean,
        isIgnored: Boolean,
    ): AddContactResult

    /** Saves new details for the contact with [id]. The public key cannot change. */
    suspend fun updateContact(id: Int, name: String?, email: String?, isFavorite: Boolean, isIgnored: Boolean)

    /** Deletes the contact with [id] together with every review written by or to it. */
    suspend fun deleteContact(id: Int): DeleteContactResult

    /**
     * Returns the id of the contact with [publicKey], creating an unnamed contact first if none exists.
     */
    suspend fun getOrCreateContactId(publicKey: String): InsertResult
}

sealed interface InsertResult {
    class Success(val id: Int) : InsertResult

    class Error(val e: SQLiteConstraintException) : InsertResult
}

sealed interface DeleteContactResult {
    data object Success : DeleteContactResult

    /** One of the user's identities uses this contact. Nothing was deleted. */
    data object UsedByIdentity : DeleteContactResult
}

sealed interface AddContactResult {
    data class Success(val id: Int) : AddContactResult

    /** A contact with the same public key already exists. Nothing was saved. */
    data object Duplicate : AddContactResult
}
