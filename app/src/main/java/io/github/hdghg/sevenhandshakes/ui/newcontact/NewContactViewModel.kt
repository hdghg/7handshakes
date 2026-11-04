package io.github.hdghg.sevenhandshakes.ui.newcontact

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.DuplicateContactException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class NewContactUiState(
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NewContactUiState())

        val uiState: StateFlow<NewContactUiState> = _uiState.asStateFlow()

        fun updatePublicKey(newKey: String) {
            if (newKey.toByteArray().size > BYTES_PUBLIC_KEY_MAX) {
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
            if (state.isSaving) return
            val publicKey = state.publicKey.trim()
            if (publicKey.isEmpty()) {
                _uiState.update { it.copy(userMessage = R.string.new_contact_empty_public_key) }
                return
            }
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                try {
                    contactRepository.addContact(
                        publicKey = publicKey,
                        name = state.name.trim().ifEmpty { null },
                        isFavorite = state.isFavorite,
                        isIgnored = state.isIgnored,
                    )
                    _uiState.update { it.copy(isSaving = false, editComplete = true) }
                } catch (e: DuplicateContactException) {
                    Timber.w(e, "Contact already exists")
                    _uiState.update {
                        it.copy(isSaving = false, userMessage = R.string.new_contact_duplicate)
                    }
                }
            }
        }

        companion object {
            const val BYTES_PUBLIC_KEY_MAX = 64
            const val BYTES_NAME_MAX = 64
        }
    }
