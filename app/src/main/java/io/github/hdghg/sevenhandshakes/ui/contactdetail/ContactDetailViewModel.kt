package io.github.hdghg.sevenhandshakes.ui.contactdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.DEFAULT_HANDSHAKE_DEPTH
import io.github.hdghg.sevenhandshakes.data.DeleteContactResult
import io.github.hdghg.sevenhandshakes.data.HandshakeDepthRepository
import io.github.hdghg.sevenhandshakes.data.ReviewExchange
import io.github.hdghg.sevenhandshakes.data.ReviewRepository
import io.github.hdghg.sevenhandshakes.data.SelectedIdentityRepository
import io.github.hdghg.sevenhandshakes.data.selectedIdentityOf
import io.github.hdghg.sevenhandshakes.data.IdentityRepository
import io.github.hdghg.sevenhandshakes.data.share.ContactLink
import io.github.hdghg.sevenhandshakes.data.share.SharedContact
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ContactDetailUiState(
    val contact: ContactRecord? = null,
    val isLoading: Boolean = false,
    /**
     * Handshake ratings between this contact and the selected identity calculated so far: index 0 is one
     * handshake, index 1 two handshakes, and so on. Null when there is no identity to compare against.
     */
    val handshakeRatings: List<ReviewExchange>? = null,
    /** How many handshake levels are calculated, shared by the whole app. */
    val handshakeDepth: Int = DEFAULT_HANDSHAKE_DEPTH,
    /** True when one of the user's identities uses this contact, which then cannot be deleted. */
    val isUsedByIdentity: Boolean = false,
    /** True once the contact was deleted; the screen should close. */
    val isDeleted: Boolean = false,
    /** A [ContactLink] to the contact, or null when it has none; see [ContactLink.encode]. */
    val shareLink: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailViewModel
    constructor(
        private val contactRepository: ContactRepository,
        reviewRepository: ReviewRepository,
        identityRepository: IdentityRepository,
        selectedIdentityRepository: SelectedIdentityRepository,
        handshakeDepthRepository: HandshakeDepthRepository,
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

        private val handshakeDepth: Flow<Int> = handshakeDepthRepository.getHandshakeDepthStream()

        /** Ratings with the selected identity in the middle of the paths and this contact at the ends. */
        private val handshakeRatings: Flow<List<ReviewExchange>?> =
            combine(
                identityRepository.getIdentitiesStream(),
                selectedIdentityRepository.getSelectedIdentityIdStream(),
                handshakeDepth,
            ) { identities, selectedId, depth ->
                selectedIdentityOf(identities, selectedId)?.identity?.contactId to depth
            }.distinctUntilChanged()
                .flatMapLatest { (me, depth) ->
                    if (me == null) {
                        flowOf(null)
                    } else {
                        // Empty until level 1 is ready, so the screen shows it as calculating.
                        reviewRepository.getHandshakeRatingsStream(contactId, me, depth).onStart { emit(emptyList()) }
                    }
                }

        val uiState: StateFlow<ContactDetailUiState> =
            combine(contactInfo, isDeleted, handshakeRatings, handshakeDepth) { (contact, usedByIdentity), deleted, ratings, depth ->
                ContactDetailUiState(
                    contact = contact,
                    shareLink = contact?.let {
                        ContactLink.encode(SharedContact(publicKey = it.publicKey, name = it.name, email = it.email))
                    },
                    isUsedByIdentity = usedByIdentity,
                    isDeleted = deleted,
                    handshakeRatings = ratings,
                    handshakeDepth = depth,
                )
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
    }
