package io.github.hdghg.sevenhandshakes.ui.newcontact

import android.util.Patterns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.AddContactResult
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Value of the contact id argument when a new contact is being added. */
const val NO_CONTACT_ID = -1

data class NewContactUiState(
    /** True when editing an existing contact; its public key cannot change. */
    val isEdit: Boolean = false,
    /** True while the edited contact is being loaded. */
    val isLoading: Boolean = false,
    val publicKey: String = "",
    val name: String = "",
    val email: String = "",
    val isFavorite: Boolean = false,
    val isIgnored: Boolean = false,
    val isSaving: Boolean = false,
    /** Set by the first save attempt, so field errors only show up after the user tried to save. */
    val saveAttempted: Boolean = false,
    /** String resource id of an error to show once, or null. */
    val userMessage: Int? = null,
    val editComplete: Boolean = false,
) {
    val isPublicKeyError: Boolean get() = saveAttempted && publicKey.isBlank()

    val isEmailError: Boolean get() = saveAttempted && !isValidEmail(email)
}

/** Empty counts as valid, since the email is optional. */
private fun isValidEmail(email: String): Boolean = email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

@HiltViewModel
class NewContactViewModel
    @Inject
    constructor(
        private val contactRepository: ContactRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        /** Id of the contact being edited, or null when adding a new one. */
        private val contactId: Int? =
            savedStateHandle.get<Int>(MyAppDestinationsArgs.CONTACT_ID_ARG)?.takeIf { it != NO_CONTACT_ID }

        private val _uiState =
            MutableStateFlow(NewContactUiState(isEdit = contactId != null, isLoading = contactId != null))

        val uiState: StateFlow<NewContactUiState> = _uiState.asStateFlow()

        init {
            if (contactId != null) loadContact(contactId)
        }

        private fun loadContact(id: Int) {
            viewModelScope.launch {
                val contact = contactRepository.getContact(id)
                _uiState.update {
                    if (contact == null) {
                        it.copy(isLoading = false, userMessage = R.string.contact_not_found)
                    } else {
                        it.copy(
                            isLoading = false,
                            publicKey = contact.publicKey,
                            name = contact.name.orEmpty(),
                            email = contact.email.orEmpty(),
                            isFavorite = contact.isFavorite,
                            isIgnored = contact.isIgnored,
                        )
                    }
                }
            }
        }

        fun updatePublicKey(newKey: String) {
            if (contactId != null || newKey.toByteArray().size > BYTES_PUBLIC_KEY_MAX) {
                return
            }
            _uiState.update { it.copy(publicKey = newKey) }
        }

        fun updateName(newName: String) {
            if (newName.toByteArray().size > BYTES_NAME_MAX) {
                return
            }
            _uiState.update { it.copy(name = newName) }
        }

        fun updateEmail(newEmail: String) {
            if (newEmail.toByteArray().size > BYTES_EMAIL_MAX) {
                return
            }
            _uiState.update { it.copy(email = newEmail) }
        }

        fun setFavorite(value: Boolean) {
            _uiState.update { it.copy(isFavorite = value) }
        }

        fun setIgnored(value: Boolean) {
            _uiState.update { it.copy(isIgnored = value) }
        }

        fun userMessageShown() {
            _uiState.update { it.copy(userMessage = null) }
        }

        fun complete() {
            if (_uiState.value.isSaving || _uiState.value.isLoading) return
            _uiState.update { it.copy(saveAttempted = true) }
            val state = _uiState.value
            if (state.isPublicKeyError || state.isEmailError) return

            val name = state.name.trim().ifEmpty { null }
            val email = state.email.trim().ifEmpty { null }
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                if (contactId != null) {
                    contactRepository.updateContact(
                        id = contactId,
                        name = name,
                        email = email,
                        isFavorite = state.isFavorite,
                        isIgnored = state.isIgnored,
                    )
                    _uiState.update { it.copy(isSaving = false, editComplete = true) }
                    return@launch
                }
                val result = contactRepository.addContact(
                    publicKey = state.publicKey.trim(),
                    name = name,
                    email = email,
                    isFavorite = state.isFavorite,
                    isIgnored = state.isIgnored,
                )
                _uiState.update {
                    when (result) {
                        is AddContactResult.Success -> it.copy(isSaving = false, editComplete = true)
                        AddContactResult.Duplicate ->
                            it.copy(isSaving = false, userMessage = R.string.new_contact_duplicate)
                    }
                }
            }
        }

        companion object {
            const val BYTES_PUBLIC_KEY_MAX = 64
            const val BYTES_NAME_MAX = 64

            /** Longest address allowed by RFC 5321. */
            const val BYTES_EMAIL_MAX = 254
        }
    }
