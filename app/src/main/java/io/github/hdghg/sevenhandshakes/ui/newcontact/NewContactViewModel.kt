package io.github.hdghg.sevenhandshakes.ui.newcontact

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
    val isFavorite: Boolean = false,
    val isIgnored: Boolean = false,
    val isSaving: Boolean = false,
    /** String resource id of an error to show once, or null. */
    val userMessage: Int? = null,
    val editComplete: Boolean = false,
)

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
            val state = _uiState.value
            if (state.isSaving || state.isLoading) return
            if (contactId != null) {
                save(contactId, state)
                return
            }
            val publicKey = state.publicKey.trim()
            if (publicKey.isEmpty()) {
                _uiState.update { it.copy(userMessage = R.string.new_contact_empty_public_key) }
                return
            }
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                val result = contactRepository.addContact(
                    publicKey = publicKey,
                    name = state.name.trim().ifEmpty { null },
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

        private fun save(id: Int, state: NewContactUiState) {
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                contactRepository.updateContact(
                    id = id,
                    name = state.name.trim().ifEmpty { null },
                    isFavorite = state.isFavorite,
                    isIgnored = state.isIgnored,
                )
                _uiState.update { it.copy(isSaving = false, editComplete = true) }
            }
        }

        companion object {
            const val BYTES_PUBLIC_KEY_MAX = 64
            const val BYTES_NAME_MAX = 64
        }
    }
