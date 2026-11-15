package io.github.hdghg.sevenhandshakes.ui.reviewdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.Review
import io.github.hdghg.sevenhandshakes.data.ReviewRepository
import io.github.hdghg.sevenhandshakes.util.UuidV7
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ReviewDetailUiState(
    /** Null while loading, and when there is no such review. */
    val review: Review? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class ReviewDetailViewModel
    @Inject
    constructor(
        reviewRepository: ReviewRepository,
        contactRepository: ContactRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val reviewId: Int = checkNotNull(savedStateHandle[MyAppDestinationsArgs.REVIEW_ID_ARG])

        val uiState: StateFlow<ReviewDetailUiState> =
            combine(
                reviewRepository.getReviewStream(reviewId),
                contactRepository.getContactsStream(),
            ) { record, contacts ->
                val namesById = contacts.associate { it.id to it.displayName }
                ReviewDetailUiState(
                    review = record?.let {
                        Review(
                            id = it.id,
                            uuid = it.uuid,
                            text = it.text,
                            rate = it.gratitude,
                            sourceName = namesById[it.sourceContactId],
                            targetName = namesById[it.targetContactId],
                            createdAt = UuidV7.timestampOf(it.uuid),
                        )
                    },
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ReviewDetailUiState(isLoading = true),
            )
    }
