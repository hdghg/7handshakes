package io.github.hdghg.sevenhandshakes.ui.newgratitude

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.InsertResult
import io.github.hdghg.sevenhandshakes.data.ReviewRepository
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.util.Ed25519Keys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class NewGratitudeUiState(
    val text: String = "",
    val bytesSpentText: Int = 0,
    val receiver: String = "",
    val bytesSpentReceiver: Int = 0,
    /** True when [receiver] is filled in but is not a valid Ed25519 public key. */
    val isReceiverInvalid: Boolean = false,
    val stars: Int = 2,
    val isSaving: Boolean = false,
    /** String resource id of an error to show once, or null. */
    val userMessage: Int? = null,
    val editComplete: Boolean = false,
)

@HiltViewModel
class NewGratitudeViewModel
    @Inject
    constructor(
        private val reviewRepository: ReviewRepository,
        private val contactRepository: ContactRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(NewGratitudeUiState())

        val uiState: StateFlow<NewGratitudeUiState> = _uiState.asStateFlow()

        /** Contacts the receiver can be picked from. Ignored contacts are left out. */
        val contacts: StateFlow<List<ContactRecord>> =
            contactRepository
                .getContactsStream()
                .map { list -> list.filterNot { it.isIgnored } }
                .map { list -> list.filterNot { it.name == null } }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList(),
                )

        fun updateText(newText: String) {
            val size = newText.toByteArray().size
            if (size > BYTES_TEXT_MAX) {
                return
            }
            _uiState.update {
                it.copy(
                    text = newText,
                    bytesSpentText = size,
                )
            }
        }

        fun updateReceiver(newReceiver: String) {
            val size = newReceiver.toByteArray().size
            if (size > BYTES_RECEIVER_MAX) {
                return
            }
            _uiState.update {
                it.copy(
                    receiver = newReceiver,
                    bytesSpentReceiver = size,
                    isReceiverInvalid = newReceiver.isNotBlank() && !Ed25519Keys.isValidPublicKey(newReceiver),
                )
            }
        }

        fun setStars(count: Int) {
            _uiState.update {
                it.copy(
                    stars = count.coerceAtMost(3),
                )
            }
        }

        fun userMessageShown() {
            _uiState.update { it.copy(userMessage = null) }
        }

        fun complete() {
            val state = _uiState.value
            if (state.isSaving) return
            val receiverKey = state.receiver.trim()
            if (receiverKey.isEmpty()) {
                _uiState.update { it.copy(userMessage = R.string.new_gratitude_empty_receiver) }
                return
            }
            Timber.d("Completing new gratitude, stars=%d", state.stars)
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                when (val res = contactRepository.getOrCreateContactId(receiverKey)) {
                    is InsertResult.Error -> {
                        Timber.e(res.e, "Failed to place review")
                        _uiState.update {
                            it.copy(
                                userMessage = R.string.new_gratitude_cannot_create_contact,
                                isSaving = false,
                            )
                        }
                    }

                    is InsertResult.Success -> {
                        reviewRepository.placeNewReview(
                            reviewText = state.text,
                            gratitude = state.stars.toByte(),
                            contactId = res.id,
                        )
                        _uiState.update {
                            it.copy(
                                isSaving = false,
                                editComplete = true,
                            )
                        }
                    }
                }
            }
        }

        companion object {
            const val BYTES_TEXT_MAX = 200
            const val BYTES_RECEIVER_MAX = 64
        }
    }
