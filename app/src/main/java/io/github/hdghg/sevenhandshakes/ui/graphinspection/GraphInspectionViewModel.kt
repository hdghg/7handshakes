package io.github.hdghg.sevenhandshakes.ui.graphinspection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.DEFAULT_HANDSHAKE_DEPTH
import io.github.hdghg.sevenhandshakes.data.HandshakeDepthRepository
import io.github.hdghg.sevenhandshakes.data.IdentityRepository
import io.github.hdghg.sevenhandshakes.data.ReviewExchange
import io.github.hdghg.sevenhandshakes.data.ReviewRepository
import io.github.hdghg.sevenhandshakes.data.SelectedIdentityRepository
import io.github.hdghg.sevenhandshakes.data.selectedIdentityOf
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GraphInspectionUiState(
    /** Public key typed or picked for the contact in the middle of every handshake path. */
    val middleKey: String = "",
    /** Public key typed or picked for the contact at both ends of every handshake path. */
    val endKey: String = "",
    /** The contact [middleKey] names, or null when no contact has that key. */
    val middleContact: ContactRecord? = null,
    /** The contact [endKey] names, or null when no contact has that key. */
    val endContact: ContactRecord? = null,
    /** All contacts, to pick from. */
    val contacts: List<ContactRecord> = emptyList(),
    /**
     * Handshake ratings calculated so far: index 0 is one handshake, index 1 two handshakes, and so on.
     * Levels past the end of the list are still waiting. Null until both contacts are chosen.
     */
    val handshakeRatings: List<ReviewExchange>? = null,
    /** How many handshake levels are calculated, shared by the whole app. */
    val handshakeDepth: Int = DEFAULT_HANDSHAKE_DEPTH,
) {
    /** Display names of all contacts by id, for labelling handshake paths. Null for unknown contacts. */
    val contactNames: Map<Int, String?> get() = contacts.associate { it.id to it.displayName }
}

/** Shows the handshake ratings between any two contacts, one in the middle and one at the ends of the paths. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GraphInspectionViewModel
    @Inject
    constructor(
        contactRepository: ContactRepository,
        private val reviewRepository: ReviewRepository,
        identityRepository: IdentityRepository,
        selectedIdentityRepository: SelectedIdentityRepository,
        private val handshakeDepthRepository: HandshakeDepthRepository,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        /**
         * Whether the middle was saved before this view model, e.g. across process death.
         * Read before [getStateFlow][SavedStateHandle.getStateFlow], which stores its default in the handle.
         */
        private val isMiddleRestored: Boolean = savedStateHandle.contains(MIDDLE_KEY)

        private val middleKey: StateFlow<String> = savedStateHandle.getStateFlow(MIDDLE_KEY, "")
        private val endKey: StateFlow<String> = savedStateHandle.getStateFlow(END_KEY, "")

        private val contacts: Flow<List<ContactRecord>> = contactRepository.getContactsStream()

        private val handshakeDepth: Flow<Int> = handshakeDepthRepository.getHandshakeDepthStream()

        private val middleContact: Flow<ContactRecord?> =
            combine(contacts, middleKey) { list, key -> list.byKey(key) }.distinctUntilChanged()
        private val endContact: Flow<ContactRecord?> =
            combine(contacts, endKey) { list, key -> list.byKey(key) }.distinctUntilChanged()

        private val handshakeRatings: Flow<List<ReviewExchange>?> =
            combine(middleContact.map { it?.id }, endContact.map { it?.id }, handshakeDepth) { middle, end, depth ->
                Triple(middle, end, depth)
            }.distinctUntilChanged()
                .flatMapLatest { (middle, end, depth) ->
                    if (middle == null || end == null) {
                        flowOf(null)
                    } else {
                        // Empty until level 1 is ready, so the screen shows it as calculating.
                        reviewRepository.getHandshakeRatingsStream(end, middle, depth).onStart { emit(emptyList()) }
                    }
                }

        val uiState: StateFlow<GraphInspectionUiState> =
            combine(
                combine(middleKey, endKey, ::Pair),
                combine(middleContact, endContact, ::Pair),
                contacts,
                handshakeRatings,
                handshakeDepth,
            ) { (middle, end), (middleRecord, endRecord), list, ratings, depth ->
                GraphInspectionUiState(
                    middleKey = middle,
                    endKey = end,
                    middleContact = middleRecord,
                    endContact = endRecord,
                    contacts = list,
                    handshakeRatings = ratings,
                    handshakeDepth = depth,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = GraphInspectionUiState(),
            )

        init {
            // The middle starts as the selected identity, as on the rest of the app.
            if (!isMiddleRestored) {
                viewModelScope.launch {
                    val identities = identityRepository.getIdentitiesStream().first()
                    val selectedId = selectedIdentityRepository.getSelectedIdentityIdStream().first()
                    val key = selectedIdentityOf(identities, selectedId)?.contact?.publicKey
                    if (key != null && middleKey.value.isEmpty()) savedStateHandle[MIDDLE_KEY] = key
                }
            }
        }

        fun updateMiddleKey(key: String) {
            savedStateHandle[MIDDLE_KEY] = key
        }

        fun updateEndKey(key: String) {
            savedStateHandle[END_KEY] = key
        }

        fun setHandshakeDepth(depth: Int) {
            viewModelScope.launch { handshakeDepthRepository.setHandshakeDepth(depth) }
        }

        private companion object {
            const val MIDDLE_KEY = "middleKey"
            /** The same key as the route argument, so a contact passed in fills the field. */
            const val END_KEY = MyAppDestinationsArgs.END_KEY_ARG

            fun List<ContactRecord>.byKey(key: String): ContactRecord? =
                key.trim().takeIf { it.isNotEmpty() }?.let { trimmed -> find { it.publicKey == trimmed } }
        }
    }
