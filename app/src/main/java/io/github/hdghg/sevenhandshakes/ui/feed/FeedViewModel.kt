package io.github.hdghg.sevenhandshakes.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.data.Review
import io.github.hdghg.sevenhandshakes.data.ReviewRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
    ) : ViewModel() {
        val uiState: StateFlow<FeedUiState> =
            reviewRepository
                .getGratitudesStream()
                .map { records ->
                    FeedUiState(
                        items =
                            records.map {
                                Review(id = it.id, uuid = it.uuid, text = it.text, rate = it.gratitude)
                            },
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = FeedUiState(isLoading = true),
                )
    }
