package io.github.hdghg.sevenhandshakes.ui.contactdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.IdentityRepository
import io.github.hdghg.sevenhandshakes.data.ReviewRepository
import io.github.hdghg.sevenhandshakes.data.SelectedIdentityRepository
import io.github.hdghg.sevenhandshakes.data.selectedIdentityOf
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Review counts between this contact and the selected identity, keyed by rate level.
 * Levels without reviews are absent.
 */
data class ReviewExchange(
    /** Reviews this contact wrote to the selected identity. */
    val received: Map<Byte, Int> = emptyMap(),
    /** Reviews the selected identity wrote to this contact. */
    val given: Map<Byte, Int> = emptyMap(),
) {
    fun receivedOf(level: Byte): Int = received[level] ?: 0

    fun givenOf(level: Byte): Int = given[level] ?: 0

    /** Received minus given: positive means this contact thanked you more often than you thanked them. */
    fun differenceOf(level: Byte): Int = receivedOf(level) - givenOf(level)
}

data class ContactDetailUiState(
    val contact: ContactRecord? = null,
    val isLoading: Boolean = false,
    /** Null when there is no identity to compare against. */
    val exchange: ReviewExchange? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactDetailViewModel
    @Inject
    constructor(
        contactRepository: ContactRepository,
        reviewRepository: ReviewRepository,
        identityRepository: IdentityRepository,
        selectedIdentityRepository: SelectedIdentityRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val contactId: Int = checkNotNull(savedStateHandle[MyAppDestinationsArgs.CONTACT_ID_ARG])

        private val myContactId: Flow<Int?> =
            combine(
                identityRepository.getIdentitiesStream(),
                selectedIdentityRepository.getSelectedIdentityIdStream(),
            ) { identities, selectedId ->
                selectedIdentityOf(identities, selectedId)?.identity?.contactId
            }.distinctUntilChanged()

        private val exchange: Flow<ReviewExchange?> =
            myContactId.flatMapLatest { me ->
                if (me == null) {
                    flowOf(null)
                } else {
                    combine(
                        reviewRepository.getRateCountsStream(sourceContactId = contactId, targetContactId = me),
                        reviewRepository.getRateCountsStream(sourceContactId = me, targetContactId = contactId),
                    ) { received, given -> ReviewExchange(received = received, given = given) }
                }
            }

        val uiState: StateFlow<ContactDetailUiState> =
            combine(contactRepository.getContactStream(contactId), exchange) { contact, exchange ->
                ContactDetailUiState(contact = contact, exchange = exchange)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ContactDetailUiState(isLoading = true),
            )
    }
