package io.github.hdghg.sevenhandshakes.ui.contactlink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs
import io.github.hdghg.sevenhandshakes.data.share.ContactLink
import io.github.hdghg.sevenhandshakes.data.share.ContactLinkImporter
import io.github.hdghg.sevenhandshakes.data.share.SharedContact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/** The contact of a link as it will be once saved. */
data class ContactPreview(val name: String?, val email: String?, val publicKey: String)

data class ContactLinkUiState(
    /** True while the link is being read. */
    val isLoading: Boolean = true,
    /** The contact of the link, not saved yet. Null while loading, and when the link holds no valid contact. */
    val contact: ContactPreview? = null,
    val isSaving: Boolean = false,
    /**
     * Id in the contact table once there is nothing left to save: the contact was saved now,
     * or the app has it already and the link adds nothing to it. The screen then shows it.
     */
    val savedContactId: Int? = null,
) {
    val isLinkInvalid: Boolean get() = !isLoading && contact == null && savedContactId == null
}

/** Shows the contact of a [ContactLink] another app asked to open, and saves it when the user says so. */
class ContactLinkViewModel
    constructor(
        private val importer: ContactLinkImporter,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val link: String = checkNotNull(savedStateHandle[MyAppDestinationsArgs.CONTACT_LINK_ARG])

        private var shared: SharedContact? = null

        private val _uiState = MutableStateFlow(ContactLinkUiState())
        val uiState: StateFlow<ContactLinkUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val contact = ContactLink.decode(link)
                if (contact == null) {
                    Timber.w("A link without a valid contact was opened")
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                shared = contact
                val existing = importer.find(contact)
                val preview = ContactPreview(
                    name = ContactLinkImporter.nameAfterSaving(existing, contact),
                    email = ContactLinkImporter.emailAfterSaving(existing, contact),
                    publicKey = contact.publicKey,
                )
                // The app's own contact is shown at once unless the link has a name or email it lacks.
                val nothingToSave = existing != null &&
                    (contact.name == null || !existing.name.isNullOrBlank()) &&
                    (contact.email == null || !existing.email.isNullOrBlank())
                _uiState.update {
                    it.copy(isLoading = false, contact = preview, savedContactId = existing?.id.takeIf { nothingToSave })
                }
            }
        }

        fun save() {
            val contact = shared ?: return
            if (_uiState.value.isSaving) return
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                val id = importer.save(contact)
                Timber.d("Contact %d saved from a link", id)
                _uiState.update { it.copy(isSaving = false, savedContactId = id) }
            }
        }
    }
