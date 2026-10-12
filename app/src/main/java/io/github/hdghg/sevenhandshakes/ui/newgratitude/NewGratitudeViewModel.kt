package io.github.hdghg.sevenhandshakes.ui.newgratitude

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class NewGratitudeUiState(
    val text: String = "",
    val bytesSpentText: Int = 0,
    val receiver: String = "",
    val bytesSpentReceiver: Int = 0,
    val stars: Int = 2,
    val editComplete: Boolean = false,
)

@HiltViewModel
class NewGratitudeViewModel
    @Inject
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(NewGratitudeUiState())

        val uiState: StateFlow<NewGratitudeUiState> = _uiState.asStateFlow()

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

        fun complete() {
            _uiState.update {
                it.copy(
                    editComplete = true,
                )
            }
        }

        companion object {
            const val BYTES_TEXT_MAX = 200
            const val BYTES_RECEIVER_MAX = 64
        }
    }
