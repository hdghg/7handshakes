package io.github.hdghg.sevenhandshakes.ui.addeditidentity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.AddIdentityResult
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.IdentityRepository
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
    /** True when [privateKey] is a passphrase-protected OpenSSH key, which cannot be read. */
    val isPrivateKeyEncrypted: Boolean = false,
    val name: String = "",
    /** True once the user has edited the name, so the "required" error is not shown on a fresh form. */
    val nameTouched: Boolean = false,
    val isSaving: Boolean = false,
    /** String resource id of an error to show once, or null. */
    val userMessage: Int? = null,
    val editComplete: Boolean = false,
) {
    val isPrivateKeyError: Boolean get() = privateKey.isNotBlank() && publicKey == null

    val isNameError: Boolean get() = name.isBlank() && (nameTouched || publicKey != null)
}

@HiltViewModel
class AddEditIdentityViewModel
    @Inject
    constructor(
        private val contactRepository: ContactRepository,
        private val identityRepository: IdentityRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AddEditIdentityUiState())

        val uiState: StateFlow<AddEditIdentityUiState> = _uiState.asStateFlow()

        private var contactLookup: Job? = null

        fun updatePrivateKey(newKey: String) {
            if (newKey.toByteArray().size > BYTES_PRIVATE_KEY_MAX) {
                return
            }
            val publicKey = Ed25519Keys.publicKeyOf(newKey)
            val isEncrypted = publicKey == null && Ed25519Keys.isEncryptedOpenSshKey(newKey)
            _uiState.update {
                it.copy(privateKey = newKey, publicKey = publicKey, isPrivateKeyEncrypted = isEncrypted)
            }
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

        fun userMessageShown() {
            _uiState.update { it.copy(userMessage = null) }
        }

        fun complete() {
            val state = _uiState.value
            if (state.isSaving) return
            val name = state.name.trim()
            if (state.publicKey == null || name.isEmpty()) {
                _uiState.update { it.copy(nameTouched = true, userMessage = R.string.identity_cannot_save) }
                return
            }
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                val result = identityRepository.addIdentity(privateKey = state.privateKey, name = name)
                _uiState.update {
                    when (result) {
                        is AddIdentityResult.Success -> it.copy(isSaving = false, editComplete = true)
                        AddIdentityResult.Duplicate -> {
                            Timber.w("Identity already exists")
                            it.copy(isSaving = false, userMessage = R.string.identity_duplicate)
                        }
                        AddIdentityResult.InvalidPrivateKey ->
                            it.copy(isSaving = false, userMessage = R.string.identity_cannot_save)
                    }
                }
            }
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
            // Room for a PEM or OpenSSH key with its header lines.
            const val BYTES_PRIVATE_KEY_MAX = 1024
            const val BYTES_NAME_MAX = 64
        }
    }
