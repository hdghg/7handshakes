package io.github.hdghg.sevenhandshakes.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.IdentityRepository
import io.github.hdghg.sevenhandshakes.data.Review
import io.github.hdghg.sevenhandshakes.data.ReviewRepository
import io.github.hdghg.sevenhandshakes.data.SelectedIdentityRepository
import io.github.hdghg.sevenhandshakes.data.selectedIdentityOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FeedUiState(
    val items: List<Review> = emptyList(),
    val isLoading: Boolean = false,
    /** False: only reviews written by or to the selected identity. True: every review. */
    val showAll: Boolean = false,
)

@HiltViewModel
class FeedViewModel
    @Inject
    constructor(
        reviewRepository: ReviewRepository,
        contactRepository: ContactRepository,
        identityRepository: IdentityRepository,
        selectedIdentityRepository: SelectedIdentityRepository,
    ) : ViewModel() {
        private val showAll = MutableStateFlow(false)

        val uiState: StateFlow<FeedUiState> =
            combine(
                reviewRepository.getGratitudesStream(),
                contactRepository.getContactsStream(),
                identityRepository.getIdentitiesStream(),
                selectedIdentityRepository.getSelectedIdentityIdStream(),
                showAll,
            ) { records, contacts, identities, selectedId, showAll ->
                val namesById = contacts.associate { it.id to it.name }
                val myContactId = selectedIdentityOf(identities, selectedId)?.identity?.contactId
                val visible = if (showAll) {
                    records
                } else {
                    records.filter { it.sourceContactId == myContactId || it.targetContactId == myContactId }
                }
                FeedUiState(
                    showAll = showAll,
                    items =
                        visible.map {
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

        fun setShowAll(value: Boolean) {
            showAll.value = value
        }
    }
