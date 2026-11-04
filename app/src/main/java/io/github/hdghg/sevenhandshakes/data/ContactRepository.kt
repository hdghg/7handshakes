package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun getContactsStream(): Flow<List<ContactRecord>>

    /**
     * Saves a new contact.
     * @throws DuplicateContactException if a contact with the same public key already exists.
     */
    suspend fun addContact(publicKey: String, name: String?, isFavorite: Boolean, isIgnored: Boolean)
}

class DuplicateContactException(publicKey: String) : Exception("Contact with public key $publicKey already exists")
