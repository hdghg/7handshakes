package io.github.hdghg.sevenhandshakes.ui.contactdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.DEFAULT_HANDSHAKE_DEPTH
import io.github.hdghg.sevenhandshakes.data.DeleteContactResult
import io.github.hdghg.sevenhandshakes.data.HandshakeDepthRepository
import io.github.hdghg.sevenhandshakes.data.IdentityRepository
import io.github.hdghg.sevenhandshakes.data.ReviewExchange
import io.github.hdghg.sevenhandshakes.data.ReviewRepository
import io.github.hdghg.sevenhandshakes.data.SelectedIdentityRepository
import io.github.hdghg.sevenhandshakes.data.selectedIdentityOf
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactDetailUiState(
    val contact: ContactRecord? = null,
    val isLoading: Boolean = false,
    /**
     * Handshake ratings calculated so far: index 0 is one handshake, index 1 two handshakes, and so on.
     * Levels past the end of the list are still waiting. Null when there is no identity to compare against.
     */
    val handshakeRatings: List<ReviewExchange>? = null,
    /** Contact id of the selected identity: the middle of every handshake path. */
    val myContactId: Int? = null,
    /** Display names of all contacts by id, for labelling handshake paths. Null for unknown contacts. */
    val contactNames: Map<Int, String?> = emptyMap(),
    /** How many handshake levels are calculated, shared by all contacts. */
    val handshakeDepth: Int = DEFAULT_HANDSHAKE_DEPTH,
    /** True when one of the user's identities uses this contact, which then cannot be deleted. */
    val isUsedByIdentity: Boolean = false,
    /** True once the contact was deleted; the screen should close. */
    val isDeleted: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactDetailViewModel
    @Inject
    constructor(
        private val contactRepository: ContactRepository,
        private val reviewRepository: ReviewRepository,
        identityRepository: IdentityRepository,
        selectedIdentityRepository: SelectedIdentityRepository,
        private val handshakeDepthRepository: HandshakeDepthRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val contactId: Int = checkNotNull(savedStateHandle[MyAppDestinationsArgs.CONTACT_ID_ARG])

        private val isDeleted = MutableStateFlow(false)

        /** The contact, and whether an identity uses it. */
        private val contactInfo: Flow<Pair<ContactRecord?, Boolean>> =
            combine(
                contactRepository.getContactStream(contactId),
                identityRepository.getIdentitiesStream(),
            ) { contact, identities ->
                contact to identities.any { it.identity.contactId == contactId }
            }

        private val myContactId: Flow<Int?> =
            combine(
                identityRepository.getIdentitiesStream(),
                selectedIdentityRepository.getSelectedIdentityIdStream(),
            ) { identities, selectedId ->
                selectedIdentityOf(identities, selectedId)?.identity?.contactId
            }.distinctUntilChanged()

        private val handshakeDepth: Flow<Int> = handshakeDepthRepository.getHandshakeDepthStream()

        private val handshakeRatings: Flow<List<ReviewExchange>?> =
            combine(myContactId, handshakeDepth) { me, depth -> me to depth }
                .flatMapLatest { (me, depth) ->
                    if (me == null) {
                        flowOf(null)
                    } else {
                        // Empty until level 1 is ready, so the screen shows it as calculating.
                        reviewRepository.getHandshakeRatingsStream(contactId, me, depth).onStart { emit(emptyList()) }
                    }
                }

        val uiState: StateFlow<ContactDetailUiState> =
            combine(
                contactInfo,
                handshakeRatings,
                myContactId,
                contactRepository.getContactsStream(),
                handshakeDepth,
            ) { (contact, usedByIdentity), ratings, me, contacts, depth ->
                ContactDetailUiState(
                    contact = contact,
                    handshakeRatings = ratings,
                    myContactId = me,
                    contactNames = contacts.associate { it.id to it.displayName },
                    handshakeDepth = depth,
                    isUsedByIdentity = usedByIdentity,
                )
            }.combine(isDeleted) { state, deleted ->
                state.copy(isDeleted = deleted)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ContactDetailUiState(isLoading = true),
            )

        /** Deletes the contact with its reviews. Does nothing if an identity uses it. */
        fun deleteContact() {
            viewModelScope.launch {
                if (contactRepository.deleteContact(contactId) == DeleteContactResult.Success) {
                    isDeleted.value = true
                }
            }
        }

        fun setHandshakeDepth(depth: Int) {
            viewModelScope.launch { handshakeDepthRepository.setHandshakeDepth(depth) }
        }
    }
