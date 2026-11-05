package io.github.hdghg.sevenhandshakes.ui.addeditidentity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AddEditIdentityUiState(
    val privateKey: String = "",
    /** Public key derived from [privateKey], or null while [privateKey] is not a valid Ed25519 key. */
    val publicKey: String? = null,
    val name: String = "",
    /** True once the user has edited the name, so the "required" error is not shown on a fresh form. */
    val nameTouched: Boolean = false,
) {
    val isPrivateKeyError: Boolean get() = privateKey.isNotBlank() && publicKey == null

    val isNameError: Boolean get() = name.isBlank() && (nameTouched || publicKey != null)
}

@HiltViewModel
class AddEditIdentityViewModel
    @Inject
    constructor(
        private val contactRepository: ContactRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AddEditIdentityUiState())

        val uiState: StateFlow<AddEditIdentityUiState> = _uiState.asStateFlow()

        private var contactLookup: Job? = null

        fun updatePrivateKey(newKey: String) {
            if (newKey.toByteArray().size > BYTES_PRIVATE_KEY_MAX) {
                return
            }
            val publicKey = Ed25519Keys.publicKeyOf(newKey)
            _uiState.update { it.copy(privateKey = newKey, publicKey = publicKey) }
            lookUpContactName(publicKey)
        }

        fun generatePrivateKey() {
            updatePrivateKey(Ed25519Keys.generatePrivateKey())
        }

        fun updateName(newName: String) {
            if (newName.toByteArray().size > BYTES_NAME_MAX) {
                return
            }
            _uiState.update { it.copy(name = newName, nameTouched = true) }
        }

        /** Fills the name from the contact with [publicKey], if such a contact exists and has a name. */
        private fun lookUpContactName(publicKey: String?) {
            contactLookup?.cancel()
            if (publicKey == null) return
            contactLookup = viewModelScope.launch {
                val name = contactRepository.getContactByPublicKey(publicKey)?.name ?: return@launch
                Timber.d("Found existing contact for identity public key")
                _uiState.update {
                    if (it.publicKey == publicKey) it.copy(name = name) else it
                }
            }
        }

        companion object {
            const val BYTES_PRIVATE_KEY_MAX = 128
            const val BYTES_NAME_MAX = 64
        }
    }
