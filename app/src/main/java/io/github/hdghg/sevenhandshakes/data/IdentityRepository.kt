package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityWithContact
import kotlinx.coroutines.flow.Flow

interface IdentityRepository {
    fun getIdentitiesStream(): Flow<List<IdentityWithContact>>

    /**
     * Saves a new identity linked to the contact with [contactId].
     * @return id of the new identity.
     */
    suspend fun addIdentity(privateKey: String, contactId: Int): Int
}
