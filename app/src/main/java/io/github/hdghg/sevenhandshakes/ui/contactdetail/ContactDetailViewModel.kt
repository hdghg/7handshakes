package io.github.hdghg.sevenhandshakes.ui.contactdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.DeleteContactResult
import io.github.hdghg.sevenhandshakes.data.IdentityRepository
import io.github.hdghg.sevenhandshakes.data.share.ContactLink
import io.github.hdghg.sevenhandshakes.data.share.SharedContact
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactDetailUiState(
    val contact: ContactRecord? = null,
    val isLoading: Boolean = false,
    /** True when one of the user's identities uses this contact, which then cannot be deleted. */
    val isUsedByIdentity: Boolean = false,
    /** True once the contact was deleted; the screen should close. */
    val isDeleted: Boolean = false,
    /** A [ContactLink] to the contact, or null when it has none; see [ContactLink.encode]. */
    val shareLink: String? = null,
)

@HiltViewModel
class ContactDetailViewModel
    @Inject
    constructor(
        private val contactRepository: ContactRepository,
        identityRepository: IdentityRepository,
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

        val uiState: StateFlow<ContactDetailUiState> =
            contactInfo.combine(isDeleted) { (contact, usedByIdentity), deleted ->
                ContactDetailUiState(
                    contact = contact,
                    shareLink = contact?.let {
                        ContactLink.encode(SharedContact(publicKey = it.publicKey, name = it.name, email = it.email))
                    },
                    isUsedByIdentity = usedByIdentity,
                    isDeleted = deleted,
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
