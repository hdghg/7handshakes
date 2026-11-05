package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityWithContact
import kotlinx.coroutines.flow.Flow

interface IdentityRepository {
    fun getIdentitiesStream(): Flow<List<IdentityWithContact>>

    /**
     * Saves a new identity for [privateKey]. The contact with the matching public key
     * is created if missing, and its name is set to [name].
     */
    suspend fun addIdentity(privateKey: String, name: String): AddIdentityResult
}

sealed interface AddIdentityResult {
    data class Success(val id: Int) : AddIdentityResult

    /** [privateKey] is not a readable Ed25519 private key. Nothing was saved. */
    data object InvalidPrivateKey : AddIdentityResult

    /** An identity with this private key already exists. Nothing was saved. */
    data object Duplicate : AddIdentityResult
}
