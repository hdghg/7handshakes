package io.github.hdghg.sevenhandshakes.ui.contactdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.IdentityRepository
import io.github.hdghg.sevenhandshakes.data.MAX_HANDSHAKES
import io.github.hdghg.sevenhandshakes.data.ReviewExchange
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ContactDetailUiState(
    val contact: ContactRecord? = null,
    val isLoading: Boolean = false,
    /**
     * Handshake ratings calculated so far: index 0 is one handshake, index 1 two handshakes, and so on.
     * Levels past the end of the list are still waiting. Null when there is no identity to compare against.
     */
    val handshakeRatings: List<ReviewExchange>? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactDetailViewModel
    @Inject
    constructor(
        contactRepository: ContactRepository,
        private val reviewRepository: ReviewRepository,
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

        private val handshakeRatings: Flow<List<ReviewExchange>?> =
            myContactId.flatMapLatest { me ->
                if (me == null) flowOf(null) else ratingsFrom(me, level = 1, previous = null, done = emptyList())
            }

        val uiState: StateFlow<ContactDetailUiState> =
            combine(contactRepository.getContactStream(contactId), handshakeRatings) { contact, ratings ->
                ContactDetailUiState(contact = contact, handshakeRatings = ratings)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ContactDetailUiState(isLoading = true),
            )

        /**
         * Calculates [level] from [previous], then the next level from that result, up to [MAX_HANDSHAKES].
         * Emits the growing list of finished levels, so each level appears as soon as it is ready.
         * When a level emits a new value, every level after it starts over.
         */
        private fun ratingsFrom(
            me: Int,
            level: Int,
            previous: ReviewExchange?,
            done: List<ReviewExchange>,
        ): Flow<List<ReviewExchange>> =
            reviewRepository
                .getRateCountsStream(contactId = contactId, myContactId = me, level = level, previous = previous)
                .flatMapLatest { exchange ->
                    val finished = done + exchange
                    if (level >= MAX_HANDSHAKES) {
                        flowOf(finished)
                    } else {
                        ratingsFrom(me, level + 1, exchange, finished).onStart { emit(finished) }
                    }
                }
    }
