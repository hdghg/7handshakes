package io.github.hdghg.sevenhandshakes.ui.reviewlink

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.Review
import io.github.hdghg.sevenhandshakes.data.share.ReviewLink
import io.github.hdghg.sevenhandshakes.data.share.ReviewLinkImporter
import io.github.hdghg.sevenhandshakes.data.share.SharedReview
import io.github.hdghg.sevenhandshakes.di.DefaultDispatcher
import io.github.hdghg.sevenhandshakes.util.UuidV7
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

data class ReviewLinkUiState(
    /** True while the link is being read. */
    val isLoading: Boolean = true,
    /** The review of the link, not saved yet. Null while loading, and when the link holds no valid review. */
    val review: Review? = null,
    val isSaving: Boolean = false,
    /** Id in the review table once the review is there: saved now, or there already. The screen then shows it. */
    val savedReviewId: Int? = null,
) {
    val isLinkInvalid: Boolean get() = !isLoading && review == null && savedReviewId == null
}

/** Shows the review of a [ReviewLink] another app asked to open, and saves it when the user says so. */
@HiltViewModel
class ReviewLinkViewModel
    @Inject
    constructor(
        private val importer: ReviewLinkImporter,
        private val contactRepository: ContactRepository,
        @param:DefaultDispatcher private val dispatcher: CoroutineDispatcher,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val link: String = checkNotNull(savedStateHandle[MyAppDestinationsArgs.REVIEW_LINK_ARG])

        private var shared: SharedReview? = null

        private val _uiState = MutableStateFlow(ReviewLinkUiState())
        val uiState: StateFlow<ReviewLinkUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                // Checking the signature takes a moment.
                val review = withContext(dispatcher) { ReviewLink.decode(link) }
                if (review == null) {
                    Timber.w("A link without a valid review was opened")
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }
                shared = review
                val savedId = importer.findId(review)
                val namesByKey = contactRepository.getContactsStream().first().associate { it.publicKey to it.displayName }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        savedReviewId = savedId,
                        review = Review(
                            id = savedId ?: 0,
                            uuid = review.uuid,
                            text = review.text,
                            rate = review.rating,
                            sourceName = namesByKey[review.authorPublicKey],
                            targetName = namesByKey[review.receiverPublicKey],
                            createdAt = UuidV7.timestampOf(review.uuid),
                        ),
                    )
                }
            }
        }

        fun save() {
            val review = shared ?: return
            if (_uiState.value.isSaving) return
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                val id = importer.save(review)
                Timber.d("Review %s saved from a link", review.uuid)
                _uiState.update { it.copy(isSaving = false, savedReviewId = id) }
            }
        }
    }
