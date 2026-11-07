package io.github.hdghg.sevenhandshakes.data

import io.github.hdghg.sevenhandshakes.data.source.identity.IdentityWithContact
import kotlinx.coroutines.flow.Flow

/**
 * Remembers which identity the user acts as.
 */
interface SelectedIdentityRepository {
    /** Emits the id of the selected identity, or null when none was selected yet. */
    fun getSelectedIdentityIdStream(): Flow<Int?>

    suspend fun selectIdentity(id: Int)
}

/**
 * The identity with [selectedId]. Until the user picks one (or if the stored one no longer exists),
 * the first identity counts as selected. Null only when there are no identities.
 */
fun selectedIdentityOf(identities: List<IdentityWithContact>, selectedId: Int?): IdentityWithContact? =
    identities.find { it.identity.id == selectedId } ?: identities.firstOrNull()
