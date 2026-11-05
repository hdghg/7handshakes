package io.github.hdghg.sevenhandshakes.data

import kotlinx.coroutines.flow.Flow

/**
 * Remembers which identity the user acts as.
 */
interface SelectedIdentityRepository {
    /** Emits the id of the selected identity, or null when none was selected yet. */
    fun getSelectedIdentityIdStream(): Flow<Int?>

    suspend fun selectIdentity(id: Int)
}
