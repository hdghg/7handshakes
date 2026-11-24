package io.github.hdghg.sevenhandshakes.ui.reviewrequest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.IdentityRepository
import io.github.hdghg.sevenhandshakes.data.REVIEW_TEXT_BYTES_MAX
import io.github.hdghg.sevenhandshakes.data.SelectedIdentityRepository
import io.github.hdghg.sevenhandshakes.data.cleanReviewText
import io.github.hdghg.sevenhandshakes.data.selectedIdentityOf
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How a review request is handed to the person asked for the review. */
enum class RequestMethod { Qr, DeepLink, RichTextLink, File }

data class ReviewRequestUiState(
    val method: RequestMethod = RequestMethod.Qr,
    /** Public key of the contact the review is to be about. Required. */
    val targetKey: String = "",
    val targetName: String = "",
    val targetEmail: String = "",
    /** Review text the requester suggests; the reviewer may keep, change or drop it. */
    val suggestedText: String = "",
    /** Only taken into account when [method] is [RequestMethod.File]. */
    val includeFavorites: Boolean = false,
) {
    val isTargetKeyBlank: Boolean get() = targetKey.isBlank()

    /** True when [targetKey] is filled in but is not a valid Ed25519 public key. */
    val isTargetKeyInvalid: Boolean get() = targetKey.isNotBlank() && !Ed25519Keys.isValidPublicKey(targetKey)

    val canIncludeFavorites: Boolean get() = method == RequestMethod.File
}

/** The form of a review request. Nothing is sent or shared yet: the screen is only its UI. */
class ReviewRequestViewModel(
    contactRepository: ContactRepository,
    identityRepository: IdentityRepository,
    selectedIdentityRepository: SelectedIdentityRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReviewRequestUiState())
    val uiState: StateFlow<ReviewRequestUiState> = _uiState.asStateFlow()

    /** Set once the user types or picks a target, so the selected identity no longer replaces it. */
    private var targetKeyEdited = false

    private val allContacts = contactRepository.getContactsStream()

    /** Contacts the target can be picked from: the ones with a name or an email. */
    val pickableContacts: StateFlow<List<ContactRecord>> =
        allContacts
            .map { list -> list.filterNot { it.displayName == null } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The contact whose public key is the target key, the source of the name and email buttons. */
    val targetContact: StateFlow<ContactRecord?> =
        combine(_uiState.map { it.targetKey }.distinctUntilChanged(), allContacts) { key, contacts ->
            val canonical = Ed25519Keys.canonicalPublicKey(key) ?: return@combine null
            contacts.firstOrNull { Ed25519Keys.canonicalPublicKey(it.publicKey) == canonical }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // By default the review is asked about the selected identity.
        viewModelScope.launch {
            val me = selectedIdentityOf(
                identities = identityRepository.getIdentitiesStream().first(),
                selectedId = selectedIdentityRepository.getSelectedIdentityIdStream().first(),
            )
            if (me != null && !targetKeyEdited) {
                _uiState.update { it.copy(targetKey = me.contact.publicKey) }
            }
        }
    }

    fun setMethod(method: RequestMethod) {
        _uiState.update { it.copy(method = method) }
    }

    fun updateTargetKey(key: String) {
        if (key.toByteArray().size > BYTES_KEY_MAX) return
        targetKeyEdited = true
        _uiState.update { it.copy(targetKey = key) }
    }

    fun updateTargetName(name: String) {
        _uiState.update { it.copy(targetName = name) }
    }

    fun updateTargetEmail(email: String) {
        _uiState.update { it.copy(targetEmail = email) }
    }

    /** Characters a review may not hold are taken out, and text past the review size limit is refused. */
    fun updateSuggestedText(text: String) {
        val clean = cleanReviewText(text)
        if (clean.toByteArray().size > REVIEW_TEXT_BYTES_MAX) return
        _uiState.update { it.copy(suggestedText = clean) }
    }

    fun setIncludeFavorites(include: Boolean) {
        _uiState.update { it.copy(includeFavorites = include) }
    }

    fun copyNameFromContact() {
        targetContact.value?.name?.let(::updateTargetName)
    }

    fun copyEmailFromContact() {
        targetContact.value?.email?.let(::updateTargetEmail)
    }

    companion object {
        const val BYTES_KEY_MAX = 64
    }
}
