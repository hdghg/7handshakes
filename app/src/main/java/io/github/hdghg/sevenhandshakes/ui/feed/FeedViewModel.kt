package io.github.hdghg.sevenhandshakes.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.Review
import io.github.hdghg.sevenhandshakes.data.ReviewRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FeedUiState(
    val items: List<Review> = emptyList(),
    val isLoading: Boolean = false,
)

@HiltViewModel
class FeedViewModel
    @Inject
    constructor(
        reviewRepository: ReviewRepository,
        contactRepository: ContactRepository,
    ) : ViewModel() {
        val uiState: StateFlow<FeedUiState> =
            combine(
                reviewRepository.getGratitudesStream(),
                contactRepository.getContactsStream(),
            ) { records, contacts ->
                val namesById = contacts.associate { it.id to it.name }
                FeedUiState(
                    items =
                        records.map {
                            Review(
                                id = it.id,
                                uuid = it.uuid,
                                text = it.text,
                                rate = it.gratitude,
                                sourceName = namesById[it.sourceContactId],
                                targetName = namesById[it.targetContactId],
                            )
                        },
                )
            }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = FeedUiState(isLoading = true),
                )
    }
